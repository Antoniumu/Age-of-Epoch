#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Синтаксическая проверка Java-исходников (tree-sitter-java, понимает современный синтаксис).

Сборка Gradle в этой среде недоступна (нет доступа к maven Mojang/Fabric), поэтому компиляцию
заменяет разбор каждого файла в AST: ищем узлы ERROR / MISSING — это синтаксические ошибки.

Запуск:
  python3 tools/check_java_syntax.py                # только изменённые файлы (git status)
  python3 tools/check_java_syntax.py --all          # весь src/main/java
  python3 tools/check_java_syntax.py path/to/A.java # конкретные файлы
"""

import os
import subprocess
import sys

from tree_sitter import Language, Parser
import tree_sitter_java

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def java_files():
    args = sys.argv[1:]

    if "--all" in args:
        for root, _, files in os.walk(os.path.join(REPO, "src", "main", "java")):
            for name in sorted(files):
                if name.endswith(".java"):
                    yield os.path.join(root, name)
        return

    explicit = [a for a in args if not a.startswith("--")]
    if explicit:
        for a in explicit:
            yield a if os.path.isabs(a) else os.path.join(REPO, a)
        return

    out = subprocess.check_output(["git", "status", "--porcelain"], cwd=REPO, text=True)
    for line in out.splitlines():
        path = line[3:].strip()
        if path.endswith(".java"):
            yield os.path.join(REPO, path)


def problems(parser, path):
    src = open(path, "rb").read()
    tree = parser.parse(src)

    found = []
    stack = [tree.root_node]
    while stack:
        node = stack.pop()
        if node.type == "ERROR" or node.is_missing:
            line = node.start_point[0] + 1
            snippet = src.splitlines()[node.start_point[0]].decode("utf-8", "replace").strip()
            found.append("строка %d: %s | %s" % (line, node.type, snippet[:90]))
        stack.extend(node.children)
    return found


def main():
    parser = Parser(Language(tree_sitter_java.language()))

    files = list(java_files())
    bad = 0

    for path in files:
        if not os.path.exists(path):
            print("НЕТ ФАЙЛА: %s" % path)
            bad += 1
            continue

        issues = problems(parser, path)
        if issues:
            bad += 1
            print("ОШИБКА: %s" % os.path.relpath(path, REPO))
            for i in issues[:5]:
                print("    " + i)

    print("Проверено файлов: %d, с ошибками: %d" % (len(files), bad))
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
