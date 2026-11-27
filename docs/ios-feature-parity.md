# iOS feature parity

The port is judged against this table. Shared means the same Kotlin code runs on
both clients; Android-only means iOS hides the feature through `Capabilities`,
never labels it "Android only" inside the app (App Store 2.3.10).

Status legend: **shared** (common code today), **planned** (phase), **seam**
(Swift implementation of a Kotlin interface), **Android** (absent on iOS by
design).

| Area | Android | iOS | Phase | Notes |
|---|---|---|---|---|
| Onboarding pages | Yes | shared | P5 | Script, pace, brush, recall; skip works offline |
| Guided tour | Yes | shared | P5 | Anchors, spotlight, invite, rotation; no Android-only copy |
| Home | Yes | shared | done | Today, resume, word of the day, stats, calendar, pet; sample state until the shared stores land |
| Review session | Yes | shared | P3 | Meet/Trace/Write/Reveal, grades, reasons, undo |
| Pronunciation / meaning tasks | Yes | shared | P3 | Optional modes, no canvas |
| Characters grid and detail | Yes | shared | in progress | Search, status filters, real tiles, and a read-only detail with example words; notes, synonyms, history, and the store follow |
| Review loop | Yes | shared | in progress | Cue, ink, reveal, grade, next; sample cards and local advance until the shared FSRS/progress store lands |
| Dictionary | Yes | shared | P3 | Offline, four scripts |
| Practice canvas | Yes | shared | in progress | Free pad with a guide, clear, and undo; per-character brush, guide fading, and stroke playback follow |
| Compare | Yes | shared | P3 | Up to four characters |
| Word practice | Yes | shared | P5 | Whole-word canvas and one-by-one fallback |
| Spell quest and grimoire | Yes | shared domain + UI | P5 | Puzzle data bundled |
| Progress | Yes | shared | done | Level, known/review/new, calendar, learned chart; sample state until P2 |
| Progress PDF | Yes | shared model + Swift writer | P4 | Save/Share through the picker and share sheet |
| Worksheet PDF | Yes | shared model + Swift writer | P5 | A4/Letter |
| Study lists | Yes | shared | P5 | Create, rename, import, export |
| Known-list import | Yes | shared | P5 | Parser and preview; picker seam |
| Backup export/import | Yes | shared zip + picker | P4 | Android backup round-trips on iOS |
| Settings and search | Yes | shared | in progress | Appearance, course, pet, About/privacy; platform rows land with their capabilities |
| Reminders | Yes | seam | P4 | `UNUserNotificationCenter` one-shot |
| Speech | Yes | seam | P4 | `AVSpeechSynthesizer`, 5 s init timeout |
| Sounds | Yes | seam | P4 | `AVAudioEngine` with the same tables |
| Reader / handwriting model | ML Kit Android | seam | P4 | ML Kit iOS on device; zh-Hans probe recognized 一 on the connected iPhone; simulator has no arm64 pod slice |
| Ink pressure | Android stylus + finger | Compose pressure on device | P4 | Pencil force probed; ship uniform width if it is not reliable |
| File picker | SAF | seam | P4 | `UIDocumentPickerViewController` |
| Share sheet | Intent | seam | P4 | `UIActivityViewController` |
| Avatar / pet | Photo picker (1.1.x) | shared Compose pets | done | Twelve drawn Mò looks in a 3 x 4 picker; no photo library, file read, or image decoder. There is no name field and no photo access. |
| Rate | Play listing | seam | P4 | App Store URL once the record exists |
| Clipboard | ClipboardManager | seam | P4 | `UIPasteboard` |
| Local sync | Yes | **Android** | — | `capabilities.localSync = false`; absent from Settings and search |
| Widgets and outfits | Yes | **Android** | — | Deferred from the first iOS build. The SwiftUI WidgetKit code stays in `iosApp/BihuaWidgets` but is not an Xcode target and is not embedded; `capabilities.widgets = false`. |
| Foldable book layouts | Yes | **Android** | — | iOS is iPhone/iPad only, no foldable target; `capabilities.foldables = false` |
| Crash attachment in report | Yes | **Android** | — | `capabilities.crashAttach = false`; the composer says traces are Android only on the store page, not in the app |
| Privacy policy | Yes | shared link | P7 | About to the iOS-aware page |
| Privacy manifest | n/a | Yes | P0 | `iosApp/Bihua/PrivacyInfo.xcprivacy` |

## Parity gates

1. `docs/ios-feature-parity.md` has no "planned" row left for a 1.0 feature.
2. Settings search on iOS returns nothing for sync, widgets, foldables, or
   crash attachment.
3. The iOS bundle contains no "Android" string, Play badge, or Android-only
   imagery. The report composer prints `iOS` and the device model.
4. Every Android journey that covers a shared screen has an iOS equivalent in
   `qa/ios/` (P6).
5. An Android v18 database opens on iOS, accepts a review, and exports a backup
   the Android build imports.
6. Offline first run reaches Home and a review on both.
7. The reader notice names the model size and is optional on both.

## Deliberate differences

- iOS has no local sync, widgets, or foldable-fold layout in the first build.
- iOS notifications use the system permission sheet; iOS may hide the app's
  reminder row until permission is granted.
- The iOS home indicator and safe areas replace Android's gesture insets; the
  floating bar keeps its height from live insets.
- The App Store rate URL replaces the Play URL.
- The iOS report composer has no crash attachment.

## Testing the table

- Shared rows: `make ios-test` (commonTest on both targets) plus the matching
  `qa/ios` scenario.
- Seam rows: a Swift unit test with a fake Kotlin caller (`iosAppTests`, P4).
- Android rows: the existing journeys are the referee.
- Visual rows: iPhone 6.9-inch and iPad 13-inch, light and dark, 1.0/1.5/2.0
  text; compare iOS to iOS.
