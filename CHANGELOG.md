# Changelog: `Andruav_AP_Original` → `Andruav_AP_2026`

This document summarizes everything that changed between the `Andruav_AP_Original`
baseline (version 7.2.1, 2025-09-26) and `Andruav_AP_2026` (version 11.0.2, 2026-08-15).

**Scope:** 33 commits · 189 files changed · +3,881 / −7,559 lines for the v7.2.1 → v9.0.1
baseline rewrite (net code reduction, despite the new features, from aggressive dead-code
removal alongside the rewrite), followed by 27 commits for v10.0.0 → v11.0.2.

For deeper technical write-ups of individual efforts, see the [`wiki/`](wiki/) folder:
- [Architecture Migration](wiki/Architecture-Migration.md) — ClientLib de-AIDL-ification, EventBus 2→3, GreenDAO 2→3
- [Android OS Compatibility](wiki/Android-OS-Compatibility.md) — permission handling rewrite, API 24-34 compatibility audit
- [FPV & WebRTC Streaming](wiki/FPV-Streaming.md) — foreground streaming service, PiP, signaling race-condition fix
- [UI Theme System](wiki/UI-Theme-System.md) — new dark theme, color palette, screen-by-screen restyle
- [Dependency Upgrades](wiki/Dependency-Upgrades.md) — full before/after table with rationale

---

## Version milestones

| Version | Commit | Date |
|---|---|---|
| 7.2.1 (baseline) | `d8169d8` | 2025-09-26 |
| 7.5.0 | `c987be5` | 2026-08-08 |
| 7.5.1 | `5cebf7f` | 2026-08-08 |
| 7.6.0 | `c809882` | 2026-08-08 |
| 8.0.0 | `df950e7` | 2026-08-09 |
| 9.0.0 ("New UI") | `b9a4af2` | 2026-08-09 |
| 9.0.1 | `7ddadbb` | 2026-08-11 |
| 10.0.0 | `894e315` | 2026-08-11 |
| 10.1.0 | `6fca9e0` | 2026-08-12 |
| 10.2.0 | `229c097` | 2026-08-13 |
| 11.0.0 | `10f6d21` | 2026-08-15 |
| 11.0.1 | `7fc5b3a` | 2026-08-15 |
| 11.0.2 (current) | `70a8390` | 2026-08-15 |

---

## 🏗️ Architecture: ClientLib de-AIDL-ification (Phase 3)

`ClientLib` (the DroidPlanner-derived MAVLink/vehicle-control layer) was originally built
around AIDL/Binder as if the drone-control service could live in a separate process. It
never has in this app (`DroidPlannerService` has always run in-process). Three commits
collapse that indirection into direct in-process calls:

- **`b2d4c7d`** — collapsed `IDroidPlannerServices`/`IApiListener` AIDL round-trip to a
  standard `LocalBinder`. Removed `checkForSelfRelease()`/`binderDied()` (only meaningful
  for a cross-process client dying).
- **`d415089`** — `DroneApi` stops extending `IDroneApi.Stub`; `Drone` holds a direct
  reference. Removed `RemoteException` handling and binder-death machinery.
- **`4f9b964`** — `IObserver`/`IMavlinkObserver`/`ICommandListener` converted from
  AIDL-generated `.Stub` interfaces to plain Java interfaces.

Every public `Drone`/`ControlTower`/`apis/*` signature is unchanged — no app-module call
site needed updating. ClientLib no longer contains any AIDL interface, `Stub`, `asBinder()`,
or `RemoteException` (confirmed by repo-wide grep).

## 🏗️ Architecture: Dependency runtime migrations (Phase 4)

- **`d0ec349`** — **EventBus 2.4.0 → org.greenrobot:eventbus 3.3.1**. The 2.x
  reflection-based `onEvent(...)` name convention was replaced by explicit `@Subscribe`
  annotations across 87 methods in 37 files, plus a two-arg `register(this, priority)`
  overload replaced by per-method `@Subscribe(priority = ...)`. Verified with an
  independent source scan (every `onEvent(` preceded by `@Subscribe`, every `register()`
  call site resolves to a class with ≥1 `@Subscribe` method).
- **`c938794`** — follow-up fix: the migration script's Python text-mode I/O had silently
  normalized CRLF→LF in 15 of the 37 touched files; restored original line endings so the
  diff reflects only the intended annotation/import changes.
