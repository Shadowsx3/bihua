# Spellbook progress and handoff

Continuity document. Read this with `docs/spellbook-game-plan.md` before touching
the spell quest. The plan is the design intent; this file is the working state,
the traps, and the next checks.

## Current state

Everything below compiles and is covered by unit tests.

- `spellbook/Spellbook.kt` reads `assets/spellbook.csv`: **1,387 spells across
  four scripts, 35 families, 10 chapters**. The chapters are Tides, Flame, Sky,
  Ground, Creatures, Craft, Mind, Time, Arcana, and Spirit. Simplified and
  Traditional have 35 choices in every chapter; Japanese has 347 spells and
  Korean 340, with a smaller Flame chapter because those courses have fewer
  fire characters. Japanese uses kanji and Korean uses Hanja; readings and
  meanings are native to each course.
- `tools/spells/spell_catalog.py` is the authored semantic core; the builder maps
  it to the learner's script through the content database and only fills gaps
  with characters that are in that course.
- `tools/spells/parts.sql` remains the authored source for `puzzle_parts.db`:
  35 families, 47 obstacles, 18 tags with several outcomes each, 40 settings,
  26 times, and 20 weathers. `tools/spells/build_parts.py` compiles it.
- The fixed 1500-variant library, `puzzles.db`, `PuzzleBook`, and the seed/build
  tools are retired. Every scene is built by `PuzzleForge` from the parts.
- `spellbook/Puzzle.kt` holds the `Puzzle` and `Solution` models. A solution can
  name families, a whole category, or exact characters. `PuzzleStyle` chooses the
  width, and every six-scene chapter mixes three family puzzles, two chapter
  puzzles, and one exact-shape puzzle.
- `QuestGenerator` always uses the forge. It never selects a library puzzle.
- `spellbook/QuestText.kt` owns the scroll word, the first-gloss fallback, the
  emoji, the result family, and the definition gloss. `SpellQuestLogicTest`
  covers them.
- `practice/SpellQuestScreen.kt`:
  - gate with a pinned Start action and a book-fold two-page layout;
  - visual novel with a fixed-height, scrollable card;
  - cast overlay with the same rune ring as the main circle stage, a clipped paper
    drawing surface with black boundaries, ten-colour runes, neutral background
    while writing, family tint after recognition, a larger drawing area, the clue
    card above, and a pinned Cast;
  - definition card above the circle on a phone, and on the story page on a
    tablet/fold so the circle gets the extra room; clues/ink controls below;
    clues reveal once, stay revealed for the scene, and keep their names; a
    quest-only brush tray changes this game's ink without touching the app-wide
    brush;
  - a small note button opens the same anchored card as the text highlighter,
    with the attempted character's reading, meaning, real word, new-page, and
    milestone news; the old tall tip card is gone, so the result body stays
    legible at 1.5x and 2x;
  - wrong answers use Mò's need hint with a "Not that shape"/"Another chapter"
    label, and the note popup shows what was actually drawn, not the scene answer;
  - guided drawing and learner playback use a roughly fixed total time; learner
    ink is flat and takes the detected family colour, with no glow;
  - a cast or gift goes straight to the result; the strokes draw there and the
    result actions wait for the drawing to settle; there is no Continue screen;
  - gift playback starts empty (`startComplete = false`) and only becomes the
    completed guide after its strokes play;
  - reduce motion stops the scene motes, rune spin, and effects.
- `practice/GrimoireScreen.kt`:
  - 10 chapter tabs, cover, index, one spread per chapter, script-specific seals;
  - index rows are compact and scroll on short heights;
  - light/dark palette; fold hinge gap instead of a fake spine on BOOK, including
    a 5 + 5 bookmark split so no tab crosses the physical hinge.
- `HomeScreen` Study tools card has a direct "Spell quest" row in every script,
  and the Study tools sheet has the same entry. The game is one tap from Home,
  but framed as extra practice, not the main action.
- Sound: a generated ambient bed (pad, low fifth, heartbeat, plucks) and warm cues
  for cast, wrong, gift, reveal, walk, and chapter.
- The spell quest is available in Simplified, Traditional, Japanese, and Korean
  courses. Each script's grimoire and puzzle pool come from its own spell rows.
