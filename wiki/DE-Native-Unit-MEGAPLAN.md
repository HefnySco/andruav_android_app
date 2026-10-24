# MEGAPLAN — Andruav as a native DroneEngage unit

Goal: the Android phone runs the **real** DroneEngage modules
(`de_comm` = `droneengage_communication_pro`, `de_mavlink` =
`droneengage_mavlink` incl. `mavlink_sdk`) unmodified, built with the
Android NDK, talking over their normal UDP databus on `127.0.0.1`, exactly as
on a Raspberry Pi. Android-only abilities (sound, screen/torch/vibrate,
phone health, camera, phone GPS) join that databus as a Java DE module.
ClientLib and the Java comm-server client are retired later, behind a
setting.

Background and the capability specs: [Mission-Planner-Capabilities-TOBE](Mission-Planner-Capabilities-TOBE.md)
(its D1 "seam layer" is **superseded** by this plan; D4 is folded into Plan A).

Written 2026-10-23. Tracked in TeamEngage project `andruav` (epics and task
ids are listed at the end once proposed).

---

## 1. The rule of this plan

**Plan A is the foundation and gates everything.** Nothing in Plans B, C or
D starts until Plan A's exit gate passes: the phone, running native
de_comm + de_mavlink, passes the same simulation_land scenarios as a Linux
unit. T0 is the only work that runs in parallel, because it fixes the Java
path users fly on today.

```
 T0 hygiene (Java path) ───────────────────────────────────────────── now
 Plan A  native core ── A1 → A2 → {A3, A4} → A5 → A6 ─┬─ A7 (de_mavlink alone)
                                         A5 → A8 (FC link bridge)
                                         A5 → A9 (sim: Android unit)
                        A7 + A8 + A10 (de_comm alone) → A11 (both + soak) = GATE
 Plan B  capability module ───────────── after GATE
 Plan C  app integration ─────────────── after GATE
 Plan D  missions & fleet validation ─── after GATE (+ B for capability tests)
 Plan E  STM32-F411 serial GPIO ──────── idea only (future)
```

## 2. Binding design decisions

