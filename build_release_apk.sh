#!/usr/bin/env bash
set -euo pipefail

# build_release_apk.sh — Build the Andruav Android app as a release APK.
# Uses the existing Gradle configuration, which signs the release build
# with the default debug keystore. For a production release, configure a
# release signing config in app/build.gradle or re-sign the output APK.

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

DEFAULT_OUT_DIR="$PROJECT_DIR/app/build/outputs/apk/release"
CLEAN=1
OUTPUT_DIR="$DEFAULT_OUT_DIR"

usage() {
  cat <<EOF
Usage: $0 [options]
Options:
  -o, --output-dir DIR   Copy the final APK to DIR (default: $DEFAULT_OUT_DIR)
  -n, --no-clean         Skip ./gradlew clean before building
  -h, --help             Show this help
EOF
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -o|--output-dir)
      OUTPUT_DIR="$2"
      shift 2
      ;;
    -n|--no-clean)
      CLEAN=0
      shift
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      ;;
  esac
done

# Resolve ANDROID_HOME from the environment or from local.properties.
if [[ -z "${ANDROID_HOME:-}" ]]; then
  if [[ -f "$PROJECT_DIR/local.properties" ]]; then
    ANDROID_HOME="$(grep '^sdk.dir=' "$PROJECT_DIR/local.properties" | cut -d= -f2- || true)"
    # Remove any trailing whitespace / CR.
    ANDROID_HOME="$(printf '%s' "$ANDROID_HOME" | tr -d '\r')"
  fi
fi

if [[ -z "${ANDROID_HOME:-}" || ! -d "$ANDROID_HOME" ]]; then
  cat <<EOF >&2
Error: ANDROID_HOME is not set and no valid sdk.dir was found in local.properties.
Please set ANDROID_HOME to your Android SDK directory, e.g.:
  export ANDROID_HOME=/home/mhefny/TDisk/Android/SDK
EOF
  exit 1
fi

export ANDROID_HOME

# Validate the SDK components this project expects.
REQUIRED_PLATFORM="$ANDROID_HOME/platforms/android-34"
REQUIRED_BUILD_TOOLS="$ANDROID_HOME/build-tools/34.0.0"

if [[ ! -d "$REQUIRED_PLATFORM" ]]; then
  echo "Error: missing required SDK platform: $REQUIRED_PLATFORM" >&2
  exit 1
fi

if [[ ! -d "$REQUIRED_BUILD_TOOLS" ]]; then
  echo "Error: missing required build tools: $REQUIRED_BUILD_TOOLS" >&2
  exit 1
fi

# Sanity-check the Java version.
JAVA_VERSION_OUTPUT=$(java -version 2>&1 | head -n1)
JAVA_MAJOR=$(echo "$JAVA_VERSION_OUTPUT" | sed -E 's/.* version "([0-9]+)(\.[0-9]+)*".*/\1/')
if [[ -z "$JAVA_MAJOR" ]]; then
  JAVA_MAJOR=$(echo "$JAVA_VERSION_OUTPUT" | sed -E 's/.* version "([0-9]+)\..*/\1/')
fi

if [[ "${JAVA_MAJOR:-0}" -lt 17 ]]; then
  echo "Warning: Java 17+ is recommended for this project. Found: $JAVA_VERSION_OUTPUT" >&2
fi

# Build.
if [[ "$CLEAN" -eq 1 ]]; then
  echo "==> Cleaning previous build artifacts..."
  ./gradlew clean --no-daemon
fi

echo "==> Building release APK..."
./gradlew :app:assembleRelease --no-daemon

# Locate the built APK.
APK_PATH=$(find "$PROJECT_DIR/app/build/outputs/apk/release" -maxdepth 1 -name '*.apk' -type f | sort -V | tail -n1)
if [[ -z "$APK_PATH" ]]; then
  echo "Error: no APK found in $PROJECT_DIR/app/build/outputs/apk/release" >&2
  exit 1
fi

APK_NAME=$(basename "$APK_PATH")

# Copy to the requested output directory (skip if it's already there).
mkdir -p "$OUTPUT_DIR"
FINAL_PATH="$OUTPUT_DIR/$APK_NAME"
if [[ "$(readlink -f "$APK_PATH")" != "$(readlink -f "$FINAL_PATH")" ]]; then
  cp -f "$APK_PATH" "$FINAL_PATH"
fi

echo ""
echo "==> Release APK built successfully:"
echo "    $FINAL_PATH"
ls -lh "$FINAL_PATH"

# Optional verification.
APK_SIGNER="$REQUIRED_BUILD_TOOLS/apksigner"
if [[ -x "$APK_SIGNER" ]]; then
  echo ""
  echo "==> APK signer verification:"
  "$APK_SIGNER" verify --verbose "$FINAL_PATH" || true
fi