- **`92579d1`** — **GreenDAO 2.1.0 → org.greenrobot:greendao 3.3.0**. Runtime-library-only
  migration (hand-written DAOs, no codegen). Added the now-abstract `hasKey(T)` method and
  both `bindValues` overloads (`DatabaseStatement` + legacy `SQLiteStatement`) required by
  3.x's `AbstractDao`.
- **`93015bf`** — `androidx.legacy:legacy-support-v4` removed outright (zero usages);
  `com.google.android.material:material` 1.0.0 → 1.12.0; `androidx.appcompat:appcompat`
  1.0.0 → 1.7.0.
- **`ec948d4`** — OkHttp 3.3.1 → 3.14.9, Java-WebSocket 1.3.8 → 1.5.7, Timber 3.1.0 → 4.7.1,
  androidplot 0.6.1 → 1.5.11.
- **`9c5e03a`** — removed entirely-dead dependencies first: `play-services-plus`
  (Google+ API, shut down 2019), `com.jcraft:jsch` (unused `SshConnection`), and an
  undeclared `jackson-mapper-asl` jar left over from a pre-`Java-WebSocket` implementation.
- **`7ddadbb`** (this change) — `android.enableJetifier=true` enabled so AGP rewrites
  legacy `android.support.*` references inside third-party AARs (e.g. `rangebar-release`)
  to AndroidX at build time — fixes a `ClassNotFoundException` crash on RC Settings.

See [Dependency Upgrades](wiki/Dependency-Upgrades.md) for the full table.

## 🔐 Permissions & Android OS compatibility

- **`1072b6a`** (phase 2) — broad compatibility audit against the declared
  minSdk 24 / targetSdk 34 range: Bluetooth `SecurityException` guards for API 31+,
  a missing `NotificationChannel` (silent failures on API 26+), unrequested
  `POST_NOTIFICATIONS` (API 33+), a scoped-storage crash on API 30+, undeclared
  `RECORD_AUDIO`, and a copy-paste bug that checked `READ_MEDIA_IMAGES` twice instead
  of `READ_MEDIA_VIDEO`.
- **`befb277`** (phase 2 item 8) — `SensorService` promoted to a foreground service
  (`startForeground()`/`stopForeground()`) so GPS/IMU collection isn't throttled or
  killed by API 26+ once the app is backgrounded mid-flight.
- **`09b7e24`** (item 9) — trip folders (KML + images + video) now saved via
  `MediaStore.Downloads` on API 29+, so they survive uninstall and stay visible in a
  File Manager, while keeping the KML's relative `href="files/<name>"` links intact.
- **`3f2dc71`** — permission handling rewrite: the app now starts and runs with partial
  or no permissions instead of a blocking popup on every launch. Missing permissions are
  reported as an Andruav protocol `ERROR` message when connected, or a per-feature popup
  when not.

Full detail in [Android OS Compatibility](wiki/Android-OS-Compatibility.md).

## 📹 FPV / WebRTC streaming

- **`d0b00d7`** — camera capture and recording moved into a new foreground
  `FPVStreamingService`, decoupled from any Activity's lifecycle, so a screen-off or
  accidental power-button press no longer kills a live stream. Added Picture-in-Picture
  on back/home, and a green streaming indicator on the home screen's FPV tile.
- **`feda9ef`** — added an explicit camera-streaming close button.
- **`6fecaf3`** — fixed a stop/restart bug: `stopSelf()` alone didn't tear down the
  service while a bound FPV Activity kept it alive via `BIND_AUTO_CREATE`, so
  `onDestroy()` never ran and no later remote start could revive streaming. The stop
  funnel now clears its running flag and announces the stop directly.
- **`1322719`** — added `postSticky` to the `IEventBus` abstraction so late-registering
  subscribers still receive already-posted signaling events.
- **`8812673`** — deferred WebRTC peer-connection construction (which registers the
  incoming-signaling listener) until *after* the local media stream/track exist, closing
  a race where an early `joinme` signal produced a trackless offer that needed a
  disconnect/rejoin to actually deliver video.
- **`d781c10`** — WebSocket client updates to match the deferred-initialization protocol
  timing.
- **`05afb0e`** — `RECORD_AUDIO` made optional for FPV startup (video doesn't need
  microphone audio); FPV no longer blocks launch if the mic permission is denied.

