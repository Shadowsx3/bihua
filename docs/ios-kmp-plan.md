# Bihua iOS plan — Compose Multiplatform, native shell, deferred widgets

Status: **P3 in progress on `main`**. The port no longer lives on a branch:
`composeApp/` and `iosApp/` sit side by side in this repository. Home,
Characters, Progress, and Settings render on iPhone and iPad behind a native
UIKit tab bar; review and character detail use native pushes. The first iOS
build has no widgets. The shared stores, practice, dictionary, and the remaining
seams are next. Android 1.1.2 is built but held until iOS reaches parity. The
Apple Developer Program is renewed through November 2027 (team `DUST62RX27`); a
real iPhone and iPad are connected for device evidence.

> **Superseded decisions (26 November 2026).** WidgetKit work is kept in
> `iosApp/BihuaWidgets` but deferred from the first build to keep the review
> scope small. The app has no App Group and no `.appex`. iOS still has no local
> sync, foldable target, or crash attachment. Anything later in this historical
> plan that says widgets are in scope is superseded by
> `docs/ios-architecture.md` and `docs/ios-feature-parity.md`.

Working documents:

- `docs/ios-architecture.md` - source sets, seams, data, UI, build, tests.
- `docs/ios-feature-parity.md` - the parity table and hiding rules.
- `docs/ios-app-store-checklist.md` - what App Store Connect asks and where
  the answer lives.
- `docs/ios-design-guidelines.md` - the 100% iOS guideline review list for the
  native shell and every screen.
- `store/ios-listing.md` - the local listing draft (never committed).

This document replaces the earlier SwiftUI-over-KMP plan. The decision changed:
the UI is **shared Compose Multiplatform**, not a second SwiftUI app. Native
Swift is used only where Kotlin cannot reach the platform.

## Starting conditions

Before P0:

1. Android 1.1.2 is tagged and the release decision is made. The Android gates
   live in `docs/backlog.md`; this plan does not repeat them.
2. The port happens on `ios/kmp`, branched from the tagged release, not on
   `release/android-1.1.2`. `main` stays buildable and shippable throughout.
3. Every commit is either behaviour-preserving (moves, `expect`/`actual`) or
   behind `Capabilities`. Reviewers should be able to run the Android journey
   suite after any commit in this plan.
4. An Apple Developer account exists before P7. Simulator work needs none, but
   TestFlight does, and enrollment is the only step with an external delay.
5. Xcode 27.2 and a simulator runtime are installed. No Apple dependencies are
   added until the branch exists.

---

## P0 findings (November 2026)

- The KMP module, CMP 1.9.3 on Kotlin 2.2.21, the static `BihuaKit` framework,
  the XcodeGen host, and a Compose screen run on the iPhone and iPad
  simulators. A text field takes iOS keyboard input and CJK metrics render.
- Compose pointer changes expose `pressure`; the Pencil probe draws with it.
  The simulator has no force, so the Pencil decision waits for a real iPad.
- **ML Kit's digital-ink pod has no arm64 simulator slice.** It ships device
  arm64 and simulator x86_64. Rosetta 2 is installed, but the iOS 26.5 runtime
  is arm64-only, so an x86_64 simulator app cannot run. The pod is installed
  only for device work (`make ios-mlkit-pods`); the Swift seam uses `canImport`,
  and the simulator stays on the Compose host. Recognition must be proven on a
  real iPad.
- Apple documents are started: `PrivacyInfo.xcprivacy`, export compliance,
  the listing draft, and the review-notes checklist.
- On the connected iPhone (iOS 27.2, team `DUST62RX27`), the signed build runs.
  The ML Kit pod links, the `zh-Hans` model downloads, and a synthetic horizontal
  stroke with a writing-area context is recognized as 一. A missing
  `CADisableMinimumFrameDurationOnPhone` key crashed the first device launch;
  the simulator did not catch it.
- The iOS app supports iPhone and iPad only. There is no iOS foldable target
  and no Duo test; the Android fold journeys remain the layout referee.
- P3 milestone: the real shared Home composable renders on the iPhone and iPad
  simulators and on the connected iPhone, with the theme, pet avatar, calendar,
  word card, and stats. It uses a sample state until P2 connects settings,
  progress, and content. Dynamic Type and pointer support are wired at the iOS
  root. The native tab bar/navigation shell is next.
- Android stayed green after every move (`make check`). `spellbook` is now in
  `commonMain` with native tests; the shared platform seams are installed in
  both shells.
- Do not grow the P0 probe screen. The next evidence comes from the real
  screens as they move to `commonMain`.

## 0. Decisions

| # | Decision | Reason |
|---|---|---|
| D1 | Share the Compose UI, not just the domain | 35,000 of 45,660 lines are Compose screens. A SwiftUI rewrite would be 250 to 400 hours of UI and a permanent second codebase. |
| D2 | Swift only for platform seams | ML Kit, speech, PDF, file picking, sharing, notifications, stylus force, image decode. Twelve small files, not a second app. |
| D3 | No widgets in the first iOS build | Deferred. The SwiftUI WidgetKit views are kept in `iosApp/BihuaWidgets` but are not an Xcode target; a later release can enable them with an App Group. |
| D4 | No local sync on iOS | Removes the only Kotlin/Native-hostile piece (`java.net.ServerSocket`, `javax.net.ssl`, NSD). Android keeps it. |
| D5 | No crash attachment on iOS at launch | AGENTS already states this. `CrashLog`/`CrashHandler` stay Android. The report composer ships without the checkbox. |
| D6 | No SKIE | With a shared UI, Swift never consumes Kotlin flows or suspend functions. Seams are callback-based interfaces exported as ObjC protocols. |
| D7 | iOS 16.0 minimum, iPhone and iPad, all orientations | ML Kit digital ink needs 15+; 16 is the sane floor. All orientations are required for iPad multitasking. |
| D8 | One app target, one bundle id: `com.bassmd.bihua` | No App Group, no extensions, so no second identifier. |
| D9 | No CloudKit, no iCloud sync, no push, no sign-in, no purchase | The product promise. |

Non-goals: App Store optimisation, iPad pointer hover polish, Apple Pencil
squeeze, live activities, share extension, Shortcuts actions, watchOS.

---

## 0.5 Compliance, checked November 2026

Applies to the iOS build only. Android keeps its own Play rules.

