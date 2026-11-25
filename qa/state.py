#!/usr/bin/env python3
"""Deterministic progress and settings fixtures for the local journey harness."""
from __future__ import annotations

import json
import math
import random
import sqlite3
import time
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "app/src/main/assets/bihua_content.db"
QA = ROOT / ".qa"
SCHEMA_VERSION = 10
DAY = 86_400_000


SCHEMA = """
CREATE TABLE card(
  id TEXT PRIMARY KEY,
  character_id TEXT NOT NULL,
  glyph_group_id TEXT NOT NULL,
  mode TEXT NOT NULL,
  s_long REAL NOT NULL,
  s_short REAL NOT NULL,
  difficulty REAL NOT NULL,
  due_at INTEGER NOT NULL,
  last_review_at INTEGER NOT NULL,
  reps INTEGER NOT NULL,
  lapses INTEGER NOT NULL,
  introduced_at INTEGER NOT NULL,
  known_dots INTEGER NOT NULL,
  last_grade INTEGER NOT NULL
);
CREATE TABLE review_log(
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  card_id TEXT NOT NULL,
  character_id TEXT NOT NULL,
  reviewed_at INTEGER NOT NULL,
  grade INTEGER NOT NULL,
  interval_days REAL NOT NULL,
  s_long REAL NOT NULL,
  s_short REAL NOT NULL,
  difficulty REAL NOT NULL,
  reverted INTEGER NOT NULL DEFAULT 0,
  elapsed_ms INTEGER NOT NULL DEFAULT 0,
  task TEXT NOT NULL DEFAULT 'WRITE',
  strokes INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX card_due ON card(due_at);
CREATE INDEX log_card ON review_log(card_id);
CREATE INDEX log_char ON review_log(character_id);
CREATE INDEX log_time ON review_log(reviewed_at);
CREATE TABLE xp_log(card_id TEXT NOT NULL, day TEXT NOT NULL, xp INTEGER NOT NULL, PRIMARY KEY(card_id, day));
CREATE TABLE player(id INTEGER PRIMARY KEY CHECK(id=1), xp INTEGER NOT NULL);
CREATE TABLE note(character_id TEXT PRIMARY KEY, text TEXT NOT NULL, updated_at INTEGER NOT NULL);
CREATE TABLE confusion(seen_id TEXT NOT NULL, wrote_id TEXT NOT NULL, count INTEGER NOT NULL, last_at INTEGER NOT NULL, PRIMARY KEY(seen_id, wrote_id));
CREATE TABLE word_practice(
  mode TEXT NOT NULL,
  word TEXT NOT NULL,
  reading TEXT NOT NULL,
  meaning TEXT NOT NULL,
  practiced_at INTEGER NOT NULL,
  completions INTEGER NOT NULL,
  mature INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY(mode, word)
);
"""


@dataclass
class ScenarioState:
    name: str
    db_path: Path | None
    settings: dict
    profile: dict
    metadata: dict = field(default_factory=dict)


def study_day(now_ms: int, tz_name: str, start_hour: int = 4):
    try:
        tz = ZoneInfo(tz_name) if tz_name else timezone.utc
    except Exception:
        tz = timezone.utc
    local = datetime.fromtimestamp(now_ms / 1000, tz)
    boundary = local.replace(hour=start_hour, minute=0, second=0, microsecond=0)
    if local < boundary:
        local = (local - timedelta(days=1)).replace(hour=start_hour, minute=0, second=0, microsecond=0)
    return local.date(), tz


def _characters(mode: str, limit: int = 2000, unique_glyph: bool = False):
    db = sqlite3.connect(CONTENT)
    try:
        rows = db.execute(
            "SELECT character, id, glyph_group_id FROM character WHERE mode=? ORDER BY list_index LIMIT ?",
            (mode, limit),
        ).fetchall()
        if not unique_glyph:
            return rows
        out, seen = [], set()
        for row in rows:
            if row[2] in seen:
                continue
            seen.add(row[2])
            out.append(row)
        return out
    finally:
        db.close()


def _words(mode: str, include_mature: bool, limit: int = 4):
    db = sqlite3.connect(CONTENT)
    try:
        mature = "1" if include_mature else "0"
        return db.execute(
            "SELECT word, reading, meaning, mature FROM word_context "
            "WHERE mode=? AND (mature=0 OR mature=?) "
            "ORDER BY common DESC, frequency IS NULL, frequency LIMIT ?",
            (mode, mature, limit),
        ).fetchall()
    finally:
        db.close()


