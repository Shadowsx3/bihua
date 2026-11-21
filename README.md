# Bihua 筆畫

Bihua is an offline, handwriting-first Android app for learning to write and
read **Simplified Chinese, Traditional Chinese, Japanese, and Korean Hanja**.

The hand is the centre of the app: every character is drawn, every review is
scheduled with FSRS, and writing and pronunciation are practised as separate
tasks. There is no account, no analytics, and no cloud dependency.

## Current build

- Version **1.0.1** (versionCode 3)
- Android 10+ (`minSdk 29`), arm64
- Content version **19**
- Public source repository and landing page.

## Features

- **Four script-pure courses.** Simplified 3,180, Traditional 3,163,
  Japanese 2,286, Korean 2,239 characters. Shared glyphs appear in both Chinese
  courses; simplified-only forms do not appear in Traditional, and vice versa.
- **FSRS-7 scheduling** per script, with separate writing and pronunciation
  tasks.
- **Stroke feedback** for order, shape, missing/extra strokes, reversals, and an
  isolated correction loop.
- **Guided writing** that fades from full strokes to outline to hidden.
- **Transfer practice:** word writing, paper checks, and stroke-order rules.
- **Dictionary depth:** meanings, readings, level, strokes, frequency, variants,
  radical, components, etymology, related families, words, notes, history.
- **Dark mode** with a paper-and-forest palette.
- **Local data ownership:** full zip backup/import, CSV export, optional local
  Wi-Fi sync, and no account.
- **Optional local assistant** on capable devices; hidden by default, no phone
  inference.

## Screenshots

See [`docs/assets/`](docs/assets/) or the public page for current screenshots.

## Build

Requirements: JDK 17, Android SDK 36, and a connected device for device checks.

```bash
source scripts/env.sh
make check      # compile, unit tests, content audit, debug APK, verification
make run        # check, install, and launch on the selected device
make release    # minified release APK (uses keystore.properties when present)
```

Content is rebuilt from the immutable manifest:

```bash
tools/fetch-content.sh
python3 tools/import/build_content_db.py
```

Raw datasets are not committed; `tools/data/manifest.json` records every source,
checksum, licence, and count.

## Privacy

Bihua does not create an account or upload learner data. Progress, notes, and the
study schedule stay on the device. Network access is limited to:

- optional on-device handwriting model downloads;
- optional local-assistant model downloads;
- optional local Wi-Fi sync between two devices.

There is no analytics or advertising SDK. See [`PRIVACY.md`](PRIVACY.md).

## Licences and attribution

The application code is MIT-licensed; see [`LICENSE`](LICENSE).

The bundled content database is a compilation of open datasets. CC BY-SA
portions are distributed under CC BY-SA 4.0; Make Me a Hanzi graphics remain
under the Arphic Public License; the Make Me a Hanzi dictionary remains under
LGPL-3.0-or-later; Unihan remains under the Unicode License V3.

Full notices are in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) and
[`licenses/`](licenses/), and are bundled in the app under
**Settings → About → Licences and sources**.

Hanjaro data is **not** included. Its site grants no redistribution rights, so
the Korean course is built from Unihan only and Korean word examples are not
bundled.

## Repository layout

- `app/` — Android app
- `tools/import/` — content importer and audit
- `tools/data/manifest.json` — source manifest
- `licenses/` — full third-party licence texts
- `docs/` — public landing page and methodology
- `.opencode/plan/bihua/` — local design and implementation notes (not tracked)
