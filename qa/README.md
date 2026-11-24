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

Guide strokes are drawn with one batched ADB motion-event command per stroke. This avoids uiautomator2's idle wait after each segment. The quick suite is about two minutes on a warm phone AVD; a 100-review drain is about two minutes including setup.

## Scenarios

| Scenario | What it checks |
|---|---|
| `fresh` | onboarding, Skip setup, five new characters |
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
| `progress-ranges` | 7D/30D/90D/1Y labels, Momentum, and the actual streak surviving each window |
| `progress-empty` | untouched course shows New window, no grades, and calm empty cards |
| `settings-outfit` | pins a widget outfit from Display and checks the dialog labels |
| `grade-help` | opens the plain-word grade legend from a session |
| `spell-entry` | opens the quest from the Home Study tools card and from Study tools |
| `spell-quest` | gate, scene, grimoire gift, result, real 水 draw/cast, brush tray, clue reveals, effect frames, wrong-answer path |
| `spell-quest-dark` | the same flow with the dark palette |
| `spell-quest-calm` | the same flow with reduce motion; effects skip, strokes still play |
| `spell-quest-ja` | the same flow in Japanese kanji |
| `spell-quest-ko` | the same flow in Korean Hanja |
| `spell-quest-traditional` | the same flow in Traditional characters |
| `spell-entry-ja`, `spell-entry-ko` | the two entry points in those scripts |

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

`app/src/debug/.../QaBridgeReceiver.kt` accepts base64 settings and profile JSON, writes them through the real `SettingsStore`, and marks completion. Release builds do not contain it.

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
