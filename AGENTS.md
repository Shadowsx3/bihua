# Bihua — agent guide

This file is the shared context for a new chat. Read it before changing code.

## What this project is

Bihua is an offline-first Android app for learning to write Simplified Chinese,
Traditional Chinese, Japanese, and Korean. Characters are the study items;
readings and meanings are cues. Writing is the default recall task, with an
optional Anki-style reveal. Pronunciation is an optional, separate review task. FSRS-7 schedules each script, with optional
sharing between identical glyphs. No streaks, no account, no ads, no
developer-run analytics.

The Korean course contains Hangul and Hanja. Do not describe it as Hanja only.

## Product feel

Keep this in mind for every screen:

- Warm paper and forest ink, with vivid accents. Lively, not muted or generic.
- Juicy but comfy: soft rounded shapes, generous breathing room, clear hierarchy,
  gentle motion, and satisfying feedback.
- The learner should feel ownership: their pace, their course, their finish
  date, their words, their progress.
- This is a small personal project made with care in Uruguay. Aim for warm,
  specific copy and quiet delight, not a faceless product or a generated demo.
- Keep the writing surface calm and paper like. No decorative color or shapes
  under the ink. The guide lines stay clear in light and dark themes.
- One clear next action per screen. Put explanations in dialogs, not paragraphs.
- No AI sounding copy, no em dashes in user-facing strings, no cringe slogans.
- Prefer short sentences and real verbs. Error copy says what happened and what
  to try next. It does not blame the learner.

## Home and review feel

- Home should feel like a lived-in study desk, not a dashboard for managers.
  Keep the mode-coloured hero or Continue card, the Word of the day, the three
  colourful stat cards, and the practice calendar above the fold. Study tools,
  level, recent characters, and browse may sit below or inside More.
- The Word of the day is a real word from the current script, filtered by the
  mature setting. It has audio and a Write action. Never show a made-up word.
- Grade interval labels are optional and off by default. When enabled, use the
  real FSRS interval for the current card and only the buttons the learner can
  actually use.
- Leave-session flow: back or X opens Resume later / End session / Keep
  studying. Resume later keeps the queue; End session clears it deliberately.
- The Compare writing drill is one character at a time with a large canvas,
  switchable character chips, a guide toggle, and Next/Done. It appears only
  when at least two characters are selected.
- Character detail for a known character should show one real word with audio
  and Write the word before the full word list.
- Settings stays compact. Explanations belong in dialogs. Mature words are off
  by default and require a confirmation when enabled.

## Scheduling and research models

- FSRS-7 and SBD are the advertised schedulers. Both keep a state on every card
  and must stay current; a target-recall change affects future intervals only.
- Target recall is a 70–97% setting with 90% as the recommendation. Do not
  hard-code 0.90 in review, interval-preview, or load-estimate paths.
- FSRS-7 and SBD are the only schedulers. RWKV, ONNX, Piper, and neural voice
  support were removed before 1.0.8. Do not add a research model back as a side
  effect of another feature; a return needs a full port, a reviewed package
  plan, and landing/store wording.
- Credits for FSRS and SBD research belong in the README and Settings → About.
  Keep the licensing wording precise.

## Study lists

- A study list is a collection, not a filter. Studying a list limits the session
  to its characters. Grades still count in the schedule; the rest of the daily
  queue is unchanged. Word practice stays free and never creates cards.
- Lists hold characters and words together. Words stay as text, resolve their
  meaning from the dictionary, and fall back to writing-only practice.
- Imports stream line by line (TXT/CSV/TSV) or through the streaming JSON string
  scanner. Do not read a whole file into memory or cap the number of items.
  Strip a UTF-8 byte-order mark before the first line. Only the root JSON object
  may name the list; nested item `name` fields must not rename it.
- Keep copy plain and professional. Name lists for what they hold, not for a
  mood.

## Feedback and readings

- Feedback colours are semantic, not decorative. Mint means clean, peach means
  turn the stroke around, sky means order or a different reading, and butter
  means a stroke is waiting. Do not use red for a missing stroke or a
  recognition difference; red reads as failure.