| Guideline | Requirement | What Bihua does | Proof |
|---|---|---|---|
| 2.1(a) | Final build, no crashes, tested on device, review notes for non-obvious features | Full journey pass before submission; notes explain the offline first run and the optional model | Goldens + TestFlight |
| 2.3.1(a) | New features described specifically in Notes for Review | Short list: no account, no purchase, offline, optional reader, mature words off by default | Review notes |
| 2.3.3 | Screenshots show the app in use | Real iOS captures of Home, review, spell quest, Progress; no splash, no widget art | `simctl` set |
| 2.3.6 | Honest age rating | 16+ with "Infrequent/Mild Sexual Content and Nudity", no location sharing, no dating | Questionnaire |
| 2.3.7 | Name ≤ 30 chars, no trademarked or other-app terms in metadata | "Bihua"; remove "Anki" from metadata and from in-app strings if the reviewer reads them | Copy pass |
| 2.3.10 | No other platform's names, icons, or imagery **in the app** or metadata | No "Android" string in the iOS UI, no Play badge, no "Android only" row. Platform-only features are simply absent | Unit test on shipped strings |
| 2.4.1 | iPhone apps should run on iPad | Universal app, all orientations | Device matrix |
| 2.5.1 | Public APIs only | Compose, Skia, UIKit, AVFoundation, ML Kit | Build |
| 2.5.2 | No downloaded executable code | ML Kit models are data resources; the content DB is bundled | Architecture |
| 2.5.15 | File access includes Files app and iCloud documents | `UIDocumentPickerViewController`, no custom browser | Import test |
| 2.5.16 | Notifications relate to app content | Study reminders only | Reminder test |
| 3.1.1 | Unlocking features requires IAP | No IAP, no donation, no purchase, nothing to unlock | Listing |
| 4.2.3(ii) | If the app downloads extra resources at first launch, disclose the size and prompt first | Model notice says "Reader model, about 20 MB" and is optional; setup never waits | Offline journey |
| 5.1.1(i) | Privacy policy in App Store Connect and in the app | About → Privacy policy; listing URL | Screenshot |
| 5.1.1(ii) | Consent for collected data, and a way to withdraw | Reader hint can be turned off; ML Kit diagnostics disclosed; no developer collection | Settings toggle |
| 5.1.2(i) | Explicit permission before sharing with third parties, including third-party AI | ML Kit runs on-device and receives no writing; diagnostics disclosed in policy and listing | Policy |
| 5.2.1 | No third-party marks without permission | "Anki-style" replaced by "Reveal"; no Apple marks implying endorsement | Copy |

Additional 2026 requirements:

- **SDK minimum.** Uploads must be built with Xcode 26 or later and the iOS 26 SDK
  (since 28 April 2026). Xcode 27.2 is installed, so this is satisfied. The next
  cutoff, the iOS 27 SDK, is expected around April 2027; a deployment target of
  16.0 is still allowed, only the build SDK must be current.
- **Age ratings.** Five tiers: 4+, 9+, 13+, 16+, 18+. Two new content descriptors
  arrived on 16 November 2026, **Shared Location** and **Dating**; both are "no"
  here. No social media capability, so the Declared Age Range API is not required.
  Avoid an 18+ rating: several regions (Australia, Brazil, Singapore, parts of the
  US) now block 18+ downloads without adult confirmation.
- **DSA trader status.** Required for EU distribution; apps without it are
  removed from all 27 EU storefronts. Declaring "trader" shows a contact address
  publicly in the EU. Decide before the first submission: either declare trader
  and accept the public contact, or do not distribute in the EU.
- **Privacy manifest.** Ship `PrivacyInfo.xcprivacy`. GoogleMLKit itself is not on
  Apple's SDK list, but its dependencies (GoogleUtilities, GoogleDataTransport,
  Protobuf, BoringSSL, Promises, nanopb) are, and ship their own manifests. Do not
  copy their reasons into ours.
- **Required-reason APIs we must declare ourselves.** The Kotlin/Native, Okio, and
  Skia layers touch a few: file timestamps (`C617.1`), system boot time
  (`35F9.1`), disk space (`E174.1`), and user defaults (`CA92.1`, if we use
  `NSUserDefaults` directly). Start with the set the first archive reports and
  remove anything unused; Apple's ITMS-91053 mail names the missing categories.
- **Privacy labels.** Declare Diagnostics for ML Kit, no tracking, no advertising
  identifier, no account, and no developer-side collection. This must match
  `PRIVACY.md` and the About paragraph that names ML Kit.
- **App Privacy "third-party AI".** ML Kit is a third-party ML SDK. It runs
  on-device here and receives no learner text or ink, but the policy and the
  listing must say so explicitly; the 2025 rules added AI to the sharing clause.

---

## 1. What is being shared, measured

| Measure | Value |
|---|---|
| Kotlin files in `composeApp/src/androidMain/kotlin` | 147 |
| Kotlin lines | 45,660 |
| Compose screen lines (`ui`, `practice`, `settings`, `learn`) | ~35,000 |
| Files importing `android.*` directly | 36 |
| Files importing an Android-only `androidx` artifact | 12 more |
| `androidx.compose` files | the rest; portable to CMP |
| Unit test files / lines | 105 / 9,878 |
| Journey scenarios in `qa/scenarios.py` | 119 |
| Content assets | `bihua_content.db` 30 MB, `puzzle_parts.db` 104 KB, `spellbook.csv` 69 KB |

Package sizes for planning:

| Package | Lines | Android imports | Destination |
|---|---|---|---|
| `ink` | 2,227 | `Stylus.kt` only | `commonMain` |
| `fsrs` | 1,045 | none (one `java.time`) | `commonMain` |
| `spellbook` | 781 | none | `commonMain` |
| `backup` | 176 | `java.util.zip` | `commonMain` after Okio zip |
| `content` | 1,271 | `ContentRepository.kt` | mixed |
| `learn` | 4,119 | `LearnSessionStore.kt` | mixed |
| `progress` | 1,817 | `ProgressStore.kt` | mixed |
| `settings` | 6,814 | 4 files | mixed |
| `ui` | 16,923 | 3 files + `LocalContext` | `commonMain` after seams |
| `practice` | 6,967 | `SpellQuestScreen`, `GrimoireScreen` context use | `commonMain` |
| `audio` | 553 | all | `androidMain` + Swift |
| `reminder` | 245 | all | `androidMain` + Swift |
| `widget` | 882 | Android providers | Android providers stay; iOS has SwiftUI WidgetKit views and a shared snapshot contract |
| `worksheet` | 439 | `WorksheetPdf` | shared layout + Swift PDF |
| `sync` | 437 | all | Android only |
| `ai` | 569 | 2 files | mixed |
| `diagnostics` | ~300 | handler + file store | shared formatter |
| `MainActivity`, `BihuaApplication` | ~200 | all | Android shell |
| `worksheet` + `ui/components/PdfExport` | ~500 | `PdfDocument`, `Intent` | shared model + Swift PDF |

---

## 2. Target architecture

