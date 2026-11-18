#!/usr/bin/env bash
#
# check_apk_no_sim.sh <apk>
#
# Refuse a release APK whose native libraries carry sim code. Extracts
# every lib/*/*.so inside the APK to a temporary folder and runs
# de_common's tools/check_no_sim.sh (GL-0136) on each one. Sim builds
# compile de_common/de_sim/*.cpp; de_sim_context.cpp holds the
# DE_SIM_BUILD_MARKER_V1 string for exactly this scan.
#
# Covers libde_comm.so, libde_mavlink.so and libde_host.so across all
# packaged ABIs.
#
#   exit 0 - no marker found in any .so
#   exit 1 - a .so contains sim code (the message names the library)
#   exit 2 - the APK does not exist, no argument, the gate tool is
#            missing, or the APK cannot be read

set -u

apk="${1:-}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
check_tool="$script_dir/external/droneengage_communication_pro/src/de_common/tools/check_no_sim.sh"

if [ -z "$apk" ] || [ ! -f "$apk" ]; then
    echo "check_apk_no_sim: no such APK: ${apk:-<none>}" >&2
    exit 2
fi

if [ ! -f "$check_tool" ]; then
    echo "check_apk_no_sim: gate tool missing: $check_tool" >&2
    exit 2
fi

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

# unzip -o keeps the lib/<abi>/<name>.so structure so a failure names the
# offending library. An APK is a zip; a corrupt one fails here with exit 2.
if ! unzip -o -q "$apk" 'lib/*/*.so' -d "$tmpdir"; then
    echo "check_apk_no_sim: cannot extract lib/*/*.so from $apk" >&2
    exit 2
fi

found=0
shopt -s nullglob
for so in "$tmpdir"/lib/*/*.so; do
    found=1
    rel="${so#"$tmpdir"/}"
    "$check_tool" "$so"
    rc=$?
    if [ "$rc" -eq 1 ]; then
        echo "check_apk_no_sim: $apk - $rel contains sim code - refuse" >&2
        exit 1
    elif [ "$rc" -ne 0 ]; then
        echo "check_apk_no_sim: $apk - gate tool failed on $rel (exit $rc)" >&2
        exit 2
    fi
done

if [ "$found" -eq 0 ]; then
    echo "check_apk_no_sim: $apk has no lib/*/*.so - nothing to check" >&2
    exit 2
fi

exit 0