- Free practice shows the same handwriting check, coach card, repair modes, and
  shape evidence as a review. The shared component is
  `ui/components/StrokeCoachFeedback.kt`; do not fork it.
- Pinyin display defaults to tone marks (`nǐhǎo`). Numbers (`ni3hao3`) are an
  opt-in Settings choice. Pronunciation input accepts numbered pinyin, tone
  marks, or plain letters regardless of the display setting.

## Reminders and foldable

- Study reminders are off by default. Settings → Reminders has a switch, an hour,
  and day chips. Turning them on requests `POST_NOTIFICATIONS` on Android 13+.
  The app never asks before the learner opts in.
- `ReminderWorker` is a one-shot WorkManager job at the next selected hour and
  day. It schedules the next one after it runs and checks for due reviews or an
  allowed new lesson. If there is nothing to study, it stays quiet.
- The worker reads settings and progress locally and sends no data. The
  notification contains only counts, never a character, word, or meaning.
- Fold state comes from `androidx.window` in `ui/FoldState.kt`. A flat vertical
  fold is BOOK; a half-open horizontal fold is TABLETOP.
- On BOOK, the rail is replaced by a centered dock. Home is a two-page spread
  (action and word on the left, stats and calendar on the right). Learn and
  Practice use the wide canvas/controls layouts, Characters uses grid/detail,
  and filters wrap. The outer folded screen keeps the phone layout.
- Do not let the Home hero span the fold. Do not stretch a phone layout across
  the inner display.
- Test foldables with `adb emu unfold` and `adb emu fold`. `scripts/device.sh
  shot` captures to a device file and pulls it, which avoids the multi-display
  warning on foldables.

## Home-screen widgets

- Widgets are **RemoteViews**, not Compose. Only a limited view set is allowed:
  `LinearLayout`, `TextView`, `ImageView`, and a few others. A plain `View` is
  rejected by the launcher. Use an empty `TextView` for a coloured accent.
- Two providers live in `widget/`: `TodayWidgetProvider` and
  `WordWidgetProvider`. They read a `WidgetSnapshot` on `Dispatchers.IO` through
  `goAsync()` and update the launcher. Do not add a background service.
- The Today copy changes by day part (morning, afternoon, evening, night) and by
  due count, new lessons, vacation, and all-clear. At night it never pushes; it
  says the reviews will keep until morning.
- The Word widget uses the same `featuredWord` as Home. The daily word is always
  non-mature, 2–6 characters, and filtered away from variants, surnames, place
  names, archaic/rare/dialect words, and abbreviations. Curated words lead each
  cycle, then a deterministic hash of the common pool. Never let mature content
  leak into the daily word, even when the dictionary mature setting is on.
- `WidgetUpdater.updateAll` refreshes on app resume and on stop; the widgets
  also ask the launcher every 30 minutes. Keep the update cheap.
- Widget counts must use the same hidden-character filter as Home: skipped and
  suspended cards never appear in the widget's due or new counts.
- The default profile avatar and the small top icon use the launcher pet:
  `InkMascot` with the brush, plus the sparkle on the profile avatar. Do not fork
  a second pet.
- The cold-start welcome holds for at least 1,050 ms (520 ms with reduce-motion)
  so a fast device does not flash it. A tap still skips.

## Content QA backlog

Every character, word, reading, meaning, and story users see must be checked
before the next content release. The working checklist is
`docs/content-qa-backlog.md`. The screen and journey risk list is
`docs/user-journey-risk-audit.md`. Known gaps: Korean coverage for 115 rare
Hanja, 29 rows without a decomposition, a full TalkBack/large-text pass, and
some etymology quality.
- Dependency policy: `docs/dependency-updates.md`.
- Practical agent playbook and traps: `docs/agent-playbook.md`.
- Local journey harness: `qa/README.md`; debug-only setup bridge is `app/src/debug`.
- Learning-science proposals and the joy roadmap: `docs/learning-proposals.md`.
- Current release plan: `docs/1.1.0-roadmap.md`.
- Screen, flow, and feature inventories: `docs/screen-inventory.md`,
  `docs/flow-inventory.md`, `docs/feature-inventory.md`.
