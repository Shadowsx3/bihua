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
- Practice uses the full height of the screen, with the result in a compact
  dialog instead of a long scroll.
- The Meet step centres the character and pins Next to the bottom, instead of
  leaving most of the screen empty.
- Home, Characters, Progress, and Settings no longer add bottom padding on top
  of the navigation bar, so more content fits on screen.
- Character detail has top actions for Write a word and Write on paper.
- Home names the active course in the greeting line.
- The privacy policy now states that handwriting strokes are processed on the
  device and explains ML Kit model updates and metrics.
- Handwriting results now say plainly that a reading can miss or misread a
  stroke. It is a hint; the learner still chooses the grade.
- The active handwriting model and the device voice engine are prepared in the
  background during onboarding, on Home, and after a script switch.
- Practice now defaults to the first curriculum character and its picker has
  search by character, meaning, or reading.
- Discarding a paused lesson asks for confirmation.
- Reset, backup import, and local sync clear paused lessons so a stale session
  cannot reappear.
- Character detail puts Practice and word/paper actions before the sharing
  controls.
- Onboarding input copy matches whether the device has a stylus.
- Settings now opens on a searchable home list with grouped rows, then a
  focused subpage with a back arrow.
- The new-lesson pace estimate uses a realistic grade mix, a complete-course
  horizon, and reports average and busy-day review load. At 10 new a day it
  now shows roughly 88 average and 113 busy-day reviews, not 30.
- Pronunciation review has a large result card, your-answer/accepted-answer
  comparison, retry and hear-it actions, and hides the grade until the reading
  is checked.
- A small confetti burst marks a correct reading; session completion keeps its
  burst. Both respect reduce motion.
- The session progress bar is taller, animated, and fills through lesson steps
  and graded reviews.
- Settings About discloses Google ML Kit diagnostics and usage analytics and
  offers Send feedback and Source code actions.
- Study now offers reviews first, mixed review/lesson blocks, or lessons first.
  Mixed sessions can use a configurable interval and a top bar that fills to
  the next lesson instead of the whole session.
- The session progress bar is taller and animated through both lesson steps and
  graded reviews.
- The lesson pace estimate now simulates a realistic Good/Hard/Again mix and a
  completed course. At 10 new a day it reports about 88 average and 113 busy
  reviews, and about 11 months to finish.
- Pronunciation review puts the result in a large feedback card with your
  answer, the accepted reading, retry, hear-it, and a small confetti burst.
- Settings is now a searchable home list of grouped rows with focused
  subpages, instead of category chips or a dropdown.
- Long helper copy on Home, Meet, Trace, Write, word practice, paper check, and
  settings was shortened so the canvas and actions keep the space.
- Home shows the course under the greeting; the hero card carries the counts.
- Emulator and mouse input now work, and emulators are no longer treated as
  stylus-only devices.

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
