# Contributing to Bihua

Bihua is a small, opinionated Android project. Contributions are welcome when
they make the handwriting loop clearer, the data safer, or the content more
accurate.

## Before you start

- Read the [method](docs/method.html). It explains why the app asks for writing
  before recognition and why it avoids streaks.
- For a large change, open an issue first. Bihua deliberately avoids becoming a
  general-purpose flashcard platform.
- Do not add a new cloud service, account system, analytics SDK, or advertising
  dependency.

## Development

Requirements: JDK 17, Android SDK 36, and an Android 10+ ARM64 device for the
on-device checks.

```bash
source scripts/env.sh
make check          # compile, unit tests, content audit, debug APK, verification
make lint           # Android lint
make smoke          # optional: launch/navigation smoke test on a connected device
```

The content database is generated. Do not edit `bihua_content.db` by hand.
Change `tools/import/build_content_db.py`, rebuild, bump `contentVersion` in
`ContentRepository.kt`, and run `make audit`.

The spell quest's authored parts live in `tools/spells/parts.sql`; the spell
catalog lives in `tools/spells/spell_catalog.py`. After a content change run
`python3 tools/spells/build_parts.py` (or `build_spellbook.py`) and then
`make check`. Every obstacle needs at least two fitting families, at least eight
compatible place kinds, a place-kind row, and a success line. Every setting needs
at least one place kind. Do not hand-edit `puzzle_parts.db` or `spellbook.csv`.

The daily word is a shared calendar slot: seed it from the study day and course
only. Never use install time, progress, or the mature-content setting.

For a visible UI change, run the matching journey and attach the screenshot or
the failing `result.json`/`failed.png`. The harness needs local emulators; see
`qa/README.md`. Journeys are not a CI substitute for `make check`.

## Pull requests

- Keep changes focused.
- Add or update unit tests for logic changes.
- Include before/after screenshots for visible UI changes.
- Run `make check`, `make lint`, and the matching journey for a UI or flow change.
- Update the README, changelog, test matrix, and release checklist when the
  version, counts, or user flow changes.
- Do not commit keystores, `keystore.properties`, personal backups, notes, raw
  datasets, or device screenshots.

## Content and licensing

The app code is MIT-licensed. Bundled content keeps its upstream terms. If you
add a dataset, update `tools/data/manifest.json`, add the licence text under
`licenses/`, update `THIRD_PARTY_NOTICES.md`, and copy the notice into
`app/src/main/assets/licenses/`.

## Conduct

Be direct, kind, and specific. Critique the work, not the person. Harassment and
personal attacks are not welcome.