- Test coverage: `docs/test-matrix.md`; risk audit: `docs/user-journey-risk-audit.md`.

## Answer privacy

- A writing recall must never show the target character, a word containing it,
  or a stroke guide unless the learner explicitly asks for help. The journey
  harness checks this.
- A pronunciation review must never show the expected reading, including inside
  a meaning string. `learn/ReadingPrivacy.kt` masks kana and Hangul self-describing
  meanings ("hiragana for a", "syllable ga"). Keep this covered by tests.
- Recognition results and grade buttons appear only after the learner commits to
  an answer and asks to check.

## Anki-style answering

- Settings → Practice has two independent switches: **Writing: tap to reveal**
  and **Pronunciation: tap to reveal**. Both default off.
- Writing reveal-only: the cue appears first, then **Show answer** shows the
  character, reading, meaning, and grades. There is no canvas. New cards go
  Meet → Reveal, skipping Trace.
- Pronunciation reveal-only: the character appears, then **Show answer** shows
  the reading and grades. There is no text field. Hide the "You typed" chip and
  do not suggest a grade.
- The answer reveal is still a real review. Grades schedule normally, XP counts,
  and the session order is unchanged. Do not mark these as practice-only.
- Keep the switch between writing and pronunciation tasks independent. A learner
  may want to type readings but not draw, or draw but not type.
- `ReviewTask.revealsAnswer` is the single mapping; do not duplicate it in the UI.

## Meaning checks and synonyms

- Meaning is an optional review task, off by default. Writing stays the default
  and keeps at least half of the reviews when meaning and pronunciation are on.
- New cards never start with meaning. Meet introduces the meaning first; reviews
  can ask for it.
- The meaning task is self-graded. Do not auto-grade English synonyms. The
  learner types a wording if they want, taps **Show answer**, compares it with
  the clean gloss and a real example word, then grades.
- Sanitize before teaching: `settings/MeaningSanitizer.kt` rejects blank,
  placeholder, overlong, and structural glosses such as "variant of", "surname",
  "see ...", "used in ...", "radical", or "component". Example words with those
  glosses are skipped. Phonetic self-descriptions such as "hiragana for a" or
  "syllable ga" are not meaning checks; those cards fall back to writing and
  pronunciation. A character with no teachable gloss also falls back to writing.
- Synonyms are user wordings keyed by character id in
  `AppSettings.meaningSynonyms`. They can be added or removed in the meaning
  reveal and in character detail; keep the two editors consistent. They are trimmed, deduplicated ignoring case,
  capped at 12 per character and 60 characters each, cleaned on restore, and
  removed when the learner taps the chip's x. They travel in settings backups.
- The reveal shows the official gloss, the learner's words if typed, accepted
  synonyms, and one or two non-mature example words. Highlight the target
  character inside the word. Keep the tone warm: "Your words", "Also accepted",
  "In a word", "How did that feel?".
- Record the task as `MEANING` in the review log. Show it in task stats and
  character history. Do not count it as writing pace or strokes.

## Navigation and loading

- `qa/scenarios.py:navigation-deep` is the back-stack regression: every Settings
  section, Compare, Paper, Practice picker, study lists, and the study tools
  sheet. Run it on phone and tablet after changing headers or overlays.
- System back must close overlays one layer at a time: Settings section, then
  Settings; paper/Compare, then the character; word queue, then the character or
  Home. Never leave a screen that exits the app while an overlay is open.
- Loading must end in success, an empty state, or a readable error. Handwriting
  model downloads and recognition have timeouts. Content loading has Retry.
  The first read-aloud tap is queued while the voice engine starts.
- Large text is a layout target, not an afterthought. Check 1.5x on Home Study
  tools, Compare, Paper, Settings, and session. Titles should wrap, not become a
  vertical strip; action rows may move below the title.

