#!/usr/bin/env python3
"""Read-only content audit. Fails on critical data problems, warns on content quality."""
from __future__ import annotations

import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB = ROOT / "app/src/androidMain/assets/bihua_content.db"
CRITICAL = 0
MATURE_GLOSS = re.compile(
    r"\b(?:sexual(?:ly)?|sex organs?|genital(?:ia|s)?|penile|vaginal|semen|sperm|"
    r"penis|vagina|testicles?|intercourse|fornicat\w*|prostitut\w*|whore\w*|"
    r"slut\w*|brothel|rape|molest\w*|masturbat\w*|orgasm\w*|pornograph\w*|"
    r"erotic\w*|pussy\w*|fuck\w*|shit\w*|cunt\w*|dick\w*|bitch\w*|"
    r"lewd\w*|obscene\w*|lust|lustful|adulter\w*|debauch\w*|naked\w*|nude|"
    r"coition|carnal)\b",
    re.IGNORECASE,
)
EXPLICIT_GLOSS = re.compile(
    r"\b(?:sexual(?:ly)?|sex organs?|genital(?:ia|s)?|penile|vaginal|semen|sperm|"
    r"penis|vagina|testicles?|intercourse|fornicat\w*|prostitut\w*|whore\w*|"
    r"slut\w*|brothel|rape|molest\w*|masturbat\w*|orgasm\w*|pornograph\w*|"
    r"erotic\w*|pussy\w*|fuck\w*|shit\w*|cunt\w*|dick\w*|bitch\w*)\b",
    re.IGNORECASE,
)


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

    unsafe_defaults = [
        (mode, word, meaning)
        for mode, word, meaning in db.execute(
            "SELECT mode, word, meaning FROM word_context WHERE mature=0"
        )
        if MATURE_GLOSS.search(meaning)
    ]
    if unsafe_defaults:
        fail(f"{len(unsafe_defaults)} default word glosses contain explicit vocabulary")
        for mode, word, _ in unsafe_defaults[:5]:
            warn(f"  {mode} {word}")
    unsafe_characters = [
        (mode, character, meaning)
        for mode, character, meaning in db.execute(
            "SELECT mode, character, meaning FROM character"
        )
        if EXPLICIT_GLOSS.search(meaning)
    ]
    if unsafe_characters:
        fail(f"{len(unsafe_characters)} character meanings contain explicit vocabulary")
        for mode, character, _ in unsafe_characters[:5]:
            warn(f"  {mode} {character}")

    if count(db, "SELECT COUNT(*) FROM word_context WHERE mature NOT IN (0,1)"):
        fail("word_context.mature has values other than 0 or 1")
    if count(db, "SELECT COUNT(*) FROM word_context WHERE mature=1") == 0:
        warn("no mature words are tagged; explicit examples may have been dropped instead")

    korean_words = count(db, "SELECT COUNT(*) FROM word_context WHERE mode='KOREAN'")
    if korean_words < 8_000:
        fail(f"only {korean_words} Korean word examples")
    korean_coverage = count(
        db,
        "SELECT COUNT(DISTINCT wc.character_id) FROM word_context w "
        "JOIN word_character wc ON wc.word_id=w.id "
        "WHERE w.mode='KOREAN' AND wc.character_id IN "
        "(SELECT id FROM character WHERE mode='KOREAN' AND character >= '\u4e00')",
    )
    if korean_coverage < 1_650:
        fail(f"Korean word examples cover only {korean_coverage} Hanja")

    artifacts = count(
        db,
        "SELECT COUNT(*) FROM word_context WHERE meaning LIKE 'variant of%' "
        "OR meaning LIKE 'erhua%' OR meaning LIKE 'used in%' OR meaning LIKE '%CL:%'",
    )
    if artifacts:
        warn(f"{artifacts} word glosses still look like dictionary artifacts")

    gloss_artifact = re.compile(r"[\[\]{}|]|\([^)]*$")
    bad_glosses = [
        (mode, word, meaning)
        for mode, word, meaning in db.execute(
            "SELECT mode, word, meaning FROM word_context"
        )
        if gloss_artifact.search(meaning)
    ]
    if bad_glosses:
        fail(f"{len(bad_glosses)} word glosses contain dictionary notation")
        for mode, word, _ in bad_glosses[:5]:
            warn(f"  {mode} {word}")
    bad_character_glosses = [
        (mode, character, meaning)
        for mode, character, meaning in db.execute(
            "SELECT mode, character, meaning FROM character"
        )
        if gloss_artifact.search(meaning)
    ]
    if bad_character_glosses:
        fail(f"{len(bad_character_glosses)} character glosses contain dictionary notation")
        for mode, character, _ in bad_character_glosses[:5]:
            warn(f"  {mode} {character}")
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

    blank_character = count(
        db,
        "SELECT COUNT(*) FROM character WHERE trim(mode)='' OR trim(character)='' "
        "OR trim(level_label)='' OR trim(reading)='' OR trim(meaning)=''",
    )
    if blank_character:
        fail(f"{blank_character} character rows have a blank required field")
    blank_word = count(
        db,
        "SELECT COUNT(*) FROM word_context WHERE trim(mode)='' OR trim(word)='' "
        "OR trim(reading)='' OR trim(meaning)=''",
    )
    if blank_word:
        fail(f"{blank_word} word rows have a blank required field")

    def codepoint(value: str) -> int:
        return ord(value[0]) if value else 0

    def is_han(cp: int) -> bool:
        return (0x4E00 <= cp <= 0x9FFF or 0x3400 <= cp <= 0x4DBF
                or 0xF900 <= cp <= 0xFAFF or 0x20000 <= cp <= 0x2FA1F)

    def is_hiragana(cp: int) -> bool:
        return 0x3040 <= cp <= 0x309F

    def is_katakana(cp: int) -> bool:
        return (0x30A0 <= cp <= 0x30FF or 0x31F0 <= cp <= 0x31FF
                or 0xFF66 <= cp <= 0xFF9D)

    def is_hangul(cp: int) -> bool:
        return (0xAC00 <= cp <= 0xD7AF or 0x1100 <= cp <= 0x11FF
                or 0x3130 <= cp <= 0x318F)

    bad_script = []
    for mode, character in db.execute("SELECT mode, character FROM character"):
        cp = codepoint(character)
        ok = (
            is_han(cp) if mode in {"SIMPLIFIED", "TRADITIONAL"}
            else (is_han(cp) or is_hiragana(cp) or is_katakana(cp))
            if mode == "JAPANESE"
            else (is_han(cp) or is_hangul(cp))
            if mode == "KOREAN"
            else True
        )
        if not ok:
            bad_script.append((mode, character))
    if bad_script:
        fail(f"{len(bad_script)} characters are in the wrong script")
        for mode, character in bad_script[:5]:
            warn(f"  {mode} contains {character!r}")

    orphan_links = count(
        db,
        "SELECT COUNT(*) FROM word_character wc "
        "WHERE NOT EXISTS (SELECT 1 FROM word_context w WHERE w.id=wc.word_id) "
        "OR NOT EXISTS (SELECT 1 FROM character c WHERE c.id=wc.character_id)",
    )
    if orphan_links:
        fail(f"{orphan_links} word links point at missing rows")

    empty_etymology = count(db, "SELECT COUNT(*) FROM char_meta WHERE trim(etymology)=''")
    empty_structure = count(db, "SELECT COUNT(*) FROM char_meta WHERE trim(decomposition)=''")
    shape_notes = count(
        db, "SELECT COUNT(*) FROM char_meta WHERE etymology_type='shape'"
    )
    if empty_etymology:
        warn(f"{empty_etymology} structure rows still have no shape note")
    japanese_markers = count(
        db,
        "SELECT COUNT(*) FROM character WHERE mode='JAPANESE' "
        "AND (reading LIKE '%.%' OR reading LIKE '%-%')",
    )
    print(f"  info         {empty_etymology} structure rows have no shape note")
    print(f"  info         {shape_notes} structure rows use an honest shape note")
    print(f"  info         {empty_structure} structure rows have no decomposition")
    print(f"  info         {japanese_markers} Japanese readings use dictionary markers; display cleans them")
    for mode, common, mature in db.execute(
        "SELECT mode, SUM(common), SUM(mature) FROM word_context GROUP BY mode ORDER BY mode"
    ):
        print(f"  info         {mode} common words: {common or 0}, mature words: {mature or 0}")
    print(f"  info         Korean word coverage: {korean_coverage} Hanja")

    if CRITICAL:
        print(f"audit failed: {CRITICAL} critical problem(s)")
        return 1
    print("audit passed: no critical content problems")
    return 0


if __name__ == "__main__":
    sys.exit(main())
