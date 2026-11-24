# Learning and joy proposals

Status: living document. It records what the app already does, what the learning
science supports, and what to build next. Nothing here overrides the product
rules in `AGENTS.md`: local data only, no account, no developer analytics, and
the learner always keeps the grade.

## 1. What the evidence actually says

These are signals, not laws. Several are small studies or have mixed results, so
the app should treat them as hypotheses to test against its own learners, not as
slogans.

- **Retrieval beats rereading.** Producing an answer from memory improves later
  recall more than reviewing it again, especially when practice is spaced. A
  2026 classroom meta-analysis reports an average retrieval-practice effect of
  about *g* = 0.37. ([meta-analysis](https://link.springer.com/article/10.1007/s10648-026-10225-5))
- **Handwriting and typing train different things.** Handwriting tends to help
  form and visuospatial/motor memory; typing tends to help sound and
  sound-to-spelling mapping. A synthetic review of Chinese learners finds mixed
  writing outcomes and advantages for typing on some recognition tasks, so the
  honest answer is "both, for different jobs."
  ([review](https://doi.org/10.1016/j.ijer.2021.101740),
  [modality study](https://pmc.ncbi.nlm.nih.gov/articles/PMC13412517/))
- **Stroke order is not decoration.** Correct stroke order is associated with
  better form recognition and writing, while violating it can interfere with
  orthographic processing. But the effect depends on the learner and the task.
  ([stroke-order study](https://doi.org/10.1016/j.chb.2017.10.013),
  [review](https://doi.org/10.1016/j.langsci.2024.101681))
- **Passive stroke animations are not automatically good.** At least one study
  found stroke-order animations could *reduce* recognition accuracy for some L2
  learners, while active tracing and generation helped. Animations should be a
  tool the learner asks for, not the default show.
  ([interference study](https://pmc.ncbi.nlm.nih.gov/articles/PMC9403612/))
- **Desirable difficulty is real, but fragile.** Effortful retrieval and varied
  practice can improve long-term retention while feeling worse in the moment.
  Too much difficulty, or difficulty without support, can demotivate beginners.
  ([Bjork & Bjork](https://www.unh.edu/teaching-learning-resource-hub/sites/default/files/media/2023-06/itow-introducing-desirable-difficulties-into-practice-and-instruction-bjork-and-bjork.pdf),
  [review](https://pmc.ncbi.nlm.nih.gov/articles/PMC10858853/))
- **Immediate, specific feedback usually beats vague delayed feedback** for form
  errors, but the learner still needs a moment to commit to an answer first.
  ([timing review](https://pmc.ncbi.nlm.nih.gov/articles/PMC9995700/))
- **Interleaving helps confusable material**, particularly when the learner can
  compare the confusable items. It is not a reason to mix everything at random.
  ([interleaving study](https://link.springer.com/article/10.1186/s41235-025-00680-z),
  [confusion discussion](https://doi.org/10.1080/09571736.2021.1915365))
- **Joy is not sugar on top.** Self-determination theory predicts that
  autonomy, competence, and relatedness sustain intrinsic motivation; controlling
  rewards and pressure can undermine it. Gamification studies are mixed and
  often show novelty effects fading, so celebrate progress without holding it
  hostage. ([SDT and gamification review](https://www.ntnu.edu/documents/139799/1279149990/04+Article+Final_camildah_fors%C3%B8k_2017-12-06-13-53-55_TPD4505.Camilla.Dahlstr%C3%B8m.pdf),
  [critical position](https://papers.ssrn.com/sol3/Delivery.cfm?abstractid=5403850))

## 2. What Bihua already gets right

- Character-first scheduling with production before recognition.
- FSRS-7 spacing, learner-chosen grades, no forced pass/fail.
- Introduce → trace → recall, with a guide that fades.
- Separate writing and pronunciation tasks.
- Local-only progress, no account, no ads, no telemetry.
- A dictionary that is a real reference, not a second review queue.
- Words are context, not scheduled cards, so the app stays about characters.
- The learner can turn the guide on in recall if they want support. It is off by
  default.

## 3. Completed in this pass

- **Answer privacy for pronunciation.** Kana and Hangul meanings often spell the
  reading ("hiragana for “a”", "syllable ga"). Those readings are now masked in
  pronunciation reviews. Unit tests cover kana, Hangul, Chinese, one-letter
  readings, tone marks, and multiple readings.
- **Writing recall privacy check.** The journey harness now fails if the target
  character is visible during a "From memory" step.
- **Stroke feedback that does not invent errors.**
  - Matching is order-aware and shape-aware, so a correct stroke is not paired
    with a neighbour.
  - Reversal needs both ends swapped, an opposing direction, *and* a backwards
    path; short and U-shaped strokes are never guessed.
  - Out-of-order copy now says "belongs earlier", which is always the true
    direction of the repair. It used to say "later".
  - The grader compares ink with the guide the learner actually sees (the same
    6% inset as the visible guide), not an invisible unscaled original.
  - Shape evidence now measures both precision and coverage, so ink stacked on
    one stroke cannot look like a complete character.
  - The repair pad requires the right count, shape, and direction before saying
    the single stroke is good.
- **Word practice guides.** Off, faint, and real numbered strokes, with stroke
  count, reading, meaning, and playback. Fixed the missing content/pinyin
  providers on the full-screen word-practice and drill routes, which had been
  silently preventing stroke guides from loading there.
- **Brush guides that read like a character.** Tapered stroke polygons, a paper
  halo between overlapping marks, the writing grid behind the guide, and small
  numbered start badges. Checked on 一, 学, 谢, and 爱.
- **Printable practice that prints cleanly.** The grid border no longer fills
  each box, model and trace cells have distinct printable tints, the first
  action is Export PDF, and the cover previews the most complex character.
- **Dictionary.** A real full-screen lookup with debounced search, recent words,
  detail view, per-character reading/meaning/stroke count, audio, and a direct
  path into word practice.
- **Print studio.** A4 PDF export from course order, study list, recently
  learned, recent mistakes, and confusions. Trace, trace-twice, memory, and
  alternate templates; Mi zi ge, Tian zi ge, and plain grids; numbered models,
  pale tracing cells, cover page, and a sheet contents list.
- **Per-stroke coaching closure.** The coach card highlights the exact repair
  stroke, offers Show me / Trace it / Write alone, and returns to the whole
  character with the fixed stroke in mint.
- **Word practice that keeps the word in view.** Show answer reveals one
  character at a time; completed characters stay visible while the next one is
  written. The guide-mode row keeps its play slot, and playback reveals the
  final filled outline instead of swapping styles.
- **A daily word that feels chosen.** The Word of the Day is non-mature, short,
  and filtered; a small curated set leads the cycle, followed by a large
  deterministic common-word pool. Korean shows the Hangul word with the Hanja
  beside it.
- **A desk on the home screen.** The Today and Word of the Day widgets use the
  pet and change with the hour, due count, new lessons, vacation, and all-clear
  state. They are reminders, not debts.
- **Practice that teaches, not just counts.** Free practice now gets the same
  handwriting check and per-stroke coach as a review, with a warm palette:
  butter for a waiting stroke, sky blue for a different reading, mint for a clean
  repair.
## Meaning, without a second debt

Writing from a meaning cue proves that the meaning can drive production. It does
not prove the reverse: seeing 愛 and producing "love". That direction is worth
teaching, but a second scheduled card for every character would double the debt
and weaken the writing-first loop.

Options considered:

1. **A separate meaning card per character.** Honest measurement, too much debt,
   and it competes with writing for the same daily minutes.
2. **Reverse review (character to meaning).** Useful, but it is a different task
   and should not silently replace a writing review.
3. **One meaning check after some writings.** Show the character and three
   meanings, or a short word context with a gap. One tap, no typing. It can run
   every third review, after a lapse, or on a new character.
4. **Meaning in word practice.** Use the example words already in the dictionary:
   choose the word that matches a meaning, or choose the meaning of a word in a
   sentence. Free practice, no schedule.

Recommendation: start with option 3 as an **optional, low-frequency check** and
option 4 in word practice. Record the result separately (correct / unsure /
wrong), do not create a new FSRS card, and use it only to choose better cues and
confusions. If it proves useful, give it its own small schedule later. Keep the
writing review first; meaning should not become a second full deck.

### 1.1.0 implementation note

The first pass chose option 3, but made it **self-graded instead of
multiple-choice**. English senses have too many fair paraphrases for a reliable
auto-check. The meaning review asks the learner to type a wording if they want,
then reveals a sanitized gloss, one or two real example words, and the learner's
own synonyms. It is optional, off by default, and never appears on a new card.
Writing stays the default and at least half of the reviews when meaning and
pronunciation are both on. Synonyms are stored per character in settings, capped,
deduplicated, and cleaned again on restore. Dictionary redirects and blank
glosses are filtered before they can become a review.


- **A shelf beside the schedule.** Study lists let a learner collect characters
  and words without creating review debt. Presets make a shelf in one tap, a
  streamed import respects an existing list, and a word queue turns a list into
  a short free-practice session. The schedule stays the schedule; the shelf is
  for what the learner wants to revisit on their own terms.
- **Kana that reads like kana.** Hiragana and katakana use a round, even pen
  instead of the Han brush polygon, so long curves no longer pinch.
- **Collapsible character sections.** The 3,100-entry grid groups by level with
  counts and collapse/expand; search returns a flat list.

## 4. Proposed work, ordered by learning value per unit of risk

| # | Proposal | Why it matters | Effort | Risk |
|---|---|---|---|---|
| 1 | Answer-privacy invariants for every recall surface | A leak makes the review worthless and teaches the learner to distrust the app | S | Low |
| 2 | Per-stroke coaching with an optional "show me" loop | Turns a score into one concrete next action | M | Low |
| 3 | Confusable-set practice and compare | Visual/radical confusion is a main bottleneck for character learners | M | Medium |
| 4 | Cue ladder: meaning → reading → word → audio → blank | Gradually removes support and builds flexible access | M | Low |
| 5 | Pronunciation studio: tones, minimal pairs, record-and-compare | Typing a reading is recognition; production needs pitch and mouth feedback | L | Medium |
| 6 | Handwriting-to-typing transfer tasks | Typing helps sound mapping; writing helps form. Use each deliberately | M | Low |
| 7 | Calibration and confidence before reveal | Learners misjudge what they know; a gentle prediction improves study decisions | S | Low |
| 8 | Rich "recently learned" and "mistake" sheets, plus family sheets | Print is where handwriting transfers to paper | S | Low |
| 9 | Dictionary as graded reading: example sentences and audio | Comprehensible input at i+1 makes the dictionary useful beyond lookup | L | Medium |
| 10 | Joy system: mascot moments, paper rituals, seasonal ink, no streaks | Sustains autonomy, competence, and relatedness without coercion | M | Low |

### 4.1 Answer-privacy invariants (highest priority)

Every recall surface must pass these rules before shipping:

- The writing recall screen never renders the target character, a word containing
  it, or a stroke guide unless the learner explicitly asks for help.
- The pronunciation screen never renders the expected reading, including inside
  a meaning string, an example, a tooltip, or an accessibility label.
- A hint, mnemonic, or audio button is either clearly labelled as a hint or
  absent until the learner commits to an answer.
- Recognition results are not computed or shown before the attempt.
- The grade buttons do not appear before the answer is checked or revealed.

Implementation: keep the pure `ReadingPrivacy` masker and the journey assertion.
Add a debug-only "privacy audit" that dumps the semantics tree before and after
reveal and checks the target against every text node. Add the same check for
every new cue type.

### 4.2 Per-stroke coaching (extend what is there)

The current coach is a good first step. Make it teach:

- Highlight the exact stroke on the character, not only a numbered dot.
- Offer three actions on a problem stroke: **Show me**, **Trace it**, **Write it
  alone**. The repair pad already exists; add the highlighted model inside it.
- Keep one repair at a time. Never show a wall of red.
- Distinguish "direction", "order", "missing", and "extra" with color and a
  short sentence, not a grade.
- After a successful repair, return to the whole character with the fixed stroke
  marked in mint. This is the "closure" moment that makes the loop feel good.
- Track the same error twice in a row and offer the confusable family or a
  targeted drill. Do not silently reschedule as a failure.

Copy rules: "belongs earlier", "wants to turn", "is waiting", "a little extra
ink". Never "wrong", "failed", or "bad".

### 4.3 Confusable sets and interleaving

- Build visual families from radical, decomposition, and shared components
  (already in `char_meta` and `CharacterFamily`).
- After a character is introduced, schedule a short **contrast** card that asks
  the learner to choose or write one of two confusable characters from a shared
  cue. This is not a new grade; it is a practice drill.
- Show the pair side by side in Compare with the differing strokes highlighted.
- Interleave only after initial acquisition. Do not mix brand-new characters
  with their lookalikes on day one.
- Use the existing `topConfusions` data to pick the set. The worksheet builder
  can already print it; add a one-tap "practise this confusion" action.

### 4.4 The cue ladder

Today the learner can choose meaning, meaning + reading, reading, word, or a
mixed cue. Make the ladder explicit and progressive:

1. **Meaning + reading** (default for new cards).
2. **Meaning only.**
3. **Reading only.**
4. **Word context** (reading and meaning, target masked).
5. **Audio only** (hear a word or character, produce it).
6. **Blank** (free recall: write a character you learned this week).

Promotion should be based on successful retrievals and learner choice, not a
punitive streak. The ladder should be visible in Settings as "Cue support" with
plain descriptions. Keep the current mixed mode for people who like variety.

Audio-only writing is the boldest step. It trains sound-to-form mapping, which
is exactly what handwriting plus pronunciation should build. It needs a safe
fallback: after 10 seconds, offer the meaning; after another 10, offer the
reading; the target character only after an explicit reveal.

### 4.5 Pronunciation studio

Typing pinyin/romaji is useful recognition, but it is not pronunciation. Add:

- **Tone contour practice**: show a tone line, play a native/near-native model,
  and let the learner record. Compare pitch contour, not exact waveform. Start
  with tone pairs that are easy to confuse (2–3, 1–4).
- **Minimal pairs**: 妈/麻/马/骂, す/つ, び/ぴ, same vowel length, etc. Ask the
  learner to hear the difference before producing it.
- **Record-and-compare**: local only, no upload. Playback is the feedback; the
  app can show a rough contour and a confidence band, never a grade.
- **TTS is a model, not a judge.** Use it for model audio and for reading text
  aloud, but do not grade pronunciation from TTS.
- **No answer leak**: the model audio for a pronunciation task should only be
  available after the attempt or behind a "Hear the model" hint.

### 4.6 Handwriting and typing as separate jobs

Keep writing as the scheduled production task. Add optional transfer tasks:

- **Type the word** after writing it, especially in Japanese and Korean, to
  strengthen sound-to-spelling mapping.
- **Dictation**: hear a word, type it, then write the target character. This is
  a high-value bridge between pronunciation and handwriting.
- **Read the word aloud** from the dictionary, not as a graded card.

Do not replace handwriting with typing. The evidence supports using both for
different sub-skills; a handwriting-first app should not quietly become a typing
app.

### 4.7 Calibration without punishment

- Before revealing, ask "How sure are you?" with three soft buttons (guess,
  maybe, sure).
- After the reveal, show a tiny comparison: "You were sure and it was right" or
  "This one surprised you." No score, no streak.
- Use calibration to adjust the suggested grade, not to override the learner.
- Over time, show a private "calibration" card in Progress: "You tend to be
  overconfident on Traditional characters." This is useful, not shaming.

### 4.8 Print that transfers to paper

The worksheet studio is close. Add:

- **Self-check column**: two tiny hollow circles per character, shape and order.
  The learner ticks them after writing.
- **Family sheet**: one radical or component, its meaning, and four to six
  characters that use it.
- **Confusion sheet**: two similar characters per row, with the differing
  strokes in a second color.
- **Sentence strip**: one short, level-appropriate sentence using the target
  words, with the target characters boxed.
- **Page range and preview**: a real Compose preview of the first page before
  export, plus a page-range selector for larger sets.
- **Ink-friendly mode**: no color required, thicker grid lines, larger boxes.
- **Teacher/parent mode**: name, date, instructions, and a small "what to look
  for" note on the cover.

### 4.9 Dictionary as comprehensible input

The dictionary currently answers "what does this mean?" Make it answer "what can
I read next?"

- Example sentences at the learner's level, with the target word highlighted.
- One-tap "show reading" and "hear it" per sentence.
- Word families and common collocations, not just a flat list.
- "Words with this character" and "characters in this word" cross-links.
- A daily short passage built from known characters plus a small number of new
  ones (i+1), with tap-to-define.
- Keep words out of FSRS. A passage is reading practice, not a review debt.

### 4.10 Joy, without coercion

The app should feel like a favourite desk, not a productivity dashboard.

- **Mascot with personality, not a sticker.** Give the ink drop a name, a few
  idle animations (blink, bounce, peek), and reactions that match the moment:
  curious before a new character, proud after a clean stroke, sleepy when the
  learner is done. Respect reduce-motion.
- **Micro-celebrations**: a small ink bloom, a soft chime, a paper confetti burst
  on a personal best, not on every card. Use them sparingly so they stay special.
- **Warm color stories**: forest green, cream, coral, butter, mint, lilac. Keep
  the writing surface calm; put color around it, not under the ink.
- **Rituals**: "open the desk" on the first card, "close the desk" on the last,
  a daily paper texture, a seasonal accent (plum blossom, bamboo, maple) that
  changes slowly.
- **Autonomy**: let the learner choose the session shape, the cue ladder, the
  brush, the paper, and whether to see a guide. Avoid locking progress behind a
  single path.
- **Competence**: show real evidence of growth (characters written, strokes
  improved, words read) rather than points. The grade is already the learner's.
- **Relatedness**: local, low-pressure options such as a shareable worksheet, a
  family practice sheet, or a "compare with a friend" code. No leaderboard, no
  public profile, no social feed.
- **No streaks, no lives, no guilt notifications.** A missed day is not a debt.

## 5. Measurement plan without surveillance

The app has no developer analytics, and that should stay true. Measure in ways
that respect the learner:

- **Local learning checks**: optional, on-device A/B comparisons of cue ladder
  steps, with results shown only to the learner.
- **Journey harness**: deterministic scenarios for every recall surface,
  including answer-leak checks and stroke-feedback regressions.
- **Unit tests for pure logic**: grading, privacy masking, cue building,
  worksheet layout, and session progression.
- **Self-report**: a single "Did this feel too easy / right / too hard?" after a
  session, stored locally and shown in Progress.
- **No telemetry, no experiment IDs sent anywhere.** If a study is ever run,
  make it explicit, opt-in, and separate from the app.

## 6. Suggested sequence

**Now (next few sessions)**

1. Answer-privacy audit for every cue and hint, plus semantics tests.
2. Per-stroke repair closure: highlight, fix, return, mint mark.
3. Worksheet self-check, family sheet, and page preview.
4. Mascot pass: idle motion, reactions, and a warmer icon/banner.
5. Dictionary: per-character jump and a "practise this word" path from every
   result (already partially there).

**Next**

6. Cue ladder with audio-only as an opt-in step.
7. Confusable-set contrast drills from `topConfusions`.
8. Calibration prompt and private calibration card.
9. Pronunciation studio: tone pairs and record-and-compare.
10. Dictionary example sentences at level.

**Later**

11. i+1 reading passages from known characters.
12. Teacher/parent worksheet mode.
13. Local, opt-in learning experiments.
14. Seasonal paper and ink collections.

## Spellbook (proposal, not started)

A free-practice game in Practice: write a character, and it casts an effect
from its radical family (water, earth, stone, fire, and so on). It never
creates cards, XP, or review-log rows, and it never changes the schedule.

Parts:

- **Spell book (reference):** one page per family. Each page shows the
  family's characters the learner has seen, the plain meaning, and a short
  looping effect preview. Read-only. Reviewing the book is a valid way to
  spend time before casting.
- **Cast:** a large canvas in Practice. The learner writes any character, or a
  word. The handwriting recognizer returns candidates as a hint, never a grade.
  A match plays the family effect; a miss fizzles with "The ink fizzled. Try
  again." No penalty.
- **Grimoire:** the book shows more than the learner knows, so there is always
  something to aim for. Every spell's character is shown, because the grimoire
  is a reference book, not a recall task (the Characters tab already shows the
  same characters). Three tiers per spell:
  - **Known:** the character, meaning, and effect. Lit up when cast.
  - **Seen:** the character and meaning, effect dimmed until first cast.
  - **Locked:** the character dimmed, family name shown, meaning hidden until
    the learner meets it in a lesson. Copy says "still to learn", never
    "missed".
  It reads progress only and writes nothing. Mature words are never shown.
- **Strength:** each cast scales with what the ink did, never with a penalty.
  - Size: the drawn box sets the effect's scale.
  - Stroke count and order: more clean strokes add particles or layers.
  - Trace quality: the existing per-stroke evidence (`StrokeGrade`, coach)
    picks a tier: Faint, Steady, or Bright. Any cast with at least one stroke
    still works; the floor is "Faint", never a fizzle for imperfect strokes.
  - Pace and pressure: optional flourish only, off by default for finger input.
  - An effect never draws the target character, its outline, or its guide. A
    Bright cast can use the family colour and motion, not the glyph.
- **First cast:** a short moment the first time each family is cast.
- **Spell of the day:** picked from learned characters, so it is always
  something the learner can already do.

Content:

- Family table from the radical column in `char_meta` (4,875 characters have
  one). Families are a design choice, not etymology; mark exceptions where a
  radical does not match its element.
- Signature flourishes for about 30 to 50 common characters first. The count
  can grow toward 300 by data, not new code.
- Kana and Hangul get per-script rune effects, since they have no radicals.

Rules:

- No cue that shows the target before the learner writes it. Prompts name the
  family or meaning, never the character, as in any writing task.
- Mature words are never spells. Only the current course counts.
- Effects stay off the writing surface; no motion or colour under the ink.
- Reduce motion and TalkBack get the full text with no animation.
- No timers, lives, or "you missed" copy.

Order: family table and content audit, read-only spell book, cast screen,
then effects and the grimoire.

## 7. Anti-patterns to avoid

- Showing the answer "just a little" during recall.
- Red-pen grading, "failed", or a wall of corrections.
- Streaks, lives, energy, or notifications that imply guilt.
- Passive stroke videos as the main way to learn order.
- Random interleaving of brand-new characters.
- Leaderboards or public profiles.
- Making the dictionary a second queue of due words.
- Treating TTS as a pronunciation judge.
- Confusing immediate performance with long-term learning.

## 8. Launch-asset refresh (do this last)

Status (2026-11-23): 1.0.7 and the refreshed Play listing were submitted for
review with the final build. The landing page now uses cache-busted 1.0.7
captures.

Do not finalise the public landing page or Play Store images until the 1.0.7 UI
is frozen and the release candidate is built. The current icon, mascot, and
splash are the source of truth; any screen change after the final capture means
redoing the whole set.

- Rebuild `docs/assets/*` from the frozen build with a friendly profile (not
  "QA Learner"), light and dark where it matters: Home, Practice with a real
  stroke guide, Dictionary detail, word practice guide, worksheet builder,
  Progress, and a calm recall screen with the answer hidden.
- Update `docs/index.html`, `docs/method.html`, `docs/privacy.html`, the web
  manifest, the favicon/apple-touch icon, and the OG/Twitter preview image.
- Regenerate the Play feature graphic (1024x500), 512 icon, phone, tablet, and
  foldable screenshots, and the listing copy only after the final versionCode
  and release notes are fixed.
- Keep all store material local in `store/` (ignored). Do not commit Play
  Console files to the app repo.
- Verify the round adaptive icon at 48 dp, the circular launcher mask, the
  system splash on Android 12+, and the hand-off into Home on a cold start.

## 9. References

- Retrieval practice meta-analysis (2026): <https://link.springer.com/article/10.1007/s10648-026-10225-5>
- Typing vs handwriting synthetic review: <https://doi.org/10.1016/j.ijer.2021.101740>
- Handwriting plus typewriting modality study: <https://pmc.ncbi.nlm.nih.gov/articles/PMC13412517/>
- Stroke-order learning and handwriting: <https://doi.org/10.1016/j.chb.2017.10.013>
- Stroke-order animation interference: <https://pmc.ncbi.nlm.nih.gov/articles/PMC9403612/>
- Corrective feedback timing: <https://pmc.ncbi.nlm.nih.gov/articles/PMC9995700/>
- Desirable difficulties (Bjork & Bjork): <https://www.unh.edu/teaching-learning-resource-hub/sites/default/files/media/2023-06/itow-introducing-desirable-difficulties-into-practice-and-instruction-bjork-and-bjork.pdf>
- Interleaved spelling practice: <https://link.springer.com/article/10.1186/s41235-025-00680-z>
- Confusion and Chinese character learning: <https://doi.org/10.1080/09571736.2021.1915365>
- SDT and gamification review: <https://www.ntnu.edu/documents/139799/1279149990/04+Article+Final_camildah_fors%C3%B8k_2017-12-06-13-53-55_TPD4505.Camilla.Dahlstr%C3%B8m.pdf>
- Critical position on principled gamification: <https://papers.ssrn.com/sol3/Delivery.cfm?abstractid=5403850>