```
composeApp/                      single Kotlin Multiplatform module (the Compose app)
  src/commonMain/kotlin/    every screen, every pure domain package
  src/commonMain/composeResources/   only if a string or drawable must be a resource
  src/androidMain/kotlin/   Android services, Activity, Application, widgets, sync
  src/androidMain/res/      themes, widget layouts, adapters
  src/commonTest/kotlin/    pure tests, moved from src/test
  src/androidUnitTest/      Robolectric and Compose tests stay here
  src/iosMain/kotlin/       the Compose entry point and the iOS service declarations
  src/iosTest/kotlin/       native-driver tests (SQLDelight, Okio, dates)

iosApp/                   Xcode project
  Bihua/
    BihuaApp.swift        SwiftUI App
    RootView.swift        UIViewControllerRepresentable over ComposeUIViewController
    Services/             PlatformServices implementations (12 files)
    Resources/            Info.plist, PrivacyInfo.xcprivacy, assets
  Podfile                 GoogleMLKit/DigitalInkRecognition
```

Rules:

- `commonMain` must never import `android.*`, `androidx.work.*`, `androidx.window.*`,
  `androidx.activity.*`, `androidx.datastore.preferences.core.preferencesDataStore`,
  `android.speech.*`, `android.graphics.pdf.*`, `com.google.mlkit.*`.
- `iosMain` must never import `sync.*`, `widget.*`, or `androidMain` symbols.
- No business logic in Swift. Swift implements protocols and nothing else.
- No Compose UI in Swift. The only SwiftUI is the 20-line host that mounts the
  Compose view controller.
- Android behaviour must not change in any commit that moves code. The port is
  refactor plus `expect`/`actual`, not a rewrite.

### Gradle

`settings.gradle.kts` keeps one module. No new Gradle modules; the shared UI is
the app module itself.

The tree now uses the canonical layout (`src/androidMain`, `src/commonMain`,
`src/iosMain`, `src/androidUnitTest`, `src/androidDebug`), with no source-dir
overrides. This was the migration step used to get there:

```kotlin
kotlin {
    sourceSets {
        androidMain {
            kotlin.srcDir("src/main/java")   // during migration
        }
    }
}

android {
    sourceSets["main"].res.srcDir("src/main/res")
    sourceSets["debug"].java.srcDir("src/debug/java")
}
```

Packages moved one at a time into `src/commonMain/kotlin`; the Android tree now
lives in `src/androidMain/kotlin`.

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)      // Compose compiler, already applied
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.compose)   // new
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "BihuaKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources) // only if needed
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)        // new
            implementation(libs.sqldelight.runtime)      // new
            implementation(libs.okio)                    // new
            implementation(libs.androidx.lifecycle.compose) // KMP artifact
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.mlkit.digital.ink)
            implementation(libs.androidx.work.ktx)
            implementation(libs.androidx.window)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}
```

Version rules:

- Do not bump Kotlin to chase a Compose Multiplatform release. Pick the CMP
  version built for Kotlin 2.2.x. Today that is CMP 1.9.x; verify at port start.
- Xcode 27.2 is installed. Deployment target 16.0.
- ML Kit iOS is added through the **Podfile**, not Gradle. Gradle only builds the
  framework; Xcode compiles Swift against the pod. This avoids the KMP CocoaPods
  plugin entirely and its `use_frameworks!` conflicts.

Xcode build phase (target → Build Phases → Run Script, before Compile Sources):

```sh
cd "$SRCROOT/.."
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

### Entry point

`iosMain/kotlin/com/bassmd/bihua/MainViewController.kt`:

```kotlin
fun MainViewController(): UIViewController = ComposeUIViewController {
    BihuaTheme { BihuaApp(services = Services.current) }
}
```

`RootView.swift`:

```swift
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Services.install(BihuaServices())      // before the first frame
        return MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

### The shell decision: Compose chrome or a native Liquid Glass shell

This is the only architectural question the research changed, and it must be
settled before the UI port starts.

iOS 26 Liquid Glass is a system material. Compose cannot draw it, and a
Compose-drawn tab bar will not blur or adopt the floating behaviour. JetBrains'
own guidance is that adopting Liquid Glass requires a **native shell**: a SwiftUI
`TabView` + `NavigationStack`, or a `UITabBarController` + `UINavigationController`,
with Compose rendering each screen's content.

| Option | What it looks like | Cost | Risk |
|---|---|---|---|
| **A. Compose chrome** | Bihua's own floating paper bar on both platforms | 0 extra | Does not follow the iOS guidelines; no Liquid Glass, no system back swipe or tab behaviour |
| **B. Native UIKit shell** (chosen) | System tab bar and nav bar with Bihua colours; Compose renders content | +30 to 50 h, plus a screen renderer and route metadata | Navigation state lives in two places; every screen needs a content-only entry point |
| **C. Compose plus `UIDesignRequiresCompatibility`** | Pre-iOS 26 appearance, no glass | 0 | Transition flag; Apple may drop it, and the app looks dated on new OSes |

Decision: **B. Follow the iOS design guidelines fully.** The iOS app uses a
system navigation shell, not a Compose-drawn bar: a `UITabBarController` with a
per-tab `UINavigationController`, large titles, swipe-back, safe-area and
keyboard handling, system sheets, and the system share and document pickers.
Compose renders each screen's content, never the chrome. iPhone and iPad are
the supported devices; there is no iOS foldable target and no Duo test.

Implement the three seams before P3:

- every route carries a `title`, so a native nav bar can label it;
- a `ScreenContent(route)` renderer that draws one screen without the shell;
- `App(topLevelRoute, onNavigate, onActivate)` callbacks and a
  `LocalUseNativeNavigation` flag that hides Compose title bars and back buttons.

Guideline checklist for the shell and every screen:

- system tab bar and navigation bar, large title on top-level routes, back
  swipe, and the system keyboard avoidance; no Compose bottom bar or Android
  back button on iOS;
- safe areas, Dynamic Type, VoiceOver labels and rotor actions, Reduce Motion,
  and light/dark system appearance;
- system share sheet, document picker, notification permission, and alert or
  sheet presentation for destructive confirmations;
- context menus and swipe actions where the platform expects them;
- iOS-like press feedback instead of Material ripples; no Android-only terms in
  the UI.

Do not use option C as the plan. It buys a year at most.

### Info.plist and app shell

| Key | Value | Why |
|---|---|---|
| `CFBundleDisplayName` | Bihua | Matches Android |
| `UISupportedInterfaceOrientations` | all four, iPhone and iPad | Required for iPad multitasking |
| `UIRequiresFullScreen` | `NO` | iPad split view and Stage Manager |
| `ITSAppUsesNonExemptEncryption` | `NO` | Standard HTTPS only |
| `UIFileSharingEnabled` | `NO` | App data is not browsable in Files |
| `LSSupportsOpeningDocumentsInPlace` | `NO` | Import copies into the sandbox |
| `UILaunchScreen` | paper colour background, seal centred | Android splash hand-off parity |
| `NSLocalNetworkUsageDescription` | absent | No sync, no local network |
| `NSMicrophoneUsageDescription` | absent | No recording in this release |

App size: the 30 MB content DB, Skia, and the ML Kit pod put the download near
60 to 80 MB. Acceptable for a language app with a dictionary inside; measure
before P7 and state it on the listing.

`composeApp/src/androidDebug` (the QA bridge) must not leak into `commonMain`.

---

## 3. Platform seam catalogue

Every seam is a Kotlin interface in `commonMain`, implemented once in
`androidMain` and once in Swift. Interfaces must be **synchronous or
callback-based**: no `suspend`, no `Flow`, no generics, so Swift can conform to
the exported ObjC protocol without SKIE.

```kotlin
// commonMain/platform/PlatformServices.kt
interface PlatformServices {
    val info: AppInfo                 // version, build type, device, OS
    val files: AppFiles               // app paths, read/write bytes
    val settings: SettingsEngine      // DataStore-equivalent key/value
    val database: DatabaseFactory     // SQLDelight driver
    val content: ContentSource        // bundled DB + spellbook csv
    val recognition: InkRecognizer    // ML Kit
    val speech: Speaker               // TTS
    val sounds: SfxPlayer             // synthesized cues
    val sharer: Sharer                // text + file share sheet
    val picker: FilePicker            // open/save documents
    val pdf: PdfWriter                // worksheet + progress report
    val reminders: Reminders          // local notifications
    val network: NetworkState         // online/offline
    val images: ImageDecoder          // avatar / widget-free bitmaps
    val capabilities: Capabilities    // feature flags per platform
    val links: LinkOpener             // privacy, source, rate
    val clipboard: Clipboard
}

