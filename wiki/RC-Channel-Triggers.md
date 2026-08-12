# RC Channel Triggers — Blocking & Camera Switch

Andruav monitors the flight controller's live RC channel values and uses two of
them as hardware triggers driven from the user's physical RC transmitter:

1. **RC Block** — a "kill switch" channel that, when high, freezes out all
   software/GCS control of the drone and forces it back to the physical
   transmitter.
2. **RC Camera Switch** — a channel that flips the phone's streaming camera
   between front-facing and back-facing whenever it crosses a threshold.

Both features live in the **Drone Settings** screen (`SettingsDrone`), are gated
by a master enable checkbox, and share the same trigger model: pick a channel
number, pick a PWM threshold, the feature activates when
`channelsRaw[channelNum-1] >= threshold`. They differ in *what* they activate.

## Shared polling loop

Both triggers are evaluated inside the MAVLink RC-channels handler, not on a
timer. `DroneMavlinkHandler.execute_rc_channels()` copies every incoming
`RC_CHANNELS` message's `chan1_raw..chan18_raw` into the static
`channelsRaw[]` array, then — once every 5 messages — calls both checkers:

```java
if ((rcChannelBlock_trials % 5) == 0) {
    ((ControlBoard_DroneKit) ...FCBoard).checkBlockingMode();
    ((ControlBoard_DroneKit) ...FCBoard).checkRCCamSwitch();
}
```

The 1-in-5 throttle avoids re-evaluating the trigger on every single
`RC_CHANNELS` frame (which arrives at ~10–50 Hz depending on the FC); a ~2–10 Hz
effective check rate is more than enough for a switch transition and keeps the
event bus quiet. Both checkers are edge-triggered internally (see below), so the
throttle does not affect debounce behavior — it just rate-limits polling.

Source: [`DroneMavlinkHandler.execute_rc_channels`](../app/src/main/java/ap/andruav_ap/communication/controlBoard/mavlink/DroneMavlinkHandler.java).

## RC Block

### What it does

When the configured RC channel goes above its threshold, the drone enters
**blocked mode**: all software-originated RC commands (GCS joystick, gamepad,
Andruav remote-control messages) are refused, and any in-flight software RC
override is forcibly released. The drone effectively becomes pilotable *only*
from the physical RC transmitter. When the channel drops back below threshold,
software control is allowed again.

This is a safety feature — think of it as "the pilot always wins". A common
setup is to wire a 2-position switch on the TX to channel 8 with a threshold of
~1800; flipping it up takes the drone back from any GCS that may be flying it
via Andruav.

### Settings (under *System Recovery*)

| Setting | Key | Default | Notes |
|---|---|---|---|
| Enable RC Block | `RFdWXmZaN0` | `false` | Master enable. Styled red/bold in the UI to flag it as a safety-affecting toggle. |
| RC Block TX Channel | `key_block_channel` | `8` | 1–16. Validator rejects out-of-range with a Toast. |
| RC Block Channel PWM min | `key_block_pwm_min` | `1800` | Threshold (≥ this = "blocked"). The "min" in the name is historical; it's a threshold, not a floor. |

### Runtime path

`ControlBoard_DroneKit.checkBlockingMode()`:

1. If `Preference.isRCBlockEnabled(null)` is false → call
   `do_RCChannelBlocked(false)` to make sure blocked mode is cleared, and
   return. This guarantees that disabling the setting at runtime also releases
   an active block.
2. Otherwise, read `channelsRaw[channelNum-1]` and compare against
   `getChannelRCBlock_min_value(null)`.
3. Call `do_RCChannelBlocked(block)` with the resulting boolean.

`ControlBoardBase.do_RCChannelBlocked(boolean)` is the single source of truth
for the blocked state. It is **edge-triggered** — it only acts when the new
value differs from the current `rcChannelBlock`:

- On a rising edge (false → true), it sends three
  `RC_SUB_ACTION_RELEASED` RC-channel messages to the FC (triplicated for
  reliability over a possibly-lossy link) so any active GCS/gamepad override is
  dropped, then speaks "Blocked" via TTS.
- On a falling edge (true → false), it speaks "Unblocked".
- Either way, it updates `rcChannelBlock` and propagates to
  `mAndruavUnitBase.setisGCSBlockedFromBoard(block)`, which is what the rest of
  the app reads.

Source: [`ControlBoardBase.do_RCChannelBlocked`](../andruavProtocol/src/main/java/com/andruav/controlBoard/ControlBoardBase.java).

### Where the block is enforced

Three call sites check `do_RCChannelBlocked()` (the no-arg getter) and refuse
to act while blocked:

1. **`TelemetryProtocolParser.onEvent(Event_SocketData)`** — drops incoming
   socket data destined for the FC unless the event is marked
   `byPassBlockedGCS`. This is the gate that stops GCS-sent MAVLink packets
   from reaching the FC.
2. **`DroneKitServer.sendSimulatedPacket(...)`** — same gate for the DroneKit
   path; only forwards to `ExperimentalApi.getApi(mDrone).sendMavlinkMessage`
   if `byPassBlocked` is set or the board is not blocked.
