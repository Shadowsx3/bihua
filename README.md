# Bihua 筆畫

[![CI](https://github.com/Shadowsx3/bihua/actions/workflows/ci.yml/badge.svg)](https://github.com/Shadowsx3/bihua/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/code%20license-MIT-2f7d55.svg)](LICENSE)
[![Android 10+](https://img.shields.io/badge/Android-10%2B-3ddc84.svg)](#requirements)
[![Version](https://img.shields.io/badge/version-1.0.2-e89a6b.svg)](CHANGELOG.md)

**An offline, handwriting-first Android app for learning to write and read
Simplified Chinese, Traditional Chinese, Japanese, and Korean Hanja.**

The hand is the interface. Bihua asks the learner to produce a character from a
cue, fades the reference guide over time, and schedules the next review with
FSRS-7. Writing and pronunciation are separate tasks. There is no account, no ads, and
no developer-run analytics.

<p align="center">
  <img src="docs/assets/home-dark.png" width="220" alt="Bihua home screen in dark mode">
  <img src="docs/assets/practice-dark.png" width="220" alt="Handwriting practice with a stroke guide">
  <img src="docs/assets/detail-dark.png" width="220" alt="Character detail">
  <img src="docs/assets/word-dark.png" width="220" alt="Word practice">
</p>

<p align="center">
  <a href="https://shadowsx3.github.io/bihua/"><strong>Landing page</strong></a>
  ·
  <a href="https://shadowsx3.github.io/bihua/method.html"><strong>The method</strong></a>
  ·
  <a href="https://github.com/Shadowsx3/bihua/releases/latest"><strong>Download</strong></a>
</p>

## Why it exists

Most character apps start with recognition: show a character and ask the learner
to tap a meaning. Bihua starts with production. Recognising 愛 is not the same
skill as writing it, and pronunciation is not the same skill again.

The design follows from that:

- **The character is the study item.** Words are context, not the scheduled unit.
- **Write from memory first.** The answer is not shown before the attempt.
- **The guide fades.** Full strokes, then outline, then nothing.
- **FSRS-7 decides the date.** The learner decides the grade.
- **Two skills, two tasks.** Writing and pronunciation can be tested separately.
- **Local data.** Progress, notes, and history stay on the device.

Read the full rationale in [The Bihua method](docs/method.html).

## Features

| Area | What it does |
|---|---|
| **Scripts** | Simplified, Traditional, Japanese, Korean. Separate courses; script-pure content. |
| **Scheduling** | FSRS-7 per script, optional sharing for identical glyphs, vacation mode, seven-day ease-back. |
| **Writing** | Introduce → Trace → Recall; guide fade; stroke order, shape, reversals, missing/extra strokes, and a focused correction loop. |
| **Pronunciation** | Separate review task; type pinyin, romaji, kana, or romanised eum/Hangul. |
| **Dictionary** | Reading, meaning, level, strokes, frequency, variants, radical, components, etymology, families, words, notes, history. |
| **Transfer** | Word writing, paper checks, stroke-order rules, phonetic/semantic families. |
| **Theme** | System, Light, and Dark; cream ink on forest-night paper. |
| **Data** | Full zip backup/import, CSV export, optional local Wi-Fi sync, no account. |

## Requirements

- Android 10+ (`minSdk 29`)
- ARM64 device
- JDK 17 and Android SDK 36 to build

## Install

The latest signed APK is on the [releases page](https://github.com/Shadowsx3/bihua/releases/latest).
The app is a private beta; read the release notes before installing.

## Build

```bash
source scripts/env.sh
make check      # compile, 132 unit tests, content audit, debug APK, verification
make lint       # Android lint
make run        # check, install, and launch on a connected device
make smoke      # launch/navigation smoke test on a connected device
make release    # signed minified APK when keystore.properties is present
```

The release build is signed with a local upload key. Keystores and
`keystore.properties` are ignored by git. See [PUBLISHING.md](PUBLISHING.md) for
the release process.

GitHub Actions runs unit tests, the content audit, and lint. It does not assemble
or upload an APK.

## Content

- Simplified: **3,180** (3,088 HSK 3.0 + 92 Non-HSK)
- Traditional: **3,163**
- Japanese: **2,286** (2,136 Jōyō + 150 kana)
- Korean: **2,239** (1,800 Hanja + 40 jamo + 399 syllables)
- **21,162** word examples
- **4,875** unique structure records
- Content version **19**

The content database is generated, not hand-edited:

```bash
tools/fetch-content.sh
python3 tools/import/build_content_db.py
make audit
```

`tools/data/manifest.json` records the URL, checksum, licence, and count for each
source. Raw datasets are not committed. Korean Hanja comes from Unihan; Hanjaro
data is not included because its site grants no redistribution rights. Korean
word examples are intentionally absent.

## Repository map

```text
app/                 Android app
docs/                landing page and methodology
licenses/            full third-party licence texts
tools/import/        content importer and generator
tools/data/          source manifest (raw data ignored)
scripts/             build, device, release, smoke helpers
.github/             CI, issue templates, Dependabot
```

## Privacy

No account, no developer-run analytics, no advertising SDK. Progress and notes
live in a local SQLite database. Network access is used for handwriting-model
downloads, Google ML Kit diagnostics, and optional local Wi-Fi sync. See
[PRIVACY.md](PRIVACY.md) and the [public policy](https://shadowsx3.github.io/bihua/privacy.html).

## Licensing

The application code is **MIT-licensed**; see [LICENSE](LICENSE).

The bundled content database is a compilation with separate terms:

- CC BY-SA 4.0 portions from HSK 3.0 and CC-CEDICT;
- Make Me a Hanzi graphics under the Arphic Public License;
- Make Me a Hanzi dictionary under LGPL-3.0-or-later;
- KanjiVG under CC BY-SA 3.0;
- KANJIDIC2 and JMdict under CC BY-SA 4.0;
- Unihan under the Unicode License V3;
- ivankra/hsk30 under MIT.

Full attribution is in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and
[`licenses/`](licenses/). The same notices are bundled in the app under
**Settings → About → Licences and sources**.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md). For security issues, use
[private reporting](https://github.com/Shadowsx3/bihua/security/advisories/new),
not a public issue.

## Status

Bihua is a private beta with a public codebase. The phone path is verified; a
current tablet/S Pen pass and a TalkBack/large-text audit remain on the roadmap.
The app is not a clinical intervention or a guarantee of literacy.