object Services { lateinit var current: PlatformServices }
```

| Seam | Android today | iOS implementation | Risk |
|---|---|---|---|
| `SettingsEngine` | `settings/SettingsStore.kt` (DataStore Preferences) | Use `PreferenceDataStoreFactory.createWithPath` in `commonMain` with a platform path. No Swift needed. | Low |
| `DatabaseFactory` | `progress/ProgressStore.kt` `SQLiteOpenHelper` | SQLDelight `NativeSqliteDriver`; see §4 | Medium |
| `ContentSource` | `content/ContentRepository.kt` copies 30 MB from assets once | Bundle the three files; copy once to Application Support; prewarm during onboarding | Medium |
| `InkRecognizer` | `ai/InkRecognition.kt` | `DigitalInkRecognizer` from `GoogleMLKit/DigitalInkRecognition` | Medium |
| `Speaker` | `audio/Speech.kt` (`TextToSpeech`) | `AVSpeechSynthesizer` + `AVAudioSession` | Low |
| `SfxPlayer` | `audio/Sfx.kt` (`AudioTrack` synthesis), `audio/Ambient.kt` | Render the same wave tables into `AVAudioEngine` buffers | Low |
| `Sharer` | `Intent.ACTION_SEND` in `ReportDialog`, `SettingsScreen`, `PdfExport` | `UIActivityViewController` from the key window's top controller | Low |
| `FilePicker` | `ActivityResultContracts.OpenDocument` / `CreateDocument` | `UIDocumentPickerViewController` (`.open` / `.export`) as `NSData` | Medium |
| `PdfWriter` | `worksheet/WorksheetPdf.kt`, `progress/ProgressReportPdf.kt` (`PdfDocument`) | `UIGraphicsPDFRenderer` drawing the shared page model | Medium |
| `Reminders` | `reminder/ReminderWorker` (WorkManager) | `UNUserNotificationCenter` one-shot triggers | Low |
| `NetworkState` | `ai/NetworkStatus.kt` (`ConnectivityManager`) | `NWPathMonitor` | Low |
| `ImageDecoder` | `BitmapFactory` in `Avatar.kt`, `OutfitDialog.kt` | Decode with Skia in `commonMain` (`org.jetbrains.skia.Image.makeFromEncoded`) and convert to `ImageBitmap`. No Swift. | Low |
| `Capabilities` | `localSync = true`, `widgets = true`, `foldables = true`, `crashAttach = true` | all false except `rateUrl` | Low |
| `LinkOpener` | `LocalUriHandler` / `Intent.ACTION_VIEW` | `UIApplication.shared.open` | Low |
| `Clipboard` | `ClipboardManager` | `UIPasteboard.general` | Low |
| Stylus force | `ink/Stylus.kt` (`InputDevice`) | `UITouch.force`; see §5 | Medium |
| Fold state | `ui/FoldState.kt` (`androidx.window`) | Expect/actual returning `PHONE`; no-op | Low |
| Crash files | `diagnostics/CrashLog`, `CrashHandler` | Not implemented at launch; `capabilities.crashAttach = false` | Low |
| Rate URL | `settings/RateLinks.kt` (Play) | `https://apps.apple.com/app/id<APP_ID>` | Low |
| Build info | `BuildConfig` (7 files) | BuildKonfig or a generated `BuildInfo` actual reading `Bundle.main` | Low |

### What is implemented

`commonMain/platform/PlatformServices.kt` now holds `AppInfo`, `AppFiles`,
`Capabilities`, `LinkOpener`, and `Services`. Android installs
`AndroidPlatformServices` in `BihuaApplication`; iOS installs
`IosPlatformServices` in `MainViewController`. The rest of the catalogue below
is P4. See `docs/ios-architecture.md` for the rules and the migration ledger.

### Swift side

```
iosApp/Bihua/Services/
  BihuaServices.swift        // conforms to PlatformServices, holds the others
  AppInfo.swift              // version, build, device, iOS
  Files.swift                // Application Support, container URL, iCloud exclusion
  ContentSource.swift        // bundle copy, first-run progress
  Recognition.swift          // ML Kit: model manager + recognizer
  Speech.swift               // AVSpeechSynthesizer, 5 s init timeout, SpeechState
  Sounds.swift               // AVAudioEngine buffers from Kotlin-generated tables
  Share.swift                // UIActivityViewController
  Picker.swift               // UIDocumentPickerViewController
  Pdf.swift                  // UIGraphicsPDFRenderer
  Reminders.swift            // UNUserNotificationCenter
  Network.swift              // NWPathMonitor
  Links.swift                // UIApplication.open
  Clipboard.swift            // UIPasteboard
```

Rules for the Swift layer:

- Implement the Kotlin protocol; never build UI.
- Call back on the main queue for anything Compose reads.
- Bounce long work off `DispatchQueue.global` and return through a callback.
- Translate errors to a small enum with a human sentence; never throw into
  Kotlin as an exception.
- Every Swift face uses `@MainActor` where it touches UIKit.

---

## 4. Data: progress, settings, content, backup

### Progress database

Today: `ProgressStore.kt`, 1,705 lines, 55 raw SQL call sites, schema 18, eleven
`onUpgrade` branches, WAL, and report indexes.

Strategy: **SQLDelight driver plus a thin cursor wrapper**, not a rewrite of
every query.

1. Create `.sq` files that declare the schema exactly as `onCreate` builds it,
   plus `1.sqm` … `18.sqm` carrying the existing upgrade statements verbatim.
2. Generate the driver, then implement a `SqlHandle` in `commonMain` with the
   four methods `ProgressStore` actually uses:

