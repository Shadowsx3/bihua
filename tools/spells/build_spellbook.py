"""Build composeApp/src/androidMain/assets/spellbook.csv for every supported script.

Run from the repo root: python3 tools/spells/build_spellbook.py

The authored catalog in spell_catalog.py is the source of truth. Each entry is
mapped to the learner's script (Traditional, Japanese kanji, Korean Hanja). If a
concept has no counterpart, a common character with the same idea fills the gap,
so every chapter keeps at least 35 spells.
"""
from __future__ import annotations

import csv
import re
import sqlite3
from collections import Counter, OrderedDict
from pathlib import Path

from spell_catalog import CATEGORY_FAMILIES, FAMILIES, FAMILY_EFFECT

ROOT = Path(__file__).resolve().parents[2]
DB = ROOT / "composeApp/src/androidMain/assets/bihua_content.db"
OUT = ROOT / "composeApp/src/androidMain/assets/spellbook.csv"
PER_CATEGORY = 35
MODES = ("SIMPLIFIED", "TRADITIONAL", "JAPANESE", "KOREAN")

# A few scripts use a different everyday character for a concept. These keep the
# Flame chapter full without stretching an unrelated word into a spell.
MODE_SUPPLEMENT = {
    "JAPANESE": {
        "Flame": [
            ("炎", "fire"), ("燃", "fire"), ("焼", "fire"), ("熱", "fire"),
            ("炉", "fire"), ("灰", "fire"), ("炭", "fire"), ("焦", "fire"),
            ("爆", "fire"), ("赤", "fire"), ("紅", "fire"), ("丹", "fire"),
            ("暖", "fire"), ("暑", "fire"), ("温", "fire"), ("燥", "fire"),
            ("炊", "fire"), ("煙", "fire"), ("災", "fire"), ("熟", "fire"),
            ("窯", "fire"), ("電", "lightning"), ("雷", "lightning"),
            ("震", "lightning"), ("輝", "lightning"),
        ],
    },
    "KOREAN": {
        "Flame": [
            ("炎", "fire"), ("燃", "fire"), ("熱", "fire"), ("燈", "fire"),
            ("燭", "fire"), ("爐", "fire"), ("炭", "fire"), ("爆", "fire"),
            ("赤", "fire"), ("紅", "fire"), ("丹", "fire"), ("暖", "fire"),
            ("暑", "fire"), ("溫", "fire"), ("燥", "fire"), ("火", "fire"),
            ("燒", "fire"), ("烈", "fire"), ("煙", "fire"), ("災", "fire"),
            ("熟", "fire"), ("螢", "fire"), ("辛", "fire"),
            ("電", "lightning"), ("雷", "lightning"), ("震", "lightning"),
        ],
    },
}

CATEGORY_OF = {family: category for category, families in CATEGORY_FAMILIES.items() for family in families}

