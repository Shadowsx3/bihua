#!/usr/bin/env python3
"""Export stroke guides from the Android content DB into small iOS shards."""
from __future__ import annotations

import json
import sqlite3
import struct
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = ROOT / "composeApp/src/androidMain/assets/bihua_content.db"
OUT = ROOT / "iosApp/Bihua/Content"
SHARDS = 32


def shard_for(group_id: str) -> int:
    return sum(ord(char) for char in group_id) % SHARDS


def main() -> int:
    if not DB.is_file():
        print(f"missing content database: {DB}", file=sys.stderr)
        return 1
    con = sqlite3.connect(DB)
    con.row_factory = sqlite3.Row
    buckets: dict[int, dict] = defaultdict(dict)
    for group in con.execute("select id, character, stroke_source from glyph_group"):
        group_id = group["id"]
        strokes = []
        for stroke in con.execute(
            "select points, path from glyph_stroke where glyph_group_id=? order by idx",
            (group_id,),
        ):
            entry = {}
            if stroke["points"]:
                blob = stroke["points"]
                values = struct.unpack("<" + "f" * (len(blob) // 4), blob)
                points = [
                    [round(values[i], 4), round(values[i + 1], 4)]
                    for i in range(0, len(values) - 1, 2)
                ]
                if points:
                    entry["points"] = points
            if stroke["path"]:
                entry["path"] = stroke["path"]
            if entry:
                strokes.append(entry)
        if not strokes:
            continue
        codepoint = None
        try:
            codepoint = int(group_id.split("U+")[-1][:4], 16)
        except ValueError:
            pass
        kana = bool(
            codepoint
            and (0x3040 <= codepoint <= 0x30FF or 0xFF66 <= codepoint <= 0xFF9D)
        )
        buckets[shard_for(group_id)][group_id] = {
            "source": group["stroke_source"] or "",
            "kana": kana,
            "strokes": strokes,
        }
    OUT.mkdir(parents=True, exist_ok=True)
    for shard in range(SHARDS):
        target = OUT / f"guide_{shard:02d}.json"
        with target.open("w", encoding="utf-8") as handle:
            json.dump(buckets.get(shard, {}), handle, ensure_ascii=False, separators=(",", ":"))
        print(f"{target.relative_to(ROOT)}  {target.stat().st_size // 1024} KB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
