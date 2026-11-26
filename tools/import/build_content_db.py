#!/usr/bin/env python3
"""Build bihua_content.db from the downloaded open datasets.

No character list is hardcoded in the app. This script reads the raw files in
tools/data/raw, builds one read-only SQLite database, and copies it into the
app's assets. The app opens it as content; user progress lives elsewhere.

Sources:
  - Gushi HSK 3.0 character and word lists (CC BY-SA 4.0)
  - ivankra/hsk30 (MIT), for Traditional forms and the writing set
  - Make Me a Hanzi medians (Arphic PL / LGPL)
  - KanjiVG paths (CC BY-SA 3.0)
  - KANJIDIC2 (CC BY-SA 4.0), Jōyō grades and Japanese readings
  - Unihan (Unicode license), Korean readings and variant links
  - CC-CEDICT (CC BY-SA 4.0), Traditional readings and word meanings
  - Korean Wiktionary extract (CC BY-SA 3.0 / GFDL), Korean words and Hanja forms
  - FrequencyWords Korean list (CC BY-SA 4.0), word frequency for examples
"""
from __future__ import annotations

import argparse
import csv
import gzip
import hashlib
import json
import os
import re
import sqlite3
import struct
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / "tools/data/raw"
DEFAULT_OUT = ROOT / "app/src/main/assets/bihua_content.db"

MODE_ORDER = ["SIMPLIFIED", "TRADITIONAL", "JAPANESE", "KOREAN"]
MODE_LABEL = {
    "SIMPLIFIED": "HSK",
    "TRADITIONAL": "HSK",
    "JAPANESE": "Jōyō",
    "KOREAN": "Hanja",
}


@dataclass
class CharacterRow:
    id: str
    char: str
    mode: str
    level: int
    level_label: str
    list_index: int
    reading: str
    reading_alt: str
    meaning: str
    traditional: str
    simplified: str
    is_writing: bool
    frequency: int | None


def codepoint(char: str) -> str:
    return f"U+{ord(char):04X}"


def character_id(mode: str, char: str) -> str:
    return f"{mode}:{codepoint(char)}"


def pinyin_marks(syllable: str) -> str:
    """Convert a CC-CEDICT numbered syllable (ni3) to tone marks (nǐ)."""
    syllable = syllable.strip().replace("u:", "ü").replace("v", "ü")
    match = re.match(r"^([a-zA-Zü]+)([1-5])?$", syllable)
    if not match:
        return syllable
    body, tone_text = match.group(1), match.group(2)
    tone = int(tone_text) if tone_text else 5
    if tone == 5:
        return body
    vowels = {
        "a": {"1": "ā", "2": "á", "3": "ǎ", "4": "à"},
        "e": {"1": "ē", "2": "é", "3": "ě", "4": "è"},
        "i": {"1": "ī", "2": "í", "3": "ǐ", "4": "ì"},
        "o": {"1": "ō", "2": "ó", "3": "ǒ", "4": "ò"},
        "u": {"1": "ū", "2": "ú", "3": "ǔ", "4": "ù"},
        "ü": {"1": "ǖ", "2": "ǘ", "3": "ǚ", "4": "ǜ"},
    }
    lower = body.lower()
    if "a" in lower:
        index = lower.index("a")
    elif "e" in lower:
        index = lower.index("e")
    elif "ou" in lower:
        index = lower.index("o")
    else:
        index = max((i for i, c in enumerate(lower) if c in "aeiouü"), default=len(lower) - 1)
    if index < 0 or index >= len(body):
        return body
    target = body[index]
    marked = vowels.get(target.lower(), {}).get(str(tone), target)
    return body[:index] + marked + body[index + 1 :]


def pinyin_phrase(numbered: str) -> str:
    return " ".join(pinyin_marks(part) for part in numbered.split())


def normalize_mmh(medians: list[list[list[float]]]) -> list[list[tuple[float, float]]]:
    """Flip Make Me a Hanzi's y-up medians and fit them to a centered 0.84 square."""
    flipped = [
        [(float(x), 1024.0 - float(y)) for x, y in stroke]
        for stroke in medians
    ]
    all_points = [point for stroke in flipped for point in stroke]
    if not all_points:
        return []
    min_x = min(p[0] for p in all_points)
    max_x = max(p[0] for p in all_points)
    min_y = min(p[1] for p in all_points)
    max_y = max(p[1] for p in all_points)
    width = max(max_x - min_x, 1.0)
    height = max(max_y - min_y, 1.0)
    scale = 0.84 / max(width, height)
    center_x = (min_x + max_x) / 2.0
    center_y = (min_y + max_y) / 2.0
    return [
        [
            (
                round(0.5 + (x - center_x) * scale, 5),
                round(0.5 + (y - center_y) * scale, 5),
            )
            for x, y in stroke
        ]
        for stroke in flipped
    ]


def points_hash(strokes: list[list[tuple[float, float]]]) -> str:
    payload = b"".join(
        struct.pack("<ff", x, y)
        for stroke in strokes
        for x, y in stroke
    )
    return hashlib.sha256(payload).hexdigest()[:16]


def path_hash(paths: list[str]) -> str:
    return hashlib.sha256("|".join(paths).encode("utf-8")).hexdigest()[:16]


def points_blob(strokes: list[list[tuple[float, float]]]) -> bytes:
    flat = [value for stroke in strokes for point in stroke for value in point]
    return struct.pack(f"<{len(flat)}f", *flat) if flat else b""


def load_mmh() -> dict[str, list[list[tuple[float, float]]]]:
    result: dict[str, list[list[tuple[float, float]]]] = {}
    path = RAW / "makemeahanzi-graphics.txt"
    with path.open(encoding="utf-8") as handle:
        for line in handle:
            if not line.strip():
                continue
            row = json.loads(line)
            character = row.get("character")
            medians = row.get("medians")
            if character and medians:
                result[character] = normalize_mmh(medians)
    return result


def load_mmh_paths() -> dict[str, list[str]]:
    result: dict[str, list[str]] = {}
    with (RAW / "makemeahanzi-graphics.txt").open(encoding="utf-8") as handle:
        for line in handle:
            if not line.strip():
                continue
            row = json.loads(line)
            character = row.get("character")
            if character and row.get("strokes"):
                result[character] = row["strokes"]
    return result


def load_kanjivg() -> dict[str, list[str]]:
    result: dict[str, list[str]] = {}
    directory = RAW / "kanjivg/kanji"
    if not directory.exists():
        return result
    for path in directory.glob("*.svg"):
        stem = path.stem
        if "-" in stem:
            continue
        try:
            character = chr(int(stem, 16))
        except ValueError:
            continue
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        paths = [
            element.attrib["d"]
            for element in tree.getroot().iter()
            if element.tag.endswith("path") and "d" in element.attrib
        ]
        if paths:
            result[character] = paths
    return result


def load_unihan() -> tuple[
    dict[str, list[str]],
    dict[str, list[str]],
    dict[str, str],
    dict[str, list[str]],
    dict[str, list[str]],
]:
    korean: dict[str, list[str]] = {}
    variants: dict[str, list[str]] = defaultdict(list)
    traditional: dict[str, list[str]] = defaultdict(list)
    simplified: dict[str, list[str]] = defaultdict(list)
    readings: dict[str, str] = {}
    readings_path = RAW / "unihan/Unihan_Readings.txt"
    if readings_path.exists():
        for line in readings_path.open(encoding="utf-8"):
            if line.startswith("#") or not line.strip():
                continue
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 3:
                continue
            code, field, value = parts[0], parts[1], parts[2]
            try:
                character = chr(int(code[2:], 16))
            except ValueError:
                continue
            if field == "kKorean":
                korean[character] = value.split()
    variants_path = RAW / "unihan/Unihan_Variants.txt"
    if variants_path.exists():
        for line in variants_path.open(encoding="utf-8"):
            if line.startswith("#") or not line.strip():
                continue
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 3:
                continue
            code, field, value = parts[0], parts[1], parts[2]
            if field not in {"kTraditionalVariant", "kSimplifiedVariant", "kZVariant"}:
                continue
            try:
                character = chr(int(code[2:], 16))
            except ValueError:
                continue
            for item in value.split():
                if item.startswith("U+"):
                    try:
                        variant = chr(int(item[2:], 16))
                    except ValueError:
                        continue
                    variants[character].append(variant)
                    if field == "kTraditionalVariant":
                        traditional[character].append(variant)
                    elif field == "kSimplifiedVariant":
                        simplified[character].append(variant)
    return korean, variants, readings, traditional, simplified


