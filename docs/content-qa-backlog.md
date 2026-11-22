# Content QA backlog

Goal: every character, word, reading, meaning, story, and label a learner sees
is correct, in the right script, and free of pipeline artifacts.

## Character data

- [ ] Audit every row in `character`: script, character, level, reading,
      meaning, stroke count, and writing flag.
- [ ] Confirm kana, Hangul, Hanja, Simplified, and Traditional rows are in the
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

- [ ] Confirm every `word_character` link points to the right character.
- [ ] Check word readings and meanings for truncation and wrong-script text.
- [ ] Review the words chosen for the Meet "Found in words" cards. Remove
      obscure compounds, proper nouns, and misleading examples.
- [ ] Add a frequency or commonness signal for Japanese and Korean words. The
      current Chinese ordering uses HSK level and word length, which is a good
      proxy. Japanese has no level data and can surface rare compounds.
- [ ] Decide whether Korean should get a Hangul word list at all.

## Stories and etymology

- [ ] Review all `char_meta` etymologies for accuracy, typos, and offensive or
      misleading claims.
- [ ] Check that the story shown in the Hint popup is the same one used in the
      character detail.
- [ ] Mark uncertain etymologies or move them to a neutral "shape story" label.

## Pipeline and tests

- [ ] Extend `tools/audit_content.py` with checks for empty fields, wrong
      scripts, duplicate rows, broken links, and missing readings.
- [ ] Add a coverage report by script and level.
- [ ] Add a sample review sheet for the top 500 characters and words in each
      course.
- [ ] Re-run the large-history import and FSRS fit after content changes.
- [ ] Check licences and attribution after any new source is added.