| # | Decision | Why |
|---|---|---|
| N1 | **One Android process per DE module.** de_comm runs in a service with `android:process=":de_comm"`, de_mavlink in `":de_mavlink"`. | Each module keeps its own singletons and its own copy of `de_common` (no symbol clash). `exit()` inside a module only ends that process, like on Linux. A native crash never takes down the UI. Restart = start the process again, with fresh state. |
| N2 | **Modules are built as shared libraries with a C entry point.** Upstream adds, only when compiled with `-DDE_ANDROID`: `extern "C" int de_module_main(int argc, char* argv[])` (the body of today's `main`) and `extern "C" void de_module_request_stop(void)` (calls `de::shutdown::request()`). Linux builds are unchanged. | A host library loads the module `.so` with `dlopen` and calls these two symbols. No JNI code lives in the Linux repos. |
| N3 | **A small JNI host library (`libde_host.so`) in the app** does the Android-specific work: `chdir` into the module's folder, redirect `stdout`/`stderr` to logcat, `dlopen` the module, run `de_module_main` on a thread, forward stop requests. | Keeps every Android detail out of the upstream code. |
| N4 | **FC serial/Bluetooth reaches de_mavlink as TCP.** A Java "FC link bridge" opens USB-serial or Bluetooth and serves the raw byte stream on `127.0.0.1:5760`; de_mavlink uses its existing `fcb_connection_uri {"type":"tcp","ip":"127.0.0.1","port":5760}`. | Zero C++ change for serial. `serial_port.cpp` (termios on `/dev/tty*`) cannot work on Android without root. |
| N5 | **Source pinning by lock file + sync script**, not git submodules: `deNative/sources.lock` records the commit of each upstream repo; `deNative/sync_sources.sh` copies those commits from the sibling `../../drone_engage/` checkout into `deNative/external/` (gitignored). | The upstream repos are private (`origin_local` = `HefnySco/prv_*`); the Android repo must still clone and build its Java parts without them. Owner may switch to submodules later. |
| N6 | **Third-party libraries are built by a script, never committed**: OpenSSL 3, curl, Boost headers (asio/beast are header-only; Boost ≥ 1.74 required by de_comm). Output in `deNative/third_party/out/<abi>/` (gitignored). | Reproducible, no large binaries in git. |
| N7 | **ABIs: `arm64-v8a` (release) and `x86_64` (emulator, debug).** No 32-bit. | APK size; every phone that matters is arm64. |
| N8 | **Duplicate, then hide.** The Java comm client, `Andruav_Parser` and ClientLib keep working and stay the default until Plan C's parity check passes. A setting "Native unit" switches. Never both online with the same unit id. | Users keep flying during the migration. |
| N9 | **Namespaces** (owner decision 2026-10-23): FC outputs are `fc_io`, **owned by de_mavlink** (upstream, so Linux gets it too); phone outputs and health are `phone`; `gpio` is reserved for real pins (future STM32 board). | One name per thing on every platform. |

## 3. Plans

### T0 — Hygiene on the Java path (parallel, now)

Small fixes to the code users run today.

1. Merge the AN-0023 branch (`te/AN-0023-udp-relay-token`, commit `887a8eb`) into master.
2. Apply the 9008 relay token only for this unit (move `setToken` out of `AndruavSystem_UdpProxy.setMessageText`).
3. Fix 6511 text-to-speech encode/decode and honour language/pitch/volume.
4. Parse and handle 6512 play-file.

### Plan A — Native core foundation (the gate)

| Step | Task | Proves |
|---|---|---|
| A1 | NDK toolchain + `:deNative` Gradle module + hello JNI | the app can build and load native code |
| A2 | Third-party build script (OpenSSL, curl, Boost) | dependencies exist for both ABIs |
| A3 | de_comm upstream: `DE_ANDROID` entry points + Android CMake target | de_comm builds as `libde_comm.so`, Linux unchanged |
| A4 | de_mavlink upstream: same | de_mavlink builds as `libde_mavlink.so`, Linux unchanged |
| A5 | Source lock + sync script + native build wired into Gradle | `assembleDebug` produces an APK with both module libraries |
| A6 | `libde_host.so` + `DeModuleService` (process per module) + debug screen | each module starts, logs to logcat, stops, restarts after a crash |
| A7 | Test: de_mavlink alone on the phone, de_comm on the PC | Android de_mavlink = Linux de_mavlink on the sim suites |
| A8 | FC link bridge (USB-serial / Bluetooth → TCP 5760) | de_mavlink flies a real FC over USB and Bluetooth |
| A9 | simulation_land: Android device/emulator as a fleet unit | the sim can run scenarios with a phone unit |
| A10 | Test: de_comm alone on the phone, de_mavlink + SITL on the PC | Android de_comm = Linux de_comm on the sim suites |
| A11 | Test: both on the phone + 24 h soak | **GATE**: full suites pass; soak clean |

### Plan B — Android capability module (after the gate)

1. Java databus client (`:deDatabus`, port of `de_databus/python`).
2. Capability host (adverts 6542, invoke 6543 → result 6544, state 6545, `Sync_EventFire` 1061 consumer).
3. `sound` provider.
4. `phone` outputs: screen strobe, torch, vibrate (+ Emergency rewired to them).
5. `phone` health state and facts.
6. Camera: headless still capture.
7. Camera: `camera` advert, record choke point, events.
8. Phone GPS as the unit position (9102) when no FC position.
9. `fc_io` namespace in de_mavlink (upstream).

### Plan C — App integration (after the gate)

1. SPIKE: how the Java UI reads unit state in native mode (decision only).
2. Generate the module config files from the app settings.
3. "Native unit" setting with mutual exclusion of the two stacks.
4. UI wiring to the native unit (per the spike decision).
5. Parity checklist run on SITL and a real FC; owner decides the default.
6. Leave ClientLib and the Java comm client unreferenced (after one release with native as default).

### Plan D — Missions and fleet validation (after the gate)

1. Static-unit and sensor-lane suites with a phone unit.
2. Fleet bus suites (4A) with a phone unit.
3. Flight mission, payload and gate suites with a phone unit flying SITL.
4. New sim scenarios for the phone capabilities (sound, phone outputs, phone health).

### Plan E — STM32-F411 serial GPIO (future idea)

Recorded as a TeamEngage idea. See TO-BE §5.8.

## 4. Facts every task relies on (verified 2026-10-23)

- **Repos and builds**
  - de_comm: `droneengage_communication_pro`, CMake (`./build.sh`), needs Threads, CURL, OpenSSL, Boost ≥ 1.74 (asio + beast, header-only use). Unit tests: `make -C src/test test` (targets `sv-core`, `ev-bus`, … `static-location-policy`).
  - de_mavlink: `droneengage_mavlink`, CMake, needs Threads + OpenSSL; `mavlink_sdk/` and `c_library_v2/` are in-tree. Unit tests: `cd src/test && make && make test`.
  - Both use `src/de_common` at commit `71d0131`.
- **Entry points**: de_comm `src/main.cpp` — `init()` (calls `initArguments`, `de::shutdown::installHandlers()`, config load), `loop()` (exits on `de::STATUS::m_exit_me` or `de::shutdown::requested()`), `uninit()` ends with `exit(0)`, `main()` at line ~872. de_mavlink `src/main.cpp` — `init()`, `de::shutdown::waitForRequest()`, `uninit()` ends with `exit(0)`, `main()` at ~530. Arguments: `-c <config.json> -b <local file>`.
- **`exit()` sites** (fine under N1, but listed): de_comm `main.cpp` 651/655/659/662/864, `comm_server/andruav_auth.cpp` 39/46/109, `andruav_parser.cpp` 267, `de_broker/udpCommunicator.cpp` 63–106, `unixDgramCommunicator.cpp` 27, `abort()` in `andruav_comm_server*.cpp` after `max_offline_count`; de_common `configFile.cpp`, `localConfigFile.cpp`, `udpClient.cpp`, `de_message_parser_base.cpp`. de_mavlink `main.cpp` 316–330, 526.
- **Linux-only hardware** in de_comm: `hal_linux/rpi_gpio.cpp` maps `/dev/mem`; skipped when the config has `"led_pins_enabled": false` and `"buzzer_pins_enabled": false`. `/proc` and `/sys` reads (`util_rpi.cpp`, `de_cpu_monitor.cpp`, `de_module_health.hpp`) must fail soft.
- **Configs**: de_comm `de_comm.config.module.json` keys incl. `module_id`, `s2s_udp_listening_ip/port`, `auth_ip/port`, `auth_verify_ssl`, `userName`, `accessCode`, `unitID`, `groupID`, `led_pins_enabled`, `buzzer_pins_enabled`, `de_plan_file`, `de_package_file`, `de_run_file`, `de_journal_dir`. de_mavlink `de_mavlink.config.module.json` incl. `fcb_connection_uri {type: udp|tcp|serial, ip, port}` and `s2s_udp_listening_ip/port` (default 127.0.0.1:61003), `s2s_udp_target_ip/port` (127.0.0.1:60000). JSON with `//` comments; a `.local` file overrides keys.
- **Databus wire format** (for the Java client, Plan B1): UDP; each datagram = 2-byte little-endian chunk number + payload; chunk 0 starts a message, `0xFFFF` marks the last chunk; reference implementation `de_databus/python/udpClient.py`, `de_module.py`, `de_facade_base.py`, tests `test_de_module.py`, `test_cap_schema.py`.
- **simulation_land**: `./sim run <test.yaml>`; suites under `tests/` (`smoke_copters.yaml`, `missions/`, `static_units/`, `fleet_bus/`, `payload/`, `sound/`, `gpio/`, `udp_relay/`, `soak/`). Remote units were designed in `docs/HIL.md` (SL-0017): fcb over TCP to the host's SITL port, s2s stays local, server binds `0.0.0.0`.
- **Android app**: compileSdk/targetSdk 34, minSdk 24, AGP 8.4.2, Gradle 8.14, JDK 17; modules `:app :andruavProtocol :andruavmiddlelibrary :ClientLib :Mavlink :rangebar-release :webrtc`; prebuilt `.so` only in `webrtc/libs/`. The SDK at `local.properties` → `sdk.dir` has **no NDK and no CMake installed yet**. USB-serial driver code: `ClientLib/.../communication/connection/usb/` (`CustomProber`); Bluetooth: `ClientLib/.../connection/BluetoothConnection.java`, `app/.../telemetry/BlueTooth/Bluetooth.java`.

## 5. Gate definition (end of Plan A)

All of these, with evidence pasted into A11:

1. `tests/smoke_copters.yaml` with one of the three copters being the phone: pass.
2. Every yaml under `tests/missions/`, `tests/static_units/`, `tests/fleet_bus/`, `tests/payload/`, `tests/udp_relay/` with the phone as unit 1: same pass/fail set as with three Linux units (any difference explained and accepted by the owner).
3. 24 h soak on a real phone (screen off, on charger, LTE): no process restarts except injected ones; memory of each module process within ±10 % between hour 1 and hour 24.
4. Wi-Fi → LTE handover and back: de_comm reconnects within 30 s each time.
5. `adb shell am kill` / `kill -9` of each module process: restarted within the backoff, unit back online in the GCS within 30 s.

## 6. TeamEngage ids (proposed 2026-10-23)

Each task file carries the exact steps, files and acceptance checks. "Depends on" is a hard order.

| Plan | Id | Task | Depends on |
|---|---|---|---|
| — | AN-0024 | MEGAPLAN epic | — |
| T0 | AN-0025 | epic | — |
| T0 | AN-0026 | Merge the AN-0023 branch into master | — |
| T0 | AN-0027 | 9008 token only for this unit | AN-0026 |
| T0 | AN-0028 | 6511 TTS fix + language/pitch/volume | — |
| T0 | AN-0029 | 6512 play-file | — |
| A | AN-0030 | epic (gate for B/C/D) | — |
| A | AN-0031 | A1 NDK + `:deNative` hello JNI | — |
| A | AN-0032 | A2 third-party build script | AN-0031 |
| A | DE-0012 | A3 de_comm `DE_ANDROID` target | AN-0032 |
| A | MV-0019 | A4 de_mavlink `DE_ANDROID` target | AN-0032 |
| A | AN-0033 | A5 source lock + build in APK | DE-0012, MV-0019 |
| A | AN-0034 | A6 host lib + process per module | AN-0033 |
| A | SL-0095 | A9 sim: Android phone as a unit | AN-0034, SL-0017 |
| A | AN-0035 | A8 FC link bridge (USB/BT → TCP) | AN-0034 |
| A | AN-0036 | A7 test de_mavlink alone | SL-0095 |
| A | AN-0037 | A10 test de_comm alone | SL-0095 |
| A | AN-0038 | **A11 GATE** | AN-0036, AN-0037, AN-0035 |
| B | AN-0039 | epic | AN-0038 |
| B | AN-0040 | B1 Java databus client | AN-0038 |
| B | AN-0041 | B2 capability host | AN-0040 |
| B | AN-0042 | B3 sound | AN-0041 |
| B | AN-0043 | B4 phone outputs | AN-0041 |
| B | AN-0044 | B5 phone health | AN-0041 |
| B | AN-0045 | B6 headless still capture | AN-0038 |
| B | AN-0046 | B7 camera provider | AN-0041, AN-0045 |
| B | AN-0047 | B8 phone GPS as position | AN-0040 |
| B | MV-0020 | B9 `fc_io` in de_mavlink | AN-0038 |
| C | AN-0048 | epic | AN-0038 |
| C | AN-0049 | C1 SPIKE UI/video in native mode | AN-0038 |
| C | AN-0050 | C2 config generation | AN-0038 |
| C | AN-0051 | C3 "Native unit" setting | AN-0050 |
| C | AN-0052 | C4 UI/video/emergency wiring | AN-0049, AN-0051 |
| C | AN-0053 | C5 parity checklist | AN-0052, AN-0035 |
| C | AN-0054 | C6 hide ClientLib + Java client | AN-0053 |
| D | AN-0055 | epic | AN-0038 |
| D | SL-0096 | D1 static units with phone | AN-0038 |
| D | SL-0097 | D2 fleet bus with phone | AN-0038 |
| D | SL-0098 | D3 flight/payload with phone | AN-0038 |
| D | SL-0099 | D4 phone capability scenarios | AN-0042, AN-0043, AN-0044, SL-0095 |
| E | AN-0056 | IDEA: STM32-F411 serial GPIO | — |

### Related, separate plan: virtual FC serial links in DEStudio (SL-0100)

Not part of this megaplan; A8 (AN-0035) and the gate (AN-0038) can use it so
that no real flight controller is needed for the USB and Bluetooth checks.

| Id | Task | Depends on |
|---|---|---|
| SL-0100 | epic: per-unit `fcb_link` (pty / host_serial / bt_spp) between the FC client and SITL | — |
| SL-0101 | fcb_proxy `rate <baud>` throttle + `fcb_rate` fault | — |
| SL-0102 | `fcb_link: pty` — socat virtual serial device; de_mavlink on `type: serial` | SL-0101 |
| SL-0103 | `fcb_unplug` fault — the device disappears and comes back | SL-0102 |
| SL-0104 | `fcb_link: host_serial` — host USB-serial adapter crossed to the phone's adapter (`fc_client: external`) | SL-0102 |
| SL-0105 | `fcb_link: bt_spp` — host Bluetooth RFCOMM serial server | SL-0102 |
| AN-0057 | today's Java USB/Bluetooth path validated against SL-0104/SL-0105 (baseline for AN-0035) | SL-0104, SL-0105 |

Limit: Android's USB-host serial stack cannot be faked in software on a phone
or emulator, so the USB path needs two cheap USB-serial adapters and a crossed
cable (SL-0104); everything else is virtual.
