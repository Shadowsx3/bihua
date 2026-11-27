# Bihua shared architecture (Android + iOS)

This is the working contract for the port. The product plan and phases stay in
`docs/ios-kmp-plan.md`; this file explains how the code is arranged, what is
allowed in each source set, and how to build, run, and test both clients.

Status: iOS 1.0 feature-complete for the first scope. Real content, stroke
guides, and FSRS-7 progress are persisted in Application Support; Home, Review,
Characters, Dictionary, Practice, Word practice, stroke order, Progress,
Settings, study lists, speech, reminders, and JSON backup export/import run on
iPhone and iPad behind a native UIKit tab bar. Widgets and sync are absent; PDF,
spell quest, compare, the ML Kit reader, and sounds are post-1.0.

## iOS data stores

| Store | File | Purpose |
|---|---|---|
| `IosSettingsStore` | `NSUserDefaults` `settings_json` / `profile_json` | course, theme, pet, practice and reminder choices |
| `IosContentRepository` | `Content/content_<mode>.json` | 10k characters, 34k words, meta; parsed one mode at a time |
| `IosGuideRepository` | `Content/guide_XX.json` | 32 shards of stroke guides, loaded on demand |
| `IosProgressStore` | `Application Support/bihua_progress.json` | FSRS memory, due dates, history, notes |
| `IosStudyListStore` | `Application Support/bihua_study_lists.json` | learner-made character lists |
| Backup | share sheet + Files picker | settings, profile, and the progress document |

All of these are app-private. The progress and list files live under the
Application Support directory that `IosAppFiles` excludes from iCloud backup.
The settings preferences may be included in a device backup, but contain no
review history or notes.

## Source sets

One Gradle module, `:composeApp`, with two clients and canonical KMP source sets. No
source-dir overrides remain in `build.gradle.kts`.

```
composeApp/src/
  commonMain/kotlin/        shared domain and Compose UI
  commonTest/kotlin/        pure tests that run on Android and iOS
  androidMain/kotlin/       Android code, services, widgets, sync
  androidMain/res/          Android resources
  androidMain/assets/       content databases and spellbook
  androidMain/AndroidManifest.xml
  androidDebug/kotlin/      QA bridge (debug only)
  androidDebug/AndroidManifest.xml
  androidUnitTest/kotlin/   Robolectric and JVM-only tests
  androidUnitTest/resources/
  iosMain/kotlin/           Compose entry point, routes, Foundation services, widget snapshot
  iosTest/kotlin/           native-driver tests (P2)
iosApp/
  Bihua/                    Swift host, native tab bar, privacy manifest
  BihuaUITests/             XCUITest journeys on iPhone and iPad
  WidgetShared/             deferred WidgetKit views (not in the Xcode target)
  BihuaWidgets/             deferred WidgetKit extension (not in the Xcode target)
```

Rules:

- `commonMain` never imports `android.*`, `androidx.*`, `java.*`, or a platform
  SDK. It may use Compose Multiplatform and `kotlinx.*`.
- `androidMain` keeps the current behaviour. It is allowed to import Android
  and Jetpack Compose while the shared UI is not there yet.
- Android-only packages stay out of iOS: `sync`, the Android `widget`
  provider, `reminder` worker, the crash handler, the ML Kit Android service.
  iOS widgets are deferred; their SwiftUI code is outside the Xcode target.
- `iosMain` never imports Android packages. Swift holds only platform seams,
  never screen code or business logic.
- Compose code that is shared must be portable: no `LocalContext`, no
  `Intent`, no `BuildConfig`, no `android.graphics` (see the P3 checklist in the
  plan).

## Platform seams

`commonMain/platform/PlatformServices.kt` is the only platform surface the
shared code can reach. It is deliberately small; pieces are added when a shared
screen needs them, not speculatively.

Implemented in P0/P1:

| Seam | Contract | Android | iOS |
|---|---|---|---|
| `AppInfo` | version, build, platform, device, OS, debug | `BuildConfig`, `Build` | `NSBundle`, `UIDevice` |
| `AppFiles` | app-private dirs, byte read/write/delete | `Context.filesDir` | Application Support / Caches, excluded from iCloud backup |
| `Capabilities` | what this build offers | sync, widgets, foldables, crash attach true | widgets, sync, foldables, crash attach false; rate URL empty until the App Store record |
| `LinkOpener` | open a URL | `Intent.ACTION_VIEW` | `UIApplication.openURL` |

