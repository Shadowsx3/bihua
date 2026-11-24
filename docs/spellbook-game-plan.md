# Spellbook: design and plan

This is the design record for the spell quest and the grimoire. It says what is
built, how the game loop works, and what is still missing. The working handoff
(traps, verification steps, next tasks) lives in `docs/spellbook-progress.md`.

## Principles

- Free practice. Casting never creates cards, XP, review rows, or schedule changes.
- Nothing is graded or timed. A wrong cast is not a failure: it names what was drawn,
  shows the right answer if asked, and offers another try.
- Every puzzle has several honest solutions. A family solves an obstacle only when it
  carries one of the obstacle's accepted tags.
- Clues are optional and escalate: reading (Hear it), meaning, shape. Casting without
  clues is always possible.
- Warm and quiet: paper, ink, soft motion, no streaks, no pressure, no ads.
- Mò (the witch) is the guide. Mò's nudge names an element or a need, never the answer.

## What is built

### Data

- `app/src/main/assets/spellbook.csv`: 104 spells, 25 families, 8 chapters (Tides,
  Flame, Sky, Ground, Creatures, Craft, Mind, Time). Each spell has a character, a
  family, a category, an effect, a source, and a note.
- `tools/spells/parts.sql`: the procedural puzzle parts (families and their tags,
  obstacles and what they accept, tag outcomes, settings, times of day, weather).
  Source of truth. `tools/spells/build_parts.py` compiles it to
  `app/src/main/assets/puzzle_parts.db`.
- The fixed 1500-variant library and its seed/build files are retired. Every scene is
  built from `puzzle_parts.db`; there is no fallback puzzle database in the app.
- Content store: `bihua_content.db` provides the stroke guides (path-only strokes are
  common; never key UI on `centerlines()`).

### Engine (`spellbook/`)

- `Spellbook.kt`, `SpellFamilies.kt`, `Motifs.kt`: the taxonomy and visual identity.
- `Puzzle.kt`: the `Puzzle` and `Solution` models shared by the forge, solver, and UI.
- `PuzzleForge.kt`: procedural generation. `PuzzleParts.load(context)` reads the parts;
  `PuzzleForge.generate(parts, random, family, previousPlace)` builds a puzzle. The same
  seed gives the same puzzle, and the combinations are far more than any library holds.
- `Quest.kt`: `QuestSolver` (a cast solves when its family is one of the puzzle's
  solutions, otherwise it gets the hint) and `QuestGenerator` (scenes per chapter,
  always built from the forge).
- `QuestText.kt` (practice package): `shortGloss` for the word on the scroll, and
  `spellEmoji` for the emoji (animal faces for animals, element emoji otherwise).

### Screens (`practice/`)

- **Gate** (`SpellQuestScreen.kt`): "The road ahead", three rule cards, Start the road,
  Open the grimoire, Leave. On a book fold it is a two-page gate: intro on the left,
  rules and actions on the right. The action row stays pinned so Start is always reachable.
- **Visual novel**: the scene card (place, speaker, Mò, the label pill, the text, the
  tip card, the actions). The card height is fixed per font scale (380 dp at 1x, 400 dp
  for a hint at 1.5x) and its text scrolls inside it.
- **Grimoire** (`GrimoireScreen.kt`): a book. Cover, index, then one spread per
  category: the category on the left, every character on the right. The index rows and
  the bookmark tabs jump to a category. Light and dark follow the app theme. Phones
  stack the same pair (category card above the characters).
- **Circle stage**: outer ring with rotating runes (drawn as strokes, no font), the
  guide circle, the drawn shape, and the effects. The effect plays inside the circle:
  a lead-in before the strokes, a flourish after them.
- **Draw** (cast overlay): a paper drawing surface inside the same rune ring as the
  main circle stage. The paper is clipped to the inner circle so ink cannot run over
  the runes. Each rune takes one of the eight chapter colours. The outer ring starts
  neutral and switches to the detected spell family's colour after recognition.
  "Show the shape" plays the strokes, Hear it reveals the reading, Meaning reveals
  the gloss; Undo, Clear, Cast, Go back. The definition card sits above the circle;
  the Cast button stays pinned. Clues and ink controls scroll below.
- **Tip card**: readings and meanings appear in a tinted card with a small spark icon,
  not as bare text.
- **Home entry**: the Study tools card has a direct "Spell quest" row, so the game is
  one tap from Home but still framed as extra practice.

### Game loop

1. A chapter is built from six scenes. Each scene has a spell (a real character from
   the course) and a puzzle the spell's family can solve.