```kotlin
interface SqlHandle {
    fun exec(sql: String, args: List<Any?> = emptyList())
    fun <T> query(sql: String, args: List<Any?>, map: (SqlRow) -> T): List<T>
    fun <T> queryOne(sql: String, args: List<Any?>, map: (SqlRow) -> T): T?
    fun transaction(block: () -> Unit)
}
```

3. Replace `SQLiteDatabase.rawQuery` and `ContentValues` with `SqlHandle` calls,
   one method at a time, keeping the SQL strings byte-identical.
4. Keep `SCHEMA_VERSION = 18`. Do not regenerate migrations from the fresh schema.

Acceptance tests:

- A fixture `progress-v18-android.db` built by the Android app opens on iOS,
  reports the same card count, accepts one review, and exports a backup that the
  Android build re-imports.
- `ProgressStoreModeTest` runs on `commonTest`, so course scoping cannot regress.
- An upgrade test seeds a v10 database and walks it to 18 on both platforms.

Why not Room KMP: it would regenerate the schema and its migrations, and the
whole point is byte-identical compatibility with installed Android databases.

### Settings

`AppSettings` and `UserProfile` are already `kotlinx.serialization` JSON. Replace
the `Context.preferencesDataStore` delegate with a path-based factory in
`commonMain`:

```kotlin
fun createSettingsStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { path.toPath() },
    )
```

- iOS path: `Application Support/bihua_prefs.preferences_pb`.
- Keep `SettingsJsonMigration`, `sanitized()`, and the stored shape unchanged.
- iOS toggles with no meaning (`androidOrientation`, widget outfit) are hidden by
  `Capabilities`, not removed from the model, so backups stay symmetric.

### Content

- Bundle `bihua_content.db`, `puzzle_parts.db`, `spellbook.csv` in the app.
- Copy to `Application Support/content/` on first launch, 1 MB buffer, on a
  background queue, with the existing "reader is arriving" notice.
- The reader model is a separate download. The notice must name the size (about
  20 MB) and stay optional, because 4.2.3(ii) requires disclosing the size before
  a first-launch download, and because a reviewer on a clean offline device must
  still reach Home and a review.
- Prewarm during onboarding, never on the first Home frame (AGENTS rule).
- Set the copy directory `isExcludedFromBackup = true`, so "your data does not go
  to iCloud" stays true on both platforms.
- Keep content version 34 in sync; a content bump must copy over the old file.

### Backup

`BackupArchive` uses `java.util.zip`. Replace with Okio's `ZipFileSystem` for
reading and a small writer for `backup.json` + `progress.db`.

- Format unchanged; Android backups import on iOS and back.
- iOS export: write to `tmp/`, then `UIDocumentPickerViewController(forExporting:)`
  or the share sheet.
- iOS import: document picker with `asCopy: true`, then existing validation.
- `TransferScope` unchanged. Sync remains Android-only; it never appears.
- Tests: round-trip on both platforms, plus a legacy settings-only JSON, plus a
  progress-only zip with a schema mismatch (must refuse politely).

---

## 5. Ink, stylus, and the Compose UI port

### What compiles as-is

`InkCanvas`, `InkModel`, `StrokeGuide`, `StrokePlayback`, `StrokeOrderGrader`,
`StrokeGrade`, `StrokeCoach`, `GuideScaffold`, `CompareScreen`, and the spell
quest draw overlay are pure Compose drawing with no `android.graphics` imports.
They move to `commonMain` unchanged.

### What changes mechanically

| Android API | Files | Replacement |
|---|---|---|
| `androidx.activity.compose.BackHandler` | several | CMP `androidx.compose.ui.backhandler.BackHandler` |
| `LocalContext` | 14 | injected `PlatformServices`; no `Context` in `commonMain` |
| `Intent` / `Uri` share and open | 9 | `Sharer`, `LinkOpener` |
| `rememberLauncherForActivityResult` | 5 | `FilePicker` callbacks |
| `stringResource` | 7 | inline the English strings, or `compose.components.resources` |
| `LocalConfiguration` | 3 | `BoxWithConstraints` / `LocalWindowInfo` |
| `BuildConfig` | 7 | `AppInfo` |
| `Toast` | 3 | snackbar through the existing host |
| `androidx.window` fold | 1 | `expect` returning `PHONE` |
| `android.graphics.Bitmap` | 2 | Skia decode in `commonMain` |
| `Icons.Rounded.*` | 31 | `compose.materialIconsExtended` |
| `WindowInsets` | 4 | CMP `safeDrawing` / `safeContent` |

### Compose Multiplatform pitfalls to budget for

1. **Insets and the shell.** The Android shell computes the floating bottom bar
   height in `BihuaApp` and applies it to the content box. On iOS the same code
   must run with `safeAreaInsets`; do not hard-code 72 dp or a notch assumption.
2. **Keyboard.** `adjustResize` does not exist. Compose iOS reports the IME as an
   inset. The report sheet, study list name, and pronunciation field must be
   tested with the keyboard open on an iPhone and an iPad.
3. **Text metrics and CJK.** iOS system CJK fonts differ in weight and advance.
   Budget a pass on Home, character detail, and the grade bar at 1.0, 1.3, 1.5,
   and 2.0 text size.
4. **Scroll physics.** Compose's own physics, not `UIScrollView`. Accept it; do
   not fake native feel.
5. **Dialogs and popups.** `Dialog` and `Popup` hosting differs. The report sheet,
   the outfit dialog, the grade legend, and the tour overlay all need a
   positioning pass, including iPad size classes.
6. **Back.** Wire the CMP `BackHandler` to the iOS swipe gesture and the
   Settings section stack, matching `navigation-deep` on Android.
7. **Reduce motion.** `LocalAccessibilityManager` / `UIAccessibility.isReduceMotionEnabled`
   must gate the tour typewriter, confetti, bobbing, and the splash hold.
8. **Dynamic Type.** Compose does not pick up iOS text size automatically. Map
   `UIContentSizeCategory` to a font scale, then verify 1.5x and 2.0x layouts.
9. **VoiceOver.** Compose semantics map to `UIAccessibility`, but the canvas needs
   custom actions ("Check", "Show answer", "Undo") that TalkBack gets from
   semantics. Test with the rotor.
10. **iOS 26 system UI.** The translucent keyboard can reveal content behind it,
   and native bars (tab, navigation, sheets) adopt Liquid Glass while Compose
   chrome does not. Test the keyboard on a short iPhone with the report sheet and
   the pronunciation field, and take a visual decision on the shell before the UI
   port grows.
11. **Status bar and home indicator.** The paper background must extend under
   both; a Compose `Scaffold` alone will not do it.

### Stylus force

`Stylus.kt` reads `InputDevice` and per-motion pressure. On iOS, `UITouch.force`
is only available through UIKit. Compose Multiplatform does not expose it
portably.

Plan:

