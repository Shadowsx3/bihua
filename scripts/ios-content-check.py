#!/usr/bin/env python3
"""Check that the exported iOS content JSON matches the Android content DB."""
from __future__ import annotations

import json
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = ROOT / "composeApp/src/androidMain/assets/bihua_content.db"
CONTENT = ROOT / "iosApp/Bihua/Content"
MODES = ["SIMPLIFIED", "TRADITIONAL", "JAPANESE", "KOREAN"]


def main() -> int:
    if not DB.is_file():
        print(f"missing content database: {DB}", file=sys.stderr)
        return 1
    con = sqlite3.connect(DB)
    failures = 0
    guides: dict[str, dict] = {}
    for shard in range(32):
        target = CONTENT / f"guide_{shard:02d}.json"
        if not target.is_file():
            print(f"FAIL: missing {target.relative_to(ROOT)}")
            failures += 1
            continue
        with target.open(encoding="utf-8") as handle:
            guides.update(json.load(handle))
    if guides:
        print(f"ok: {len(guides)} stroke guides across 32 shards")
    else:
        print("FAIL: no stroke guides")
        failures += 1
    for mode in MODES:
        target = CONTENT / f"content_{mode.lower()}.json"
        if not target.is_file():
            print(f"FAIL: missing {target.relative_to(ROOT)}")
            failures += 1
            continue
        with target.open(encoding="utf-8") as handle:
            payload = json.load(handle)
        db_chars = con.execute(
            "select count(*) from character where mode=?", (mode,)
        ).fetchone()[0]
        db_words = con.execute(
            "select count(*) from word_context where mode=?", (mode,)
        ).fetchone()[0]
        if payload.get("mode") != mode:
            print(f"FAIL: {target.name} mode is {payload.get('mode')}")
            failures += 1
        if len(payload.get("characters", [])) != db_chars:
            print(f"FAIL: {target.name} characters {len(payload.get('characters', []))} != {db_chars}")
            failures += 1
        if len(payload.get("words", [])) != db_words:
            print(f"FAIL: {target.name} words {len(payload.get('words', []))} != {db_words}")
            failures += 1
        if not payload.get("meta"):
            print(f"FAIL: {target.name} has no character meta")
            failures += 1
        covered = sum(1 for char in payload.get("characters", []) if char.get("glyphGroupId") in guides)
        if failures == 0:
            print(
                f"ok: {target.name}: {db_chars} characters, {db_words} words, "
                f"{covered}/{db_chars} with stroke guides"
            )
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
