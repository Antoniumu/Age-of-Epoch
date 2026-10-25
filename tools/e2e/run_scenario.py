#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Прогон сценария по dev-клиенту: действия (клик, клавиша, ожидание) плюс
скриншот после каждого шага.

Зачем отдельный скрипт, а не xdotool-однострочники: нужен журнал «что сделали →
что получилось», а скриншоты нужны даже если шаг упал. Поэтому ни один шаг не
прерывает прогон: ошибка пишется в steps.log, снимок делается всё равно.

Сценарий — построчный текстовый файл (не YAML, чтобы не тянуть зависимости на
раннер). Каждая строка: команда и аргументы.

    # комментарий
    shot main-menu              снимок экрана -> NAME.png
    click 640 400               левый клик по координатам окна
    rclick 640 400              правый клик
    move 640 400                только навести курсор
    key H                       нажать клавишу (xdotool key)
    keydown W / keyup W         удержание
    type hello                  ввести текст
    wait 1500                   пауза, мс
    wait_window Minecraft 90000 ждать окно по подстроке заголовка, мс
    wait_log <regex> <мс>       ждать строку в run/logs/latest.log
    log текст                   записать в журнал

Запуск:
    DISPLAY=:99 python3 tools/e2e/run_scenario.py tools/e2e/scenarios/boot.txt out/