- **Spike first.** A 40-line UIKit view that draws a line whose width follows
  `force`, mounted next to a Compose canvas. If `force` reaches Compose through
  an interop pointer path, keep pressure.
- If it does not: ship iOS v1 with uniform stroke width (finger behaviour), and
  keep the taper driven by speed, which is already implemented. Declare it: "pen
  pressure is Android-only for now" on the landing page.
- Do not build a custom UIKit canvas for iOS before the spike. That is a second
  renderer.

---

## 6. Feature behaviour that must differ

| Feature | Android | iOS | Guard |
|---|---|---|---|
| Local sync | Full TLS pairing | Absent | `capabilities.localSync = false`; hide row and search entry |
| Widgets / outfit | Today, Week, Word with outfit picker | Today, Week, Word with hour/state costume; outfit picker absent | `capabilities.widgets = true`; no outfit row on iOS |
| Foldables | Book, tabletop | Absent | `capabilities.foldables = false` |
| Crash attachment | Checkbox in report | Absent | `capabilities.crashAttach = false`; copy says traces are Android only |
| Rate | Play listing | `apps.apple.com/app/id…` | `capabilities.rateUrl` |
| Reminders | WorkManager one-shot | `UNUserNotificationCenter` | seam |
| Print | `PdfDocument` | `UIGraphicsPDFRenderer` | shared page model |
| Backup destination | SAF picker | Files app / share sheet | seam |
| Orientation | Manifest-configured | OS-managed, all orientations | no code |
| Splash | System splash + hand-off | `LaunchScreen` storyboard + hand-off | asset |

Every guard needs a test: the Settings search must not return a hidden row, and
`navigation-deep` on iOS must not find a Sync section.

---

## 7. Testing strategy

### Pyramid

| Layer | Where | What |
|---|---|---|
| Pure unit | `commonTest` | FSRS/SBD, queue, grading, pinyin, privacy masks, spellbook, backup codec, settings migration, date boundary, capabilities |
| Native unit | `iosTest` | SQLDelight driver, Okio paths, zip round-trip, `kotlinx-datetime` |
| Android only | `androidUnitTest` | Robolectric + Compose tests (35 files) |
| Swift unit | `iosAppTests` | every seam, with a fake Kotlin caller |
| UI | XCUITest | journeys, accessibility labels, keyboard, orientation |
| Visual | screenshot goldens | every screen, 3 device sizes, light/dark, 1.0/1.5 |
| Performance | Instruments + a timer harness | cold start, first Home frame, canvas frame time, DB open |

### Moving the 105 test files

- 70 are pure JVM: move to `commonTest` and switch `junit` to `kotlin.test`.
- 35 use Robolectric or Compose test: stay in `androidUnitTest`.
- Add native variants only where behaviour can differ (dates, files, SQL).

### New tests that matter

1. **Cross-platform database fixture.** Read an Android-created v18 database.
   Do not synthesise it; commit a real one (small: 20 cards).
2. **Backup round-trip across platforms.** Android export to iOS import, and the
   reverse, asserting card counts, due dates, and settings.
3. **Settings symmetry.** iOS hides sync/widget fields but still writes them to
   the backup and re-reads an Android file with them present.
4. **Recognition contract.** Recorded stroke fixtures for 爱, 谢, 学, 一 in each
   script go through the iOS recognizer; assert at least one expected candidate
   and that a missing model returns the local-only path, never a crash.
5. **Speech timeout.** A dead engine answers within 5 s with `Unavailable`, and a
   queued first tap does not hang.
6. **Reminder scheduling.** Cross the 4 AM day boundary and the next selected
   day; assert one notification, correct hour, and nothing when the queue is
   empty.
7. **PDF page model.** Worksheet grid geometry and the progress report render the
   expected page count, and text is extractable from the generated file.
8. **Capability hiding.** Search "sync" and "foldable" in Settings and get
   nothing on iOS; widgets are present.
9. **Dynamic Type layout.** 1.5x and 2.0x on Home, review, Settings, character
   detail: no clipped titles, no content under the home indicator.
10. **No other-platform leakage (2.3.10).** A test over every user-facing string
    in the iOS build asserts no "Android", "Play Store", or Play badge asset. The
    report composer prints `iOS: <version>` and the device model, never "Android
    release and SDK".
11. **Age gate honesty.** With the mature toggle off, no explicit gloss appears in
    Home, cues, word practice, or the dictionary; with it on, the confirmation is
    required first. A test enumerates the 52 mature rows and checks every surface.
12. **Download disclosure.** The reader notice contains a size, and a fresh
    offline launch reaches Home without it.

### UI checks with simulators

Devices: iPhone 16 Pro (or current) and iPad Pro 13-inch, portrait and landscape.

```bash
# boot and install
xcrun simctl boot "iPhone 16 Pro"
xcrun simctl install booted build/ios/Debug-iphonesimulator/Bihua.app
xcrun simctl launch booted com.bassmd.bihua

# appearance and text size
xcrun simctl ui booted appearance dark
xcrun simctl ui booted content_size extra-extra-extra-large   # if supported by Xcode

# screenshots
xcrun simctl io booted screenshot qa/ios/out/home-dark.png

# reset app data
xcrun simctl uninstall booted com.bassmd.bihua
```

Golden comparison: reuse `qa/visual.py`'s SSIM idea with a per-platform tolerance.
iOS text rendering differs from Android, so compare iOS against iOS, not across
platforms.

### Tooling (P0)

```bash
make ios            # iPhone 17 Pro: build, boot, install, launch
make ios-ipad       # iPad Pro 13-inch
make ios-test       # Android unit tests plus iosSimulatorArm64Test
make ios-check      # native tests plus the Xcode Debug build
make ios-pods       # CocoaPods, simulator-safe
make ios-mlkit-pods # ML Kit for a device build
make ios-shot NAME=home
make compliance      # Play + App Store policy checks
make ios-device      # signed build, install, launch on the connected iPhone
make ios-device-build
make ios-device-id
make ios-ipad-device  # signed build, install, launch on the connected iPad
make ios-ipad-device-shot NAME=home
python3 qa/ios/smoke.py
```

`qa/ios/driver.py` wraps `idb` with the same verbs as `qa/driver.py`
(`find`, `tap_label`, `text`, `swipe`, `screenshot`). Rotation, the keyboard
permission sheet, and VoiceOver stay XCUITest jobs.

### Journey harness

The Android harness is `uiautomator2` over adb, 119 scenarios. Do not try to
port it to XCTest wholesale. Two layers:

1. **`idb` (iOS Development Bridge)** mirrors the Android driver closely:
   `idb ui describe-all` gives the accessibility tree as JSON,
   `idb ui tap x y`, `idb ui text`, `idb screenshot`. Build `qa/ios/driver.py`
   with the same `Journey` API, so `qa/scenarios.py` steps can be reused.
2. **XCUITest** for anything idb cannot do: keyboard, swipe-back, rotation,
   VoiceOver rotor, notification permission dialogs.