3. **`ControlBoard_DroneKit.onEvent(Event_Remote_ChannelsCMD)`** — drops
   incoming Andruav remote-control channel commands from other units (e.g. a
   GCS driving the drone via Andruav's virtual joystick), unless the sender is
   this unit itself.

Additionally, `adjustRCActionByMode()` forces any RC action to
`RC_SUB_ACTION_RELEASED` while blocked, so even locally-generated software RC
can't sneak through.

### Visibility to other units

`setisGCSBlockedFromBoard(block)` updates the unit's
`m_isGCSBlockedFromBoard` flag, which is:

- Serialized into `AndruavMessage_ID` and broadcast to the group, so GCS
  clients display the blocked state (the web client shows it as a "hand" icon
  in the unit list via `SlidingAndruavUnitItem`, and the unit info widget shows
  a blocked indicator).
- Read by `AndruavUnitBase.isAvailableForFCBTelemetry()` to skip FCB-IMU
  fusion while blocked.

## RC Camera Switch

### What it does

When the configured RC channel goes above its threshold, the phone's active
streaming camera flips between back-facing and front-facing. This is purely a
camera selection feature — it does not affect flight control. The flip is
edge-triggered: holding the switch high keeps the current camera; toggling it
back and forth swaps each time.

A common use case: the phone is mounted with the rear camera pointing forward
for FPV, and a spare TX switch is wired to flip to the front camera for a quick
"look back" or pilot-selfie view without touching the phone.

### Settings (under *FPV Settings*)

| Setting | Key | Default | Notes |
|---|---|---|---|
| Enable RC CAM Switch | `sw_cam_rc_en` | `false` | Master enable. |
| RC CAM Channel | `sw_cam_rc_num` | `8` | 1–18. Validator allows 18 (extended MAVLink channels); the in-app Toast now correctly says "1 to 18" (it previously said "1 to 16" — a copy-paste from the RC Block validator, which is genuinely 1–16). |
| RC CAM PWM min | `sw_cam_rc_pwm` | `1800` | Threshold (≥ this = "switched on"). Same "min is really a threshold" naming quirk as RC Block. |

### Runtime path

`ControlBoard_DroneKit.checkRCCamSwitch()`:

1. If `Preference.isRCCamEnabled(null)` is false → return immediately. No
   state change, no event.
2. Read `channelsRaw[channelNum-1]` and compare against
   `getChannelRCCam_min_value(null)`.
3. Compute `rcCamera_temp = button_on ? 1 : 0`.
4. **Only if `rcCamera != rcCamera_temp`** (edge — the perceived switch state
   just changed) → update `rcCamera` and post
   `Event_FPV_CMD.FPV_CMD_SWITCHCAM` on the event bus.

`FPVStreamingService` handles `FPV_CMD_SWITCHCAM` by calling
`mPeerConnectionManager.switchCamera()`, which flips the active Android camera
for the WebRTC stream. The FPV activities intentionally do *not* handle this
event themselves — a comment in both `FPVDroneRTCWebCamActivity` and
`FPVModuleRTCWebCamActivity` notes that `SWITCHCAM`/`FLASHCAM`/`RECORDVIDEO`
are handled by the service directly to avoid being executed twice.

Source: [`ControlBoard_DroneKit.checkRCCamSwitch`](../app/src/main/java/ap/andruav_ap/communication/controlBoard/ControlBoard_DroneKit.java),
[`FPVStreamingService`](../app/src/main/java/ap/andruav_ap/services/fpv/FPVStreamingService.java).

### Scope

- **Phone cameras only.** The class doc on `checkRCCamSwitch` explicitly says
  "Only Mobile camera is affected by this command." External/USB cameras
  aren't part of this switch.
- The initial `rcCamera` value is derived from
  `(Preference.getCameraNumber(null) + 1) % 2` in the constructor, so the first
  edge transition will always produce a visible flip regardless of which
  camera was selected at startup.

## Differences at a glance

| | RC Block | RC Camera Switch |
|---|---|---|
| Purpose | Safety: revoke software RC control | Convenience: flip phone camera |
| Settings group | System Recovery | FPV Settings |
| Channel range | 1–16 | 1–18 |
| Edge-triggered | Yes (TTS on change) | Yes (only flips on transition) |
| Side effects on activate | Sends 3× `RC_SUB_ACTION_RELEASED` to FC, TTS "Blocked" | Posts `FPV_CMD_SWITCHCAM`, no FC traffic |
| Visible to other Andruav units | Yes (via `AndruavMessage_ID.isGCSBlocked`) | No (local camera only) |
| Affects FC-bound traffic | Yes (drops GCS→FC packets while blocked) | No |

## Validation in `SettingsDrone`

Both channel-number fields have `OnPreferenceChangeListener`s that parse the
input as an integer and reject out-of-range values with a Toast, returning
`false` so the preference is not persisted with a bad value. The PWM fields
validate against `Constants.Default_RC_MIN_VALUE`/`Default_RC_MAX_VALUE`. The
battery-min field (unrelated to these triggers, but in the same screen) is
validated 0–100.

Note: the validators use `Integer.parseInt`, so non-numeric input will throw
and propagate up to the preference framework rather than showing the Toast.
This matches the existing style across the screen and is acceptable because
the `inputType` on the recovery fields is `phone` (numeric keypad), but it is
a known rough edge if the FPV channel fields are ever given free-text input.