"""

import os
import re
import subprocess
import sys
import time
import traceback

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
LOG = os.path.join(REPO, "run", "logs", "latest.log")


class Runner:
    def __init__(self, outdir):
        self.outdir = outdir
        os.makedirs(outdir, exist_ok=True)
        self.journal = open(os.path.join(outdir, "steps.log"), "w", encoding="utf-8")
        self.shots = []
        self.failures = 0
        self.window = None

    # ------------------------------------------------------------------ утилиты

    def note(self, message):
        line = "[%s] %s" % (time.strftime("%H:%M:%S"), message)
        print(line, flush=True)
        self.journal.write(line + "\n")
        self.journal.flush()

    def run(self, cmd, timeout=60):
        try:
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
            return result.returncode, (result.stdout or "") + (result.stderr or "")
        except FileNotFoundError:
            return 127, "нет команды: %s" % cmd[0]
        except subprocess.TimeoutExpired:
            return 124, "таймаут: %s" % " ".join(cmd)

    def fail(self, message):
        self.failures += 1
        self.note("ОШИБКА: " + message)

    # ------------------------------------------------------------------ окно

    def find_window(self):
        code, out = self.run(["xdotool", "search", "--onlyvisible", "--name", "Minecraft"])
        ids = [line.strip() for line in out.splitlines() if line.strip().isdigit()]
        if not ids:
            # запасной путь: единственное видимое окно на дисплее
            code, out = self.run(["xdotool", "search", "--onlyvisible", "--name", "."])
            ids = [line.strip() for line in out.splitlines() if line.strip().isdigit()]
        if ids:
            self.window = ids[-1]
            code, name = self.run(["xdotool", "getwindowname", self.window])
            code, geo = self.run(["xdotool", "getwindowgeometry", "--shell", self.window])
            self.note("окно %s: %s (%s)" % (self.window, name.strip(), geo.strip().replace("\n", " ")))
            return True
        return False

    def focus(self):
        if not self.window and not self.find_window():
            return False
        self.run(["xdotool", "windowactivate", "--sync", self.window], timeout=20)
        self.run(["xdotool", "windowfocus", "--sync", self.window], timeout=20)
        return True

    def wait_window(self, pattern, timeout_ms):
        deadline = time.time() + timeout_ms / 1000.0
        while time.time() < deadline:
            code, out = self.run(["xdotool", "search", "--onlyvisible", "--name", pattern])
            if any(line.strip().isdigit() for line in out.splitlines()):
                self.find_window()
                return True
            time.sleep(2)
        self.fail("окно '%s' не появилось за %d мс" % (pattern, timeout_ms))
        return False

    def wait_log(self, pattern, timeout_ms):
        deadline = time.time() + timeout_ms / 1000.0
        rx = re.compile(pattern)
        while time.time() < deadline:
            if os.path.exists(LOG):
                try:
                    with open(LOG, "r", encoding="utf-8", errors="replace") as handle:
                        for line in handle:
                            if rx.search(line):
                                self.note("в логе найдено: " + line.strip()[:160])
                                return True
                except OSError:
                    pass
            time.sleep(2)
        self.fail("в логе нет /%s/ за %d мс" % (pattern, timeout_ms))
        return False

    # ------------------------------------------------------------------ снимок

    def shot(self, name):
        path = os.path.join(self.outdir, name + ".png")
        self.focus()
        code, out = self.run(["import", "-window", "root", "-silent", path], timeout=120)
        if code != 0 or not os.path.exists(path) or os.path.getsize(path) < 1000:
            # запасной путь: xwd + convert
            raw = path + ".xwd"
            code2, out2 = self.run(["xwd", "-root", "-silent", "-out", raw], timeout=120)
            if code2 == 0:
                self.run(["convert", raw, path], timeout=120)
                if os.path.exists(raw):
                    os.remove(raw)
        if os.path.exists(path) and os.path.getsize(path) > 1000:
            self.shots.append(name)
            self.note("снимок %s (%d КБ)" % (name, os.path.getsize(path) // 1024))
            return True
        self.fail("снимок %s не получился: %s" % (name, out.strip()[:200]))
        return False

    # ------------------------------------------------------------------ действия

    def click(self, x, y, button=1):
        if not self.focus():
            self.fail("клик %s,%s: нет окна" % (x, y))
            return
        self.run(["xdotool", "mousemove", "--window", self.window, str(x), str(y)])
        time.sleep(0.15)
        self.run(["xdotool", "click", "--window", self.window, str(button)])
        self.note("клик %s,%s" % (x, y))

    def move(self, x, y):
        if not self.focus():
            return
        self.run(["xdotool", "mousemove", "--window", self.window, str(x), str(y)])
        self.note("курсор %s,%s" % (x, y))

    def key(self, spec):
        if not self.focus():
            self.fail("клавиша %s: нет окна" % spec)
            return
        self.run(["xdotool", "key", "--window", self.window, "--clearmodifiers", spec])
        self.note("клавиша " + spec)

    # ------------------------------------------------------------------ цикл

    def execute(self, path):
        with open(path, "r", encoding="utf-8") as handle:
            lines = handle.readlines()

        self.note("DISPLAY=%s, сценарий %s" % (os.environ.get("DISPLAY"), os.path.basename(path)))
        for number, raw in enumerate(lines, 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split(None, 1)
            command = parts[0]
            rest = parts[1] if len(parts) > 1 else ""
            args = rest.split()
            try:
                if command == "shot":
                    self.shot(args[0] if args else "shot-%02d" % number)
                elif command == "click":
                    self.click(int(args[0]), int(args[1]))
                elif command == "rclick":
                    self.click(int(args[0]), int(args[1]), 3)
                elif command == "move":
                    self.move(int(args[0]), int(args[1]))
                elif command == "key":
                    self.key(rest.strip())
                elif command == "keydown":
                    self.key("keydown " + rest.strip())
                elif command == "keyup":
                    self.key("keyup " + rest.strip())
                elif command == "type":
                    self.key("type --delay 60 " + rest)
                elif command == "wait":
                    time.sleep(int(args[0]) / 1000.0)
                elif command == "wait_window":
                    self.wait_window(args[0] if args else "Minecraft", int(args[1]) if len(args) > 1 else 120000)
                elif command == "wait_log":
                    self.wait_log(args[0], int(args[1]) if len(args) > 1 else 120000)
                elif command == "log":
                    self.note(rest)
                else:
                    self.fail("неизвестная команда в строке %d: %s" % (number, line))
            except Exception:
                self.fail("строка %d (%s):\n%s" % (number, line, traceback.format_exc()))

        self.note("итог: снимков %d, ошибок %d" % (len(self.shots), self.failures))
        with open(os.path.join(self.outdir, "index.txt"), "w", encoding="utf-8") as handle:
            for name in self.shots:
                handle.write(name + "\n")
        return 0 if self.shots else 1


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    scenario = sys.argv[1]
    outdir = sys.argv[2] if len(sys.argv) > 2 else os.path.join(REPO, "run", "e2e", "shots")
    runner = Runner(outdir)
    try:
        return runner.execute(scenario)
    finally:
        runner.journal.close()


if __name__ == "__main__":
    sys.exit(main())
