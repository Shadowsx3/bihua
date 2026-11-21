# Bihua — agent guide

## What this project is

Bihua is an offline-first Android app for learning to write Simplified Chinese,
Traditional Chinese, Japanese, and Korean Hanja. Characters are the study items;
readings and meanings are cues. Writing is always practised. Pronunciation is an
optional, separate review task. FSRS-7 schedules each script, with optional
sharing between identical glyphs. No streaks and no cloud account.

## Build, test, install

```bash
source scripts/env.sh          # JDK 17 is required
make check                     # compile, 129 unit tests, debug APK, verify
make quick                     # compile only
make test                      # unit tests
make run                       # check, install, launch on the selected device
make release                   # minified release APK
```

Use the device tools instead of ad-hoc adb:

```bash
scripts/device.sh status
scripts/device.sh ui                          # visible labels + coordinates
scripts/device.sh find "Start studying"
scripts/device.sh tap "Start studying"
scripts/device.sh type "ai4"
scripts/device.sh draw 300 500 500 700 700 900 # motionevent pen path
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

## Devices and screenshots — the traps

1. **Screenshots are scaled.** Never derive tap coordinates from the image.
   Use `scripts/device.sh ui/find`; it re-dumps the current UI.
2. **Tap by label, not by remembered coordinates.** Tabs collapse their labels
   when inactive. `device.sh tap` matches `text` or `content-desc`, waits up to
   10 seconds, and re-dumps, because layouts settle late.
3. **Canvas drags need `input motionevent`.** `input swipe` often does not draw.
   `device.sh draw x1 y1 x2 y2 ...` sends DOWN/MOVE/UP.
4. **The tablet uses stylus input by default.** Finger drawing is a setting.
5. **System animation scale 0 kills Compose animations.** Essential playback
   uses `withFrameNanos` (`ink/StrokeGuide.kt`, `ui/CompareScreen.kt`). Do not
   build essential behaviour on `Animatable`.
6. **The keyboard moves the layout.** Dismiss it before tapping Check/Grade, or
   re-dump with `device.sh ui` first.
7. Do not run assistant inference on the phone. It is opt-in and a
   tablet-class feature.

## Transfer practice

- Word practice and paper check are free practice: never create cards, XP, or
  review-log rows.
- Paper check must hide the character and show only reading + meaning before
  reveal.
- Resolve word characters in the current script and refuse mixed availability.

## Word practice

- Word practice is free transfer practice, not FSRS. Never create cards or XP.
- Resolve every word character in the current script before opening; refuse
  words that are not fully available.
- Keep the word hidden until Show answer; cue from reading + meaning.

## Lesson and review rules

- A new card goes Introduce → Trace → Recall. Trace needs at least one stroke.
- A reviewed card starts directly at Recall. Do not show Introduce or Trace
  first; that leaks the answer.
- One review asks for one skill:
  - `WRITE`: draw from a cue.
  - `PRONOUNCE`: show the character, type the reading.
- New cards always write. Reviewed cards alternate writing and pronunciation
  from a random phase (`ReviewTaskPlan`). Pronunciation is opt-in.
- A pronunciation cue must never include the character's reading before the
  check. Writing cues must never include the character.
- Only the first grade is scheduled. Later repeats are practice.
- Due means `dueAt <= now`. Cards in learning steps are not due. Home and the
  session queue must use the same rule (`ProgressStore.dueCount` /
  `ProgressStore.session`).
- Failed cards return later in the session. A failed stroke offers
  `Fix stroke N`, which replays and validates one isolated stroke before the
  rewrite.

## Publishing

- Private source: `Shadowsx3/bihua`; public site: `Shadowsx3/bihua-site`.
- The upload keystore lives at `~/Documents/bihua-keystore/bihua-upload.jks`
  and `keystore.properties` points Gradle at it. Never commit either file.
- GitHub Pages requires a public repo on the Free plan; keep source out of
  `bihua-site`.
- Release APKs go to the public site release, not the private source repo.

## Theme

- Paper, ink, grid, graphite, and guide colours come from
  `LocalBihuaPalette`; never hardcode them in a screen.
- Use `AccentInk` for dark text/icons on a pastel accent in both themes.
- `BihuaTheme(themeMode)` owns the palette; System follows the device.
- Check new screens at 1.3× font scale and in both themes.

## Ink and guides

- Stroke order rules are a reading/reference dialog in Settings → Writing; the
  actual character guide is authoritative.


- Canvas is square; guide coordinates scale x/y independently.
- Completed strokes are immutable. `InkCanvas` caches smoothed geometry and
  segment widths by canvas size, density, brush, scale, and tool. Only the live
  stroke is rebuilt per frame. Preserve taper, pencil dots, and playback
  fractions when changing this.
- Guide scaffolds: full strokes for new cards, outline for early reviews,
  hidden for mature cards. Initialise visibility synchronously; never let a
  review flash the guide for one frame.
- `StrokeOrderGrader` stroke numbers are 1-based, including the empty-ink path.
- Brush presets: Ink, Pen, Pencil, Marker. S Pen pressure is averaged and
  eased; speed thins; ends taper. Finger input has no pressure.

## Scheduling and data

- Progress DB schema 9. `review_log` stores `elapsed_ms`, `task`, and `strokes`.
  - `WRITE` / `PRONOUNCE` task stats and per-character history depend on it.
  - Bump the version and add an upgrade path when the schema changes.
- Content DB contentVersion 15. Bump it whenever the asset changes.
  - `char_meta` holds radical, decomposition, etymology, type, phonetic, and
    semantic. The family card prefers the phonetic part, then semantic, then
    radical. Loaded only by the detail pane.
  - Keep the scripts pure: no simplified-only forms in Traditional, no
    traditional-only forms in Simplified; shared glyphs stay in both.
  - Traditional words come from CEDICT word pairs, not character substitution.
  - Word examples are unique per mode; glosses drop classifier/variant noise.
  - Rebuild with `scripts/device.sh content --bump`.
- Backups must include progress. The zip format is `backup.json` +
  `progress.db`; import accepts the zip and legacy settings-only JSON.
- Reset all learning deletes cards, reviews, XP, and confusions; it keeps notes,
  study list, and settings.
- Vacation mode is `vacationStartedAtMs > 0`. While active, due count is 0 and
  sessions refuse to start. Ending it shifts `due_at` and `last_review_at` by
  the elapsed pause; never shift review-log timestamps.
- Ending vacation sets `easeBackUntilMs` for seven days; sessions are capped at
  10 cards during that window.

## Search

- Use `CharacterSearch.matches` for character search: form, meaning, numbered
  or marked pinyin, Japanese romaji (kana-normalised), Korean romanised eum.
- Word search is on demand; do not query the word table on every keystroke.
- Speaker buttons follow `readAloud`; hide them when it is off.

## Assistant

- `assistantEnabled` is false by default. When off, remove the tab, the
  character card, and the model manager. Do not leave a dead tab.
- Enabling is in Settings → Assistant. Models are downloaded, never bundled.

## UI copy and layout

- English UI. No streaks or guilt copy. Specific, plain text.
- One primary action per card. Cards are near-white on the forest background.
- Phone: compact header. Characters uses one title/count row, one search +
  filter row, and one horizontally scrolling status row; keep the grid visible.
- Pronunciation reviews cap at 560 dp and scroll on phones.
- Use `formatDuration` and `formatCompactCount` from `ui/Formatting.kt`.

## Testing expectations

- Pure logic belongs in a unit test. Current suites include FSRS, queue,
  session progression, pinyin, known lists, stroke grading, order grading,
  guide scaffold, reading checks, task planning, backup archive, search, and
  formatting.
- `make check` must be green before commit. The pre-commit hook runs compile.
- Verify UI changes on the phone with `device.sh shot`; use a seeded review or
  learning card when a flow needs progress.

## Current gaps

- Tablet/S Pen pass on the current build and real frame timing.
- Upload keystore/privacy policy only if publishing.
- Accessibility and dark mode.
- Automated UI tests.
- Full-backup file-picker test on a device.
- Content spot-checks.
- Assistant QA on a capable tablet.

## Files worth knowing

| Area | Files |
|---|---|
| Shell, tabs, sessions, TTS | `ui/BihuaApp.kt` |
| Home / Characters / Progress | `ui/HomeScreen.kt`, `ui/CharactersScreen.kt`, `ui/DashboardScreen.kt` |
| Lesson, tasks, cues, grading | `learn/LearnScreen.kt`, `learn/SessionProgression.kt`, `learn/ReviewTask.kt`, `learn/ReadingCheck.kt` |
| Ink, guides, grading | `ink/InkCanvas.kt`, `ink/InkModel.kt`, `ink/StrokeGuide.kt`, `ink/StrokePlayback.kt`, `ink/StrokeOrderGrader.kt` |
| Scheduling and progress | `fsrs/Fsrs7.kt`, `progress/ProgressStore.kt`, `progress/ReviewQueue.kt` |
| Content and search | `content/ContentRepository.kt`, `content/CharacterSearch.kt`, `content/Romanization.kt`, `content/Pinyin.kt` |
| Backup and sync | `backup/BackupArchive.kt`, `sync/Sync.kt` |
| Settings | `settings/SettingsScreen.kt`, `settings/Models.kt` |
| Tooling | `scripts/device.sh`, `scripts/check.sh`, `Makefile` |
