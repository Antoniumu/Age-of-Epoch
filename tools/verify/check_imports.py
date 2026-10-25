#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Ловит использование класса без импорта — то, что javac сообщает как
`cannot find symbol`.

Зачем: компиляция мода идёт на CI и занимает минуты, а ошибка вида «забыл
`import ...ParticleTypes`» обнаруживается только там. Этот скрипт проверяет тот
же класс ошибок локально за секунду: если в файле есть `Foo.BAR` (класс и
константа), а `Foo` ни импортирован, ни объявлен в этом же файле, ни лежит в
том же пакете — javac на этом упадёт.

Что НЕ считается ошибкой:
  * `import net.minecraft.world.item.*;` — звёздочный импорт покрывает класс;
  * `Foo` объявлен в самом файле (класс/enum/record/interface верхнего уровня);
  * рядом с файлом есть `Foo.java` в том же пакете;
  * совпадение внутри комментария или строки (комментарии вырезаются).

Запуск:
    python3 tools/verify/check_imports.py            # только свои исходники
    python3 tools/verify/check_imports.py --all      # вместе с вендорными
Выход: 0 — чисто, 1 — есть находки.
"""

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, "src/main/java")

PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
IMPORT_RE = re.compile(r"^\s*import\s+(?:static\s+)?([\w.*]+)\s*;", re.M)
# Класс + КОНСТАНТА: переменные так почти не пишут, поэтому ложных срабатываний мало.
USAGE_RE = re.compile(r"(?<![\w.$])([A-Z][\w]*)\.([A-Z][A-Z0-9_]{2,})")
TYPE_DECL_RE = re.compile(
    r"\b(?:class|interface|enum|record)\s+([A-Z][\w]*)"
)

# Типы из java.lang импортировать не нужно, и как «класс.константа» они выглядят
# ровно так же (Integer.MAX_VALUE).
JAVA_LANG = {
    "Integer", "Long", "Double", "Float", "Short", "Byte", "Boolean", "Character",
    "String", "Object", "Math", "Number", "System", "Class", "Thread", "Runtime",
    "StringBuilder", "Iterable", "Comparable", "Runnable", "Exception", "Error",
    "Throwable", "Override", "Deprecated", "SuppressWarnings", "Record",
}


def java_files(include_vendor=False):
    files = []
    for base, _, names in os.walk(SRC):
        if not include_vendor and os.sep + "library_of_exile" + os.sep in base + os.sep:
            continue
        if not include_vendor and os.sep + "forge_compat" + os.sep in base + os.sep:
            continue
        for name in names:
            if name.endswith(".java"):
                files.append(os.path.join(base, name))
    return sorted(files)


def strip_comments(text):
    """Убирает // и /* */ вместе со строками, чтобы не ловить совпадения в тексте."""
    out = []
    i = 0
    length = len(text)
    while i < length:
        ch = text[i]
        nxt = text[i + 1] if i + 1 < length else ""
        if ch == "/" and nxt == "/":
            while i < length and text[i] != "\n":
                i += 1
        elif ch == "/" and nxt == "*":
            i += 2
            while i + 1 < length and not (text[i] == "*" and text[i + 1] == "/"):
                i += 1
            i += 2
        elif ch == '"':
            i += 1
            while i < length and text[i] != '"':
                if text[i] == "\\":
                    i += 1
                i += 1
            i += 1
        else:
            out.append(ch)
            i += 1
    return "".join(out)


def package_of(text):
    match = PACKAGE_RE.search(text)
    return match.group(1) if match else ""


def declared_anywhere(files):
    """Все имена типов, объявленных в репозитории (включая вложенные enum/class).

    Вложенный тип родителя или реализуемого интерфейса доступен без импорта,
    поэтому такие имена не считаем ошибкой.
    """
    names = set()
    for path in files:
        text = strip_comments(open(path, encoding="utf-8", errors="replace").read())
        names.update(TYPE_DECL_RE.findall(text))
    return names


def known_imported_names(files):
    """Простые имена классов, которые где-то в проекте импортируются."""
    names = set()
    for path in files:
        text = open(path, encoding="utf-8", errors="replace").read()
        for match in IMPORT_RE.finditer(text):
            raw = match.group(1)
            if raw.endswith("*"):
                continue
            names.add(raw.split(".")[-1])
    return names - JAVA_LANG


def check(path, known_classes_by_package, declared, imported_somewhere):
    raw = open(path, encoding="utf-8", errors="replace").read()
    text = strip_comments(raw)

    imports = [m.group(1) for m in IMPORT_RE.finditer(text)]
    simple_imports = {i.split(".")[-1] for i in imports if not i.endswith("*")}
    wildcard = {i[:-2] for i in imports if i.endswith("*")}

    package = package_of(text)
    own_types = set(TYPE_DECL_RE.findall(text))

    # типы из того же пакета
    same_package = known_classes_by_package.get(package, set())

    problems = []
    reported = set()
    for match in USAGE_RE.finditer(text):
        name = match.group(1)
        if name in simple_imports or name in own_types or name in same_package:
            continue
        if wildcard:
            # звёздочный импорт может покрывать класс — не гадаем
            continue
        if name in declared:
            # вложенный тип (наследование/реализация интерфейса) — законно
            continue
        if name not in imported_somewhere:
            # не похоже на класс: это поле/переменная в верхнем регистре
            continue
        if (name, match.group(2)) in reported:
            continue
        reported.add((name, match.group(2)))
        line = text.count("\n", 0, match.start()) + 1
        problems.append("%s:%d: %s — нет импорта" % (
            os.path.relpath(path, ROOT), line, match.group(0)))
    return problems


def known_packages(files):
    """{пакет: {имена классов из этого пакета}} по фактическим файлам."""
    result = {}
    for path in files:
        name = os.path.basename(path)[:-5]
        text = open(path, encoding="utf-8", errors="replace").read(4000)
        match = PACKAGE_RE.search(text)
        if not match:
            continue
        result.setdefault(match.group(1), set()).add(name)
    return result


def main():
    include_vendor = "--all" in sys.argv
    files = java_files(include_vendor)
    known = known_packages(files)
    declared = declared_anywhere(files)
    imported_somewhere = known_imported_names(files)

    all_problems = []
    for path in files:
        all_problems.extend(check(path, known, declared, imported_somewhere))

    for problem in all_problems:
        print(problem)

    print("\nПроверено файлов: %d, находок: %d" % (len(files), len(all_problems)))
    if all_problems:
        print("ПОХОЖЕ НА ОШИБКУ КОМПИЛЯЦИИ: добавь недостающий импорт.")
        return 1
    print("ОК: все `Класс.КОНСТАНТА` имеют импорт.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
