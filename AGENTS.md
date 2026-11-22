# Bihua — agent guide

This file is the shared context for a new chat. Read it before changing code.

## What this project is

Bihua is an offline-first Android app for learning to write Simplified Chinese,
Traditional Chinese, Japanese, and Korean. Characters are the study items;
readings and meanings are cues. Writing is always practised. Pronunciation is an
optional, separate review task. FSRS-7 schedules each script, with optional
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
- Keep the writing surface calm and paper like. No decorative color or shapes
  under the ink. The guide lines stay clear in light and dark themes.
- One clear next action per screen. Put explanations in dialogs, not paragraphs.
- No AI sounding copy, no em dashes, no cringe slogans.

## Current release state

- Closed test: **1.0.3**, versionCode **13**.
- Local next release: **1.0.4**, versionCode **14**. This is the design,
  review, and content-quality build.
- Do not upload another build with versionCode 12 or 13.
- The 1.0.3 code includes personal FSRS weights, the day boundary, permanent
  suspension, and selective transfer. 1.0.4 adds the vivid palette, review cue
  setup, script filters, review polish, handwriting sanitisation, cleaner
  readings, and better Japanese word ordering.
- Progress schema **10**; contentVersion **34**.
- 214 unit tests.
- Public repo: <https://github.com/Shadowsx3/bihua>
- Pages: <https://shadowsx3.github.io/bihua/>
- Play package: `com.bassmd.bihua`
- Play name: `Bihua: Character Writing`
- The app is in closed testing with 12 testers. Do not pause or replace the
  closed-test track. Upload updates to the same track.
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Play AAB: `app/build/outputs/bundle/release/app-release.aab`
- Play upload assets and listing copy live in the local-only `store/` folder.
  `store/` is ignored by git and must not be published. It contains the upload
  checklist, listing copy, release notes, and store screenshots.
- Play upload checklist: `store/PLAY-UPLOAD.md`
- Listing copy: `store/listing.md`, `store/full-description.txt`
- Release notes: `store/release-notes-1.0.3.txt` (previous closed test) and
  `store/release-notes-1.0.4.txt` (next release)
- Store screenshots: `store/phone-1..7.png`, `store/tablet-7-*`,
  `store/tablet-10-*`
## Content QA backlog

Every character, word, reading, meaning, and story users see must be checked
before the next content release. The working checklist is
`docs/content-qa-backlog.md`. The screen and journey risk list is
`docs/user-journey-risk-audit.md`. Known gaps: Korean coverage for 115 rare
Hanja, 29 rows without a decomposition, a full TalkBack/large-text pass, and
some etymology quality.
- GitHub release tag: `v1.0.4`; the site uses `releases/latest`.

## Build, test, install

