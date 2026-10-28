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
  - `ndk;26.3.11579264`
  - `cmake;3.22.1`
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

## Native modules

The `:deNative` module builds the upstream DroneEngage modules (`de_comm`,
`de_mavlink`) as `.so` files inside the APK. Two one-time setup commands,
run from the repo root:

```bash
# 1) Build OpenSSL/curl/Boost into deNative/third_party/out/<abi> (needs ANDROID_HOME)
deNative/third_party/build_deps.sh

# 2) Copy the pinned upstream sources into deNative/external/
deNative/sync_sources.sh
```

`deNative/sources.lock` pins one commit per upstream repo;
`deNative/external/` and `deNative/third_party/out/` are gitignored. Gradle
runs `sync_sources.sh --check` in `preBuild` — if the lock changes, re-run
`deNative/sync_sources.sh`. `DE_WORKSPACE` overrides where the upstream
checkouts live (default `../../drone_engage`, falls back to the repo's
parent dir).

## Notes

- `gradle.properties` enables `org.gradle.configuration-cache=true`.
- `app/build.gradle` signs release builds with `signingConfigs.debug` — fine for
  dev/testing, not for Play Store production release.
- `compileOptions` targets Java 11 bytecode while the toolchain JDK is 17.

## Testing on a real (non-rooted) phone — remember always

Verified on a production phone (adb over USB, no root):

- **`am start(-foreground)-service` CANNOT start the app-internal
  `DeAndroidModuleService`** — it is `exported=false`, so shell (uid 2000)
  gets `Permission Denial`. This works only on an emulator running `adb root`.
  The same applies to `ap.andruav.denative.*` module services? (DeCommService /
  DeMavlinkService DO start from shell — they are exported.)
- The app-side bus client (`andruav_phone` CModule → `AndruavDeBusClient`)
  is installed by that supervisor service inside the main app process; its
  internal start defaults the s2s dial to port **60000** (no `s2s_port`
  extra possible from shell on a non-rooted phone). Match the module
  configs to it: `de_comm s2s_udp_listening_port` and
  `de_mavlink s2s_udp_target_port` must be `60000` on a real phone,
  or `andruav_phone` never registers (Com OFFLINE / telemetry NOT LINKED).
- `pref_native_unit` must go into the DEFAULT shared prefs
  (`shared_prefs/ardupilot.andruav_preferences.xml`); party/unit identity is
  the `owPg4M` string pref.
- Sim-pushed module configs land at
  `/sdcard/Android/data/ardupilot.andruav/files/de_override/<mod>/` and
  override the bundled assets at module START — editing the APK asset config
  alone does nothing while an override exists.
- USB phone → host connectivity: `adb reverse tcp:<port> tcp:<port>` makes
  host ports reachable as `127.0.0.1:<port>` on the phone (no LAN needed).
  `adb reverse --remove` is a clean way to simulate a real socket loss
  (de_comm reports `Socket Status Error` → comm-loss failsafe fires).
  The in-band `commline`/`6509` command is a *commanded* offline and does
  NOT trip the failsafe — only a real socket error does.

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
