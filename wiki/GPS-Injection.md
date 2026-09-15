# GPS Injection — Phone GNSS as the Flight Controller's GPS

Andruav can feed the drone-side phone's own GNSS fix to an ArduPilot flight
controller as a MAVLink `GPS_INPUT` stream. With the FC's GPS type set to
MAVLink, the phone *is* the FC's GPS — either the only one, or a second GPS
next to a real receiver.

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
| Phone | Android version | 8.0 (API 26) or later — the checkbox is disabled below that |
| Phone | Location permission | precise location granted |

Once the FC is connected, the **GPS Injection** summary in Drone Settings shows
the FC's live GPS type: `✓ Flight controller GPS1_TYPE is set to MAVLink (14)`
when it will accept the stream, or a `⚠` warning with the current `GPS_TYPE` /
`GPS_TYPE2` values when injection would be silently ignored.

## Data path

```
GPS_PROVIDER fix ─► Sensor_GPS.onLocationChanged() ─► mLastGnssFix  (untouched copy, only while gps_inject is on)
NMEA GSA / GGA   ─► Sensor_GPS NMEA listener       ─► fix mode, HDOP, VDOP, geoid separation
GnssStatus       ─► Sensor_GPS status callback     ─► SatUsedInFixCount
                                    │
        10 Hz fixed-rate timer: ControlBoard_DroneKit.GPSInjectorRunnable
                                    │
                    ControlBoard_DroneKit.injectLatestGnssFix()
                                    │
        DroneKitServer.do_InjectGPS() ─► GPS_INPUT (MAVLink2, sysid 255 / compid 190) ─► FC
```

The timer is created in `ControlBoard_DroneKit.ActivateListener(true)` next to
the RC repeater and shut down in `ActivateListener(false)`.

### When a packet is sent

Every 100 ms `injectLatestGnssFix()` sends one `GPS_INPUT` only if **all** of
these hold; otherwise that tick is silently skipped:

1. The FC has `GPS1_TYPE` or `GPS2_TYPE` = 14 (`isFCConfiguredForGPSInjection()`,
   read from the FC parameter list).
2. Android 8.0+.
3. The `gps_inject` preference is on.
4. The DroneKit server exists.
5. `Sensor_GPS.getLastGnssFix()` is non-null and **younger than 2 s**
   (`GPS_INJECT_MAX_FIX_AGE_MS`), measured with `elapsedRealtimeNanos`.

### GNSS-only, and only while injecting

`Sensor_GPS` keeps two location streams apart:

- **The app's own location** (map, server reports) is unchanged: GPS and
  network/Wi-Fi providers, `getBestLocation()` picking the best last-known fix,
  speed recomputed and altitude rewritten relative to the ground. This is the
  behaviour both with injection off and with it on.
- **The injection copy** (`mLastGnssFix`) is taken at the very top of
  `onLocationChanged()`, only for `GPS_PROVIDER` fixes, only while `gps_inject`
  is enabled, and before any of the above rewrites. A Wi-Fi/cell fix can never
  reach the FC — its 20–100 m accuracy and jumps would fail the EKF's GPS
  checks, or worse, pull the vehicle's position estimate.

A consequence worth knowing: indoors with no GNSS fix, Andruav injects
nothing at all. That is intended.

## Timing

ArduPilot's `AP_GPS::is_healthy()` marks a GPS unhealthy unless the average gap
between messages stays under **215 ms** and no two consecutive gaps exceed
**245 ms** — i.e. at least 5 Hz. Phone GNSS delivers about **1 fix per second**,
and `requestLocationUpdates(minTime = 200)` is only a hint the chipset ignores.

So the latest fix is **re-sent at 10 Hz**:

- 10 Hz (not 5) leaves margin for executor and link jitter; the timer is
  `scheduleAtFixedRate` because health is judged on the *average* gap.
- Each re-send is stamped with the fix time **plus the fix's age**, so
  `time_week_ms` keeps advancing instead of repeating (ArduPilot runs it through
  its jitter correction).
- After 2 s without a new fix, sending stops; ArduPilot declares the GPS lost
  after its own 4 s `GPS_TIMEOUT_MS` (`GPS 1: Bad fix`, later
  `GPS 1: detected MAV` when the stream resumes).
- Between phone fixes the same position is repeated, so in fast flight the
  FC's GPS position steps about once a second.

## Field mapping

`Float.NaN` inside the app means "the phone did not report this value";
`DroneKitServer.do_InjectGPS()` turns every NaN into the matching
`GPS_INPUT_IGNORE_FLAG_*` bit instead of sending a 0.

