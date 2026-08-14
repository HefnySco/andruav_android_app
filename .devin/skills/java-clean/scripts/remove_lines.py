#!/usr/bin/env python3
"""Remove specific lines from Java files based on a removal manifest.

Usage:
    python3 remove_lines.py <manifest_json>

The manifest JSON format:
{
  "/absolute/path/to/File.java": [line1, line2, ...],
  ...
}

Line numbers are 1-based from the CURRENT (unmodified) file.
All lines are removed at once (set-based) to avoid shift issues.
Preserves original line endings (CRLF-safe) via newline='' mode.

Before removing, verifies each line is a comment (starts with //) to
prevent accidental deletion of live code. Use --force to skip this check.
"""
import json
import sys


def main():
    args = sys.argv[1:]
    force = False
    if '--force' in args:
        force = True
        args.remove('--force')
    if not args:
        print("Usage: remove_lines.py <manifest_json> [--force]")
        sys.exit(1)
    manifest_path = args[0]
    with open(manifest_path) as f:
        manifest = json.load(f)

    total_removed = 0
    for fp, line_numbers in manifest.items():
        with open(fp, 'r', encoding='utf-8', newline='') as fh:
            lines = fh.readlines()
        idx_to_remove = set()
        for ln in line_numbers:
            i = ln - 1
            if i < 0 or i >= len(lines):
                print(f"WARNING: {fp} line {ln} out of range")
                continue
            if not force:
                stripped = lines[i].lstrip()
                if not stripped.startswith('//'):
                    print(f"WARNING: {fp} line {ln} is NOT a comment: {lines[i].rstrip()!r}")
                    print("  Use --force to remove anyway. Skipping.")
                    continue
            idx_to_remove.add(i)
        new_lines = [l for i, l in enumerate(lines) if i not in idx_to_remove]
        with open(fp, 'w', encoding='utf-8', newline='') as fh:
            fh.writelines(new_lines)
        removed = len(lines) - len(new_lines)
        if removed:
            print(f"{fp}: removed {removed} line(s)")
            total_removed += removed
    print(f"\nTotal lines removed: {total_removed}")


if __name__ == '__main__':
    main()