## Dependency updates

See `docs/dependency-updates.md`. GitHub Actions and Gradle minor/patch updates
arrive as grouped PRs. Gradle, AGP, Kotlin, and Compose toolchain majors are
manual migrations and must not be merged one at a time. Keep app dependency PRs
separate from behavior changes.

## Build, test, install

```bash
source scripts/env.sh          # JDK 17
make check                     # compile, 310 tests, content audit, debug APK, verify
make quick                     # compile only
make test                      # unit tests
make lint                      # Android lint
make run                       # check, install, launch on selected device
make journey                   # quick phone journeys
make journey-standard          # phone, fold, tablet core journeys
make journey-full              # every profile and daily state
make release                   # signed minified release APK
make smoke                     # on-device launch/navigation smoke test
```

GitHub Actions runs **unit tests, content audit, and lint only**. It does not
assemble or upload an APK. The debug APK is a local build artifact.

The Makefile sets `SHELL := /bin/bash` because `scripts/env.sh` uses
`BASH_SOURCE`. Do not remove it: Ubuntu CI runs recipes with `/bin/sh`, and
`source` plus `BASH_SOURCE` fail there. Use `. scripts/env.sh` inside recipes.

Use the device tools instead of ad-hoc adb:

```bash
scripts/device.sh status
scripts/device.sh ui                          # visible labels + coordinates
scripts/device.sh find "Start studying"
scripts/device.sh tap "Start studying"
scripts/device.sh type "ai4"
scripts/device.sh draw 300 500 500 700 700 900 # DOWN/MOVE/UP pen path
scripts/device.sh swipe 540 1900 540 700
scripts/device.sh shot before
scripts/device.sh a11y                         # clickable nodes with no spoken label
scripts/device.sh keyboard on                  # force the soft keyboard on
scripts/device.sh session-clear                # drop a saved lesson session
scripts/device.sh db-cards
scripts/device.sh db-reviews 20
scripts/device.sh seed-review 爱 SIMPLIFIED 2 0 # due now, reps=2
scripts/device.sh seed-learning 一 KOREAN 1 10 # due in 10 minutes
scripts/device.sh content [--bump]
```

With more than one device connected, set `BIHUA_DEVICE=<serial>` or pass
`-s <serial>`. DB commands need a debug build. The progress DB is
`databases/bihua_progress.db`.

## Device and screenshot traps

1. **Never derive tap coordinates from a screenshot.** Use `device.sh ui/find`
   or tap by label. The screenshot is scaled.
2. **Tap by label.** The bottom navigation now has fixed positions and labels for
   every tab, but dialogs and keyboards still move content.
3. **Prefer `device.sh draw`.** `input swipe` works for touch on most devices but
   can be unreliable for stylus input. `device.sh draw` sends motionevents.
4. **Emulators and desktop input.** `hasStylus` treats emulator/goldfish/ranchu as
   touch, and `InkCanvas` accepts mouse pointers. Finger drawing is on by default
   everywhere, including stylus phones; onboarding and Settings offer a
   stylus-only choice. A pressed stylus wins over a palm or finger.
5. **System animation scale 0 kills Compose animations.** Essential playback uses
   `withFrameNanos` (`ink/StrokeGuide.kt`, `ui/CompareScreen.kt`). Do not build
   essential behaviour on `Animatable`.
6. **The keyboard moves the layout.** Dismiss it before tapping Check/Grade, or
   re-dump with `device.sh ui`.
7. **Tablets use a navigation rail; phones use a labelled bottom bar.** Test
   both. The wide threshold for Characters is 780 dp; Practice uses 900 dp.
8. **Tablet emulator:** `bihua_tablet`, Pixel Tablet, 2560 × 1600, 320 dpi.
   Phone emulator: `voxbloom`.

## Lesson and review flow

- A lesson is **Meet → Trace → Write** by default.
- With Anki writing on, a lesson is **Meet → Reveal**; Trace is skipped.
- Trace needs at least one stroke when it is present.
- A reviewed card starts directly at Recall. Do not show Meet or Trace first.
- Write cue is reading, meaning, or a word. The character is never shown before
  the learner reveals it.
