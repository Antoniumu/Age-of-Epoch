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
    hold H 400                  нажать и подержать (для хоткеев через isDown())
    keydown W / keyup W         удержание
    type hello                  ввести текст
    wait 1500                   пауза, мс
    wait_window Minecraft 90000 ждать окно по подстроке заголовка, мс
    wait_log 120000 <regex>     ждать строку в run/logs/latest.log
    log_mark                    отметить позицию в логе
    wait_log_new 120000 <regex> ждать строку в логе ПОСЛЕ log_mark (новый прогон)
    windows                     вывести список окон (диагностика)
    jstack [метка]              сохранить стек клиентского JVM
    wmctrl                      список окон через WM
    touch run/aoe-devtest.flag  создать файл-триггер (перезапуск зонда замеров)
    write run/x.flag all        создать файл-триггер с содержимым (аудит умений)
    shell bash script.sh start  выполнить команду оболочки (запись видео и т.п.)
    wait_file run/spell-audit.md 600000  ждать появления файла, мс
    wait_screen 30000 AscendedClasses    ждать экран из дампа виджетов (regex по классу)
    wait_widget 30000 AllocateStatButton ждать виджет (regex по class/label/id)
    click_widget AllocateStatButton      настоящий клик по центру виджета из дампа
    click_widget Выбрать.#0              клик по первому совпадению regex
    rclick_widget node:tide_bolt         правый клик (например, назначить на хотбар)
    widgets                              вывести текущие виджеты в журнал
    log текст                   записать в журнал

Команды *_widget читают run/gui-widgets.txt, который пишет клиентский
GuiWidgetDumper, пока лежит флаг run/aoe-gui-dump.flag. Так сценарий кликает
кнопки по реальным координатам, а не по зашитым пикселям.

Запуск:
    DISPLAY=:99 python3 tools/e2e/run_scenario.py tools/e2e/scenarios/boot.txt out/
