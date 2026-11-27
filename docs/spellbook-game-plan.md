# Spellbook: design and plan

This is the design record for the spell quest and the grimoire. It says what is
built, how the game loop works, and what is still missing. The working handoff
(traps, verification steps, next tasks) lives in `docs/spellbook-progress.md`.

## Principles

- Free practice. Casting never creates cards, XP, review rows, or schedule changes.
- Nothing is graded or timed. A wrong cast is not a failure: it names what was drawn,
  shows the right answer if asked, and offers another try.
- Every puzzle has several honest solutions. An obstacle accepts tags, and any spell
  whose family or chapter carries one of those tags can solve it. Some puzzles widen
  the door to a whole chapter; a few ask for one exact shape, and say so in the
  scene and in the hint label ("Another chapter", "Not that shape").
- The generated copy stays plain: spell notes are short first glosses (never
  "A water spell"), time/weather pairs are checked for contradictions, and places
  do not repeat "under" or contradict a frozen/dry setting. Obstacles also carry
  place kinds, so a frozen lake never appears inside a copper market. A solved
  scene names what changed in that scene, not just the spell's element.
- The daily word is a shared calendar slot: the same study day and course give
  every learner the same word. It is not seeded by install time, progress, or
  the mature-content setting.
- Clues are optional and escalate: reading (Hear it), meaning, shape. Casting without
  clues is always possible.
- Warm and quiet: paper, ink, soft motion, no streaks, no pressure, no ads.
- A reason to continue that is not a grade: met pages, chapter banners, new-page
  notes, and every-tenth-page milestones. None of it changes study scheduling.
- Mò (the witch) is the guide. Mò's nudge names a need, never the answer.

## What is built

### Data

- `composeApp/src/androidMain/assets/spellbook.csv`: about 1,390 spells across four scripts
  (Simplified, Traditional, Japanese kanji, Korean Hanja), 35 families, and 10
  chapters (Tides, Flame, Sky, Ground, Creatures, Craft, Mind, Time, Arcana, Spirit).
  Each script has roughly 30–35 choices in every chapter. A spell row carries its
  script, character, family, category, effect, source, and note. Readings and
  meanings come from the course content database, so they are native to the script.
- `tools/spells/spell_catalog.py` is the authored semantic core; the builder maps
  each idea to the learner's script and fills gaps only with characters that exist in
  that course. `tools/spells/build_spellbook.py` writes the CSV.
- `tools/spells/parts.sql`: the procedural puzzle parts (35 families and their tags,
  47 obstacles and what they accept, 18 tags with multiple outcomes, 40 settings,
  26 times of day, 20 weathers, and the place-kind tables that keep obstacles and
  settings honest). The compatible frames give 19,607 unique places and 520,488
  obstacle-place pairs; each obstacle still fits 9–38 settings. Source of truth.
  `tools/spells/build_parts.py` compiles it to `composeApp/src/androidMain/assets/puzzle_parts.db`.
- The fixed 1500-variant library and its seed/build files are retired. Every scene is
  built from `puzzle_parts.db`; there is no fallback puzzle database in the app.
- Content store: `bihua_content.db` provides the stroke guides (path-only strokes are
  common; never key UI on `centerlines()`).

### Engine (`spellbook/`)

- `Spellbook.kt`, `SpellFamilies.kt`, `Motifs.kt`: the taxonomy and visual identity.
- `Puzzle.kt`: the `Puzzle` and `Solution` models shared by the forge, solver, and UI.
  A solution can require families, a whole category, or exact characters, so a puzzle
  can be broad or narrow.
- `PuzzleForge.kt`: procedural generation. `PuzzleParts.load(context)` reads the parts;
  `PuzzleForge.generate(parts, random, family, character, style, previousPlace)` builds
  a puzzle. `PuzzleStyle` chooses a family door, a whole chapter, or one exact shape.
  The same seed gives the same puzzle. After place fit, the authored doors still give
  about 29 million scene + place + style + solution combinations. Family puzzles
  always have at least two fitting families; chapter puzzles have a whole chapter of
  spells; exact-shape puzzles are the deliberate single-answer exception. Scenes
  within one chapter do not repeat the same obstacle, and the result body comes from
  the obstacle's own success line.