# Only used to fill a shortfall after the authored catalog is mapped. Matching is
# on whole words, so "well" does not smuggle a character into Tides.
FILL_KEYWORDS = {
    "water": ["water", "liquid", "river", "stream", "sea", "ocean", "lake", "pond", "well", "spring", "wave", "tide", "flow", "swim", "wash", "bathe", "drink", "wet", "tear", "sweat", "milk", "juice", "soup", "pool", "fountain", "waterfall", "flood", "drown", "sink"],
    "ice": ["ice", "frost", "freeze", "frozen", "icicle", "hail", "chill", "glacier"],
    "snow": ["snow", "flake"],
    "rain": ["rain", "drizzle", "umbrella", "dew", "shower", "storm"],
    "fire": ["fire", "flame", "blaze", "burn", "heat", "hot", "warm", "cook", "roast", "bake", "ash", "ember", "candle", "incense", "volcano", "furnace", "forge", "lamp", "torch"],
    "lightning": ["lightning", "thunder", "electric", "shock"],
    "wind": ["wind", "breeze", "gust", "blow", "air", "breath", "fan", "kite", "fly"],
    "cloud": ["cloud", "fog", "mist", "smoke", "vapor", "steam", "haze", "sky"],
    "sun": ["sun", "solar", "noon", "daylight", "dawn", "yang"],
    "moon": ["moon", "lunar", "night", "silver"],
    "light": ["light", "shine", "glow", "bright", "star", "glimmer", "beacon"],
    "shadow": ["shadow", "shade", "dark", "black", "eclipse"],
    "earth": ["earth", "soil", "ground", "land", "dirt", "mud", "clay", "sand", "dust", "field", "farm", "garden", "desert", "valley", "plain", "grave", "tomb"],
    "stone": ["stone", "rock", "cliff", "boulder", "pebble", "brick", "wall", "cave", "quarry", "jade", "gem"],
    "mountain": ["mountain", "hill", "peak", "ridge", "summit", "slope"],
    "wood": ["wood", "tree", "forest", "bamboo", "grass", "flower", "leaf", "root", "branch", "seed", "fruit", "grain", "rice", "plant", "green"],
    "metal": ["metal", "gold", "silver", "iron", "copper", "steel", "coin", "money", "treasure", "jade", "bell"],
    "animal": ["animal", "horse", "bird", "fish", "dog", "cat", "tiger", "rabbit", "turtle", "frog", "bee", "insect", "snake", "dragon", "ox", "cow", "sheep", "pig", "fox", "bear", "deer", "wolf", "mouse", "monkey", "chicken", "duck", "butterfly", "whale", "shark", "lion", "elephant", "bug"],
    "person": ["person", "man", "woman", "child", "people", "human", "friend", "family", "king", "queen", "teacher", "student", "doctor", "body", "face", "eye", "self", "name"],
    "hand": ["hand", "finger", "palm", "grasp", "hold", "pull", "push", "lift", "carry", "touch", "point", "throw", "catch", "hit", "strike"],
    "boat": ["boat", "ship", "sail", "oar", "canoe", "raft", "harbor", "port", "anchor", "voyage", "float", "ferry"],
    "tool": ["tool", "sword", "knife", "blade", "hammer", "axe", "saw", "needle", "wheel", "cart", "plow", "rope", "net", "basket", "pot", "cup", "bowl", "plate", "box", "chest", "door", "gate", "window", "house", "home", "room", "bridge", "road", "city", "tower", "castle", "book", "brush", "pen", "paper", "cloth", "clothes", "shoe", "hat", "machine", "engine", "vehicle", "car"],
    "mind": ["mind", "thought", "think", "idea", "know", "knowledge", "wise", "wisdom", "understand", "remember", "memory", "forget", "decide", "believe", "doubt", "reason", "logic", "truth", "lie", "will", "intention", "attention", "focus", "imagine", "imagination", "conscious"],
    "dream": ["dream", "sleep", "nightmare", "fantasy", "hope", "wish", "desire"],
    "sound": ["sound", "voice", "song", "music", "sing", "speak", "word", "language", "speech", "talk", "say", "tell", "ask", "answer", "call", "read", "write", "poem", "story", "drum", "flute", "piano", "echo", "silence", "whisper", "shout", "laugh", "cry", "weep"],
    "time": ["time", "hour", "minute", "second", "moment", "instant", "clock", "watch", "calendar", "date", "day", "night", "morning", "evening", "midnight", "dusk", "today", "tomorrow", "yesterday", "week", "month", "year", "season", "spring", "summer", "autumn", "winter", "age", "old", "ancient", "new", "young", "early", "late", "begin", "start", "end", "finish", "future", "past", "present", "always", "never", "often", "sometimes", "wait", "long", "short", "quick", "slow", "sudden", "gradual", "forever", "temporary", "era", "period", "century", "decade"],
    "rune": ["rune", "symbol", "sign", "mark", "letter", "glyph", "sigil", "seal", "charm", "spell", "magic", "enchant", "circle", "pentagram"],
    "mirror": ["mirror", "reflect", "reflection", "glass", "lens", "image", "copy", "twin", "double"],
    "key": ["key", "lock", "unlock", "password", "code", "secret"],
    "potion": ["potion", "medicine", "drug", "herb", "brew", "elixir", "poison", "antidote", "heal", "cure", "sick", "illness", "health", "alchemy", "flask", "bottle", "bubble"],
    "illusion": ["illusion", "trick", "deceive", "fake", "pretend", "mirage", "disguise", "mask", "hidden", "invisible", "appear", "disappear", "transform", "shapeshift"],
    "oracle": ["oracle", "fortune", "fate", "destiny", "predict", "prophecy", "divination", "card", "dice", "chance", "luck", "omen", "vision"],
    "spirit": ["spirit", "ghost", "god", "goddess", "deity", "divine", "holy", "sacred", "angel", "demon", "devil", "monster", "fairy", "elf", "ancestor", "shrine", "temple", "prayer", "worship", "ritual", "offering", "bless", "pure", "immortal", "death", "life", "afterlife"],
    "soul": ["soul", "rebirth", "reincarnation", "eternal", "essence", "blood", "heart"],
    "ward": ["ward", "protect", "protection", "shield", "guard", "defense", "safe", "safety", "amulet", "talisman", "barrier", "curse", "hex"],
}