def java_string_hash(text: str) -> int:
    value = 0
    for char in text:
        value = (31 * value + ord(char)) & 0xFFFFFFFF
    return value - 0x100000000 if value >= 0x80000000 else value


def _settings(**overrides) -> dict:
    settings = {
        "uiLanguage": "ENGLISH",
        "themeMode": "SYSTEM",
        "studyMode": "SIMPLIFIED",
        "reduceMotion": False,
        "soundEffects": True,
        "readAloud": True,
        "pinyinNumbers": True,
        "pronunciationPractice": False,
        "meaningPractice": False,
        "ankiWriting": False,
        "ankiPronunciation": False,
        "ankiMeaning": False,
        "meaningSynonyms": {},
        "includeHiragana": True,
        "includeKatakana": True,
        "includeHangul": True,
        "showHangulRomanization": True,
        "includeMatureContent": False,
        "onlyAgainAndGood": False,
        "showGradeIntervals": False,
        "wifiOnlyModels": True,
        "vacationStartedAtMs": 0,
        "easeBackUntilMs": 0,
        "guideMode": "FADE",
        "guideInRecall": False,
        "recallCue": "MEANING_READING",
        "mnemonicMode": "EARLY",
        "onboarded": True,
        "showCounters": True,
        "remindersEnabled": False,
        "reminderHour": 20,
        "reminderDays": [1, 2, 3, 4, 5, 6, 7],
        "gridStyle": "MI_ZI",
        "showStrokeNumbers": True,
        "writingSize": "COMFORTABLE",
        "inkWidth": "BOLD",
        "drawingTool": "INK",
        "allowFingerDrawing": False,
        "orientation": "AUTO",
        "dailyNewGoal": 5,
        "dayStartHour": 4,
        "characterOrder": "CURRICULUM",
        "reviewOrder": "RANDOM",
        "sessionOrder": "MIXED",
        "mixInterval": 5,
        "sessionProgress": "NEXT_LESSON",
        "shareIdenticalGlyphs": False,
        "sharingOverrides": {},
        "priorityIds": [],
        "skippedUntil": {},
        "suspendedIds": [],
        "studyList": [],
        "fsrsWeights": [],
        "fsrsWeightsSource": "",
        "fsrsWeightsUpdatedAtMs": 0,
        "fsrsProfiles": {},
    }
    settings.update(overrides)
    return settings


