# Android OS Compatibility & Permission Handling

The app declares `minSdkVersion 24` / `targetSdkVersion 34` — a 10-API-level span across
which Android's permission model changed substantially several times (runtime
permissions in 23, scoped storage in 29/30, Bluetooth permission split in 31,
notification opt-in in 33). A compatibility audit found several places where the code
silently narrowed real-world compatibility rather than failing loudly, plus a permission
UX that blocked app startup unconditionally.

## Permission handling rewrite (`3f2dc71`)

**Before:** an unconditional permission popup fired on every app startup, blocking
progress until the user responded.

**After:** permissions are optional. The app starts and runs with partial or no
permissions granted. Behavior differs by connection state:
- **Connected to the Andruav server:** a missing permission triggers an `ERROR` message
  over the Andruav protocol instead of a blocking dialog — visible to whoever's
  operating the unit remotely, not just standing in front of the phone.
- **Not connected:** per-feature popups are shown at the point of use.

Key pieces:
- `CheckAppPermissions.isPermissionsOK()` became a pure check with no side effects.
  Added `ensurePermission()`, `reportMissingPermission()`, and
  `requestAllPermissions()` as explicit helpers instead of one method doing everything.
- `FirstScreen` now does a non-blocking permission ask with a real dialog, waits for
  `onRequestPermissionsResult`, and has a safety timer to prevent a deadlock if the
  callback never fires. It always proceeds to `MainScreen` regardless of the user's
  choice.
- `MainActivityBuilder` routes to `FirstScreen` only on first run or when a permission is
  actually missing — not on every launch.
- Per-feature guards (`Sensor_GPS`, Bluetooth, SMS, FPV, `App`) send an Andruav `ERROR`
  when connected and a permission is denied; otherwise skip silently or show a popup.
- The FCB Bluetooth radio button is disabled when BT hardware or the runtime permission
  is unavailable; `DroneKitServer` guards the `FCB_COM_BT` case the same way the existing
  USB case was already guarded.

## Broad compatibility fixes (`1072b6a`, phase 2)

| Area | Problem | Fix |
|---|---|---|
| Bluetooth | Zero `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` guards in `Bluetooth.java` — `SecurityException` crash on API 31+ | Added permission checks + exception handling on `Enable()`, `getBondedDevices()`, `cancelDiscovery()`, `Connect()` |
| Notifications | No `NotificationChannel` was ever created — silent failure on API 26+ | Channel created in `Notification.init()`; builder switched to the channel-aware constructor |
| Notifications | `POST_NOTIFICATIONS` declared but never requested — silent failure on API 33+ | Now requested in `CheckAppPermissions.isPermissionsOK()` |
| Storage | `FileHelper.GetFolder()`'s legacy public-storage path silently failed on API 30+ (`requestLegacyExternalStorage` is ignored once `targetSdk >= 30`; the code only special-cased API 29) | Switched to app-specific external storage (`getExternalFilesDir`) for API 29+ |
| Audio | `RECORD_AUDIO` used by `CameraRecorder`/`MediaAudioEncoder` but never declared or checked — crashed on every API level when that path ran | Declared in manifest, checked/requested in `FPVActivityFactory` alongside the existing `CAMERA` check |
| Media | `READ_MEDIA_VIDEO` was never checked — a copy-paste bug checked `READ_MEDIA_IMAGES` twice instead | Fixed the duplicate check |
| Bluetooth | `neverForLocation` flag was attached to `BLUETOOTH_CONNECT` instead of `BLUETOOTH_SCAN` (the permission it's documented for); unused `BLUETOOTH_ADVERTISE` was declared | Moved the flag, dropped the unused permission |

Later (`7ddadbb`, this range's final commit), `CheckAppPermissions` was further refined
to gate Camera/GPS/SMS permission requests behind `DeviceFeatures.hasCamera` /
`hasGPS`/`hasLocation` / `hasSMSCapabilities` checks, so a device genuinely lacking that
hardware (e.g. a GPS-less tablet acting as a display-only unit) isn't held up waiting for
a permission it has nothing to grant.

## Foreground service for sensor collection (`befb277`, phase 2 item 8)

`SensorService` (GPS/IMU/battery collection feeding the telemetry pipeline) was started
with plain `startService()` and never called `startForeground()`. On API 26+ this meant
its sensor/location updates could be throttled or killed once the app is backgrounded
mid-flight — exactly when the user has switched away from the app during a flight.

Fix: `SensorService.onStartCommand()` now calls `startForeground()` (idempotent, safe on
every restart) with a low-priority ongoing notification on the app's existing
`andruav_notifications` channel (widened from private to public so `SensorService`, in a
different package, can reuse it), and `stopForeground()` in both `shutDown()` and
`onDestroy()`. `App.startSensorService()` switched from `startService()` to
`ContextCompat.startForegroundService()`, which API 26+ requires when a
`startForeground()` call is imminent.

Manifest: added `android:foregroundServiceType="location"` and the matching
`FOREGROUND_SERVICE_LOCATION` permission directly to the app module (previously only
implicitly present via ClientLib's manifest merge). Uses notification id `120`, distinct
from `DroidPlannerService`'s `101`, since both services can be in the foreground
simultaneously during a flight (FCB link + phone sensors).

## Scoped-storage trip export (`09b7e24`, item 9)

A phase-2 fix for the API 30+ storage crash (above) had moved trip folders (per-trip KML
+ image + video export) to app-private storage. That fixed the crash but broke the
actual feature: files no longer survived uninstall or showed up in a normal File
Manager.

**Design:** on API 29+, the entire trip bundle (`path.kml`, `files/*.jpg`,
`video/*.mp4`) is written under `MediaStore.Downloads` with a shared `RELATIVE_PATH`
prefix (`Download/AndruavKML/<trip>/...`), keeping them real filesystem siblings. This
matters because `path.kml` references its images via a relative
`href="files/<name>"`, which would break if the files were split across separate
MediaStore collections (Images/Video/Downloads independently). Below API 29, behavior is
unchanged.

Touched: `FileHelper.java` (`createDownloadsEntry()`/
`markMediaStoreEntryComplete()`/`savePicToMediaStore()`/`buildTimestampedJpgName()`),
`KMLFileHandler.java` (SDK-branching in `openKMZ()`/`shutDown()`),
`MediaMuxerWrapper.java` (new `FileDescriptor` constructor — `MediaMuxer` needs
random-access write, not a plain `OutputStream`), `AndruavVideoFileRenderer.java` (the
primary video-recording path on modern devices), `Image_Helper.java`
(`FileDescriptor`-based `AddGPStoJpg()` overload, since MediaStore items have no real
filesystem path for EXIF GPS writing), and both FPV camera activities (branch
image-save/video-record call sites by `SDK_INT`).
