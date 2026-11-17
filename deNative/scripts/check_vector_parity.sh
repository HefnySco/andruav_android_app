#!/usr/bin/env bash
# check_vector_parity.sh - compare the shared conformance vectors inside
# the pinned Android copy (deNative/external/droneengage_communication_pro)
# with the WebClient originals. The pin can lag: a WebClient vector change
# reaches de_comm through sync_fixtures.sh + the pure-gates drift job, but
# the Android copy only moves when sync_sources.sh bumps the pin.
#
#   deNative/scripts/check_vector_parity.sh [path-to-webclient-repo]
#
# Default webclient repo: <workspace>/droneengage_webclient_react,
# resolved from this script's own folder. Prints one line per file with
# MATCH or DIFFER plus the two sha256 values on mismatch. Exit 1 when any
# file differs or is missing, so a release step can gate on it.

set -u

HERE="$(cd "$(dirname "$0")" && pwd)"
APP_ROOT="$(cd "$HERE/../.." && pwd)"
ANDROID_FIX="$APP_ROOT/deNative/external/droneengage_communication_pro/src/test/fixtures"
WEBCLIENT="${1:-$APP_ROOT/../droneengage_webclient_react}"
WFIX="$WEBCLIENT/src/js/core/mission/test/__fixtures__"
WGOLD="$WEBCLIENT/src/js/core/mission/test/__golden__"

fail=0
check() {   # <label> <android file> <webclient file>
    local label="$1" a="$2" w="$3"
    if [ ! -f "$a" ] || [ ! -f "$w" ]; then
        echo "DIFFER $label (missing: ${a} or ${w})"
        fail=1
        return
    fi
    local ha hw
    ha=$(sha256sum "$a" | cut -d' ' -f1)
    hw=$(sha256sum "$w" | cut -d' ' -f1)
    if [ "$ha" = "$hw" ]; then
        echo "MATCH  $label"
    else
        echo "DIFFER $label"
        echo "       android $ha"
        echo "       webclient $hw"
        fail=1
    fi
}

# the seven shared vector files (author: webclient __fixtures__)
for v in geo expr wm alloc inst sep fact_cover; do
    check "${v}_vectors.json" "$ANDROID_FIX/${v}_vectors.json" \
          "$WFIX/${v}_vectors.json"
done
# the two package goldens the C++ tests also read (author: __golden__)
for p in pattern_survey_concave_terrain.pkg.json launch_envelope.pkg.json; do
    check "$p" "$ANDROID_FIX/$p" "$WGOLD/$p"
done

if [ "$fail" -eq 0 ]; then
    echo "parity: all shared vectors match"
else
    echo "parity: drift found - bump the pin with" \
         "deNative/sync_sources.sh --bump" >&2
fi
exit "$fail"
