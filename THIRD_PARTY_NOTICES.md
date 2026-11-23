# Bihua — Third-Party Notices

Bihua is an offline handwriting study app. The application code is MIT-licensed.
The bundled content database is a compilation of the open datasets below; it is
**not** covered by the application code licence.

## Content and data

### HSK 3.0 character and word lists
- Source: Gushi, <https://gushichinese.com/hsk-3.0/downloads>
- Licence: Creative Commons Attribution-ShareAlike 4.0 International
- Used for: Simplified/Traditional character levels, pinyin, meanings, and word context.

### ivankra/hsk30
- Source: <https://github.com/ivankra/hsk30>
- Licence: MIT
- Copyright: 2023 Ivan Krasilnikov; 2021 Shawky; 2021 Pleco Inc.
- Used for: HSK cross-check, Traditional forms, and the 1,200-character writing set.

### Make Me a Hanzi
- Source: <https://github.com/skishore/makemeahanzi>
- Graphics: Arphic Public License. Copyright (C) 1999 Arphic Technology Co., Ltd.
  The graphics are derived from the Arphic PL KaitiM GB and Arphic PL UKai fonts.
- Dictionary: GNU Lesser General Public License, version 3 or later.
  Copyright Shaunak Kishore and contributors. The dictionary is derived from
  Unihan and CJKlib.
- Used for: Chinese stroke paths, radicals, decomposition, and etymology hints.

### KanjiVG
- Source: <https://kanjivg.tagaini.net>
- Licence: Creative Commons Attribution-ShareAlike 3.0 Unported
- Copyright: Ulrich Apel and contributors.
- Used for: Japanese kanji and kana stroke paths.

### KANJIDIC2
- Source: <https://www.edrdg.org/kanjidic/kanjidic2.xml.gz>
- Licence: Creative Commons Attribution-ShareAlike 4.0 International
- Copyright: Electronic Dictionary Research and Development Group (EDRDG).
- Used for: Jōyō readings, meanings, grades, and stroke counts.

### JMdict
- Source: <https://www.edrdg.org/pub/Nihongo/JMdict_e.gz>
- Licence: Creative Commons Attribution-ShareAlike 4.0 International
- Copyright: Electronic Dictionary Research and Development Group (EDRDG).
- Used for: Japanese word examples.

### CC-CEDICT
- Source: <https://www.mdbg.net/chinese/dictionary?page=cedict>
- Licence: Creative Commons Attribution-ShareAlike 4.0 International
- Copyright: MDBG and CC-CEDICT contributors.
- Used for: Chinese word context and Traditional word pairs.

### Unihan Database
- Source: <https://www.unicode.org/charts/unihan.html>
- Licence: Unicode License V3
- Copyright © 1991–2026 Unicode, Inc.
- Used for: variant relations, Kangxi radicals, Korean Hanja education list,
  Korean readings, Korean definitions, and stroke counts.

The generated `bihua_content.db` is distributed as a compilation under
CC BY-SA 4.0 for the portions derived from CC BY-SA sources, with the Korean
Wiktionary portions also under CC BY-SA 3.0 and GFDL. The Make Me a Hanzi
graphics remain under the Arphic Public License, and the Make Me a Hanzi
dictionary remains under LGPL-3.0-or-later. Full licence texts are in the
`licenses/` directory and are bundled in the app under Settings → About.

### Korean Wiktionary extract
- Source: <https://kaikki.org/dictionary/Korean/> from English Wiktionary.
- Licence: Creative Commons Attribution-ShareAlike 3.0 Unported and GFDL.
- Copyright: Wiktionary contributors; extraction by Tatu Ylönen / kaikki.org.
- Used for: Korean word examples, Hanja spellings, and English glosses.

### FrequencyWords Korean
- Source: <https://github.com/hermitdave/FrequencyWords>
- Licence: Creative Commons Attribution-ShareAlike 4.0 International for the
  word lists (MIT for the generator code).
- Copyright: Hermit Dave and contributors.
- Used for: ranking common Korean word examples.

## Runtime and libraries

- AndroidX, Jetpack Compose, Material Components — Apache License 2.0.
- Google ML Kit Digital Ink Recognition — Google ML Kit Terms of Service.
- ONNX Runtime — MIT License, Microsoft. Used only for an optional,
  user-supplied model package.
- FSRS / FSRS-7 — MIT, Jarrett Ye and the open-spaced-repetition contributors.
- SBD memory model — A. Schill, *Interpretable Memory Models for Spaced
  Repetition* (2026), <https://doi.org/10.5281/zenodo.22727105>. The app's
  implementation is original; the paper describes the model.
- RWKV-Instant research — JSchoreels, Expertium, BlinkDL, and the
  open-spaced-repetition contributors. **Weights are not bundled or
  redistributed.** The upstream notice marks them research/personal-use only:
  <https://github.com/JSchoreels/anki-rwkv/blob/main/WEIGHTS-NOTICE.md>.
- Android TextToSpeech — the device's installed voices. No cloud speech service
  is used.
- Kotlin and kotlinx libraries — Apache License 2.0.

## Full licence texts

- `licenses/CC-BY-SA-4.0.txt`
- `licenses/CC-BY-SA-3.0.txt`
- `licenses/Arphic-Public-License.txt`
- `licenses/LGPL-3.0.txt`
- `licenses/MIT-ivankra-hsk30.txt`
- `licenses/Unicode-License.txt`
- `licenses/Apache-2.0.txt`
- `licenses/MIT-Bihua.txt`

The same notices are shown inside the app under **Settings → About → Licences
and sources**.
