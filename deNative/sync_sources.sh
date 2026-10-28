#!/usr/bin/env bash
# sync_sources.sh — copy the pinned upstream DroneEngage module sources into
# deNative/external/ (decision N5: lock file + sync script, no submodules —
# the upstream repos are private).
#
#   deNative/sync_sources.sh              sync deNative/external/ to sources.lock
#   deNative/sync_sources.sh --check      exit 1 if external/.synced != sources.lock;
#                                         warn (never fail) when a pin drifts from master
#   deNative/sync_sources.sh --bump [repo...]
#                                         move pins to the local checkout's master,
#                                         tag them and re-sync (default: all repos)
#   deNative/sync_sources.sh --tag        create the andruav-pin-<sha8> tags locally
#   deNative/sync_sources.sh --push-tags  push those tags to the lock's URLs
#
# Each pin is protected by the tag andruav-pin-<first 8 chars of sha>, in the
# module repo and in de_common (for the src/de_common gitlink). Pushing the tag
# keeps the commit reachable even if its branch is deleted or rebased.
#
# Sources are taken from a local checkout when it has the pinned commit,
# otherwise the tag is fetched from the lock's URL into a cache.
#
# DE_WORKSPACE (env) points at the folder that holds the upstream checkouts.
# Default: ../../drone_engage relative to the repo root; falls back to the
# repo's parent dir (the de_code sibling layout on this machine).
# DE_SRC_CACHE (env) is the fetch cache (default ~/.cache/andruav_denative).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOCK_FILE="$SCRIPT_DIR/sources.lock"
EXTERNAL_DIR="$SCRIPT_DIR/external"
SYNCED_FILE="$EXTERNAL_DIR/.synced"
CACHE_DIR="${DE_SRC_CACHE:-$HOME/.cache/andruav_denative}"

die()  { echo "ERROR: $*" >&2; exit 1; }
warn() { echo "WARNING: $*" >&2; }
pin_tag() { echo "andruav-pin-${1:0:8}"; }

[ -f "$LOCK_FILE" ] || die "lock file not found: $LOCK_FILE"

# ---- lock parsing ------------------------------------------------------------
# Module lines: <repo> <url> <full sha>
# One line:     de_common <url>   (source of each module's src/de_common gitlink)
PIN_NAMES=(); PIN_URLS=(); PIN_SHAS=(); DE_COMMON_URL=""
while IFS= read -r line; do
    line="${line%%#*}"
    read -r -a f <<< "$line" || true
    [ "${#f[@]}" -eq 0 ] && continue
    if [ "${f[0]}" = "de_common" ]; then
        [ "${#f[@]}" -eq 2 ] || die "bad lock line: '$line' (want 'de_common <url>')"
        DE_COMMON_URL="${f[1]}"
        continue
    fi
    [ "${#f[@]}" -eq 3 ] || die "bad lock line: '$line' (want '<repo> <url> <full sha>')"
    [[ "${f[2]}" =~ ^[0-9a-f]{40}$ ]] || die "bad sha in lock line: '$line'"
    PIN_NAMES+=("${f[0]}"); PIN_URLS+=("${f[1]}"); PIN_SHAS+=("${f[2]}")
done < "$LOCK_FILE"
[ "${#PIN_NAMES[@]}" -gt 0 ] || die "no module pins in $LOCK_FILE"

# ---- local checkouts ---------------------------------------------------------
WORKSPACES=()
if [ -n "${DE_WORKSPACE:-}" ]; then
    WORKSPACES+=("$DE_WORKSPACE")
else
    WORKSPACES+=("$REPO_ROOT/../../drone_engage" "$REPO_ROOT/..")
fi

is_git() { git -C "$1" rev-parse --git-dir >/dev/null 2>&1; }
has_commit() { git -C "$1" cat-file -e "$2^{commit}" 2>/dev/null; }

# find_checkout <repo> -> path of the local checkout, or empty.
find_checkout() {
    local names=("$1")
    [ "$1" = "droneengage_communication_pro" ] && names+=("droneengage_communication")
    local ws name
    for ws in "${WORKSPACES[@]}"; do
        for name in "${names[@]}"; do
            if [ -d "$ws/$name" ] && is_git "$ws/$name"; then
                (cd "$ws/$name" && pwd); return
            fi
        done
    done
}