def main() -> None:
    db = sqlite3.connect(DB)
    pools = {}
    for mode in MODES:
        pools[mode] = {
            row[0]: {
                "reading": row[1],
                "meaning": row[2],
                "strokes": row[3],
                "level": row[4],
                "index": row[5],
            }
            for row in db.execute(
                "SELECT character, reading, meaning, stroke_count, level, list_index "
                "FROM character WHERE mode = ? AND stroke_count > 0",
                (mode,),
            )
        }
    traditional = {
        row[0]: (row[1].split() or [row[0]])[0]
        for row in db.execute("SELECT character, traditional FROM character WHERE mode = 'SIMPLIFIED'")
    }

    # The authored catalog, with the first family claiming a repeated character.
    catalog: list[tuple[str, str]] = []
    claimed: set[str] = set()
    for family, chars in FAMILIES.items():
        for char in chars.split():
            if char in claimed:
                continue
            if char not in pools["SIMPLIFIED"]:
                continue
            claimed.add(char)
            catalog.append((char, family))

    rows: list[dict] = []
    report: dict[str, Counter] = {}
    for mode in MODES:
        pool = pools[mode]
        used: set[str] = set()
        mapped_seen: set[str] = set()
        by_category: dict[str, list[tuple[str, str, str]]] = {c: [] for c in CATEGORY_FAMILIES}
        for char, family in catalog:
            candidate = _candidate(mode, char, traditional, pool)
            if candidate is None or candidate in mapped_seen:
                continue
            mapped_seen.add(candidate)
            source = "override" if candidate == char else "variant"
            by_category[CATEGORY_OF[family]].append((candidate, family, source))

        all_mapped = {item[0] for items in by_category.values() for item in items}
        for category, families in CATEGORY_FAMILIES.items():
            items = [item for item in by_category[category] if item[0] not in used]
            selected = _balanced(items, families, PER_CATEGORY)
            # Leftovers from this same chapter are still honest fits; use them
            # before borrowing a character from another chapter.
            for item in items:
                if len(selected) >= PER_CATEGORY:
                    break
                if item[0] not in {chosen[0] for chosen in selected}:
                    selected.append(item)
            if len(selected) < PER_CATEGORY:
                _fill.mode = mode
                selected += _fill(
                    category, families, pool, used | all_mapped, PER_CATEGORY - len(selected)
                )
            for character, family, source in selected[:PER_CATEGORY]:
                if character in used:
                    continue
                used.add(character)
                entry = pool[character]
                rows.append({
                    "script": mode,
                    "character": character,
                    "family": family,
                    "category": category,
                    "effect": f"fx_{family}",
                    "source": source,
                    "note": _note(family, entry["meaning"], character),
                })
            report.setdefault(mode, Counter())[category] = min(len(selected), PER_CATEGORY)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    fields = ["script", "character", "family", "category", "effect", "source", "note"]
    with OUT.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    print(f"wrote {len(rows)} spells to {OUT.relative_to(ROOT)}")
    for mode in MODES:
        counts = report.get(mode, Counter())
        print(f"  {mode}: {sum(counts.values())} · " + " ".join(f"{c}={counts[c]}" for c in CATEGORY_FAMILIES))
    families = Counter(r["family"] for r in rows)
    empty = [f for f in CATEGORY_OF if families[f] == 0]
    if empty:
        print(f"  families with no spell: {' '.join(empty)}")


def _candidate(mode: str, char: str, traditional: dict[str, str], pool: dict) -> str | None:
    fallback = traditional.get(char, char)
    order = {
        "SIMPLIFIED": (char,),
        "TRADITIONAL": (fallback, char),
        "JAPANESE": (char, fallback),
        "KOREAN": (fallback, char),
    }[mode]
    for candidate in dict.fromkeys(order):
        if candidate in pool:
            return candidate
    return None


