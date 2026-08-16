# Build Environment

This project needs a JDK 17 and the Android SDK — the exact install locations
are not important as long as the versions match. `local.properties` already
points `sdk.dir` at the correct SDK for this machine, so you usually only need
to set `JAVA_HOME` before building.

## Toolchain requirements

- **JDK 17** (any distribution: Temurin, OpenJDK, Oracle…). The toolchain JDK
  is 17 while `compileOptions` targets Java 11 bytecode.
- **Android SDK** (location read from `local.properties` → `sdk.dir`), with:
  - `cmdline-tools/latest`
  - `platform-tools`
  - `build-tools;34.0.0`
  - `platforms;android-34`
- **Gradle 8.14**: auto-downloaded by the wrapper to `~/.gradle/wrapper/dists/`
- **Android Gradle Plugin**: 8.4.2

## Build commands

Set `JAVA_HOME` to a JDK 17 install and `ANDROID_HOME` to the SDK directory
(from `local.properties`). For example:

```bash
# Detect a system JDK 17 if one is installed:
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))   # only if `javac` is JDK 17
# Or point at it explicitly, e.g.:
# export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

export ANDROID_HOME=$(grep '^sdk.dir=' local.properties | cut -d= -f2)
export PATH="$JAVA_HOME/bin:$PATH"
```

Then from the project root:

```bash
# Release APK (signed with debug keystore per app/build.gradle)
./gradlew :app:assembleRelease --no-daemon

# Debug APK
./gradlew :app:assembleDebug --no-daemon

# Or use the helper script (also exports nothing — set env vars first)
./build_release_apk.sh
```

Output APKs land in `app/build/outputs/apk/<variant>/`.

## Verify APK signature

```bash
"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --verbose <apk>
```

## Notes

- `gradle.properties` enables `org.gradle.configuration-cache=true`.
- `app/build.gradle` signs release builds with `signingConfigs.debug` — fine for
  dev/testing, not for Play Store production release.
- `compileOptions` targets Java 11 bytecode while the toolchain JDK is 17.
