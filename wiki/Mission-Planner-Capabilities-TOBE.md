# Andruav × DroneEngage Mission Planner — Deep Review and TO-BE

Scope: what the Android app (master `407d709`) needs so a phone can take
part in v2 missions (MP-0074 Phase 4A, MP-0091 Phase 4B, MP-0128 Phase 6)
and the UDP-relay work (GL-0059), **as a first-class capability unit**:
sound, light (screen strobe / torch / FC LED-relay as "GPIO"), position,
camera, payload, and phone health.

Written 2026-10-23 against `drone_engage/Tasks/mission_planner/`
(`PROTOCOL.md`, `PHASE-2/3/4A/4B/6`, `Phase-3-Tasks/CAPABILITY_SCHEMA.md`,
`Static-Units/README.md`, `MODULE-EVENT-SURFACE-PLAYBOOK.md`) and the code of
`droneengage_sound_module`, `droneengage_rpi_gpio`, `droneengage_camera_2025`,
`droneengage_mavlink/src/payload`, `droneengage_communication_pro/src/de_mission_supervisor`.

---

## 1. Corrections to the earlier gap analysis

| Earlier claim | Verified state | Consequence |
|---|---|---|
| GL-0059 P3 (DEU1 token) is **done** on Android (AN-0023) | AN-0023 is `done` in TeamEngage, but commit `887a8eb` lives only on `te/AN-0023-udp-relay-token` (worktree `andruav_android_app_AN-0023`). **Not merged to master**; `grep DEU1` on master finds nothing. | Merge AN-0023 first; the "token overwrite from a foreign 9008" fix then goes on top. |
| "A full Java port of `sv_supervisor` is heavy" | It is ~31 k lines (sv_core 4.7 k, sensor lane 1.7 k, bus, world model, expr, alloc, approval…). **The pure cores depend only on STL + nlohmann/json**, and the shell (`sv_supervisor*.cpp`) touches de_comm through only **~20 singleton methods**. | A Java port is the wrong default. Build the supervisor from the **same C++ sources via the NDK** and implement the ~20 seams in an Android shim (§4.1). Phase 4B's `${…}` evaluator comes for free — no third implementation. |
| Sound already supported (6511/6512) | 6511 TTS: the dispatcher calls `SpeakNow(text)` and **drops `l`/`p`/`v`**. 6512 `Sound_PlayFile`: the class exists but is **neither parsed nor dispatched**. | Sound needs a real choke point, not just an advert. |

## 2. AS-IS — device capabilities, verified in code

| Area | What exists | Where | Gaps that matter for missions |
|---|---|---|---|
| **TTS** | Android `TextToSpeech`, queued / `SpeakNow` | `andruavmiddlelibrary/.../factory/tts/TTS.java` | No lang/pitch/volume from 6511. `AndruavMessage_Sound_TextToSpeech.getJsonMessage()` writes **volume under `"p"`** (overwriting pitch) and compares `language != ""` by reference. |
| **Sound clips** | `SoundPool` with 2 built-ins (`SND_ERR`, `SND_EMERGENCY` siren), looping siren | `SoundManager.java`, `Emergency.playSiren` | No named clip library, no started/finished, 6512 dead. Siren only via the `RemoteCommand_MAKEBEEP` toggle. |
| **Screen flashing** | `AlarmWidget` overlay added to `App.activeActivity` on `GUIEvent_EnableFlashing` | `BaseAndruavShasha`, `FPV*Activity`, `FlashActivity`, `Emergency.doFlash` | Works **only while an Andruav activity is in the foreground**; no colour/rate; only via the `RemoteCommand_MAKEFLASH` toggle; gated by emergency prefs. |
| **Torch (camera LED)** | 1051 `CameraFlash`, `FPV_CMD_FLASHCAM` → `PeerConnectionManager.setFlash` (Camera1/Camera2 TORCH) | `FPVStreamingService` | Only while the WebRTC camera session is live (`mPeerConnectionManager != null`); no `CameraManager.setTorchMode` path when idle. |
| **"GPIO"** | No phone GPIO. FC servo via 6001 `ServoChannel` → `Event_RemoteServo`. RC-channel triggers (RC Block, camera switch) in `DroneMavlinkHandler.execute_rc_channels` ([RC-Channel-Triggers](RC-Channel-Triggers.md)). | `MessageDispatcher:330`, ClientLib | No relay/servo as named resources, no events, no `DO_SET_RELAY`. |
| **Payload** | none (servo only) | — | No `DO_GRIPPER`, no approval-gated release. |
| **Position** | Phone GPS (`Sensor_GPS`, NMEA listener, foreground service type `location`), GPS mode AUTO / MOBILE / FCB, FC GPS via ClientLib | `andruavmiddlelibrary/sensors`, `AndruavUnitBase:208` | No 9102-equivalent into a supervisor, no position age (`pa`), no static-unit declaration. |
| **Camera** | WebRTC streaming (front/back, zoom, switch), recording (`FPVStreamingService.startRecording`), still images 1050 `Ctrl_Camera` → `FPV_CMD_TAKEIMAGE`, camera list | `FPVStreamingService`, `FPV*RTCWebCamActivity`, `CameraRecorder` | **Still-image capture is handled only inside the FPV activities** (`FPVDroneRTCWebCamActivity:363`), so a headless capture from a mission fails. Recording needs the streaming service up. No `camera.captured` with geotag. |
| **Phone health** | `BatterySensor` (level, charging, health, temp), `CommSignalsStatus`, IMU, compass | `sensors/power`, `textMessages` | Not exposed as state/events. |
| **Protocol** | Parser up to 1084 + 6001/6501/6511/9005–9008 | `Andruav_Parser.java` | **No 1061, 1085–1094, 1089.** 9007 `ts` ignored (only `s` read). |