Minimum iOS scenarios for launch (subset of the 119):

- `fresh`, `onboarding-pages`, `daily20`, `rotation-state`
- `settings-pages`, `report-flow` (no crash row), `offline-first-run`
- `screen-pass` on iPhone and iPad, light and dark
- `progress-screen`, `progress-all-time`, `characters-scroll`, `word-audio`
- `spell-quest`, `spell-flow`, `study-lists`, `worksheet`
- `a11y-labels` (new): every clickable node has a spoken label

Add `scripts/ios-journey.sh` with the same flags as `scripts/journey.sh`
(`--scenario`, `--device`, `--font-scale`, `--screenshots`), shelling out to
`xcrun simctl` and `idb`. Keep it out of `make check` until it is stable.

### CI

- macOS runner: `xcodebuild -scheme Bihua -destination 'platform=iOS Simulator,name=iPhone 16 Pro' test`.
- Gradle: `:composeApp:iosSimulatorArm64Test`, `:composeApp:testDebugUnitTest`.
- Cache Gradle, Xcode DerivedData, and the ML Kit pod.
- No device farm. Screenshots and journeys stay local, like Android.

---

## 8. Policy, landing, and store updates

Nothing here may claim an iOS feature that does not exist.

Updated 26 November 2026: `PRIVACY.md` and `docs/privacy.html` now cover both
platforms (Android-only sync, iOS Application Support and no iCloud backup, ML
Kit AI/third-party SDK, both permission sets, App Store rating row). The landing
page marks foldables and sync as Android, states that widgets are on Android
and built for iOS, and that iOS is in progress, and adds no App Store link before the build exists. The App Store
checklist and local listing are in `docs/ios-app-store-checklist.md` and
`store/ios-listing.md`. Deploy `docs/` before linking the policy from either
store.

### `PRIVACY.md` and `docs/privacy.html`

Rewrite into platform sections. Current text is Android-specific in these
places, with line references from today's file:

| Line | Today | Required change |
|---|---|---|
| 7 | meta description mentions "local sync" | "local progress, optional model download, no account, no analytics"; sync marked Android |
| 26 | "Local Wi-Fi sync between two devices" | "Android only" |
| 52 | "Internet and network state: handwriting-model downloads and local sync" | iOS: model download only; Android: both |
| 80, 94 | "Android cloud backup … disabled in the app manifest" | Add: iOS stores data outside iCloud backup, same promise |
| 100 | sync paragraph with NSD, TLS, pairing code | Move under "Android: local sync" |
| 104–105 | TLS note | Android only |
| 108–115 | Android permission list | Keep as Android; add an iOS list: notifications (optional), Files access via the picker, speech voices, no local network, no tracking |
| 129 | "The Android file picker chooses the destination" | "the system file picker (Android or iOS Files)" |
| 133 | PDF via Android picker | system share sheet on iOS |
| 136 | crash report fields include "Android release and SDK" | "Android release and SDK, or iOS version and device"; iOS launch sends no crash attachment |
| 139 | reminders via WorkManager, "Android may show it on the lock screen" | iOS: local notifications, same lock-screen caveat |
| 142 | "app sandbox" Android | "app sandbox on each platform" |
| 152 | "turn off … local sync at any time" | "(Android)" |

Also add one paragraph: **App Store link and rate row** send nothing until the
learner taps; the App Store page is opened by the OS.

### `docs/index.html` (landing)

| Line | Today | Change |
|---|---|---|
| 74 | kicker "Android · pen or finger · offline" | "Android and iOS · pen or finger · offline" |
| 88 | "Android 10+ · arm64" | Add "iOS 16+" beside it |
| 193 | "the widgets use the same paper-ink pet" | "on Android"; iOS widgets are deferred |
| 239 | "Apple Pencil is not supported because Bihua is an Android app." | Replace with the truth: Pencil draws; pressure detail is Android-only until the force spike lands |
| 522–538 | foldables section | Mark Android-only, or move under an Android heading |
| download section | APK only | Add the App Store link, keep the APK card, and label both |
| 595–602 | "Today and Week ahead widgets" | "on Android, and built for iPhone/iPad" |
| feature table / chips | "Local sync", "widgets" | Add "Android" tags |

Also update `docs/manifest.webmanifest` description if it names Android, and the
OG image alt text if it shows a nav bar.

### App Store listing (local file, not committed)

Per AGENTS, store material stays local in `store/`. Create
`store/ios-listing.md` with:

- Name: Bihua. Subtitle: characters by hand, offline.
- Description: writing-first, four scripts, no account, no ads, no purchase.
- Keywords: handwriting, Chinese, Japanese, Korean, characters, offline.
- Age rating: 16+, sexual content infrequent/mild (52 mature dictionary entries,
  off by default and gated).
- Privacy: no data collected by the developer; Diagnostics disclosed for ML Kit;
  no tracking; no advertising identifier.
- No IAP, no subscription, no donation link.
- Screenshots: iPhone 6.9-inch and iPad 13-inch, light and dark, from the frozen
  build; widget captures are allowed.
- Review notes: offline first run works, the model download is optional, there is
  no account, local sync and foldables are Android-only, and widgets are present.

### Things to keep true

- Privacy manifest `PrivacyInfo.xcprivacy`, plus the pod's own manifest.
- Export compliance: standard HTTPS only, `ITSAppUsesNonExemptEncryption = NO`.
- `UIFileSharingEnabled = false`, so app data is not browsable in Files.
- Data directory excluded from iCloud backup.
- App Store privacy answers must match the About paragraph that admits ML Kit
  diagnostics.
- Inside the iOS app, no string may name another mobile platform (2.3.10). Hidden
  features are absent, not labelled "Android only"; that phrasing belongs on the
  landing page, which is a website and not App Store metadata.
- The report composer prints `iOS: <version>` and the device model, never the
  Android release and SDK.

### Other docs to update with the first green iOS build

| File | Change |
|---|---|
| `AGENTS.md` | Replace "Android is the only client" with the iOS branch rules, the capability guards, and where the seams live. Keep the 1.1.2 freeze note until it lifts. |
| `docs/backlog.md` | Add the iOS track as its own P0/P1 list, or link this plan; the backlog stays the live checklist. |
| `docs/test-matrix.md` | iOS row: commonTest, iosTest, XCUITest, `idb` journeys, goldens. |
| `docs/screen-inventory.md` | Platform column; mark sync, widgets, foldables Android-only. |
| `docs/flow-inventory.md` | Add the iOS backup import/export flow and the App Store rate flow. |
| `docs/feature-inventory.md` | Same platform column; no iOS claim for widgets or sync. |
| `docs/play-store-submission.md` | Unchanged for Android. Do not reuse its permissions section for iOS. |
| `CHANGELOG.md` | A separate "iOS 1.0" section when it ships; Android entries stay untouched. |

---

## 9. Phases

Assumes one agent working continuously. Hours are work, not calendar.