- Met pages and the current chapter are stored in the progress database
  (`spell_quest_met`, `spell_quest_road`) so export, import, and sync carry
  them. The gate shows the met count and chapter; the grimoire marks met pages
  with a star; the result card celebrates a new page and every tenth page.
- Korean Hanja and Japanese kanji casts use the Han recognizer (a Han target and
  the zh-Hant/ja model), not the Hangul/kana model. This keeps a 水 drawing from
  being read as a Hangul syllable.

## Tests

- `SpellbookTest`: four scripts, 10 chapters, at least 300 spells per script,
  at least 30 choices per chapter, unique characters per script.
- `QuestGeneratorTest`: six-scene chapters, no back-to-back place, every chosen
  spell can solve its own scene, and chapters mix family, chapter, and exact-shape
  puzzles. Exact-shape puzzles reject another spell in the same family; chapter
  puzzles accept any spell from the chapter.
- `PuzzleForgeTest` also covers each solution shape and the hint for a narrow
  puzzle.
- `PuzzleForgeTest`: solver rule, hints, determinism, target generation, variety.
- `ShippedPuzzlePartsTest`: every obstacle has at least two fitting families;
  2000 seeds give 200+ place/prompt combinations.
- `GrimoireSpreadsTest`: cover/index order, spread pairing, phone pairs.
- `ProgressStoreSpellQuestTest`: met pages and chapter are per script and survive
  export/import; duplicate marks do not create a second page.
- `SpellQuestStressTest`: 50 chapters per script, six scenes each (1,200
  puzzles); every scene's own spell solves it, every shape and chapter appears,
  no chapter repeats a spell, family puzzles accept every fitting family,
  chapter puzzles accept another spell from the chapter, exact-shape puzzles
  reject another spell from the family, and every non-solving family gets a
  hint.
- `SpellQuestLogicTest`: scroll word, result family, definition gloss, timing,
  emoji, tip text.
- UIAutomator `spell-quest` checks the gate, a gift from the grimoire, the
  result, a real draw/cast, the cast overlay's labels, the brush tray, the
  meaning reveal, and the wrong-answer path. It draws 水's real guide strokes,
  not a synthetic plus.
- `spell-quest-ja`, `spell-quest-ko`, and `spell-quest-traditional` run the same
  flow in the other scripts; `spell-entry-ja`/`-ko` check the entry points.
- `spell-onboarding` starts a first run, reaches the extra-practice card, starts,
  and opens the quest from the top Home link.
- `spell-flow` checks the gate copy, every draw-tool label, one-way clues,
  one-layer system back, and the grimoire chapters;
  all 10 grimoire chapter names, and a clean leave on phone, tablet, and fold.
- `spell-quest-dark` and `spell-quest-calm` cover theme and reduce motion.
- `scripts/journey.sh --spell` runs the whole spell suite on phone, tablet, and
  fold-inner.

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
  documented in `docs/1.1.1-release-checklist.md`.

## Next tasks

1. Finish the verification pass on phone, tablet, fold-inner, and fold-outer for
   all four scripts; inspect the new 10-chapter grimoire, the wider circle, and
   the effect frames.
2. Dark theme and large text on the gate, grimoire, scene, hint, and cast
   overlay.
3. Reduce motion: effects skip, strokes still play, the scene does not drift.
4. Re-run the standard matrix so the Home/Study tools entry, shared spell cache,
   and new data do not regress other screens.
5. Signed 1.1.1 (22) APK and AAB are built; the APK is hosted and the landing hash is updated. Play upload waits for 1.0.9 review.
6. The daily word is a shared date slot (same study day + course = same word; no install seed), and the puzzle forge now pairs obstacles only with place kinds that fit. The compatible parts still yield 19,607 places, 520,488 obstacle-place pairs, and about 29 million scene/style/solution combinations.

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
- Quest progress belongs in the progress DB, never SharedPreferences: the app's
  export/import/sync only carries the DB. Schema changes are idempotent and need
  a `ProgressStore` test.
- Korean/Japanese Han spells need the Han recognizer. Passing no target uses the
  Hangul/kana model and misreads Han characters.
- The quest overlay can sit outside a CompositionLocal provider. Pass the
  resolved `dark` flag into `SpellQuestScreen`/`GrimoireScreen` as well as the
  progress store; do not assume `LocalBihuaPalette` or `LocalProgressStore`
  reaches it.
