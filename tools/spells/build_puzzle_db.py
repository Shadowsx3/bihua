#!/usr/bin/env python3
"""Compile the authored puzzle CSVs into a small indexed SQLite asset.

Source of truth stays the CSV (easy to write and diff). The app reads the
database, so a library of thousands loads by family instead of scanning text.
"""
import csv
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app/src/main/assets"
OUT = ASSETS / "puzzles.db"


def main() -> None:
    if OUT.exists():
        OUT.unlink()
    db = sqlite3.connect(OUT)
    db.executescript(
        """
        CREATE TABLE puzzle(
            id TEXT PRIMARY KEY,
            place TEXT NOT NULL,
            lines TEXT NOT NULL,
            prompt TEXT NOT NULL,
            fallback TEXT NOT NULL
        );
        CREATE TABLE solution(
            puzzle_id TEXT NOT NULL,
            family TEXT NOT NULL,
            text TEXT NOT NULL,
            PRIMARY KEY(puzzle_id, family, text)
        );
        CREATE TABLE hint(
            puzzle_id TEXT NOT NULL,
            family TEXT NOT NULL,
            text TEXT NOT NULL,
            PRIMARY KEY(puzzle_id, family)
        );
        CREATE INDEX solution_family ON solution(family);
        """
    )
    fallbacks: dict[str, str] = {}
    for row in csv.DictReader((ASSETS / "puzzle_hints.csv").open(encoding="utf-8")):
        if row["family"] == "*":
            fallbacks[row["id"]] = row["text"]

    seen: set[str] = set()
    for row in csv.DictReader((ASSETS / "puzzles.csv").open(encoding="utf-8")):
        pid = row["id"]
        if pid not in seen:
            seen.add(pid)
            db.execute(
                "INSERT INTO puzzle(id, place, lines, prompt, fallback) VALUES(?,?,?,?,?)",
                (pid, row["place"], row["lines"], row["prompt"],
                 fallbacks.get(pid, "The ink settles and nothing changes. Try a different element.")),
            )
        db.execute(
            "INSERT OR IGNORE INTO solution(puzzle_id, family, text) VALUES(?,?,?)",
            (pid, row["family"], row["text"]),
        )
    for row in csv.DictReader((ASSETS / "puzzle_hints.csv").open(encoding="utf-8")):
        if row["family"] == "*":
            continue
        db.execute(
            "INSERT OR REPLACE INTO hint(puzzle_id, family, text) VALUES(?,?,?)",
            (row["id"], row["family"], row["text"]),
        )
    db.commit()
    puzzles = db.execute("SELECT COUNT(*) FROM puzzle").fetchone()[0]
    solutions = db.execute("SELECT COUNT(*) FROM solution").fetchone()[0]
    db.close()
    print(f"puzzles.db: {puzzles} puzzles, {solutions} solutions, {OUT.stat().st_size // 1024} KiB")


if __name__ == "__main__":
    main()
