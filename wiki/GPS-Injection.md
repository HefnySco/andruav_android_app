# GPS Injection — Phone GNSS as the Flight Controller's GPS

Andruav can feed the drone-side phone's own GNSS fix to an ArduPilot flight
controller as a MAVLink `GPS_INPUT` stream. With the FC's GPS type set to
MAVLink, the phone *is* the FC's GPS — either the only one, or a second GPS
next to a real receiver.

The injection lives in the native `de_mavlink` module, so the exact same code
runs on the phone and on a Raspberry Pi. On the Pi the fix comes from a GNSS
receiver wired to a serial port instead of the phone's GNSS.

This page is the engineering reference: the data path, the timing and
data-quality rules ArduPilot enforces on an injected GPS, and how to test the
whole chain in SITL. The operator-facing manual is the *GPS Injection* page in
the Andruav wiki (`andruav_wiki/source/andruav-gps-injection.rst`).

## Setup at a glance

| Where | Setting | Value |
|---|---|---|
| Flight controller | `GPS1_TYPE` (≥ 4.6) / `GPS_TYPE` (older) | `14` (MAV) — phone is the primary GPS |
| Flight controller | or `GPS2_TYPE` / `GPS_TYPE2` | `14` — phone is the second GPS |
| Flight controller | reboot | required after changing the GPS type |
| Andruav | Drone Settings → *FCB & Telemetry* → **GPS Injection** | on (default) |
| Andruav | Drone Settings → *FCB & Telemetry* → **Ignore Mobile Sensors** | off — the two are mutually exclusive |
| Andruav | Drone Settings → *FCB & Telemetry* → **Inject Heading** | off unless the phone is rigidly fixed to the airframe |
| Andruav | Drone Settings → *FCB & Telemetry* → **RTK / NTRIP** | caster host / port / mountpoint / user / password |
| Phone | Android version | 8.0 (API 26) or later — the checkbox is disabled below that |
| Phone | Location permission | precise location granted |
| Pi / Linux unit | `de_mavlink.config.module.json` → `gnss_source` | see the *Raspberry Pi* section |

The **GPS Injection** summary in Drone Settings shows the live state from
`de_mavlink`: the FC's GPS type (`✓ FC GPS1_TYPE = MAV (14)` or a `⚠` warning
with the value it read), the injection state and `GPS_INPUT` rate, and the
NTRIP state. The **PHONE > GPS** tab shows the same status line live. The
webclient's GPS tile shows the injection status for Pi units that have no
phone screen.

## Data path

```
Phone GNSS fix  ─► Sensor_GPS (raw GPS_PROVIDER copy, only while gps_inject is on)
                ─► PhoneGnssFixSource ─► bus message 9103 (one send per new fix)
                ─► de_comm broker ─► de_mavlink  (gnss_source "type": "bus")
                                        │
Pi GNSS receiver ─► de_mavlink serial reader  (gnss_source "type": "serial")
                                        │
                          CGnssInjectManager ─► GPS_INPUT (MAVLink2,
                          sysid 255 / compid 190, re-sent at 10 Hz) ─► FC

NTRIP caster ─► de_mavlink native NTRIP v1 client ─► GPS_RTCM_DATA (233) ─► FC
bus message 6548 (module or GCS) ──────────────────► GPS_RTCM_DATA (233) ─► FC
```

`de_mavlink` reports the whole chain's state as bus message `6549`
(injection state, `GPS_INPUT` rate, FC GPS type, NTRIP state and throughput)
to the phone UI and the webclient.

### When a packet is sent

`de_mavlink` sends one `GPS_INPUT` every 100 ms while **all** of these hold;
otherwise that tick is silently skipped:

1. A GNSS source is configured (`gnss_source.type` = `bus` or `serial`).
2. The newest fix from that source is **younger than about 2 s** — a phone
   that lost its fix shows up on the FC as a lost GPS, not a position frozen
   in place.
3. The source fix passes the required-field check (lat/lon, altitude, fix
   type, satellites); a bad 9103 fix is dropped and logged.

On the phone side, `PhoneGnssFixSource` sends one 9103 per **new** fix, only
while `gps_inject` is on, and nothing when the newest fix is stale.

