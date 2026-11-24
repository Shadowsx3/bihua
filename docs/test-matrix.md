# Flow and test matrix

Scope: 1.1.0 (1.0.9 in Play review), phone, foldable, tablet, light and dark,
and 1.5x text. The automated suite is JVM/Robolectric plus the local journey
harness; pure composables and physical hardware still need the device pass.

## 1.1.0 additions

| Flow | What is covered | Where |
|---|---|---|
| Anki-style writing | Cue → Show answer → grade with no canvas; new cards go Meet → Reveal and skip Trace | `ReviewTaskTest`, `anki` journey |
| Anki-style pronunciation | Character → Show answer → grade with no typing field or unfair grade suggestion | `ReviewTaskTest`, `anki` journey |
| Meaning review | Type a wording → reveal clean gloss and example word → add synonym → grade | `MeaningSanitizerTest`, `meaning` journey |
| Meaning Anki | Reveal-only meaning, no typing field, synonyms still available | `meaning-anki` journey |
| Meaning data | Blank, placeholder, structural, overlong, duplicate synonym, cap; shipped glosses stay 98% teachable | `MeaningSanitizerTest`, `ContentDatabaseTest` |
| Synonym management | Add and remove from character detail; card and chips render at 1.5x; detail bottom reachable | `synonyms` journey |
| Task mix | Writing keeps half when meaning or pronunciation is enabled; meaning never starts a new card | `ReviewTaskTest` |
| Settings switches | Writing and pronunciation are independent, off by default, and survive sanitize | `SettingsSanitizeTest`, `anki` journey |
| Deep navigation | Every Settings section, Compare, Paper, Practice picker, study lists, and study tools sheet, closed with system back | `navigation-deep` journey |
| Large-text layout | Home study tools, Compare header, and Paper header at 1.5x | manual phone pass |
| Loading states | Model download and recognition timeouts, TTS first tap, content Retry, session prepare | `InkRecognition` timeout test plus manual phone pass |
| Widget parity | Skipped and suspended cards excluded; refresh on stop | `WidgetCopyTest`, manual launcher pass |
| Widget pet costumes | Study cap, word monocle, witch hat in the evening, vampire at night, nightcap on vacation, party hat at all-clear; pinned outfit wins | `WidgetCopyTest`, manual launcher pass |
| Widget layout states | 2×2 Today and 4×2 Word, counts, all-clear, vacation, 1.5× font, night text, backgrounds | manual launcher pass |
| Grade legend | “?” opens Again/Hard/Good/Easy from the session prompt | `grade-help` journey |
| Widget outfit | Pin Witch in Display and verify the row persists | `settings-outfit` journey |
| Progress ranges | 7D, 90D, 1Y, then 30D; Momentum and range labels follow | `progress-ranges` journey |
| Caught-up copy | “All done for today” appears without a duplicate “All caught up” | `caught-up` journey |
| Import resilience | UTF-8 BOM and nested JSON names | `StudyListImportTest` |
| List name uniqueness | Long names keep their numeric suffix | `StudyListsTest` |
| Guided tour | Invite dismiss, replay from Settings, tap and Next through all nine stops, Back to app | `tour` journey |
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
| Home-screen widgets | Today/Word layouts inflate, time copy, action, picker preview | `WidgetCopyTest` plus manual launcher review |
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
| Dark theme and 1.5x text | Yes | Yes | Full TalkBack sweep at 2x still open |
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
| `device.sh ui` dropped all `content-desc`-only nodes | Parse each node once | smoke test |
