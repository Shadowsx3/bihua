# Changelog

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