## 3. What the mission planner expects from any unit

A unit participates through **four contracts**, all frozen on Linux:

1. **Capabilities (Phase 3)**: per namespace a `de.cap/1` advert; the GCS
   reads them through **1089** (`full` snapshot + deltas); actions are
   invoked by the supervisor with an idempotent `action_id`; module state is
   mirrored. A unit that does not answer 1089 shows **no capabilities**, so
   the compiler rejects any role with `requires: [...]` bound to it.
2. **Event surface (GE/EV playbook)**: named resources with commands
   `<ns>.<res>.<verb>` (unit-local `Sync_EventFire`, never from the server),
   facts `<ns>.<res>.went_*|changed|started|finished` (edge-only, one choke
   point), and per-resource `do` actions.
3. **Supervisor (Phase 2 + SU)**: 1085 package load, 1086 actions
   (start/stop/fire), 1087 status, 1088 journal; nav lane (flight) and
   sensor lane (≤ 4 sensor missions, `do`/`gate`/`emit`/`on_exit`/`repeat`).
4. **Fleet bus (Phase 4A)**: fleet clock from 9007/9005, 1090 events
   (dedupe by id, per-source seq, ack/retry, TTL, sticky facts), 1091 acks,
   1092 roster, 1093 world ops, 1094 prepare/ready/start/abort/authorize.
   Phase 6 then needs nothing extra from the unit.

## 4. TO-BE architecture

```
 ┌──────────────────────────── Andruav APK ─────────────────────────────────┐
 │  Java/Android                              │  NDK  libde_supervisor.so    │
 │                                            │  (same sources as de_comm)   │
 │  WS client / Andruav_Parser ──1085..1094──►│  CSupervisor + sv_core       │
 │        ▲            ◄── API_sendCMD ───────│  sensor lane, ev_bus, wm,    │
 │        │                                   │  expr, alloc, approval       │
 │  DeHost (JNI shim, ~20 seams) ◄───────────►│  CCapabilityRegistry         │
 │        │                                   │  cap_schema (de_common)      │
 │  CapabilityHost (in-process 6542–6545)     └──────────────────────────────┤
 │   ├─ SoundProvider      ns sound                                          │
 │   ├─ FcIoProvider       ns fc_io  (FC relay / servo / RC-channel input)   │
 │   ├─ PayloadProvider    ns payload (FC DO_GRIPPER / servo bay)            │
 │   ├─ CameraProvider     ns camera                                         │
 │   ├─ PhoneProvider      ns phone  (screen/torch/vibrate + health, facts)  │
 │   ├─ Stm32GpioProvider  ns gpio   (STM32-F411 over serial)  [future]      │
 │   ├─ FC core (NDK)      mavlink_sdk + de_mavlink nav adapter/payload/proxy│
 │   └─ DetectProvider     ns detect (on-device model)        [optional]     │
 │  PositionSource → 9102-equivalent (phone GPS / FC GPS / static)          │
 └──────────────────────────────────────────────────────────────────────────┘
```

### 4.1 Decision D1: reuse the C++ supervisor through the NDK (recommended)

