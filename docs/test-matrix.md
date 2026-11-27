# Flow and test matrix

Scope: 1.1.2 (versionCode 23), phone, foldable, tablet, light and dark,
and 2.0x text. The automated suite is JVM/Robolectric plus the local journey
harness; pure composables and physical hardware still need the device pass.

## Local quality loop

No external CI is used for these passes. The release gate runs on the
maintainer's machine:

```bash
make check          # compile, unit/Robolectric tests, content audit, APK verify
make lint
make quality        # check + phone screenshot goldens + layout audit + monkey
make quality-full   # phone, fold-inner, and tablet visual matrix
make monkey         # deterministic random walk
make perf           # gfxinfo snapshot for Characters and a daily session
```

- `screen-pass` captures Home, every tab, Settings sections and dialogs, the
  character detail, dictionary, study tools, study lists, learn reveal, the
  spell gate, scene, cast, and grimoire.
- Screenshot goldens live in `qa/goldens/`; compare with `--goldens`, update
  after an intentional UI change with `--update-goldens`.
- `--layout-audit` writes hierarchy bounds and fails on off-screen, zero-size,
  or overlapping named actions, plus too-small touch targets as warnings.
- `spell-long` injects a ten-line problem and asserts only the problem body
  scrolls while Draw, Grimoire, and the title stay fixed.
- `monkey` is a deterministic random walk across the busiest tabs; it checks the
  app stays foreground and audits layout every ten actions.
- Compose UI tests (`ScreensSmokeTest`, `LayoutInvariantTest`,
  `ModelDownloadNoticeTest`, `StudyToolsSheetTest`) run on the JVM with
  Robolectric, including 1.5x text and tablet width.

## iOS port (ios/kmp)

P0 complete, P1 in progress. iOS compares against iOS, never against Android:
text metrics and rendering differ.

| Layer | Command | What |
|---|---|---|
| Shared pure tests | `make ios-test` | `commonTest` on the Android and iOS targets (`spellbook` today) |
| Android tests | `make test` | JVM/Robolectric stay in `androidUnitTest` |
| iOS native | `iosSimulatorArm64Test` | files, zip, SQL driver, dates (P2) |
| Simulator smoke | `python3 qa/ios/smoke.py` | boot, install, launch, app label, screenshot |
| Driver | `qa/ios/driver.py` | idb `find`, `tap_label`, `text`, `swipe`, `screenshot` |
| Build gate | `make ios-check` | native tests plus the Xcode Debug build |
| Native shell | `make ios` / `make ios-ipad` | tab bar, large titles, safe areas, Dynamic Type, light/dark |
| Store policy | `make compliance` | documents, permissions, privacy manifest, export, other-platform wording |
| XCUITest | `iosAppTests` (P6) | rotation, keyboard, VoiceOver, permission sheets |

Devices: iPhone 17 Pro, iPad Pro 13-inch, iPhone Duo (foldable), light and dark,
1.0/1.5/2.0 text. Screenshots go to `.qa/ios/`. The parity table is
`docs/ios-feature-parity.md`.

## 1.1.2 additions

| Flow | What is covered | Where |
|---|---|---|
| Progress report PDF | Selected range, cumulative characters, level rings, quality, levels, characters, study log, empty and starting states, Chinese/Japanese/Korean, year and all time | `progress-pdf-*` journeys; `ProgressReportPdf` |
| Whole-word practice | One-page canvas by default, optional clues and stroke playback, on-device multi-character check, one-by-one fallback without the model, back to Home | `word-whole-canvas`, `word-one-by-one-no-model`, `word-practice-home-back` |
| Detail rotation | The character page and an open mnemonic draft survive a phone turn | `characters-rotation`, `mnemonic-rotation` |
| Long detail sections | Review history and words collapse and expand | `characters-scroll` |
| Study tools completeness | Character PDF opens from Study tools, not only Settings | `study-tools-pdf` |
| Custom range | Exact start/end days, one day, presets, range-aware charts and history | `progress-custom-range`, `progress-custom-single-day` |
| Optional drawing | Writing can be turned off while meaning or pronunciation stays on; at least one mode remains | `settings-writing-mode`, `modes-*` |
| Grade reasons | Optional post-grade note; never a second grade | `grade-reasons` |
| Mnemonic palace | Chinese sound roles, Japanese/Korean reusable parts, kana/Hangul hidden | `mnemonic-studio`, `mnemonic-japanese`, `mnemonic-korean`, `mnemonic-kana-hidden`, `mnemonic-hangul-hidden` |

