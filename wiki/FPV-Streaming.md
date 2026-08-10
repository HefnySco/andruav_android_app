# FPV & WebRTC Streaming

Five commits reworked how the FPV (camera) video stream survives Activity lifecycle
events and how WebRTC signaling avoids a startup race condition, plus two smaller
fixes to the streaming permission and UI.

## Foreground `FPVStreamingService` (`d0b00d7`)

**Problem:** the FPV activities tore down the WebRTC stream in `onPause()`. An
accidental power-button press against a drone mount, or the screen simply timing out,
killed live video mid-flight.

**Fix:** camera capture and local recording moved into a new `FPVStreamingService` — a
foreground service (`camera|microphone` type) that runs independently of any Activity's
lifecycle. The FPV activities now only attach/detach their preview renderer to/from the
already-running service. Streaming stops only via an explicit stop button, or the remote
`RemoteCommand_STREAMVIDEO Act:false` command — never as a side effect of the Activity
pausing.

Also added in the same commit:
- **Picture-in-Picture** on back/home instead of closing the camera — the stream floats
  like a video call and is closed via the PiP window's own button.
- A **green streaming indicator** on the home screen's FPV button, mirroring the
  existing FCB connection indicator.

## Restart-after-close bug (`6fecaf3`)

**Problem:** the stop funnel called `stopSelf()`, but a bound FPV Activity
(`BIND_AUTO_CREATE`) kept the service alive regardless — so `onDestroy()`, the only place
that cleared the running flag and announced the stop, never ran. The FPV screen stayed
open, `App.startFPVStreamingService()` became a permanent no-op, and no later remote
start could revive streaming until the user manually pressed exit on the phone.

**Fix:** the stop funnel now clears the running flag and posts the stop event inline
(on the main thread, since the funnel usually runs on the websocket thread) instead of
relying on `onDestroy()`. Also handles `_7adath_InitAndroidCamera` in both FPV activities,
so a remote start landing while the FPV screen is already in front restarts/rebinds the
service instead of being silently dropped — `MainScreen` unregisters from the event bus
in `onPause()`, so `BaseAndruavShasha` was previously the only subscriber able to act on
it.

## WebRTC signaling race condition (`1322719` + `8812673` + `d781c10`)

**Problem:** on a fresh connection, the remote peer sometimes received an offer with no
video track — working video only appeared after the peer disconnected and rejoined.

**Root cause:** `PnRTC_3ameel` (which registers the incoming-signaling EventBus
listener, including the `joinme` handler) was being instantiated *before*
`mediaStream`/`localVideoTrack` were fully constructed. If a `joinme` signal arrived in
that window, the resulting offer was built without a track attached.

**Fix, in three parts:**
1. **`1322719`** — added `postSticky` to the `IEventBus` abstraction (implemented in
   `App.java` and the `Dummy_EventBus` stub). A sticky-posted event is cached, so a
   subscriber that registers *after* the post still receives it immediately — closing
   the class of race where a signaling event arrives before the listener finishes
   initializing.
2. **`8812673`** — moved `PnRTC_3ameel` instantiation to *after*
   `attachLocalMediaStream()` completes, so the incoming-signaling listener can't fire
   until the track genuinely exists. Also updated `PeerConnectionClientBase`/
   `AndruavPeerConnectionClientClient` to coordinate with the new timing.
3. **`d781c10`** — `AndruavWSClientBase` updated to work with the sticky-posting
   mechanism and the deferred peer-connection timing.

**Result:** the remote peer now receives a working video track on the *first*
connection attempt — no rejoin needed.

## Microphone permission made optional (`05afb0e`)

Video streaming does not require microphone audio. `RECORD_AUDIO` is now optional —
FPV can launch without it. The app still surfaces the missing permission to the user,
but no longer blocks FPV startup on it.

## Camera streaming close button (`feda9ef`)

Added an explicit UI control to close camera streaming, rather than relying solely on
back/home (which, after the PiP change above, now floats the stream instead of closing
it).