- After writing, tap **Check & grade**. A fixed grade bar shows the answer,
  reading, meaning, four grades, the app suggestion, and Draw again.
- Pronunciation shows the character, asks for the reading, then shows a large
  feedback card (`Correct!`, `Almost — the tone`, `Not quite`, or `Answer`).
- One review asks for one skill:
  - `WRITE`: draw from a cue.
  - `PRONOUNCE`: show the character, type the reading.
  - `MEANING`: show the character, type a wording if desired, reveal the gloss.
- New cards write, unless Anki writing is on. Reviewed cards alternate writing
  and pronunciation from a deterministic phase; pronunciation is opt-in.
- With Anki pronunciation on, a pronunciation card shows the character, then
  Show answer, then grades. No text field. Do not suggest Again just because
  the answer was revealed.
- A pronunciation cue must never include the reading before the check. Writing
  cues must never include the character.
- Only the first grade is scheduled. Later repeats are practice.
- A failed card returns later in the session. A failed stroke may offer
  `Fix stroke N`, which replays and validates one isolated stroke.
- The handwriting check is a hint. It can miss or misread a stroke. The learner
  always chooses the grade.

## Session order and progress

- Settings → Study has **Session order**: Reviews first, Mixed, Lessons first.
- Mixed uses `mixInterval` (1–10 reviews before one new character).
- **Progress bar** can be Full session or Next lesson. Next lesson fills the
  current block and resets at the next lesson; it shows “Next lesson in N cards.”
- The top bar is animated and semantic; it fills through lesson steps and graded
  reviews.
- The pace estimate uses a realistic Good/Hard/Again mix, a completed course, and
  a 120-day window after completion. At 10 new/day it shows roughly 88 average
  and 113 busy-day reviews, and about 11 months to finish. Do not revert this to
  the old “settles near 30” copy.
- A saved lesson is per script. Discard asks for confirmation. Reset, import, and
  sync clear paused lessons.

## Transfer practice

- Word practice and paper check are free practice: never create cards, XP, or
  review-log rows.
- Paper check hides the character and shows only reading + meaning before reveal.
- Word practice resolves every character in the current script and refuses mixed
  availability.
- Word practice: cue from reading + meaning, keep the word hidden until Show
  answer, one character at a time.
- Character detail has top actions for Write a word and Write on paper before
  the sharing controls.

## Ink and guides

- Stroke order rules are a reference dialog in Settings → Writing; the actual
  character guide is authoritative.
- Canvas is square; guide coordinates scale x/y independently.
- Completed strokes are immutable. `InkCanvas` caches smoothed geometry and
  segment widths by canvas size, density, brush, scale, and tool. Only the live
  stroke is rebuilt per frame. Preserve taper, pencil dots, and playback
  fractions.
- Guide scaffolds: full strokes for new cards, outline for early reviews,
  hidden for mature cards. Initialise visibility synchronously; never flash the
  guide for one frame.
- `StrokeOrderGrader` stroke numbers are 1-based, including the empty-ink path.
- Brush presets: Ink, Pen, Pencil, Marker. Stylus pressure is averaged and eased;
  speed thins; ends taper. Finger input has no pressure.
- The Practice screen fills the screen: title, character summary, weighted square
  canvas, compact Guide/Order/undo/eraser row, and Check. Result opens in a
  compact scrollable dialog.
- The Practice character picker defaults to the first curriculum character and
  has search by character, meaning, or reading.

## Settings

- Settings opens on a searchable home list of grouped rows:
  YOU / LEARNING / WRITING / YOUR DATA / ABOUT. On a phone, Data and Sync are
  below the first screen; scroll or use search.
- Rows open focused subpages with a back arrow. Android Back returns to the
  settings home while a subpage is open.
- All sections are reachable: Profile, Practice, Study, Writing, Display, Data,
  Sync, About.
