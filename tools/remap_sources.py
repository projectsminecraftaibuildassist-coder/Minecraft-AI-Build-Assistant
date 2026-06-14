#!/usr/bin/env python3
"""Remap intermediary Minecraft names in Java sources to Yarn (named) mappings."""

from __future__ import annotations

import re
import sys
from pathlib import Path

TINY = Path(".gradle/loom-cache/source_mappings").glob("*.tiny")
CLASS_PATTERN = re.compile(r"\bclass_\d+(?:\$class_\d+)*\b")
METHOD_PATTERN = re.compile(r"\bmethod_\d+\b")
FIELD_PATTERN = re.compile(r"\bfield_\d+\b")


def parse_tiny(path: Path) -> tuple[dict[str, str], dict[str, str], dict[str, str]]:
    classes: dict[str, str] = {}
    methods: dict[str, str] = {}
    fields: dict[str, str] = {}
    current_class: str | None = None

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("tiny\t"):
            continue
        parts = line.split("\t")
        kind = parts[0]
        if kind == "c" and len(parts) >= 4:
            named = parts[1]
            intermediary = parts[3]
            key = intermediary.rsplit("/", 1)[-1]
            simple = named.rsplit("/", 1)[-1]
            pkg = named.rsplit("/", 1)[0].replace("/", ".")
            classes[key] = f"{pkg}.{simple}"
            current_class = key
        elif kind == "m" and len(parts) >= 5:
            named = parts[2]
            intermediary = parts[4]
            if intermediary.startswith("method_"):
                methods[intermediary] = named
        elif kind == "f" and len(parts) >= 5 and current_class:
            named = parts[2]
            intermediary = parts[4]
            if intermediary.startswith("field_"):
                fields[intermediary] = named

    return classes, methods, fields


def remap_file(text: str, classes: dict[str, str], methods: dict[str, str], fields: dict[str, str]) -> str:
    used_types: set[str] = set()

    def class_repl(match: re.Match[str]) -> str:
        key = match.group(0)
        fqcn = classes.get(key)
        if not fqcn:
            return key
        used_types.add(fqcn)
        return fqcn.rsplit(".", 1)[-1]

    text = CLASS_PATTERN.sub(class_repl, text)
    text = METHOD_PATTERN.sub(lambda m: methods.get(m.group(0), m.group(0)), text)
    text = FIELD_PATTERN.sub(lambda m: fields.get(m.group(0), m.group(0)), text)

    lines = text.splitlines(keepends=True)
    out: list[str] = []
    package_end = 0
    for i, line in enumerate(lines):
        out.append(line)
        if line.startswith("package "):
            package_end = i + 1

    filtered: list[str] = []
    for line in out:
        if line.startswith("import net.minecraft."):
            continue
        filtered.append(line)

    import_lines = [f"import {fqcn};\n" for fqcn in sorted(used_types)]
    if import_lines:
        filtered[package_end:package_end] = ["\n", *import_lines]

    return "".join(filtered)


def main() -> int:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    tiny_files = list((root / ".gradle/loom-cache/source_mappings").glob("*.tiny"))
    if not tiny_files:
        print("No tiny mappings found", file=sys.stderr)
        return 1

    classes, methods, fields = parse_tiny(tiny_files[0])
    src = root / "src"
    for path in src.rglob("*.java"):
        original = path.read_text(encoding="utf-8")
        remapped = remap_file(original, classes, methods, fields)
        path.write_text(remapped, encoding="utf-8")
        print(f"Remapped {path.relative_to(root)}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