2. The scene shows the problem in the visual novel.
3. The learner chooses one of:
   - **Draw**: opens the drawing circle. Show the shape if stuck; Hear it or Meaning
     for clues. Cast reads the ink with the handwriting recogniser.
   - **Grimoire**: opens the book. Picking a character starts a reading gift: the
     character's real guide plays in the circle, then Continue unlocks.
4. A valid read or a gift plays the learner's strokes (or the guide) in the circle,
   then the element effect, then the result ("It worked") with the word on the scroll.
5. A cast that does not solve the puzzle shows "Not this one" with the character still
   on the circle. Actions: See the right answer (plays the correct character, then
   Walk on), Walk on without it, Draw, Grimoire.
6. Walk on clears the circle and moves to the next scene.

### Layout by device

- **Phone**: stacked. The circle stage on top, the fixed-height card below. The cast
  overlay keeps the drawing circle large and the Cast button pinned.
- **Tablet**: two halves. The visual novel on the left, the drawing circle on the
  right, equal weight.
- **Book fold**: the same two pages with a 28 dp hinge gap. The gate, scene, cast
  overlay, and grimoire all respect the hinge; no card or circle spans the fold.
  The circle is sized from the room it gets (up to 440 dp).
- Rune ring and circle use the smaller side on both axes, so they stay round in any
  container.

### Feel and motion

- Essential playback (strokes, effects) runs on frame time (`withFrameNanos`), so it
  still plays when system animations are off. Decorative loops may use the infinite
  variant.
- Guided drawing uses a **roughly fixed total time** for every character (about two
  seconds, clamped per stroke), so a complex character does not crawl and a simple
  one does not vanish. The learner's own cast playback uses the same pacing.
- Reduce motion skips the effects, keeps the strokes, and shortens the waits. Scene
  motes and the rune spin stop as well.
- A guide never flashes complete before its strokes: the player can start empty
  (`startComplete = false`) and resets progress at the moment play is requested.
- The learner's own ink is **never glowing**. In the draw overlay it is ordinary dark
  ink on paper. When it plays back after a successful cast, it is flat ink in the
  detected spell family's colour, with no halo or glow. Only guides and the spell
  effect carry magic.
- Sound: a 24-second generated pad, low fifth, heartbeat, and pentatonic plucks loop
  under the quest. Cues are soft bells, harps, and low warm answers, never buzzers or
  jingles. The solved cue is a three-note chime; a wrong cast is a low fifth, not a
  failure sound. A new chapter gets a warm chord, and a walk gets a quiet step.

### Quality

- Unit tests: `GrimoireSpreadsTest` (spread rules), `PuzzleForgeTest` (solver rule,
  nudges, determinism, variety), `ShippedPuzzlePartsTest` (every obstacle has at least
  two fitting families; 2000 seeds give 200+ distinct combinations), `QuestGeneratorTest`,
  `SpellbookTest`, `SpellQuestLogicTest`.
- UIAutomator scenario `spell-quest` (`qa/scenarios.py`): the gate, the gift from the
  grimoire, the result, a real draw and cast, the cast overlay's labels, and the
  wrong-answer path. Runs on phone, tablet, and fold-inner.

## Still to do before release

### Required

- Done: gameplay uses the forge only; the 1500-variant library and its tools are gone.
- Done: every family that carries an accepted tag is now accepted by the solver; the
  old four-solution cap made some honest answers read as wrong.
- Verify on device: the fold two-page layout, the rune ring, effect choreography, name
  scroll timing, the definition card, and the wrong-answer flow on phone, tablet, fold.
- Dark theme and 1.5x text on the grimoire, gate, and visual novel.
- Reduce-motion pass on every flow.
- Accessibility: TalkBack labels on the circle, buttons, and grimoire tiles.

### Planned (from the earlier backlog)

- Mnemonic shapes after a cast.
- Reading gift detail and a typed incantation.
- Compounds as combos (two spells, one puzzle).
- Sister spells across scripts.
- Personal margin notes in the grimoire.
- A quiet cast mode.
- Mò's familiar.
- Spell of the day.
- Weather in the book.
- Bad-spell near misses.
- Ink jar (earned ink as currency for hints).
- Scene variants and easter eggs.
- Bestiary (characters met in the quest).
- Daily scroll.
- Share card.

### Procedural follow-ups

- More obstacles and settings in `tools/spells/parts.sql`, each with at least two
  fitting families.
- Tag outcomes per obstacle, so the solution text fits the obstacle more closely.
- A difficulty curve: early chapters use obstacles with many fitting families.

## Content QA

Every character, word, reading, meaning, and story line is checked before a content
release (`docs/content-qa-backlog.md`). Puzzle text is authored in `parts.sql` and the
seed files; it must name elements and needs, never answers.