"""

import os
import re
import subprocess
import sys
import time
import traceback

_ENV_PATTERN = re.compile(r"\$\{(\w+)(?::-([^}]*))?\}|\$(\w+)")


def expand_env(text):
    """Раскрывает $VAR и ${VAR:-default} так же, как оболочка.

    os.path.expandvars не знает про `${VAR:-default}`, а именно так в сценарии
    spells.txt задаётся цель прогона (AOE_AUDIT_TARGET). Без своей реализации во
    флаг-файле осталась бы строка целиком, и аудит молча шёл бы по всем умениям
    вместо выбранной школы.
    """
    def repl(match):
        name = match.group(1) or match.group(3)
        default = match.group(2)
        value = os.environ.get(name, "")
        if value:
            return value
        return default if default is not None else ""

    return _ENV_PATTERN.sub(repl, text)


REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
LOG = os.path.join(REPO, "run", "logs", "latest.log")


class Runner:
    def __init__(self, outdir):
        # Дисплей, на котором реально живёт клиент. Loom запускает runClient
        # через собственный `xvfb-run --auto-servernum`, поэтому это НЕ тот
        # дисплей, который поднимает run-in-xvfb.sh: его нужно найти перебором
        # сокетов /tmp/.X11-unix и проверкой, есть ли там окно Minecraft.
        self.display = os.environ.get("DISPLAY")
        # Xvfb, который поднимает Loom'овский xvfb-run, закрыт cookie-файлом:
        # без XAUTHORITY другие процессы получают 'unable to open display'.
        self.xauthority = os.environ.get("XAUTHORITY")
        self.outdir = outdir
        os.makedirs(outdir, exist_ok=True)
        self.journal = open(os.path.join(outdir, "steps.log"), "w", encoding="utf-8")
        self.shots = []
        self.failures = 0
        self.window = None
        self.log_mark_pos = 0
        self._snapshot_gradle_log()

    # ------------------------------------------------------------------ утилиты

    def note(self, message):
        line = "[%s] %s" % (time.strftime("%H:%M:%S"), message)
        print(line, flush=True)
        self.journal.write(line + "\n")
        self.journal.flush()

    def xenv(self, display=None):
        env = dict(os.environ)
        target = display or self.display
        if target:
            env["DISPLAY"] = target
        if self.xauthority:
            env["XAUTHORITY"] = self.xauthority
        return env

    def run(self, cmd, timeout=60, display=None):
        try:
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout,
                                    env=self.xenv(display))
            return result.returncode, (result.stdout or "") + (result.stderr or "")
        except FileNotFoundError:
            return 127, "нет команды: %s" % cmd[0]
        except subprocess.TimeoutExpired:
            return 124, "таймаут: %s" % " ".join(cmd)

    def fail(self, message):
        self.failures += 1
        self.note("ОШИБКА: " + message)

    # ------------------------------------------------------------------ окно

    def dump_threads(self, tag):
        """
        Снимок стека клиентского JVM. Без него невозможно понять, почему окно
        создано по логу, но в X его нет: видно, на чём стоит render thread
        (создание swapchain, ожидание события SDL, отрисовка кадра).
        """
        # pgrep -f находит и обёртку `/bin/sh /usr/bin/xvfb-run ... java ...`,
        # и сам JVM: нужен второй (в его командной строке есть bin/java).
        code, out = self.run(["pgrep", "-af", "devlaunchinjector"])
        pid = ""
        for line in out.splitlines():
            parts = line.split(" ", 1)
            if len(parts) == 2 and "bin/java" in parts[1] and not parts[1].startswith("/bin/sh"):
                pid = parts[0]
                break
        if not pid:
            self.note("jstack[%s]: JVM клиента не найден (pgrep: %s)" % (tag, out.strip()[:200]))
            return
        jstack = os.environ.get("JAVA_HOME", "")
        jstack = os.path.join(jstack, "bin", "jstack") if jstack else "jstack"
        code, out = self.run([jstack, "-l", pid], timeout=120)
        path = os.path.join(self.outdir, "jstack-%s.txt" % tag)
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(out)
        self.note("jstack[%s]: pid %s, %d строк -> %s" % (tag, pid, len(out.splitlines()), os.path.basename(path)))
        # короткая выжимка по render thread прямо в журнал
        lines = out.splitlines()
        for index, line in enumerate(lines):
            if "Render thread" in line:
                self.note("render thread:\n    " + "\n    ".join(l.strip() for l in lines[index:index + 12]))
                break

    def _snapshot_gradle_log(self):
        """Копия gradle.log в каталог снимков: публикация забирает только его,
        а именно там причина, когда JVM клиента не запустилась."""
        src = os.path.join(REPO, "run", "e2e", "gradle.log")
        if os.path.exists(src):
            try:
                import shutil
                shutil.copy(src, os.path.join(self.outdir, "gradle.log"))
            except OSError:
                pass

    def candidate_displays(self):
        displays = []
        try:
            for name in sorted(os.listdir("/tmp/.X11-unix")):
                if name.startswith("X") and name[1:].isdigit():
                    displays.append(":" + name[1:])
        except OSError:
            pass
        if os.environ.get("DISPLAY") and os.environ["DISPLAY"] not in displays:
            displays.insert(0, os.environ["DISPLAY"])
        return displays

    def client_pid(self):
        """PID JVM клиента (не обёртки /bin/sh xvfb-run)."""
        code, out = self.run(["pgrep", "-af", "devlaunchinjector"])
        for line in out.splitlines():
            parts = line.split(" ", 1)
            if len(parts) == 2 and "bin/java" in parts[1] and not parts[1].startswith("/bin/sh"):
                return parts[0]
        return ""

    def display_from_process(self):
        """
        Берёт DISPLAY прямо из окружения процесса клиента. Loom запускает
        runClient через `xvfb-run --auto-servernum`, который поднимает свой
        Xvfb с неизвестным номером дисплея — угадывать его перебором сокетов
        ненадёжно, а /proc/<pid>/environ знает точный ответ.
        """
        pid = self.client_pid()
        if not pid:
            return ""
        try:
            with open("/proc/%s/environ" % pid, "rb") as handle:
                blob = handle.read()
        except OSError:
            return ""
        found = ""
        for pair in blob.split(b"\0"):
            if pair.startswith(b"DISPLAY="):
                found = pair.decode("utf-8", "replace").split("=", 1)[1]
                if found and found != self.display:
                    self.note("дисплей клиента из /proc/%s/environ: %s (был %s)"
                              % (pid, found, self.display))
                self.display = found or self.display
            elif pair.startswith(b"XAUTHORITY="):
                auth = pair.decode("utf-8", "replace").split("=", 1)[1]
                if auth and auth != self.xauthority:
                    self.xauthority = auth
                    self.note("XAUTHORITY клиента: %s" % auth)
        return found

    def discover_display(self):
        """
        Ищет дисплей, на котором есть окно клиента. Возвращает True, если нашёл
        и запомнил (self.display); перебирает все X-сокеты.
        """
        for display in self.candidate_displays():
            for marker in (["--class", "Minecraft"], ["--name", "Minecraft"],
                           ["--class", "minecraft"]):
                code, out = self.run(["xdotool", "search"] + marker, display=display)
                ids = [line.strip() for line in out.splitlines() if line.strip().isdigit()]
                if ids:
                    if display != self.display:
                        self.note("клиент найден на дисплее %s (был %s)" % (display, self.display))
                    self.display = display
                    self.window = ids[-1]
                    return True
        return False

    def list_windows(self):
        """Диагностика: все окна с именем и геометрией (без WM их легко не найти)."""
        code, out = self.run(["xwininfo", "-root", "-tree"])
        self.note("дисплей: %s" % self.display)
        lines = [line.strip() for line in out.splitlines() if '"' in line or "xwininfo" in line]
        if lines:
            self.note("окна на дисплее:\n    " + "\n    ".join(lines[:15]))
        return lines

    def find_window(self):
        self.display_from_process()
        if not self.discover_display():
            return False
        code, name = self.run(["xdotool", "getwindowname", self.window])
        code, geo = self.run(["xdotool", "getwindowgeometry", "--shell", self.window])
        code, mapped = self.run(["xwininfo", "-id", self.window])
        state = ""
        for line in mapped.splitlines():
            if "Map State" in line:
                state = line.strip()
        self.note("окно %s на %s: %s | %s | %s" % (
            self.window, self.display, name.strip(), geo.strip().replace("\n", " "), state))
        return True

    def focus(self):
        if not self.window and not self.find_window():
            return False
        self.run(["xdotool", "windowraise", self.window], timeout=20)
        self.run(["xdotool", "windowactivate", "--sync", self.window], timeout=20)
        self.run(["xdotool", "windowfocus", "--sync", self.window], timeout=20)
        # wmctrl — второй путь: у openbox windowactivate через xdotool иногда
        # не срабатывает, а без фокуса SDL не отдаёт клавиатуру вообще
        # (мышь при этом работает, поэтому клики проходят).
        self.run(["wmctrl", "-i", "-a", str(self.window)], timeout=20)
        code, active = self.run(["xdotool", "getwindowfocus"])
        active = active.strip()
        if active != str(self.window):
            self.note("фокус на %s вместо %s — ещё одна попытка" % (active or "—", self.window))
            self.run(["xdotool", "windowfocus", "--sync", "--clearmodifiers", self.window], timeout=20)
            self.run(["xdotool", "windowraise", self.window], timeout=20)
            self.run(["wmctrl", "-i", "-a", str(self.window)], timeout=20)
            time.sleep(0.3)
            code, active = self.run(["xdotool", "getwindowfocus"])
            active = active.strip()
        if active != str(self.window):
            self.fail("не удалось перевести фокус на окно %s (сейчас %s)" % (self.window, active or "—"))
            return False
        return True

    def wait_window(self, pattern, timeout_ms):
        deadline = time.time() + timeout_ms / 1000.0
        last_note = 0.0
        # Клиент может умереть/не запуститься тихо — тогда ждать окно бессмысленно.
        # Отслеживаем появление JVM: за весь timeout ни разу не увидели → аварийный
        # выход с подсказкой (иначе три сценария подряд жгли по часу на пустоту).
        saw_jvm_deadline = time.time() + max(120000, timeout_ms) / 1000.0
        while time.time() < deadline:
            if self.find_window():
                return True
            if self.client_pid():
                saw_jvm_deadline = time.time() + timeout_ms / 1000.0
            elif time.time() > saw_jvm_deadline:
                self.fail("окно '%s' не появилось: JVM клиента так и не запустилась "
                          "(gradlew runClient упал? смотри run/e2e/gradle.log)" % pattern)
                self.list_windows()
                return False
            if time.time() - last_note > 30:
                last_note = time.time()
                self.note("жду окно: дисплей %s, X-сокеты: %s, JVM: %s"
                          % (self.display, ", ".join(self.candidate_displays()),
                             self.client_pid() or "нет"))
            time.sleep(2)
        self.fail("окно '%s' не появилось за %d мс" % (pattern, timeout_ms))
        self.list_windows()
        self.dump_threads("no-window")
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

    def wait_file(self, rel_path, timeout_ms):
        path = rel_path if os.path.isabs(rel_path) else os.path.join(REPO, rel_path)
        deadline = time.time() + timeout_ms / 1000.0
        while time.time() < deadline:
            if os.path.exists(path):
                self.note("появился файл %s" % rel_path)
                return True
            time.sleep(2)
        self.fail("файл %s не появился за %d мс" % (rel_path, timeout_ms))
        return False

    # ------------------------------------------------------------------ виджеты GUI

    def widgets_path(self):
        return os.path.join(REPO, "run", "gui-widgets.txt")

    def read_widgets(self):
        """Последнее состояние экрана из дампа клиента (run/gui-widgets.txt).

        Файл пишет GuiWidgetDumper внутри клиента, пока лежит флаг
        run/aoe-gui-dump.flag. Возвращает dict {screen, scale, widgets[]} или None.
        """
        import json
        try:
            with open(self.widgets_path(), "r", encoding="utf-8", errors="replace") as handle:
                lines = [l for l in handle.read().splitlines() if l.strip()]
            if not lines:
                return None
            return json.loads(lines[-1])
        except (OSError, ValueError):
            return None

    def widget_matches(self, widget, rx):
        """Виджет подходит, если regex совпал с классом, подписью или id."""
        for key in ("class", "label", "id"):
            value = widget.get(key) or ""
            if value and rx.search(value):
                return True
        return False

    def find_widgets(self, pattern):
        """Все виджеты, подходящие под regex, в порядке дампа."""
        rx = re.compile(pattern)
        state = self.read_widgets()
        if not state:
            return None, []
        return state, [w for w in state.get("widgets", []) if self.widget_matches(w, rx)]

    def wait_screen(self, pattern, timeout_ms):
        """Ждёт, пока откроется экран, чьё имя класса/заголовок матчит regex."""
        deadline = time.time() + timeout_ms / 1000.0
        rx = re.compile(pattern)
        while time.time() < deadline:
            state = self.read_widgets()
            if state and rx.search(state.get("screen", "") or ""):
                self.note("экран открыт: %s (виджетов %d)"
                          % (state.get("screen"), len(state.get("widgets", []))))
                return True
            time.sleep(0.5)
        self.fail("экран /%s/ не открылся за %d мс (дамп: %s)"
                  % (pattern, timeout_ms, self.widgets_path()))
        return False

    def wait_widget(self, pattern, timeout_ms):
        """Ждёт, пока на экране появится виджет, матчащий regex (class/label/id)."""
        deadline = time.time() + timeout_ms / 1000.0
        while time.time() < deadline:
            state, found = self.find_widgets(pattern)
            if found:
                self.note("виджет /%s/ найден: %d шт. (экран %s)"
                          % (pattern, len(found), (state or {}).get("screen")))
                return True
            time.sleep(0.5)
        self.fail("виджет /%s/ не появился за %d мс" % (pattern, timeout_ms))
        return False

    def click_widget(self, pattern, button=1):
        """Клик по центру виджета из дампа. pattern может нести #N — номер
        совпадения (с 0), по умолчанию 0. Координаты дампа — в gui-пикселях,
        домножаем на scale до оконных."""
        if "#" in pattern:
            pattern, _, idx = pattern.rpartition("#")
            index = int(idx)
        else:
            index = 0
        state, found = self.find_widgets(pattern)
        if not state:
            self.fail("клик по виджету /%s/: дампа нет (флаг run/aoe-gui-dump.flag стоит?)" % pattern)
            return False
        if not found:
            names = ["%s[%s]" % (w.get("class"), (w.get("label") or w.get("id") or "")[:30])
                     for w in state.get("widgets", [])]
            self.fail("клик по виджету /%s/: не найден. На экране %s: %s"
                      % (pattern, state.get("screen"), ", ".join(names)[:400]))
            return False
        if index >= len(found):
            self.fail("клик по виджету /%s#%d/: совпадений только %d" % (pattern, index, len(found)))
            return False
        widget = found[index]
        scale = float(state.get("scale") or 2.0)
        wx = int((widget["x"] + widget["w"] / 2) * scale)
        wy = int((widget["y"] + widget["h"] / 2) * scale)
        self.note("клик по виджету %s (%s %s) gui=(%d,%d %dx%d) окно=(%d,%d)"
                  % (widget.get("class"), widget.get("label") or widget.get("id") or "",
                     "активен" if widget.get("active", True) else "НЕАКТИВЕН",
                     widget["x"], widget["y"], widget["w"], widget["h"], wx, wy))
        self.click(wx, wy, button)
        return True

    def dump_widgets_note(self):
        """Пишет текущий список виджетов в журнал — диагностика сценария."""
        state = self.read_widgets()
        if not state:
            self.note("дампа виджетов нет (флаг run/aoe-gui-dump.flag стоит?)")
            return
        names = ["%s(%d,%d %dx%d) '%s'" % (
            w.get("class"), w.get("x", 0), w.get("y", 0), w.get("w", 0), w.get("h", 0),
            (w.get("label") or w.get("id") or "")[:40]) for w in state.get("widgets", [])]
        self.note("экран %s, scale %s, виджеты:\n    %s"
                  % (state.get("screen"), state.get("scale"), "\n    ".join(names) or "(пусто)"))

    def log_mark(self):
        """Отметка в логе: wait_log_new ищет только в строках ПОСЛЕ неё.

        Нужна, чтобы дождаться именно СЛЕДУЮЩЕГО прогона зонда: обычный
        wait_log находит строки первого прогона и возвращается мгновенно,
        из-за чего снимок выпадает не в тот момент игры.
        """
        try:
            self.log_mark_pos = os.path.getsize(LOG) if os.path.exists(LOG) else 0
        except OSError:
            self.log_mark_pos = 0
        self.note("отметка в логе: байт %d" % self.log_mark_pos)

    def wait_log_new(self, pattern, timeout_ms):
        """Как wait_log, но ищет только в строках, дописанных после log_mark."""
        deadline = time.time() + timeout_ms / 1000.0
        rx = re.compile(pattern)
        while time.time() < deadline:
            if os.path.exists(LOG):
                try:
                    with open(LOG, "r", encoding="utf-8", errors="replace") as handle:
                        handle.seek(self.log_mark_pos)
                        text = handle.read()
                    # Недописанная последняя строка — не матчим, дождёмся конца.
                    lines = text.splitlines()
                    if text and not text.endswith("\n"):
                        lines = lines[:-1]
                    for line in lines:
                        if rx.search(line):
                            self.note("в логе найдено (новое): " + line.strip()[:160])
                            return True
                except OSError:
                    pass
            # 0.3 с: строки зонда пишутся сразу после замера, окно ношения брони
            # ~8 с — мелкий шаг опроса нужен, чтобы кадр HUD успел в него попасть.
            time.sleep(0.3)
        self.fail("в логе нет нового /%s/ за %d мс" % (pattern, timeout_ms))
        return False

    # ------------------------------------------------------------------ снимок

    def shot(self, name):
        path = os.path.join(self.outdir, name + ".png")
        self.focus()
        # Сначала снимаем само окно клиента (без WM координаты и границы корня
        # могут не совпадать с окном), при неудаче — весь корень дисплея.
        out = ""
        if self.window:
            code, out = self.run(["import", "-window", self.window, "-silent", path], timeout=120)
            if code == 0 and os.path.exists(path) and os.path.getsize(path) > 5000:
                self.shots.append(name)
                self.note("снимок %s (окно %s, %d КБ)" % (name, self.window, os.path.getsize(path) // 1024))
                return True
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

    def window_origin(self):
        """Абсолютная позиция окна на экране (нужна для XTEST-координат)."""
        code, geo = self.run(["xdotool", "getwindowgeometry", "--shell", self.window])
        x = y = 0
        for line in geo.splitlines():
            if line.startswith("X="):
                x = int(line.split("=", 1)[1] or 0)
            elif line.startswith("Y="):
                y = int(line.split("=", 1)[1] or 0)
        return x, y

    def click(self, x, y, button=1):
        if not self.focus():
            self.fail("клик %s,%s: нет окна" % (x, y))
            return
        ox, oy = self.window_origin()
        # XTEST, а не `xdotool click --window`: синтетические события,
        # отправленные прямо в окно (XSendEvent), SDL3 игнорирует, поэтому
        # клик по кнопке меню не срабатывал. XTEST двигает настоящий курсор.
        self.run(["xdotool", "mousemove", str(ox + x), str(oy + y)])
        time.sleep(0.2)
        self.run(["xdotool", "click", str(button)])
        self.note("клик %s,%s (экран %s,%s, кнопка %s)" % (x, y, ox + x, oy + y, button))

    def move(self, x, y):
        if not self.focus():
            return
        ox, oy = self.window_origin()
        self.run(["xdotool", "mousemove", str(ox + x), str(oy + y)])
        self.note("курсор %s,%s" % (x, y))

    def hold(self, spec, millis):
        if not self.focus():
            self.fail("hold %s: нет окна" % spec)
            return
        code, before = self.run(["xdotool", "getwindowfocus"])
        self.run(["xdotool", "keydown", "--clearmodifiers", spec])
        time.sleep(millis / 1000.0)
        self.run(["xdotool", "keyup", "--clearmodifiers", spec])
        self.note("удержание %s %d мс (фокус: %s)" % (spec, millis, before.strip() or "—"))

    def key(self, spec):
        if not self.focus():
            self.fail("клавиша %s: нет окна" % spec)
            return
        # Клавиши — тоже через XTEST (без --window), иначе SDL3 их не увидит.
        self.run(["xdotool", "key", "--clearmodifiers", spec])
        self.note("клавиша " + spec)

    def type_text(self, text):
        if not self.focus():
            self.fail("ввод текста %r: нет окна" % text)
            return
        # Важно: это должна быть отдельная подкоманда `xdotool type`.
        # Передача "type --delay ..." как spec в `xdotool key` не вводит текст.
        self.run(["xdotool", "type", "--delay", "60", text])
        self.note("ввод текста " + text)

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
                elif command == "hold":
                    # hold <клавиша> <мс>: нажать, подержать, отпустить.
                    # Нужно для хоткеев, которые мод читает через isDown() в тике:
                    # мгновенный key (нажал-отпустил за 10 мс) тик не замечает.
                    # Последний токен — время удержания, всё до него — имя клавиши
                    # (в xdotool это может быть 'shift+a' или 'ctrl+alt+k').
                    parts = rest.rsplit(None, 1)
                    if len(parts) < 2:
                        self.fail("hold требует: <клавиша> <мс>")
                    else:
                        self.hold(parts[0].strip(), int(parts[1]))
                elif command == "keydown":
                    self.key("keydown " + rest.strip())
                elif command == "keyup":
                    self.key("keyup " + rest.strip())
                elif command == "type":
                    self.type_text(rest)
                elif command == "baritone_goal":
                    # Candidate is produced by the dev server; this sends a real player command.
                    import json
                    with open(os.path.join(REPO, "run", "airship-route-target.json"), encoding="utf-8") as handle:
                        goal=json.load(handle)
                    if goal.get("candidate"):
                        self.key("t")
                        time.sleep(.4)
                        self.type_text("#goto %d %d %d" % (goal["x"],goal["y"],goal["z"]))
                        self.key("Return")
                    else:
                        self.note("No candidate near " + str(goal.get("npc")))
                elif command == "baritone_click":
                    import json
                    with open(os.path.join(REPO, "run", "airship-route-target.json"), encoding="utf-8") as handle:
                        result=json.load(handle)
                    if result.get("click_ready"):
                        time.sleep(1)
                        self.click(640, 360, 3)
                        time.sleep(2)
                        self.shot("route-click-" + result["npc"])
                        self.key("Escape")
                        time.sleep(1)
                    else:
                        self.note("Interaction not attempted: player did not reach a suitable point")
                elif command == "wait":
                    time.sleep(int(args[0]) / 1000.0)
                elif command == "wait_window":
                    self.wait_window(args[0] if args else "Minecraft", int(args[1]) if len(args) > 1 else 120000)
                elif command == "wait_log":
                    # Формат: wait_log <таймаут_мс> <regex с пробелами...>
                    # Таймаут идёт первым именно потому, что regex почти всегда
                    # содержит пробелы ("Sound engine started") и делить его
                    # по пробелам нельзя.
                    if len(args) < 2:
                        self.fail("wait_log требует: <таймаут_мс> <regex>")
                    else:
                        self.wait_log(rest[len(args[0]):].strip(), int(args[0]))
                elif command == "log_mark":
                    self.log_mark()
                elif command == "wait_log_new":
                    # Как wait_log, но только строки после log_mark: ждём именно
                    # следующий прогон зонда, а не строки прошлого.
                    if len(args) < 2:
                        self.fail("wait_log_new требует: <таймаут_мс> <regex>")
                    else:
                        self.wait_log_new(rest[len(args[0]):].strip(), int(args[0]))
                elif command == "windows":
                    self.list_windows()
                elif command == "jstack":
                    self.dump_threads(rest.strip() or "dump")
                elif command == "wmctrl":
                    code, out = self.run(["wmctrl", "-l", "-p"])
                    self.note("wmctrl:\n    " + out.strip().replace("\n", "\n    "))
                elif command == "touch":
                    # touch <путь>: создать файл-триггер. Нужно для зонда
                    # CombatProbe — он стартует, увидев run/aoe-devtest.flag,
                    # и сам удаляет флаг после прогона. Так сценарий управляет
                    # моментом замера: снимок попадает в момент боя.
                    path = os.path.join(REPO, rest.strip())
                    os.makedirs(os.path.dirname(path), exist_ok=True)
                    with open(path, "a", encoding="utf-8"):
                        pass
                    self.note("создан файл-триггер " + rest.strip())
                elif command == "write":
                    # write <путь> <содержимое>: флаг-файл с параметрами,
                    # например `write run/aoe-spell-audit.flag all`.
                    bits = rest.split(None, 1)
                    if not bits:
                        self.fail("write требует: <путь> [содержимое]")
                    else:
                        path = os.path.join(REPO, bits[0])
                        os.makedirs(os.path.dirname(path) or REPO, exist_ok=True)
                        # ${VAR:-default} — чтобы цель прогона задавалась из CI.
                        content = expand_env(bits[1]) if len(bits) > 1 else ""
                        with open(path, "w", encoding="utf-8") as handle:
                            handle.write(content)
                        self.note("записан файл-триггер %s: %s"
                                  % (bits[0], bits[1] if len(bits) > 1 else "(пусто)"))
                elif command == "shell":
                    # shell <команда>: произвольная команда оболочки в окружении
                    # клиента (тот же DISPLAY/XAUTHORITY). Нужна для записи видео:
                    # ffmpeg должен снимать именно тот дисплей, где живёт окно.
                    if not rest.strip():
                        self.fail("shell требует команду")
                    else:
                        self.focus()
                        code, out = self.run(["bash", "-lc", rest.strip()], timeout=300,
                                             display=self.display)
                        self.note("shell[%s] -> %s\n    %s"
                                  % (rest.strip()[:80], code,
                                     (out or "").strip().replace("\n", "\n    ")[:1200]))
                        if code != 0:
                            self.fail("команда оболочки вернула %s: %s" % (code, rest.strip()[:120]))
                elif command == "wait_file":
                    bits = rest.split()
                    if len(bits) < 1:
                        self.fail("wait_file требует: <путь> [таймаут_мс]")
                    else:
                        self.wait_file(bits[0], int(bits[1]) if len(bits) > 1 else 600000)
                elif command == "wait_screen":
                    # wait_screen <таймаут_мс> <regex>: ждать экран из дампа виджетов.
                    if len(args) < 2:
                        self.fail("wait_screen требует: <таймаут_мс> <regex>")
                    else:
                        self.wait_screen(rest[len(args[0]):].strip(), int(args[0]))
                elif command == "wait_widget":
                    # wait_widget <таймаут_мс> <regex>: ждать виджет на экране.
                    if len(args) < 2:
                        self.fail("wait_widget требует: <таймаут_мс> <regex>")
                    else:
                        self.wait_widget(rest[len(args[0]):].strip(), int(args[0]))
                elif command == "click_widget":
                    # click_widget <regex>[#N]: настоящий клик мышью по центру
                    # виджета из дампа клиента (run/gui-widgets.txt).
                    if not rest.strip():
                        self.fail("click_widget требует: <regex>[#N]")
                    else:
                        self.click_widget(rest.strip())
                elif command == "rclick_widget":
                    # rclick_widget <regex>[#N]: правый клик по виджету
                    # (например, «ПКМ — назначить на панель заклинаний»).
                    if not rest.strip():
                        self.fail("rclick_widget требует: <regex>[#N]")
                    else:
                        self.click_widget(rest.strip(), button=3)
                elif command == "widgets":
                    # widgets: вывести текущий список виджетов в журнал.
                    self.dump_widgets_note()
                elif command == "log":
                    self.note(rest)
                else:
                    self.fail("неизвестная команда в строке %d: %s" % (number, line))
            except Exception:
                self.fail("строка %d (%s):\n%s" % (number, line, traceback.format_exc()))

            if self.failures and os.environ.get("E2E_FAIL_FAST") == "1":
                self.note("Остановка сценария после первой ошибки (E2E_FAIL_FAST)")
                break

        self.note("итог: снимков %d, ошибок %d" % (len(self.shots), self.failures))
        with open(os.path.join(self.outdir, "index.txt"), "w", encoding="utf-8") as handle:
            for name in self.shots:
                handle.write(name + "\n")
        # A scenario that produced screenshots but also recorded failures must
        # still fail the job. Previously `self.failures` was ignored entirely,
        # so a red E2E run was reported as green to CI.
        return 0 if self.shots and not self.failures else 1


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
        runner._snapshot_gradle_log()
        runner.journal.close()


if __name__ == "__main__":
    sys.exit(main())