def build_state(
    name: str,
    now_ms: int,
    tz_name: str,
    *,
    reviews: int = 0,
    new: int = 5,
    known: int = 0,
    learning: int = 0,
    days: int = 0,
    leeches: int = 0,
    lapses: int = 0,
    skipped: int = 0,
    suspended: int = 0,
    study_list: int = 0,
    priority: int = 0,
    mode: str = "SIMPLIFIED",
    theme: str = "SYSTEM",
    reduce_motion: bool = False,
    shared_glyphs: bool = False,
    mature: bool = False,
    goal: int | None = None,
    vacation: bool = False,
    ease_back: bool = False,
    review_order: str = "RANDOM",
    session_order: str = "MIXED",
    session_progress: str = "NEXT_LESSON",
    mix_interval: int = 5,
    pronunciation: bool = False,
    meaning_practice: bool = False,
    anki_writing: bool = False,
    anki_pronunciation: bool = False,
    anki_meaning: bool = False,
    meaning_synonyms: dict | None = None,
    task_mode: str = "BOTH",
    guides_only: bool = False,
    onboarded: bool = True,
    seed: int = 7,
) -> ScenarioState:
    QA.mkdir(exist_ok=True)
    rng = random.Random(seed)
    mode = mode.upper()
    rows = _characters(mode, unique_glyph=shared_glyphs)
    if guides_only:
        guide_db = sqlite3.connect(CONTENT)
        try:
            rows = [
                row for row in rows
                if guide_db.execute(
                    "SELECT 1 FROM glyph_stroke WHERE glyph_group_id=? AND length(points)>0 LIMIT 1",
                    (row[2],),
                ).fetchone()
            ]
        finally:
            guide_db.close()
    if not rows:
        raise SystemExit(f"no content characters for mode {mode}")
    day, tz = study_day(now_ms, tz_name)
    due_count = max(0, reviews)
    history_days = max(days, 1)
    picked = []
    used_cards = set()
    for character, char_id, glyph in rows:
        card_id = f"glyph:{glyph}" if shared_glyphs else f"char:{char_id}"
        if card_id in used_cards:
            continue
        used_cards.add(card_id)
        picked.append((character, char_id, glyph, card_id))
        if len(picked) >= due_count + known + learning:
            break
    if len(picked) < due_count:
        raise SystemExit(f"not enough characters for {due_count} due cards in {mode}")

    cards, logs = [], []
    hidden_due = 0
    hidden_ids = []

    def add_card(entry, index: int, *, due_at: int, known_dots: int, lapses_for: int,
                 reps_floor: int = 1, introduced_at: int | None = None,
                 reps_override: int | None = None,
                 last_at: int | None = None) -> tuple:
        character, char_id, glyph, card_id = entry
        stability = 2.0 + rng.random() * 48.0
        if lapses_for >= 5:
            stability = 1.4 + rng.random() * 3.2
        difficulty = 3.0 + rng.random() * 5.5
        introduced = introduced_at if introduced_at is not None else now_ms - rng.randrange(history_days) * DAY
        introduced = min(introduced, now_ms - DAY)
        last = last_at if last_at is not None else max(introduced, due_at - max(1, int(stability)) * DAY)
        last = min(last, now_ms - 60_000)
        reps = reps_override if reps_override is not None else max(reps_floor, rng.randrange(2, 13))
        card = (
            card_id, char_id, glyph, mode, stability, max(0.4, stability * 0.7), difficulty,
            due_at, last, reps, lapses_for, introduced,
            known_dots, rng.choice([1, 2, 3, 3, 3, 4]),
        )
        cards.append(card)
        log_count = min(8, max(2, reps))
        for step in range(log_count):
            at = introduced + int((now_ms - introduced) * (step + 1) / (log_count + 1))
            if at >= now_ms:
                at = now_ms - (log_count - step) * 60_000
            grade = rng.choices([1, 2, 3, 4], weights=[10, 18, 58, 14])[0]
            if step == log_count - 1 and due_at <= now_ms:
                grade = 3
            task = "PRONOUNCE" if (step + index) % 4 == 0 else "WRITE"
            logs.append((
                card_id, char_id, at, grade,
                max(0.2, stability * (step + 1) / (log_count + 1)),
                max(0.2, stability * 0.8), max(0.4, stability * 0.5), difficulty,
                0, rng.randrange(4_000, 28_000), task,
                0 if task == "PRONOUNCE" else rng.randrange(4, 18),
            ))
        return card_id, char_id

    for index in range(due_count):
        entry = picked[index]
        reps_override = None
        if pronunciation:
            if task_mode.upper() == "PRONOUNCE":
                desired = "PRONOUNCE"
            elif task_mode.upper() == "WRITE":
                desired = "WRITE"
            else:
                desired = "PRONOUNCE" if index % 2 == 0 else "WRITE"
            phase = abs(java_string_hash(entry[1])) % 2
            if desired == "PRONOUNCE":
                reps_override = 1 if phase == 0 else 2
            else:
                reps_override = phase if phase > 0 else 2
        add_card(
            entry,
            index,
            due_at=now_ms - rng.randrange(0, 3 * DAY) - 60_000,
            known_dots=1 if index < max(1, due_count // 8) else 0,
            lapses_for=(5 + index % 3) if index < leeches else (min(lapses, 4) if index < lapses else 0),
            reps_override=reps_override,
            last_at=now_ms - (due_count - index) * 60_000 if pronunciation and review_order.upper() == "OLDEST_FIRST" else None,
        )
    for index in range(known):
        entry = picked[due_count + index]
        add_card(
            entry, due_count + index,
            due_at=now_ms + rng.randrange(2, 80) * DAY,
            known_dots=2,
            lapses_for=0,
            reps_floor=5,
        )
    for index in range(learning):
        entry = picked[due_count + known + index]
        add_card(
            entry, due_count + known + index,
            due_at=now_ms + rng.randrange(1, 10) * 60_000,
            known_dots=0,
            lapses_for=0,
            introduced_at=now_ms - 30 * 60_000,
        )

    # Hidden study tools apply to the current script's characters.
    candidates = [row[1] for row in picked[:due_count]]
    if suspended:
        hidden_ids.extend(candidates[:suspended])
    if skipped:
        hidden_ids.extend(candidates[suspended:suspended + skipped])
    hidden_due = len(set(hidden_ids))
    suspended_ids = candidates[:suspended]
    skipped_ids = candidates[suspended:suspended + skipped]
    skipped_until = {cid: day.isoformat() for cid in skipped_ids}
    priority_ids = candidates[suspended + skipped:suspended + skipped + priority]
    study_ids = candidates[suspended + skipped + priority:suspended + skipped + priority + study_list]

    settings = _settings(
        studyMode=mode,
        themeMode=theme.upper(),
        reduceMotion=reduce_motion,
        pronunciationPractice=pronunciation,
        meaningPractice=meaning_practice,
        ankiWriting=anki_writing,
        ankiPronunciation=anki_pronunciation,
        ankiMeaning=anki_meaning,
        meaningSynonyms=meaning_synonyms or {},
        shareIdenticalGlyphs=shared_glyphs,
        includeMatureContent=mature,
        dailyNewGoal=new if goal is None else goal,
        sessionOrder=session_order.upper(),
        sessionProgress=session_progress.upper(),
        mixInterval=mix_interval,
        onboarded=onboarded,
        reviewOrder=review_order.upper(),
        suspendedIds=suspended_ids,
        skippedUntil=skipped_until,
        priorityIds=priority_ids,
        studyList=study_ids,
        vacationStartedAtMs=(now_ms - 2 * DAY) if vacation else 0,
        easeBackUntilMs=(now_ms + 3 * DAY) if ease_back else 0,
    )
    profile = {"displayName": "QA Learner", "createdAtEpochMs": now_ms - 90 * DAY, "photoPath": None}

    db_path = QA / "states" / f"{name}.db"
    db_path.parent.mkdir(parents=True, exist_ok=True)
    if db_path.exists():
        db_path.unlink()
    db = sqlite3.connect(db_path)
    try:
        db.executescript(SCHEMA)
        db.executemany("INSERT OR REPLACE INTO card VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", cards)
        db.executemany(
            "INSERT INTO review_log(card_id,character_id,reviewed_at,grade,interval_days,"
            "s_long,s_short,difficulty,reverted,elapsed_ms,task,strokes) "
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
            logs,
        )
        xp = {}
        for card in cards:
            for log in logs:
                if log[0] == card[0]:
                    day_key = datetime.fromtimestamp(log[2] / 1000, tz).strftime("%Y-%m-%d")
                    xp[(card[0], day_key)] = 8
        db.executemany("INSERT OR REPLACE INTO xp_log(card_id,day,xp) VALUES(?,?,?)",
                       [(key[0], key[1], value) for key, value in xp.items()])
        db.execute("INSERT OR REPLACE INTO player(id,xp) VALUES(1,?)", (max(0, len(logs) * 6),))
        db.execute("INSERT OR IGNORE INTO player(id,xp) VALUES(1,0)")
        words = _words(mode, mature)
        for index, (word, reading, meaning, is_mature) in enumerate(words):
            db.execute(
                "INSERT OR REPLACE INTO word_practice(mode,word,reading,meaning,practiced_at,completions,mature) "
                "VALUES(?,?,?,?,?,?,?)",
                (mode, word, reading, meaning, now_ms - index * 3_600_000, 2, is_mature),
            )
        if cards:
            first = cards[0][1]
            second = cards[1][1] if len(cards) > 1 else first
            db.execute("INSERT OR REPLACE INTO note(character_id,text,updated_at) VALUES(?,?,?)",
                       (first, "QA note: shape and sound.", now_ms - 60_000))
            db.execute("INSERT OR REPLACE INTO confusion(seen_id,wrote_id,count,last_at) VALUES(?,?,?,?)",
                       (first, second, 3, now_ms - 120_000))
        db.execute(f"PRAGMA user_version={SCHEMA_VERSION}")
        db.commit()
    finally:
        db.close()

    samples = []
    content_db = sqlite3.connect(CONTENT)
    try:
        for character, char_id, glyph, card_id in picked[:due_count]:
            info = content_db.execute(
                "SELECT reading, meaning FROM character WHERE id=? LIMIT 1",
                (char_id,),
            ).fetchone()
            if info:
                samples.append({
                    "character": character,
                    "id": char_id,
                    "reading": info[0],
                    "meaning": info[1],
                })
    finally:
        content_db.close()

    metadata = {
        "due": due_count,
        "samples": samples,
        "visible_due": max(0, due_count - hidden_due),
        "new": 0 if goal == 0 else new,
        "known": known,
        "learning": learning,
        "hidden": hidden_due,
        "mode": mode,
        "shared": shared_glyphs,
        "cards": len(cards),
        "reviews": len(logs),
        "today": day.isoformat(),
    }
    return ScenarioState(name=name, db_path=db_path, settings=settings, profile=profile, metadata=metadata)


def write_settings_files(state: ScenarioState) -> tuple[Path, Path]:
    qa = QA / "settings"
    qa.mkdir(parents=True, exist_ok=True)
    settings_path = qa / f"{state.name}.settings.json"
    profile_path = qa / f"{state.name}.profile.json"
    settings_path.write_text(json.dumps(state.settings, indent=2))
    profile_path.write_text(json.dumps(state.profile, indent=2))
    return settings_path, profile_path


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("name")
    parser.add_argument("--reviews", type=int, default=0)
    parser.add_argument("--new", type=int, default=5)
    parser.add_argument("--known", type=int, default=0)
    parser.add_argument("--learning", type=int, default=0)
    parser.add_argument("--days", type=int, default=0)
    parser.add_argument("--leeches", type=int, default=0)
    parser.add_argument("--lapses", type=int, default=0)
    parser.add_argument("--skipped", type=int, default=0)
    parser.add_argument("--suspended", type=int, default=0)
    parser.add_argument("--study-list", type=int, default=0)
    parser.add_argument("--priority", type=int, default=0)
    parser.add_argument("--mode", default="SIMPLIFIED")
    parser.add_argument("--theme", default="SYSTEM")
    parser.add_argument("--reduce-motion", action="store_true")
    parser.add_argument("--shared-glyphs", action="store_true")
    parser.add_argument("--mature", action="store_true")
    parser.add_argument("--goal", type=int)
    parser.add_argument("--vacation", action="store_true")
    parser.add_argument("--ease-back", action="store_true")
    parser.add_argument("--pronunciation", action="store_true")
    parser.add_argument("--review-order", default="RANDOM")
    parser.add_argument("--task-mode", default="BOTH")
    parser.add_argument("--guides-only", action="store_true")
    args = parser.parse_args()
    now = int(time.time() * 1000)
    state = build_state(
        args.name, now, "UTC",
        reviews=args.reviews, new=args.new, known=args.known, learning=args.learning,
        days=args.days, leeches=args.leeches, lapses=args.lapses, skipped=args.skipped,
        suspended=args.suspended, study_list=args.study_list, priority=args.priority,
        mode=args.mode, theme=args.theme, reduce_motion=args.reduce_motion,
        shared_glyphs=args.shared_glyphs,
        mature=args.mature, goal=args.goal, vacation=args.vacation, ease_back=args.ease_back,
        pronunciation=args.pronunciation, review_order=args.review_order,
        task_mode=args.task_mode, guides_only=args.guides_only,
    )
    settings, profile = write_settings_files(state)
    print(json.dumps({"db": str(state.db_path), "settings": str(settings), "profile": str(profile), "metadata": state.metadata}, indent=2))


def character_reading(character: str, mode: str) -> str | None:
    db = sqlite3.connect(CONTENT)
    try:
        row = db.execute(
            "SELECT reading FROM character WHERE character=? AND mode=? LIMIT 1",
            (character, mode.upper()),
        ).fetchone()
        return row[0] if row else None
    finally:
        db.close()


def character_guide(character: str, mode: str) -> list[list[tuple[float, float]]]:
    db = sqlite3.connect(CONTENT)
    try:
        row = db.execute(
            "SELECT glyph_group_id FROM character WHERE character=? AND mode=? LIMIT 1",
            (character, mode.upper()),
        ).fetchone()
        if not row:
            row = db.execute(
                "SELECT glyph_group_id FROM character WHERE character=? LIMIT 1",
                (character,),
            ).fetchone()
        if not row:
            return []
        strokes = []
        for (_idx, points) in db.execute(
            "SELECT idx, points FROM glyph_stroke WHERE glyph_group_id=? ORDER BY idx",
            (row[0],),
        ):
            if not points:
                continue
            import struct
            values = struct.unpack("<" + "f" * (len(points) // 4), points)
            strokes.append([
                (values[index], values[index + 1])
                for index in range(0, len(values) - 1, 2)
            ])
        return strokes
    finally:
        db.close()


def character_variants(character: str) -> list[str]:
    db = sqlite3.connect(CONTENT)
    try:
        rows = db.execute(
            "SELECT variant FROM char_variant WHERE char=?",
            (character,),
        ).fetchall()
        return [row[0] for row in rows]
    finally:
        db.close()
