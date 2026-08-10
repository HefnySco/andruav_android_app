# Changelog: `Andruav_AP_Original` → `Andruav_AP_2026`

This document summarizes everything that changed between the `Andruav_AP_Original`
baseline (version 7.2.1, 2025-09-26) and `Andruav_AP_2026` (version 9.0.1, 2026-08-11).

**Scope:** 33 commits · 189 files changed · +3,881 / −7,559 lines (net code reduction,
despite the new features, from aggressive dead-code removal alongside the rewrite).

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
| 9.0.1 (current) | `7ddadbb` | 2026-08-11 |

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

## Contributors

Commits in this range include AI-assisted changes co-authored by Claude (Anthropic) and
one commit (`3f2dc71`) co-authored by Devin (Cognition), alongside manual engineering work.