- `Quest.kt`: `QuestSolver` (a cast solves when its family, chapter, or exact character
  matches a solution, otherwise it gets the hint) and `QuestGenerator` (six scenes per
  chapter, always built from the forge, with a shuffled mix of the three styles).
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
  main circle stage. The paper is clipped to the inner circle, with black inner and
  outer borders, so ink cannot run over the runes and the drawing boundary is
  unmistakable. The drawing canvas fills the entire inner circle. Each rune takes one
  of the ten chapter colours and category runes. The outer background starts neutral and takes the
  detected spell family's tint after recognition.
  "Show the shape" plays the strokes, Hear it reveals the reading, Meaning reveals
  the gloss; both stay revealed and keep their names. A quest-only brush tray (Ink,
  Pen, Pencil, Marker) fills the quiet space under the clues and changes only this
  game's ink, never the app-wide brush. Undo, Clear, Cast, Go back. On a phone the
  definition card sits above the circle; on a tablet or fold it sits on the story
  page, where it fills the page and leaves the circle more room. The Cast button
  stays pinned. Clues and ink controls scroll below.
- **Mò's note**: a small spark button on the result/hint opens a compact card
  over the open middle of the dialogue card, like the text highlighter. It holds
  the reading, meaning, a real word, and new-page/milestone news in a scrollable
  body. The note no longer takes the result line's space, which fixed
  large-text clipping.
- **Home entry**: the Study tools card has a direct "Spell quest" row, so the game is
  one tap from Home but still framed as extra practice.

### Game loop

1. A chapter is built from six scenes. Each scene has a spell (a real character from
   the learner's script) and a puzzle that spell can solve. The six scenes always mix
   three broad family puzzles, two whole-chapter puzzles, and one exact-shape puzzle.
2. The scene shows the problem in the visual novel.
3. The learner chooses one of:
   - **Draw**: opens the drawing circle. Show the shape if stuck; Hear it or Meaning
     for clues. Cast reads the ink with the handwriting recogniser.
   - **Grimoire**: opens the book. Picking a character is one step: the result card
     appears immediately and the character's real guide draws in the circle. The
     result actions unlock when the guide finishes; there is no second Continue.
4. A valid read or a gift lands directly on the result screen. The learner's own
   strokes (or the guide) draw in the circle there, then the element effect plays;
   the result actions wait for the drawing to settle. There is no second
   "Continue" screen.
5. A cast that does not solve the puzzle shows "A different way" with the character
   still on the circle. The nudge names what the scene needs and, for narrow puzzles,
   says that a whole chapter or one exact shape is wanted. Actions: See the right
   answer (plays the correct character, then Walk on), Walk on without it, Draw,
   Grimoire.
6. Walk on clears the circle and moves to the next scene. The result body is only
   the thing that happened, not a second unrelated scene line. Every tenth new page
   gets a small milestone line, and crossing the sixth scene opens a short
   "Chapter N — the road goes on" banner.
7. The pages met and the chapter are stored in the progress database
   (`spell_quest_met`, `spell_quest_road`), so closing the quest, exporting,
   importing, or syncing keeps the collection. The gate shows the count; the
   grimoire marks met pages with a star.

### Layout by device

- **Phone**: stacked. The circle stage on top, the fixed-height card below. The cast
  overlay keeps the drawing circle large and the Cast button pinned.
- **Tablet**: two halves. The visual novel and definition card on the left, the
  drawing circle and tools on the right, equal weight.
- **Book fold**: the same two pages with a 28 dp hinge gap. The gate, scene, cast
  overlay, grimoire, and bookmark tabs all respect the hinge; no card or circle
  spans the fold. The circle is sized from the room it gets (up to 440 dp).
- Rune ring and circle use the smaller side on both axes, so they stay round in any
  container.

### Feel and motion

- Essential playback (strokes, effects) runs on frame time (`withFrameNanos`), so it
  still plays when system animations are off. Decorative loops may use the infinite
  variant.
- Guided drawing uses a **roughly fixed total time** for every character (about two
  seconds, clamped per stroke), so a complex character does not crawl and a simple
  one does not vanish. The learner's own cast playback uses the same pacing.
- A result appears immediately, but its Walk on / hint actions stay quiet until the
  guide or the learner's own strokes finish. "Mò is drawing the character…" or
  "Mò is reading the circle…" holds the moment.
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
  two fitting families and eight compatible places; 2000 seeds stay place-honest and
  give 200+ distinct combinations), `QuestGeneratorTest`, `SpellbookTest`,
  `SpellQuestLogicTest`, and `FeaturedWordTest` (same study day and course are shared;
  a month has no repeats; courses walk their own sequences). `ShippedPuzzlePartsTest`
  also checks every obstacle's success line and that generated results use it.
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
  fitting families and a place-kind fit.
- Tag outcomes per obstacle, so the solution text fits the obstacle more closely.
- A difficulty curve: early chapters use obstacles with many fitting families.

## Content QA

Every character, word, reading, meaning, and story line is checked before a content
release (`docs/content-qa-backlog.md`). Puzzle text is authored in `parts.sql` and the
seed files; it must name elements and needs, never answers.
