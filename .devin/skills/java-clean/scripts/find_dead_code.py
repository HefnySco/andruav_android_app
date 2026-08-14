#!/usr/bin/env python3
"""Find commented-out dead code in Java files.

Usage:
    python3 find_dead_code.py <java_source_dir> [output_json]

Scans .java files for lines that are commented-out executable code (not
explanatory comments). Heuristics:
  - Line starts with // (after whitespace)
  - Content after // looks like a code statement:
      function call:  identifier(args)
      assignment:     identifier = ...
      method chain:   obj.method(...)
      keyword start:  if/for/while/switch/return/try/catch/...
      Log/println debug calls
      EventBus posts
      db.close() / socket operations

Does NOT flag:
  - Comments starting with words like "TODO", "FIXME", "NOTE", "HACK"
  - Comments that are sentences (start with uppercase word followed by space)
  - Javadoc/block comments (/* ... */)

Output: JSON with total_matches and per-file line details.
Review the output manually before removing — false positives are possible.
"""
import os
import re
import json
import sys

# Patterns that strongly indicate commented-out CODE (not prose)
CODE_PATTERNS = [
    r'\w+\s*\(',            # function call: foo( or foo.bar(
    r'\w+\s*=\s*',          # assignment: x = ...
    r'\bif\s*\(',
    r'\bfor\s*\(',
    r'\bwhile\s*\(',
    r'\bswitch\s*\(',
    r'\breturn\b',
    r'\btry\s*[\{]',
    r'\bcatch\s*\(',
    r'\belse\b',
    r'\bbreak\b',
    r'\bcontinue\b',
    r'\bnew\s+\w+',
    r'\}\s*else',
    r'\{',
    r'\}',
    r'Log\.\w+\(',
    r'System\.out\.print',
    r'EventBus\.getDefault\(\)',
    r'\.close\(\)',
    r'\.post\(',
    r'\.sendToTarget\(',
    r'//\s*\}',            # commented-out closing brace
    r'//\s*\{',            # commented-out opening brace
    r'//\s*set\w+\s*\(',   # commented-out setter calls
    r'//\s*mSocketState',
    r'//\s*socketState',
]

CODE_RE = re.compile('|'.join(CODE_PATTERNS))

# Skip lines that look like prose/rationale comments
PROSE_RE = re.compile(
    r'//\s*'
    r'(TODO|FIXME|NOTE|HACK|XXX|WARNING|IMPORTANT|BUG|OPTIMIZE|REVIEW|SAFETY'
    r'|NOT NEEDED|ALL Vehicles|this is different|should be|maybe|potential bug'
    r'|param\d?|opt\))',
    re.IGNORECASE
)

# A sentence: starts with capital letter, has a space and more words, no semicolons/parens
SENTENCE_RE = re.compile(r'//\s*[A-Z][a-z]+\s+[a-z]')


def is_dead_code(line):
    stripped = line.strip()
    if not stripped.startswith('//'):
        return False
    # Block comment markers
    if stripped.startswith('/*') or stripped.startswith('*/'):
        return False
    content = stripped[2:].strip()  # after //
    if not content:
        return False
    # Skip prose-like comments
    if PROSE_RE.match(stripped):
        return False
    if SENTENCE_RE.match(stripped) and ';' not in content and '(' not in content:
        return False
    # Check code patterns
    if CODE_RE.search(content):
        return True
    return False


def scan_file(path):
    with open(path, 'r', encoding='utf-8', errors='replace') as f:
        lines = f.readlines()
    matches = []
    for i, line in enumerate(lines):
        if is_dead_code(line):
            matches.append({'line': i + 1, 'content': line.rstrip('\n')})
    return matches


def main():
    if len(sys.argv) < 2:
        print("Usage: find_dead_code.py <java_source_dir> [output_json]")
        sys.exit(1)
    root = sys.argv[1]
    out_path = sys.argv[2] if len(sys.argv) > 2 else None

    results = {}
    total = 0
    for dirpath, dirnames, filenames in os.walk(root):
        for fn in filenames:
            if fn.endswith('.java'):
                fp = os.path.join(dirpath, fn)
                matches = scan_file(fp)
                if matches:
                    results[fp] = matches
                    total += len(matches)
    output = json.dumps({'total_matches': total, 'files_affected': len(results),
                         'details': results}, indent=2)
    if out_path:
        with open(out_path, 'w') as f:
            f.write(output)
        print(f"Report written to {out_path}: {total} dead code line(s) in {len(results)} file(s)")
    else:
        print(output)


if __name__ == '__main__':
    main()
