# Bihua journey harness

Local, full-app UI journeys for daily learner states. This is not a CI job.

```bash
scripts/journey.sh                 # quick phone check
scripts/journey.sh --standard      # phone, fold, tablet core
scripts/journey.sh --full          # every profile and daily state
scripts/journey.sh --spell         # the spell quest suite: phone, tablet, fold
scripts/journey.sh --list
```

## Useful runs

```bash
# One scenario on one profile
scripts/journey.sh --scenario daily80 --profile phone

# Exact review count, all profiles in the preset
scripts/journey.sh --full --reviews 100

# Drain every review, not just a sample
scripts/journey.sh --scenario daily80 --profile phone --drain

# Review-task coverage: typing, tone feedback, drawing, recognition
scripts/journey.sh --scenario review-modes --profile phone

# Draw the real guide strokes from the content database
scripts/journey.sh --scenario draw-recognition --profile phone --screenshots

# Run two AVDs at once
scripts/journey.sh --standard --parallel 2

# Bottom bar must meet the content viewport, at 1x and large text
scripts/journey.sh --scenario nav-gap --profile phone --screenshots
scripts/journey.sh --scenario nav-gap --profile phone-compact --font-scale 1.5

# Progress windows, empty states, and course-scoped analytics
scripts/journey.sh --scenario progress-ranges --profile phone
scripts/journey.sh --scenario progress-empty --profile phone

# Fold posture transition
scripts/journey.sh --scenario fold-transition --profile fold-inner

# Spell quest: every script, theme, motion setting, and entry point
scripts/journey.sh --spell --screenshots
scripts/journey.sh --scenario spell-quest --profile phone --screenshots
scripts/journey.sh --scenario spell-quest-ja --profile phone --screenshots
scripts/journey.sh --scenario spell-quest-ko --profile phone --screenshots

# Keep screenshots and close the emulators when finished
scripts/journey.sh --scenario daily20 --profile phone --screenshots --shut-down
```

## Presets

- `--quick`: phone, 8 short journeys including writing/typing review modes and guide-stroke drawing recognition. Good before a commit.
- `--standard`: phone, fold inner/outer, tablet. Core, daily edge cases, typing pronunciation, and guide-stroke drawing recognition.
- `--full`: all profiles, all daily states, the 100-review drain, multi-script drawing recognition, accessibility sweep, fold transition.
- `--spell`: the spell quest on phone, tablet, fold-inner, and fold-outer: gate, entry points, gift, result, real draw/cast, brush tray, one-way clues, effect frames, dark, reduce motion, and all four scripts.

`--drain` converts daily and completion scenarios into full drains. Drain runs force pronunciation reviews because they have a “Show answer” path, so the queue can be completed quickly. Drawing and recognition coverage stays in `review-modes`.

## Visual and layout regression

The harness can compare every screenshot with a local golden and audit the live
hierarchy for layout mistakes. This is the main defence when there is no
analytics or crash reporting.

```bash
# Regenerate the local baselines after an intentional UI change
scripts/journey.sh --scenario screen-pass --profile phone --screenshots --update-goldens

# Compare a run with the baselines and fail on a diff
scripts/journey.sh --scenario screen-pass --profile phone --screenshots --goldens --require-goldens

# Check controls for clipping, off-screen bounds, tiny targets, and overlaps
scripts/journey.sh --scenario screen-pass --profile phone --screenshots --layout-audit

# Everything on the phone
make quality

# Phone, fold inner, and tablet
make quality-full
```

Baselines live in `qa/goldens/<profile>/<scenario>/<shot>.png`. The top status
bar and bottom gesture area are masked; the rest is compared at half resolution
with a tolerance. A diff writes `visual/<shot>.compare.png` next to the run and
fails the scenario.

`--layout-audit` writes `layout-<shot>.json` and `layout-<shot>.xml` for every
screenshot. It reports:

- clickable nodes with no usable size
- clickable nodes that leave the screen
- clickable nodes that extend past their parent
- touch targets below 40 dp (warning)
- named action controls that overlap

The run also writes `contact-sheet.png` and `contact-sheet.html` for human
review. Review the sheet before accepting a release; a bad layout can become a
golden if nobody looks at it.

## Random walk

```bash
make monkey
```

The deterministic random walk switches tabs, flings lists, and taps safe
actions for 60 steps. After every step it checks that Bihua is still the
foreground app; every tenth step audits the hierarchy for layout errors. It is
intended to find the awkward combined state that a scripted happy path misses.

## Performance and motion notes

