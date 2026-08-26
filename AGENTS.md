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

## Relationship to DroneEngage (sibling repo)

This APK is the **Android counterpart of the Linux DroneEngage modules**
`de_comm` + `de_mavlink` + `de_camera`, packaged as a single app. The Linux
side lives in a sibling git repo at `../drone_engage/` and has a shared
`AGENTS.md` there covering the cross-module architecture:

- `../drone_engage/AGENTS.md` — workspace-wide guide: repo layout, the
  `de_common` UDP message-bus pattern (`CModule` / `CFacade_Base` /
  `CAndruavMessageParserBase` / `cUDPClient`), the virtual-video-device
  pipeline (`v4l2loopback` chaining), and the
  `de_<module>.config.module.json` / `de_<module>.local` config conventions.

Because AGENTS.md discovery walks *up* the filesystem from the file being
edited and does not cross the sibling-repo boundary, that shared file is
**not auto-discovered** from here — consult it manually when you need the
Linux module architecture, message protocol, or config schema. The Android
app mirrors the same Andruav message types and routing concepts, so the
`de_common` notes there apply when porting/aligning behavior between the
Android and Linux implementations.