## 1.1.1 additions

| Flow | What is covered | Where |
|---|---|---|
| Reveal writing | Cue → Show answer → grade with no canvas; new cards go Meet → Reveal and skip Trace | `ReviewTaskTest`, `reveal-review` journey |
| Reveal pronunciation | Character → Show answer → grade with no typing field or unfair grade suggestion | `ReviewTaskTest`, `reveal-review` journey |
| Meaning review | Type a wording → reveal clean gloss and example word → add synonym → grade | `MeaningSanitizerTest`, `meaning` journey |
| Meaning reveal | Reveal-only meaning, no typing field, synonyms still available | `meaning-reveal` journey |
| Meaning data | Blank, placeholder, structural, overlong, duplicate synonym, cap; shipped glosses stay 98% teachable | `MeaningSanitizerTest`, `ContentDatabaseTest` |
| Synonym management | Add and remove from character detail; card and chips render at 1.5x; detail bottom reachable | `synonyms` journey |
| Task mix | Writing keeps half when meaning or pronunciation is enabled; meaning never starts a new card | `ReviewTaskTest` |
| Settings switches | Writing and pronunciation are independent, off by default, and survive sanitize | `SettingsSanitizeTest`, `reveal-review` journey |
| Deep navigation | Every Settings section, Compare, Paper, Practice picker, study lists, and study tools sheet, closed with system back | `navigation-deep` journey |
| Large-text layout | Home study tools, Compare header, Paper header, synonym header, outfit dialog at 1.5x; outfit scroll at 2x | manual phone pass plus `navigation-deep`, `synonyms`, `settings-outfit` at 1.5x |
| Script switch | Old course never flashes while the new one loads | `script-switch-loading` journey |
| Known-list import | Character or short-word lists; prose skipped; preview count and Confirm/Cancel; 500 cards spread across 10–45 days with no day over the cap | `KnownListTest`, `ProgressStoreKnownImportTest`, `known-list-preview` journey |
| Progress storage | Schema 18 migration, WAL, report/history indexes, study-day SQL boundary | `ProgressStoreMigrationTest`, `ProgressStoreRangeTest`, `ProgressStoreKnownImportTest` |
| PDF export choice | Save to Files or Share PDF for Progress and practice sheets; cached share PDF is complete | `PdfExportTest`, `progress-pdf-save`, `progress-pdf-share`, `worksheet` journeys |
| Word practice layout | One contiguous word cue, capped guide, scaled brush, compact brush menu, numbers only with strokes | `word-whole-canvas` journey at phone/tablet/fold |
| Loading states | Model download and recognition timeouts, the shared 900 ms readiness cache/probe, TTS 5 s init timeout, content Retry, session prepare | `InkRecognition`, `ModelPreparation`, and `OnboardingStart` tests plus manual phone pass |
| Widget parity | Skipped and suspended cards excluded; refresh on stop | `WidgetCopyTest`, manual launcher pass |
| Widget pet costumes | Study cap, word monocle, witch hat in the evening, vampire at night, nightcap on vacation, party hat at all-clear; pinned outfit wins | `WidgetCopyTest`, manual launcher pass |
| Widget layout states | 2×2 Today and 4×2 Word, counts, all-clear, vacation, 1.5× font, night text, backgrounds | manual launcher pass |
| Grade legend | “?” opens Again/Hard/Good/Easy from the session prompt | `grade-help` journey |
| Widget outfit | Pin Witch in Display and verify the row persists | `settings-outfit` journey |
| Progress ranges | 7D, 90D, 1Y, then 30D; Momentum and range labels follow; actual streak survives every window | `progress-ranges` journey, `StudyStreakTest` |
| Progress empty states | No grades, no reviews, no memory data, nothing scheduled, nothing stuck; no `0% passed` | `progress-empty` journey |
| Progress course scoping | Reviews, practice time, pace, task mix, activity, and growth ignore the other three courses | `ProgressStoreModeTest` |
| Progress range scope | Recent mistakes and Recently learned follow 7D/30D/90D/1Y/All; the cumulative line reports a tapped day's total | `ProgressStoreRangeTest`, `progress-screen` |
| Custom date range | A real start/end day, a single Monday, last week, future end clamped to today, and a past end that excludes later reviews | `ProgressWindowTest`, `ProgressStoreRangeTest`, `progress-custom-range` |
| Recognition-only practice | Drawing off with pronunciation, meaning, or both; lessons skip trace; no canvas appears; the last mode cannot be switched off | `ReviewTaskTest`, `SettingsSanitizeTest`, `modes-*`, `settings-writing-mode` |
| Grade reason note | After Again/Hard, the note is optional, saved separately from the grade, and skipped without blocking the session | `ProgressStoreReasonTest`, `grade-reasons` |
| Mnemonic palace | Chinese sound roles; Japanese kanji parts; Korean Hanja parts; kana/Hangul hidden; saved pages open from Characters, lessons, and reviews | `ProgressStorePalaceTest`, `ProgressStoreMnemonicTest`, `PalaceSuggestionsTest`, `HanCharactersTest`, `MnemonicPiecesTest`, `mnemonic-*` |
| Sync merge | Cards keep the newer state, review logs union without duplicates, notes keep the newer text, XP is recomputed | `ProgressStoreMergeTest` |
| Level progress | HSK, Jōyō, and Hanja levels sit under Momentum and use accurate script labels | `progress-screen` screenshot |
| Bottom bar chrome | Scroll viewport meets the floating bottom bar at 1x and 1.5x; no fixed-gap leftover and no overlapped content | `nav-gap` journey |
| Caught-up copy | “All done for today” appears without a duplicate “All caught up” | `caught-up` journey |
| Import resilience | UTF-8 BOM and nested JSON names | `StudyListImportTest` |
| List name uniqueness | Long names keep their numeric suffix | `StudyListsTest` |
| Spell quest | Gate, scene, reading gift, real draw/cast, brush tray, one-way clues, effect frames, note popup, wrong-answer path; phone portrait and landscape, tablet, fold | `spell-quest` journey; `spell-flow`, `spell-long` phone-landscape |
| Daily word sharing | Same study day + course agree across fresh repositories; a month has no repeats; courses walk separate sequences; mature setting does not change it | `FeaturedWordTest` |
| Puzzle place fit | Every obstacle has 8+ compatible settings; 2,000 generated scenes keep the obstacle and place kinds aligned; family puzzles have 2+ solutions; every result names the obstacle's own outcome; repeated visits reuse cached parts | `ShippedPuzzlePartsTest`, `PuzzlePartsCacheTest` |
| Writing reveal | Show answer is available before ink, after ink, and while recognition settles; it is in both phone and wide review layouts | `writing-reveal`; `review-modes` |
| Model preparation | Tags per script (Korean needs ko + zh-Hant), overlapping downloads keep the notice until the last finishes, an extra end cannot wedge it; the shared notice follows Home → Settings; a fresh quest does the real download | `ModelPreparationTest`; `model-notice`; `spell-quest` |
| Offline reading | No-model gate offers the book, the scene hides Draw, practice falls back to shape/order, search offers text | `spell-offline`, `practice-check` |
| Brush presets | Each tool restores its own values; edits save under the active tool; backup carries the map; bad keys/values are clamped | `brush-presets`, `SettingsSanitizeTest`, `SettingsTransferTest` |
| Reminders | Enable, time, days, test notification, disable; off state is explicit and the preview stays visible | `reminders` |
| Widget refresh | Next hour, day-part, or study-day boundary; one inexact alarm; six-hour fallback | `WidgetRefreshTest` |
| Spell quest scripts | Same flow in Traditional, Japanese kanji, and Korean Hanja; entry from Home and Study tools | `spell-quest-ja`, `spell-quest-ko`, `spell-quest-traditional`, `spell-entry-ja`, `spell-entry-ko` |
| Spell quest flow clarity | Onboarding mention, top Home link, gate copy, every draw control label, one-way clues, one-layer back, grimoire chapter names, clean leave | `spell-onboarding`, `spell-flow` |
| Spell quest long play | 50 chapters per script, six scenes each (1,200 puzzles); every scene solvable, all shapes and chapters seen, no repeated spell in a chapter, every unhelpful family has a hint | `SpellQuestStressTest` |
| Spell quest theme and motion | Dark palette, reduce motion, large text, effect frames | `spell-quest-dark`, `spell-quest-calm`, `--font-scale 1.5` |
| Spellbook data | 1,387 spells across four scripts, 35 families, 10 chapters, ≥30 choices per chapter, unique characters per script | `SpellbookTest` |
| Puzzle shapes | Family doors, whole-chapter doors, and exact-shape puzzles; the scene spell always solves; narrow rules reject wider casts | `QuestGeneratorTest`, `PuzzleForgeTest` |
| Forge honesty | Every family carrying an accepted tag solves; targeted generation; no repeated place; 950k+ place combinations | `PuzzleForgeTest`, `QuestGeneratorTest`, `ShippedPuzzlePartsTest` |
| Gift playback | Starts empty, plays the real guide (or a readable fallback) in the circle, actions wait for the stroke end | `spell-quest` journey, screenshots |
| Spell quest copy and logic | Scroll word, family label, definition gloss, fixed pacing, tip card, emoji | `SpellQuestLogicTest` |
| Spell quest audio | Ambient bed and cast/wrong/gift/reveal/walk/chapter cues; quiet, no buzzer | manual audio pass |
| Guided tour | Invite dismiss, replay from Settings, tap and Next through all ten stops, Back to app | `tour` journey |
| Tour dark theme | Same tour with the dark palette; card, spotlight, and pet stay legible | `tour-dark` journey |
| Tour copy and anchors | Every stop has a live anchor; copy explains reviews and grades without SRS jargon | `GuidedTour` review plus `tour` journey |
| Tour motion and sound | Reduce motion, TalkBack full line, optional sound cues | manual phone/fold/tablet pass |
| Tour large text | 1.5x compact and tablet; top/bottom card sheet, no clipped body or wrapped controls | `tour` journey with `--font-scale 1.5` |