- Do not reintroduce the old horizontally scrolling category chips or the
  three-row chip grid.
- Writing has the active-script background model download; Prepare models downloads
  all four.
- About is transparent about Google ML Kit diagnostics and has Send feedback and
  Source code actions.
- Schedule is intentionally compact: profile dropdown, status, Fit, Import,
  Export, and a More menu. Long explanations belong in the info dialog, not on
  the page. Do not bring back the chip wall and paragraph stack.
- Data and Sync use `TransferScope`: Everything, Settings, or Progress. Import
  and sync confirm dialogs use checkboxes for the parts present in the file.
- Slow actions must show feedback: `BusyDialog` for blocking work, inline
  progress for the weight fit, and a snackbar after completion. A tap should
  never look ignored.
- The TTS engine is warmed at start/onboarding/script switch; a missing voice
  shows a Toast instead of silently doing nothing.
- Rotation/theme/font-scale must be tested. The app supports 1.3×+ text.

## Scheduling and data

- Progress DB schema 10. `review_log` stores `elapsed_ms`, `task`, and `strokes`; `word_practice` stores `mature`.
- Bump the schema and add an upgrade path when tables change.
- Content DB contentVersion 34. Bump it whenever the asset changes.
- Keep scripts pure: no simplified-only forms in Traditional, no
  traditional-only forms in Simplified; shared glyphs stay in both.
- Traditional words come from CEDICT word pairs, not character substitution.
- Korean uses Unihan/Hangul. Word examples come from the Korean Wiktionary
  extract, ranked by FrequencyWords, with explicit examples tagged mature.
- Backups include settings and progress. Zip format is `backup.json` +
  `progress.db`; import accepts the zip, a settings-only zip, a progress-only
  zip, and legacy settings-only JSON. `TransferScope` decides what is written.
- Settings and profile are part of every sync package. The receiver chooses
  whether to apply settings, progress, or both.
- Do not call `enableEdgeToEdge()` and do not set status-bar or navigation-bar
  colors directly. The 1.0.2 build was flagged for
  `Window.setStatusBarColor`, `Window.setNavigationBarColor`, and
  `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`. `LocalEdgeToEdge` in
  `ui/EdgeToEdge.kt` applies system-bar padding only on Android 15+, where the
  platform draws edge-to-edge by default. Keep the `MainActivity`
  inset-controller icon colors on API 35+; older versions use the theme's inset
  bars. After a release build, check the DEX strings for those three APIs.
- After a review is submitted, the canvas is locked (`InkCanvas(enabled=false)`).
  Do not place a Draw again button over the drawing; the grade bar action is the
  only restart. Playback stays available and editor tools are disabled.
- Practice and review use `InkHistoryPlaybackBar` so history and playback are
  aligned. Practice must keep play, reverse, previous, count, and next.
- `AppSettings.onlyAgainAndGood` hides Hard and Easy in the grade bar and
  pronunciation check. Scheduling still accepts all four grades.
- FSRS weights live in `AppSettings`. `fsrsWeights` is the shared profile;
  `fsrsProfiles` overrides it per script. `parametersFor(mode, glyphGroupId)`
  uses the shared profile whenever the glyph is shared, so a shared card never
  changes models between scripts.
- The local fit (`FsrsOptimizer`) needs 400 reviews. It uses the recent 2,000
  by default or all reviews when chosen, checks a time-based holdout, and keeps
  the current weights if the holdout does not improve. It is not the official
  Rust optimizer; do not claim parity.
- `DayBoundary` is the single source for the study date. `dayStartHour` defaults
  to 4 AM and ranges 0–6. New counts, activity, forecasts, study days, XP days,
  skips, and the calendar all use it. `clockTick` and lifecycle resume refresh
  the day when the app stays open.
- `suspendedIds` is permanent. `skippedUntil` is one day. Both are excluded from
  sessions; Study tools shows them separately.
- Reset all learning deletes cards, reviews, XP, confusions, and word-practice
  history; it keeps notes, study list, and settings. Paused sessions are cleared
  by the Settings screen after reset.