> **Superseded 2026-10-23** by [DE-Native-Unit-MEGAPLAN](DE-Native-Unit-MEGAPLAN.md):
> the phone runs the whole de_comm and de_mavlink modules (one Android
> process each, over the normal localhost databus) instead of the
> supervisor plus a seam layer. D4 below is folded into that plan's Plan A,
> and the FC-output namespace `fc_io` is owned by de_mavlink. The rest of
> this document (capability specs, protocol needs) still applies.

| Option | Cost | Risk | Verdict |
|---|---|---|---|
| **A. NDK build of `de_mission_supervisor/` + `de_capability_registry` + `de_common` (cap_schema, geo projector)**, Android implementations of the ~20 de_comm seams | Medium: CMake in Gradle (`externalNativeBuild`), JNI shim, source sync | std::filesystem/plog on NDK r26 (fine), APK +3–5 MB, ABI testing | **Recommended.** One implementation, shared goldens, 4B free. |
| B. Java port | ~25–30 k lines of Java + all golden tests | Behaviour drift on every Linux change | Reject. |
| C. Phone as a module of a Linux de_comm (companion Pi) | Low for the phone | Doesn't help a phone-only unit | Side benefit: the same providers can later speak the UDP databus. |

Seams to implement in `DeHost` (from `grep getInstance()` in the supervisor):
`CAndruavCommServerManager::{API_sendCMD, queueStats, getCongestionLevel}`,
`CAndruavUnitMe::{getUnitInfo, getUnitLocationInfo}`,
`CConfigFile::GetConfigJSON`, `CMissionFile::getMissionFilePath`,
`CMissionManagerBase::clearModuleMissionItems`,
`CUavosModulesManager::{sendIntermoduleToModule, forwardOperatorEventToOwner, forwardCommandsToModules, processIncommingServerMessage, hasAliveModuleOfClass, aliveModuleIdsOfClass, positionAgeSeconds}`,
`CAndruavFacade::API_sendErrorMessage`, plus the registry (compiled in).
Provide Android headers with the same names (link-time substitution) so
`sv_supervisor*.cpp` compile **unmodified**. Everything crosses JNI as JSON
strings in the wire-message shape, so there is no new schema.

This is **not** a new shared library and not a fork. The Android build
compiles the existing `droneengage_communication_pro` sources as they are
(pinned by commit); the only Android-specific C++ is the seam layer above.
A fix in the Linux supervisor reaches Android by moving the pin. (The count
comes from single-line call sites; a few multi-line uses may add seams.)

### 4.2 Decision D2: providers are in-process "modules"

`CapabilityHost` replaces the UDP databus with direct calls but keeps the
**exact 6542–6545 semantics**:

```java
interface DeCapabilityProvider {
    List<JSONObject> adverts();                       // de.cap/1, one per ns
    void invoke(String id, String ns, String act,
                JSONObject params, long deadlineMs,
                ResultSink sink);                    // 6543 → 6544, idempotent by id
    void onEvent(String name, JSONObject ed, String nonce); // locally delivered 1061 for an owned ns
    JSONObject state(String ns);                      // 6545 mirror
}
```

- Idempotency ring per provider (last 256 ids → cached result).
- Invokes run on a provider worker, never on the WS thread.
- `fireEvent(name, payload)` returns its nonce (self-echo filter, GF-06).
- A server-sourced 1061 is dropped at the parser (GF-01/02).
- Legacy commands (6511, 6512, 1051, MAKEBEEP, MAKEFLASH, 1050, RECORDVIDEO)
  are re-routed into the **same choke points** with `src:"gcs"`/`"legacy"`.

### 4.3 Decision D3: everything must work headless

Missions run with the screen off and no activity in front. Move every
capability's execution into foreground services: camera capture/record into
`FPVStreamingService` (or a new `CameraControlService`, type `camera`),
position into `SensorService` (type `location`), the supervisor and providers
into a `DeUnitService` (type `specialUse`, already declared). The screen
strobe uses a dedicated `showWhenLocked`/`turnScreenOn` activity (reuse
`FlashActivity`) launched from the service, so it works on a locked phone.

### 4.4 Decision D4: replace ClientLib with de_mavlink's FC core (recommended, staged)

`mavlink_sdk` (~6.5 k lines, C++17, only POSIX + `c_library_v2`) is the
bottom layer of de_mavlink: ports, vehicle state, commands, mission and
parameter managers, FTP, tlog. On its own it only replaces what ClientLib
already does. The value is the de_mavlink logic **on top of it**, which
Android would otherwise have to rewrite in Java:

| de_mavlink part | Lines | What Android gains |
|---|---|---|
| `mission/` (`nav_adapter`, `mission_manager`, `mission_translator`, `saved_mission_guard`) | 6.3 k | the 6540/6541 nav adapter for tier T4: segment, upload-on-activate with readback, guided lease, step tracking |
| `payload/` (`fcb_gripper`, `gripper_advert`) | 0.5 k | `payload` namespace from §5.3 as-is |
| `udp_proxy/` (incl. `udp_proxy_token`, MV-0017) | 0.5 k | DEU1 token and relay pinning without a separate Android port (AN-0023 becomes a fallback); MAVLink 2 signing (MV-0018) later on the same code |
| `geofence/`, `fcb_traffic_optimizer`, `fcb_modes`, `px4_modes.h` | 2 k | the same fence behaviour, telemetry rates and mode tables as Linux units |
| `de_pilot/`, `tracking/`, `precland/`, `swarm/` | 7 k | optional later: tracking/precland/swarm parity |

**How.** Same pattern as D1: build `mavlink_sdk` + the selected de_mavlink
folders into the same NDK library, and replace the de_mavlink-to-de_comm
seams (`CFCBFacade::sendErrorMessage`, `sendMissionItemSequence`,
`sendSyncFireEvent`, the `CModule` databus) with in-process calls. The nav
adapter then talks to the supervisor directly, with no UDP databus.

**Ports.** UDP and TCP run natively. Serial **cannot** use `serial_port.cpp`
(termios on `/dev/tty*` needs root on Android). Add a `JniPort` that
implements `mavlinksdk::comm::GenericPort` (`read_message`/`write_message`/
`start`/`stop`) and moves raw bytes to and from the existing Java USB-serial
(`CustomProber`) and Bluetooth code. Parsing stays in C++.

**Java side.** The UI and the Andruav telemetry code read DroneKit state
(`mDrone.getAttribute` in 28 places) and call ~30 DroneKit methods, almost
all inside `ControlBoard_DroneKit` (2.3 k lines) and `DroneKitServer`
(2.0 k lines). Add a `ControlBoard_DeMavlink` behind the same control-board
interface, fed by `CMavlinkEvents` callbacks (`OnArmed`, `OnModeChanges`,
`OnMissionCurrentChanged`, `OnWaypointReached`, `OnParamReceived`…) into a
Java state mirror.

**Migration: duplicate, then hide.** Keep `ControlBoard_DroneKit` and
ClientLib, and pick the board with a setting (default DroneKit). Switch the
default only after the parity checklist passes on SITL and on a real FC
over USB and Bluetooth. Then leave ClientLib unreferenced, and delete it in
a later release. Parity checklist (every `mDrone.*` the app calls today):
connect/disconnect, set mode (Copter/Plane/Rover/PX4), arm, takeoff, climb
to, goTo, turnTo, guided velocity (local and global frame), mission
load/set/start/speed, set home, parameters read/write/refresh, set servo,
camera trigger, gimbal start/orientation, manual control (RC override),
raw MAVLink send plus observers (used by TrafficOptimizer and the UDP proxy).

**Risks.**
- The DroneKit API surface the UI uses must be fully mirrored, or screens go
  blank. The duplicate-then-hide setting is the safety net.
- Some calls may have no `mavlink_sdk` equivalent yet (check gimbal, manual
  control, guided velocity in a global frame). Add any missing ones to the
  SDK upstream so Linux gains them too, rather than patching them on Android.
- One more native component on a flying path: a native crash takes the FC
  link down. Run the native core in the unit service, with a watchdog that
  restarts the link; the FC's own failsafes cover the gap.

## 5. Capability specs (TO-BE adverts)

Names, reserved actions, verbs and payloads match the Linux modules, so a
mission written for a Pi unit runs unchanged on a phone.

### 5.1 `sound` — parity with `droneengage_sound_module`

