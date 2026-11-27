#!/usr/bin/env python3
"""Seed a progress database with enough review history to exercise the local FSRS fit.

This is a development tool, not shipped in the app. It writes per-script cards and
a separate set of shared-glyph cards so both weight profiles can be tested.

Usage:
  scripts/device.sh db-pull /tmp/bihua-fsrs.db
  python3 tools/seed_fsrs_fit.py /tmp/bihua-fsrs.db
  scripts/device.sh db-push /tmp/bihua-fsrs.db
"""
from __future__ import annotations

import random
import sqlite3
import sys
import time
from datetime import datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "composeApp/src/androidMain/assets/bihua_content.db"
DAY = 86_400_000
MODES = ["SIMPLIFIED", "TRADITIONAL", "JAPANESE", "KOREAN"]


def main(path: Path, per_mode: int, reviews_per_card: int) -> None:
    if not path.is_file():
        raise SystemExit(f"missing progress database: {path}")
    rng = random.Random(42)
    content = sqlite3.connect(CONTENT)
    rows = {
        mode: content.execute(
            "SELECT character, id, glyph_group_id FROM character WHERE mode=? "
            "ORDER BY list_index LIMIT ?",
            (mode, per_mode),
        ).fetchall()
        for mode in MODES
    }
    shared_rows = content.execute(
        "SELECT s.character, s.id, t.id, s.glyph_group_id FROM character s "
        "JOIN character t ON t.glyph_group_id=s.glyph_group_id AND t.mode='TRADITIONAL' "
        "WHERE s.mode='SIMPLIFIED' ORDER BY s.list_index LIMIT 60"
    ).fetchall()
    content.close()

    db = sqlite3.connect(path)
    db.executescript(
        """
        DELETE FROM card;
        DELETE FROM review_log;
        DELETE FROM xp_log;
        DELETE FROM player;
        DELETE FROM confusion;
        DELETE FROM word_practice;
        DELETE FROM note;
        INSERT OR IGNORE INTO player(id, xp) VALUES(1, 0);
        """
    )
    now = int(time.time() * 1000)
    cards = []
    logs = []
    xp = []

    def add_history(card_id: str, character_ids: list[str], mode: str, glyph_id: str, seed: int) -> None:
        rng = random.Random(seed)
        start = now - (240 + rng.randrange(90)) * DAY
        interval = 1.0
        grade = 3
        lapses = 0
        reviewed = start
        for index in range(reviews_per_card):
            if index > 0:
                reviewed += int(interval * DAY)
            if reviewed > now - DAY:
                break
            grade = rng.choices([1, 2, 3, 4], weights=[8, 18, 60, 14])[0]
            if grade == 1:
                lapses += 1
                interval = max(0.3, interval * 0.25)
            elif grade == 2:
                interval = min(180.0, max(0.5, interval * 1.15))
            elif grade == 4:
                interval = min(180.0, max(0.8, interval * 2.8))
            else:
                interval = min(180.0, max(0.7, interval * 1.8))
            s_long = interval * (0.8 + rng.random() * 0.4)
            difficulty = min(9.5, max(1.5, 4.5 + (index % 4) * 0.7))
            character = character_ids[index % len(character_ids)]
            task = "PRONOUNCE" if index % 4 == 0 else "WRITE"
            logs.append(
                (
                    card_id,
                    character,
                    int(reviewed),
                    int(grade),
                    float(interval),
                    float(s_long),
                    float(max(0.2, s_long * 0.8)),
                    float(difficulty),
                    0,
                    rng.randrange(5_000, 25_000),
                    task,
                    0 if task == "PRONOUNCE" else rng.randrange(5, 15),
                )
            )
            xp.append((card_id, datetime.fromtimestamp(reviewed / 1000).strftime("%Y-%m-%d"), 8))
        last = logs[-1] if logs and logs[-1][0] == card_id else None
        if last is None:
            return
        final_interval = last[4]
        cards.append(
            (
                card_id,
                character_ids[0],
                glyph_id,
                mode,
                float(final_interval),
                float(max(0.2, final_interval * 0.8)),
                float(last[7]),
                int(now + max(1, final_interval) * DAY),
                int(last[2]),
                len([log for log in logs if log[0] == card_id]),
                int(lapses),
                int(start),
                min(2, len([log for log in logs if log[0] == card_id]) // 3),
                int(last[3]),
            )
        )

    for mode_index, mode in enumerate(MODES):
        for index, (_char, char_id, _glyph) in enumerate(rows[mode]):
            add_history(f"char:{char_id}", [char_id], mode, _glyph, mode_index * 1000 + index)

    for index, (_char, simp_id, trad_id, glyph_id) in enumerate(shared_rows):
        add_history(f"glyph:{glyph_id}", [simp_id, trad_id], "SIMPLIFIED", glyph_id, 9000 + index)

    unique_cards = list({card[0]: card for card in cards}.values())
    db.executemany("INSERT OR REPLACE INTO card VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", unique_cards)
    db.executemany(
        "INSERT INTO review_log(card_id,character_id,reviewed_at,grade,interval_days,s_long,s_short,difficulty,reverted,elapsed_ms,task,strokes) "
        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
        logs,
    )
    db.executemany("INSERT OR REPLACE INTO xp_log(card_id,day,xp) VALUES(?,?,?)", xp)
    db.execute("UPDATE player SET xp=?", (max(0, len(logs) * 6),))
    db.commit()
    db.close()
    shared_reviews = sum(1 for log in logs if log[0].startswith("glyph:"))
    print(
        f"seeded {len(unique_cards)} cards, {len(logs)} reviews "
        f"({shared_reviews} shared), {reviews_per_card} reviews max per card"
    )


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3, 4):
        raise SystemExit("usage: seed_fsrs_fit.py <progress.db> [cards-per-mode] [reviews-per-card]")
    per_mode = int(sys.argv[2]) if len(sys.argv) > 2 else 60
    reviews = int(sys.argv[3]) if len(sys.argv) > 3 else 12
    main(Path(sys.argv[1]), per_mode, reviews)
