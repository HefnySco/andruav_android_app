#!/usr/bin/env bash
set -euo pipefail

# build_release_apk.sh — Build the Andruav Android app as a signed release APK.
# Signing credentials are read from a gitignored keystore.properties file at
# the project root. If the file is absent the build falls back to the debug
# keystore (see app/build.gradle signingConfigs), which is fine for local
# testing but NOT for a production/SourceForge release.

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

DEFAULT_OUT_DIR="$PROJECT_DIR/app/build/outputs/apk/release"
CLEAN=1
INSTALL=0
UPLOAD=0
OUTPUT_DIR="$DEFAULT_OUT_DIR"

usage() {
  cat <<EOF
Usage: $0 [options]
Options:
  -o, --output-dir DIR   Copy the final APK to DIR (default: $DEFAULT_OUT_DIR)
  -n, --no-clean         Skip ./gradlew clean before building
  -i, --install          Install on the connected device via adb after building;
                         falls back to pushing the APK to /sdcard/Download/
  -u, --upload           Upload the APK to the connected device via adb push
                         (to /sdcard/Download/) without installing
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
    -i|--install)
      INSTALL=1
      shift
      ;;
    -u|--upload)
      UPLOAD=1
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

# Resolve JAVA_HOME to a JDK 17 (the Gradle toolchain requires 17 even though
# compileOptions targets Java 11 bytecode). Prefer an existing JAVA_HOME if it
# already points at JDK 17, otherwise auto-detect one under /usr/lib/jvm.
java_major_of() {
  "$1/bin/javac" -version 2>&1 | sed -E 's/.* ([0-9]+)(\.[0-9]+)*/\1/'
}

if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/javac" \
      && "$(java_major_of "$JAVA_HOME")" == "17" ]]; then
  : # JAVA_HOME already good
else
  DETECTED_JDK=""
  for candidate in /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/*17*/; do
    if [[ -x "$candidate/bin/javac" && "$(java_major_of "$candidate")" == "17" ]]; then
      DETECTED_JDK="$candidate"
      break
    fi
  done
  if [[ -n "$DETECTED_JDK" ]]; then
    export JAVA_HOME="$DETECTED_JDK"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "==> Using JDK 17 at $JAVA_HOME"
  else
    echo "Warning: no JDK 17 found under /usr/lib/jvm; using java on PATH." >&2
  fi
fi

# Require keystore.properties for a signed release. The Gradle script falls
# back to the debug keystore when it's missing, so we only warn here and let
# the build proceed — but make it very visible.
KEYSTORE_PROPS="$PROJECT_DIR/keystore.properties"
if [[ ! -f "$KEYSTORE_PROPS" ]]; then
  echo "Warning: keystore.properties not found at $KEYSTORE_PROPS" >&2
  echo "         Release will be signed with the DEBUG keystore (not suitable for production)." >&2
  echo "         Create keystore.properties with storeFile/storePassword/keyAlias/keyPassword." >&2
else
  echo "==> Using signing credentials from $KEYSTORE_PROPS"
fi

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

if [[ "${JAVA_MAJOR:-0}" != "17" ]]; then
  echo "Warning: JDK 17 is required for this project. Found: $JAVA_VERSION_OUTPUT" >&2
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

# Optional install/upload on a connected device.
if [[ "$INSTALL" -eq 1 || "$UPLOAD" -eq 1 ]]; then
  ADB="$ANDROID_HOME/platform-tools/adb"
  if [[ ! -x "$ADB" ]]; then
    echo "Error: adb not found at $ADB" >&2
    exit 1
  fi

  if [[ -z "$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')" ]]; then
    echo "Error: no adb device connected." >&2
    exit 1
  fi

  REMOTE_APK="/sdcard/Download/$APK_NAME"

  # adb push writes the file behind MediaStore's back, so file managers that list
  # MediaStore (e.g. Xiaomi/HyperOS Files) show it with no size/date, or not at
  # all, until it is scanned. Ask MediaProvider to index it right away.
  push_apk() {
    "$ADB" push "$FINAL_PATH" "$REMOTE_APK"
    "$ADB" shell "content call --uri content://media --method scan_file --arg '$REMOTE_APK'" >/dev/null 2>&1 \
      || "$ADB" shell "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d 'file://$REMOTE_APK'" >/dev/null 2>&1 \
      || echo "Warning: media scan failed; the APK may not show in the Files app until the device rescans." >&2
  }

  if [[ "$INSTALL" -eq 1 ]]; then
    echo ""
    echo "==> Installing APK on connected device..."
    if "$ADB" install -r "$FINAL_PATH"; then
      echo "==> Installed successfully."
    else
      echo "Warning: adb install failed (check 'Install via USB' in Developer options)." >&2
      echo "==> Pushing APK to /sdcard/Download/ instead..."
      push_apk
      echo "==> Pushed. Open the Files app on the device and tap the APK to install."
    fi
  fi

  if [[ "$UPLOAD" -eq 1 ]]; then
    echo ""
    echo "==> Uploading APK to connected device..."
    push_apk
    echo "==> Uploaded to $REMOTE_APK"
  fi
fi