```jsonc
{ "schema":"de.cap/1", "ns":"sound", "module":"andruav_android", "ver":"<app ver>",
  "desc":"events: sound.<clip>.play|stop (commands), sound.<clip>.started|finished (facts)",
  "actions": {
    "say":  { "params": { "text":{"type":"string"},
                          "lang":{"type":"enum","values":[…device TTS locales…],"optional":true},
                          "language":{"type":"enum","values":[…],"optional":true},
                          "pitch":{"type":"int","min":0,"max":99,"optional":true},
                          "volume":{"type":"int","min":0,"max":100,"optional":true} } },
    "play": { "params": { "file":{"type":"enum","values":["siren","beep","error", …user clips…]} } },
    "stop": {},
    "siren": { "params": { "loop":{"type":"bool","optional":true} } }   // one per-clip action per clip
  },
  "state": { "playing": {"type":"bool"} },
  "resources": { "siren":"clip", "beep":"clip", "error":"clip" },
  "event_patterns": {
    "*.play":     { "kind":"command", "payload":{"loop":{"type":"bool","optional":true}} },
    "*.stop":     { "kind":"command" },
    "*.started":  { "kind":"fact", "payload":{"src":{"type":"string"},"loop":{"type":"bool"}} },
    "*.finished": { "kind":"fact", "payload":{"src":{"type":"string"},"reason":{"type":"string"}} } } }
```

Android implementation:
- **Clip library**: built-ins (`siren`, `beep`, `error`) plus user files in
  `getExternalFilesDir("sounds")`, configured as `sound_files[] {key?, name, file, events?}`
  (same key/slug rules as `sound_events.clip_key`).
- **Choke point**: one `SoundWorker` thread with a bounded FIFO. Every
  surface (invoke, `sound.<clip>.play` event, 6512, MAKEBEEP siren) ends there
  and emits `started`/`finished {reason: done|stopped|preempted|limit|error}`;
  `loop_max_s` default 60.
- `say` → `TTS` with `setPitch`, per-utterance volume (`KEY_PARAM_VOLUME`),
  locale; muted (home-screen sound toggle) → `{queued:false, muted:true}`.
- Audio focus + a `STREAM_ALARM` option so a siren is audible when media is muted.
- Fix the 6511 encoder bug and wire 6512.

### 5.2 Outputs — `phone` resources, `fc_io`, and `gpio` kept for real pins

Owner decision (2026-10-23): FC outputs get their own `fc_io` namespace, and
`gpio` stays reserved for **real pins**. A planned STM32-F411 serial GPIO
extension (§5.8) will own `gpio` on a phone, and the same board can extend a
Raspberry Pi. So phone-local outputs (screen, torch, vibrate) also stay out
of `gpio`: they are resources of the `phone` namespace. All three use the
same GE verbs and facts, so missions look the same whichever one they drive.

| Namespace | Resource key (default) | Class | Backed by | `on` / `off` | `pulse {ms}` | Notes |
|---|---|---|---|---|---|---|
| `phone` | `screen` | `out` | strobe activity (§4.3) | start / stop strobe | strobe for ms | config `strobe_hz` (default 4), `color` (`#FFFFFF`), full brightness |
| `phone` | `torch` | `out` | `CameraManager.setTorchMode`; if WebRTC owns the camera → `PeerConnectionManager.setFlash` | torch on / off | on for ms | `strobe_hz` (0 = steady) |
| `phone` | `vibrate` | `out` | `Vibrator` | pattern on / off | vibrate ms | |
| `fc_io` | `relay_<n>` | `out` | FC `MAV_CMD_DO_SET_RELAY` (ClientLib now, the FC core after D4) | relay 1 / 0 | pulse | only with an FC; `active_low` honoured |
| `fc_io` | `servo_<ch>` | `pwm` | FC `MAV_CMD_DO_SET_SERVO` (the 6001 path) | `on_width` / 0 | — | `set {v}` maps 0..1024 onto a configured µs range (documented, not raw µs) |
| `fc_io` | `rc_<ch>` | `in` | FC `RC_CHANNELS` (reuse the edge-triggered checker in `DroneMavlinkHandler.execute_rc_channels`) | — | — | threshold per channel → `went_on/went_off` |
| `gpio` | STM32 pins | `out`/`pwm`/`in` | STM32-F411 over serial (§5.8) | | | future |

`fc_io` is not Android-only: de_mavlink can advertise the same namespace
later, so the name and verbs should be identical on both.

Config: `phone_outputs[]` `{name, kind: screen|torch|vibrate, pulse_ms?, events?, strobe_hz?, color?}`
and `fc_io_pins[]` `{name, kind: relay|servo|rc, index, active_low?, pulse_ms?, events?, on_width?, threshold?}`
(the de_rpi_gpio field names wherever they apply).

Advert (both namespaces): `write {pin, value}`, `pulse {pin, length_ms}`,
per-resource `<ns>.<key> {on, pulse_ms?}` (`timeout_s: 5` when pulsing), PWM
`<ns>.<key> {pwm, value?}`; `resources` + `event_patterns` as in the `gpio`
example of CAPABILITY_SCHEMA.md; facts `went_on|went_off|changed {v, src}`
with `src = legacy|invoke|event|pulse|input|emergency`.

