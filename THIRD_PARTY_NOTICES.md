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
CC BY-SA 4.0 for the portions derived from CC BY-SA sources. The Make Me a Hanzi
graphics remain under the Arphic Public License, and the Make Me a Hanzi
dictionary remains under LGPL-3.0-or-later. Full licence texts are in the
`licenses/` directory and are bundled in the app under Settings → About.

### Hanjaro removed
Earlier development builds used Hanjaro data for the Korean course. The Hanjaro
site is all-rights-reserved and grants no redistribution rights, so it is **not**
included in this repository or in published builds. The Korean course is now
built from Unihan only. Korean word examples are intentionally absent.

## Runtime and libraries

- AndroidX, Jetpack Compose, Material Components — Apache License 2.0.
- Google ML Kit Digital Ink Recognition — Google ML Kit Terms of Service.
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