def _balanced(items: list[tuple[str, str, str]], families: tuple[str, ...], limit: int) -> list[tuple[str, str, str]]:
    """One from each family in turn, so no chapter becomes a single family."""
    buckets = {family: [item for item in items if item[1] == family] for family in families}
    out: list[tuple[str, str, str]] = []
    while len(out) < limit and any(buckets.values()):
        for family in families:
            if buckets[family]:
                out.append(buckets[family].pop(0))
                if len(out) >= limit:
                    break
    # Preserve the authored order as much as possible for the book's feel.
    order = {item[0]: i for i, item in enumerate(items)}
    return sorted(out, key=lambda item: order.get(item[0], 0))


def _fill(category: str, families: tuple[str, ...], pool: dict, used: set[str], need: int) -> list[tuple[str, str, str]]:
    out: list[tuple[str, str, str]] = []
    for char, family in MODE_SUPPLEMENT.get(_fill.mode, {}).get(category, []):
        if len(out) >= need:
            break
        if char in pool and char not in used and family in families:
            used.add(char)
            out.append((char, family, "fill"))
    target = len(out) + need
    candidates = sorted(
        ((entry["level"], entry["index"], char) for char, entry in pool.items() if char not in used),
        key=lambda row: (row[0], row[1], row[2]),
    )
    cursor = 0
    while len(out) < target and cursor < len(candidates):
        _level, _index, char = candidates[cursor]
        cursor += 1
        meaning = pool[char]["meaning"]
        family = _family_for(category, meaning, families)
        if family is None:
            continue
        used.add(char)
        out.append((char, family, "fill"))
    return out


def _family_for(category: str, meaning: str, families: tuple[str, ...]) -> str | None:
    text = " " + (meaning or "").lower() + " "
    for family in families:
        for word in FILL_KEYWORDS.get(family, []):
            if re.search(r"(?<![a-z])" + re.escape(word) + r"(?![a-z])", text):
                return family
    return None


# A few dictionary glosses are adult, clinical, or simply not a spell. Keep the
# page readable and kind without changing what the character actually means.
NOTE_OVERRIDES = {
    "煙": "Smoke.",
    "爆": "Burst.",
    "炮": "Firecracker.",
    "藥": "Remedy.",
    "飲": "Drink.",
    "歲": "Year.",
    "慢": "Slow.",
    "問": "Ask.",
    "秒": "Second.",
    "呼": "Breathe.",
    "凝": "Freeze.",
    "反": "Reverse.",
    "固": "Solid.",
    "浪": "Wave.",
    "龍": "Dragon.",
    "琴": "Lute.",
    "尺": "Ruler.",
    "杯": "Cup.",
    "陣": "Array.",
    "拜": "Greet.",
    "等": "Equal.",
    "钥": "Key.",
    "月": "Moon.",
    "涼": "Cool.",
    "米": "Rice.",
    "符": "Talisman.",
    "園": "Garden.",
    "場": "Field.",
    "掌": "Palm.",
    "晒": "Sun-dry.",
    "提": "Lift.",
    "初": "First.",
    "布": "Cloth.",
    "江": "River.",
    "綱": "Rope.",
    "環": "Ring.",
    "蛍": "Firefly.",
    "卜": "Fortune-telling.",
    "識": "Discern.",
    "推": "Guess.",
    "恒": "Steady.",
    "療": "Healing.",
    "疗": "Healing.",
    "久": "Long.",
}


def _first_segment(meaning: str) -> str:
    """The first gloss, without splitting inside parentheses or a qualifier."""
    text = (meaning or "").split(";")[0]
    depth = 0
    for index, char in enumerate(text):
        if char == "(":
            depth += 1
        elif char == ")":
            depth = max(0, depth - 1)
        elif char == "," and depth == 0:
            text = text[:index]
            break
    # Register and domain labels ("(bound form)", "(literary)", "(of the sun)")
    # are dictionary furniture, not the page's meaning. Units and chemical
    # notes are clutter too.
    text = re.sub(r"^\s*\([^)]*\)\s*", "", text)
    text = re.sub(r"\s*\([^)]*\)", "", text)
    return text.split("(", 1)[0].strip()


def _note(family: str, meaning: str, character: str = "") -> str:
    first = NOTE_OVERRIDES.get(character, _first_segment(meaning)).rstrip(".").strip()
    first = first.removeprefix("to ").removeprefix("a ").removeprefix("an ").removeprefix("the ")
    if not first or first.lower().startswith(("surname ", "classifier for ")):
        first = family.replace("-", " ")
    if first:
        return f"{first[0].upper() + first[1:]}."
    return f"A {family} page."


if __name__ == "__main__":
    main()
