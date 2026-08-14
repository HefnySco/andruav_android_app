#!/usr/bin/env python3
"""Remove unused imports identified by find_unused_imports.py.

Usage:
    python3 remove_unused_imports.py <report_json>

Reads the JSON report produced by find_unused_imports.py and deletes the
flagged import lines from each file. Preserves original line endings by
using newline='' mode (important for CRLF files).

Before removing, verifies each line is actually an import statement.
"""
import json
import sys


def main():
    if len(sys.argv) < 2:
        print("Usage: remove_unused_imports.py <report_json>")
        sys.exit(1)
    report_path = sys.argv[1]
    with open(report_path) as f:
        data = json.load(f)

    total_removed = 0
    for fp, items in data['details'].items():
        with open(fp, 'r', encoding='utf-8', newline='') as fh:
            lines = fh.readlines()
        to_delete = set()
        for it in items:
            idx = it['line'] - 1
            if idx < 0 or idx >= len(lines):
                print(f"WARNING: {fp} line {it['line']} out of range")
                continue
            stripped = lines[idx].lstrip()
            if not stripped.startswith('import '):
                print(f"WARNING: {fp} line {it['line']} is not an import: {lines[idx].rstrip()!r}")
                continue
            to_delete.add(idx)
        new_lines = [l for i, l in enumerate(lines) if i not in to_delete]
        with open(fp, 'w', encoding='utf-8', newline='') as fh:
            fh.writelines(new_lines)
        removed = len(lines) - len(new_lines)
        if removed:
            print(f"Removed {removed} import(s) from {fp}")
            total_removed += removed
    print(f"\nTotal unused imports removed: {total_removed}")


if __name__ == '__main__':
    main()
