# Build Environment

This project requires no system-wide installs — the full Android toolchain lives
in userspace under `~/android-build/`.

## Toolchain (userspace)

- **JDK 17**: `~/android-build/jdk` (Eclipse Temurin 17.0.13)
- **Android SDK**: `~/android-build/sdk`
  - `cmdline-tools/latest`
  - `platform-tools`
  - `build-tools;34.0.0`
  - `platforms;android-34`
- **Gradle 8.14**: auto-downloaded by the wrapper to `~/.gradle/wrapper/dists/`
- **Android Gradle Plugin**: 8.4.2

`local.properties` points `sdk.dir` at `~/android-build/sdk`.

## Build commands

Always export these env vars first (no system `java`/`gradle` on PATH):

```bash
export JAVA_HOME=~/android-build/jdk
export ANDROID_HOME=~/android-build/sdk
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
~/android-build/sdk/build-tools/34.0.0/apksigner verify --verbose <apk>
```

## Notes

- `gradle.properties` enables `org.gradle.configuration-cache=true`.
- `app/build.gradle` signs release builds with `signingConfigs.debug` — fine for
  dev/testing, not for Play Store production release.
- `compileOptions` targets Java 11 bytecode while the toolchain JDK is 17.