Full detail in [FPV & WebRTC Streaming](wiki/FPV-Streaming.md).

## 🔋 Power management

- **`0c440dc`** — a 3-second re-evaluation loop now automatically gates the phone's own
  GPS/IMU sensors based on flight-controller state (armed + valid FC GPS fix → phone
  sensors OFF; disconnected, unarmed, or no fix → phone sensors ON), reducing heat and
  battery drain when the FC's own GPS is already authoritative. The existing manual
  "ignore mobile sensors" setting still overrides this logic.

## 🎨 UI: new theme (v9.0.0 / v9.0.1)

- **`b9a4af2`** — "New UI" version 9.0.0: introduced the dark, gradient-based visual
  language now used across the app.
- **`ed8bf40`** — new drawable/color resources: gradient backgrounds, card/dialog
  surfaces, section-header chips, module badge icons (COM/FCB/FPV/IMU), segmented tabs.
- **`8a002b5`** — About dialog rebuilt from an inline HTML string into a themed,
  inflated layout (`dialog_about.xml`).
- **`7ddadbb`** (this change) — RC Settings screen restyled to match: dark gradient
  background, card-style section header, chip-style range-bar tracks, checkboxes tinted
  to the theme's accent color, and rounded chip-style inputs.

Full detail, including the color palette reference, in [UI Theme System](wiki/UI-Theme-System.md).

## 🧹 Code cleanup

- **`6d77364`** (phase 1) — removed 36 confirmed-dead classes (widgets, events, util
  helpers), an orphaned `build.gradle` inside the Java source tree, the fully-orphaned
  `libuvccamera-release` module, a dead `usb4java` dependency, and a disabled
  `signingConfigs` block that held a hardcoded keystore path and plaintext password.
  Also removed the disabled "Native FCB" and "External Cam" code paths, both permanently
  dead behind `FeatureSwitch` kill-switches with no way to re-enable.
- **`ee986d0`** — general Gradle script cleanup.

## 🐛 Other fixes

- **`cfb5115`** — `getIPWifi()` now falls back to scanning all interfaces for any
  private-range (RFC1918) IPv4 address when the phone is acting as a Wi-Fi AP/hotspot,
  instead of falling through to `127.0.0.1` on OEMs with a non-standard hotspot
  interface name.
- **`a3589d8`** — login changes for compatibility with the new Auth flow (no version
  info in the handshake).

---

## v10.0.0 – v11.0.2 (2026-08-11 → 2026-08-15)

27 commits continuing the rewrite: a full home-screen redesign, FPV/camera
background-restart reliability, a battery-optimization exemption prompt, a
themed-dialog overhaul, a telemetry/sensor/camera preferences rework, and
several connection-state bug fixes.

### 🏠 Home screen redesign (v10.0.0)

- **`38c6650`** — "SUPER REFACTOR - UI": complete `MainScreen` redesign. New
  `activity_main.xml` layout, a `FcbConnectionSheet` bottom sheet for Bluetooth
  pairing, a new home drawable set (`bg_home_*`, `ic_home_*`), a
  `colors_home_redesign.xml` palette, and a `popup_home_overflow` menu.
  41 files, +2,316 / −334.
- **`e6c2d12`** — GUI fixes: removed unused `list_item_tlog_info` and
  `widget_cardwheel` layouts; adjusted `activity_first.xml` across all size
  qualifiers (normal, land, small, xlarge).
- **`0e287df`** — app icon update.

### 📹 FPV / camera streaming reliability

- **`772ff0d`** — decoupled camera capture/publish (`FPVStreamingService`) from
  the FPV Activity being launchable. When the app is backgrounded with no visible
  window (e.g. after PiP closes), Android blocks `startActivity()` but allows
  service starts; the Activity launch is now best-effort UI only and no longer
  gates whether the stream restarts — fixes camera permanently unable to restart
  after a power-button press during flight.
- **`fbe5169`** — moved the camera-restart `EventBus` subscriber from
  `BaseAndruavShasha` (only alive while an Activity is resumed) to `App` (registered
  once in `onCreate()`, never unregistered), so a remote "start streaming" command
  posted while backgrounded is no longer silently dropped.