Rules carried over from GE: one choke point per resource with a generation
counter (an explicit write cancels a pulse restore), a second pulse is
rejected, boot is silent, the first input reading is a silent baseline, and
per-resource actions are flagged `claim:"action"` so a strobe mission and a
siren mission run concurrently in the sensor lane.

The legacy **Emergency** logic (battery/connection emergency, MAKEFLASH,
MAKEBEEP) becomes a client of `phone.screen` / `phone.torch` / `sound.siren`,
so GCS buttons, missions and emergencies share one state and one set of facts.

### 5.3 `payload` — parity with `droneengage_mavlink/src/payload`

`release {bay?}` (frozen, `timeout_s: 5`) and `payload.<bay> {}` per bay,
backed by FC `MAV_CMD_DO_GRIPPER` (instance) or a servo bay
(`release_width`/`rest_width`/`pulse_ms`). Facts `payload.released {t, bay?}`
and `payload.<bay>.released {t, src}`. **No command events**: every release
is a `do` so it passes the approval gate. Advertised only with an FC and a
configured bay.

### 5.4 `camera` — parity with `droneengage_camera_2025`

```jsonc
"actions": {
  "capture": { "params": { "count":{"type":"int","min":1,"max":100,"default":1},
                           "interval_ms":{"type":"int","min":0,"max":60000,"default":0,"unit":"ms"},
                           "channel":{"type":"string","optional":true},
                           "gcs_preview":{"type":"bool","default":false} }, "timeout_s":10 },
  "record":  { "params": { "on":{"type":"bool"}, "channel":{"type":"string","optional":true} }, "timeout_s":10 },
  "back":  { "params": { "record":{"type":"bool"} } },      // per-channel actions
  "front": { "params": { "record":{"type":"bool"} } },
  "zoom":  { "params": { "level":{"type":"number","min":1,"max":10}, "channel":{"type":"string","optional":true} } }  // Android extra, additive
},
"events":  { "captured": { "payload": { "file":{"type":"string"}, "pos":{"type":"geo"},
                                        "t":{"type":"number"}, "n":{"type":"int"}, "ch":{"type":"string"} } } },
"state":   { "recording": {"type":"bool"} },
"resources": { "back":"channel", "front":"channel" },
"event_patterns": { "*.capture":{"kind":"command"}, "*.record_on":{"kind":"command"},
                    "*.record_off":{"kind":"command"},
                    "*.went_recording":{"kind":"fact"}, "*.went_idle":{"kind":"fact"} }
```

Android work:
- Channels `back`, `front` (+ `usb_<n>` for UVC); keys stable by facing, not index.
- **Headless capture/record** (D3): move `FPV_CMD_TAKEIMAGE` handling out of
  the FPV activities into the camera service; grab from the WebRTC capturer
  when it owns the camera, otherwise open a Camera2 `ImageReader`.
- A `setRecording(ch, on, cause)` choke point → `went_recording`/`went_idle
  {src, reason, file}`; keep capturing while recording (a viewer disconnect
  must not stop the MP4).
- `captured` carries a **geotag** from `PositionSource` plus heading/attitude
  for the projector later.
- Torch is **not** a camera action: it is `phone.torch` (one owner per
  resource); the camera provider arbitrates the device with it.

### 5.5 Position — phone GPS as the unit's position source

Not a capability namespace (on Linux: de_mavlink 9102 → de_comm). TO-BE:

- `PositionSource` selects by `GPSMode`: FCB → FC `GLOBAL_POSITION_INT`;
  MOBILE → phone `Sensor_GPS`; AUTO → FC when its fix ≥ 3D, else phone. It
  feeds `DeHost.getUnitLocationInfo` and `positionAgeSeconds`.
- Static declaration (`location.static: true` + lat/lng/alt) for a phone used
  as a mast → SU-03 behaviour. A non-declared unit never falls back to the
  configured site; it reports `pa` when stale (SU-06).
- Fix type, accuracy and source are published as `phone` state so missions
  can gate on `phone.gps_ok`.
- Phone-GPS changes that feed the FC `GPS_INPUT` apply only when `gps_inject`
  is on ([GPS-Injection](GPS-Injection.md)); otherwise keep the existing
  Wi-Fi/network location logic.

