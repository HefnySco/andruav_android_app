# UI Theme System

Version 9.0.0 (`b9a4af2`) introduced a dark, gradient-based visual language — internally
referred to as the "magnetic" dark theme — replacing the app's original
`Theme.AppCompat.Light.DarkActionBar` look (light backgrounds, default Material blue).
The base Android theme (`AppTheme` in `styles.xml`) was **not** changed; instead, each
screen opts in by setting a gradient drawable as its root layout background and using a
shared palette of colors/drawables. This keeps the change low-risk (no
`Theme.MaterialComponents.*` migration, no app-wide attribute reinterpretation) while
still reaching every screen incrementally.

## Where the palette lives

`app/src/main/res/values/colors_main_screen.xml`:

```xml
<!-- Palette for the redesigned MainScreen ("magnetic" dark theme) -->
<color name="main_bg_top">#0C1526</color>
<color name="main_bg_bottom">#141F38</color>

<color name="card_surface">#17223C</color>
<color name="card_surface_light">#1E2C4C</color>
<color name="card_stroke_subtle">#2C3E63</color>

<color name="header_title">#F2F5FA</color>
<color name="header_subtitle">#8CA0C4</color>

<color name="chip_bg">#182747</color>
<color name="chip_border">#2E4470</color>
<color name="chip_icon">#5EA8FF</color>
<color name="chip_text">#C6D4EE</color>

<color name="accent_imu">#4FC3F7</color>       <!-- IMU module -->
<color name="accent_imu_dark">#1D8FD6</color>
<color name="accent_fpv">#B388FF</color>       <!-- FPV module -->
<color name="accent_fpv_dark">#7C4DFF</color>
<color name="accent_com">#2FE0BF</color>       <!-- COM module -->
<color name="accent_com_dark">#0EA98F</color>
<color name="accent_fcb">#FFB74D</color>       <!-- FCB module -->
<color name="accent_fcb_dark">#F5891F</color>

<color name="badge_bg">#33FFFFFF</color>
<color name="badge_bg_pressed">#4DFFFFFF</color>
<color name="btn_glow_active">#3E75D375</color>
<color name="btn_glow_error">#3EF75050</color>
<color name="disabled_surface">#0F1729</color>
<color name="disabled_stroke">#1C2740</color>
<color name="disabled_text">#5B6B8C</color>
```

Each of the app's four vehicle-control modules (**IMU**, **FPV**, **COM**, **FCB**) has
its own accent color pair, used for that module's home-screen tile and any screen that
belongs to it — this is how a screen visually signals which subsystem it belongs to at a
glance.

## Shared drawables

| Drawable | Purpose |
|---|---|
| `bg_main_gradient.xml` | The root gradient (linear, 270°, `main_bg_top` → `main_bg_bottom`) — set as `android:background` on a screen's outermost view |
| `chip_info_background.xml` | Rounded (14dp radius) card surface with a `chip_border` stroke — used for text inputs, range-bar tracks, list backgrounds |
| `section_header_bg.xml` | Same shape as above but tuned for section-title bars (10dp radius) |
| `dialog_card_bg.xml` | Card surface for dialogs (About dialog, confirmation dialogs) |
| `header_logo_glow.xml` | Glow effect behind the app logo on the home screen header |
| `badge_icon_imu.xml` / `badge_icon_fpv.xml` / `badge_icon_com.xml` / `badge_icon_fcb.xml` | Per-module tile icons |
| `sel_segment_tab_bg.xml` / `sel_segment_tab_text.xml` | Selector states for segmented-tab controls (e.g. FCB connection-type tabs) |
| `sel_main_btn_text.xml` | Selector for the big home-screen action-button text color |

## The established pattern

Looking at any already-themed screen (`activity_imu.xml`, `content_fcb__drone.xml`,
`activity_hubcommunication.xml`, `activity_drone_login.xml`) shows the same recipe:

- Root layout: `android:background="@drawable/bg_main_gradient"`.
- Section titles: a `TextView` with `android:background="@drawable/section_header_bg"`,
  `android:textColor="@color/chip_icon"`, bold, centered.
- Text inputs: `android:background="@drawable/chip_info_background"`,
  `android:textColor="@color/chip_text"`, `android:textColorHint="@color/header_subtitle"`.
- Checkboxes: `android:textColor="@color/chip_text"` +
  `android:buttonTint="@color/chip_icon"` (first established in
  `activity_hubcommunication.xml`'s `hubactivity_chkLocalServer`).
- Dividers: 1dp `View` with `android:background="@color/chip_border"`.
- Segmented choices (e.g. connection type): `RadioButton` with `android:button="@null"`,
  `android:background="@drawable/sel_segment_tab_bg"`,
  `android:textColor="@color/sel_segment_tab_text"`.

## Screens themed so far

| Screen | Module accent |
|---|---|
| MainScreen (home) | mixed (per-tile) |
| IMU calibration | `accent_imu` |
| FPV / camera | `accent_fpv` |
| Hub / login (`activity_hubcommunication.xml`, `activity_drone_login.xml`) | — |
| FCB drone connection (`content_fcb__drone.xml`) | `accent_fcb` |
| About dialog | — |
| **RC Settings** (`activity_remote_control_setting_activity_tab.xml`,
  `fragment_remote_channels_setting.xml`, `widget_remote_channels_config.xml`) | `accent_fcb` |

### RC Settings restyle detail (`7ddadbb`)

RC channel configuration conceptually belongs to the FCB (flight-control-board) module,
so it adopted the `accent_fcb`/`accent_fcb_dark` pair rather than the generic
`chip_icon` blue:

- `activity_remote_control_setting_activity_tab.xml` — `bg_main_gradient` added to the
  `ViewPager` root (the ViewPager fills the entire content frame, so this is enough to
  theme the whole screen — no per-fragment background needed as long as fragments stay
  transparent).
- `fragment_remote_channels_setting.xml` — the "RC Channels Setting" title switched from
  a flat `btn_TXT_BLUE` bar to the `section_header_bg` card pattern.
- `widget_remote_channels_config.xml` (the per-channel `ChannelSettingsWidget`) —
  channel name labels use `header_title`; the `RangeBar`'s container uses
  `chip_info_background`; both checkboxes (`Reverse`, `Return to Center`) use the
  `chip_text`/`chip_icon` pattern; the dual-rate `EditText` uses the chip card style.
- `ChannelSettingsWidget.java` — the `RangeBar`'s *programmatic* colors (set in code,
  not XML, since the third-party `RangeBar` widget takes `@ColorInt` values via setters)
  were updated from the old white/blue/green scheme to
  `chip_border` (unselected track) / `header_subtitle` (tick marks) /
  `accent_fcb` (selected range + pin handles) / `accent_fcb_dark` (selector).

This is a template for theming the app's remaining un-migrated screens: set one
background on the outermost container, then walk inward replacing hardcoded
`btn_TXT_*`/`android:color` references with the palette above.