| Phase | Work | Hours | Exit criteria |
|---|---|---|---|
| P0 | Toolchain spike: KMP module, CMP on the simulator, ASCII text field, Pencil force probe, ML Kit pod link | 16–24 | One Compose screen runs on both platforms from the same source |
| P1 | Source-set split; move pure packages; `expect/actual` skeleton; Android still green | 24–40 | `make check` and `make lint` green; `:composeApp:iosSimulatorArm64Test` runs |
| P2 | SQLDelight + schema 18 + fixture test; settings via path DataStore; content copy; backup zip | 40–64 | Android fixture opens and round-trips on iOS |
| P3 | Compose UI to `commonMain`: Home, review, characters, dictionary, settings, progress | 80–120 | Screen pass on iPhone and iPad, light/dark, 1.5x |
| P4 | Swift seams: ML Kit, speech, sounds, share, picker, PDF, reminders, network, links, clipboard | 48–72 | Every seam has a Swift test and a Kotlin fake |
| P5 | Spell quest, worksheet PDF, study lists, tour, onboarding | 40–56 | Feature parity list green except sync/foldables/crash |
| P6 | Test migration, idb harness, goldens, a11y pass, performance | 40–64 | 20 iOS scenarios green; no clipped layouts at 2.0x |
| P7 | Store: signing (needs the owner), TestFlight, privacy labels, screenshots | 16–24 | Build approved for TestFlight |
| — | **Total** | **304–464 h** | Roughly 25 to 38 days at a 55% duty cycle |

Order is not negotiable: P0 before P1, P1 before P2, P3 before P4 for each screen.

Add 30 to 50 hours if the shell decision in §2 lands on **option B** (native
UIKit tab bar and navigation stack). Add P0 half a day for the shell visual
gate: one Compose screen, keyboard open, on an iOS 26 simulator, light and dark.

P7 includes the compliance pass: privacy manifest, required-reason categories,
age rating questionnaire, DSA trader status, App Privacy labels, and the review
notes text.

---

## 10. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| CMP version/Kotlin mismatch | Build blocked for days | Pin CMP to Kotlin 2.2.x; spike in P0; never bump both in one commit |
| SQLDelight migration drift | Silent data loss on restore | Keep the original SQL text; fixture from a real Android DB; refuse mismatched schemas |
| ML Kit pod link with Kotlin/Native static framework | A day of linker flags | Podfile only; Gradle does not see ML Kit; prove in P0 |
| Pencil force not reachable from Compose | Ink feel differs on iOS | Spike in P0; ship uniform width and say so, or build the interop later |
| Compose iOS IME and CJK metrics | Unreadable layouts, broken input | Early screen pass; inline English strings; test at 2.0x |
| Insets and the floating bar | Content under the home indicator | Port `BihuaApp` measurement to safe-area insets; nav-gap equivalent on iOS |
| 35 Compose tests stay Android-only | Less coverage on iOS | XCUITest + `idb` scenarios; commonTest for all pure logic |
| No Apple account yet | No TestFlight, simulator only | Start enrollment in parallel; simulator evidence is enough until P7 |
| Android branch churn | Merge pain | Freeze 1.1.2; do the split on `ios/kmp` from the tagged release |
| Scope creep back to a second sync stack | Weeks lost | Local sync stays out of scope; guards in `Capabilities` and tests that assert the row is gone |
| Liquid Glass mismatch on iOS 26 | Looks dated, or a late shell rewrite | Option A with the migration seams; decide after the first visual pass; option B costs +30 to 50 h and is documented |
| Age rating disagreement over mature glosses | Rejection or a removal request | 16+ with the descriptor; off by default; offer a build without explicit glosses if a reviewer objects |
| DSA trader status missed | App pulled from all EU storefronts | Decide before submission; EU off is an acceptable first release |
| Missing required-reason declarations | ITMS-91053 warnings block the upload | Add only what the archive reports; never copy a vendor's reasons |

---

## 11. Definition of done

- [ ] Android `make check`, `make lint`, and the release journeys stay green
      through every move.
- [ ] `commonMain` has no `android.*` import; a CI grep enforces it.
- [ ] Every seam is an interface with an Android implementation, a Swift
      implementation, and a fake for tests.
- [ ] A real Android v18 database opens, reviews, and round-trips through backup
      on iOS.
- [ ] Settings search returns nothing for sync, foldables, or crash attachment
      on iOS, and the fields survive a backup. Widgets are present.
- [ ] Offline first run works: skip setup, Home, start a review, Show answer.
- [ ] Recognition degrades to local shape and order feedback when the model is
      absent, with a readable notice.
- [ ] Report composer works without crash files and says so.
- [ ] Rate opens the App Store listing.
- [ ] Screen pass on iPhone and iPad, light and dark, 1.0/1.3/1.5/2.0 text,
      portrait and landscape, with no clipped titles or content under the home
      indicator.
- [ ] VoiceOver: every control has a label and a hint; the canvas has actions.
- [ ] Reduce Motion stops the tour typewriter, confetti, and bobbing.
- [ ] 20 iOS journey scenarios and goldens are green and stored in `qa/ios/`.
- [ ] Policy pages name Android-only features and describe iOS storage,
      permissions, and no-iCloud-backup behaviour.
- [ ] Landing page has the App Store link and no Android-only claim left
      unqualified.
- [ ] Privacy manifest, App Privacy labels, and export compliance are consistent
      with the About text.
- [ ] Age rating is 16+ with the sexual content descriptor, and both new 2026
      descriptors (Shared Location, Dating) are answered no.
- [ ] DSA trader status decided before submission; EU availability matches it.
- [ ] Required-reason API categories match the first archive report and nothing
      unused is declared.
- [ ] The iOS build contains no "Android" string, Play badge, or other-platform
      imagery (2.3.10), and the report composer prints iOS metadata.
- [ ] The reader download notice names the size and is clearly optional; a clean
      offline launch reaches Home and a review (4.2.3(ii)).
- [ ] `store/ios-listing.md` exists locally; Play listing untouched.
- [ ] `git status` clean on the port branch.

---

## 12. Appendix: first commits

1. `build: add multiplatform targets and Compose Multiplatform plugin` — no code
   moves; Android builds identically; iOS framework links an empty screen.
2. `refactor: move pure packages to commonMain` — `fsrs`, `spellbook`,
   `content` models, `learn` pure files, `backup` codec; tests move too.
3. `refactor: inject platform services` — replace `LocalContext` and `Intent` in
   the 14 + 9 files with the seam interfaces; Android implementations keep the
   existing behaviour.
4. `feat: sql driver facade` — `SqlHandle` over `SQLiteOpenHelper`, then over
   SQLDelight; fixture test.
5. `feat: ios app shell` — Xcode project, Podfile, `MainViewController`, services
   registration, one screen.
6. Then one screen per commit from P3, each with its Android journey still green.

Do not squash these. The port is only reviewable as a sequence of behaviour-
preserving steps.
