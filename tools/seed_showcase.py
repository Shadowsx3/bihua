#!/usr/bin/env python3
"""Seed a progress database with a realistic showcase save.

This is a development tool, not shipped in the app. It is used to capture
local screenshots and to exercise the Progress, Home, and word-history screens
with enough data to look like a real learner.

Usage:
  scripts/device.sh db-pull /tmp/bihua-progress.db
  python3 tools/seed_showcase.py /tmp/bihua-progress.db
  scripts/device.sh db-push /tmp/bihua-progress.db
"""
from __future__ import annotations

import sqlite3
import sys
import time
from datetime import datetime, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "app/src/main/assets/bihua_content.db"

# A small, coherent Traditional Chinese study set.
CHARS = [
    ("愛", 2, 8),
    ("學", 2, 14),
    ("道", 2, 9),
    ("心", 2, 7),
    ("語", 2, 12),
    ("寫", 2, 11),
    ("書", 2, 10),
    ("靜", 2, 6),
    ("山", 1, 5),
    ("水", 1, 4),
    ("風", 1, 3),
    ("花", 1, 2),
    ("讀", 1, 8),
    ("日", 1, 6),
    ("月", 0, 2),
    ("人", 0, 3),
    ("好", 0, 1),
    ("可", 0, 2),
]

GRADES = [3, 3, 4, 2, 3, 3, 1, 3, 4, 3, 2, 3, 4, 3, 3, 2, 1, 3]


def day_start_ms(days_ago: int = 0) -> int:
    d = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    return int((d - timedelta(days=days_ago)).timestamp() * 1000)


def main(path: Path) -> None:
    if not path.is_file():
        raise SystemExit(f"missing progress database: {path}")
    content = sqlite3.connect(CONTENT)
    meta = {
        row[0]: (row[1], row[2])
        for row in content.execute(
            "SELECT character, id, glyph_group_id FROM character WHERE mode='TRADITIONAL'"
        )
    }
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
    missing = []
    cards = []
    logs = []
    for order, (char, dots, reps) in enumerate(CHARS):
        if char not in meta:
            missing.append(char)
            continue
        char_id, glyph_id = meta[char]
        card_id = f"char:{char_id}"
        introduced = day_start_ms(0 if order < 2 else 3 + order)
        if dots >= 2:
            due = now + (8 + order * 3) * 86_400_000
            s_long = 26 + order * 4
            last = now - (2 + order % 5) * 86_400_000
            lapses = 0 if order % 4 else 1
            last_grade = 3 if order % 5 else 4
        elif dots == 1:
            due = now - (1 + order % 4) * 86_400_000
            s_long = 3 + order % 4
            last = now - (1 + order % 3) * 86_400_000
            lapses = 1 if order % 2 else 0
            last_grade = 1 if order % 2 else 2
        else:
            due = now + (2 + order % 5) * 86_400_000
            s_long = 1.2 + order * 0.2
            last = now - 2 * 86_400_000
            lapses = 0
            last_grade = 2
        cards.append(
            (
                card_id,
                char_id,
                glyph_id,
                "TRADITIONAL",
                float(s_long),
                float(max(1.0, s_long * 0.35)),
                float(3.5 + (order % 5) * 0.8),
                int(due),
                int(last),
                int(reps),
                int(lapses),
                int(introduced),
                int(dots),
                int(last_grade),
            )
        )
        for index in range(max(2, min(6, reps))):
            reviewed = last - index * 86_400_000
            grade = GRADES[(order + index) % len(GRADES)]
            task = "PRONOUNCE" if (order + index) % 3 == 0 else "WRITE"
            strokes = 0 if task == "PRONOUNCE" else max(4, 8 + order % 9)
            elapsed = 6_000 + ((order * 1_700 + index * 900) % 12_000)
            logs.append(
                (
                    card_id,
                    char_id,
                    int(reviewed),
                    int(grade),
                    float(max(1, s_long * 0.6)),
                    float(s_long),
                    float(max(1.0, s_long * 0.35)),
                    float(3.5 + (order % 5) * 0.8),
                    0,
                    int(elapsed),
                    task,
                    int(strokes),
                )
            )
    if missing:
        raise SystemExit(f"missing characters in content: {missing}")
    db.executemany(
        "INSERT INTO card VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        cards,
    )
    db.executemany(
        "INSERT INTO review_log(card_id,character_id,reviewed_at,grade,interval_days,s_long,s_short,difficulty,reverted,elapsed_ms,task,strokes) "
        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
        logs,
    )
    db.executemany(
        "INSERT INTO xp_log(card_id,day,xp) VALUES(?,?,?)",
        [(card[0], datetime.fromtimestamp(card[8] / 1000).strftime("%Y-%m-%d"), 12) for card in cards],
    )
    db.execute("UPDATE player SET xp=640 WHERE id=1")
    db.execute(
        "INSERT INTO note(character_id,text,updated_at) VALUES(?,?,?)",
        (meta["愛"][0], "Heart 心 under a roof. The hand remembers the house.", now),
    )
    db.execute(
        "INSERT INTO confusion(seen_id,wrote_id,count,last_at) VALUES(?,?,?,?)",
        (meta["愛"][0], meta["受"][0] if "受" in meta else meta["愛"][0], 3, now),
    )
    words = [("愛人", "àiren", "spouse; lover"), ("喜愛", "xǐài", "to like; to love"), ("可愛", "kěài", "adorable; cute"), ("愛情", "àiqíng", "romance; love")]
    for char, reading, meaning in words:
        db.execute(
            "INSERT OR REPLACE INTO word_practice(mode,word,reading,meaning,practiced_at,completions) VALUES(?,?,?,?,?,?)",
            ("TRADITIONAL", char, reading, meaning, now - len(words) * 60_000, 2),
        )
    db.commit()
    db.close()
    print(f"seeded {len(cards)} cards, {len(logs)} reviews, {len(words)} practised words")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: seed_showcase.py <progress.db>")
    main(Path(sys.argv[1]))
