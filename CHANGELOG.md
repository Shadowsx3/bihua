# Changelog

## 1.0.4 (2026-11-22)

### Added

- Korean word examples from the Korean Wiktionary extract, with Hanja
  spellings, Hangul readings, and safer English glosses. 11,266 examples cover
  1,684 of the 1,799 Hanja in the course.
- Korean pronunciation accepts Hangul or romaja and previews the composed Hangul
  while the learner types.
- A Mature words setting, off by default, for explicit dictionary examples. It
  is confirmed once and filters review cues, character cards, word search, and
  recent practice.
- Common-word flags and FrequencyWords ranking for Japanese and Korean examples.
- A shipped-content test, import validation, and a user-journey risk audit.

### Changed

- Version 1.0.4, versionCode 14.
- The review cue defaults to meaning + reading, with a compact Hint popup.
- Japanese word cues show ruby and mask the character to write.
- The writing grid gets more room on small phones; the tablet Home and Meet
  layouts are balanced for their screens.
- Settings content is capped to a readable width on tablets.
- Word glosses are tidied: no `X|Y` pairs, bracketed notes, braces, dangling
  parentheses, or truncated dictionary apparatus.
- 587 structure rows without a source story now use an honest “Shape” note built
  from the decomposition and radical.

### Fixed

- Korean word cues align the highlighted syllable when the word uses a sound
  change.
- Japanese word cues mask every occurrence of the target character.
- Known list import reads supplementary Han characters and reports unreadable
  files.
- Empty review forecasts no longer list fourteen zero rows.
- A corrupt content database shows a retry screen instead of an endless loader.
- Importing a newer or foreign progress database is rejected before it replaces
  local progress.
- Reset all progress clears word practice history as well.
- FSRS handles a corrupt zero-stability card without NaN intervals.
- Onboarding back navigation and rotation no longer lose the current page.

## 1.0.3 (2026-11-22)

### Added

- FSRS-7 weights can be imported, exported, or fitted locally from the
  learner's own reviews. Each script can keep its own profile; shared glyphs
  use the shared profile. A fit needs 400 reviews, uses the recent 2,000 by
  default or all reviews if chosen, and keeps the defaults when a time-based
  holdout gets worse.
- The study day can start at midnight or up to 6 AM. The daily goal, calendar,
  review counts, skipped characters, and XP follow that boundary. The app
  refreshes the boundary after being open overnight or resumed days later.
- "Skip always" suspends a character until it is resumed. The one-day skip and
  the permanent suspension are separate lists in Study tools.
- Backups, imports, and local sync can move settings and progress together or
  separately. The receiving device chooses what to apply.
- Slow actions show a blocking progress dialog or an inline progress bar:
  session start, backup restore, sync apply, model download, reset, weight fit,
  backup export, and review CSV export.
- `tools/seed_fsrs_fit.py` creates a large, realistic review history for
  testing the fit across per-script and shared profiles.

### Changed

- Version 1.0.3, versionCode 13.
- The Schedule settings page is now compact: profile menu, status, fit, import,
  export, and a More menu. The long explanation moved to an info dialog.
- Practice and review share one aligned ink toolbar. Undo, redo, and clear sit
  on the left; play, reverse, previous stroke, stroke count, and next stroke sit
  on the right. Practice now has the same stroke playback controls as review.
- After a review is submitted, the canvas is locked. The drawing stays visible
  for inspection, the editor tools are disabled, playback still works, and the
  grade bar's Draw again is the only way to start another attempt. No button is
  placed over the drawing.
- Settings → Study can show only Again and Good. Hard and Easy are hidden from
  the grade bar and pronunciation check; scheduling is unchanged.
- Removed `enableEdgeToEdge()` and the deprecated status-bar, navigation-bar,
  and cutout-mode calls it pulls into the app. Android 15+ still uses the
  platform's edge-to-edge default, and system-bar padding is applied only when
  the window draws behind the bars. Android 10–14 keep their normal inset
  window, and bar icon colors are only overridden on Android 15+.

## 1.0.2 — 2026-11-21
 — 2026-11-21

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
- The bottom navigation keeps fixed positions and shows every tab label, so
  the icons no longer move when the selected tab changes.
- Play screenshots were regenerated from the real 1.0.2 app in light mode, with
  a grade-sheet shot and a Progress shot added.
- Tablet screenshots were recaptured from a 2560 × 1600 Pixel Tablet emulator
  for both the 7-inch and 10-inch slots.
- Duplicate variant rows in character details are hidden.
- The store copy now says “Korean: Hangul and Hanja” and discloses Google ML Kit
  diagnostics, matching the Data Safety declaration.
- CI runs unit tests, content audit, and lint only. It no longer assembles or
  uploads an APK; the APK size check is portable across macOS and Linux.
- The Makefile now uses `/bin/bash` for its recipes. `scripts/env.sh` uses bash
  syntax, so `make test`, `make lint`, and `make apk` failed under Ubuntu's
  `/bin/sh` in CI.
- The landing page uses current 1.0.2 light-theme screenshots and a crisp
  ink-and-paper layout. The blurred forest wash, drifting leaves, fireflies,
  and glass cards are gone.
- The landing page has a live ink pad, a four-step recognition diagram, a
  retention graph, an interval ladder, and an FSRS-7 versus FSRS-6 comparison.
  The hero now says “Learn characters by writing them.”
- The landing page, README, method page, and store copy mention stylus pressure.
  A finger or mouse still works.
- The Korean course is described as Hangul and Hanja throughout the site and
  README, matching the app and the store listing.

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
- Korean Hanja now comes from Unihan.
- Korean word examples come from the Korean Wiktionary extract, ranked by
  FrequencyWords. Explicit examples are tagged and hidden by default.
- The public repository and site are now the primary distribution point.

### Fixed

- Word practice records the active script, not a stale session script.
- Character meanings drop dictionary references, classifier lists, and
  Taiwan-pronunciation fragments.
- Japanese KANJIDIC radical-number notes and Korean hexagram notes are cleaned.

## 1.0.0 — 2026-11-21

First signed private beta: four scripts, FSRS-7, stroke feedback, dictionary,
dark mode, transfer practice, and local backup.