# de_common_checkouts <module checkout or empty> -> candidate de_common repos,
# the workspace's own de_common first (that is where pin tags are created).
de_common_checkouts() {
    local ws
    for ws in "${WORKSPACES[@]}"; do
        [ -d "$ws/de_common" ] && is_git "$ws/de_common" && (cd "$ws/de_common" && pwd)
    done
    [ -n "$1" ] && [ -d "$1/src/de_common" ] && is_git "$1/src/de_common" && echo "$1/src/de_common"
    return 0
}

# fetch_cached <cache name> <url> <sha> -> bare cache repo holding <sha>.
fetch_cached() {
    local name="$1" url="$2" sha="$3" tag
    tag="$(pin_tag "$sha")"
    local repo="$CACHE_DIR/$name.git"
    [ -d "$repo" ] || git init -q --bare "$repo"
    if ! has_commit "$repo" "$sha"; then
        echo ">>>   fetching $tag from $url" >&2
        git -C "$repo" fetch -q --depth 1 "$url" "+refs/tags/$tag:refs/tags/$tag" 2>/dev/null \
            || git -C "$repo" fetch -q --depth 1 "$url" "$sha" 2>/dev/null \
            || die "cannot fetch $tag ($sha) from $url — no access, or the tag was never pushed (sync_sources.sh --push-tags)"
    fi
    has_commit "$repo" "$sha" || die "$url: $tag does not resolve to $sha"
    if git -C "$repo" rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
        [ "$(git -C "$repo" rev-parse "$tag^{commit}")" = "$sha" ] \
            || die "$url: tag $tag points at $(git -C "$repo" rev-parse "$tag^{commit}"), lock says $sha"
    fi
    echo "$repo"
}

# resolve_module <i> -> git repo holding the pinned module commit.
resolve_module() {
    local name="${PIN_NAMES[$1]}" url="${PIN_URLS[$1]}" sha="${PIN_SHAS[$1]}" co
    co="$(find_checkout "$name")"
    if [ -n "$co" ] && has_commit "$co" "$sha"; then echo "$co"; return; fi
    fetch_cached "$name" "$url" "$sha"
}

# resolve_de_common <module checkout or empty> <sha> -> git repo holding <sha>.
resolve_de_common() {
    local c
    while IFS= read -r c; do
        has_commit "$c" "$2" && { echo "$c"; return; }
    done < <(de_common_checkouts "$1")
    [ -n "$DE_COMMON_URL" ] || die "de_common commit $2 not found locally and the lock has no 'de_common <url>' line"
    fetch_cached de_common "$DE_COMMON_URL" "$2"
}

# ---- modes -------------------------------------------------------------------
mode_sync() {
    local i
    for i in "${!PIN_NAMES[@]}"; do
        local name="${PIN_NAMES[$i]}" sha="${PIN_SHAS[$i]}" src dest sub
        src="$(resolve_module "$i")"
        echo ">>> syncing $name @ ${sha:0:8} from $src"
        dest="$EXTERNAL_DIR/$name"
        rm -rf "$dest"
        mkdir -p "$dest"
        git -C "$src" archive "$sha" | tar -x -C "$dest"

        # Populate the pinned src/de_common submodule content, if any.
        sub="$(git -C "$src" ls-tree "$sha" src/de_common | awk '{print $3}')"
        if [ -n "$sub" ]; then
            local sub_src co=""
            [ "$src" = "$CACHE_DIR/$name.git" ] || co="$src"
            sub_src="$(resolve_de_common "$co" "$sub")"
            echo ">>>   src/de_common @ ${sub:0:8} from $sub_src"
            mkdir -p "$dest/src/de_common"
            git -C "$sub_src" archive "$sub" | tar -x -C "$dest/src/de_common"
        fi
    done

    cp "$LOCK_FILE" "$SYNCED_FILE"
    echo ">>> synced -> $EXTERNAL_DIR (.synced written)"
}

# Warn when a pin is behind, or off, the local checkout's master. Never fails.
report_drift() {
    local i
    for i in "${!PIN_NAMES[@]}"; do
        local name="${PIN_NAMES[$i]}" sha="${PIN_SHAS[$i]}" co ahead
        co="$(find_checkout "$name")"
        [ -n "$co" ] && has_commit "$co" "$sha" || continue
        git -C "$co" rev-parse -q --verify master >/dev/null || continue
        if ! git -C "$co" merge-base --is-ancestor "$sha" master; then
            warn "$name pin ${sha:0:8} is not on $co master (side branch?)"
            continue
        fi
        ahead="$(git -C "$co" rev-list --count "$sha..master")"
        [ "$ahead" -gt 0 ] && warn "$name master is $ahead commit(s) ahead of the pin ${sha:0:8} — sync_sources.sh --bump $name to move it"
    done
    return 0
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
    report_drift
}