```bash
source scripts/env.sh          # JDK 17
make check                     # compile, 214 tests, content audit, debug APK, verify
make quick                     # compile only
make test                      # unit tests
make lint                      # Android lint
make run                       # check, install, launch on selected device
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
scripts/device.sh db-cards
scripts/device.sh db-reviews 20
scripts/device.sh seed-review 爱 SIMPLIFIED 2  # due now, reps=2
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
   touch, and `InkCanvas` accepts mouse pointers. Real stylus devices default to
   pen-only; finger drawing is a setting.
5. **System animation scale 0 kills Compose animations.** Essential playback uses
   `withFrameNanos` (`ink/StrokeGuide.kt`, `ui/CompareScreen.kt`). Do not build
   essential behaviour on `Animatable`.
6. **The keyboard moves the layout.** Dismiss it before tapping Check/Grade, or
   re-dump with `device.sh ui`.
7. **Tablets use a navigation rail; phones use a labelled bottom bar.** Test
   both. The wide threshold for Characters is 780 dp; Practice uses 900 dp.
8. **Screenshot rendering.** Phone/tablet SVGs are rendered with headless Chrome,
   not `sips` (sips produces a missing-image placeholder). See `store/README.md`.
9. **Tablet emulator:** `bihua_tablet`, Pixel Tablet, 2560 × 1600, 320 dpi.
   Phone emulator: `voxbloom`.

## Lesson and review flow

- A lesson is **Meet → Trace → Write**.
- Trace needs at least one stroke.
- A reviewed card starts directly at Write; do not show Meet or Trace first.
- Write cue is reading, meaning, or a word. The character is never shown.
- After writing, tap **Check & grade**. A fixed grade bar shows the answer,
  reading, meaning, four grades, the app suggestion, and Draw again.
- Pronunciation shows the character, asks for the reading, then shows a large
  feedback card (`Correct!`, `Almost — the tone`, `Not quite`, or `Answer`).
- One review asks for one skill:
  - `WRITE`: draw from a cue.
  - `PRONOUNCE`: show the character, type the reading.
- New cards always write. Reviewed cards alternate writing and pronunciation
  from a deterministic phase; pronunciation is opt-in.
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

- Progress DB schema 9. `review_log` stores `elapsed_ms`, `task`, and `strokes`.
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
- Reset all learning deletes cards, reviews, XP, and confusions; it keeps notes,
  study list, and settings.
- Vacation mode: due count 0 and sessions refuse to start. Ending shifts
  `due_at`/`last_review_at`, never review-log timestamps. Ease-back caps
  sessions at 10 cards for seven days.
- Local sync is opt-in, same Wi-Fi, six-digit pairing code, short-lived HTTP.
- Android cloud backup is disabled.

## Data safety and privacy

- ML Kit Digital Ink runs on-device. Strokes and candidates do not leave the
  device.
- ML Kit collects per-installation identifiers, device/app info, performance and
  usage metrics, configured languages, API config, feature I/O size, event type,
  and error codes; encrypted in transit; Google acts as a service provider and
  does not transfer to third parties.
- Play Data safety: **Device or other IDs + Diagnostics**, collected, not shared,
  not ephemeral, required, purpose Analytics.
- No advertising ID, account, location, camera, contacts, storage, or
  developer-run analytics.
- The privacy policy is linked in the app (Settings → About) and the store listing.
  It must stay consistent with Data safety.
- Do not add the generative assistant or LiteRT-LM without a separate policy,
  data-safety, and privacy review.

## Publishing and store

- Play closed testing currently has 12 testers; the 14-day continuous opt-in
  window must be preserved. Upload updates to the same track.
- AAB is the Play artifact; never commit APKs, AABs, keystores,
  `keystore.properties`, `screenshots/`, or build output.
- Store listing is plain text; Google Play does not render Markdown/HTML.
- Phone screenshots are 1080 × 1920, 9:16. Tablet assets are 1920 × 1080 and
  2560 × 1440. All are real 1.0.4 captures.
- The app UI is English-only. Do not publish AI listing translations without a
  native review. `store/translations/listing-en-US.csv` is the source row.
- The site `docs/` and GitHub release are separate from Play Console. Local files
  do not upload themselves.
- The landing page (`docs/index.html`, `docs/style.css`) uses a crisp ink-and-paper
  style with current 1.0.4 light-theme screenshots. Do not reintroduce the old
  blurred forest wash, drifting leaves, fireflies, glass cards, or translucent
  gradients. Keep hard borders, solid shadows, and the vermillion accent.
- `docs/assets/*-light.png` are the current screenshots. Do not point the site or
  README back at the old dark 1.0.1 images.
- The hero leads with “Learn characters by writing them.” Do not bring back the
  old “the character has to come out of your hand” line or similar slogans. Keep
  copy plain and specific; avoid em dashes, “not just X, but Y”, and other
  marketing patterns.
- The landing has a live ink pad (`#draw`, `docs/site.js`). It counts pointer
  strokes against a guide and keeps nothing. It uses pointer pressure when the
  device reports it, so a stylus changes the line; a finger or mouse still works.
  It does not run ML Kit and must not claim to recognise what the visitor drew.
- The ink-check flow (`#recognizer`) explains the real path: ML Kit candidates,
  the stroke comparison, then the learner's grade. Keep the claim that recognition
  is evidence and never presses the grade button.
- The spacing section (`#spacing`) has an interval ladder and a retention
  sawtooth. Its values come from the shipped default weights: after Good grades
  the intervals are about 5, 26, 92, 293, then the 365-day cap. The load estimate
  at 10 new a day is 88 average, 113 busy, 317 days. Do not invent new numbers.
- FSRS-7 has two stability traces and 34 weights; FSRS-6 has one stability and 21
  weights. FSRS-7 uses fractional days and a mixed curve, and its interval is
  solved numerically. The app still ships the published default weights and does
  not fit a personal model yet.
- GitHub Actions must not assemble/upload APKs. `scripts/verify-apk.sh` is
  portable across macOS/Linux (`wc -c`, not macOS `stat -f%z`).

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
- Check the pre-launch report and no-crash behavior on the exact Play build.

## Files worth knowing

| Area | Files |
|---|---|
| Shell, tabs, sessions, TTS, bottom bar | `ui/BihuaApp.kt` |
| Home / Characters / Progress | `ui/HomeScreen.kt`, `ui/CharactersScreen.kt`, `ui/DashboardScreen.kt` |
| Lesson, tasks, cues, grading | `learn/LearnScreen.kt`, `learn/SessionProgression.kt`, `learn/ReviewTask.kt`, `learn/ReadingCheck.kt`, `learn/Grading.kt` |
| Ink, guides, grading | `ink/InkCanvas.kt`, `ink/InkModel.kt`, `ink/StrokeGuide.kt`, `ink/StrokePlayback.kt`, `ink/StrokeOrderGrader.kt`, `ink/Stylus.kt` |
| Scheduling and progress | `fsrs/Fsrs7.kt`, `fsrs/FsrsWeights.kt`, `fsrs/FsrsOptimizer.kt`, `fsrs/ReviewLoad.kt`, `progress/ProgressStore.kt`, `progress/ReviewQueue.kt`, `settings/DayBoundary.kt` |
| Content and search | `content/ContentRepository.kt`, `content/CharacterSearch.kt`, `content/Romanization.kt`, `content/Pinyin.kt` |
| Backup and sync | `backup/BackupArchive.kt`, `backup/TransferScope.kt`, `sync/Sync.kt` |
| Settings | `settings/SettingsScreen.kt`, `settings/Models.kt`, `settings/DayBoundary.kt` |
| Tooling | `scripts/device.sh`, `scripts/check.sh`, `scripts/verify-apk.sh`, `Makefile`, `tools/seed_fsrs_fit.py` |
| Store | `store/PLAY-UPLOAD.md`, `store/README.md`, `store/listing.md` |

## Known gaps

- Pre-launch report and Android vitals are only available in Play Console; run
  them on the exact versionCode being reviewed.
- The physical stylus pass was done on the SM-X520; recheck after any ink change.
- Full backup/import through the system file picker should be retested on a
  release build before production.
- Automated UI/instrumentation tests are not present.
- Foldable, desktop, and XR screenshots are intentionally not supplied.
