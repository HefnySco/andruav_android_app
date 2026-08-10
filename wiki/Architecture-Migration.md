# Architecture Migration

Two independent migrations landed between `Andruav_AP_Original` and `Andruav_AP_2026`:
collapsing `ClientLib`'s AIDL/Binder layer into direct in-process calls (Phase 3), and
moving two of its runtime dependencies — EventBus and GreenDAO — off long-abandoned
`de.greenrobot` group IDs onto their maintained `org.greenrobot` successors (part of
Phase 4). Both were done as a sequence of small, independently buildable commits rather
than one large rewrite.

## ClientLib de-AIDL-ification (Phase 3)

### Background

`ClientLib` is the DroidPlanner-derived MAVLink/vehicle-control layer. It was originally
architected assuming `DroidPlannerService` might run in a different process than the
Activities that use it — hence AIDL interfaces (`IDroidPlannerServices`, `IApiListener`,
`IDroneApi`, `IObserver`, `IMavlinkObserver`, `ICommandListener`) and `Stub`-based
implementations, `RemoteException` handling everywhere, and binder-death detection
(`linkToDeath`/`binderDied()`).

In this app, `DroidPlannerService` has always run in-process (no `android:process` in its
manifest declaration). The entire AIDL layer was ceremony with a real cost: every method
crossing that boundary needed exception handling for a failure mode (the other process
dying) that could never happen.

### The three steps

**Step 1 — `b2d4c7d`: collapse the registration path to `LocalBinder`.**
`ControlTower` now binds with the standard `LocalBinder` pattern and gets a direct
`DroidPlannerService` reference. `DroneApi` computes its own version info instead of
routing through an `IApiListener` callback. `checkForSelfRelease()`/`binderDied()` are
removed — they only existed to detect a cross-process client dying.

