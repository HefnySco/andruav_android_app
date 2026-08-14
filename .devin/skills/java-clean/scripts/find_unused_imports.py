#!/usr/bin/env python3
"""Detect unused imports in Java files.

Usage:
    python3 find_unused_imports.py <java_source_dir> [output_json]

Scans all .java files under <java_source_dir> and reports imports whose
simple class name does not appear anywhere in the file body (after the
import block). Wildcard imports (com.foo.*) are skipped.

Output: JSON with total_unused, files_affected, and per-file details
including line numbers and the full import statement. If output_json is
omitted, writes to stdout.
"""
import os
import re
import json
import sys

IMPORT_RE = re.compile(r'^\s*import\s+(static\s+)?([\w.]+(?:\.\*)?);')


def simple_name(fqn):
    return fqn.split('.')[-1]


def find_unused_in_file(path):
    with open(path, 'r', encoding='utf-8', errors='replace') as f:
        lines = f.readlines()
    imports = []
    for i, line in enumerate(lines):
        m = IMPORT_RE.match(line)
        if m:
            is_static = m.group(1) is not None
            fqn = m.group(2)
            sn = simple_name(fqn)
            imports.append((i + 1, line.rstrip('\n'), is_static, fqn, sn))
    if imports:
        last_import_line = max(imp[0] for imp in imports)
        body = ''.join(lines[last_import_line:])
    else:
        body = ''.join(lines)
    unused = []
    for (lineno, full_line, is_static, fqn, sn) in imports:
        if sn == '*':
            continue
        pattern = r'\b' + re.escape(sn) + r'\b'
        if re.search(pattern, body):
            continue
        unused.append({'line': lineno, 'import': full_line.strip(),
                       'fqn': fqn, 'simple': sn})
    return unused


def main():
    if len(sys.argv) < 2:
        print("Usage: find_unused_imports.py <java_source_dir> [output_json]")
        sys.exit(1)
    root = sys.argv[1]
    out_path = sys.argv[2] if len(sys.argv) > 2 else None

    results = {}
    total = 0
    for dirpath, dirnames, filenames in os.walk(root):
        for fn in filenames:
            if fn.endswith('.java'):
                fp = os.path.join(dirpath, fn)
                unused = find_unused_in_file(fp)
                if unused:
                    results[fp] = unused
                    total += len(unused)
    output = json.dumps({'total_unused': total, 'files_affected': len(results),
                         'details': results}, indent=2)
    if out_path:
        with open(out_path, 'w') as f:
            f.write(output)
        print(f"Report written to {out_path}: {total} unused import(s) in {len(results)} file(s)")
    else:
        print(output)


if __name__ == '__main__':
    main()