```bash
make perf
```

`--perf` records `gfxinfo` at the end of each scenario and writes `perf.json`.
Use it on a physical device for a meaningful frame result; the emulator uses
software rendering. `--perf-jank-limit 10` turns the snapshot into a gate.

The harness sets the system animation scales to 0 for deterministic journeys.
Visual goldens therefore show static backgrounds and no confetti. Use a
physical device with animation scales at 1 to review motion, the spell effects,
and the reduced-motion fallback. The app follows the system remove-animations
switch, and Settings says so.

## Profiles

| Profile | AVD | Size |
|---|---|---|
| phone | voxbloom | 1080 × 2400, 420 dpi |
| phone-compact | voxbloom | 720 × 1280, 320 dpi |
| phone-landscape | voxbloom | 2400 × 1080, 420 dpi |
| fold-inner | bihua_fold | unfolded 2208 × 1840 |
| fold-outer | bihua_fold | folded 1080 × 2092 |
| tablet | bihua_tablet | 2560 × 1600, 320 dpi |
| tablet-7 | bihua_tablet | 1200 × 1920, 240 dpi |

One harness run holds an AVD lock. A second run that needs the same AVD exits instead of resetting the app under the first run.

AVDs are kept alive by default so the next run starts almost immediately. Use `--shut-down` to kill emulators the harness started. Standard and full runs work on different AVDs in parallel; `--parallel N` controls how many, with a default of two.

`SpellQuestStressTest` generates 50 chapters for each of the four scripts
(1,200 puzzles) and checks every scene, shape, hint, and chapter. It is part of
the normal unit-test run.

Guide strokes are drawn with one batched ADB motion-event command per stroke. This avoids uiautomator2's idle wait after each segment. The quick suite is about two minutes on a warm phone AVD; a 100-review drain is about two minutes including setup.

## Scenarios

| Scenario | What it checks |
|---|---|
| `fresh` | onboarding, Skip setup, five new characters |
| `onboarding-pages` | every onboarding page, the scrollable extra-practice card, and Start writing; phone, tablet, fold |
| `offline-first-run` | clean device with airplane mode on: skip setup, open Home, and reach a review without waiting for the reader |
| `rotation-state` | review ink/answer, report draft, Settings section, and Practice survive a turn |
| `rotation-onboarding` | the script page and setup step survive a turn |
| `day1` | no reviews, 5 new, new-only session |
| `daily20` | 20 due, 5 new, mixed block, lesson, leave |
| `daily80` | 80 due, 5 new, Home counts and session start |
| `daily100` | 100 due, full drain, completion, caught-up Home |
| `completion` | small drain, completion screen, Back to home |
| `resume` | two grades, force-stop, resume card, resume |
| `process-death` | grade, process death, restore, continue |
| `missed-week` | 120 due backlog |
| `vacation` | paused Home and end-vacation dialog |
| `ease-back` | shorter 10-card session |
| `study-tools` | skipped and suspended cards stay out of the queue |
| `shared` | shared-glyph progress |
| `learning` | learning steps return shortly and Practice CTA |
| `goal30` | 30 new/day |
| `mature` | mature-content state across navigation |
| `navigation` | Home, Characters, Progress, Practice, Settings |
| `fold-transition` | fold and unfold while Home survives |
| `accessibility` | screenshots and unlabeled main controls |
| `review-modes` | writing draw/recognition and pronunciation typing, correct, wrong, tone-only |
| `draw-recognition` | draws real guide strokes for Simplified, Japanese, and Korean; checks same, variant, different, unavailable |
| `stroke-coach` | deliberately writes one stroke and checks the per-stroke coach card |
| `stroke-clean` | traces every guide stroke correctly and asserts the coach reports no false problem |
| `worksheet` | opens the builder, exercises sources, a template and grid, then exports a complete A4 PDF |
| `dictionary` | searches the dictionary, opens a word, and checks detail and reading |
| `word-guide` | opens word practice from the dictionary and checks the real stroke guide |
| `pronounce-kana` | checks that a kana pronunciation review masks its reading in the meaning |
| `reminders` | enables reminders, sends a test notification, and checks the notification shade |
| `nav-gap` | scroll viewport meets the floating bottom bar at 1x and 1.5x; fails above 14dp gap or under 2dp overlap |
| `progress-ranges` | 7D/30D/90D/1Y/All labels, Momentum, and the actual streak surviving each window |
| `progress-screen` | the whole report: stats, every range, calendar, charts, and the lower cards |
| `progress-all-time` | All time outgrows the one-year window and keeps the honest all-time copy |
| `report-flow` | About rows, the composer with and without a saved crash, preview, Copy, Send handoff, and the rate link; also dark, 1.5x, and landscape |
| `progress-empty` | untouched course shows New window, no grades, and calm empty cards |
| `settings-outfit` | pins a widget outfit from Display and checks the dialog labels |

