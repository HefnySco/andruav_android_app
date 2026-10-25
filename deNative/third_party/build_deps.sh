#!/usr/bin/env bash
# build_deps.sh — build OpenSSL, curl and copy Boost headers for Android.
#
# Usage: ANDROID_HOME=<sdk> deNative/third_party/build_deps.sh
#
# Produces deNative/third_party/out/<abi>/{lib,include} for arm64-v8a and
# x86_64, then compiles deNative/third_party/smoke/ against them as a check.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
THIRD_PARTY_DIR="$SCRIPT_DIR"
DOWNLOADS_DIR="$THIRD_PARTY_DIR/downloads"
SRC_DIR="$THIRD_PARTY_DIR/src"
OUT_DIR="$THIRD_PARTY_DIR/out"
SMOKE_DIR="$THIRD_PARTY_DIR/smoke"

source "$THIRD_PARTY_DIR/versions.env"

: "${ANDROID_HOME:?set ANDROID_HOME to the Android SDK directory}"
NDK="$ANDROID_HOME/ndk/26.3.11579264"
API=24
TOOLCHAIN_BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
ABIS="arm64-v8a x86_64"

[ -d "$NDK" ] || { echo "ERROR: NDK not found at $NDK" >&2; exit 1; }

fetch_and_verify() {
    local name="$1" archive="$2" url="$3" sha256="$4"
    local dest="$DOWNLOADS_DIR/$archive"
    mkdir -p "$DOWNLOADS_DIR"
    if [ ! -f "$dest" ]; then
        echo ">>> downloading $name ($url)"
        curl -fL --retry 3 -o "$dest" "$url"
    else
        echo ">>> $name archive already present, skipping download"
    fi
    echo "$sha256  $dest" | sha256sum --check --status \
        || { echo "ERROR: SHA256 mismatch for $dest — aborting" >&2; exit 1; }
}

extract_fresh() {
    local name="$1" archive="$2"
    local dest_dir="$SRC_DIR/$name"
    echo ">>> extracting $name"
    rm -rf "$dest_dir"
    mkdir -p "$dest_dir"
    tar -xf "$DOWNLOADS_DIR/$archive" -C "$dest_dir" --strip-components=1
}

fetch_and_verify "OpenSSL" "$OPENSSL_ARCHIVE" "$OPENSSL_URL" "$OPENSSL_SHA256"
fetch_and_verify "curl"    "$CURL_ARCHIVE"    "$CURL_URL"    "$CURL_SHA256"
fetch_and_verify "Boost"   "$BOOST_ARCHIVE"   "$BOOST_URL"   "$BOOST_SHA256"

extract_fresh "openssl" "$OPENSSL_ARCHIVE"
extract_fresh "curl"    "$CURL_ARCHIVE"
extract_fresh "boost"   "$BOOST_ARCHIVE"

build_openssl() {
    local abi="$1" out="$2"
    local target
    case "$abi" in
        arm64-v8a) target=android-arm64 ;;
        x86_64)    target=android-x86_64 ;;
        *) echo "ERROR: no OpenSSL target for $abi" >&2; exit 1 ;;
    esac
    local build_dir="$SRC_DIR/openssl/build-$abi"
    rm -rf "$build_dir"
    mkdir -p "$build_dir"
    echo ">>> OpenSSL $OPENSSL_VER configure: $target -> $out"
    (
        cd "$build_dir"
        export ANDROID_NDK_ROOT="$NDK"
        export PATH="$TOOLCHAIN_BIN:$PATH"
        "$SRC_DIR/openssl/Configure" "$target" \
            no-shared no-tests \
            -D__ANDROID_API__="$API" \
            --prefix="$out"
        make -j"$(nproc)"
        make install_sw
    )
}

build_curl() {
    local abi="$1" out="$2"
    local build_dir="$SRC_DIR/curl/build-$abi"
    rm -rf "$build_dir"
    mkdir -p "$build_dir"
    echo ">>> curl $CURL_VER configure: $abi -> $out"
    cmake -S "$SRC_DIR/curl" -B "$build_dir" \
        -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$abi" \
        -DANDROID_PLATFORM="$API" \
        -DBUILD_SHARED_LIBS=OFF \
        -DCURL_USE_PKGCONFIG=OFF \
        -DCURL_USE_OPENSSL=ON \
        -DOPENSSL_ROOT_DIR="$out" \
        -DOPENSSL_INCLUDE_DIR="$out/include" \
        -DOPENSSL_CRYPTO_LIBRARY="$out/lib/libcrypto.a" \
        -DOPENSSL_SSL_LIBRARY="$out/lib/libssl.a" \
        -DHTTP_ONLY=ON \
        -DCURL_USE_LIBPSL=OFF \
        -DCURL_USE_LIBSSH2=OFF \
        -DBUILD_CURL_EXE=OFF \
        -DBUILD_TESTING=OFF \
        -DCMAKE_INSTALL_PREFIX="$out"
    cmake --build "$build_dir" -j"$(nproc)"
    cmake --install "$build_dir"
}

install_boost_headers() {
    local abi="$1" out="$2"
    echo ">>> Boost $BOOST_VER headers -> $out/include"
    mkdir -p "$out/include"
    cp -a "$SRC_DIR/boost/boost" "$out/include/"
}

build_smoke() {
    local abi="$1" out="$2"
    local build_dir="$SMOKE_DIR/build-$abi"
    rm -rf "$build_dir"
    echo ">>> smoke build: $abi"
    cmake -S "$SMOKE_DIR" -B "$build_dir" \
        -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$abi" \
        -DANDROID_PLATFORM="$API" \
        -DDE_DEPS_PREFIX="$out"
    cmake --build "$build_dir" -j"$(nproc)"
}

for abi in $ABIS; do
    out="$OUT_DIR/$abi"
    rm -rf "$out"
    build_openssl "$abi" "$out"
    build_curl "$abi" "$out"
    install_boost_headers "$abi" "$out"
done

for abi in $ABIS; do
    build_smoke "$abi" "$OUT_DIR/$abi"
done

echo
echo "=== build_deps summary ==="
for abi in $ABIS; do
    out="$OUT_DIR/$abi"
    echo "[$abi] $out"
    find "$out/lib" -maxdepth 1 -name '*.a' -printf '    lib/%f (%s bytes)\n' 2>/dev/null | sort
    [ -d "$out/include/boost/beast" ] && echo "    include/boost/beast/ present"
    find "$SMOKE_DIR/build-$abi" -name '*.so' -printf '    smoke: %p\n'
done
echo "=== done ==="
