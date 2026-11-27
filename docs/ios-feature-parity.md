# iOS feature parity

The port is judged against this table. Shared means the same Kotlin code runs on
both clients; Android-only means iOS hides the feature through `Capabilities`,
never labels it "Android only" inside the app (App Store 2.3.10).

Status legend: **shared** (common code today), **planned** (phase), **seam**
(Swift implementation of a Kotlin interface), **Android** (absent on iOS by
design).

| Area | Android | iOS | Phase | Notes |
|---|---|---|---|---|
| Onboarding pages | Yes | shared | done | Welcome, loop, course, pet; Skip and Start persist the choice |
| Guided tour | Yes | **not in 1.0** | — | Deferred; the Home invitation is hidden, so no dead control |
| Home | Yes | shared | done | Today, resume, word of the day, stats, calendar, pet; sample state until the shared stores land |
| Review session | Yes | shared | in progress | Real due cards, FSRS-7 grading, reveal, sticky grade bar, persisted history; recognition check and reasons follow |
| Pronunciation / meaning tasks | Yes | shared | P3 | Optional modes, no canvas |
| Characters grid and detail | Yes | shared | in progress | Real catalog, search, status filters, detail, example words, structure meta, and a sequential stroke-order guide; notes, synonyms, and review history UI follow |
| Stroke-order guide | Yes | shared + content export | done | Sequential playback from the Android glyph data; points animate, KanjiVG/Make Me a Hanzi outlines reveal in order; entry from Characters and Practice |
| Review loop | Yes | shared | in progress | Cue, ink, reveal, grade, next; sample cards and local advance until the shared FSRS/progress store lands |
| Dictionary | Yes | shared | done | Real offline word search for the active course; mature examples opt-in |
| Practice canvas | Yes | shared | done | Free pad with guide toggle, clear, undo, and a smooth local stroke; per-character brush sizes follow |
| Compare | Yes | **not in 1.0** | — | Deferred; no entry point is shown |
| Word practice | Yes | shared | done | Whole-word pad from Home and Dictionary; never scheduled |
| Spell quest and grimoire | Yes | **not in 1.0** | — | Deferred; the Home link and Study tools card are hidden |
| Progress | Yes | shared | in progress | Real level, known/review/new, calendar, learned chart, and study days; range selector and PDF follow |
| Progress PDF | Yes | **not in 1.0** | — | Deferred |
| Worksheet PDF | Yes | **not in 1.0** | — | Deferred |
| Study lists | Yes | shared | in progress | Create, rename, add/remove, persist; Android-format import/export follows |
| Known-list import | Yes | **not in 1.0** | — | Deferred |
| Backup export/import | Yes | shared JSON + picker | done | iOS JSON backup of settings, pet, and progress; system share sheet and Files picker. The Android database format is not read yet. |
| Settings and search | Yes | shared | in progress | Appearance, course, pet, daily goal, practice, mature, motion, reminders, study lists, backup, reset, About/privacy; search row follows |
| Reminders | Yes | seam | done | Optional local weekday notifications at the saved time; no server. Time/day picker follows. |
| Speech | Yes | seam | done | `AVSpeechSynthesizer` with a course-matched voice |
| Sounds | Yes | **not in 1.0** | — | Deferred; the setting is hidden |
| Reader / handwriting model | ML Kit Android | **not in 1.0** | — | Deferred; the iOS review is self-graded and downloads no model |
| Ink pressure | Android stylus + finger | **not in 1.0** | — | Uniform width; Pencil pressure deferred |
| File picker | SAF | seam | P4 | `UIDocumentPickerViewController` |
| Share sheet | Intent | seam | P4 | `UIActivityViewController` |
| Avatar / pet | Photo picker (1.1.x) | shared Compose pets | done | Twelve drawn Mò looks in a 3 x 4 picker; no photo library, file read, or image decoder. There is no name field and no photo access. |
| Rate | Play listing | **not in 1.0** | — | Deferred until the App Store record exists |
| Clipboard | ClipboardManager | **not in 1.0** | — | Deferred |
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
