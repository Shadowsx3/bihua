#!/usr/bin/env python3
"""Read-only content audit. Fails on critical data problems, warns on content quality."""
from __future__ import annotations

import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB = ROOT / "app/src/main/assets/bihua_content.db"
CRITICAL = 0


def fail(message: str) -> None:
    global CRITICAL
    CRITICAL += 1
    print(f"CRITICAL  {message}")


def warn(message: str) -> None:
    print(f"WARN      {message}")


def count(db: sqlite3.Connection, sql: str, args: tuple = ()) -> int:
    return db.execute(sql, args).fetchone()[0]


def main() -> int:
    if not DB.exists():
        print(f"missing {DB}")
        return 1
    db = sqlite3.connect(DB)
    print(f"content: {DB} ({DB.stat().st_size / 1_000_000:.1f} MB)")
    for mode, number in db.execute("SELECT mode, COUNT(*) FROM character GROUP BY mode"):
        print(f"  {mode:12} {number}")
    print(f"  words        {count(db, 'SELECT COUNT(*) FROM word_context')}")
    print(f"  structures   {count(db, 'SELECT COUNT(*) FROM char_meta')}")

    empty_reading = count(db, "SELECT COUNT(*) FROM character WHERE trim(reading)=''")
    if empty_reading:
        fail(f"{empty_reading} characters have no reading")
    empty_meaning = db.execute(
        "SELECT mode, character FROM character WHERE trim(meaning)=''"
    ).fetchall()
    for mode, char in empty_meaning:
        fail(f"{mode} {char} has no meaning")

    duplicates = count(
        db,
        "SELECT COALESCE(SUM(n),0) FROM (SELECT COUNT(*)-COUNT(DISTINCT character) n "
        "FROM character GROUP BY mode)",
    )
    if duplicates:
        fail(f"{duplicates} duplicate characters")

    cross = count(
        db,
        "SELECT COUNT(*) FROM character s JOIN character t "
        "ON s.character=t.character AND s.mode='SIMPLIFIED' AND t.mode='TRADITIONAL' "
        "WHERE s.traditional<>s.character AND instr(' '||s.traditional||' ', ' '||t.character||' ')>0",
    )
    if cross:
        fail(f"{cross} simplified-only forms appear in the Traditional course")
    backward = count(
        db,
        "SELECT COUNT(*) FROM character WHERE mode='SIMPLIFIED' AND character IN "
        "('後','發','臺','雲','隻','髮','裡','麵','幹','裏')",
    )
    if backward:
        fail(f"{backward} traditional-only forms appear in the Simplified course")

    word_empty = count(
        db, "SELECT COUNT(*) FROM word_context WHERE trim(reading)='' OR trim(meaning)=''"
    )
    if word_empty:
        fail(f"{word_empty} words have no reading or meaning")
    word_dups = count(
        db,
        "SELECT COALESCE(SUM(n),0) FROM (SELECT COUNT(*)-COUNT(DISTINCT word) n "
        "FROM word_context GROUP BY mode)",
    )
    if word_dups:
        fail(f"{word_dups} duplicate words")
    orphans = count(
        db,
        "SELECT COUNT(*) FROM word_context w WHERE NOT EXISTS "
        "(SELECT 1 FROM word_character wc WHERE wc.word_id=w.id)",
    )
    if orphans:
        fail(f"{orphans} words have no character link")
    wrong_script = count(
        db,
        "SELECT COUNT(*) FROM word_context w WHERE EXISTS "
        "(SELECT 1 FROM word_character wc WHERE wc.word_id=w.id AND NOT EXISTS "
        "(SELECT 1 FROM character c WHERE c.id=wc.character_id AND c.mode=w.mode))",
    )
    if wrong_script:
        fail(f"{wrong_script} words link characters outside their mode")

    artifacts = count(
        db,
        "SELECT COUNT(*) FROM word_context WHERE meaning LIKE 'variant of%' "
        "OR meaning LIKE 'erhua%' OR meaning LIKE 'used in%' OR meaning LIKE '%CL:%'",
    )
    if artifacts:
        warn(f"{artifacts} word glosses still look like dictionary artifacts")
    long_meanings = count(db, "SELECT COUNT(*) FROM character WHERE length(meaning)>180")
    if long_meanings:
        warn(f"{long_meanings} character meanings exceed 180 characters")
    no_guide = count(db, "SELECT COUNT(*) FROM character WHERE stroke_source='none'")
    non_han_guide = count(
        db,
        "SELECT COUNT(*) FROM character WHERE stroke_source='none' "
        "AND level_label IN ('Hiragana','Katakana','Hangul','Syllables')",
    )
    print(f"  info         {no_guide} characters have no stroke guide")
    print(f"  info         {non_han_guide} kana/Hangul rows have no stroke guide by design")
    missing_meta = count(
        db,
        "SELECT COUNT(*) FROM character c "
        "WHERE c.level_label NOT IN ('Hiragana','Katakana','Hangul','Syllables') "
        "AND NOT EXISTS (SELECT 1 FROM char_meta m WHERE m.character=c.character)",
    )
    if missing_meta:
        warn(f"{missing_meta} Han characters have no structure metadata")
    bracket_meanings = count(
        db, "SELECT COUNT(*) FROM character WHERE meaning LIKE '%[%' OR meaning LIKE '%CL:%'"
    )
    if bracket_meanings:
        fail(f"{bracket_meanings} character meanings still contain dictionary references")

    if CRITICAL:
        print(f"audit failed: {CRITICAL} critical problem(s)")
        return 1
    print("audit passed: no critical content problems")
    return 0


if __name__ == "__main__":
    sys.exit(main())
