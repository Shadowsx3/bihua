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

## Pull requests

- Keep changes focused.
- Add or update unit tests for logic changes.
- Include before/after screenshots for visible UI changes.
- Run `make check` and `make lint`.
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