### 5.6 `phone` — device health as state + facts (new, Android-only)

```jsonc
{ "ns":"phone", "state": { "battery":{"type":"int","unit":"%"}, "charging":{"type":"bool"},
                           "temp_c":{"type":"number"}, "net":{"type":"enum","values":["none","wifi","lte","5g","other"]},
                           "rssi_dbm":{"type":"number","unit":"dBm"}, "gps_ok":{"type":"bool"},
                           "gps_src":{"type":"enum","values":["phone","fc","static","none"]} },
  "events": { "battery_low":{"payload":{"v":{"type":"int"}}},
              "overheat":{"payload":{"temp_c":{"type":"number"}}},
              "net_lost":{}, "net_back":{} } }
```

The same namespace also owns the `screen`/`torch`/`vibrate` output
resources of §5.2 (`resources` + `event_patterns`, per-resource actions).

Thresholds in config; edge-only with hysteresis. A static phone mission can
then `on_exit` cleanly or report before the battery dies.

### 5.7 `detect` — optional, later

An on-device person/vehicle detector (TFLite/NNAPI) producing observations
projected to `detect.detected {cls, pos, unc, conf}` with the de_common
projector (already pulled in by the NDK build). It owns `detect` only when no
other owner exists (S14). This makes a phone a real "spotter" for 4B
confirmation policies.

### 5.8 `gpio` — STM32-F411 serial GPIO extension (future)

Future plan, not scheduled. A small STM32-F411 board gives a phone real pins
(relays, LEDs, buttons, PWM), and the same board can extend a Raspberry Pi.

- **Link**: USB-CDC serial (the phone already has USB-serial probing for the
  FC link in `CustomProber`; this needs a second, independent port) or UART
  on a Pi. A small framed protocol: write / pulse / PWM / read, input-edge
  reports with a timestamp, and a hello with the board's pin map and
  firmware version.
- **Namespace**: the board's host side owns `gpio`, with the exact
  de_rpi_gpio surface (pin keys, `on/off/toggle/pulse/set`,
  `went_on/went_off/changed`, `write`/`pulse`/per-pin actions,
  `payload_release` bays). Missions written for a Pi's GPIO run unchanged.
- **Where the logic lives**: debounce, pulse restore and the generation
  counter run on the MCU (hard timing); naming, adverts and events run on
  the host. On a Pi this is a driver backend of de_rpi_gpio, next to
  WiringPi. On Android it is a `Stm32GpioProvider` that reuses the
  same `gpio_caps` rules.
- **Safety**: the firmware drives every output to its rest level when the
  host goes silent (heartbeat timeout), so a crashed app never leaves a
  relay on.

## 6. Protocol work in the Android app

| Item | Detail |
|---|---|
| Message classes | `AndruavMessage_DE_*` for 1085–1094 (+ 1061 local only); raw JSON passthrough to the native lib, no field re-modelling in Java |
| Permission gate | 1085/1086/1090(GCS)/1093/1094 need `PERMISSION_ALLOW_GCS_WP_CONTROL`; 1091/1092 from a GCS dropped; server-sourced 1061 dropped |
| Send classes | 1090–1094 and system messages on a `critical` lane ahead of telemetry in the WS sender (mirror TASK-07); 1092 `latest_wins`, coalesced |
| Fleet clock | Parse `ts` in 9007 and the 9005 pong; offset + `tq`/`terr` per CLOCK.md (math in native `ev_clock`) |
| 1089 | Answer `{r:true}` with a `full` snapshot; push deltas on provider change (state ≤ 2/s) |
| Chunking | 1085 and 1094 `prepare` reuse the 1085 size limits and chunking |
| Version gate | Accept exactly the `de.package` versions the native lib implements (2.0/2.1 at T2, 2.2 at T3, 2.3 at T5) |

## 7. Delivery plan (tiers, each useful on its own)