Widget rendering is covered by JVM tests: `ReviewWidgetBinderTest` inflates the
1.1.1 layout and checks the two big numbers, the colored labels, the
plain-language forecast line, the short-slot fit, and the empty/vacation
states; `ReviewWidgetPreviewTest` draws the daily, wide, weekly, and night
layouts to PNGs under `composeApp/build/reports/`; `WidgetCopyTest` and
`ProgressStoreModeTest` cover the copy and the scheduler-backed day/week
amounts. The launcher picker and resize path still need a real launcher pass.
| `grade-help` | opens the plain-word grade legend from a session |
| `spell-entry` | opens the quest from the Home Study tools card and from Study tools |
| `spell-quest` | gate, scene, grimoire gift, result, real 水 draw/cast, brush tray, clue reveals, effect frames, wrong-answer path |
| `spell-quest-dark` | the same flow with the dark palette |
| `spell-quest-calm` | the same flow with reduce motion; effects skip, strokes still play |
| `spell-quest-ja` | the same flow in Japanese kanji |
| `spell-quest-ko` | the same flow in Korean Hanja |
| `spell-quest-traditional` | the same flow in Traditional characters |
| `spell-entry-ja`, `spell-entry-ko` | the two entry points in those scripts |
| `spell-flow` | gate copy, every draw tool label, one-way clues, all 10 grimoire chapters, and a clean leave |

## Review modes

`review-modes` seeds alternating `WRITE` and `PRONOUNCE` tasks and checks:

- writing canvas and guide are present
- the recall target character is not rendered before the answer is checked
- drawing a stroke unlocks `Check & grade`
- recognition feedback appears, or the explicit unavailable message does
- pronunciation shows the character and meaning
- a wrong reading shows `Not quite`
- a correct numbered pinyin reading shows `Correct!`
- missing the tone shows `Almost. Just the tone`
- both task types can be graded and the session returns Home

## State generation

`qa/state.py` builds a schema-10 progress database from the real content DB. It supports:

```text
--reviews N --new N --known N --learning N --days D
--leeches N --lapses N --skipped N --suspended N
--study-list N --priority N
--mode SIMPLIFIED|TRADITIONAL|JAPANESE|KOREAN
--theme SYSTEM|LIGHT|DARK
--shared-glyphs --mature --goal N --vacation --ease-back
--pronunciation --task-mode BOTH|WRITE|PRONOUNCE
--review-order RANDOM|OLDEST_FIRST|...
```

For pronunciation scheduling it reproduces the app’s Java string hash so tasks can be forced to alternate or to one type.

## Debug-only bridge

`composeApp/src/debug/.../QaBridgeReceiver.kt` accepts base64 settings and profile JSON, writes them through the real `SettingsStore`, and marks completion. Release builds do not contain it.

## Artifacts

```text
.qa/runs/<timestamp>/
  index.html
  junit.xml
  <profile>/<scenario>/
    result.json
    screenshots
    review-modes.txt
    failed.xml / failed.png / logcat.txt on failure
```

The whole `.qa/` directory is ignored by git.

## Flow galleries

`scripts/flow-gallery.sh` turns the latest captures for a flow into one image per
device, so a design review can see every screen in that flow together.

```bash
# Use the latest runs
scripts/flow-gallery.sh progress-screen
scripts/flow-gallery.sh word-whole-canvas,grade-reasons
scripts/flow-gallery.sh screen-pass --profiles phone,tablet

# Capture a fresh run first (three profiles, no rebuild)
scripts/flow-gallery.sh progress-pdf-rich --run

# Capture a fresh run and rebuild the debug APK once
scripts/flow-gallery.sh word-whole-canvas --run --build
```

The output is a run folder with three device folders:

```text
qa/gallery/<run-name>/
  phone/<flow>.png
  tablet/<flow>.png
  fold/<flow>.png
  INDEX.md
```

Name the run with `--name` (for example `--name 1.1.2-audit`); the default is a
timestamp. Each sheet labels every capture and notes the source run. These are
evaluation sheets, not store assets; the store set stays in
`store/play-assets/` and never includes the Mnemonic palace.

`make gallery SCENARIO=progress-screen` is the short form.
