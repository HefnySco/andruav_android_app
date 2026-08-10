# Dependency Upgrades (Phase 4)

Phase 4 followed a strict rule stated in its first commit: **removals and version bumps
each land as their own reviewable commit**, ordered dead-code-removal-first so later
version bumps aren't hiding an unrelated deletion in their diff.

## Removed outright (`9c5e03a`)

These were audited and found to have zero live usage — deleted rather than upgraded.

| Dependency | Reason |
|---|---|
| `com.google.android.gms:play-services-plus` | The Google+ API, shut down by Google in 2019. Zero `com.google.android.gms.plus` references anywhere. |
| `com.jcraft:jsch` | `SshConnection.java`'s only reference anywhere in the repo was a commented-out line in `MainScreen.java`. Class and dependency deleted together. |
| `jackson-mapper-asl-1.9.7.jar` (`andruavProtocol/libs/autoBahn/`) | Zero `org.codehaus.jackson` imports. The sibling jars in the same folder (`autobahn-0.5.0.jar`, `jackson-core-asl-1.9.7.jar`) weren't even declared as build inputs — leftover clutter from a pre-`Java-WebSocket` implementation. |
| `androidx.legacy:legacy-support-v4` | Long-superseded umbrella artifact; zero `androidx.legacy` imports in Java source or XML. Its real classes (`androidx.core`/`fragment`/`loader`/`localbroadcastmanager`) were already pulled in directly elsewhere. |
| `org.usb4java:usb4java` | Confirmed dead in the phase-1 code-cleaning audit (`6d77364`). |
| `com.github.supervital:swipenumberpicker` | Was already commented out ("only FCB mode supported, remove"); removed the dead reference entirely. |

## Version bumps

| Dependency | Before | After | Module | Commit |
|---|---|---|---|---|
| `androidx.appcompat:appcompat` | 1.0.0 | 1.7.0 | app, andruavProtocol | `93015bf` |
| `com.google.android.material:material` | 1.0.0 | 1.12.0 | app | `93015bf` |
| `org.greenrobot:eventbus` (was `de.greenrobot:eventbus`) | 2.4.0 | 3.3.1 | andruavmiddlelibrary | `d0ec349` |
| `org.greenrobot:greendao` (was `de.greenrobot:greendao`) | 2.1.0 | 3.3.0 | andruavmiddlelibrary | `92579d1` |
| `com.squareup.okhttp3:okhttp` | 3.3.1 | 3.14.9 | andruavmiddlelibrary | `ec948d4` |
| `org.java-websocket:Java-WebSocket` | 1.3.8 | 1.5.7 | andruavProtocol | `ec948d4` |
| `com.jakewharton.timber:timber` | 3.1.0 | 4.7.1 | ClientLib | `ec948d4` |
| `com.androidplot:androidplot-core` | 0.6.1 | 1.5.11 | app | `ec948d4` |

### Why these specific target versions (not "latest")

Each bump deliberately stayed on the last version before an API-shape-changing major
release, to avoid also having to migrate call sites in the same commit:

- **OkHttp** stopped at 3.14.9 (the last 3.x release) — OkHttp 4 changes its Java API
  shape toward a Kotlin-first surface; staying on 3.x picks up ~7 years of bug/security
  fixes with zero source changes for this Java codebase.
- **Timber** stopped at 4.7.1 — the last Java-friendly major before 5.x pulls in the
  Kotlin stdlib as an `api` dependency.
- **appcompat**/**material** went to their latest stable (1.7.0 / 1.12.0) because the
  app's theme is plain `Theme.AppCompat.Light.DarkActionBar`, not a
  `Theme.MaterialComponents.*` parent — there's no Material color/attribute theming
  system in play to regress. Direct Material usage is limited to `AppBarLayout` and
  `Snackbar` (`Notification.java`'s `showSnack`), both stable, non-MaterialComponents
  widgets.

Each of these bumps' usage in the codebase was surveyed first and confirmed to be
limited to long-stable, core API surface (e.g. OkHttp: `Call`/`Request`/`Response`/
`OkHttpClient` in 2 files; Java-WebSocket: `WebSocketClient`/`CloseFrame`/
`ServerHandshake` in 1 file) — which is why no source changes were needed alongside the
version bumps themselves.

## Build tooling

- **`android.enableJetifier=true`** (`gradle.properties`, `7ddadbb`) — added so AGP
  rewrites legacy `android.support.*` references inside third-party AARs to AndroidX
  equivalents at build time. `android.useAndroidX=true` alone only governs first-party
  source; it does nothing for a prebuilt AAR (like `rangebar-release.aar`) that was
  compiled against the old support library. Without Jetifier, such an AAR's classes
  reference support-library types the app no longer ships, producing a
  `ClassNotFoundException` at the exact moment that code path is exercised. See
  [UI Theme System](UI-Theme-System.md) for the RC Settings crash this specifically fixed.
