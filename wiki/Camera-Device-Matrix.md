# Camera device test matrix

The phones every camera task in Plan E is signed off on. Mix of current
hardware, an old LEGACY-level device, and MIUI - the three places camera
behavior diverges in the field.

How to read a device's Camera2 hardware level:

```bash
adb shell dumpsys media.camera | grep -i 'hardware level'
# or on builds where the enum is truncated:
adb shell dumpsys media.camera | grep -A2 supportedHardwareLevel
```

## Devices

| # | Model | Android | Camera2 hardware level | Why it's in the matrix |
|---|-------|---------|------------------------|------------------------|
| 1 | Xiaomi 24030PN60G (Redmi Note 13 Pro class) | 16 | FULL-class (12 camera ids on a Qualcomm HAL; the Android 16 dumpsys truncates the enum name - confirm on device) | Xiaomi/MIUI representative + current flagship-class SoC. CONNECTED to the dev bench - AndruavCameraCapturer self-test ran 36 frames/3 s @1280x720 on it (2026-10-30). |
| 2 | Current flagship (pick a recent Pixel or Samsung) | 14-16 | FULL / LEVEL_3 | Mainline modern Camera2, latest stack. |
| 3 | Older phone on Android 8-10 | 8-10 | LEGACY | The worst case: LEGACY hardware level + old HAL. Camera1-fallback and edge-case coverage. |
| 4 | Mid-range Xiaomi/MIUI on Android 11-13 | 11-13 | FULL or LIMITED | MIUI camera quirks (aggressive HAL, permission prompts) differ from stock Android. |

Devices 2-4 are proposed picks - swap in whichever real phones are on hand;
what matters is covering a modern flagship, a LEGACY Android 8-10, and at
least one MIUI device (#1 already fills one Xiaomi slot).

## Parity checklist (run on every matrix device, both flag settings)

Every camera-related task is verified against this list on the app capturer
(`use_app_camera_capturer` ON) and, for parity, the fork path (flag OFF):

- [ ] FPV starts and plays in the webclient (Chrome and Firefox)
- [ ] torch on/off (legacy FLASHCAM command AND the camera-namespace invoke)
- [ ] zoom in/out (Event_CameraZoom AND the camera-namespace invoke) - picture visibly changes
- [ ] switch camera front/back during a live stream, peer does not drop
- [ ] record start/stop - MP4 lands and plays
- [ ] still capture - photo lands and opens
- [ ] PiP orientation: minimize into Picture-in-Picture, stream stays upright (the regression the fixed-orientation write fixes)
- [ ] flash status fact emitted (`sendCameraFlashStatus` reaches the GCS)
- [ ] zoom status fact emitted (`sendCameraZoomStatus` reaches the GCS)
- [ ] `adb logcat -s AndruavCamMetrics` shows `ttff_ms=<n>` on start and `freezes=<n> encoder_errors=<n>` on stop
- [ ] covering the camera / another app grabbing it produces `onCameraError` or `onCameraDisconnected` in logcat
