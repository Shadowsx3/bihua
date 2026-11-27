#!/usr/bin/env python3
"""Checks composeApp/src/androidMain/assets/spellbook.csv against the shipped content database.

Fails on structural problems: unknown characters, duplicates, a family in the
wrong category, or a bad effect id. Reports (does not fail) effects that have
no vector drawable yet, so art can land family by family.
"""
import csv
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB = ROOT / "composeApp/src/androidMain/assets/bihua_content.db"
CSV = ROOT / "composeApp/src/androidMain/assets/spellbook.csv"
DRAWABLES = ROOT / "composeApp/src/main/res/drawable"

FAMILY_CATEGORY = {
    "water": "Tides", "ice": "Tides", "snow": "Tides", "rain": "Tides",
    "fire": "Flame", "lightning": "Flame",
    "wind": "Sky", "cloud": "Sky", "sun": "Sky", "moon": "Sky",
    "earth": "Ground", "stone": "Ground", "mountain": "Ground", "wood": "Ground",
    "animal": "Creatures", "person": "Creatures", "hand": "Creatures",
    "boat": "Constructs", "metal": "Constructs",
}


def main() -> int:
    rows = list(csv.DictReader(CSV.read_text(encoding="utf-8").splitlines()))
    db = sqlite3.connect(DB)
    known = {r[0] for r in db.execute("SELECT DISTINCT character FROM character WHERE mode='SIMPLIFIED'")}
    errors = []
    seen = set()
    for row in rows:
        char = row["character"]
        if len(char) != 1:
            errors.append(f"{char!r}: not a single character")
        if char not in known:
            errors.append(f"{char}: not in the Simplified course")
        if char in seen:
            errors.append(f"{char}: listed twice")
        seen.add(char)
        family = row["family"]
        if family not in FAMILY_CATEGORY:
            errors.append(f"{char}: unknown family {family}")
        elif row["category"] != FAMILY_CATEGORY[family]:
            errors.append(f"{char}: {family} belongs to {FAMILY_CATEGORY[family]}, not {row['category']}")
        if not row["effect"].startswith("fx_") or not row["effect"][3:].replace("_", "").isalnum():
            errors.append(f"{char}: bad effect id {row['effect']}")
    art = {p.stem for p in DRAWABLES.glob("fx_*.xml")} if DRAWABLES.exists() else set()
    missing_art = sorted({r["effect"] for r in rows} - art)
    print(f"{len(rows)} spells, {len(art)} effect drawables, {len(missing_art)} effects still need art")
    for error in errors:
        print("  ERROR", error)
    if errors:
        return 1
    print("spellbook audit ok")
    return 0


if __name__ == "__main__":
    sys.exit(main())