| `GPS_INPUT` field | Source | When unavailable |
|---|---|---|
| `lat`, `lon` | `Location.getLatitude/Longitude() × 1e7` | — |
| `alt` | `getMslAltitudeMeters()` on API 34+, else `getAltitude() − GGA geoid separation` (AMSL) | ignored (`!hasAltitude()`) |
| `fix_type` | best NMEA GSA fix mode of the epoch across constellations; GGA quality when > 3 | — |
| `satellites_visible` | GnssStatus satellites **used in fix** (not satellites in view) | — |
| `hdop` / `vdop` | NMEA GGA field 8 / GSA field 17 | ignored when 0 |
| `horiz_accuracy` | `getAccuracy()` | ignored |
| `vert_accuracy` | `getVerticalAccuracyMeters()` | ignored |
| `speed_accuracy` | `getSpeedAccuracyMetersPerSecond()` | ignored |
| `vn`, `ve` | `speed × cos/sin(bearing)` | explicit `0, 0` when speed is exactly 0 with no bearing; ignored otherwise |
| `vd` | — | always ignored (Android reports no vertical speed) |
| `yaw` | phone compass + `GeomagneticField` declination, centidegrees from **true** north, `36000` = north | `0` = not sent (heading preference off, or no magnetometer) |
| `gps_id` | `0` if `GPS1_TYPE = 14`, else `1` if `GPS2_TYPE = 14` | — |
| `time_week`, `time_week_ms`, `time_usec` | fix UTC time → GPS time (+18 s leap seconds), advanced by fix age | — |

Why these rules matter on the FC side:

- **No zero accuracies.** A 0 accuracy passes EKF3's GPS checks as a perfect
  measurement. Unknown values are flagged ignored instead.
- **No stale velocity.** `AP_GPS_MAV` only updates velocity when a packet
  carries it and keeps the last value otherwise — hence the explicit zero for a
  stationary fix without bearing.
- **MAVLink2.** `yaw` is an extension field; the message is always packed as
  MAVLink2 so it reaches the FC.
- **Sender identity 255/190.** `GPS_INPUT` has no target; stamping it with the
  FC's own sysid/compid made ArduPilot's routing drop every packet as a
  loopback.
- **`gps_id` must match exactly.** `AP_GPS_MAV` drops packets whose `gps_id`
  is not its own instance; there is no broadcast value for `GPS_INPUT`.

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
  Andruav. `GPS_INPUT` from Andruav is forwarded onto it too.

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
| No GPS at all, Drone Settings shows `⚠ … GPS_TYPE=0` | FC GPS type not MAV | set `GPS1_TYPE`/`GPS_TYPE` = 14, reboot |
| No GPS at all, settings show `✓` | no GNSS fix on the phone (indoors), or fix older than 2 s | move the phone to open sky; check the phone's own GPS status |
| `PreArm: GPS 1: not healthy` | stream under ~5 Hz or gappy | check the link; Andruav sends 10 Hz |
| `GPS 1: Bad fix` then `GPS 1: detected MAV` | stream stopped ≥ 4 s (fix lost, app reconnect, link drop) | check phone GNSS and the FC link |
| `Need Position Estimate` / `AHRS: waiting for home` | EKF3 GPS checks failing — usually horizontal accuracy > 5 m | see the EKF3 table; `EK3_CHECK_SCALE` / `EK3_GPS_CHECK` |
| `Check mag field (xy diff …)` | compass vs expected field mismatch | real vehicle: move/recalibrate compass; SITL: `--home` at the phone |
| `GPS Glitch or Compass error` | position jump larger than the reported accuracy | better sky view; keep the phone still |

## Sources

- [`ControlBoard_DroneKit.java`](../app/src/main/java/ap/andruav_ap/communication/controlBoard/ControlBoard_DroneKit.java) — timer, gating, field mapping (`injectLatestGnssFix`), GPS type detection
- [`DroneKitServer.java`](../app/src/main/java/ap/andruav_ap/communication/telemetry/DroneKit/DroneKitServer.java) — `do_InjectGPS`, ignore flags, packing
- [`Sensor_GPS.java`](../andruavmiddlelibrary/src/main/java/ap/andruavmiddlelibrary/sensors/Sensor_GPS.java) — raw GNSS copy, NMEA parsing, used-in-fix count
- [`SettingsDrone.java`](../app/src/main/java/ap/andruav_ap/activities/settings/SettingsDrone.java) — preference, FC GPS type status in the summary
- ArduPilot: `libraries/AP_GPS/AP_GPS_MAV.cpp`, `AP_GPS.cpp` (`is_healthy`),
  `libraries/AP_NavEKF3/AP_NavEKF3_VehicleStatus.cpp` (`calcGpsGoodToAlign`),
  `ArduCopter/AP_Arming_Copter.cpp` (`mandatory_position_checks`)
