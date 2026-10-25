#!/usr/bin/env bash
# sync_sources.sh — copy the pinned upstream DroneEngage module sources into
# deNative/external/ (decision N5: lock file + sync script, no submodules —
# the upstream repos are private).
#
#   deNative/sync_sources.sh          sync deNative/external/ to sources.lock
#   deNative/sync_sources.sh --check  exit 1 if external/.synced != sources.lock
#
# DE_WORKSPACE (env) points at the folder that holds the upstream checkouts.
# Default: ../../drone_engage relative to the repo root; falls back to the
# repo's parent dir (the de_code sibling layout on this machine).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOCK_FILE="$SCRIPT_DIR/sources.lock"
EXTERNAL_DIR="$SCRIPT_DIR/external"
SYNCED_FILE="$EXTERNAL_DIR/.synced"

die() { echo "ERROR: $*" >&2; exit 1; }

[ -f "$LOCK_FILE" ] || die "lock file not found: $LOCK_FILE"

mode_sync() {
    # Locate the workspace holding the upstream checkouts.
    local candidates=()
    if [ -n "${DE_WORKSPACE:-}" ]; then
        candidates+=("$DE_WORKSPACE")
    else
        candidates+=("$REPO_ROOT/../../drone_engage" "$REPO_ROOT/..")
    fi

    while IFS= read -r line; do
        line="${line%%#*}"                      # strip comments
        line="$(echo "$line" | xargs)"          # trim
        [ -z "$line" ] && continue

        local repo sha
        repo="$(echo "$line" | awk '{print $1}')"
        sha="$(echo "$line" | awk '{print $2}')"
        [ -n "$repo" ] && [ -n "$sha" ] || die "bad lock line: '$line' (want '<repo> <full sha>')"

        # Resolve the local checkout (canonical name first, then aliases).
        local checkout=""
        local names=("$repo")
        [ "$repo" = "droneengage_communication_pro" ] && names+=("droneengage_communication")
        for ws in "${candidates[@]}"; do
            for name in "${names[@]}"; do
                if [ -d "$ws/$name/.git" ] || git -C "$ws/$name" rev-parse --git-dir >/dev/null 2>&1; then
                    checkout="$(cd "$ws/$name" && pwd)"
                    break 2
                fi
            done
        done
        [ -n "$checkout" ] || die "checkout for '$repo' not found (looked in: ${candidates[*]}). Set DE_WORKSPACE."
        git -C "$checkout" cat-file -e "$sha^{commit}" 2>/dev/null \
            || die "commit $sha not found in $checkout"

        echo ">>> syncing $repo @ ${sha:0:8} from $checkout"
        local dest="$EXTERNAL_DIR/$repo"
        rm -rf "$dest"
        mkdir -p "$dest"
        git -C "$checkout" archive "$sha" | tar -x -C "$dest"

        # Populate the pinned src/de_common submodule content, if any.
        local sub
        sub="$(git -C "$checkout" ls-tree "$sha" src/de_common | awk '{print $3}')"
        if [ -n "$sub" ]; then
            git -C "$checkout/src/de_common" cat-file -e "$sub^{commit}" 2>/dev/null \
                || die "de_common commit $sub not found in $checkout/src/de_common"
            echo ">>>   src/de_common @ ${sub:0:8}"
            mkdir -p "$dest/src/de_common"
            git -C "$checkout/src/de_common" archive "$sub" | tar -x -C "$dest/src/de_common"
        fi
    done < "$LOCK_FILE"

    cp "$LOCK_FILE" "$SYNCED_FILE"
    echo ">>> synced -> $EXTERNAL_DIR (.synced written)"
}

mode_check() {
    if [ ! -f "$SYNCED_FILE" ]; then
        echo "deNative/external/ is not synced — run deNative/sync_sources.sh" >&2
        exit 1
    fi
    if ! cmp -s "$LOCK_FILE" "$SYNCED_FILE"; then
        echo "deNative/external/ is out of date vs sources.lock — run deNative/sync_sources.sh" >&2
        exit 1
    fi
}

case "${1:-}" in
    --check) mode_check ;;
    "")      mode_sync ;;
    *)       die "unknown argument '$1' (usage: sync_sources.sh [--check])" ;;
esac