## Automated coverage

| Flow | What is covered | Where |
|---|---|---|
| Onboarding choices | Page/back, script choice, pace limits, saved state | `LearnSessionStateTest`, `SessionProgressionTest`; visual pass |
| Home queue | Due/new counts, skipped and suspended hiding, empty states | `SessionFilterTest`, `ReviewQueueTest`, `ProgressStoreGlyphTest` |
| Review session | Repeat-until-pass, first-grade-only scheduling, resume validity | `SessionProgressionTest`, `LearnSessionStateTest`, `Fsrs7Test` |
| Reading check | Pinyin and romaji normalisation, tone-only feedback | `PinyinTest`, `ReadingCheckTest`, `RecallPromptTest` |
| Task choice | Write/read mix by repetition count | `ReviewTaskTest` |
| Ink and strokes | Undo/redo, stroke matching, missing guide, drawing matrix | `InkStateTest`, `StrokeGradeTest`, `StrokeOrderGraderTest`, `StrokeOrderMissingTest` |
| Drawing recognition | Script filtering, score ordering, target script, supplementary Han, malformed candidates | `RecognitionSanitizerTest`, `ContentDatabaseTest` |
| Stroke coach | Clean, reversed, out-of-order, missing, extra, no-guide states; curved and U-shaped strokes; correct traces never called reversed | `StrokeCoachTest`, `StrokeReversalRegressionTest` |
| Worksheets | A4 PDF export and saved file header | `worksheet` journey |
| Dictionary | Live search, detail, reading, word practice handoff | `dictionary` journey |
| Word practice guide | Off/faint/strokes, numbered playback, stroke count and reading | `word-guide` journey |
| Answer privacy | Target hidden in writing recall; reading masked in pronunciation | `RecallPromptTest`, `ReadingPrivacyTest`, `review-modes`, `pronounce-kana` journeys |
| Guide rendering | Tapered strokes, paper halo, grid behind the guide, numbered start badges | visual check on 一, 学, 谢, 爱; `stroke-clean` journey |
| Worksheet print | Clean paper cells, model/trace tints, cover contents, complete PDF | `worksheet` journey and PDF page review |
| Worksheet selection | Course start number, picked set, imported .txt/.csv, all, reverse | manual review plus `worksheet` journey |
| Worksheet templates | Trace, two traces, model/copy, memory, trace only, alternate, poster; A4/Letter; boxes and pages | manual PDF review (A4 and Letter poster) |
| Worksheet builder look | Filled pills, inset panel, start-at field, pick/import tiles, character preview | manual phone and tablet review |
| Kana guides | Hiragana and katakana use round stroked paths; Han keeps the filled brush | manual phone/tablet capture of あ and ア |
| Character categories | Level headers with counts, collapse/expand, flat search results | manual tablet and fold review |
| Stroke coaching | Exact-stroke highlight, Show me/Trace it/Write alone, mint fixed-stroke closure | manual review plus `stroke-clean` journey |
| Word practice flow | One-character reveal, previous characters stay visible, fixed play slot, compact history | manual review plus `word-guide` journey |
| Practice feedback | Handwriting check, coach, repair modes, shape evidence, mint closure | manual phone/tablet review |
| Pinyin display | Tone marks default, numbers optional, numeric input still accepted | `PinyinTest` and `ReadingCheckTest` plus manual Settings review |
| Feedback colour | No punitive red for a missing stroke or a recognition difference | manual light/dark review |
| Home-screen widgets (Android) | Today/Word layouts inflate, time copy, action, picker preview | `WidgetCopyTest` plus manual launcher review |
| Widgets (iOS) | Today/Week/Word in small, medium, and large; hour palette; accessory families | Debug gallery at `-widget-gallery`, WidgetKit extension build, real-device pass |
| Study lists | Named character and word lists, migration, presets, quick add, word bookmark and queue | `StudyListsTest`, `StudyListImportTest`, `study-lists` journey |
| List import | TXT, CSV, TSV, JSON streaming; BOM, nested names; words kept; no item cap | `StudyListImportTest` plus manual file-picker import |
| List export | `.txt` round-trips characters and words | manual file-picker export |
| SBD scheduler | Reference recurrence, pass/fail, interval inversion, per-user fit | `Sbd7Test`, `SbdOptimizerTest`, `ProgressStoreSbdTest` |
| Target recall | 70–97% changes intervals for SBD/FSRS | `Sbd7Test`, `ProgressStoreSbdTest`, manual Settings review |
| Onboarding | Five pages, one schedule choice, target recall, writing-only default | manual phone/tablet review |
| Dark theme | Pet stays paper-and-ink, contrast, navigation bar, every tab | `appearance` journey plus manual light/dark review |
| Paper check | Strokes toggle after reveal, back to character details | `paper-compare` journey plus phone review |
| Compare write drill | Fixed canvas, Off/Faint/Strokes, undo/redo/clear, back | `paper-compare` journey plus phone review |
| Word of the Day quality | At least 365 non-mature quality candidates per course; curated + pool | `ContentDatabaseTest` |
| Word of the Day audio | Speaker present, tap is safe, missing voice is reported | `word-audio` journey plus manual audio pass |
| Cold start | Minimum hold, reduce-motion, tap-to-skip | manual launch on phone and tablet |
| Pet identity | Mò is the launcher icon, profile avatar, top icon, tour, and completion pet | manual light/dark review |
| Review tasks | Writing and typing paths, wrong/correct/tone feedback, real guide-stroke drawing | `review-modes`, `draw-recognition` journeys; `ReadingCheckTest`, `ReviewTaskTest` |
| Guide and hints | Fade rules for guide, story, and mnemonic | `GuideScaffoldTest`, `MnemonicModeTest` |
| Known list | Import matching and header validation | `KnownListTest`, `CharacterSearchTest` |
| Character search and filters | Query, level, frequency, script filter | `CharacterSearchTest`, `ScriptFilterTest` |
| Mature examples | Hidden by default in cards, search, cues, recent words | `WordContextVisibilityTest` |
| Progress statistics | Due forecast, learned history, totals, review load | `ReviewLoadTest`, `Fsrs7Test`, `ProgressStoreGlyphTest` |
| Shared glyphs | Card lookup, old-card fallback, recently learned, daily goal | `CardKeyTest`, `ProgressStoreGlyphTest` |
| Settings | Day boundary, ease-back, vacation, reminders, transfer | `DayBoundaryTest`, `EaseBackTest`, `VacationTest`, `SettingsTransferTest`, `SettingsSanitizeTest` |
| Reminders | Next local hour, selected days, script and skip filter | `ReminderTimeTest`, `ReminderSelectionTest` |
| Backup and import | Zip shapes, settings round trip, newer/corrupt database rejection | `BackupArchiveTest`, `BackupCodecTest`, `SettingsTransferTest`, `ProgressStoreImportTest` |
| Sync | Pin code normalisation, pinned TLS accept/reject, certificate lifetime, IPv6 host URL | `SyncCodeTest`, `SyncTlsTest`, `SyncHostTest` |
| Profile photo | Large source downscaled, old avatars removed | `AvatarTest` |
| Palette | Light/dark contrast | `PaletteContrastTest` |
| Formatting | Counts, durations, dates | `FormattingTest` |

