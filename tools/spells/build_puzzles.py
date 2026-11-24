#!/usr/bin/env python3
"""Build the app's puzzle library from the SQL seeds.

    python3 tools/spells/build_puzzles.py [target]

The source of truth is `tools/spells/puzzle_seeds.sql` (one place per puzzle, several
honest solutions, hints per family). This script runs that SQL in memory, expands each
seed into variants that differ only in time of day and weather, and writes
`app/src/main/assets/puzzles.db`. The app reads that database; no CSV is involved.
"""
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SEEDS = ROOT / "tools/spells/puzzle_seeds.sql"
OUT = ROOT / "app/src/main/assets/puzzles.db"

TIMES = [
    ("", ""),
    ("at dawn", "Dawn is still deciding what colour to be."),
    ("at noon", "The sun is directly overhead and taking it personally."),
    ("at dusk", "The light goes soft and orange at the edges."),
    ("at night", "Everything is quieter than it has any right to be."),
    ("in the rain", "Rain has been at this all morning and shows no sign of stopping."),
    ("in the fog", "Fog has swallowed the distance and is working on the rest."),
    ("in the wind", "The wind keeps changing its mind about which way to blow."),
    ("under a cold moon", "The moon is up, thin and sharp, and the air has teeth."),
]

SCHEMA = """
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


def expand(seed: sqlite3.Connection, target: int):
    puzzles = {}
    for pid, place, lines, prompt in seed.execute(
        "SELECT id, place, lines, prompt FROM seed_puzzle ORDER BY id"
    ):
        puzzles[pid] = {
            "place": place,
            "lines": lines,
            "prompt": prompt,
            "solutions": list(seed.execute(
                "SELECT family, text FROM seed_solution WHERE puzzle_id=?", (pid,)
            )),
            "hints": list(seed.execute(
                "SELECT family, text FROM seed_hint WHERE puzzle_id=?", (pid,)
            )),
        }
    ids = list(puzzles)
    out_puzzles, out_solutions, out_hints = [], [], []
    seen = set()
    round_no = 0
    while len(seen) < target and round_no <= 400:
        for pid in ids:
            if len(seen) >= target:
                break
            suffix, line = TIMES[round_no % len(TIMES)]
            if round_no == 0:
                variant = pid
                place = puzzles[pid]["place"]
                lines = puzzles[pid]["lines"]
            else:
                variant = f"{pid}-v{round_no}"
                place = f'{puzzles[pid]["place"]} {suffix}'.strip()
                lines = line + "|" + puzzles[pid]["lines"]
            if variant in seen:
                continue
            seen.add(variant)
            fallback = ""
            for family, text in puzzles[pid]["hints"]:
                if family == "*":
                    fallback = text
                else:
                    out_hints.append((variant, family, text))
            out_puzzles.append((variant, place, lines, puzzles[pid]["prompt"], fallback))
            for family, text in puzzles[pid]["solutions"]:
                out_solutions.append((variant, family, text))
        round_no += 1
    return out_puzzles, out_solutions, out_hints


def main() -> None:
    target = int(sys.argv[1]) if len(sys.argv) > 1 else 1500
    seed = sqlite3.connect(":memory:")
    seed.executescript(SEEDS.read_text(encoding="utf-8"))
    puzzles, solutions, hints = expand(seed, target)
    seed.close()

    if OUT.exists():
        OUT.unlink()
    db = sqlite3.connect(OUT)
    db.executescript(SCHEMA)
    db.executemany("INSERT INTO puzzle(id, place, lines, prompt, fallback) VALUES(?,?,?,?,?)", puzzles)
    db.executemany("INSERT OR IGNORE INTO solution(puzzle_id, family, text) VALUES(?,?,?)", solutions)
    db.executemany("INSERT OR REPLACE INTO hint(puzzle_id, family, text) VALUES(?,?,?)", hints)
    db.commit()
    db.execute("VACUUM")
    counts = db.execute("SELECT (SELECT COUNT(*) FROM puzzle), (SELECT COUNT(*) FROM solution)").fetchone()
    db.close()
    print(f"puzzles.db: {counts[0]} puzzles, {counts[1]} solutions, {OUT.stat().st_size // 1024} KiB")


if __name__ == "__main__":
    main()