# tag_one <repo> <sha> — create andruav-pin-<sha8> in <repo> (idempotent).
tag_one() {
    local repo="$1" sha="$2" tag cur
    tag="$(pin_tag "$sha")"
    if cur="$(git -C "$repo" rev-parse -q --verify "refs/tags/$tag^{commit}")"; then
        [ "$cur" = "$sha" ] || die "$repo: tag $tag already points at $cur, not $sha"
        echo ">>> $tag exists in $repo"
        return
    fi
    git -C "$repo" tag -a "$tag" -m "andruav_android_app deNative pin" "$sha"
    echo ">>> tagged $tag in $repo"
}

# for_each_pin <fn> — fn <repo> <sha> <url> for every module pin and every
# distinct de_common gitlink, using local checkouts only.
for_each_pin() {
    local fn="$1" i seen=" "
    for i in "${!PIN_NAMES[@]}"; do
        local name="${PIN_NAMES[$i]}" sha="${PIN_SHAS[$i]}" co sub
        co="$(find_checkout "$name")"
        [ -n "$co" ] && has_commit "$co" "$sha" || die "$name: no local checkout with ${sha:0:8} (set DE_WORKSPACE)"
        "$fn" "$co" "$sha" "${PIN_URLS[$i]}"
        sub="$(git -C "$co" ls-tree "$sha" src/de_common | awk '{print $3}')"
        if [ -n "$sub" ] && [[ "$seen" != *" $sub "* ]]; then
            seen+="$sub "
            local dc
            dc="$(de_common_checkouts "$co" | while IFS= read -r c; do has_commit "$c" "$sub" && { echo "$c"; break; }; done)"
            [ -n "$dc" ] || die "de_common ${sub:0:8} not found in any local de_common checkout"
            [ -n "$DE_COMMON_URL" ] || die "the lock has no 'de_common <url>' line"
            "$fn" "$dc" "$sub" "$DE_COMMON_URL"
        fi
    done
}

tag_pin()  { tag_one "$1" "$2"; }
push_pin() {
    local tag
    tag="$(pin_tag "$2")"
    # The public DroneEngage org is published by hand, never from here.
    case "$3" in
        *DroneEngage/*) [ "${DE_ALLOW_PUBLIC_PUSH:-}" = 1 ] \
            || die "refusing to push $tag to public $3 (set DE_ALLOW_PUBLIC_PUSH=1 to override)" ;;
    esac
    tag_one "$1" "$2"
    echo ">>> pushing $tag -> $3"
    git -C "$1" push -q "$3" "refs/tags/$tag"
}

mode_bump() {
    local targets=("$@") i changed=0
    [ "${#targets[@]}" -gt 0 ] || targets=("${PIN_NAMES[@]}")
    local t
    for t in "${targets[@]}"; do
        local found=""
        for i in "${!PIN_NAMES[@]}"; do [ "${PIN_NAMES[$i]}" = "$t" ] && found="$i"; done
        [ -n "$found" ] || die "no pin named '$t' in $LOCK_FILE"
        local co new old="${PIN_SHAS[$found]}"
        co="$(find_checkout "$t")"
        [ -n "$co" ] || die "$t: no local checkout (set DE_WORKSPACE)"
        new="$(git -C "$co" rev-parse master)"
        if [ "$new" = "$old" ]; then echo ">>> $t already at master ${new:0:8}"; continue; fi
        sed -i "s/^\($t[[:space:]].*[[:space:]]\)$old\b/\1$new/" "$LOCK_FILE"
        PIN_SHAS[$found]="$new"
        echo ">>> $t: ${old:0:8} -> ${new:0:8}"
        changed=1
    done
    [ "$changed" = 1 ] || return 0
    for_each_pin tag_pin
    mode_sync
    echo ">>> review/commit deNative/sources.lock, then: deNative/sync_sources.sh --push-tags"
}

case "${1:-}" in
    --check)     mode_check ;;
    --tag)       for_each_pin tag_pin ;;
    --push-tags) for_each_pin push_pin ;;
    --bump)      shift; mode_bump "$@" ;;
    "")          mode_sync ;;
    *)           die "unknown argument '$1' (usage: sync_sources.sh [--check|--bump [repo...]|--tag|--push-tags])" ;;
esac