### GNSS-only, and only while injecting

`Sensor_GPS` keeps two location streams apart:

- **The app's own location** (map, server reports) is unchanged: GPS and
  network/Wi-Fi providers, `getBestLocation()` picking the best last-known fix,
  speed recomputed and altitude rewritten relative to the ground.
- **The injection copy** is taken at the very top of `onLocationChanged()`,
  only for `GPS_PROVIDER` fixes, only while `gps_inject` is enabled, and before
  any of the above rewrites. A Wi-Fi/cell fix can never reach the FC — its
  20–100 m accuracy and jumps would fail the EKF's GPS checks, or worse, pull
  the vehicle's position estimate.

A consequence worth knowing: indoors with no GNSS fix, nothing is injected at
all. That is intended.

## Timing

ArduPilot's `AP_GPS::is_healthy()` marks a GPS unhealthy unless the average gap
between messages stays under **215 ms** and no two consecutive gaps exceed
**245 ms** — i.e. at least 5 Hz. Phone GNSS delivers about **1 fix per second**.

So `de_mavlink` **re-sends the latest fix at 10 Hz**:

- 10 Hz (not 5) leaves margin for executor and link jitter.
- Each re-send is stamped with the fix time **plus the fix's age**, so
  `time_week_ms` keeps advancing instead of repeating (ArduPilot runs it
  through its jitter correction).
- After ~2 s without a new fix, sending stops; ArduPilot declares the GPS lost
  after its own 4 s `GPS_TIMEOUT_MS` (`GPS 1: Bad fix`, later
  `GPS 1: detected MAV` when the stream resumes).
- Between source fixes the same position is repeated, so in fast flight the
  FC's GPS position steps about once a second.

## Field mapping

The 9103 message carries the fix in compact fields; `de_mavlink`'s
`parseGnssFixJson` turns every missing optional value into the matching
`GPS_INPUT_IGNORE_FLAG_*` bit instead of sending a 0.

| `GPS_INPUT` field | 9103 field | When unavailable |
|---|---|---|
| `lat`, `lon` | `la`, `ln` (degE7) | — |
| `alt` | `a` (mm above MSL; Android computes AMSL via `getMslAltitudeMeters()` on API 34+, else `getAltitude() − GGA geoid separation`) | fix dropped — `a` is required |
| `fix_type` | `fix` (0 none, 2 2D, 3 3D, 4 DGPS, 5 RTK float, 6 RTK fixed) | — |
| `satellites_visible` | `ns` (satellites **used in fix**, not in view) | — |
| `hdop` / `vdop` | `hd`, `vd` (x100) | ignored |
| `horiz_accuracy` | `ha` (mm) | ignored |
| `vert_accuracy` | `va` (mm) | ignored |
| `speed_accuracy` | `sa` (mm/s) | ignored |
| `vn`, `ve`, `vdn` | `vn`, `ve`, `vdn` (mm/s) | ignored; the phone sends explicit 0 for a stationary fix without bearing |
| `yaw` | `yw` (cdeg, `36000` = north) | `0` = not sent (heading preference off, or no magnetometer) |
| `gps_id` | `gid` (`0` targets `GPS1`, `1` targets `GPS2`) | default `0` |
| `time_week`, `time_week_ms`, `time_usec` | `tw`, `tms`; `de_mavlink` computes them from UTC when missing | — |

Why these rules matter on the FC side:

- **No zero accuracies.** A 0 accuracy passes EKF3's GPS checks as a perfect
  measurement. Unknown values are flagged ignored instead.
- **No stale velocity.** `AP_GPS_MAV` only updates velocity when a packet
  carries it and keeps the last value otherwise — hence the explicit zero for
  a stationary fix without bearing.
- **MAVLink2.** `yaw` is an extension field; the message is always packed as
  MAVLink2 so it reaches the FC.
- **Sender identity 255/190.** `GPS_INPUT` has no target; stamping it with the
  FC's own sysid/compid made ArduPilot's routing drop every packet as a
  loopback.
- **`gps_id` must match exactly.** `AP_GPS_MAV` drops packets whose `gps_id`
  is not its own instance; there is no broadcast value for `GPS_INPUT`.

## NTRIP / RTCM corrections