## Local full-app journeys

`scripts/journey.sh` builds the debug APK, boots local AVDs, seeds deterministic
progress states, and drives the real app with `uiautomator2`.

```bash
make journey
make journey-standard
make journey-full
scripts/journey.sh --scenario review-modes --profile phone
scripts/journey.sh --scenario daily100 --profile phone --drain
scripts/journey.sh --scenario nav-gap --profile phone-compact --font-scale 1.5
```

All runs write screenshots, logs, JUnit XML, and an HTML report under
`.qa/runs/`, which is ignored by git. The harness covers onboarding, 20/80/100
review days, completion, resume after process death, vacation, ease-back, study
tools, shared glyphs, fold posture, accessibility, writing recognition, and
typing pronunciation.

## On-device smoke

`scripts/smoke.sh` launches the installed debug build and checks the Home tab,
Settings, Display, Practice, and a Settings round trip. It waits for cold start
instead of assuming a fixed delay. Run it on both form factors:

```bash
BIHUA_DEVICE=<phone> scripts/smoke.sh
BIHUA_DEVICE=<fold> scripts/smoke.sh
```

`scripts/device.sh a11y` reports clickable nodes without a spoken label. Treat
its findings as prompts, not proof: a wrapper node can be silent while its child
label is still read. Confirm each one with TalkBack before changing semantics.

