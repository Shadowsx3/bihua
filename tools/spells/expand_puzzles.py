#!/usr/bin/env python3
"""Expand authored puzzle seeds into a large library of variations.

Each seed (one place, several honest solutions) becomes many variants that
differ in time of day and weather. Mechanics never change, so the solver stays
truthful. Seeds live in tools/spells/puzzle_seeds.csv; hints in
puzzle_hints_seeds.csv.

    python3 tools/spells/expand_puzzles.py [target]
"""
import csv
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app/src/main/assets"
SEEDS = Path(__file__).with_name("puzzle_seeds.csv")
SEED_HINTS = Path(__file__).with_name("puzzle_hints_seeds.csv")

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


def main() -> None:
    target = int(sys.argv[1]) if len(sys.argv) > 1 else 1500
    rows = list(csv.DictReader(SEEDS.open(encoding="utf-8")))
    puzzles: dict[str, dict] = {}
    for row in rows:
        pid = row["id"]
        entry = puzzles.setdefault(pid, {
            "place": row["place"], "lines": row["lines"], "prompt": row["prompt"],
            "solutions": [],
        })
        entry["solutions"].append((row["family"], row["text"]))
    hints: dict[str, list[dict]] = {}
    for row in csv.DictReader(SEED_HINTS.open(encoding="utf-8")):
        hints.setdefault(row["id"], []).append(row)

    ids = list(puzzles)
    out_puzzles: list[dict] = []
    out_hints: list[dict] = []
    seen_variants: set[str] = set()
    round_no = 0
    while len(seen_variants) < target:
        for pid in ids:
            if len(seen_variants) >= target:
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
            if variant in seen_variants:
                continue
            seen_variants.add(variant)
            for family, text in puzzles[pid]["solutions"]:
                out_puzzles.append({
                    "id": variant, "place": place, "lines": lines,
                    "prompt": puzzles[pid]["prompt"], "family": family, "text": text,
                })
            for row in hints.get(pid, []):
                out_hints.append({"id": variant, "family": row["family"], "text": row["text"]})
        round_no += 1
        if round_no > 400:
            break

    with (ASSETS / "puzzles.csv").open("w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=["id", "place", "lines", "prompt", "family", "text"])
        writer.writeheader()
        writer.writerows(out_puzzles)
    with (ASSETS / "puzzle_hints.csv").open("w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=["id", "family", "text"])
        writer.writeheader()
        writer.writerows(out_hints)
    print(f"{len(out_puzzles)} rows across {len(seen_variants)} puzzle variants")


if __name__ == "__main__":
    main()
