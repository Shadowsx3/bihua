# Content QA backlog

Goal: every character, word, reading, meaning, story, and label a learner sees
is correct, in the right script, and free of pipeline artifacts.

## Character data

- [x] Audit every row in `character`: script, character, level, reading,
      meaning, stroke count, and writing flag.
- [x] Confirm kana, Hangul, Hanja, Simplified, and Traditional rows are in the
      right script and cannot leak into the wrong course.
- [ ] Check stroke-guide coverage and confirm every missing guide has a clear
      in-app message.
- [ ] Check variants and shared glyph groups. Confirm one writing memory is
      shared only where the shapes really match.
- [ ] Check level labels and ordering for HSK, Jōyō, Hanja, Hangul, and kana.

## Readings and meanings

- [ ] Check every reading for tone marks, romaji, kana, romaja, and Hangul.
- [ ] Check every meaning for empty strings, English artifacts, duplicated
      punctuation, and unrelated glosses.
- [ ] Review the longest and most ambiguous meanings by hand.
- [ ] Check Japanese on and kun readings. Keep the primary reading for cues.
- [ ] Check Korean Hanja readings and Hangul romanization.

## Words and common-word cards

- [x] Confirm every `word_character` link points to the right character.
- [x] Check word readings and meanings for truncation and wrong-script text.
- [x] Add Korean word examples from the Korean Wiktionary extract, with Hanja
      spellings, Hangul readings, and full character coverage for 1,684 of
      1,799 Hanja. The remaining 115 rare characters have no safe compound.
- [x] Add a commonness signal and rank Korean examples with FrequencyWords.
      Japanese still needs a real corpus frequency list beyond JMdict priority.
- [ ] Review the words chosen for the Meet "Found in words" cards. Remove
      obscure compounds, proper nouns, and misleading homographs.
- [x] Keep explicit words out of the default experience. Word glosses are
      scrubbed sense by sense; words with only explicit senses are tagged
      `mature` and hidden until the learner turns on Mature words.

## Stories and etymology

- [ ] Review all `char_meta` etymologies for accuracy, typos, and offensive or
      misleading claims.
- [ ] Check that the story shown in the Hint popup is the same one used in the
      character detail.
- [ ] Mark uncertain etymologies or move them to a neutral "shape story" label.

## Pipeline and tests

- [x] Extend `tools/audit_content.py` with checks for empty fields, wrong
      scripts, duplicate rows, broken links, missing readings, Korean coverage,
      and explicit default glosses.
- [x] Add `ContentDatabaseTest` so the shipped database is checked in the unit
      suite, not only by the Python audit.
- [ ] Add a coverage report by script and level.
- [ ] Add a sample review sheet for the top 500 characters and words in each
      course.
- [ ] Re-run the large-history import and FSRS fit after content changes.
- [ ] Check licences and attribution after any new source is added.
