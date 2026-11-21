# Changelog

## 1.0.2 — 2026-11-21

### Changed

- The four grade buttons now sit in a fixed bar after every writing check, with
  the answer, reading, and meaning beside them. Checking a drawing no longer
  leaves the grade choices below the fold.
- The lesson flow is named Meet, Trace, Write. The first run explains the three
  steps and says that the learner chooses the grade.
- A gentler start inside each HSK/Jōyō/geup level: simple shapes such as 一, 二,
  and 三 come first, while the level order stays intact.
- All settings sections are visible at once instead of hiding Display, Data,
  Sync, and About in a horizontally scrolling row.
- Home says “No reviews due” when new characters are still available.
- Handwriting checks prefer a target character found in the top three readings,
  so a recognisable 一 is not labelled different because of one stray guess.
- Practice, word practice, and paper check explain that they stay outside the
  review schedule.
- “Leech” is now “tricky” in the interface.
- Word rows have a visible Write action; the character detail no longer uses
  SRS/model jargon.
- Stylus and finger drawing controls fit above the canvas on a phone.

### Fixed

- The trace and write screens no longer hide the guide and ink tools behind the
  sticky action bar.
- “1 strokes” and similar plural forms are corrected.
- Redrawing a card keeps the same cue instead of changing the question.

## 1.0.1 — 2026-11-21

### Added

- Session resume per script: after process death, Home offers Continue, Resume, or Discard for the current course.
- Word practice history with a Recently practised list and one-tap repeat.
- Full third-party notices and licence texts inside the app.
- Public landing page, methodology page, CI, and release packaging.
- On-device launch/navigation smoke test (`make smoke`).

### Changed

- The optional local AI assistant and LiteRT-LM runtime were removed. The APK is now 38 MB instead of 61 MB; the AAB is 20 MB.
- The app code is now MIT-licensed; content keeps its upstream terms.
- Korean Hanja now comes from Unihan. Hanjaro data was removed because its site
  grants no redistribution rights.
- Korean word examples were removed rather than shipping unlicensed content.
- The public repository and site are now the primary distribution point.

### Fixed

- Word practice records the active script, not a stale session script.
- Character meanings drop dictionary references, classifier lists, and
  Taiwan-pronunciation fragments.
- Japanese KANJIDIC radical-number notes and Korean hexagram notes are cleaned.

## 1.0.0 — 2026-11-21

First signed private beta: four scripts, FSRS-7, stroke feedback, dictionary,
dark mode, transfer practice, and local backup.