## Manual device pass

These are not fully automated yet.

| Flow | Phone | Foldable | Notes |
|---|---|---|---|
| Onboarding and restore | Yes | Yes | Rotation and back checked |
| Home, book spread, dock | Yes | Yes | Book layout at 1:1 and 4:3 |
| Review, grade, undo, leave | Yes | Yes | Resume after process death |
| Pronunciation and TTS | Yes | Yes | Voice depends on installed engine |
| Practice, word practice, paper drill | Yes | Yes | Paper layout on wide screens |
| Compare and write-both | Yes | Yes | Family compare |
| Characters, detail, notes, study tools | Yes | Yes | Mature confirmation |
| Progress and empty states | Yes | Yes | Forecast now hides suspended and skipped cards |
| Settings, backup, import, export | Yes | Yes | Invalid file errors |
| Local sync | Yes | Yes | Two devices on one Wi-Fi |
| Reminders, permission, test notification | Yes | Yes | Off by default |
| Dark theme and 1.5x text | Yes | Yes | Bottom chrome is automated with `nav-gap`; full TalkBack sweep at 2x still open |
| Fold postures | n/a | Partial | Folded and unfolded checked; tabletop is detected but not used |

## Open gaps

1. No Compose instrumentation tests. A flow test for onboarding, grade, undo,
   and restore would catch more than the current unit suite.
