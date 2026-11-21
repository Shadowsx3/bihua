# Content data

The app's characters, readings, and stroke data come from open datasets. Raw
files live in `tools/data/raw/` and are **not committed**. `tools/data/manifest.json`
is committed and records the source URL, size, SHA-256, license, and count for
each file.

## Fetch and verify

```sh
tools/fetch-content.sh          # download missing files, extract, verify
tools/fetch-content.sh --force  # re-download everything
```

If upstream changes, the script stops with a checksum error. Update the
manifest deliberately, then rebuild the content database.

## Current sources

| File | Source | License | Verified count |
|------|--------|---------|----------------|
| `hsk30-characters-all.json` | Gushi, HSK 3.0 character list | CC BY-SA 4.0 | 3,088 characters |
| `hsk30-words-all.json` | Gushi, HSK 3.0 word list | CC BY-SA 4.0 | 11,000 words |
| `ivankra-hsk30-chars.csv` | `ivankra/hsk30` | MIT | 3,000 rows; 1,200 writing-set flags |
| `makemeahanzi-graphics.txt` | Make Me a Hanzi | Arphic Public License | 9,000+ graphics |
| `makemeahanzi-dictionary.txt` | Make Me a Hanzi | LGPL-3.0-or-later | 9,000+ entries |
| `kanjivg-20260714-all.zip` | KanjiVG r20260714 | CC BY-SA 3.0 | 11,662 SVG files |
| `kanjidic2.xml.gz` | EDRDG KANJIDIC2 | CC BY-SA 4.0 | 13,108 kanji; 2,136 Jōyō |
| `cedict_1_0_ts_utf-8_mdbg.txt.gz` | CC-CEDICT | CC BY-SA 4.0 | 125,446 entries |
| `JMdict_e.gz` | EDRDG JMdict | CC BY-SA 4.0 | Japanese word examples |
| `Unihan.zip` | Unicode Unihan | Unicode License V3 | variants, radicals, Korean Hanja, stroke counts |

## Rules

- Characters are the only study items. Words from `hsk30-words-all.json` and
  CC-CEDICT are read-only context on a character card.
- JMdict supplies Japanese word examples. Never show Chinese words on a
  Japanese card.
- The Jōyō set is KANJIDIC2 grade 1–6 plus grade 8. That is exactly 2,136.
- The writing set is `WritingLevel` non-empty in `ivankra-hsk30-chars.csv`.
  That is exactly 1,200.
- The app never reads a live website. The importer reads only `tools/data/raw/`.

## Attribution

Keep these credits in the About screen and in any exported content manifest:

- HSK 3.0 lists by Gushi, CC BY-SA 4.0.
- Make Me a Hanzi by Shaunak Kishore; graphics under the Arphic Public License,
  dictionary under LGPL-3.0-or-later.
- KanjiVG by Ulrich Apel, CC BY-SA 3.0.
- KANJIDIC2 and JMdict by EDRDG, CC BY-SA 4.0.
- CC-CEDICT, CC BY-SA 4.0.
- `ivankra/hsk30`, MIT.
- Unihan, Unicode License V3.
- Hanjaro is not used: its site grants no redistribution rights. The Korean
  course comes from Unihan's `kKoreanEducationHanja`, `kHangul`, and
  `kDefinition` fields; Korean word examples are intentionally absent.
