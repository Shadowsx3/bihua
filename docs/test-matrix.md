# Flow and test matrix

Scope: 1.0.7, phone, foldable, and tablet. The automated suite is JVM/Robolectric, so it
covers rules, persistence, and content. Composables and hardware still need the
device pass below.

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
| Study lists | Named character and word lists, migration, presets, quick add, word bookmark and queue | `StudyListsTest`, `StudyListImportTest`, manual phone review |
| List import | TXT, CSV, TSV, JSON streaming; words kept; no item cap | `StudyListImportTest` plus manual file-picker import |
| List export | `.txt` round-trips characters and words | manual file-picker export |
| SBD scheduler | Reference recurrence, pass/fail, interval inversion, per-user fit | `Sbd7Test`, `SbdOptimizerTest`, `ProgressStoreSbdTest` |
| Target recall | 70–97% changes intervals for SBD/FSRS | `Sbd7Test`, `ProgressStoreSbdTest`, manual Settings review |
| RWKV package | Manifest, checksum, tensor shapes, ONNX inference, state BLOB | `RwkvOnnxEngineTest`, manual package import |
| Onboarding method | Three answers, recommendation, model choice, target recall | manual phone/tablet review |
| Word of the Day quality | At least 365 non-mature quality candidates per course; curated + pool | `ContentDatabaseTest` |
| Cold start | Minimum hold, reduce-motion, tap-to-skip | manual launch on phone and tablet |
| Pet identity | Launcher icon, profile avatar, top icon use the same pet | manual light/dark review |
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
| Dark theme and 1.3x text | Yes | Yes | Full TalkBack sweep still open |
| Fold postures | n/a | Partial | Folded and unfolded checked; tabletop is detected but not used |

## Open gaps

1. No Compose instrumentation tests. A flow test for onboarding, grade, undo,
   and restore would catch more than the current unit suite.
2. Tabletop posture is detected but layouts do not use it yet.
3. A full TalkBack sweep on all screens and both fold postures remains.
4. Physical S Pen pressure and palm rejection still need a hardware pass.

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