`Services.install(...)` is called once by each shell before the first Compose
frame: `BihuaApplication` on Android, `MainViewController` on iOS. Common code
uses `Services.current`; there is no other platform singleton.

The remaining catalogue, added in P4 (all synchronous or callback-based, no
suspend/Flow across the Swift boundary):

`SettingsEngine`, `DatabaseFactory`, `ContentSource`, `InkRecognizer`,
`Speaker`, `SfxPlayer`, `Sharer`, `FilePicker`, `PdfWriter`, `Reminders`,
`NetworkState`, `Clipboard`. There is no image decoder for avatars: the twelve
pet looks are drawn by Compose. Add an image seam only if a future screen
imports a picture.

## Data

- **Progress** stays schema 18. A SQLDelight driver plus a thin `SqlHandle`
  cursor facade replaces `SQLiteOpenHelper` without rewriting the SQL. A real
  Android v18 database is the fixture; the first native test opens it and
  round-trips a review.
- **Settings** keeps the serialized `AppSettings`/`UserProfile` shape and
  `SettingsJsonMigration`. A path-based DataStore file replaces the Android
  delegate: `Application Support/bihua_prefs.preferences_pb`.
- **Content** bundles `bihua_content.db`, `puzzle_parts.db`, `spellbook.csv`.
  It is copied once to Application Support on a background queue, excluded from
  backup, and prewarmed during onboarding. The reader model remains a separate
  optional download named with its size.
- **Backup** keeps the current zip format, read and written with Okio, so an
  Android backup imports on iOS and the reverse. Sync stays Android-only and
  never appears in the iOS UI.

## UI

- Shared Compose Multiplatform 1.9.3 on Kotlin 2.2.21. Android keeps Jetpack
  Compose through `androidMain` until screens move in P3, so the Android
  dependency graph is unchanged today.
- Shell: **native iOS navigation**, not Compose chrome. A `UITabBarController`
  hosts one `UINavigationController` per tab, with large titles, swipe-back,
  safe areas, keyboard avoidance, and system sheets. Compose renders content
  only. Routes carry titles and a `ScreenContent(route)` seam; the Compose tab
  bar and back buttons are hidden on iOS through `LocalUseNativeNavigation`.
- Follow the iOS guidelines: Dynamic Type, VoiceOver, Reduce Motion, context
  menus, swipe actions, system share and document pickers, and iOS press
  feedback instead of Material ripples. iPhone and iPad only; no foldable
  target.
- Profile: the learner picks one of twelve hand-drawn Mò looks in a 3 x 4 grid.
  There is no name field, no photo picker, and no photo-library permission on
  either platform.
- Dynamic Type: `IosDynamicType` reads `UIApplication.preferredContentSizeCategory`
  and provides the matching Apple body-text font scale to Compose, updating on
  `UIContentSizeCategoryDidChangeNotification`. Text grows with the system
  setting; spacing in dp does not.
- Pointer: `UIApplicationSupportsIndirectInputEvents` is on so iPad trackpad and
  mouse clicks are real touches, not a compatibility overlay.
- Insets: the floating bar and content use `safeDrawing`/`safeContent`, not a
  hard-coded 72 dp. The Android shell's measurement is the reference.
- Keyboard: Compose iOS reports the IME as an inset; report, study-list, and
  pronunciation fields are tested with the keyboard open on iPhone and iPad.
- Motion: `reduceMotion` comes from settings and the platform accessibility
  flag. Perpetual UI animation is not allowed (it breaks TalkBack and
  uiautomator idle).

## Capabilities and parity

The table of what exists on each client is `docs/ios-feature-parity.md`. It is
the checklist the port closes, and the source for Settings hiding rules. The
native shell and screen review list is `docs/ios-design-guidelines.md`.

Every platform-only row is checked by a capability, never by an OS name in the
UI:

- iOS hides Sync, widgets, crash attachment, and foldable-only rows.
- Search finds none of the hidden entries.
- The iOS report composer prints `iOS` and the device model, never an Android
  release or SDK string.
- The strings test fails on "Android" or Play imagery in the iOS bundle.

## Build and run

```bash
# Android (unchanged)
make check
make lint

# iOS simulator
make ios            # iPhone 17 Pro: build, boot, install, launch
make ios-ipad       # iPad Pro 13-inch
make ios-build      # Xcode Debug build only
make ios-test       # Android unit tests plus iosSimulatorArm64Test
make ios-check      # native tests plus the Xcode Debug build
make ios-pods       # CocoaPods, simulator-safe (no ML Kit)
make ios-mlkit-pods # ML Kit for a device build
make ios-shot NAME=home
make ios-device      # signed build, install, launch on the connected iPhone
make ios-device-build
make ios-device-id
make ios-ipad-device  # signed build, install, launch on the connected iPad
make ios-ipad-device-shot NAME=home
```

