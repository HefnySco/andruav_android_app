# Changelog: `Andruav_AP_Original` → `Andruav_AP_2026`

This document summarizes what changed between the `Andruav_AP_Original` baseline
(version 7.2.1, 2025-09-26) and `Andruav_AP_2026` (version 13.4.0, 2026-09-17).

It is written for users, not developers. If you want the full technical detail —
commit hashes, file names, and the reasoning behind each change — the git history
has it all.

For deeper write-ups of specific topics, see the [`wiki/`](wiki/) folder:
- [Architecture Migration](wiki/Architecture-Migration.md)
- [Android OS Compatibility](wiki/Android-OS-Compatibility.md)
- [FPV & WebRTC Streaming](wiki/FPV-Streaming.md)
- [UI Theme System](wiki/UI-Theme-System.md)
- [Dependency Upgrades](wiki/Dependency-Upgrades.md)
- [GPS Injection](wiki/GPS-Injection.md)
- [RC Channel Triggers](wiki/RC-Channel-Triggers.md)

---

## Version milestones

| Version | Date | Highlights |
|---|---|---|
| 7.2.1 (baseline) | 2025-09-26 | Original app |
| 7.5.x – 8.0.0 | 2026-08-08/09 | Internal cleanup and library upgrades |
| 9.0.x | 2026-08-09/11 | New dark user interface |
| 10.x | 2026-08-11/13 | New home screen, background FPV fixes |
| 11.0.x | 2026-08-15 | Settings rework, connection fixes |
| 11.1.x – 11.6.x | 2026-08-16/20 | Email login, screen sharing, API 34 compliance |
| 12.0.0 | 2026-08-20 | Distribution moved to SourceForge |
| 13.0.x | 2026-08-22/26 | Internationalization, home-screen info |
| 13.1.x | 2026-09-12/17 | GPS injection, NTRIP/RTK, performance |
| 13.4.0 (current) | 2026-09-17 | Link guardian, QR-code login, MAVLink update |

---

## The big rewrite (v7.2.1 → v9.0.1)

The app's internals were modernized end to end. Old libraries were replaced with
current ones, and a lot of dead code was removed — the app ended up smaller than
it started, despite the new features.

Highlights:

- **Permissions:** the app no longer blocks you at startup demanding every
  permission. It runs with whatever it has, tells you which features are limited,
  and asks for a permission only when you use the feature that needs it.
- **Reliability:** fixed several crashes and silent failures on newer Android
  versions (notifications, Bluetooth, storage, background services).
- **FPV streaming:** the camera now streams from a service instead of an
  activity, so turning the screen off or pressing the power button no longer
  kills your video feed. Picture-in-Picture is supported.
- **Battery:** the phone's own GPS/IMU sensors switch off automatically when the
  flight controller is already providing good position data, saving heat and
  battery.
- **New look:** a dark, modern theme applied across the app.

## v10.0.0 – v11.0.2 — home screen and polish

- **New home screen:** complete redesign with a cleaner layout and a Bluetooth
  connection panel for pairing with the flight controller.
- **Background streaming fixes:** remote "start streaming" commands now work
  even when the app is fully in the background — previously they were silently
  dropped, leaving the camera stuck off after a power-button press.
- **Battery exemption prompt:** the app can now ask Android to exempt it from
  battery optimization, so Doze mode doesn't kill telemetry mid-flight.
- **Nicer dialogs:** progress and connect dialogs restyled to match the theme,
  with a Stop button to cancel connection retries.
- **Settings cleanup:** removed or fixed several preferences that did nothing,
  and fixed a GPS-injection toggle that was wired to the wrong key.
- **Bug fixes:** UDP telemetry auto-start, FCB/Com tile status indicators, and
  assorted connection-state glitches.

## v11.1.0 – v11.6.x — real login, SMS, screen sharing

- **Proper login:** the drone login screen now uses your email + access code
  through the real authentication flow, instead of the old access-code-only
  lookup.
- **SMS commands:** new `AUTO X` command (switch to auto mode and jump to
  mission step X) and `HLP` (replies with the list of supported commands).
- **Screen sharing:** you can stream the phone's screen over the video link
  instead of the camera — useful for showing maps or other apps to the ground
  station.
- **Signal info:** the app reports more detail about the mobile connection
  (operator, country, data state) and sends signal status regularly.
- **Android 14 support:** fixed foreground-service restrictions so remote FPV
  start works on the latest Android — the app pops a full-screen notification to
  come forward when needed.
- **Cleanup:** removed the unused gamepad remote-control feature, GCS-mode
  screens, and several permissions the app didn't actually need.

## v12.0.0 – v13.0.x — distribution and languages

- **SourceForge:** the download/distribution link moved from Google Play to
  SourceForge.
- **Internationalization:** hardcoded English strings moved into translation
  files so the app can be properly localized.
- **Small additions:** the UDP telemetry endpoint is shown on the home screen,
  and error logs can be exported or deleted from the menu.

## v13.1.x — GPS injection, RTK, performance

- **GPS injection overhaul:** injecting the phone's GPS into the flight
  controller now actually works on modern ArduPilot — the messages were being
  silently dropped before. Optional compass-heading injection added, and
  multi-constellation receivers ($GNGSA/$GNGGA sentences) now parse correctly.
- **NTRIP / RTK support:** the app can pull RTCM3 correction data from an NTRIP
  caster and forward it to the flight controller's GPS for centimeter-level
  positioning, including GGA upload for VRS networks.
- **Faster and smoother:** fixed a duplicated video-encode step in FPV
  recording, moved image work off the render thread, and replaced a busy
  Bluetooth polling loop with a blocking read — less CPU, less battery.
- **Sound toggle:** a speaker button on the home screen mutes sounds and
  text-to-speech.

## v13.4.0 — always-on link and QR login (current)

- **Link guardian:** a dedicated background service now owns the server
  connection. It keeps the link alive, shows a single persistent status
  notification, reconnects when the network drops, and — new in this version —
  brings the link back after the app process is killed or the phone reboots
  (when autostart is enabled). Boot autostart, broken since Android 10, works
  again.
- **QR-code login:** scan a QR code from the WebClient's team-admin page to fill
  in your account, access code, and server settings in one shot.
- **Custom server:** you can point the app at your own (local) server instead of
  the cloud; the hub panel shows a red "custom" marker so you can tell at a
  glance.
- **MAVLink update:** the built-in MAVLink library was regenerated from the
  latest upstream definitions — new message types, dialects, and enums.

---
