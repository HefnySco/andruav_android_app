# Andruav-AP Wiki

Technical deep-dives into recent engineering work on this repository. For a
chronological, commit-referenced summary, see [`../CHANGELOG.md`](../CHANGELOG.md).

## Pages

- [Architecture Migration](Architecture-Migration.md) — collapsing `ClientLib`'s AIDL/Binder
  layer to direct in-process calls; EventBus 2→3 and GreenDAO 2→3 runtime migrations.
- [Android OS Compatibility](Android-OS-Compatibility.md) — the permission-handling rewrite
  (non-blocking, Andruav-protocol `ERROR` reporting) and the API 24–34 compatibility audit
  (Bluetooth, notifications, scoped storage, foreground services).
- [FPV & WebRTC Streaming](FPV-Streaming.md) — the foreground `FPVStreamingService`,
  Picture-in-Picture, and the signaling race condition that could drop the first video
  track on connect.
- [UI Theme System](UI-Theme-System.md) — the dark "magnetic" theme's color palette,
  shared drawables, and the established per-screen restyle pattern.
- [Dependency Upgrades](Dependency-Upgrades.md) — full before/after version table with the
  reasoning behind each target version, plus what `enableJetifier` fixes and why.