Deleted: `IDroidPlannerServices.aidl`, `IApiListener.aidl`, `DPServices.java`,
`DroneApiListener.java`, `ApiAvailability.java` (the last one existed for multi-app
service discovery, irrelevant once ClientLib is compiled into this app's own APK).

`DroidPlannerService` itself is **kept** — it's the app's only foreground service, and
holding foreground priority while a vehicle is connected still matters.

**Step 2 — `d415089`: replace `IDroneApi` with direct `DroneApi` calls.**
`DroneApi` stops extending `IDroneApi.Stub` and becomes a plain class. `Drone` holds a
direct `DroneApi` reference instead of an AIDL proxy. "Is the handle started" becomes
"is the reference non-null" instead of a binder liveness check.

Deleted: `IDroneApi.aidl`.

**Step 3 — `4f9b964`: convert `IObserver`/`IMavlinkObserver`/`ICommandListener` to plain
interfaces.** The last AIDL remnant. Concrete implementations (`DroneObserver`,
`MavlinkObserver`, `AbstractCommandListener`) now implement plain interfaces instead of
extending a generated `.Stub`. Call sites that caught `RemoteException` around a
listener/observer call widen to `catch (Exception)` for the same defensive-isolation
reason, since a misbehaving listener can still throw — it just can't die in a separate
process anymore.

### What didn't change

Every public `Drone`/`ControlTower`/`apis/*` method signature is unchanged across all
three steps — no app-module call site needed updating. After step 3, ClientLib contains
no AIDL interface, `Stub`, `asBinder()`/`pingBinder()`, `linkToDeath`, or
`RemoteException` (confirmed by a repo-wide grep). The only remaining `.aidl` files are
`Parcelable` marker declarations, an unrelated construct.

### Verification

Each step passed `assembleDebug`/`assembleRelease`. Step 3's commit message flags that a
real-device bench test (arm/disarm, mode changes, mission upload, telemetry) was still
recommended before trusting it on a real flight — version `7.5.0` (`c987be5`) was bumped
specifically to mark "bench-tested and confirmed working: FCB connect, arm, control".

## EventBus 2.4.0 → org.greenrobot:eventbus 3.3.1 (`d0ec349`)

This was flagged as the highest-risk change in the whole migration range, because a
missed conversion fails **silently** (a dropped listener at runtime) rather than at
compile time.

### The API break

EventBus 2.x auto-discovered subscriber methods by reflection on the method name — any
`public void onEvent(SomeEvent e)` method was picked up automatically. EventBus 3.x
removed that convention entirely in favor of explicit `@Subscribe` annotations,
discovered by scanning at `register()` time.

### What the migration did

- `import de.greenrobot.event.EventBus` → `import org.greenrobot.eventbus.EventBus`
  across 34 files.
- `@Subscribe` added to all 87 `public void onEvent(...)` methods across 37 files.
- Two real (non-cosmetic) API differences, found only at compile time:
  `EventBus.getDefault().register(this, priority)` — the two-arg overload that set a
  subscriber-wide priority — doesn't exist in 3.x. Priority moved onto the `@Subscribe`
  annotation per method. The two affected classes
  (`AndruavWSClient_TooTallNate`, `ControlBoard_DroneKit`) had every `@Subscribe` in the
  file set to `@Subscribe(priority = 1)` to preserve the original subscriber-wide
  semantics.
- No thread-mode suffix methods (`onEventMainThread`/`onEventAsync`/
  `onEventBackgroundThread`) or sticky events existed anywhere in the codebase before
  this migration, so every converted method keeps 2.x's default behavior
  (posting-thread, synchronous dispatch) under 3.x's default `@Subscribe(threadMode =
  POSTING)` — no semantic reinterpretation was needed.

### Verification methodology

Because a missed `@Subscribe` is silent, verification used two independent full-source
scans rather than relying on the compiler:
1. Every line matching `public void onEvent(` is immediately preceded by `@Subscribe` —
   zero gaps.
2. Every one of the 28 `EventBus.getDefault().register(this)` call sites resolves to a
   class whose own declaration or resolvable superclass chain contains at least one
   `@Subscribe` method (EventBus 3.x throws `EventBusException` at `register()` time for
   a subscriber with none; 2.x silently tolerated it).

### Follow-up: line-ending fix (`c938794`)

The migration script opened files with Python's default text-mode I/O, which silently
normalizes CRLF→LF on read and writes LF back regardless of the file's original
convention. 15 of the 37 touched files used CRLF in this repo (mixed line-ending
conventions predate this branch), so the original migration commit rewrote every line in
those 15 files instead of just the intended `@Subscribe`/import lines — correct final
content, but a diff roughly 40× larger than necessary. A follow-up commit restored the
original CRLF endings, shrinking the diff to the expected ~183 lines across 37 files.

## GreenDAO 2.1.0 → org.greenrobot:greendao 3.3.0 (`92579d1`)

Package rename `de.greenrobot.dao.*` → `org.greenrobot.greendao.*` across the 5 files
that touch it (`DaoMaster`, `DaoSession`, `LogDao`, `GenericDataDao`, and
`KMLFileHandler`'s `QueryBuilder` import). These are hand-written DAOs — no
annotation-processor/codegen setup exists in this project — so this is a
runtime-library-only migration, not an entity-schema regeneration.

Three real API changes beyond the package rename, confirmed against the actual
`greendao-3.3.0` jar's `AbstractDao` class:

- `AbstractDaoMaster`/`AbstractDaoSession` constructors now take greenDAO's own
  `Database` wrapper instead of a raw `android.database.sqlite.SQLiteDatabase`.
  `DaoManager.init()` now wraps the `SQLiteOpenHelper`'s database in a
  `StandardDatabase` before constructing `DaoMaster`.
- `AbstractDao` gained an abstract `hasKey(T)` method that didn't exist in 2.x.
  Implemented in both DAOs as `entity != null && entity.getId() != null`, matching what
  a 3.x-generated DAO would produce for a `Long` primary key.
- `AbstractDao.bindValues` is abstract for **both** a `DatabaseStatement` overload and a
  legacy `SQLiteStatement` overload (kept for source compatibility) — both DAOs
  implement both, with identical binding logic, matching the standard
  greenDAO-generated-code pattern.

Both migrations passed `assembleDebug`/`assembleRelease`.