`de_mavlink` carries a native NTRIP v1 client (`src/gnss/ntrip_client.cpp`).
It pulls RTCM3 corrections from a caster and sends them to the FC as
`GPS_RTCM_DATA` — the same injector the `6548` bus path uses. It is off by
default.

On the phone, the caster settings live in Drone Settings → *RTK / NTRIP* and
the app writes them into `de_mavlink`'s config at runtime (no restart). On a
Pi they are keys in `de_mavlink.config.module.json`:

```jsonc
"ntrip_enable": true,
"ntrip_host": "caster.example.com",
"ntrip_port": 2101,
"ntrip_mount": "MY_MOUNT",
"ntrip_user": "user",
"ntrip_password": "pass",
"ntrip_send_gga": true        // upload a GGA built from the FC GPS every 10 s
                             // (a VRS caster needs it)
```

- The password is write-only: it is never logged and never sent to a GCS.
- RTCM3 frames are fragmented into `GPS_RTCM_DATA` per the MAVLink rule
  (180-byte fragments, ≤ 720 bytes per frame — larger frames are dropped and
  counted as `drop_big` in the 6549 status; prefer MSM4 mountpoints over MSM7).
- Corrections can also arrive as bus message `6548` from a module or an
  authorized GCS — same output path.

## Raspberry Pi — a GNSS receiver on the unit

The same `GPS_INPUT` path works without a phone: `de_mavlink` reads a GNSS
receiver wired to the unit over a serial port. In
`de_mavlink.config.module.json`:

```jsonc
"gnss_source": {
    "type": "serial",        // none | bus | serial (default none)
    "port": "/dev/ttyUSB0",  // the receiver serial device
    "baud": 38400
}
```

- `none` (default): no injection. `bus`: fixes arrive as 9103 on the module
  bus (the phone path). `serial`: the wired receiver.
- The serial reader runs on its own thread — the main loop never blocks — and
  parses NMEA GGA (position, altitude, fix quality, satellites, HDOP), GSA
  (VDOP, fix dimension), RMC and VTG (ground velocity).
- A missing or busy port is not fatal: `de_mavlink` retries the open, logs the
  error and reports it in 6549 (`inj: "error"`).
- Config reload is live: a `type` or `port` change takes effect without a
  restart.
- Only one source is ever active — a bus fix and a serial fix are never mixed.

## What ArduPilot requires before it will use the phone

Receiving `GPS_INPUT` (`GPS 1: detected MAV`, fix shown in the GCS) is not the
same as the EKF *using* it. Three separate gates apply.

### 1. GPS health (rate)

Covered above — `PreArm: GPS 1: not healthy` means the stream is under ~5 Hz or
gappy.

### 2. EKF3 GPS alignment checks

EKF3 accepts a GPS only after **all** enabled checks pass continuously for
**10 s** (`NavEKF3_core::calcGpsGoodToAlign`). Until then the GCS shows
`PreArm: Need Position Estimate` / `PreArm: AHRS: waiting for home`; success is
`EKF3 IMU0 is using GPS` followed by home being set.

| Check (`EK3_GPS_CHECK` bit) | Limit at `EK3_CHECK_SCALE = 100` |
|---|---|
| 0 — satellites | ≥ 6 |
| 1 — HDOP | ≤ 2.5 |
| 2 — speed accuracy | ≤ 1.0 m/s |
| 3 — position accuracy | horizontal ≤ 5 m, vertical ≤ 7.5 m |
| 4 — yaw (compass innovations) | within limits |
| 5 — position drift on ground | ≤ 3 m |
| 6 — vertical speed on ground | ≤ 0.3 m/s (only when vertical velocity is supplied) |
| 7 — horizontal speed on ground | ≤ 0.3 m/s |

`EK3_GPS_CHECK` defaults to 31 (bits 0–4). `EK3_CHECK_SCALE` (50–200 %) scales
the accuracy limits; 200 doubles them.

**Phone accuracy is the usual blocker.** Measured on a recent Xiaomi phone next
to a window: 35–42 satellites, HDOP 0.4, vertical accuracy 2.5 m, speed accuracy
0.1 m/s — and horizontal accuracy steady around **9.9 m**, which fails bit 3
forever. Options:

- Real vehicle: `EK3_CHECK_SCALE = 200` (10 m limit) — a deliberate decision to
  fly on a ~10 m position estimate.
- Bench / SITL only: `EK3_GPS_CHECK = 23` (31 minus bit 3) — keeps the
  satellite, HDOP, speed and yaw checks.

The Copter-level check that prints "Need Position Estimate" gives no EKF reason
text, so compare the GCS values (`gpsh_acc`, `gpsv_acc`, `gpsvel_acc`,
`gpshdop`, `satcount` in Mission Planner's Status tab) against the table, or read
`XKF4.GPS` (failed-check bitmask) in the log.

### 3. Arming checks

- `PreArm: Check mag field (xy diff:N>100)` — the compass disagrees with the
  earth field expected at the GPS location (`ARMING_MAGTHRESH`, 100 mGauss). On a
  real vehicle: phone or wiring too close to the compass, or compass not
  calibrated with the phone mounted. In SITL: the simulator's start location
  differs from the phone's location (see below).
- `PreArm: High GPS HDOP` — HDOP above `GPS_HDOP_GOOD` (default 140 = 1.4) in a
  position mode.

## Testing in SITL

The DroneEngage Studio scenarios under `droneengage_studio/tests/gnss/` cover
the whole chain headlessly — a stub feeds 9103 fixes to `de_mavlink` and the
test asserts the FC's `fix_type` reaches 3:

```bash
cd droneengage_studio
./sim run tests/gnss/copter_gps_input.yaml   # copter, bus source -> GPS_INPUT
./sim run tests/gnss/rover_gps_input.yaml    # rover
./sim run tests/gnss/boat_gps_input.yaml     # boat
./sim run tests/gnss/pi_serial_gps.yaml      # all three via the serial source
./sim run tests/gnss/copter_rtcm_bus.yaml    # 6548 -> GPS_RTCM_DATA (byte check)
./sim run tests/gnss/copter_ntrip.yaml       # NTRIP caster -> GPS_RTCM_DATA
./sim run tests/gnss/copter_no_gnss.yaml     # baseline: fix stays < 3 with no source
```

`copter_ntrip` needs the fake caster running first:
`python3 tools/fake_ntrip_caster.py --port 2101 --mount STUDIO --user studio --password studio`.
The RTCM scenarios reassemble `GPS_RTCM_DATA` and compare the bytes to
`tests/gnss/fixtures/rtcm_fixture.bin` exactly.

### Manual phone test against SITL

SITL has its own simulated GPS, which must be switched off or the test proves
nothing. Parameter overlay (`andruav_gps_inject.parm`):

```
GPS1_TYPE        14      # 4.6+ name
GPS_TYPE         14      # pre-4.6 name (both listed so one file fits either version)
SIM_GPS1_ENABLE  0       # 4.6+ name
SIM_GPS_DISABLE  1       # pre-4.6 name
LOG_DISARMED     1
EK3_GPS_CHECK    23      # bench only: phone horizontal accuracy is ~10 m
```

Start SITL **at the phone's location** — SITL simulates its compass for its own
start location, and a mismatch with the injected GPS fails `Check mag field`:

```bash
cd ~/ardupilot
build/sitl/bin/arducopter --model + --speedup 1 -I 0 \
    --home <phone_lat>,<phone_lng>,<alt_msl>,0 \
    --serial1 udpclient:127.0.0.1:14550 \
    --defaults Tools/autotest/default_params/copter.parm,andruav_gps_inject.parm
```

- SITL waits (`Waiting for connection ....`) until something connects to
  **TCP 5760**. In Andruav's FCB connection screen choose WiFi/TCP and point it
  at `<computer_ip>:5760`. Andruav does not auto-reconnect when SITL restarts.
- `--serial1` gives a second MAVLink stream for watching without competing with
  Andruav. `GPS_INPUT` from the phone is forwarded onto it too.
- For the NTRIP leg, run `tools/fake_ntrip_caster.py` on the computer and
  `adb reverse tcp:2101 tcp:2101`, then point the phone's caster settings at
  `127.0.0.1:2101`.

Expected sequence: `GPS 1: detected MAV` → `EKF3 IMU0 is using GPS` →
`EKF3 IMU0 origin set` → home set → arming allowed.

Quick live check of what the FC receives (pymavlink):

```python
from pymavlink import mavutil
m = mavutil.mavlink_connection("udpin:127.0.0.1:14550", source_system=254)
m.wait_heartbeat()
while True:
    g = m.recv_match(type="GPS_INPUT", blocking=True)
    print(g.fix_type, g.satellites_visible, round(g.horiz_accuracy, 1),
          round(g.vert_accuracy, 1), round(g.speed_accuracy, 2), g.hdop)
```

## Troubleshooting

| Symptom (GCS messages) | Likely cause | Fix |
|---|---|---|
| No GPS at all, Settings shows `⚠ … not MAV (14)` | FC GPS type not MAV | set `GPS1_TYPE`/`GPS_TYPE` = 14, reboot |
| No GPS at all, settings show `✓` | no GNSS fix on the phone (indoors), or fix older than 2 s | move the phone to open sky; check the phone's own GPS status |
| Status line shows `timeout` | the source fix aged out — no fresh 9103 for ~2 s | check phone GNSS / the `andruav_phone` module is up |
| `PreArm: GPS 1: not healthy` | stream under ~5 Hz or gappy | check the link; de_mavlink sends 10 Hz |
| `GPS 1: Bad fix` then `GPS 1: detected MAV` | stream stopped ≥ 4 s (fix lost, app reconnect, link drop) | check phone GNSS and the FC link |
| `Need Position Estimate` / `AHRS: waiting for home` | EKF3 GPS checks failing — usually horizontal accuracy > 5 m | see the EKF3 table; `EK3_CHECK_SCALE` / `EK3_GPS_CHECK` |
| `Check mag field (xy diff …)` | compass vs expected field mismatch | real vehicle: move/recalibrate compass; SITL: `--home` at the phone |
| `GPS Glitch or Compass error` | position jump larger than the reported accuracy | better sky view; keep the phone still |
| 6549 `ntrip: error` / `auth_error` | caster unreachable, or wrong mountpoint/credentials | check the RTK / NTRIP settings and the caster |
| Pi: 6549 `inj: "error"` with `src: "serial"` | serial port missing or busy | check `gnss_source.port`/`baud`; de_mavlink retries the open |

## Sources

- `droneengage_mavlink`:
  [`src/gnss/fcb_gnss_inject.cpp`](../droneengage_mavlink/src/gnss/fcb_gnss_inject.cpp)
  (source gate, 10 Hz resend, GPS_INPUT encode, 6549 status),
  `src/gnss/gnss_fix.cpp` (9103 parse → ignore flags),
  `src/gnss/fcb_gnss_serial.cpp` + `src/gnss/gnss_nmea.cpp` (Pi serial source),
  `src/gnss/ntrip_client.cpp` (NTRIP v1 client → GPS_RTCM_DATA)
- `andruav_android_app`:
  [`PhoneGnssFixSource.java`](../app/src/main/java/ap/andruav_ap/denative/position/PhoneGnssFixSource.java)
  (one 9103 per fresh fix),
  [`Sensor_GPS.java`](../andruavmiddlelibrary/src/main/java/ap/andruavmiddlelibrary/sensors/Sensor_GPS.java)
  (raw GPS_PROVIDER copy, NMEA parsing, used-in-fix count),
  [`SettingsDrone.java`](../app/src/main/java/ap/andruav_ap/activities/settings/SettingsDrone.java)
  (preference, live FC GPS type + injection/NTRIP status from 6549),
  `PhoneGpsFragment.java` + `GnssInjectStatus.java` (status display)
- Protocol design: `droneengage_mavlink/wiki/GNSS-Injection-Design.md`
  (9103 / 6548 / 6549 wire fields, fragmentation rules)
- ArduPilot: `libraries/AP_GPS/AP_GPS_MAV.cpp`, `AP_GPS.cpp` (`is_healthy`),
  `libraries/AP_NavEKF3/AP_NavEKF3_VehicleStatus.cpp` (`calcGpsGoodToAlign`),
  `ArduCopter/AP_Arming_Copter.cpp` (`mandatory_position_checks`)