Devices are iPhone and iPad only. The connected devices are an iPhone 17e and
an iPad (9th generation), both on iOS 27.2 with Developer Mode on. The scripts
choose one with `make ios-device` or `make ios-ipad-device`. The
script discovers it through `devicectl` and uses the Apple Development identity
in the login keychain (`BIHUA_IOS_TEAM` overrides the default team id). Run
`make ios-mlkit-pods` before a device build that should include the reader; the
simulator install stays pod-free.

Under the hood: Gradle links `BihuaKit.framework` (static), XcodeGen generates
`iosApp/Bihua.xcodeproj` from `iosApp/project.yml`, and Xcode hosts Compose with
a 20-line `UIViewControllerRepresentable`. After changing `project.yml`, run
`cd iosApp && xcodegen generate && pod install` (or `make ios-pods`).

ML Kit detail: Google's digital-ink framework has device arm64 and simulator
x86_64 slices, but **no arm64 simulator slice**. Apple Silicon simulators are
arm64, so the pod is installed only for device work (`make ios-mlkit-pods`).
The Swift seam uses `canImport`, so the simulator builds and runs without it.
Rosetta 2 is installed, but the iOS 26.5 runtime's UIKit and loader are
arm64-only, so an x86_64 simulator app cannot run. Recognition is proven on a
real iPad, not the simulator.

## Testing

| Layer | Command | What |
|---|---|---|
| Shared pure tests | `make ios-test` / `make test` | `commonTest` runs on both targets |
| Android JVM/Robolectric | `make test` | 35 Android-only files stay here |
| iOS native tests | `iosSimulatorArm64Test` | files, zip, SQL driver, dates (P2) |
| iOS simulator smoke | `python3 qa/ios/smoke.py` | boot, install, launch, label, screenshot |
| iOS journeys | `qa/ios/driver.py` | idb-based verbs mirroring `qa/driver.py` |
| Android journeys | `scripts/journey.sh` | unchanged, the parity referee |
| XCUITest | `iosAppTests` (P6) | rotation, keyboard, VoiceOver, notification sheets |

Screenshots from `scripts/ios.sh shot` land in `.qa/ios/`. Compare iOS against
iOS, never iOS against Android: text metrics differ.

## Migration ledger

| Move | Status | Notes |
|---|---|---|
| `spellbook` | done | Pure parsing and puzzle generation; Android keeps the asset/SQLite loader; puzzle and quest tests run on both |
| `platform` seams | done | `AppInfo`, `AppFiles`, `Capabilities`, `LinkOpener`, `Services` |
| `fsrs` | done | Kernel, optimizers, weights, and load estimate; `kotlinx-datetime` only for the export timestamp; `MemoryModel` is shared |
| `content` models | done | `CharacterEntry`, `StudyMode`, `CharacterOrder`, search, pinyin, romanisation, curriculum, featured words, Han sets, learn items, known list. `StudyMode` now carries English labels instead of `@StringRes`; Compose `@Immutable` was dropped from the data classes (inference keeps them stable) |
| `learn` pure files | done | Review task/plan, reasons, mnemonic pieces (common code-point helpers) |
| `settings` pure files | part | `MeaningSanitizer`, `MemoryModel`; `Models.kt` still needs the `LocalDate` boundary moved to `kotlinx-datetime` |
| `progress` pure files | next | Ranges, queue, session filter, writing pace |
| `backup` | P2 | Okio zip |
| UI packages | P3 | Theme, components, Home, pet avatars, calendar, charts in `commonMain`; review, characters, dictionary, settings, progress follow. The iOS Home renders the real Home composable with a sample state until the data layer lands. |

## Ground rules

- Every commit is behaviour-preserving (moves, `expect`/`actual`, seams) or
  behind `Capabilities`; Android `make check` stays green.
- Do not rewrite queries or migrations while moving them. Keep the SQL text and
  the serialized settings shape byte-compatible.
- Do not add a second UI toolkit or a second data layer.
- Do not grow a synthetic test screen. The P0 host exists only until the real
  shell mounts in P3; new evidence comes from the real screens.
- Keep the plan's phases ordered: P1 split, P2 data, P3 UI, P4 Swift seams,
  P5 features, P6 test/goldens, P7 store.