2. Tabletop posture is detected but layouts do not use it yet.
3. A full TalkBack sweep on all screens and both fold postures remains.
4. Physical stylus pressure and palm rejection still need a hardware pass.

## Fixed in this pass

| Bug | Fix | Regression |
|---|---|---|
| Phone content left a visible gap above the floating bottom bar | Measure the bar outer height and use it as the content inset; Settings Scaffold zero insets | `nav-gap` at 1x and 1.5x |
| Settings list stopped short because its Scaffold applied navigation-bar insets twice | `contentWindowInsets = WindowInsets(0, 0, 0, 0)` | `nav-gap` |
| Progress analytics mixed every course together | Mode-filtered store queries and `mode.name` keys | `ProgressStoreModeTest` |
| Progress streak was capped at 7/30/90 days by the selected window | `studyStreak` reads up to 366 days and is passed separately | `StudyStreakTest`, `progress-ranges` |
| Empty Progress showed `0% passed` and blank cards | New window/no grades copy and empty states for memory, daily reviews, and forecast | `progress-empty` |
| “Your synonyms” broke into “Your / synonym / s” at 1.5x | Shorten the action to “Add” above 1.25x | `synonyms`, `navigation-deep` at 1.5x |
| Widget outfit dialog labels were unlabeled/clipped at large text | Two columns through 1.7x, one scrolling column above, labeled rows | `settings-outfit` at 1x/1.5x, 2x visual pass |
| Mixed-session bar stayed at zero because the pending queue shrank | Stable block math against the original block | `SessionBlockTest` |
| "Skip setup" created a 10-new session despite the 5-new recommendation | Shared onboarding recommendation constant | manual |
| First settings frame could migrate shared glyph cards before saved settings loaded | Removed the destructive startup migration and added a legacy-card fallback | `ProgressStoreGlyphTest` |
| Turning off shared glyphs dropped history for every script except the first | Keep old shared cards readable until each script earns its own | `ProgressStoreGlyphTest` |
| Phone free practice ignored the Outline guide setting | Pass `outlineOnly` in both layouts | manual |
| Progress export closed the live database and could race Home queries | `VACUUM INTO` snapshot | `ProgressStoreGlyphTest` |
| Backup imports could allocate unbounded memory from a bad file | Capped raw file and zip entry reads | `BackupArchiveTest` |
| Restore fallback and temp database files accumulated in cache | Delete on success or successful rollback | manual |
| Restored `mixInterval` was not clamped | Settings sanitizer | `SettingsSanitizeTest` |
| Finished session asked to resume or discard an empty queue | Completion state exits straight Home for button and system back | manual |
| Completion copy claimed unfinished cards after a first miss | Copy reports the finished session instead of `learned` count | manual |
| Sync pairing field showed a digits-only keyboard for a base32 code | Letter-capable ASCII keyboard | manual |
| IPv6 link-local sync URL kept a raw zone-id `%` | Escape it as `%25` and bracket the literal | `SyncHostTest` |
| Lesson step bar stopped at two thirds | Divide by the last step index | `SessionBlockTest` |
| Schema version 0 progress files could reach an invalid upgrade | Reject them during import validation | `ProgressStoreImportTest` |
| Reduce motion still animated the session bars and XP count | Snap animations when the setting is on | manual |
| Reminder counted hidden kana/Hangul and skipped or suspended cards | Shared queue filter in `ReminderSelection` | `ReminderSelectionTest` |
| A reminder set seconds before the hour moved to the next day | Removed the 30-second guard | `ReminderTimeTest` |
| Shared-glyph cards never appeared in Recently learned | Resolve by character or glyph group | `ProgressStoreGlyphTest` |
| Progress totals ignored a legacy character card | Use `cardFor` | `ProgressStoreGlyphTest` |
| Forecast included suspended and skipped cards | Filter by due date | `ProgressStoreGlyphTest` |
| Shared glyphs did not count toward another script's daily goal | Count shared cards for every script | `ProgressStoreGlyphTest` |
| Full-resolution camera photo decoded into a 56 dp avatar | Bound decode, rotate EXIF, save at 768 px | `AvatarTest` |
| Restored brush and timestamps could be negative or non-finite | `BrushConfig.sanitized`, profile and timestamp clamps | `SettingsSanitizeTest` |
| Corrupt settings in an archive left a partial import dialog | Decode both halves before updating state | manual |
| Daily reminder worker opened a content database and never closed it | `close()` in the worker finally block | compile/manual |
| One-hour sync certificate could expire while the screen was open | 24-hour certificate lifetime | `SyncTlsTest` |
| Home controls had no spoken label | Explicit semantics on the five controls | TalkBack audit |
| Today widget showed only remaining counts | Two big done-of-total numbers, a plain next-hours line, and a Week ahead widget with the next days; the line refreshes on the hour | `ReviewWidgetBinderTest`, `ReviewWidgetPreviewTest`, `ProgressStoreModeTest` |
| Learners who wanted the week on the home screen had to toggle one widget | Added a dedicated Week ahead widget beside Today and Word of the Day; all share the binder, palette, and snapshot | `ReviewWidgetBinderTest`, `ReviewWidgetPreviewTest` |
| Progress had no all-time window and long charts were unreadable | Added All time, honest empty copy, and 160/180-point bucketing that keeps totals exact | `progress-screen`, `progress-ranges`, `ProgressStoreModeTest` |
| Calendar weekday initials sat at the left edge of each column | Use the grid's 4 dp gaps and centre the letters over the day boxes | `progress-screen`, `progress-screen-large` |
| Year and All showed a 53-week calendar wall | GitHub-style 7×N mint heatmap with month labels, an M/W/F gutter, a legend, and horizontal swipe; it fills a tablet and auto-opens on the newest weeks | `progress-screen` 1Y/All on phone, tablet, dark; `progress-all-time` |
| A crash left only a mailto form, with no device context | Local crash files, a fixed formatter shared by the handler and composer, a review-before-send dialog, and a rate row; the composer keeps its words and actions above the landscape keyboard | `CrashReportTest`, `CrashLogTest`, `ReportTextTest`, `RateLinkTest`, `report-flow`, `report-flow-dark`, `report-flow-large`, `report-flow` phone-landscape |
| The report sheet sat low and against the right edge in landscape | The dialog window uses the full display and cutout, lifts by the status bar, and centres the sheet on both axes | `report-flow` phone-landscape before/after screenshots |
| Onboarding pages were only exercised on a phone | Every page, the extra-practice card, and Start writing are captured on phone, tablet, 7-inch tablet, and both fold sizes | `onboarding-pages` tablet, tablet-7, fold-inner, fold-outer |
| A reviewer on a clean offline device could think setup is blocked | With airplane mode on, Skip setup opens Home, Start studying opens a review, and Show answer is available without the reader | `offline-first-run` |
| Landscape Practice starved the canvas and could throw | Landscape uses the two-pane book layout; Home uses the tablet two-page dashboard; the one-time invite yields to the study action | `screen-pass` phone-landscape, `progress-screen` phone-landscape |
| The spell quest's landscape draw screen hid the brushes and clipped the circle | A short two-page desk keeps the story, clues, brushes, circle, Cast, and Go back visible; the gate and grimoire spreads fit the short page | `spell-quest` phone-landscape, `spell-flow` phone-landscape, `spell-long` phone-landscape |
| The spell-quest guide hid the learner's tracing ink | The guide is the scene's chapter colour at 65%, sits under the ink canvas, and the grimoire gift/result strokes drop the dark halo | `spell-quest` screenshot `spell-guide` |
| Rotation lost the review, report draft, Settings section, or Practice | The activity handles `fontWeightAdjustment`; key screen state is hoisted above the phone/wide shell; the wide review keeps Show answer and grades sticky | `rotation-state`, `rotation-onboarding` |
| Landscape review clipped Show answer and the grade bar | The wide branch uses the sticky action bar for Trace/Recall and the sticky grade bar after reveal | `rotation-state` landscape screenshot |
| Fold outer landscape collapsed the writing canvas | The review uses the two-pane layout at 760 dp, so the canvas has its own column; the Home hero is compact under 560 dp so Start studying stays above the bar | `study-tools` fold-outer-landscape, `screen-pass` fold-outer-landscape |
| Long visual sweeps restored stale grid filters | Screen-pass starts from a clean app data, opens the tile before the draw-search detour, and scrolls the wide Home by the right margin and gutter | `screen-pass` all eight profiles |
| Sync was only covered at the certificate/code level | Real local TLS client/server tests cover the correct and wrong pin, 403, 500, settings-only, progress-only, both, malformed, and empty archives | `SyncClientTest`, `SyncProtocolTest` |
| The outfit picker was a cramped radio list | A warm-paper preview-and-confirm sheet shows the real widget pet for each outfit, keeps a clear selected state, and saves only on Use this outfit | `settings-outfit` |
| Splash icon jumped from the system splash to the app splash; the ink notice covered it | Same launcher art at 288 dp and dead centre, light splash in dark theme, notice hidden until the welcome ends | `ReviewWidgetPreviewTest`/manual splash frames; `screen-pass` |
| `device.sh ui` dropped all `content-desc`-only nodes | Parse each node once | smoke test |
