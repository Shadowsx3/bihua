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

- `app/src/main/assets/spellbook.csv`: 100 spells, 24 families, 8 chapters. Each spell
  has a character, a family, a category, an effect, a source, and a note.
- `tools/spells/parts.sql`: the procedural puzzle parts (families and their tags,
  obstacles and what they accept, tag outcomes, settings, times of day, weather).
  Source of truth. `tools/spells/build_parts.py` compiles it to
  `app/src/main/assets/puzzle_parts.db`.
- `tools/spells/puzzle_seeds.sql`: the authored puzzle seeds (32 places, each with
  honest solutions and per-family hints), in SQL.
- `app/src/main/assets/puzzles.db`: the expanded puzzle library (1500 variants), built
  from the seed SQL by `tools/spells/build_puzzles.py`. The app reads only this file.
- Content store: `bihua_content.db` provides the stroke guides (path-only strokes are
  common; never key UI on `centerlines()`).

### Engine (`spellbook/`)

- `Spellbook.kt`, `SpellFamilies.kt`, `Motifs.kt`: the taxonomy and visual identity.
- `Puzzles.kt`: `PuzzleBook` loads the authored library from `puzzles.db`.
- `PuzzleForge.kt`: procedural generation. `PuzzleParts.load(context)` reads the parts;
  `PuzzleForge.generate(parts, random, family, previousPlace)` builds a puzzle. The same
  seed gives the same puzzle, and the combinations are far more than any library holds.
- `Quest.kt`: `QuestSolver` (a cast solves when its family is one of the puzzle's
  solutions, otherwise it gets the hint) and `QuestGenerator` (scenes per chapter; uses
  the forge when parts are present, the library otherwise).
- `QuestText.kt` (practice package): `shortGloss` for the word on the scroll, and
  `spellEmoji` for the emoji (animal faces for animals, element emoji otherwise).

### Screens (`practice/`)

- **Gate** (`SpellQuestScreen.kt`): "The road ahead", three rule cards, Start the road,
  Open the grimoire, Leave.
- **Visual novel**: the scene card (place, speaker, Mò, the label pill, the text, the
  actions). The card has a fixed height (380 dp); its text scrolls inside it.
- **Grimoire** (`GrimoireScreen.kt`): a book. Cover, index, then one spread per
  category: the category on the left, every character on the right. The index rows and
  the bookmark tabs jump to a category. Light and dark follow the app theme. Phones
  stack the same pair (category card above the characters).
- **Circle stage**: outer ring with rotating runes (drawn as strokes, no font), the
  guide circle, the drawn shape, and the effects. The effect plays inside the circle:
  a lead-in before the strokes, a flourish after them.
- **Draw** (cast overlay): the drawing circle, "Show the shape" (plays the strokes),
  Hear it (reveals the reading), Meaning (reveals the gloss), Undo, Clear, Cast, Go back.
  The definition card under the circle shows the reading and gloss once asked for.

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

- **Phone**: stacked. The circle stage on top, the fixed-height card below.
- **Fold and tablet** (screen width 700 dp and up): two halves. The visual novel on the
  left half, the drawing circle on the right half, equal weight, a 24 dp gap at the
  hinge. The circle is sized from the room it gets (up to 440 dp).
- Rune ring and circle use the smaller side on both axes, so they stay round in any
  container.

### Feel and motion

- Essential playback (strokes, effects) runs on frame time (`withFrameNanos`), so it
  still plays when system animations are off. Decorative loops may use the infinite
  variant.
- Reduce motion skips the effects, keeps the strokes, and shortens the waits.
- A guide never flashes complete before its strokes: the player resets progress at the
  moment play is requested.

### Quality

- Unit tests: `GrimoireSpreadsTest` (spread rules), `PuzzleForgeTest` (solver rule,
  nudges, determinism, variety), `ShippedPuzzlePartsTest` (every obstacle has at least
  two fitting families; 2000 seeds give 200+ distinct combinations), `PuzzleBookTest`,
  `SpellbookTest`.
- UIAutomator scenario `spell-quest` (`qa/scenarios.py`): the gate, the gift from the
  grimoire, the result, a real draw and cast, and the wrong-answer path. Runs on phone,
  tablet, and fold-inner.

## Still to do before release

### Required

- Switch gameplay fully to the forge and retire the 1500-variant library once the
  forge's variety is checked on device.
- Verify on device: the fold two-pane layout (left visual novel, right drawing), the
  rune ring, the effect choreography, the name scroll timing, the definition card, and
  the wrong-answer flow on all three profiles.
- Dark theme and 1.5x text on the grimoire, the gate, and the visual novel.
- Reduce-motion pass on every flow.
- Accessibility: TalkBack labels on the circle, the buttons, and the grimoire tiles.

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
