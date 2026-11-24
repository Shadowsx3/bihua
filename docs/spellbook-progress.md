# Spellbook progress and handoff

Continuity document. Read this with `docs/spellbook-game-plan.md` before touching
the spell quest. The plan is the design intent; this file is the working state,
the traps, and the next checks.

## Current state

Everything below compiles and is covered by unit tests.

- `spellbook/Spellbook.kt` reads `assets/spellbook.csv`: **104 spells, 25
  families, 8 chapters**. The chapters are Tides, Flame, Sky, Ground, Creatures,
  Craft, Mind, and Time. Light and shadow are separate families, and Mind/Time
  have their own characters and chapter art.
- `tools/spells/parts.sql` remains the authored source for `puzzle_parts.db`.
  `tools/spells/build_parts.py` compiles it.
- The fixed 1500-variant library, `puzzles.db`, `PuzzleBook`, and the seed/build
  tools are retired. Every scene is built by `PuzzleForge` from the parts.
- `spellbook/Puzzle.kt` holds the `Puzzle` and `Solution` models.
- `QuestGenerator` always uses the forge. It never selects a library puzzle.
- `spellbook/QuestText.kt` owns the scroll word, the first-gloss fallback, the
  emoji, the result family, and the definition gloss. `SpellQuestLogicTest`
  covers them.
- `practice/SpellQuestScreen.kt`:
  - gate with a pinned Start action and a book-fold two-page layout;
  - visual novel with a fixed-height, scrollable card;
  - cast overlay with the same rune ring as the main circle stage, a clipped paper
    drawing surface, eight-colour runes, neutral outer ring while writing, family
    colour after recognition, a larger drawing area, the clue card above, and a
    pinned Cast;
  - definition card above the circle, clues/ink controls below;
  - hint/result tip card with the attempted character's reading, meaning, and a
    real word after a success;
  - wrong answers use Mò's need hint and the tip card shows what was actually
    drawn, not the scene answer;
  - guided drawing and learner playback use a roughly fixed total time; learner
    ink is flat and takes the detected family colour, with no glow;
  - gift playback starts empty (`startComplete = false`) and only becomes the
    completed guide after its strokes play;
  - reduce motion stops the scene motes, rune spin, and effects.
- `practice/GrimoireScreen.kt`:
  - 8 chapter tabs, cover, index, one spread per chapter;
  - index rows are compact and scroll on short heights;
  - light/dark palette; fold hinge gap instead of a fake spine on BOOK.
- `HomeScreen` Study tools card has a direct "Spell quest" row in Simplified. The
  game is one tap from Home, but framed as extra practice, not the main action.
- Sound: a generated ambient bed (pad, low fifth, heartbeat, plucks) and warm cues
  for cast, wrong, gift, reveal, walk, and chapter.
- The spell quest is Simplified-only. The entry is hidden in other courses until
  those spell tables exist; the grimoire remains guarded too.

## Tests

- `SpellbookTest`: 104 spells, 8 chapters, unique characters.
- `QuestGeneratorTest`: six-scene chapters, no back-to-back place, every chosen
  spell can solve its own scene.
- `PuzzleForgeTest`: solver rule, hints, determinism, target generation, variety.
- `ShippedPuzzlePartsTest`: every obstacle has at least two fitting families;
  2000 seeds give 200+ place/prompt combinations.
- `GrimoireSpreadsTest`: cover/index order, spread pairing, phone pairs.
- `SpellQuestLogicTest`: scroll word, result family, definition gloss, timing,
  emoji, tip text.
- UIAutomator `spell-quest` checks the gate, a gift from the grimoire, the
  result, a real draw/cast, the cast overlay's labels, and the wrong-answer
  path. It draws 水's real guide strokes, not a synthetic plus.

## Bugs fixed in this pass

- A selected grimoire spell flashed complete before its stroke animation. Gifts
  now use an empty-start player and never show the finished guide first.
- The grimoire index overflowed after the chapter count grew to eight. Rows are
  compact and the index scrolls; the page order follows `SpellCategories`.
- The tablet/fold cast overlay was a phone stack. It is now a two-page spread
  with the story on the left and the circle on the right (tablet and BOOK).
- The cast overlay's inner circle was off-centre. It is centred on the rune ring.
- The full-screen quest left Home controls in the accessibility tree behind it.
  The app shell is cleared from semantics while the quest is open.
- The hint card was showing the scene's answer. It now shows the attempted
  character's reading and meaning, and only shows the answer after the learner
  asks for it.
- The forge kept only the first four fitting families. Every family with an
  accepted tag now solves the puzzle, so an honest answer is never rejected.
- Guided drawing was 620 ms per stroke, which crawled on complex characters. It is
  now a roughly fixed total time, and the learner's cast playback follows the same
  pace.
- Empty windows / progress / build chrome fixes from the same QA pass are
  documented in `docs/1.1.0-release-checklist.md`.

## Next tasks

1. Verify the latest builds on all three main resolutions: phone, tablet, and
   fold-inner. The current `spell-quest` scenario covers the whole flow.
2. Dark theme at 1x and 1.5x on the gate, grimoire, scene, hint, and cast
   overlay. Use the `spell-quest-dark` scenario and inspect screenshots.
3. Reduce motion: run `spell-quest-calm`; effects should skip, strokes should
   still play, and the scene should not drift.
4. Re-run the full standard matrix so the Home Study tools card, the removed
   library, and the new data do not regress other screens.
5. Keep the landing copy in sync if the quest becomes part of the release story.

## Traps

- `withInfiniteAnimationFrameNanos` returns nothing when system animations are
  off. Essential playback uses `withFrameNanos`.
- Guide strokes are often path-only. Use `player.strokeCount`; never key on
  `centerlines()` being non-empty.
- `Modifier.fillMaxWidth().widthIn(max)` ignores the max. Put `widthIn` first.
- A `contentDescription` on a tile replaces its text for UIAutomator/TalkBack.
  Keep the character as text and use `onClickLabel` or a wrapper label.
- The cast overlay hides the main dialogue column; any control needed during a
  cast must live inside `CastOverlay`.
- Overlays need `consumeAllInput()` and app bar padding. The full-screen quest
  must also clear the shell's semantics so Home controls are not focusable
  behind it.
- Casting must never write cards, XP, or review rows. `doCast` is the only
  resolver; keep it side-effect free beyond ink points.
- The cue never shows the character or meaning before a cast. The meaning lands
  in the result/tip card.
- The grimoire gift id is looked up by glyph, not by the scene's spell; a shared
  glyph can belong to another character entry.
- The scenario excludes chapter seals when picking a spell tile. New chapters
  add seals; update the exclusion set when adding a chapter.