- **`3d915a0`** (partial) — removed the redundant Stream-in-HD toggle; per-facing
  Camera Resolution (SD/HD/Max) now fully controls capture. Derived the remaining
  consumers (GCS capability report, local recording, PiP aspect ratio) from Camera
  Resolution via new `Preference.isActiveCameraHD`/`getActiveCameraDimensions`
  helpers, fixing a width/height swap bug in the recording path along the way.

### 🔋 Battery optimization exemption

- **`e45913f`** — one-time startup prompt (on `FirstScreen`, alongside the
  permissions dialog) using the standard AOSP
  `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` dialog. Background OS battery
  management (Doze/App Standby) could kill the foreground telemetry/FPV services
  mid-flight even with proper foreground-service notifications.

### 🎨 UI / UX: themed dialogs & startup

- **`9f5f773`** — replaced deprecated `ProgressDialog` with custom-styled magnetic
  dialogs (`dialog_magnetic_modal.xml`, `dialog_magnetic_progress.xml`) and a new
  magnetic dialog color palette; added `DialogHelper.buildMagneticDialog()` factory.
- **`42a8b81`** — replaced the connect/reconnect `ProgressDialog` with a themed
  `dialog_connect_progress.xml` and added a Stop button so the user can cancel the
  retry cycle instead of waiting for it to exhaust. Guarded `AndruavWSClient` login
  `onSuccess` against `mkillMe` so a stopped retry doesn't proceed.
- **`e7c1f54`** — startup dialog now lists the specific denied permissions
  (`CheckAppPermissions.getMissingPermissions()`) instead of a generic error.
- **`d8088f1`** — adjusted `activity_first.xml` spacing/sizing across all size
  qualifiers; removed legacy `dialog_reminder.xml`; updated dialog strings.

### ⚙️ Settings / preferences rework

- **`08a9fb`** — fixed GPS Injection checkbox key mismatch (XML `key_gps_inject`
  vs. runtime `gps_inject` made the toggle a no-op); added mutual-exclusion with
  Ignore Mobile Sensors to prevent feeding stale GPS to the FC; removed dead
  `AndruavUDPModuleBase` and Comm-Module-IP preference plumbing; fixed RC-Cam
  channel validator Toast (1-18, not 1-16); added `wiki/RC-Channel-Triggers.md`.
- **`3d915a0`** — reworked telemetry/sensor/camera preferences to close
  remote-control gaps: dropped the manual Smart Telemetry Settings entry (level is
  driven by the `LVL` field on remote `TELEMETRYCTRL` messages); dropped the manual
  Auto Connect UDP Telemetry entry and defaulted it OFF; guarded
  `RemoteCommand_MAKETILT` against NPE on sensor objects never created because the
  pref disabled them; pruned the string resources used only by removed entries.

### 🐛 Telemetry / connection state fixes

- **`e3d74cb`** — fixed three bugs: (1) UDP telemetry never auto-started —
  auto-start/stop calls fired at `onOpen()` before the socket became
  `SOCKETSTATE_REGISTERED`, silently dropped; moved to a new `onRegistered` event
  posted when the server confirms registration. (2) FCB "Disconnect" left the tile
  green — `MainScreen` never subscribed to `GUIEvent_UpdateConnection` from
  `TelemetryModeer`; added the missing `@Subscribe` handler wired to
  `updateFCBButton()`. (3) Com tile stayed "Live" after Server Disconnect —
  `updateConnectionIconsStatus()` refreshed the server card but not module tiles;
  added `updateModuleTiles()`. Also restored `isAutoUDPProxyConnect` defaults to
  `true` in first-run / factory-reset paths.
- **`ef51de2`** — fixed COM port issue in `UsbHohoConnection` and
  `DroneKitServer`.
- **`28e95d6`** — improved Bluetooth FCB connection UI (`FcbConnectionSheet`) and
  added auto-connect on device select.

### 🧹 Code cleanup & tooling

- **`d418fee`** — `andruavProtocol`: removed 12 unused imports across 7 files and
  123 lines of commented-out dead code across 12 files; explanatory/design-rationale
  comments preserved.
- **`0e9019a`** — added the reusable `java-clean` skill
  (`.devin/skills/java-clean/`) bundling 4 Python scripts for unused-import and
  dead-code detection/removal (CRLF-safe).
- **`3d49f02`** — added `build_release_apk.sh` release build script.
- **`a288c23`** — added DeepWiki link to `README.md`.

---


