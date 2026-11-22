#!/usr/bin/env bash
# Fetch the open content datasets used to build Bihua's content database.
#
# Files land in tools/data/raw and are ignored by git. The manifest in
# tools/data/manifest.json records the URL, size, SHA-256, license, and count
# for every file, so a download can be checked and reproduced.
#
# Usage:
#   tools/fetch-content.sh          # download missing files, then verify
#   tools/fetch-content.sh --force  # re-download everything
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RAW="$ROOT/tools/data/raw"
MANIFEST="$ROOT/tools/data/manifest.json"
FORCE="${1:-}"

mkdir -p "$RAW"

fetch() {
  local url="$1"
  local name="$2"
  if [[ -f "$RAW/$name" && "$FORCE" != "--force" ]]; then
    echo "skip  $name"
    return
  fi
  echo "fetch $name"
  curl -L --fail --retry 3 --retry-delay 2 --max-time 300 \
    -o "$RAW/$name.part" "$url"
  mv "$RAW/$name.part" "$RAW/$name"
}

fetch "https://gushichinese.com/hsk-3.0/downloads/hsk-3.0-characters-all.json" "hsk30-characters-all.json"
fetch "https://gushichinese.com/hsk-3.0/downloads/hsk-3.0-words-all.json" "hsk30-words-all.json"
fetch "https://raw.githubusercontent.com/ivankra/hsk30/master/hsk30-chars.csv" "ivankra-hsk30-chars.csv"
fetch "https://raw.githubusercontent.com/skishore/makemeahanzi/master/graphics.txt" "makemeahanzi-graphics.txt"
fetch "https://raw.githubusercontent.com/skishore/makemeahanzi/master/dictionary.txt" "makemeahanzi-dictionary.txt"
fetch "https://github.com/KanjiVG/kanjivg/releases/download/r20260714/kanjivg-20260714-all.zip" "kanjivg-20260714-all.zip"
fetch "https://www.edrdg.org/kanjidic/kanjidic2.xml.gz" "kanjidic2.xml.gz"
fetch "https://www.unicode.org/Public/UCD/latest/ucd/Unihan.zip" "Unihan.zip"
fetch "https://www.mdbg.net/chinese/export/cedict/cedict_1_0_ts_utf-8_mdbg.txt.gz" "cedict_1_0_ts_utf-8_mdbg.txt.gz"
fetch "https://www.edrdg.org/pub/Nihongo/JMdict_e.gz" "JMdict_e.gz"
fetch "https://kaikki.org/dictionary/Korean/kaikki.org-dictionary-Korean.jsonl.gz" "kaikki-korean.jsonl.gz"
fetch "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/ko/ko_50k.txt" "ko_50k.txt"

# Extract the archives the importer reads directly.
if [[ ! -d "$RAW/kanjivg/kanji" || "$FORCE" == "--force" ]]; then
  echo "unzip kanjivg"
  rm -rf "$RAW/kanjivg"
  unzip -q -o "$RAW/kanjivg-20260714-all.zip" -d "$RAW/kanjivg"
fi
if [[ ! -d "$RAW/unihan" || "$FORCE" == "--force" ]]; then
  echo "unzip unihan"
  rm -rf "$RAW/unihan"
  mkdir -p "$RAW/unihan"
  unzip -q -o "$RAW/Unihan.zip" -d "$RAW/unihan"
fi

for archive in kanjidic2.xml.gz cedict_1_0_ts_utf-8_mdbg.txt.gz JMdict_e.gz; do
  plain="${archive%.gz}"
  if [[ ! -f "$RAW/$plain" || "$FORCE" == "--force" ]]; then
    echo "gunzip $archive"
    gzip -dkf "$RAW/$archive"
  fi
done

python3 - "$RAW" "$MANIFEST" <<'PY'
import hashlib
import json
import os
import sys

raw, manifest_path = sys.argv[1], sys.argv[2]
manifest = json.load(open(manifest_path, encoding="utf-8"))
failed = []

for item in manifest["items"]:
    path = os.path.join(raw, item["file"])
    if not os.path.exists(path):
        failed.append(f"{item['file']}: missing")
        continue
    size = os.path.getsize(path)
    if size != item["bytes"]:
        failed.append(f"{item['file']}: size {size} != {item['bytes']}")
        continue
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for block in iter(lambda: handle.read(1 << 20), b""):
            digest.update(block)
    if digest.hexdigest() != item["sha256"]:
        failed.append(f"{item['file']}: sha256 mismatch")

if failed:
    print("Content verification failed:")
    for line in failed:
        print("  " + line)
    print("If upstream changed, update tools/data/manifest.json intentionally.")
    sys.exit(1)

print(f"Verified {len(manifest['items'])} content files.")
for key, value in manifest["derived"].items():
    print(f"  {key}: {value}")
PY
