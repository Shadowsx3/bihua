#!/usr/bin/env python3
"""Build the procedural puzzle parts database from tools/spells/parts.sql.

The SQL file is the source of truth. This script only compiles it into the
database the app ships, so there is one format to author and one file to review.
"""
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "tools/spells/parts.sql"
OUT = ROOT / "app/src/main/assets/puzzle_parts.db"


def main() -> None:
    if OUT.exists():
        OUT.unlink()
    db = sqlite3.connect(OUT)
    db.executescript(SOURCE.read_text(encoding="utf-8"))
    db.commit()
    counts = {
        table: db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
        for table in (
            "family", "family_tag", "obstacle", "obstacle_accepts", "tag_outcome",
            "setting", "setting_tag", "obstacle_place", "obstacle_success",
            "time_of_day", "weather",
        )
    }
    db.execute("VACUUM")
    db.close()
    print("puzzle_parts.db:", ", ".join(f"{k}={v}" for k, v in counts.items()))


if __name__ == "__main__":
    main()
