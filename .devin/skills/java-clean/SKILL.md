---
name: java-clean
description: Clean Java modules — remove unused imports and dead commented-out code. CRLF-safe.
argument-hint: "<java_source_dir>"
allowed-tools:
  - read
  - edit
  - grep
  - glob
  - exec
---

# Java Code Cleaning

Clean a Java source directory by removing unused imports and dead commented-out code.

## Scripts

All scripts live in `.devin/skills/java-clean/scripts/` and are run with `python3`.

## Workflow

The user provides a Java source directory (e.g. `andruavProtocol/src/main/java`).
If no argument is given, ask the user which directory to clean.

### Step 1: Find unused imports

```bash
python3 .devin/skills/java-clean/scripts/find_unused_imports.py <java_source_dir> /tmp/unused_imports_report.json
```

Review the report. Verify a few detections manually with `grep` before removing.
The script flags imports whose simple class name does not appear in the file body.
Wildcards (`com.foo.*`) are skipped. False positives are rare but possible if a
class name appears only in a string literal or annotation.

### Step 2: Remove unused imports

```bash
python3 .devin/skills/java-clean/scripts/remove_unused_imports.py /tmp/unused_imports_report.json
```

This preserves original line endings (CRLF-safe). It verifies each line is an
import statement before deleting.

### Step 3: Find dead commented-out code

```bash
python3 .devin/skills/java-clean/scripts/find_dead_code.py <java_source_dir> /tmp/dead_code_report.json
```

This detects `//`-commented lines that look like executable code (function calls,
assignments, Log/println debug statements, EventBus posts, if/for/while blocks,
etc.). It skips explanatory comments, TODO/FIXME/NOTE markers, and sentence-like
prose comments.

**IMPORTANT:** Review every match manually before removing. The heuristic can
produce false positives. Read the surrounding context to confirm each line is
truly dead code and not a design-rationale comment that happens to contain
parentheses or code-like syntax.

### Step 4: Remove dead code

After reviewing the dead code report, build a removal manifest JSON file with
the confirmed line numbers:

```json
{
  "/absolute/path/to/File.java": [157, 196, 216],
  ...
}
```

Then run:

```bash
python3 .devin/skills/java-clean/scripts/remove_lines.py /tmp/removal_manifest.json
```

This removes the specified lines. By default it refuses to remove non-comment
lines (safety check). Use `--force` only if you are certain.

**Line number caveat:** If a file has BOTH unused imports and dead code removed,
the dead code line numbers shift after import removal. Either:
- Apply both removals together using `remove_lines.py` with a combined manifest, OR
- Find dead code line numbers AFTER removing imports.

### Step 5: Verify build

Run the project's compile task to verify nothing broke:

```bash
./gradlew :<module>:compileDebugJavaWithJavac --no-daemon -q
```

### Step 6: Commit

Commit all changes. Per project rules: never add Co-Authored-By or AI tool
attribution to commit messages.

## What to preserve

- Explanatory / design-rationale comments (even if they mention code)
- TODO / FIXME / NOTE / HACK markers
- Javadoc and block comments
- Empty stub classes that are referenced by other modules (check with grep
  across the whole project before deleting any file)
- Original line endings (CRLF must be preserved — all scripts use `newline=''`)
