#!/usr/bin/env python3
"""Export the read-only Android content database to per-mode JSON for iOS.

The iOS app reads one mode at a time from its bundle. This keeps the shared
content truth in `composeApp/src/androidMain/assets/bihua_content.db` and gives
iOS the same characters, readings, levels, example words, and etymology notes
without a second hand-maintained copy.

Run after changing the content database:
    python3 scripts/export-ios-content.py
"""
from __future__ import annotations

import json
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = ROOT / "composeApp/src/androidMain/assets/bihua_content.db"
OUT = ROOT / "iosApp/Bihua/Content"
MODES = ["SIMPLIFIED", "TRADITIONAL", "JAPANESE", "KOREAN"]


def main() -> int:
    if not DB.is_file():
        print(f"missing content database: {DB}", file=sys.stderr)
        return 1
    OUT.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(DB)
    con.row_factory = sqlite3.Row
    meta = {
        row["character"]: {
            "radical": row["radical"],
            "decomposition": row["decomposition"],
            "etymology": row["etymology"],
            "etymologyType": row["etymology_type"],
            "phonetic": row["phonetic"],
            "semantic": row["semantic"],
        }
        for row in con.execute(
            "select character, radical, decomposition, etymology, etymology_type, "
            "phonetic, semantic from char_meta"
        )
    }
    for mode in MODES:
        characters = [
            {
                "id": row["id"],
                "glyphGroupId": row["glyph_group_id"],
                "character": row["character"],
                "level": row["level"],
                "levelLabel": row["level_label"],
                "listIndex": row["list_index"],
                "reading": row["reading"],
                "readingAlt": row["reading_alt"],
                "meaning": row["meaning"],
                "traditional": row["traditional"],
                "simplified": row["simplified"],
                "writingSet": bool(row["is_writing"]),
                "frequency": row["frequency"],
                "strokeCount": row["stroke_count"],
            }
            for row in con.execute(
                "select id, glyph_group_id, character, level, level_label, list_index, "
                "reading, reading_alt, meaning, traditional, simplified, is_writing, "
                "frequency, stroke_count from character where mode=? "
                "order by level, list_index",
                (mode,),
            )
        ]
        words = [
            {
                "id": row["id"],
                "word": row["word"],
                "reading": row["reading"],
                "meaning": row["meaning"],
                "level": row["level"],
                "common": bool(row["common"]),
                "mature": bool(row["mature"]),
                "frequency": row["frequency"],
            }
            for row in con.execute(
                "select id, word, reading, meaning, level, common, mature, frequency "
                "from word_context where mode=?",
                (mode,),
            )
        ]
        by_word: dict[int, list[str]] = {}
        for row in con.execute(
            "select wc.word_id, wc.character_id from word_character wc "
            "join character c on c.id = wc.character_id where c.mode=?",
            (mode,),
        ):
            by_word.setdefault(row["word_id"], []).append(row["character_id"])
        for word in words:
            word["characters"] = by_word.get(word["id"], [])
        target = OUT / f"content_{mode.lower()}.json"
        with target.open("w", encoding="utf-8") as handle:
            json.dump(
                {"mode": mode, "characters": characters, "words": words, "meta": meta},
                handle,
                ensure_ascii=False,
                separators=(",", ":"),
            )
        print(f"{target.relative_to(ROOT)}  {target.stat().st_size // 1024} KB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