- Vacation mode: due count 0 and sessions refuse to start. Ending shifts
  `due_at`/`last_review_at`, never review-log timestamps. Ease-back caps
  sessions at 10 cards for seven days.
- Local sync is opt-in, same Wi-Fi, a 10-character pairing code, and a short-lived
  TLS server with a per-session certificate pinned by that code. Do not add
  cleartext back.
- Android cloud backup is disabled.

## Testing expectations

- Pure logic belongs in a unit test. Current suites include FSRS, queue, session
  progression, pinyin, known lists, stroke grading, order grading, guide
  scaffold, reading checks, task planning, backup archive, search, and formatting.
- `make check` must be green before commit. The pre-commit hook runs compile.
- Verify UI changes with `device.sh shot`; use seeded reviews/learning cards when
  a flow needs progress.
- `tools/seed_fsrs_fit.py <progress.db> [cards-per-mode] [reviews-per-card]`
  writes a large per-script and shared-glyph history. Pull the debug DB, run the
  script, push it back, then fit a profile from Settings → Schedule.
- Test phone and tablet, light and dark, 1.3× text, emulator/mouse, and a real
  stylus device when available.

## Files worth knowing

| Area | Files |
|---|---|
| Shell, tabs, sessions, TTS, bottom bar | `ui/BihuaApp.kt` |
| Home / Characters / Progress | `ui/HomeScreen.kt`, `ui/CharactersScreen.kt`, `ui/DashboardScreen.kt` |
| Lesson, tasks, cues, grading | `learn/LearnScreen.kt`, `learn/SessionProgression.kt`, `learn/ReviewTask.kt`, `learn/ReadingCheck.kt`, `learn/Grading.kt` |
| Ink, guides, grading | `ink/InkCanvas.kt`, `ink/InkModel.kt`, `ink/StrokeGuide.kt`, `ink/StrokePlayback.kt`, `ink/StrokeOrderGrader.kt`, `ink/StrokeGrade.kt`, `ink/StrokeCoach.kt`, `ink/Stylus.kt` |
| Scheduling and progress | `fsrs/Fsrs7.kt`, `fsrs/FsrsWeights.kt`, `fsrs/FsrsOptimizer.kt`, `fsrs/ReviewLoad.kt`, `progress/ProgressStore.kt`, `progress/ReviewQueue.kt`, `settings/DayBoundary.kt` |
| Content and search | `content/ContentRepository.kt`, `content/CharacterSearch.kt`, `content/Romanization.kt`, `content/Pinyin.kt` |
| Backup and sync | `backup/BackupArchive.kt`, `backup/TransferScope.kt`, `sync/Sync.kt` |
| Settings | `settings/SettingsScreen.kt`, `settings/Models.kt`, `settings/DayBoundary.kt` |
| Dictionary and print | `ui/DictionaryScreen.kt`, `settings/WorksheetScreen.kt`, `worksheet/WorksheetPdf.kt` |
| Mascot and opening | `ui/components/Components.kt` (`InkMascot`), `ui/components/OpeningInk.kt`, `ui/theme/Color.kt` |
| Tooling | `scripts/device.sh`, `scripts/check.sh`, `scripts/verify-apk.sh`, `scripts/journey.sh`, `Makefile`, `tools/seed_fsrs_fit.py` |
| Local journeys | `qa/journey.py`, `qa/state.py`, `qa/driver.py`, `qa/scenarios.py` |

## Release assets

Landing-page screenshots, the OG image, the Play feature graphic, and store
screenshots are refreshed **after** the UI and versionCode are frozen. Keep the
source captures and store files local in `store/`; the repo stays app-focused.
See `docs/learning-proposals.md` for the final-asset checklist.

## Known gaps

- The physical stylus pass was done on the SM-X520; recheck after any ink change.
- Full backup/import through the system file picker should be retested on a
  release build before production.
- Automated UI/instrumentation tests are not present.
- Foldable, desktop, and XR screenshots are intentionally not supplied.