def load_all_variants() -> dict[str, set[str]]:
    """Every Unihan variant relation, used for recognition matching and search."""
    result: dict[str, set[str]] = defaultdict(set)
    path = RAW / "unihan/Unihan_Variants.txt"
    if not path.exists():
        return result
    fields = {
        "kTraditionalVariant", "kSimplifiedVariant", "kZVariant",
        "kSemanticVariant", "kJapaneseVariant", "kJapaneseNewVariant",
    }
    for line in path.open(encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        parts = line.rstrip("\n").split("\t")
        if len(parts) < 3 or parts[1] not in fields:
            continue
        try:
            char = chr(int(parts[0][2:], 16))
        except ValueError:
            continue
        for item in parts[2].split():
            if not item.startswith("U+"):
                continue
            try:
                variant = chr(int(item[2:].split("<")[0], 16))
            except ValueError:
                continue
            result[char].add(variant)
            result[variant].add(char)
    return result


# Explicit sexual or crude vocabulary. Word examples keep a safe sense by
# default; a word whose every sense is explicit is tagged mature and hidden
# until the learner turns on mature words.
EXPLICIT_GLOSS = re.compile(
    r"\b(?:sexual(?:ly)?|sex organs?|genital(?:ia|s)?|penile|vaginal|semen|sperm|"
    r"penis|vagina|testicles?|intercourse|fornicat\w*|prostitut\w*|whore\w*|"
    r"slut\w*|brothel|rape|molest\w*|masturbat\w*|orgasm\w*|pornograph\w*|"
    r"erotic\w*|pussy\w*|fuck\w*|shit\w*|cunt\w*|dick\w*|bitch\w*)\b",
    re.IGNORECASE,
)


# Word examples also mark milder adult vocabulary as mature. Character
# meanings keep the narrower explicit list so ordinary senses stay readable.
MATURE_GLOSS = re.compile(
    r"\b(?:sexual(?:ly)?|sex organs?|genital(?:ia|s)?|penile|vaginal|semen|sperm|"
    r"penis|vagina|testicles?|intercourse|fornicat\w*|prostitut\w*|whore\w*|"
    r"slut\w*|brothel|rape|molest\w*|masturbat\w*|orgasm\w*|pornograph\w*|"
    r"erotic\w*|pussy\w*|fuck\w*|shit\w*|cunt\w*|dick\w*|bitch\w*|"
    r"lewd\w*|obscene\w*|lust|lustful|adulter\w*|debauch\w*|naked\w*|nude|"
    r"coition|carnal)\b",
    re.IGNORECASE,
)


def tidy_gloss(text: str, limit: int) -> str:
    """Remove remaining dictionary notation and never leave a half sentence."""
    text = re.sub(r"([\u3400-\u9fff]+)\|[\u3400-\u9fff]+", r"\1", text)
    text = re.sub(r"\s*\[[^\]]*\]", "", text)
    text = re.sub(r"\s*\{[^}]*\}", "", text)
    text = text.replace("\u201c", "").replace("\u201d", "").replace('"', "")
    text = re.sub(r"\s+\+\s+", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    if len(text) > limit:
        cut = text[:limit]
        for separator in ("; ", ", ", " "):
            index = cut.rfind(separator)
            if index > limit * 0.6:
                cut = cut[:index]
                break
        text = cut
    kept: list[str] = []
    opens: list[int] = []
    for char in text:
        if char == "(":
            opens.append(len(kept))
            kept.append(char)
        elif char == ")":
            if opens:
                opens.pop()
                kept.append(char)
            # An unmatched close would leave a half parenthetical.
        else:
            kept.append(char)
    if opens:
        kept = kept[:opens[0]]
    text = "".join(kept)
    return text.strip(" ;,|\"'")


def sanitize_word_meaning(text: str, limit: int = 90) -> tuple[str, bool]:
    """Learner-facing word gloss plus a mature flag.

    Regional and classifier noise is removed. Explicit senses are dropped when
    a safe sense remains. A word whose every sense is explicit keeps its gloss
    but is marked mature, so the app can hide it by default.
    """
    text = re.sub(r"\[[^\]]*\]", "", text)
    parts = [part.strip() for part in text.split(";") if part.strip()]
    if not parts:
        return "", False
    plain: list[str] = []
    explicit: list[str] = []
    for part in parts:
        if MATURE_GLOSS.search(part):
            explicit.append(part)
            continue
        if part.lower().startswith("fig.") or part.lower().startswith("lit."):
            break
        if (
            part.startswith("CL:")
            or "variant of" in part
            or "vulgar" in part.lower()
        ):
            continue
        cleaned = re.sub(r"\s*\([^)]*\)", "", part).strip()
        cleaned = re.sub(r"\s+", " ", cleaned)
        if cleaned:
            plain.append(cleaned)
        if len(plain) == 2:
            break
    if plain:
        return tidy_gloss("; ".join(plain), limit), False
    if explicit:
        cleaned = re.sub(r"\s*\([^)]*\)", "", explicit[0]).strip()
        return tidy_gloss(cleaned or explicit[0], limit), True
    return "", False


def clean_meaning(text: str, limit: int = 140) -> str:
    """Keeps useful senses and removes dictionary references and metadata.

    Character meanings are reference text, so several senses are welcome. The
    raw sources mix in classifier lists, pinyin brackets, variant notes, and long
    Taiwan/etymology asides; those are not learner-facing glosses. Word glosses
    use sanitize_word_meaning and stay shorter.
    """
    text = text.strip()
    # Pinyin references such as 魚[yu2] and classifier lists such as
    # (CL:條|条[tiao2],尾[wei3]) are dictionary apparatus, not meaning.
    text = re.sub(r"\s*\[[^\]]*\]", "", text)
    text = re.sub(r"\s*\(CL:[^)]*\)", "", text, flags=re.I)
    # Variant/abbreviation/Taiwan asides are not the character's own meaning.
    text = re.sub(
        r"\s*\([^)]*(?:variant of|old variant|abbr\. for|see\s|In Taiwan|e\.g\.)[^)]*\)",
        "",
        text,
        flags=re.I,
    )
    # Traditional|simplified pairs become a single form.
    text = re.sub(r"([\u3400-\u9fff]+)\|[\u3400-\u9fff]+", r"\1", text)
    text = re.sub(r"\s+", " ", text).strip()
    parts = [part.strip(" ,") for part in text.split(";") if part.strip()]
    kept: list[str] = []
    fallback: list[str] = []
    for part in parts:
        low = part.lower()
        if (
            low.startswith(("variant of", "old variant", "abbr.", "see ", "used in", "cl:"))
            or "radical in chinese characters" in low
            or low.startswith(("taiwan pr.", "taiwanese pr."))
            or "vulgar" in low
            or EXPLICIT_GLOSS.search(part)
        ):
            if EXPLICIT_GLOSS.search(part):
                continue
            fallback.append(part)
        else:
            kept.append(part)
    chosen = kept or fallback
    joined = "; ".join(chosen)
    if not joined:
        joined = re.sub(r"\s*\([^)]*\)", "", text).strip()
    return tidy_gloss(joined, limit)


def sanitize_japanese_meaning(text: str, limit: int = 180) -> tuple[str, bool]:
    """KANJIDIC meanings without radical notes; explicit senses become mature."""
    parts = [part.strip() for part in text.split(";") if part.strip()]
    safe = [
        part for part in parts
        if "radical (no." not in part.lower() and not MATURE_GLOSS.search(part)
    ]
    if safe:
        return tidy_gloss("; ".join(safe), limit), False
    if parts:
        return tidy_gloss(parts[0], limit), True
    return "", False


def clean_korean_gloss(text: str, limit: int = 80) -> str:
    """Trim a Korean-English gloss to the short, learner-facing form."""
    return tidy_gloss(text, limit)


# Rare entries where the source gloss is empty. Keep these explicit and short;
# never invent an etymology or a meaning from the glyph alone.
MEANING_OVERRIDES = {
    "𮕩": "to decline; to weaken",
}


def meaning_quality(text: str) -> float:
    low = text.strip().lower()
    if not low:
        return -1.0
    if low.startswith(("used in", "variant of", "old variant", "erhua variant")):
        return 0.0
    score = 1.0
    if low.startswith("abbr."):
        score = 0.2
    if low.startswith("(tw)") or low.startswith("(prc)") or low.startswith("(cc)"):
        score = 0.5
    return score + min(len(text), 200) / 1000.0


def load_cedict() -> tuple[
    dict[str, tuple[str, str]],
    dict[str, tuple[str, str]],
    set[str],
    set[str],
    dict[str, str],
]:
    singles: dict[str, tuple[str, str]] = {}
    words: dict[str, tuple[str, str]] = {}
    traditional_singles: set[str] = set()
    simplified_singles: set[str] = set()
    simplified_words: dict[str, str] = {}
    path = RAW / "cedict_1_0_ts_utf-8_mdbg.txt"
    if not path.exists():
        return singles, words, traditional_singles, simplified_singles, simplified_words, simplified_words
    pattern = re.compile(r"^(\S+)\s+(\S+)\s+\[([^\]]+)\]\s+/(.*)/$")
    for line in path.open(encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        match = pattern.match(line.strip())
        if not match:
            continue
        traditional, simplified, numbered, definition = match.groups()
        reading = pinyin_phrase(numbered)
        meaning = "; ".join(part for part in definition.split("/") if part)[:180]
        if (
            traditional not in words
            or meaning_quality(meaning) > meaning_quality(words[traditional][1])
        ):
            words[traditional] = (reading, meaning)
        simplified_words.setdefault(simplified, traditional)
        if len(traditional) == 1:
            if (
                traditional not in singles
                or meaning_quality(meaning) > meaning_quality(singles[traditional][1])
            ):
                singles[traditional] = (reading, meaning)
            traditional_singles.add(traditional)
        if len(simplified) == 1:
            if (
                simplified not in singles
                or meaning_quality(meaning) > meaning_quality(singles[simplified][1])
            ):
                singles[simplified] = (reading, meaning)
            simplified_singles.add(simplified)
    return singles, words, traditional_singles, simplified_singles, simplified_words


def load_japanese_frequency() -> dict[str, int]:
    """Word frequency rank from FrequencyWords, if the Japanese list is present."""
    path = RAW / "ja_50k.txt"
    if not path.exists():
        return {}
    ranks: dict[str, int] = {}
    with path.open(encoding="utf-8") as handle:
        for index, line in enumerate(handle):
            parts = line.split()
            if len(parts) == 2 and parts[0] not in ranks:
                ranks[parts[0]] = index
    return ranks


def load_jmdict_words(kanji: set[str], per_kanji: int = 3) -> dict[str, tuple[str, str, str, bool]]:
    """Common Japanese words keyed by word: (reading, meaning, kanji string).

    JMdict priority tags come first, then shorter words. This matters because
    the entry order is not a frequency order and can otherwise surface rare
    compounds for a beginner character.
    """
    path = RAW / "JMdict_e"
    if not path.exists():
        return {}
    frequency = load_japanese_frequency()
    candidates: dict[str, list[tuple[bool, int, str, str, str, int | None]]] = defaultdict(list)
    priority_tags = {"news1", "news2", "ichi1", "ichi2"}
    context = ET.iterparse(path, events=("end",))
    for _, element in context:
        if not element.tag.endswith("entry"):
            continue
        keb = None
        reb = None
        gloss = None
        priority = False
        for child in element:
            tag = child.tag.split("}")[-1]
            if tag == "k_ele":
                for part in child:
                    name = part.tag.split("}")[-1]
                    if name == "keb" and keb is None and part.text:
                        keb = part.text
                    elif name == "ke_pri" and part.text in priority_tags:
                        priority = True
            elif tag == "r_ele" and reb is None:
                for part in child:
                    name = part.tag.split("}")[-1]
                    if name == "reb" and part.text and reb is None:
                        reb = part.text
                    elif name == "re_pri" and part.text in priority_tags:
                        priority = True
            elif tag == "sense" and gloss is None:
                for part in child:
                    if part.tag.split("}")[-1] == "gloss" and part.text:
                        gloss = part.text
                        break
        element.clear()
        if not keb or not reb or not gloss:
            continue
        hit = [char for char in keb if char in kanji]
        if not hit:
            continue
        rank = frequency.get(keb)
        for char in hit:
            candidates[char].append((priority, len(keb), keb, reb, gloss[:80], rank))

    chosen: dict[str, tuple[str, str, str, bool, int | None]] = {}
    for char in sorted(candidates):
        ranked = sorted(
            candidates[char],
            key=lambda row: (
                row[5] if row[5] is not None else (50_000 if row[0] else 10**9),
                not row[0],
                row[1],
                row[2],
            ),
        )
        picked = 0
        for priority, _, keb, reb, gloss, rank in ranked:
            common = priority or (rank is not None and rank <= 30_000)
            previous = chosen.get(keb)
            if previous is not None:
                if common and not previous[3]:
                    chosen[keb] = (previous[0], previous[1], previous[2], True, previous[4])
                continue
            chosen[keb] = (reb, gloss, keb, common, rank)
            picked += 1
            if picked >= per_kanji:
                break
    return chosen

def is_han(char: str) -> bool:
    code = ord(char)
    return (
        0x4E00 <= code <= 0x9FFF
        or 0x3400 <= code <= 0x4DBF
        or 0xF900 <= code <= 0xFAFF
        or 0x20000 <= code <= 0x2FA1F
    )


def load_korean_frequency() -> dict[str, int]:
    """Word frequency rank from FrequencyWords, if the list is present."""
    path = RAW / "ko_50k.txt"
    if not path.exists():
        return {}
    ranks: dict[str, int] = {}
    with path.open(encoding="utf-8") as handle:
        for index, line in enumerate(handle):
            parts = line.split()
            if len(parts) == 2 and parts[0] not in ranks:
                ranks[parts[0]] = index
    return ranks


KOREAN_SKIP_TAGS = {
    "archaic",
    "dated",
    "derogatory",
    "historical",
    "misspelling",
    "nonstandard",
    "obsolete",
    "offensive",
    "rare",
    "slang",
    "vulgar",
}
KOREAN_BAD_GLOSS = re.compile(
    r"\b(?:abbreviation|alternative form|clipping|form of|misspelling|"
    r"nonstandard|romanization|short for|synonym of|used in)\b",
    re.IGNORECASE,
)
KOREAN_BINOMIAL = re.compile(r"\([A-Z][a-z]+ [a-z]+")
KOREAN_COMMON_RANK = 10_000


def load_korean_words(hanja: set[str]) -> list[tuple[str, str, str, bool, bool, int]]:
    """Korean word examples: (hanja, hangul, gloss, common, mature, rank).

    The Korean Wiktionary extract gives real Korean headwords with their Hanja
    spellings. Only nouns whose Hanja spelling is fully covered by the Korean
    education list are kept, so every example can be opened on the writing
    canvas. The frequency list decides which examples are common enough to lead
    a character card.
    """
    path = RAW / "kaikki-korean.jsonl.gz"
    if not path.exists():
        return []
    frequency = load_korean_frequency()
    best: dict[str, tuple[int, str, str, str, bool]] = {}
    with gzip.open(path, "rt", encoding="utf-8") as handle:
        for line in handle:
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue
            if entry.get("pos") != "noun":
                continue
            word = entry.get("word", "")
            if not 2 <= len(word) <= 4:
                continue
            if any(not 0xAC00 <= ord(char) <= 0xD7A3 for char in word):
                continue
            forms = [
                form.get("form", "")
                for form in entry.get("forms", [])
                if "hanja" in (form.get("tags") or [])
            ]
            forms = [
                form
                for form in forms
                if form
                and len(form) == len(word)
                and all(is_han(char) for char in form)
                and all(char in hanja for char in form)
            ]
            if not forms:
                continue
            safe = None
            explicit = None
            for sense in entry.get("senses") or []:
                if set(sense.get("tags") or []) & KOREAN_SKIP_TAGS:
                    continue
                for raw in sense.get("glosses") or []:
                    if KOREAN_BAD_GLOSS.search(raw) or KOREAN_BINOMIAL.search(raw):
                        continue
                    candidate = clean_korean_gloss(re.sub(r"\s*\([^)]*\)", "", raw))
                    if not candidate or len(candidate) > 80:
                        continue
                    if MATURE_GLOSS.search(candidate):
                        if explicit is None:
                            explicit = candidate
                    else:
                        safe = candidate
                        break
                if safe:
                    break
            gloss = safe or explicit
            if not gloss:
                continue
            rank = frequency.get(word, 10**9)
            mature = safe is None
            for form in forms:
                previous = best.get(form)
                if previous is None or (rank, mature) < (previous[0], previous[4]):
                    best[form] = (rank, word, form, gloss, mature)

    ordered = sorted(
        best.values(),
        key=lambda item: (
            item[0] > KOREAN_COMMON_RANK,
            len(item[2]),
            item[0],
            item[1],
            item[2],
        ),
    )
    return [
        (form, word, gloss, rank <= KOREAN_COMMON_RANK, mature, rank)
        for rank, word, form, gloss, mature in ordered
    ]


def load_total_strokes() -> dict[str, int]:
    """kTotalStrokes: the official stroke count, used when no guide exists."""
    result: dict[str, int] = {}
    path = RAW / "unihan" / "Unihan_IRGSources.txt"
    if not path.exists():
        return result
    for line in path.open(encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 3 and parts[1] == "kTotalStrokes":
            first = parts[2].split()[0]
            if first.isdigit():
                result[chr(int(parts[0][2:], 16))] = int(first)
    return result


KANGXI_RADICALS = (
    "一丨丶丿乙亅二亠人儿入八冂冖冫几凵刀力勹匕匚匸十卜卩厂厶又口囗土士夂夊夕大女"
    "子宀寸小尢尸屮山巛工己巾干幺广廴廾弋弓彐彡彳心戈戶手支攴文斗斤方无日曰月木欠"
    "止歹殳毋比毛氏气水火爪父爻爿片牙牛犬玄玉瓜瓦甘生用田疋疒癶白皮皿目矛矢石示禸"
    "禾穴立竹米糸缶网羊羽老而耒耳聿肉臣自至臼舌舛舟艮色艸虍虫血行衣襾見角言谷豆豕"
    "豸貝赤走足身車辛辰辵邑酉釆里金長門阜隶隹雨靑非面革韋韭音頁風飛食首香馬骨高髟"
    "鬥鬯鬲鬼魚鳥鹵鹿麥麻黃黍黑黹黽鼎鼓鼠鼻齊齒龍龜龠"
)


def load_unihan_radicals() -> dict[str, str]:
    """Kangxi radical glyph from kRSUnicode, for characters outside Make Me a Hanzi."""
    result: dict[str, str] = {}
    path = RAW / "unihan" / "Unihan_IRGSources.txt"
    if not path.exists():
        return result
    for line in path.open(encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        parts = line.rstrip("\n").split("\t")
        if len(parts) < 3 or parts[1] != "kRSUnicode":
            continue
        match = re.match(r"(\d+)", parts[2])
        if not match:
            continue
        number = int(match.group(1))
        if not 1 <= number <= len(KANGXI_RADICALS):
            continue
        try:
            char = chr(int(parts[0][2:], 16))
        except ValueError:
            continue
        result[char] = KANGXI_RADICALS[number - 1]
    return result


def load_unihan_grade() -> dict[str, int]:
    """kGradeLevel: the mainland primary-school grade where a character is taught."""
    result: dict[str, int] = {}
    path = RAW / "unihan" / "Unihan_DictionaryLikeData.txt"
    if not path.exists():
        return result
    for line in path.open(encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 3 and parts[1] == "kGradeLevel":
            value = parts[2].strip()
            if value.isdigit():
                result[chr(int(parts[0][2:], 16))] = int(value)
    return result


def load_kanjidic() -> list[tuple[str, int, int, str, str, int | None]]:
    rows: list[tuple[str, int, int, str, str, int | None]] = []
    root = ET.parse(RAW / "kanjidic2.xml").getroot()
    for character in root.findall("character"):
        literal = character.findtext("literal")
        if not literal:
            continue
        misc = character.find("misc")
        grade_text = misc.findtext("grade") if misc is not None else None
        if grade_text not in {"1", "2", "3", "4", "5", "6", "8"}:
            continue
        grade = int(grade_text)
        level = 7 if grade == 8 else grade
        stroke_text = misc.findtext("stroke_count") if misc is not None else None
        strokes = int(stroke_text) if stroke_text and stroke_text.isdigit() else 0
        frequency_text = misc.findtext("freq") if misc is not None else None
        frequency = int(frequency_text) if frequency_text and frequency_text.isdigit() else None
        # KANJIDIC nests readings and meanings under <reading_meaning>.
        on = [
            element.text or ""
            for element in character.iter("reading")
            if element.attrib.get("r_type") == "ja_on"
        ]
        kun = [
            element.text or ""
            for element in character.iter("reading")
            if element.attrib.get("r_type") == "ja_kun"
        ]
        readings = " / ".join((on[:2] + kun[:2]))
        meanings = [
            element.text or ""
            for element in character.iter("meaning")
            if element.attrib.get("m_lang") in {None, "en"}
        ]
        meaning = "; ".join(meanings[:3])
        rows.append((literal, level, strokes, readings, meaning, frequency))
    rows.sort(key=lambda row: (row[1], row[2], ord(row[0])))
    return rows


def load_unihan_korean() -> tuple[set[str], dict[str, str], dict[str, str]]:
    """Korean Hanja education list, readings, and English definitions from Unihan.

    Unihan supplies the 1,800 character education list, kHangul readings, and
    kDefinition glosses. Every value in the Korean course comes from a dataset
    that can be redistributed with the app.
    """
    education: set[str] = set()
    hangul: dict[str, str] = {}
    definitions: dict[str, str] = {}
    zpath = RAW / "unihan/Unihan_OtherMappings.txt"
    if zpath.exists():
        for line in zpath.open(encoding="utf-8"):
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 3 and parts[1] == "kKoreanEducationHanja":
                try:
                    education.add(chr(int(parts[0][2:], 16)))
                except ValueError:
                    continue
    rpath = RAW / "unihan/Unihan_Readings.txt"
    if rpath.exists():
        for line in rpath.open(encoding="utf-8"):
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 3:
                continue
            field, value = parts[1], parts[2]
            try:
                char = chr(int(parts[0][2:], 16))
            except ValueError:
                continue
            if field == "kHangul":
                readings = [item.split(":")[0] for item in value.split() if item.split(":")[0]]
                if readings:
                    hangul[char] = " ".join(dict.fromkeys(readings))
            elif field == "kDefinition":
                definitions[char] = value
    print(f"Unihan Korean education Hanja: {len(education)}, definitions: {len(definitions)}")
    return education, hangul, definitions


IDS_SHAPES = {
    "⿰": "left and right",
    "⿱": "above and below",
    "⿲": "left, middle, and right",
    "⿳": "top, middle, and bottom",
    "⿴": "around the outside",
    "⿵": "around the top",
    "⿶": "around the bottom",
    "⿷": "around the left",
    "⿸": "around the upper left",
    "⿹": "around the upper right",
    "⿺": "around the lower left",
    "⿻": "overlapping",
}


def shape_note(decomposition: str, radical: str) -> str:
    """An honest shape note, never a made-up historical etymology."""
    if decomposition and " " in decomposition:
        return f"Shape: made from {' + '.join(decomposition.split())}."
    parts = [
        char for char in decomposition
        if char not in "⿰⿱⿲⿳⿴⿵⿶⿷⿸⿹⿺⿻？?"
    ]
    if parts:
        shape = IDS_SHAPES.get(decomposition[:1])
        if shape:
            return f"Shape: {shape}, {' + '.join(parts)}."
        return f"Shape: built from {' + '.join(parts)}."
    if radical:
        return f"Shape: organized around the {radical} radical."
    return "Shape: a rare glyph with no simple decomposition."


def load_kanjivg_components(char: str) -> list[str]:
    """Top-level component names from KanjiVG, used only when MMH is silent."""
    path = RAW / "kanjivg" / "kanji" / f"{ord(char):05x}.svg"
    if not path.exists():
        return []
    try:
        root = ET.parse(path).getroot()
    except Exception:
        return []
    code = f"kvg:{ord(char):05x}"
    target = next(
        (node for node in root.iter() if node.tag.endswith("g") and node.get("id") == code),
        None,
    )
    if target is None:
        return []
    parts: list[str] = []

    def collect(parent: ET.Element) -> None:
        for child in list(parent):
            if not child.tag.endswith("g"):
                continue
            element = child.get("{http://kanjivg.tagaini.net}element") or child.get("kvg:element")
            if element and element != char:
                if element not in parts:
                    parts.append(element)
            else:
                collect(child)

    collect(target)
    return parts


def load_mmh_meta() -> dict[str, tuple[str, str, str, str, str, str]]:
    """Radical, decomposition, and etymology hint from Make Me a Hanzi."""
    path = RAW / "makemeahanzi-dictionary.txt"
    if not path.exists():
        print("warning: makemeahanzi-dictionary.txt missing; structure data will be empty")
        return {}
    result: dict[str, tuple[str, str, str, str, str, str]] = {}
    with path.open(encoding="utf-8") as handle:
        for line in handle:
            try:
                row = json.loads(line)
            except json.JSONDecodeError:
                continue
            char = row.get("character")
            if not char:
                continue
            etymology = row.get("etymology") or {}
            hint = etymology.get("hint") or ""
            result[char] = (
                row.get("radical") or "",
                row.get("decomposition") or "",
                hint,
                etymology.get("type") or "",
                etymology.get("phonetic") or "",
                etymology.get("semantic") or "",
            )
    print(f"Make Me a Hanzi structure entries: {len(result)}")
    return result


def clean_pinyin_reading(text: str) -> str:
    """Chinese readings use a consistent no-space syllable stream for display."""
    return text.replace(" ", "").strip()


def build(out_path: Path) -> None:
    print("loading sources")
    mmh = load_mmh()
    mmh_paths = load_mmh_paths()
    kanjivg = load_kanjivg()
    unihan_korean, unihan_variants, _, unihan_traditional, unihan_simplified = load_unihan()
    cedict_single, cedict_words, cedict_traditional, cedict_simplified, cedict_word_pairs = load_cedict()
    mmh_meta = load_mmh_meta()
    unihan_radicals = load_unihan_radicals()
    hsk = json.load((RAW / "hsk30-characters-all.json").open(encoding="utf-8"))["data"]
    hsk_words = json.load((RAW / "hsk30-words-all.json").open(encoding="utf-8"))["data"]
    ivankra = {
        row["Hanzi"]: row
        for row in csv.DictReader((RAW / "ivankra-hsk30-chars.csv").open(encoding="utf-8"))
    }
    kanjidic = load_kanjidic()
    unihan_education, unihan_hangul, unihan_definitions = load_unihan_korean()

    characters: list[CharacterRow] = []
    simp_to_trads: dict[str, list[str]] = {}
    trad_to_simp: dict[str, str] = {}
    writing: set[str] = set()

    # Simplified and Traditional Chinese.
    for index, row in enumerate(hsk):
        char = row["hanzi"]
        source = ivankra.get(char, {})
        is_writing = bool(source.get("WritingLevel", "").strip())
        if is_writing:
            writing.add(char)
        freq_text = source.get("Freq", "").strip()
        frequency = int(freq_text) if freq_text.isdigit() else None
        level = int(row["level"])
        reading = row.get("pinyin", "")
        meaning = clean_meaning(row.get("meaning", ""))
        trads: list[str] = []
        explicit = source.get("Traditional", "").strip()
        if explicit:
            for part in re.split(r"[/\s]+", explicit):
                if part and part not in trads:
                    trads.append(part)
        for variant in unihan_traditional.get(char, []):
            if variant not in trads:
                trads.append(variant)
        if not trads:
            trads = [char]
        # Keep the first occurrence of an ambiguous Traditional form.
        trads = [trad for trad in trads if trad]
        simp_to_trads[char] = trads
        for trad in trads:
            trad_to_simp.setdefault(trad, char)
        characters.append(
            CharacterRow(
                id=character_id("SIMPLIFIED", char),
                char=char,
                mode="SIMPLIFIED",
                level=level,
                level_label=f"HSK {level}",
                list_index=index,
                reading=clean_pinyin_reading(reading),
                reading_alt="",
                meaning=meaning,
                traditional=" ".join(trads),
                simplified=char,
                is_writing=is_writing,
                frequency=frequency,
            )
        )

    hsk_by_char = {
        row["hanzi"]: (row.get("pinyin", ""), clean_meaning(row.get("meaning", "")))
        for row in hsk
    }
    trad_fallback: dict[str, tuple[str, str]] = {}
    for char, trads in simp_to_trads.items():
        source = hsk_by_char.get(char)
        if source is None:
            continue
        for trad in trads:
            if trad != char:
                trad_fallback.setdefault(trad, source)

    # Keep the courses honest: a character that is only a simplified form does
    # not belong in the Traditional course, and a traditional-only form does not
    # belong in the Simplified course. Shared glyphs (日, 人) stay in both.
    simplified_only = {
        char for char, trads in simp_to_trads.items()
        if any(trad != char for trad in trads)
    }
    traditional_only = {
        trad for char, trads in simp_to_trads.items()
        for trad in trads
        if trad != char
    }

    seen_trad: set[str] = set()
    for index, row in enumerate(hsk):
        char = row["hanzi"]
        source = ivankra.get(char, {})
        is_writing = bool(source.get("WritingLevel", "").strip())
        level = int(row["level"])
        for variant_index, trad in enumerate(simp_to_trads.get(char, [char])):
            if trad in seen_trad or trad in simplified_only:
                continue
            if trad not in cedict_traditional:
                continue
            seen_trad.add(trad)
            if trad in hsk_by_char:
                reading, meaning = hsk_by_char[trad]
            else:
                reading, meaning = cedict_single.get(
                    trad,
                    hsk_by_char.get(char, (row.get("pinyin", ""), row.get("meaning", ""))),
                )
                reading = clean_pinyin_reading(reading)
                meaning = clean_meaning(meaning)
            if (
                not meaning
                or meaning.startswith("variant of")
                or meaning.startswith("old variant")
                or meaning.startswith("used in")
                or meaning.startswith("abbr.")
            ):
                fallback = trad_fallback.get(trad) or hsk_by_char.get(trad)
                if fallback is not None:
                    reading, meaning = fallback
                    reading = clean_pinyin_reading(reading)
                else:
                    continue
            characters.append(
                CharacterRow(
                    id=character_id("TRADITIONAL", trad),
                    char=trad,
                    mode="TRADITIONAL",
                    level=level,
                    level_label=f"HSK {level}",
                    list_index=index * 10 + variant_index,
                    reading=reading,
                    reading_alt="",
                    meaning=meaning,
                    traditional=trad,
                    simplified=char,
                    is_writing=is_writing,
                    frequency=None,
                )
            )

    # Non-HSK Chinese: characters taught in mainland primary school (Unihan
    # kGradeLevel) that are not in the HSK character list, with real stroke data
    # from Make Me a Hanzi and CC-CEDICT readings and meanings.
    grades = load_unihan_grade()
    non_hsk: dict[str, int] = {}
    for char, grade in grades.items():
        if char in simp_to_trads:
            continue
        if char not in mmh or char not in cedict_single:
            continue
        if char not in cedict_simplified:
            continue
        non_hsk[char] = grade
    ordered_extra = sorted(
        non_hsk.items(),
        key=lambda item: (item[1], len(mmh.get(item[0], [])), item[0]),
    )
    for index, (char, grade) in enumerate(ordered_extra):
        reading, meaning = cedict_single[char]
        meaning = clean_meaning(meaning)
        characters.append(
            CharacterRow(
                id=character_id("SIMPLIFIED", char),
                char=char,
                mode="SIMPLIFIED",
                level=10,
                level_label="Non-HSK",
                list_index=200_000 + index,
                reading=reading,
                reading_alt="",
                meaning=meaning,
                traditional="",
                simplified=char,
                is_writing=False,
                frequency=None,
            )
        )
    print(f"non-HSK Chinese characters: {len(ordered_extra)}")

    # Japanese kana. KanjiVG carries real stroke paths for hiragana and katakana,
    # so both scripts get an actual stroke guide. Romaji is the reading.
    hiragana = [
        ("あ", "a"), ("い", "i"), ("う", "u"), ("え", "e"), ("お", "o"),
        ("か", "ka"), ("き", "ki"), ("く", "ku"), ("け", "ke"), ("こ", "ko"),
        ("さ", "sa"), ("し", "shi"), ("す", "su"), ("せ", "se"), ("そ", "so"),
        ("た", "ta"), ("ち", "chi"), ("つ", "tsu"), ("て", "te"), ("と", "to"),
        ("な", "na"), ("に", "ni"), ("ぬ", "nu"), ("ね", "ne"), ("の", "no"),
        ("は", "ha"), ("ひ", "hi"), ("ふ", "fu"), ("へ", "he"), ("ほ", "ho"),
        ("ま", "ma"), ("み", "mi"), ("む", "mu"), ("め", "me"), ("も", "mo"),
        ("や", "ya"), ("ゆ", "yu"), ("よ", "yo"),
        ("ら", "ra"), ("り", "ri"), ("る", "ru"), ("れ", "re"), ("ろ", "ro"),
        ("わ", "wa"), ("を", "wo"), ("ん", "n"),
        ("が", "ga"), ("ぎ", "gi"), ("ぐ", "gu"), ("げ", "ge"), ("ご", "go"),
        ("ざ", "za"), ("じ", "ji"), ("ず", "zu"), ("ぜ", "ze"), ("ぞ", "zo"),
        ("だ", "da"), ("ぢ", "ji"), ("づ", "zu"), ("で", "de"), ("ど", "do"),
        ("ば", "ba"), ("び", "bi"), ("ぶ", "bu"), ("べ", "be"), ("ぼ", "bo"),
        ("ぱ", "pa"), ("ぴ", "pi"), ("ぷ", "pu"), ("ぺ", "pe"), ("ぽ", "po"),
        ("ゃ", "ya"), ("ゅ", "yu"), ("ょ", "yo"), ("っ", "tsu"),
    ]
    katakana = [
        ("ア", "a"), ("イ", "i"), ("ウ", "u"), ("エ", "e"), ("オ", "o"),
        ("カ", "ka"), ("キ", "ki"), ("ク", "ku"), ("ケ", "ke"), ("コ", "ko"),
        ("サ", "sa"), ("シ", "shi"), ("ス", "su"), ("セ", "se"), ("ソ", "so"),
        ("タ", "ta"), ("チ", "chi"), ("ツ", "tsu"), ("テ", "te"), ("ト", "to"),
        ("ナ", "na"), ("ニ", "ni"), ("ヌ", "nu"), ("ネ", "ne"), ("ノ", "no"),
        ("ハ", "ha"), ("ヒ", "hi"), ("フ", "fu"), ("ヘ", "he"), ("ホ", "ho"),
        ("マ", "ma"), ("ミ", "mi"), ("ム", "mu"), ("メ", "me"), ("モ", "mo"),
        ("ヤ", "ya"), ("ユ", "yu"), ("ヨ", "yo"),
        ("ラ", "ra"), ("リ", "ri"), ("ル", "ru"), ("レ", "re"), ("ロ", "ro"),
        ("ワ", "wa"), ("ヲ", "wo"), ("ン", "n"),
        ("ガ", "ga"), ("ギ", "gi"), ("グ", "gu"), ("ゲ", "ge"), ("ゴ", "go"),
        ("ザ", "za"), ("ジ", "ji"), ("ズ", "zu"), ("ゼ", "ze"), ("ゾ", "zo"),
        ("ダ", "da"), ("ヂ", "ji"), ("ヅ", "zu"), ("デ", "de"), ("ド", "do"),
        ("バ", "ba"), ("ビ", "bi"), ("ブ", "bu"), ("ベ", "be"), ("ボ", "bo"),
        ("パ", "pa"), ("ピ", "pi"), ("プ", "pu"), ("ペ", "pe"), ("ポ", "po"),
        ("ャ", "ya"), ("ュ", "yu"), ("ョ", "yo"), ("ッ", "tsu"),
    ]
    for label, level, table, order_base in (
        ("Hiragana", -2, hiragana, -2000),
        ("Katakana", -1, katakana, -1000),
    ):
        for index, (char, romaji) in enumerate(table):
            characters.append(
                CharacterRow(
                    id=character_id("JAPANESE", char),
                    char=char,
                    mode="JAPANESE",
                    level=level,
                    level_label=label,
                    list_index=order_base + index,
                    reading=romaji,
                    reading_alt="",
                    meaning=f"{label.lower()} for “{romaji}”",
                    traditional="",
                    simplified="",
                    is_writing=False,
                    frequency=None,
                )
            )
    print(f"Japanese kana: {len(hiragana)} hiragana, {len(katakana)} katakana")

    # Japanese Jōyō.
    for index, (char, level, strokes, readings, meaning, frequency) in enumerate(kanjidic):
        characters.append(
            CharacterRow(
                id=character_id("JAPANESE", char),
                char=char,
                mode="JAPANESE",
                level=level,
                level_label="Jōyō secondary" if level == 7 else f"Jōyō {level}",
                list_index=index,
                reading=readings,
                reading_alt="",
                meaning=sanitize_japanese_meaning(meaning)[0],
                traditional="",
                simplified="",
                is_writing=False,
                frequency=frequency,
            )
        )

    # Korean Hangul jamo. These are the letters Hangul syllables are built from.
    # No open stroke-order source exists, so the guide falls back to the outline.
    hangul_consonants = [
        ("ㄱ", "g", "giyeok"), ("ㄴ", "n", "nieun"), ("ㄷ", "d", "digeut"),
        ("ㄹ", "r", "rieul"), ("ㅁ", "m", "mieum"), ("ㅂ", "b", "bieup"),
        ("ㅅ", "s", "siot"), ("ㅇ", "-", "ieung"), ("ㅈ", "j", "jieut"),
        ("ㅊ", "ch", "chieut"), ("ㅋ", "k", "kieuk"), ("ㅌ", "t", "tieut"),
        ("ㅍ", "p", "pieup"), ("ㅎ", "h", "hieut"),
        ("ㄲ", "kk", "ssanggiyeok"), ("ㄸ", "tt", "ssangdigeut"),
        ("ㅃ", "pp", "ssangbieup"), ("ㅆ", "ss", "ssangsiot"),
        ("ㅉ", "jj", "ssangjieut"),
    ]
    hangul_vowels = [
        ("ㅏ", "a", "a"), ("ㅑ", "ya", "ya"), ("ㅓ", "eo", "eo"),
        ("ㅕ", "yeo", "yeo"), ("ㅗ", "o", "o"), ("ㅛ", "yo", "yo"),
        ("ㅜ", "u", "u"), ("ㅠ", "yu", "yu"), ("ㅡ", "eu", "eu"),
        ("ㅣ", "i", "i"), ("ㅐ", "ae", "ae"), ("ㅒ", "yae", "yae"),
        ("ㅔ", "e", "e"), ("ㅖ", "ye", "ye"), ("ㅘ", "wa", "wa"),
        ("ㅙ", "wae", "wae"), ("ㅚ", "oe", "oe"), ("ㅝ", "wo", "wo"),
        ("ㅞ", "we", "we"), ("ㅟ", "wi", "wi"), ("ㅢ", "ui", "ui"),
    ]
    hangul = [
        (char, roman, f"consonant {name}") for char, roman, name in hangul_consonants
    ] + [
        (char, roman, f"vowel {name}") for char, roman, name in hangul_vowels
    ]
    for index, (char, roman, meaning) in enumerate(hangul):
        characters.append(
            CharacterRow(
                id=character_id("KOREAN", char),
                char=char,
                mode="KOREAN",
                level=-1,
                level_label="Hangul",
                list_index=-2000 + index,
                reading=roman,
                reading_alt="",
                meaning=meaning,
                traditional="",
                simplified="",
                is_writing=False,
                frequency=None,
            )
        )
    print(f"Korean Hangul letters: {len(hangul)}")

    # Hangul syllables: every initial x vowel combination without a final
    # consonant (19 x 21 = 399). Generated from the Unicode composition rules,
    # so the set is complete and the Romanisation is deterministic.
    hangul_initials = [
        ("ㄱ", "g"), ("ㄲ", "kk"), ("ㄴ", "n"), ("ㄷ", "d"), ("ㄸ", "tt"),
        ("ㄹ", "r"), ("ㅁ", "m"), ("ㅂ", "b"), ("ㅃ", "pp"), ("ㅅ", "s"),
        ("ㅆ", "ss"), ("ㅇ", ""), ("ㅈ", "j"), ("ㅉ", "jj"), ("ㅊ", "ch"),
        ("ㅋ", "k"), ("ㅌ", "t"), ("ㅍ", "p"), ("ㅎ", "h"),
    ]
    hangul_vowels = [
        ("ㅏ", "a"), ("ㅐ", "ae"), ("ㅑ", "ya"), ("ㅒ", "yae"), ("ㅓ", "eo"),
        ("ㅔ", "e"), ("ㅕ", "yeo"), ("ㅖ", "ye"), ("ㅗ", "o"), ("ㅘ", "wa"),
        ("ㅙ", "wae"), ("ㅚ", "oe"), ("ㅛ", "yo"), ("ㅜ", "u"), ("ㅝ", "wo"),
        ("ㅞ", "we"), ("ㅟ", "wi"), ("ㅠ", "yu"), ("ㅡ", "eu"), ("ㅢ", "ui"),
        ("ㅣ", "i"),
    ]
    syllables = 0
    for initial_index, (_, initial_roman) in enumerate(hangul_initials):
        for vowel_index, (_, vowel_roman) in enumerate(hangul_vowels):
            char = chr(0xAC00 + (initial_index * 21 + vowel_index) * 28)
            roman = initial_roman + vowel_roman
            characters.append(
                CharacterRow(
                    id=character_id("KOREAN", char),
                    char=char,
                    mode="KOREAN",
                    level=-1,
                    level_label="Syllables",
                    list_index=-1900 + syllables,
                    reading=roman,
                    reading_alt="",
                    meaning=f"syllable {roman}",
                    traditional="",
                    simplified="",
                    is_writing=False,
                    frequency=None,
                )
            )
            syllables += 1
    print(f"Korean Hangul syllables: {syllables}")

    # Korean Hanja from Unihan. The education list, readings, and definitions
    # all come from Unihan under the Unicode License.
    korean_hanja = sorted(
        unihan_education,
        key=lambda char: (unihan_hangul.get(char, ""), ord(char)),
    )
    for index, char in enumerate(korean_hanja):
        readings = unihan_hangul.get(char, "").split()
        reading = readings[0] if readings else ""
        alt = " ".join(readings[1:])
        characters.append(
            CharacterRow(
                id=character_id("KOREAN", char),
                char=char,
                mode="KOREAN",
                level=0,
                level_label="Hanja",
                list_index=index,
                reading=reading,
                reading_alt=alt,
                meaning=MEANING_OVERRIDES.get(
                    char,
                    clean_meaning(unihan_definitions.get(char, "")),
                ),
                traditional="",
                simplified="",
                is_writing=False,
                frequency=None,
            )
        )
    print(f"Korean Hanja from Unihan: {len(korean_hanja)}")

    # Glyph groups and stroke data. One source per character keeps sharing stable.
    modes_by_char: dict[str, set[str]] = defaultdict(set)
    for row in characters:
        modes_by_char[row.char].add(row.mode)

    total_strokes = load_total_strokes()
    glyphs: dict[str, dict] = {}
    for char in sorted(modes_by_char):
        source = "none"
        hash_value = hashlib.sha256(b"none").hexdigest()[:16]
        stroke_count = 0
        strokes: list[list[tuple[float, float]]] = []
        paths: list[str] = []
        source_char = char
        if char in mmh:
            source = "makemeahanzi"
            strokes = mmh[char]
            stroke_count = len(strokes)
            hash_value = points_hash(strokes)
        elif char in kanjivg:
            source = "kanjivg"
            paths = kanjivg[char]
            stroke_count = len(paths)
            hash_value = path_hash(paths)
        else:
            for variant in unihan_variants.get(char, []):
                if variant in mmh:
                    source = f"variant:{variant}"
                    source_char = variant
                    strokes = mmh[variant]
                    stroke_count = len(strokes)
                    hash_value = points_hash(strokes)
                    break
                if variant in kanjivg:
                    source = f"variant:{variant}"
                    source_char = variant
                    paths = kanjivg[variant]
                    stroke_count = len(paths)
                    hash_value = path_hash(paths)
                    break
        if source == "none":
            # No open stroke order for this exact glyph. The count is still real,
            # from Unihan, so cards do not claim zero strokes.
            stroke_count = total_strokes.get(char, 0)
        group_id = f"g:{codepoint(char)}:{hash_value}"
        glyphs[char] = {
            "id": group_id,
            "character": char,
            "source": source,
            "source_char": source_char,
            "hash": hash_value,
            "stroke_count": stroke_count,
            "strokes": strokes,
            "paths": paths,
            "modes": modes_by_char[char],
        }

    # Word context for Chinese. Only words whose characters all exist in the mode.
    ids_by_mode_char: dict[tuple[str, str], str] = {
        (row.mode, row.char): row.id for row in characters
    }
    words: list[tuple[str, str, str, str, int | None, int, int, int | None]] = []
    links: list[tuple[int, str]] = []
    word_slots: dict[tuple[str, str], int] = {}
    word_id = 0

    def register_word(
        mode: str,
        word: str,
        reading: str,
        meaning: str,
        level: int | None,
        present: list[str],
        common: int = 0,
        mature: int = 0,
        frequency: int | None = None,
    ) -> None:
        nonlocal word_id
        if not word or not present or not meaning.strip():
            return
        if mode in {"SIMPLIFIED", "TRADITIONAL"}:
            reading = clean_pinyin_reading(reading)
        low_meaning = meaning.strip().lower()
        if low_meaning.startswith(
            ("variant of", "erhua variant", "erhua form", "old variant", "used in")
        ):
            return
        key = (mode, word)
        existing = word_slots.get(key)
        if existing is None:
            word_id += 1
            word_slots[key] = word_id
            words.append((mode, word, reading[:120], meaning[:180], level, common, mature, frequency))
            target_id = word_id
        else:
            target_id = existing
            position = target_id - 1
            (
                old_mode, old_word, old_reading, old_meaning,
                old_level, old_common, old_mature, old_frequency,
            ) = words[position]
            readings = [part for part in old_reading.split(" / ") if part]
            if reading and reading not in readings:
                readings.append(reading)
            meanings = [part for part in old_meaning.split("; ") if part]
            if meaning not in meanings:
                meanings.append(meaning)
            merged_level = old_level
            if level is not None and (merged_level is None or level < merged_level):
                merged_level = level
            words[position] = (
                old_mode,
                old_word,
                " / ".join(readings)[:120],
                "; ".join(meanings)[:180],
                merged_level,
                max(old_common, common),
                max(old_mature, mature),
                old_frequency if frequency is None else (
                    frequency if old_frequency is None else min(old_frequency, frequency)
                ),
            )
        for character_id in present:
            links.append((target_id, character_id))

    def add_word_partial(
        mode: str,
        word: str,
        reading: str,
        meaning: str,
        level: int | None,
        link_chars: set[str],
        common: int = 0,
        mature: int = 0,
        frequency: int | None = None,
    ) -> None:
        present = [
            ids_by_mode_char[(mode, char)]
            for char in dict.fromkeys(link_chars)
            if (mode, char) in ids_by_mode_char
        ]
        register_word(mode, word, reading, meaning, level, present, common, mature, frequency)

    def add_word(
        mode: str,
        word: str,
        reading: str,
        meaning: str,
        level: int | None,
        common: int = 0,
        mature: int = 0,
        frequency: int | None = None,
    ) -> None:
        if not word or any((mode, char) not in ids_by_mode_char for char in word):
            return
        register_word(
            mode,
            word,
            reading,
            meaning,
            level,
            [ids_by_mode_char[(mode, char)] for char in dict.fromkeys(word)],
            common,
            mature,
            frequency,
        )

    for row in hsk_words:
        word = row.get("hanzi", "")
        level = row.get("level")
        meaning, mature = sanitize_word_meaning(row.get("meaning", ""))
        add_word(
            "SIMPLIFIED",
            word,
            row.get("pinyin", ""),
            meaning,
            level,
            1 if level is not None else 0,
            1 if mature else 0,
        )
        # Use CEDICT's word-level simplified→traditional mapping. Mechanical
        # character substitution produces wrong forms such as 頭發 for 頭髮.
        trad_word = cedict_word_pairs.get(word)
        if trad_word and trad_word != word:
            reading, meaning = cedict_words.get(
                trad_word, (row.get("pinyin", ""), row.get("meaning", ""))
            )
            meaning, mature = sanitize_word_meaning(meaning)
            add_word(
                "TRADITIONAL",
                trad_word,
                reading,
                meaning,
                level,
                1 if level is not None else 0,
                1 if mature else 0,
            )

    # Japanese word examples from JMdict.
    joyo_chars = {row.char for row in characters if row.mode == "JAPANESE" and row.level > 0}
    japanese_words = load_jmdict_words(joyo_chars)
    for word, (reading, meaning, _, common, rank) in japanese_words.items():
        meaning, mature = sanitize_japanese_meaning(meaning)
        add_word_partial(
            "JAPANESE",
            word,
            reading,
            meaning,
            None,
            {char for char in word if char in joyo_chars},
            1 if common else 0,
            1 if mature else 0,
            rank,
        )
    print(f"Japanese word examples: {len(japanese_words)}")

    # Korean word examples from the Korean Wiktionary extract. The word field
    # is the Hanja spelling, so word practice writes Hanja; the reading is the
    # Hangul pronunciation shown on the card.
    korean_words = load_korean_words(unihan_education)
    for hanja_word, hangul, meaning, common, mature, rank in korean_words:
        add_word_partial(
            "KOREAN",
            hanja_word,
            hangul,
            meaning,
            None,
            {char for char in hanja_word if char in unihan_education},
            1 if common else 0,
            1 if mature else 0,
            rank,
        )
    print(f"Korean word examples: {len(korean_words)}")

    # Validation gate: a shipped character must have a reading and a meaning.
    # This is what would have caught the empty Jōyō readings before release.
    missing_reading = [row.char for row in characters if not row.reading.strip()]
    missing_meaning = [row.char for row in characters if not row.meaning.strip()]
    orphan = [row.char for row in characters if row.char not in glyphs]
    if missing_reading or missing_meaning or orphan:
        problems = []
        if missing_reading:
            problems.append(f"no reading: {''.join(missing_reading[:20])}")
        if missing_meaning:
            problems.append(f"no meaning: {''.join(missing_meaning[:20])}")
        if orphan:
            problems.append(f"no glyph group: {''.join(orphan[:20])}")
        raise SystemExit("content validation failed — " + "; ".join(problems))
    no_guide = [row.char for row in characters if glyphs[row.char]["source"] == "none"]
    print(f"content validation: readings and meanings complete; {len(no_guide)} characters have no stroke guide")

    out_path.parent.mkdir(parents=True, exist_ok=True)
    if out_path.exists():
        out_path.unlink()
    connection = sqlite3.connect(out_path)
    connection.executescript(
        """
        PRAGMA journal_mode=DELETE;
        PRAGMA synchronous=OFF;
        CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT NOT NULL);
        CREATE TABLE glyph_group(
          id TEXT PRIMARY KEY,
          character TEXT NOT NULL,
          stroke_source TEXT NOT NULL,
          stroke_hash TEXT NOT NULL,
          stroke_count INTEGER NOT NULL,
          modes TEXT NOT NULL
        );
        CREATE TABLE glyph_stroke(
          glyph_group_id TEXT NOT NULL,
          idx INTEGER NOT NULL,
          points BLOB,
          path TEXT,
          path_source TEXT,
          PRIMARY KEY(glyph_group_id, idx)
        );
        CREATE TABLE char_variant(
          char TEXT NOT NULL,
          variant TEXT NOT NULL,
          PRIMARY KEY(char, variant)
        );
        CREATE TABLE character(
          id TEXT PRIMARY KEY,
          glyph_group_id TEXT NOT NULL,
          character TEXT NOT NULL,
          mode TEXT NOT NULL,
          level INTEGER NOT NULL,
          level_label TEXT NOT NULL,
          list_index INTEGER NOT NULL,
          reading TEXT NOT NULL,
          reading_alt TEXT NOT NULL,
          meaning TEXT NOT NULL,
          traditional TEXT NOT NULL,
          simplified TEXT NOT NULL,
          is_writing INTEGER NOT NULL,
          frequency INTEGER,
          stroke_source TEXT NOT NULL,
          stroke_count INTEGER NOT NULL
        );
        CREATE TABLE char_meta(
          character TEXT PRIMARY KEY,
          radical TEXT NOT NULL,
          decomposition TEXT NOT NULL,
          etymology TEXT NOT NULL,
          etymology_type TEXT NOT NULL,
          phonetic TEXT NOT NULL,
          semantic TEXT NOT NULL
        );
        CREATE TABLE word_context(
          id INTEGER PRIMARY KEY,
          mode TEXT NOT NULL,
          word TEXT NOT NULL,
          reading TEXT NOT NULL,
          meaning TEXT NOT NULL,
          level INTEGER,
          common INTEGER NOT NULL DEFAULT 0,
          mature INTEGER NOT NULL DEFAULT 0,
          frequency INTEGER
        );
        CREATE TABLE word_character(
          word_id INTEGER NOT NULL,
          character_id TEXT NOT NULL,
          PRIMARY KEY(word_id, character_id)
        );
        CREATE INDEX character_mode_level ON character(mode, level, list_index);
        CREATE INDEX character_mode_list ON character(mode, list_index);
        CREATE INDEX character_glyph ON character(glyph_group_id);
        CREATE INDEX character_char ON character(character);
        CREATE INDEX char_variant_variant ON char_variant(variant);
        CREATE INDEX word_character_character ON word_character(character_id);
        CREATE INDEX word_context_mode_level ON word_context(mode, level);
        CREATE INDEX word_context_mode_word ON word_context(mode, word);
        """
    )

    meta_rows = []
    for row in characters:
        meta = mmh_meta.get(row.char)
        if meta is not None:
            radical, decomposition, hint, kind, phonetic, semantic = meta
            if not decomposition:
                parts = load_kanjivg_components(row.char)
                if len(parts) >= 2:
                    decomposition = " ".join(parts)
            if not hint.strip():
                hint = shape_note(decomposition, radical)
                kind = "shape"
            meta_rows.append((row.char, radical, decomposition, hint, kind, phonetic, semantic))
        elif row.char in unihan_radicals:
            radical = unihan_radicals[row.char]
            parts = load_kanjivg_components(row.char)
            decomposition = " ".join(parts) if len(parts) >= 2 else ""
            meta_rows.append(
                (row.char, radical, decomposition, shape_note(decomposition, radical), "shape", "", "")
            )
    connection.executemany(
        "INSERT OR REPLACE INTO char_meta VALUES(?,?,?,?,?,?,?)",
        meta_rows,
    )

    meta = {
        "format": "bihua-content",
        "version": "2",
        "builtAt": "2026-11-21",
        "sources": "Gushi HSK 3.0, ivankra/hsk30, Make Me a Hanzi, KanjiVG, KANJIDIC2, Unihan, CC-CEDICT, Korean Wiktionary, FrequencyWords",
        "hskCharacters": sum(1 for row in characters if row.mode == "SIMPLIFIED" and row.level != 10),
        "nonHskCharacters": sum(1 for row in characters if row.mode == "SIMPLIFIED" and row.level == 10),
        "simplifiedCharacters": sum(1 for row in characters if row.mode == "SIMPLIFIED"),
        "traditionalCharacters": sum(1 for row in characters if row.mode == "TRADITIONAL"),
        "writingCharacters": len(writing),
        "joyoKanji": sum(1 for row in characters if row.mode == "JAPANESE" and row.level > 0),
        "kanaCharacters": sum(1 for row in characters if row.mode == "JAPANESE" and row.level < 0),
        "koreanHanja": sum(1 for row in characters if row.mode == "KOREAN" and row.level >= 0),
        "hangulLetters": sum(1 for row in characters if row.mode == "KOREAN" and row.level_label == "Hangul"),
        "hangulSyllables": sum(1 for row in characters if row.mode == "KOREAN" and row.level_label == "Syllables"),
        "glyphGroups": len(glyphs),
        "words": len(words),
        "structureEntries": len({row.char for row in characters if row.char in mmh_meta or row.char in unihan_radicals}),
    }
    connection.executemany("INSERT INTO meta VALUES(?,?)", meta.items())

    connection.executemany(
        "INSERT INTO glyph_group VALUES(?,?,?,?,?,?)",
        [
            (
                glyph["id"],
                glyph["character"],
                glyph["source"],
                glyph["hash"],
                glyph["stroke_count"],
                ",".join(mode for mode in MODE_ORDER if mode in glyph["modes"]),
            )
            for glyph in glyphs.values()
        ],
    )
    for glyph in glyphs.values():
        source = glyph["source"]
        source_char = glyph["source_char"]
        if source == "makemeahanzi" or source.startswith("variant:"):
            base = source_char
            if base in mmh_paths:
                for index, path in enumerate(mmh_paths[base]):
                    points = glyph["strokes"][index] if index < len(glyph["strokes"]) else []
                    connection.execute(
                        "INSERT INTO glyph_stroke VALUES(?,?,?,?,?)",
                        (glyph["id"], index, points_blob([points]), path, "makemeahanzi"),
                    )
                continue
        if glyph["paths"]:
            for index, path in enumerate(glyph["paths"]):
                connection.execute(
                    "INSERT INTO glyph_stroke VALUES(?,?,?,?,?)",
                    (glyph["id"], index, None, path, "kanjivg"),
                )

    connection.executemany(
        "INSERT INTO character VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        [
            (
                row.id,
                glyphs[row.char]["id"],
                row.char,
                row.mode,
                row.level,
                row.level_label,
                row.list_index,
                row.reading,
                row.reading_alt,
                row.meaning,
                row.traditional,
                row.simplified,
                1 if row.is_writing else 0,
                row.frequency,
                glyphs[row.char]["source"],
                glyphs[row.char]["stroke_count"],
            )
            for row in characters
        ],
    )

    all_variants = load_all_variants()
    pairs = {
        (char, variant)
        for char, variants in all_variants.items()
        for variant in variants
        if char != variant
    }
    connection.executemany(
        "INSERT OR IGNORE INTO char_variant VALUES(?,?)",
        sorted(pairs),
    )

    connection.executemany(
        "INSERT INTO word_context VALUES(?,?,?,?,?,?,?,?,?)",
        [
            (index + 1, mode, word, reading, meaning, level, common, mature, frequency)
            for index, (mode, word, reading, meaning, level, common, mature, frequency) in enumerate(words)
        ],
    )
    connection.executemany(
        "INSERT OR IGNORE INTO word_character VALUES(?,?)",
        links,
    )
    connection.commit()
    connection.execute("VACUUM")
    connection.close()

    size_mb = out_path.stat().st_size / 1024 / 1024
    print(f"wrote {out_path} ({size_mb:.1f} MB)")
    for key, value in meta.items():
        print(f"  {key}: {value}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()
    build(args.out)


if __name__ == "__main__":
    main()