| Tier | Content | Outcome | Size |
|---|---|---|---|
| **T0 — hygiene** | Merge AN-0023 to master; fix 9008 `setToken` applied from a 9008 addressed to another unit; fix the 6511 encoder (`"v"`, `language.isEmpty()`) and honour `l/p/v`; parse + dispatch 6512 | GL-0059 genuinely done on Android | S |
| **T1 — capability unit** | `CapabilityHost` + providers `sound`, `phone` (screen/torch/vibrate + health), `fc_io` (FC relay/servo/RC input), `camera` (headless), `payload`; choke points + facts; legacy commands rerouted; 1089 reply; settings UI for `sound_files`/`phone_outputs`/`fc_io_pins` | The planner shows the phone's actions with forms; roles can bind to it; GCS buttons and emergencies share one state | L |
| **T2 — sensor unit (static)** | NDK `libde_supervisor` + `DeHost` seams; 1085/1086/1087/1088; sensor lane only (`vehicle: static`, `de.package/2.1`); persistence in the app files dir; `PositionSource` + static declaration | The phone runs "keep watching and report" missions: siren + strobe on an event, photo bursts, TTS announcements, GPS beacon | L |
| **T3 — fleet participant** | Fleet clock; 1090/1091 (dedupe, ack/retry, TTL, sticky facts); 1092 roster from `PositionSource` + `phone`; 1093 consume; 1094 prepare/ready/start/abort | The phone joins multi-unit operations: emits facts others trigger on and triggers on theirs (cross-unit = fact → local `do`) | M (logic is native) |
| **F — FC core swap (D4)** | NDK `mavlink_sdk` + de_mavlink `mission/`, `payload/`, `udp_proxy/`, `geofence/`, modes; `JniPort` for USB and Bluetooth; `ControlBoard_DeMavlink` behind a setting; parity checklist; then ClientLib hidden | One FC stack for Linux and Android; DEU1 token and signing come from the shared code | L (can run in parallel with T1/T2 once the NDK skeleton from T2 exists) |
| **T4 — flight unit** | Enable de_mavlink's nav adapter (from F) against the native supervisor; PAUSED policy and contingencies; SITL scenarios in simulation_land | Phone-as-companion flies v2 flight missions exactly like de_mavlink | M (was XL with a Java nav adapter) |
| **T5 — dynamic missions** | Enable 2.3 (templates); evaluator/generators/alloc/approval are already in the native lib; add the `detect` provider | The phone takes allocated sectors / confirms targets | M (+ detector) |
| **Phase 6** | Nothing on the unit; webclient live-run, approvals and replay read 1087/1088/1090–1094 | — | — |

Recommended order: **T0 → T1 → T2 → T3**, with **F** in parallel once the NDK skeleton exists; T4 then becomes a medium step instead of a rewrite. T1
alone ends the "Android unit shows no capabilities" problem and fixes the
headless-camera and sound bugs.

### Acceptance (per tier, checkable)

- **T1**: the webclient unit panel lists `sound`, `phone`, `camera`
  (+ `fc_io` and `payload` with an FC); a planner `do: phone.screen {on:true, pulse_ms:2000}`
  form renders with no webclient change; MAKEFLASH from the GCS emits
  `phone.screen.went_on {src:"legacy"}`; capture works with the screen off.
- **T2**: a phone version of the SU "keep watching" example
  (`gate: phone.battery_low` → `do: sound.say` + `emit`) runs from a 1085
  upload, journals every step, survives an app restart (re-sync from disk),
  and 1087 shows the sensor-lane missions.
- **T3**: in simulation_land with two Linux SITL units and one phone
  emulator, a phone-emitted declared fact releases a barrier on both drones
  exactly once with 30 % packet loss; the phone shows in the roster with `tq=2`.
- **T4**: Phase 2 exit criteria 1–5 pass on SITL through the phone.

## 8. Risks and open questions

1. **NDK source sync**: the supervisor moves fast. Pin it as a submodule of
   `droneengage_communication_pro` (or a scripted copy, like
   `sync_de_common_url.sh`) and run its pure unit tests on the host in CI.
2. **Background limits**: Doze and OEM battery killers stop services; unit
   mode needs a battery-optimization exemption and per-OEM notes.
3. **Screen strobe on a locked phone**: `showWhenLocked`/`turnScreenOn` from
   a foreground-service-launched activity works on API 29+, but
   `USE_FULL_SCREEN_INTENT` is restricted on API 34; verify on target devices.
4. **Torch vs camera contention**: one owner of the camera device; the camera
   provider must arbitrate WebRTC, ImageReader and torch.
5. **Namespace ownership (decided 2026-10-23)**: FC outputs are `fc_io`,
   phone outputs are `phone.*` resources, and `gpio` is reserved for real
   pins (the future STM32 board). On a phone bridged to a Pi, the Pi's
   de_rpi_gpio and the phone never collide.
6. **Vocabulary (decided 2026-10-23)**: the Android additions (`phone`
   namespace with its output resources, `fc_io`, `camera.zoom`) go into
   PROTOCOL.md, marked as planned until the Android implementation lands.
