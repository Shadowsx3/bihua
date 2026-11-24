# Spellbook progress and handoff

Continuity document. Read this before touching the spellbook work, then continue
at "Next tasks" without waiting for a briefing. Design intent and the ordered
backlog live in `docs/spellbook-game-plan.md`.

## Where it stands

Built and compiling:

- `spellbook/Spellbook.kt` reads `assets/spellbook.csv` (100 spells).
- `spellbook/SpellFamilies.kt` is the taxonomy: 24 families, 8 chapters
  (Tides, Flame, Sky, Ground, Creatures, Craft, Mind, Time), each with an
  `EffectStyle` that drives the cast animation.
- `spellbook/Puzzles.kt` reads the compiled `assets/puzzles.db` (SQL seeds under
  `tools/spells/puzzle_seeds.sql`, built by `tools/spells/build_puzzles.py`).
- `spellbook/Quest.kt` holds `QuestSolver` (any fitting family wins, otherwise a
  hint) and `QuestGenerator` (endless chapters, real characters, no repeated
  place back to back).
- `practice/GrimoireScreen.kt`: book with wood desk, leather cover, parchment
  pages, ruled border, spine, chapter seals, colour index with page numbers,
  spell pages. All characters shown; unseen are dim with meaning hidden.
- `practice/SpellQuestScreen.kt`: gate screen (witch Mò, three rule cards,
  Grimoire / Start / Leave), illustrated scene with drifting motes, visual-novel
  dialogue card with typewriter and colour-coded labels (Problem / Mò's nudge /
  It worked), magic circle cast overlay (opaque, target hidden until "Show
  guide"), labelled Undo / Clear / Guide controls, Cast button, full-screen
  element effect, generated ambient loop.
- `audio/Ambient.kt`: synthesised pad, pulse, and pentatonic plucks; loops with a
  crossfade; respects `Sfx.enabled`.
- `ui/components/Components.kt`: `MascotHeadwear.WITCH` (large brim, band,
  buckle).
- Entry point: Home → Study tools → Spell quest. The grimoire is reached from
  the gate screen, not from Study tools.

Verified:

- `make check` green (compile, unit tests, content audit, APK, verify).
- Phone screenshots: gate, mission card, cast overlay (pre-fix), grimoire book.
- Tests: `SpellbookTest` (6), `PuzzleBookTest` (9), all passing.

Not verified yet:

- Colour-coded labels and the new cast controls on a device.
- The cast overlay after the opaque/hidden-target change.
- Fold, tablet, 1.5× text, dark theme for both screens.
- Audio actually audible (emulator audio can be muted).

## Since the first pass

- Puzzle library is 1500 variants (9,517 solution rows) expanded from the SQL seeds
  in `tools/spells/puzzle_seeds.sql` by `tools/spells/build_puzzles.py`, varying time
  of day and weather. The app reads the indexed `puzzles.db` directly. No CSV is used
  for puzzles.
- The book picker is a parchment sheet (not a Material dialog), with family
  chips that show counts and glow before a character is traced.
- Choosing a character traces its real guide strokes into the circle, then the
  effect plays. A cast always resolves, even if the guide is still loading.
- Clues escalate and stay optional: reading + meaning, "Hear it" audio, "Show
  the shape" trace, and the book at any stage.
- After a cast, the result card teaches: character, reading, meaning, and one
  real word.
- The wide layout is an open book: story on the left page, circle on the right,
  stitched spine between. The dialogue card is capped at 560 dp so it cannot
  stretch on tablets.
- Fixed a pinyin bug: `tóngnián` became `tongn2ian2`; coda handling now starts a
  new syllable after `n`/`ng`. Covered in `PinyinTest`.
- `spell-quest` UIAutomator scenario passes on phone end to end.

## Latest state (read this first)

Flow, as built:

- Gate: three rule cards, "Start the road", "Open the grimoire", "Leave".
- Scene: the visual-novel card (Problem / Mò's nudge / It worked). Two actions:
  "Draw a spell" and "From the book".
- "From the book" opens the grimoire over the quest. Each category is one
  spread: category on the left, every character on the right. Phones stack the
  same pair (category card above the characters). Tapping a character starts a
  reading gift. The scene stays on screen.
- A gift plays the book's guide with the Practice playback (`rememberStrokeOrderPlayer`
  + `StrokeGuideLayer`, 620 ms per stroke). The circle sits centred over the dialogue.
  "Continue the quest" unlocks after the strokes finish.
- "Draw a spell" opens the drawing overlay. Its only back action is "Go back"
  (returns to the dialogue). "Show the shape" draws the guide's real strokes, still,
  in lilac. A valid cast closes the overlay, plays the learner's own strokes in
  the circle over the dialogue (`GuideWriting`), runs the element effect (after the
  strokes, skipped with reduce-motion), and shows the result.
- The circle stays in its final state through the result and clears only on
  "Walk on" (`advance()`).
- Grimoire palette follows the app theme: light is ivory paper with a sage accent;
  dark is slate with pale ink. Cover is sage leather with gilt. Tiles are ivory with
  a faint chapter tint, spaced, and the grid fits every character on one page.
  Bookmark tabs show the family icon and seal, and jump to a category. Index rows
  jump too.
- Grimoire and gate panels cap at 560 dp on tablet and fold
  (`widthIn(max)` must come before `fillMaxWidth()`).

Traps found this session (keep these):

- `withInfiniteAnimationFrameNanos` returns nothing when system animations are off.
  Essential playback (stroke writing, spell effect) uses `withFrameNanos`, fully
  qualified as `androidx.compose.runtime.withFrameNanos`.
- Guide strokes in `bihua_content.db` are often path-only, so
  `CharacterGuide.centerlines()` is empty. Do not key UI on it; use the player's
  `strokeCount`.
- The guide id is looked up from the content repository when a gift starts. The
  `idByGlyph` map is built asynchronously and can still be empty at pick time.
- `Modifier.fillMaxWidth().widthIn(max)` ignores the max. Put `widthIn` first.
- A `contentDescription` on a tile replaces its text for UIAutomator and TalkBack.
  The tile keeps its character as text.
- Chapter seals are Chinese characters too. Scenarios exclude them when picking a
  spell tile.

## Investigation: why the Practice guide draws strokes and the quest did not

- The Practice guide reads the content store through `LocalContentRepository`,
  which is provided by `BihuaApp`. The quest was rendered in a composition where that
  local was not in scope, so `rememberStrokeGuide` returned null for every gift. The
  id lookup and the stroke data were both correct (石 has 5 path strokes in
  `bihua_content.db`).
- Fix in code: `SpellQuestScreen` now takes `repository` from `BihuaApp` and provides
  `LocalContentRepository` around its content, so the guide player and the id lookup
  both see the store.
- Logging: the quest logs under the tag `BihuaQuest` (`adb logcat -s BihuaQuest`).
  It records the guide id for each gift and warns when no content store is in scope.
- Tests: `app/src/test/.../practice/GrimoireSpreadsTest.kt` covers one spread per
  category, the category on the left, phone pairs matching wide pairs, and skipped empty
  categories. `bookSpreads` and `Spread` are `internal` for this.
- Pending: the device run after the fix is not confirmed yet. The last phone run
  (before the fix) still showed an empty circle and "Drawing the shape…".

## Wrong pick (Not this one)

- A wrong gift or cast keeps the character on the circle, so the learner sees what was
  drawn. The label is "Not this one" and the body is the solver's hint.
- Actions: "Walk on" (advance, clears the circle), "Draw a spell" (`drawAgain`, clean
  circle, drawing overlay), "From the book" (opens the grimoire). "Try another way" is
  gone.
- `startGift` moves to the dialogue (READ) and clears the old hint, so a book pick
  always lands on the gift screen.

## Open issues (identified, not yet verified on device)

- Fold: the card and circle still need a true two-pane layout (circle left, card right).
  Current fix only shortens the card and enlarges the circle on wide screens.
- Guide flash: fixed in `rememberStrokeOrderPlayer` (progress resets when play is asked).
  Verify in Practice and Learn as well as the quest.
- "Show the shape" plays only once started (`shapeStarted`); verify no first-frame flash.
- Pronunciation stays hidden until "Hear it". Meaning shows the character's real gloss.
- Dialogue card has a fixed height; text scrolls inside it. Verify the circle size is
  identical across READ, HINT, and RESULT.
- Effect choreography: lead-in inside the circle, strokes, flourish, then Continue.
  Verify the timing on device.
- Scenario coverage: spell-quest now drives a real draw and cast (UIAutomator).
- Tests to add: stroke playback timing helpers, InvocationBadge word selection, spread
  rules (GrimoireSpreadsTest exists), definition gloss lookup.

## Next tasks, in order

1. Verify on device: phone, tablet, fold-inner. Screenshot mid-gift (the guide
   strokes visible, no stand-in character), the cast after a valid read (circle
   over dialogue, strokes in order, effect, result), and the result with the circle
   still up.
2. Check light and dark theme on the grimoire, plus 1.5x text on the gate, grimoire,
   and dialogue.
3. Reduce-motion pass: the effect is skipped, strokes still play.
4. Cast flow: a wrong character gets Mò's nudge and "Try another way", not a dead end.
5. Re-run `make check`, then the `spell-quest` scenario on phone, tablet, and
   fold-inner.
6. Backlog from `docs/spellbook-game-plan.md`: mnemonic shapes, reading gift detail,
   compounds, sister spells, margin notes, quiet cast mode, Mò's familiar, spell of
   the day, bad-spell near misses, ink jar, bestiary, daily scroll, share card.

## Known issues and traps

- `qa/device.sh tap` can wedge inside `uiautomator dump` on the emulators; if a
  tap hangs, kill it and use `adb shell input tap` with scaled coordinates
  (screenshot is 900×2000 for a 1080×2400 screen).
- Home's Study tools card is below the fold: swipe twice before tapping Manage.
- The cast overlay hides the main dialogue column, so any control needed during
  a cast must live inside `CastOverlay`.
- Overlays must keep `consumeAllInput()` or taps reach Home behind them.
- Keep `appStatusBarsPadding()` / `appNavigationBarsPadding()` on full-screen
  overlays.
- Casting must never write cards, XP, or review rows. `doCast` is the only
  place a cast resolves; keep it side-effect free beyond ink points.
- The cue never shows the character or meaning before a cast; the meaning lands
  only as the result line.
