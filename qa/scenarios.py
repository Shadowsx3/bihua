#!/usr/bin/env python3
"""Daily learner journeys and expected states."""
from __future__ import annotations

from pathlib import Path

import re
import time
from dataclasses import dataclass, field

from driver import Journey, JourneyFailure
from state import ScenarioState, character_guide, character_reading, character_variants


@dataclass
class ScenarioSpec:
    name: str
    level: str = "quick"
    all_profiles: bool = False
    group: str | None = None
    state: dict = field(default_factory=dict)
    runner: str = "daily_sample"


SCENARIOS: list[ScenarioSpec] = [
    ScenarioSpec("fresh", level="quick", all_profiles=True, runner="fresh"),
    ScenarioSpec("day1", level="quick", all_profiles=True, state={"reviews": 0, "new": 5, "days": 1}, runner="daily_sample"),
    ScenarioSpec("daily20", level="quick", all_profiles=True, state={"reviews": 20, "new": 5, "days": 7, "known": 8}, runner="daily_sample"),
    ScenarioSpec("daily80", level="quick", all_profiles=True, state={"reviews": 80, "new": 5, "days": 30, "known": 40}, runner="daily_sample"),
    ScenarioSpec("navigation", level="quick", all_profiles=True, state={"reviews": 20, "new": 5, "days": 10}, runner="navigation"),
    ScenarioSpec("navigation-deep", level="standard", state={"reviews": 20, "new": 5, "days": 10}, runner="navigation_deep"),
    ScenarioSpec("resume", level="quick", all_profiles=True, state={"reviews": 20, "new": 5, "days": 7}, runner="resume"),
    ScenarioSpec("caught-up", level="standard", state={"reviews": 0, "new": 0, "known": 20, "goal": 0, "days": 30}, runner="learning_state"),
    ScenarioSpec("completion", level="standard", state={"reviews": 5, "new": 0, "goal": 0, "days": 7, "pronunciation": True, "task_mode": "PRONOUNCE", "review_order": "OLDEST_FIRST"}, runner="completion"),
    ScenarioSpec("missed-week", level="standard", state={"reviews": 120, "new": 5, "days": 20, "known": 60}, runner="daily_sample"),
    ScenarioSpec("vacation", level="standard", state={"reviews": 80, "new": 5, "days": 20, "vacation": True}, runner="vacation"),
    ScenarioSpec("ease-back", level="standard", state={"reviews": 80, "new": 5, "days": 20, "ease_back": True}, runner="ease_back"),
    ScenarioSpec("study-tools", level="standard", state={"reviews": 30, "new": 5, "days": 20, "skipped": 3, "suspended": 2}, runner="daily_sample"),
    ScenarioSpec("shared", level="standard", state={"reviews": 30, "new": 5, "days": 20, "shared_glyphs": True}, runner="daily_sample"),
    ScenarioSpec("learning", level="standard", state={"reviews": 0, "new": 0, "learning": 5, "goal": 0, "days": 10}, runner="learning_state"),
    ScenarioSpec("process-death", level="standard", state={"reviews": 40, "new": 5, "days": 14}, runner="process_death"),
    ScenarioSpec("fold-transition", level="standard", group="fold", state={"reviews": 20, "new": 5, "days": 7}, runner="fold_transition"),
    ScenarioSpec("reminders", level="standard", state={"reviews": 3, "new": 0, "goal": 0, "days": 7}, runner="reminders"),
    ScenarioSpec("worksheet", level="standard", state={"reviews": 20, "new": 5, "days": 10}, runner="worksheet"),
    ScenarioSpec("dictionary", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="dictionary"),
    ScenarioSpec("word-guide", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="word_guide"),
    ScenarioSpec("stroke-coach", level="standard", state={"reviews": 5, "new": 0, "goal": 0, "days": 7, "task_mode": "WRITE", "review_order": "OLDEST_FIRST"}, runner="stroke_coach"),
    ScenarioSpec("stroke-clean", level="standard", state={"reviews": 5, "new": 0, "goal": 0, "days": 7, "task_mode": "WRITE", "review_order": "OLDEST_FIRST"}, runner="stroke_clean"),
    ScenarioSpec("pronounce-kana", level="standard", state={"reviews": 5, "new": 0, "goal": 0, "days": 7, "task_mode": "PRONOUNCE", "pronunciation": True, "mode": "JAPANESE", "review_order": "OLDEST_FIRST"}, runner="pronounce_kana"),
    ScenarioSpec("daily100", level="full", state={"reviews": 100, "new": 0, "days": 45, "known": 80, "pronunciation": True, "task_mode": "PRONOUNCE", "review_order": "OLDEST_FIRST"}, runner="drain"),
    ScenarioSpec("goal30", level="full", state={"reviews": 40, "new": 30, "days": 20}, runner="daily_sample"),
    ScenarioSpec("mature", level="full", state={"reviews": 20, "new": 5, "days": 10, "mature": True}, runner="navigation"),
    ScenarioSpec("accessibility", level="full", state={"reviews": 20, "new": 5, "days": 10}, runner="accessibility"),
    ScenarioSpec("review-modes", level="quick", state={"reviews": 10, "new": 0, "goal": 0, "days": 7, "pronunciation": True, "review_order": "OLDEST_FIRST"}, runner="review_modes"),
    ScenarioSpec("review-modes-long", level="full", state={"reviews": 30, "new": 0, "goal": 0, "days": 14, "pronunciation": True, "review_order": "OLDEST_FIRST"}, runner="review_modes"),
    ScenarioSpec("draw-recognition", level="quick", state={"guides_only": True, "reviews": 8, "new": 0, "goal": 0, "days": 7, "task_mode": "WRITE", "review_order": "OLDEST_FIRST"}, runner="draw_recognition"),
    ScenarioSpec("draw-recognition-ja", level="full", state={"guides_only": True, "reviews": 4, "new": 0, "goal": 0, "days": 7, "task_mode": "WRITE", "review_order": "OLDEST_FIRST", "mode": "JAPANESE"}, runner="draw_recognition"),
    ScenarioSpec("draw-recognition-ko", level="full", state={"guides_only": True, "reviews": 4, "new": 0, "goal": 0, "days": 7, "task_mode": "WRITE", "review_order": "OLDEST_FIRST", "mode": "KOREAN"}, runner="draw_recognition"),
    ScenarioSpec("paper-compare", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="paper_compare"),
    ScenarioSpec("study-lists", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="study_lists"),
    ScenarioSpec("appearance", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="appearance"),
    ScenarioSpec("word-audio", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="word_audio"),
    ScenarioSpec("anki", level="standard", state={"reviews": 8, "new": 2, "days": 7, "pronunciation": True, "anki_writing": True, "anki_pronunciation": True}, runner="anki"),
    ScenarioSpec("meaning", level="standard", state={"reviews": 8, "new": 0, "days": 7, "meaning_practice": True, "anki_writing": True}, runner="meaning"),
    ScenarioSpec("meaning-anki", level="standard", state={"reviews": 8, "new": 0, "days": 7, "meaning_practice": True, "anki_writing": True, "anki_meaning": True}, runner="meaning_anki"),
    ScenarioSpec("synonyms", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="synonyms"),
    ScenarioSpec("tour", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="tour"),
    ScenarioSpec("tour-dark", level="standard", state={"reviews": 5, "new": 2, "days": 5, "theme": "DARK"}, runner="tour"),
    ScenarioSpec("settings-outfit", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="settings_outfit"),
    ScenarioSpec("progress-ranges", level="standard", state={"reviews": 20, "new": 2, "days": 12}, runner="progress_ranges"),
    ScenarioSpec("nav-gap", level="standard", state={"reviews": 20, "new": 2, "days": 12}, runner="nav_gap"),
    ScenarioSpec("progress-empty", level="standard", state={"reviews": 0, "new": 0, "goal": 0, "known": 0, "days": 0}, runner="progress_empty"),
    ScenarioSpec("grade-help", level="standard", state={"reviews": 5, "new": 0, "goal": 0, "days": 7, "anki_writing": True}, runner="grade_help"),
    ScenarioSpec("spell-quest", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="spell_quest"),
    ScenarioSpec("spell-entry", level="quick", state={"reviews": 5, "new": 2, "days": 5}, runner="spell_entry"),
    ScenarioSpec("spell-quest-dark", level="full", state={"reviews": 5, "new": 2, "days": 5, "theme": "DARK"}, runner="spell_quest"),
    ScenarioSpec("spell-quest-calm", level="full", state={"reviews": 5, "new": 2, "days": 5, "reduce_motion": True}, runner="spell_quest"),
]


def scenario_by_name(name: str) -> ScenarioSpec:
    for spec in SCENARIOS:
        if spec.name == name:
            return spec
    raise SystemExit(f"unknown scenario: {name}")


def _expected(state: ScenarioState) -> tuple[int, int]:
    return int(state.metadata["visible_due"]), int(state.metadata["new"])


def fresh(journey: Journey, state: ScenarioState | None = None) -> None:
    with journey.step("onboarding"):
        journey.wait("Write characters that stay", timeout=15)
        journey.assert_text("Skip setup")
        journey.tap("Skip setup")
        journey.assert_home()
        journey.assert_home_counts(due=0, new=5)


def daily_sample(journey: Journey, state: ScenarioState) -> None:
    due, new = _expected(state)
    with journey.step(f"home {due} due, {new} new"):
        journey.assert_home()
        journey.assert_home_counts(due=due, new=new)
    with journey.step("session"):
        journey.start_session()
        counts = journey.session_counts()
        if counts is None:
            raise JourneyFailure("session counter missing")
        current, total = counts
        limit = 10 if state.settings.get("easeBackUntilMs", 0) > 0 else 20
        expected_total = min(limit, due + new)
        if total != expected_total:
            raise JourneyFailure(f"session total {total}, expected {expected_total}")
        if due > 0:
            reviews = min(5, due)
            for _ in range(reviews):
                if journey.exists("Check & grade", timeout=0.2):
                    _write_card(journey, state, full_guide=False)
                else:
                    journey.grade_one()
            if new > 0:
                if not journey.exists("Lesson ", timeout=2, exact=False) and not journey.exists("New lesson", timeout=0.1, exact=False):
                    raise JourneyFailure("mixed session did not reach a lesson after five reviews")
        elif new > 0:
            if not journey.exists("Lesson 1 of", timeout=3, exact=False):
                raise JourneyFailure("new-only session did not start with a lesson")
    with journey.step("leave session"):
        journey.leave_session("End session")
        journey.assert_home()


def navigation(journey: Journey, state: ScenarioState) -> None:
    with journey.step("characters"):
        journey.open_tab("Characters")
        journey.wait("Character, meaning, or reading", timeout=8, exact=False)
    with journey.step("progress"):
        journey.open_tab("Progress")
        journey.wait("Progress", timeout=8)
    with journey.step("practice"):
        journey.open_tab("Practice")
        journey.wait("Change", timeout=8)
    with journey.step("settings"):
        journey.open_tab("Settings")
        journey.wait("Make Bihua yours", timeout=8, exact=False)
    with journey.step("home"):
        journey.open_tab("Home")
        journey.assert_home()


def resume(journey: Journey, state: ScenarioState) -> None:
    due, new = _expected(state)
    with journey.step("start and grade"):
        journey.assert_home()
        journey.start_session()
        for _ in range(2):
            journey.grade_one()
    with journey.step("process death"):
        journey.force_stop()
        journey.launch()
        journey.assert_home()
        journey.wait("Continue your lesson", timeout=8, exact=False)
    with journey.step("resume"):
        journey.resume_session()
        if journey.session_counts() is None:
            raise JourneyFailure("resumed session has no counter")
        journey.leave_session("End session")
        journey.assert_home()


def process_death(journey: Journey, state: ScenarioState) -> None:
    with journey.step("grade two cards"):
        journey.assert_home()
        journey.start_session()
        for _ in range(2):
            journey.grade_one()
    with journey.step("force-stop and relaunch"):
        journey.force_stop()
        journey.launch()
        journey.assert_home()
        journey.wait("Continue your lesson", timeout=10, exact=False)
    with journey.step("resume after death"):
        journey.resume_session()
        journey.grade_one()
        journey.leave_session("End session")
        journey.assert_home()
    journey.assert_no_crash()


def completion(journey: Journey, state: ScenarioState) -> None:
    due, new = _expected(state)
    with journey.step(f"drain {due} reviews"):
        journey.assert_home()
        journey.assert_home_counts(due=due, new=new)
        journey.start_session()
        journey.drain_session(max_cards=max(10, due + 4), fast=True)
    with journey.step("home is caught up"):
        journey.assert_home_counts(due=0, new=0)
        journey.assert_text("All done for today")


def drain(journey: Journey, state: ScenarioState) -> None:
    due, new = _expected(state)
    with journey.step(f"drain all {due} reviews"):
        journey.assert_home()
        journey.assert_home_counts(due=due, new=new)
        remaining = due
        sessions = 0
        while remaining > 0:
            sessions += 1
            if sessions > 20:
                raise JourneyFailure("too many sessions")
            journey.start_session()
            journey.drain_session(max_cards=30, fast=True)
            deadline = time.time() + 8
            current_due, _ = journey.home_counts()
            while current_due >= remaining and time.time() < deadline:
                time.sleep(0.25)
                current_due, _ = journey.home_counts()
            if current_due >= remaining:
                raise JourneyFailure(f"due count did not fall: {remaining} -> {current_due}")
            remaining = current_due
    with journey.step("caught up"):
        journey.assert_home_counts(due=0, new=0)
        journey.assert_text("All done for today")


def learning_state(journey: Journey, state: ScenarioState) -> None:
    with journey.step("home explains learning or caught-up state"):
        journey.assert_home()
        text = " | ".join(journey.texts())
        if "learning steps" not in text and "All done for today" not in text:
            raise JourneyFailure(f"missing learning or caught-up copy: {text}")
        if not journey.exists("Practice instead", timeout=2):
            raise JourneyFailure("Practice instead action missing")
        if "All done for today" in text and "All caught up" in text:
            raise JourneyFailure("the all-done card repeats the all-caught-up message")


def vacation(journey: Journey, state: ScenarioState) -> None:
    with journey.step("vacation home"):
        journey.assert_home()
        journey.assert_text("Reviews are paused")
        journey.assert_text("Vacation")
    with journey.step("end vacation"):
        journey.tap("End vacation")
        journey.wait("End vacation?", timeout=5, exact=False)
        journey.tap("End vacation")
        journey.wait("to review", timeout=8, exact=False)


def ease_back(journey: Journey, state: ScenarioState) -> None:
    due, new = _expected(state)
    with journey.step("home shows ease-back"):
        journey.assert_home()
        journey.assert_text("shorter sessions this week")
    with journey.step("session is capped at ten"):
        journey.start_session()
        counts = journey.session_counts()
        if counts is None or counts[1] != min(10, due + new):
            raise JourneyFailure(f"ease-back session counts: {counts}")
        journey.leave_session("End session")


def fold_transition(journey: Journey, state: ScenarioState) -> None:
    with journey.step("prepare real fold sizes"):
        journey.device.shell("wm size reset")
        journey.device.shell("wm density reset")
        time.sleep(1.0)
    with journey.step("fold"):
        journey.device.fold(True)
        time.sleep(1.0)
        journey.assert_home()
        journey.screenshot("folded")
    with journey.step("unfold"):
        journey.device.fold(False)
        time.sleep(1.0)
        journey.assert_home()
        journey.screenshot("unfolded")
    journey.assert_no_crash()


TONE_BASE = {
    "ā": ("a", 1), "á": ("a", 2), "ǎ": ("a", 3), "à": ("a", 4),
    "ē": ("e", 1), "é": ("e", 2), "ě": ("e", 3), "è": ("e", 4),
    "ī": ("i", 1), "í": ("i", 2), "ǐ": ("i", 3), "ì": ("i", 4),
    "ō": ("o", 1), "ó": ("o", 2), "ǒ": ("o", 3), "ò": ("o", 4),
    "ū": ("u", 1), "ú": ("u", 2), "ǔ": ("u", 3), "ù": ("u", 4),
    "ǖ": ("ü", 1), "ǘ": ("ü", 2), "ǚ": ("ü", 3), "ǜ": ("ü", 4),
}


def _numbered_pinyin(reading: str) -> tuple[str, bool]:
    first = re.split(r"[,/;]", reading)[0].strip().strip(".-")
    tone = 0
    plain = []
    for char in first:
        if char in TONE_BASE:
            base, value = TONE_BASE[char]
            plain.append(base)
            tone = value
        elif char == "ü":
            plain.append("v")
        else:
            plain.append(char)
    letters = "".join(plain)
    if tone:
        return letters + str(tone), True
    return letters, False


def _current_character(journey: Journey) -> str | None:
    import xml.etree.ElementTree as ET
    root = ET.fromstring(journey.d.dump_hierarchy(compressed=False))
    best = None
    best_area = 0
    for node in root.iter("node"):
        text = (node.attrib.get("text") or "").strip()
        if len(text) != 1:
            continue
        code = ord(text)
        if not (0x3400 <= code <= 0x9FFF or 0xF900 <= code <= 0xFAFF or
                0x3040 <= code <= 0x30FF or 0xAC00 <= code <= 0xD7AF):
            continue
        bounds = node.attrib.get("bounds", "")
        match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if not match:
            continue
        area = (int(match.group(3)) - int(match.group(1))) * (int(match.group(4)) - int(match.group(2)))
        if area > best_area:
            best_area = area
            best = text
    return best


def _reading_input(journey: Journey, texts: list[str], mode: str, *, tone: bool) -> str | None:
    character = _current_character(journey)
    if not character:
        return None
    reading = character_reading(character, mode)
    if not reading:
        return None
    numbered, has_tone = _numbered_pinyin(reading)
    if tone and not has_tone:
        return None
    return numbered


def _tap_text_node(journey: Journey, text: str, klass: str | None = None) -> bool:
    """Tap a specific text node by its bounds, avoiding duplicate-text hits."""
    import re
    import xml.etree.ElementTree as ET

    root = ET.fromstring(journey.d.dump_hierarchy(compressed=False))
    for node in root.iter("node"):
        if node.attrib.get("text") != text:
            continue
        if klass and node.attrib.get("class") != klass:
            continue
        match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if not match:
            continue
        left, top, right, bottom = (int(match.group(i)) for i in range(1, 5))
        journey.d.click((left + right) // 2, (top + bottom) // 2)
        return True
    return False


def _tap_text_node_wait(
    journey: Journey,
    text: str,
    klass: str | None = None,
    timeout: float = 10.0,
) -> bool:
    deadline = time.time() + timeout
    while time.time() < deadline:
        if _tap_text_node(journey, text, klass):
            return True
        time.sleep(0.2)
    return False


def _field(journey: Journey):
    return journey.d(className="android.widget.EditText")


def _type_and_check(journey: Journey, value: str) -> None:
    field = _field(journey)
    if field is None or not field.exists:
        raise JourneyFailure("pronunciation text field is missing")
    field.set_text(value)
    time.sleep(0.15)
    # The field uses ImeAction.Done. Enter is reliable on fold and tablet
    # layouts where the soft keyboard can cover the Check button.
    journey.d.press("enter")
    time.sleep(0.35)
    if not any(
        journey.exists(marker, timeout=0.1, exact=False)
        for marker in ("Not quite", "Correct!", "Almost", "Checked")
    ):
        journey.d.press("back")
        time.sleep(0.2)
        journey.tap("Check")
    time.sleep(0.4)


def _grade_feedback(journey: Journey) -> None:
    journey.tap_any(["Good", "Hard", "Again", "Easy"], timeout=8)


def _target_from_state(state: ScenarioState, texts: list[str]) -> dict | None:
    for sample in reversed(state.metadata.get("samples", [])):
        meaning = sample.get("meaning") or ""
        if meaning and meaning in texts:
            return sample
    return None


def _assert_target_hidden(journey: Journey, target: str, phase: str) -> None:
    for text in journey.texts():
        compact = text.strip()
        if not compact or target not in compact:
            continue
        cjk = sum(
            1
            for char in compact
            if 0x2E80 <= ord(char) <= 0x9FFF
            or 0x3040 <= ord(char) <= 0x30FF
            or 0xAC00 <= ord(char) <= 0xD7AF
        )
        if cjk >= max(1, len(compact) * 0.4):
            raise JourneyFailure(
                f"{phase} showed the recall target {target!r} in {compact!r}"
            )


def _write_card(journey: Journey, state: ScenarioState, full_guide: bool = False) -> None:
    # "From memory" stays visible on the answer card too, so use the check
    # button as the signal that this is a fresh recall, not a reveal.
    journey.wait("Check & grade", timeout=4)
    canvas = journey._canvas()
    if canvas is None:
        raise JourneyFailure("writing canvas is missing")
    sample = _target_from_state(state, journey.texts())
    if sample:
        _assert_target_hidden(journey, sample["character"], "writing recall")
    strokes = character_guide(sample["character"], state.settings.get("studyMode", "SIMPLIFIED")) if sample else []
    if strokes and not full_guide:
        strokes = [max(strokes, key=len)]
    if strokes:
        if not journey.draw_guide(strokes):
            raise JourneyFailure("could not draw the target guide")
    elif not journey._draw_once():
        raise JourneyFailure("could not draw on the writing canvas")
    time.sleep(0.4)
    grade = journey.find("Check & grade", timeout=0.5)
    if grade is not None and grade.info.get("enabled", True):
        grade.click()
    else:
        journey.tap("Check & grade", timeout=4)
    time.sleep(1.0)
    feedback = " | ".join(journey.texts())
    recognized = any(
        marker in feedback
        for marker in (
            "Handwriting check",
            "No clear reading",
            "Correct!",
            "Not quite",
            "Almost",
        )
    )
    if not recognized and not journey.exists("Good", timeout=1):
        raise JourneyFailure(f"drawing produced no recognition feedback: {feedback}")
    _grade_feedback(journey)


def _pronunciation_card(journey: Journey, mode: str, stage: int) -> bool:
    texts = journey.texts()
    if "Pronunciation" not in texts and "Type the reading" not in texts:
        return False
    if stage == 0:
        _type_and_check(journey, "zzz")
        if not journey.exists("Not quite", timeout=4):
            raise JourneyFailure("wrong pronunciation did not show Not quite")
        journey.tap("Try again")
        time.sleep(0.2)
        correct = _reading_input(journey, texts, mode, tone=False)
        if correct:
            _type_and_check(journey, correct)
            if not journey.exists("Correct!", timeout=4):
                raise JourneyFailure(f"correct pronunciation was not accepted: {correct}")
    elif stage == 1:
        tone_reading = _reading_input(journey, texts, mode, tone=True)
        if tone_reading:
            plain = re.sub(r"\d+$", "", tone_reading)
            _type_and_check(journey, plain)
            if not (journey.exists("Almost", timeout=3, exact=False) or journey.exists("Correct!", timeout=0.1)):
                raise JourneyFailure("tone-only pronunciation did not show the tone message")
    elif stage >= 2:
        journey.tap("Show answer")
        if not journey.exists("This character reads", timeout=3, exact=False):
            raise JourneyFailure("Show answer did not reveal the pronunciation")
    _grade_feedback(journey)
    return True


def review_modes(journey: Journey, state: ScenarioState) -> None:
    mode = state.settings.get("studyMode", "SIMPLIFIED")
    seen_write = 0
    seen_pronounce = 0
    observed: list[str] = []
    with journey.step("start mixed review modes"):
        journey.assert_home()
        journey.start_session()
    with journey.step("exercise writing and pronunciation"):
        for _ in range(12):
            texts = journey.texts()
            if "Session complete" in texts:
                break
            if "Pronunciation" in texts or "Type the reading" in texts:
                _pronunciation_card(journey, mode, seen_pronounce)
                seen_pronounce += 1
                observed.append(f"pronunciation {seen_pronounce}: typing, feedback, grade")
                journey.screenshot(f"pronunciation-{seen_pronounce}")
            else:
                _write_card(journey, state, full_guide=True)
                seen_write += 1
                observed.append(f"writing {seen_write}: drawing, recognition feedback, grade")
                journey.screenshot(f"writing-{seen_write}")
            time.sleep(0.2)
            if seen_write >= 2 and seen_pronounce >= 3:
                break
    if seen_write < 1:
        raise JourneyFailure("session did not expose a writing review")
    if seen_pronounce < 1:
        raise JourneyFailure("session did not expose a pronunciation review")
    journey.artifact_dir.joinpath("review-modes.txt").write_text("\n".join(observed))
    with journey.step("return home"):
        if journey.exists("Session complete", timeout=0.5):
            journey.tap("Back to home")
            journey.assert_home()
        else:
            journey.leave_session("End session")
    journey.assert_no_crash()


def draw_recognition(journey: Journey, state: ScenarioState) -> None:
    mode = state.settings.get("studyMode", "SIMPLIFIED")
    lines = []
    with journey.step("start writing recognition"):
        journey.assert_home()
        journey.start_session()
    with journey.step("draw known guides and read feedback"):
        for index in range(5):
            texts = journey.texts()
            if "Session complete" in texts:
                break
            target = _target_from_state(state, texts)
            if target is None:
                raise JourneyFailure("could not identify the target from its cue")
            sample = target
            if index == 3:
                others = [
                    candidate for candidate in state.metadata.get("samples", [])
                    if candidate["id"] != target["id"] and character_guide(candidate["character"], mode)
                ]
                if others:
                    sample = others[0]
            elif index == 4:
                variants = [
                    variant for variant in character_variants(target["character"])
                    if character_guide(variant, mode)
                ]
                if variants:
                    sample = {
                        "character": variants[0],
                        "id": f"variant:{variants[0]}",
                        "reading": character_reading(variants[0], mode) or "",
                    }
            strokes = character_guide(sample["character"], mode)
            if not strokes:
                raise JourneyFailure(f"no guide strokes for {sample['character']}")
            journey.wait("From memory", timeout=4)
            if not journey.draw_guide(strokes):
                raise JourneyFailure("could not draw the target guide")
            time.sleep(0.3)
            check = journey.find("Check & grade", timeout=1)
            if check is not None and check.info.get("enabled", True):
                check.click()
            time.sleep(0.8)
            feedback = " | ".join(journey.texts())
            if "Handwriting check unavailable" in feedback:
                result = "unavailable"
            elif "No clear reading" in feedback:
                result = "no-clear"
            else:
                result = next(
                    (value for value in ("same", "variant", "different") if value in feedback),
                    "unknown",
                )
            if index == 3 and sample["id"] != target["id"]:
                label = "wrong-guide"
            elif index == 4 and str(sample["id"]).startswith("variant:"):
                label = "variant-guide"
            else:
                label = "target"
            lines.append(
                f"{label} target={target['character']} drew={sample['character']} "
                f"{sample['reading']} -> {result}"
            )
            if label == "wrong-guide" and result == "same":
                lines.append("WARN: wrong guide reported as the target; record for review")
            journey.screenshot(f"draw-{index}-{sample['character']}")
            _grade_feedback(journey)
    journey.artifact_dir.joinpath("draw-recognition.txt").write_text("\n".join(lines))
    with journey.step("leave session"):
        journey.leave_session("End session")
    journey.assert_no_crash()


def reminders(journey: Journey, state: ScenarioState) -> None:
    with journey.step("enable reminders"):
        journey.open_tab("Settings")
        journey.tap("Reminders")
        journey.wait("Study reminders", timeout=6)
        journey.tap("Off. A quiet nudge", exact=False)
        time.sleep(0.5)
        # The test button only exists while reminders are enabled; it is a more
        # reliable signal than matching the time string.
        journey.wait("Send a test notification", timeout=12)
    with journey.step("send test notification"):
        journey.tap("Send a test notification")
        deadline = time.time() + 9
        found = False
        while time.time() < deadline and not found:
            time.sleep(0.8)
            journey.d.open_notification()
            time.sleep(0.4)
            texts = journey.texts()
            found = any("One character is waiting" in text for text in texts)
            if found:
                journey.screenshot("reminder-notification")
            journey.d.press("back")
            time.sleep(0.3)
        if not found:
            raise JourneyFailure(f"test notification missing: {texts}")
    with journey.step("disable reminders"):
        journey.tap("On at", exact=False)
        deadline = time.time() + 10
        while time.time() < deadline and journey.exists("Send a test notification", timeout=0.4):
            time.sleep(0.3)
        if journey.exists("Send a test notification", timeout=0.2):
            raise JourneyFailure("reminders did not turn off")
    journey.assert_no_crash()


def word_guide(journey: Journey, state: ScenarioState) -> None:
    with journey.step("open word practice"):
        journey.open_tab("Characters")
        journey.wait("Dictionary", timeout=8)
        journey.tap("Dictionary")
        journey.wait("Look up anything", timeout=8, exact=False)
        field = _field(journey)
        if field is None or not field.exists:
            raise JourneyFailure("dictionary search field is missing")
        field.set_text("学习")
        journey.d.press("enter")
        journey.wait("学习", timeout=10)
        if not _tap_text_node_wait(journey, "学习", "android.widget.TextView"):
            raise JourneyFailure("dictionary result row is missing")
        journey.wait("Practise writing", timeout=8)
        journey.tap("Practise writing")
        journey.wait("Write the word", timeout=8)
    with journey.step("reveal one character"):
        journey.tap("Show answer")
        journey.wait("Next character", timeout=4)
        journey.screenshot("word-guide-reveal")
    with journey.step("next character keeps the first visible"):
        journey.tap("Next character")
        journey.wait("Character 2 of 2", timeout=4, exact=False)
        journey.screenshot("word-guide-next")
    with journey.step("show the real stroke guide"):
        journey.tap("Strokes")
        journey.wait("Play stroke order", timeout=6)
        journey.tap("Play stroke order")
        time.sleep(0.4)
        if not journey.exists("Numbers", timeout=2):
            raise JourneyFailure("stroke number control is missing")
        journey.screenshot("word-guide")
    with journey.step("faint guide"):
        journey.tap("Faint")
        time.sleep(0.3)
        journey.screenshot("word-guide-faint")
    with journey.step("close"):
        journey.tap("Close word practice")
        time.sleep(0.3)
    journey.assert_no_crash()


def dictionary(journey: Journey, state: ScenarioState) -> None:
    with journey.step("open dictionary"):
        journey.open_tab("Characters")
        journey.wait("Dictionary", timeout=8)
        journey.tap("Dictionary")
        journey.wait("Look up anything", timeout=8, exact=False)
    with journey.step("search a word"):
        field = _field(journey)
        if field is None or not field.exists:
            raise JourneyFailure("dictionary search field is missing")
        field.set_text("学习")
        journey.d.press("enter")
        journey.wait("学习", timeout=10)
        journey.screenshot("dictionary-results")
        # The search field also contains 学习, so target the result TextView.
        if not _tap_text_node_wait(journey, "学习", "android.widget.TextView"):
            raise JourneyFailure("search result row is missing")
    with journey.step("open the word"):
        journey.wait("Practise writing", timeout=8)
        journey.wait("Words with", timeout=8, exact=False)
        texts = journey.texts()
        if not any("xue" in text.lower() or "xué" in text.lower() for text in texts):
            raise JourneyFailure(f"reading missing from detail: {texts}")
        journey.screenshot("dictionary-detail")
    with journey.step("close"):
        journey.d.press("back")
        time.sleep(0.4)
        if journey.exists("Close dictionary", timeout=1):
            journey.tap("Close dictionary")
        journey.wait("Characters", timeout=6)
    journey.assert_no_crash()


def worksheet(journey: Journey, state: ScenarioState) -> None:
    with journey.step("open worksheet builder"):
        journey.open_tab("Settings")
        journey.tap_visible("Data")
        journey.wait("Backups stay with you", timeout=6, exact=False)
        journey.tap_visible("Print practice sheets (PDF)")
        journey.wait("Pick characters", timeout=6, exact=False)
        journey.tap_visible("Recent mistakes")
        journey.tap_visible("Recently learned")
        journey.tap_visible("Confusions")
        journey.tap_visible("Course order")
        scrollers = journey.d(scrollable=True)
        if scrollers.exists and not journey.exists("Trace & write", timeout=0.5):
            scrollers.scroll.to(text="Trace & write")
        journey.tap_visible("Trace & write")
        journey.tap_visible("Mi zi ge")
        journey.screenshot("worksheet-builder")
    with journey.step("export PDF"):
        journey.tap_visible("Export PDF")
        time.sleep(0.8)
        # The system document picker uses Save or a check icon depending on API.
        if journey.exists("Save", timeout=6):
            journey.tap("Save")
        else:
            journey.tap_any(["SAVE", "Done", "Save", "Use this folder"], timeout=8)
        deadline = time.time() + 30
        listing = ""
        while time.time() < deadline:
            candidate = journey.device.shell(
                "ls -t /sdcard/Download/*.pdf 2>/dev/null | head -1"
            ).strip()
            if candidate:
                size = journey.device.shell(f"stat -c %s '{candidate}' 2>/dev/null").strip()
                complete = journey.device.shell(
                    f"grep -a -c '%%EOF' '{candidate}' 2>/dev/null || true"
                ).strip()
                if size.isdigit() and int(size) > 1024 and complete == "1":
                    listing = candidate
                    break
            time.sleep(0.4)
        if not listing:
            raise JourneyFailure("worksheet PDF was not confirmed as complete")
    with journey.step("verify PDF"):
        listing = journey.device.shell("ls -t /sdcard/Download/*.pdf 2>/dev/null | head -1").strip()
        if not listing:
            raise JourneyFailure("no exported PDF found in Downloads")
        local = journey.artifact_dir / "worksheet.pdf"
        import subprocess
        journey.device.shell(f"cp '{listing}' /sdcard/Download/qa-worksheet.pdf")
        proc = subprocess.run(
            ["adb", "-s", journey.device.serial, "pull", "/sdcard/Download/qa-worksheet.pdf", str(local)],
            capture_output=True,
            text=True,
        )
        if proc.returncode != 0 or not local.is_file() or local.read_bytes()[:4] != b"%PDF":
            raise JourneyFailure("exported worksheet is not a PDF")
        journey.screenshot("worksheet-saved")
    with journey.step("close"):
        journey.d.press("back")
        time.sleep(0.4)
    journey.assert_no_crash()


def stroke_coach(journey: Journey, state: ScenarioState) -> None:
    with journey.step("open a writing review"):
        journey.assert_home()
        journey.start_session()
        journey.wait("From memory", timeout=5)
        sample = _target_from_state(state, journey.texts())
        if sample is None:
            raise JourneyFailure("could not identify the writing target")
        strokes = character_guide(sample["character"], state.settings.get("studyMode", "SIMPLIFIED"))
        if not strokes:
            raise JourneyFailure("target has no guide strokes")
        # Deliberately write only one stroke so the coach has a missing-stroke
        # lesson to show, without relying on recognition.
        if not journey.draw_guide([max(strokes, key=len)]):
            raise JourneyFailure("could not draw the sample stroke")
        time.sleep(0.3)
        journey.tap("Check & grade")
        headlines = {
            "Clean stroke order",
            "One stroke wants to turn",
            "One stroke belongs earlier",
            "One stroke is waiting",
            "A little extra ink",
            "Almost in order",
        }
        deadline = time.time() + 6
        coach_lines = []
        while time.time() < deadline and not coach_lines:
            coach_lines = [text for text in journey.texts() if text in headlines]
            if not coach_lines:
                time.sleep(0.25)
        if not coach_lines:
            raise JourneyFailure(f"stroke coach headline missing: {journey.texts()}")
        journey.screenshot("stroke-coach")
        _grade_feedback(journey)
    with journey.step("leave"):
        journey.leave_session("End session")
    journey.assert_no_crash()


def stroke_clean(journey: Journey, state: ScenarioState) -> None:
    """Trace every guide stroke as shown. The coach must not invent a problem."""
    with journey.step("open a writing review"):
        journey.assert_home()
        journey.start_session()
        journey.wait("From memory", timeout=5)
        sample = _target_from_state(state, journey.texts())
        if sample is None:
            raise JourneyFailure("could not identify the writing target")
        strokes = character_guide(sample["character"], state.settings.get("studyMode", "SIMPLIFIED"))
        if not strokes:
            raise JourneyFailure("target has no guide strokes")
        if not journey.draw_guide(strokes):
            raise JourneyFailure("could not trace the full guide")
        time.sleep(0.4)
        journey.tap("Check & grade")
        deadline = time.time() + 8
        headline = None
        while time.time() < deadline and headline is None:
            texts = journey.texts()
            for candidate in (
                "Clean stroke order",
                "One stroke wants to turn",
                "One stroke belongs earlier",
                "One stroke is waiting",
                "A little extra ink",
                "Almost in order",
            ):
                if candidate in texts:
                    headline = candidate
                    break
            if headline is None:
                time.sleep(0.25)
        if headline != "Clean stroke order":
            raise JourneyFailure(
                f"a correct trace was not read as clean: {headline}; {journey.texts()}"
            )
        journey.screenshot("stroke-clean")
        journey.screenshot("stroke-clean-coach")
        _grade_feedback(journey)
    with journey.step("leave"):
        journey.leave_session("End session")
    journey.assert_no_crash()


def pronounce_kana(journey: Journey, state: ScenarioState) -> None:
    """A kana review must not print its romaji reading in the meaning."""
    with journey.step("open a kana pronunciation review"):
        journey.assert_home()
        journey.start_session()
        journey.wait("Type the reading", timeout=8)
        texts = journey.texts()
        character = _current_character(journey)
        reading = character_reading(character, state.settings.get("studyMode", "JAPANESE")) if character else None
        if character and reading:
            leaked = [text for text in texts if "hiragana for" in text.lower() and "…”" not in text]
            if leaked:
                raise JourneyFailure(f"kana meaning still shows its reading: {leaked}")
        journey.screenshot("pronounce-kana")
    with journey.step("wrong answer still gives feedback"):
        _type_and_check(journey, "zzz")
        if not journey.exists("Not quite", timeout=4):
            raise JourneyFailure("wrong pronunciation did not show Not quite")
        _grade_feedback(journey)
    with journey.step("leave"):
        journey.leave_session("End session")
    journey.assert_no_crash()


def _settings_section(journey: Journey, label: str) -> None:
    journey.open_tab("Settings")
    journey.wait("Make Bihua yours", timeout=8, exact=False)
    # Settings rows use text; the bottom navigation uses content descriptions.
    # "Practice" exists in both, so use the row text explicitly. The hierarchy
    # fallback matters because uiautomator's idle wait can fail after a theme
    # switch even though the row is on screen.
    for _ in range(8):
        if journey.exists_text(label):
            break
        journey.scroll_down()
        time.sleep(0.3)
    if not journey.exists_text(label):
        # Help is below several groups. Search narrows the list. Re-find the
        # field after every edit: large text and tablet recomposition can
        # stale the node.
        for _ in range(3):
            try:
                field = journey.d(className="android.widget.EditText")
                if not field.exists:
                    time.sleep(0.3)
                    continue
                field.click()
                time.sleep(0.2)
                field.set_text(label)
            except Exception:
                time.sleep(0.3)
                continue
            time.sleep(0.8)
            if journey.exists_text(label):
                break
    if not journey.exists_text(label):
        raise JourneyFailure(f"settings row missing: {label}")
    journey.tap_text(label)
    time.sleep(0.5)


def _search_character(journey: Journey, glyph: str) -> None:
    field = journey.d(className="android.widget.EditText")
    if not field.exists:
        raise JourneyFailure("character search field missing")
    for _ in range(3):
        field.click()
        time.sleep(0.2)
        field.set_text(glyph)
        time.sleep(0.7)
        if field.get_text() == glyph:
            break
    else:
        raise JourneyFailure(f"character search did not accept {glyph}")
    # At large text the soft keyboard can cover the result tile. Hide it before
    # asking the list for a tile so the tap lands on the result, not the IME.
    # uiautomator2's hide_keyboard() installs AdbKeyboard, which is flaky on
    # this harness, so dismiss the IME with the system back key when it is up.
    tile = journey.d(descriptionContains=f"{glyph},")
    if not tile.exists:
        tile = journey.d(descriptionContains=glyph)
    if not tile.exists and "mInputShown=true" in journey.device.shell("dumpsys input_method"):
        # Back only closes the keyboard here. It is never pressed while the
        # result tile is already visible, because on phone that leaves Characters.
        journey.d.press("back")
        time.sleep(0.4)
        tile = journey.d(descriptionContains=f"{glyph},")
        if not tile.exists:
            tile = journey.d(descriptionContains=glyph)
    if not tile.exists:
        raise JourneyFailure(f"no character tile for {glyph}")
    tile.click()
    time.sleep(0.8)


def paper_compare(journey: Journey, state: ScenarioState) -> None:
    """Paper check and Compare, including guide modes and both back arrows."""
    with journey.step("open a character"):
        journey.open_tab("Characters")
        journey.wait("Character, meaning, or reading", timeout=8, exact=False)
        _search_character(journey, "日")
        journey.wait("Write on paper", timeout=8)
    with journey.step("paper check reveal and strokes"):
        journey.tap_visible("Write on paper")
        journey.wait("Paper check", timeout=8)
        journey.wait("Write it on paper", timeout=6, exact=False)
        journey.tap("Show answer")
        journey.wait("Hide answer", timeout=6)
        journey.tap("Strokes")
        journey.wait("Play stroke order", timeout=6)
        journey.screenshot("paper-check")
        journey.tap("Back to character details")
        journey.wait("Write on paper", timeout=8)
    with journey.step("compare two characters and write both"):
        journey.tap_visible("Compare")
        journey.wait("Add a character to compare", timeout=8)
        journey.tap("Add a character to compare")
        journey.wait("Add a character", timeout=6, exact=False)
        picker = journey.d(className="android.widget.EditText")
        if picker.exists:
            picker.set_text("一")
            time.sleep(0.8)
            # The search field also contains the typed glyph. The row's
            # character is a TextView; the field is an EditText.
            row = journey.d(className="android.widget.TextView", text="一")
            if row.exists:
                row.click()
            else:
                box = journey.d(className="android.widget.CheckBox")
                if not box.exists:
                    raise JourneyFailure("compare picker row missing")
                box.click()
            # The dialog can shift while the IME hides. Wait for it to settle,
            # then retry once if the first tap lands mid-animation.
            time.sleep(2.0)
        for _ in range(2):
            journey.tap("Done")
            if not journey.exists("Add a character", timeout=2.0):
                break
        else:
            journey.d.press("back")
        journey.wait("Write both", timeout=8)
        journey.tap("Write both")
        journey.wait("Character 1 of 2", timeout=8, exact=False)
        for guide in ("Faint", "Strokes", "Off"):
            journey.tap(guide)
        journey.screenshot("compare-write")
        journey.tap("Next character")
        journey.wait("Character 2 of 2", timeout=6, exact=False)
        journey.tap("Back to character details")
        journey.wait("Write on paper", timeout=8)
    with journey.step("return home"):
        journey.open_tab("Home")
        journey.assert_home()


def study_lists(journey: Journey, state: ScenarioState) -> None:
    """Create a list, apply a preset, and study it without touching Home first."""
    with journey.step("create a named list"):
        _settings_section(journey, "Study")
        journey.tap("Open study lists")
        journey.wait("Study lists", timeout=8)
        journey.tap("New list")
        journey.wait("New study list", timeout=6, exact=False)
        field = journey.d(className="android.widget.EditText")
        if not field.exists:
            raise JourneyFailure("list name field missing")
        field.set_text("QA list")
        journey.tap("Create")
        journey.wait("Quick add goes to", timeout=6, exact=False)
        journey.assert_text("QA list")
    with journey.step("apply a preset and study it"):
        journey.tap_visible("Starter")
        journey.wait("Starter", timeout=6)
        journey.tap("Study")
        deadline = time.time() + 12
        while time.time() < deadline:
            if journey.exists("Review ", timeout=0.1, exact=False):
                break
            if journey.exists("Lesson ", timeout=0.1, exact=False):
                break
            time.sleep(0.2)
        else:
            raise JourneyFailure("list study did not start a session")
        journey.screenshot("study-list-session")
        journey.leave_session("End session")
    with journey.step("list survived the session"):
        _settings_section(journey, "Study")
        journey.tap("Open study lists")
        journey.wait("Study lists", timeout=8)
        journey.assert_text("QA list")
        journey.assert_text("Starter")
        journey.tap("Close study lists")
        journey.open_tab("Home")
        journey.assert_home()


def appearance(journey: Journey, state: ScenarioState) -> None:
    """Switch dark and light and verify every tab still renders."""
    with journey.step("switch to dark"):
        _settings_section(journey, "Display")
        journey.tap("Dark")
        time.sleep(0.8)
        journey.screenshot("theme-dark-settings")
    with journey.step("visit every tab in dark"):
        for tab in ("Home", "Characters", "Progress", "Practice"):
            journey.open_tab(tab)
            time.sleep(0.5)
            journey.screenshot(f"theme-dark-{tab.lower()}")
    with journey.step("switch back to light"):
        journey.open_tab("Settings")
        journey.tap_visible("Display")
        journey.tap("Light")
        time.sleep(0.5)
        journey.open_tab("Home")
        journey.assert_home()
        journey.screenshot("theme-light-home")


def word_audio(journey: Journey, state: ScenarioState) -> None:
    """The Word of the Day speaker is present, tappable, and does not crash."""
    with journey.step("word of the day is on Home"):
        journey.assert_home()
        journey.wait("Word of the day", timeout=8)
    with journey.step("tap the speaker"):
        journey.tap("Hear the word")
        time.sleep(1.5)
        journey.assert_home()
        journey.assert_no_crash()


SETTINGS_MARKERS = [
    ("Profile", "Your name"),
    ("Practice", "Practice modes"),
    ("Study", "Day starts at"),
    ("Reminders", "Study reminders"),
    ("Schedule", "Memory model"),
    ("Writing", "Brush"),
    ("Display", "Orientation"),
    ("Data", "Backups stay with you"),
    ("Sync", "Move settings and progress"),
    ("Help & tour", "Grading"),
    ("About", "Made by Shadowsx3"),
]


def navigation_deep(journey: Journey, state: ScenarioState) -> None:
    """Walk every settings section and leave overlays with the system back key."""
    with journey.step("settings sections keep their back stack"):
        for label, marker in SETTINGS_MARKERS:
            _settings_section(journey, label)
            journey.wait(marker, timeout=8, exact=False)
            journey.d.press("back")
            journey.wait("Make Bihua yours", timeout=8, exact=False)
    with journey.step("character detail and its overlays"):
        journey.open_tab("Characters")
        journey.wait("Character, meaning, or reading", timeout=8, exact=False)
        # Search is covered by paper-compare and dictionary. Here open the
        # first visible tile so the test stays about back-stack behaviour.
        tile = journey.d(descriptionContains="爱, ai4")
        if not tile.exists:
            tile = journey.d(descriptionContains=", HSK 1")
        if not tile.exists:
            raise JourneyFailure("no character tile to open")
        time.sleep(0.8)
        for _ in range(2):
            if journey.exists("Add to list", timeout=1.5):
                break
            tile.click()
            time.sleep(0.9)
            if journey.exists("Add to list", timeout=1.5):
                break
        else:
            raise JourneyFailure("character tile did not open its detail")
        journey.tap_visible("Write on paper", max_scrolls=8)
        journey.wait("Paper check", timeout=8)
        journey.d.press("back")
        if journey.ensure_visible("Write on paper", max_scrolls=8) is None:
            raise JourneyFailure("detail did not return after paper check")
        journey.tap_visible("Compare")
        journey.wait("Compare", timeout=8)
        journey.d.press("back")
        if journey.ensure_visible("Write on paper", max_scrolls=8) is None:
            raise JourneyFailure("detail did not return after compare")
        journey.d.press("back")
        journey.wait("Character, meaning, or reading", timeout=8, exact=False)
    with journey.step("practice picker closes"):
        journey.open_tab("Practice")
        journey.wait("Draw freely", timeout=8, exact=False)
        journey.tap("Change")
        journey.wait("Choose a character", timeout=8, exact=False)
        journey.d.press("back")
        journey.wait("Draw freely", timeout=8, exact=False)
    with journey.step("study lists close back to settings"):
        _settings_section(journey, "Study")
        journey.tap("Open study lists")
        journey.wait("Study lists", timeout=8)
        journey.d.press("back")
        journey.wait("Day starts at", timeout=8, exact=False)
        journey.d.press("back")
        journey.wait("Make Bihua yours", timeout=8, exact=False)
    with journey.step("study tools sheet closes"):
        journey.open_tab("Home")
        journey.assert_home()
        # A fresh install spends the first moment JIT-compiling the shell and
        # loading the content/progress stores. Let it settle before the tap.
        time.sleep(1.5)
        journey.tap_visible("Manage study tools")
        journey.wait("Pinned", timeout=8)
        journey.d.press("back")
        for _ in range(3):
            journey.scroll_up()
        journey.assert_home()
    journey.assert_no_crash()


def anki(journey: Journey, state: ScenarioState) -> None:
    """Reveal-only reviews never show the canvas or the reading field."""
    with journey.step("settings explain the option"):
        _settings_section(journey, "Practice")
        if journey.ensure_visible("Anki-style review") is None:
            raise JourneyFailure("Anki-style settings card missing")
        journey.assert_text("Writing: tap to reveal")
        journey.assert_text("Pronunciation: tap to reveal")
        journey.screenshot("anki-settings")
        journey.d.press("back")
        journey.wait("Make Bihua yours", timeout=8, exact=False)
    with journey.step("reviews stay reveal-only"):
        journey.open_tab("Home")
        journey.assert_home()
        journey.start_session()
        graded = 0
        for _ in range(8):
            if journey.exists("Session complete", timeout=0.4):
                break
            if journey.exists("Check & grade", timeout=0.2):
                raise JourneyFailure("Anki writing showed the canvas action")
            if journey.exists("Type the reading", timeout=0.2):
                raise JourneyFailure("Anki pronunciation showed the typing field")
            if journey.exists("Next: trace", timeout=0.2):
                raise JourneyFailure("Anki writing still asked for a trace")
            if journey.exists("Next: show answer", timeout=0.2):
                journey.tap("Next: show answer")
            if journey.exists("Show answer", timeout=3):
                journey.tap("Show answer")
                journey.screenshot(f"anki-answer-{graded}")
            journey.tap_any(["Good", "Easy", "Hard", "Again"], timeout=6)
            graded += 1
            time.sleep(0.4)
        if graded == 0:
            raise JourneyFailure("no Anki review was graded")
    with journey.step("leave"):
        if journey.exists("Session complete", timeout=1):
            journey.tap("Back to home")
            journey.assert_home()
        else:
            journey.leave_session("End session")
    journey.assert_no_crash()


def meaning(journey: Journey, state: ScenarioState) -> None:
    """Type a meaning, reveal it, add a synonym, and grade."""
    with journey.step("settings expose the meaning check"):
        _settings_section(journey, "Practice")
        if journey.ensure_visible("Meaning") is None:
            raise JourneyFailure("Meaning setting missing")
        journey.assert_text("Meaning: tap to reveal")
        journey.d.press("back")
        journey.wait("Make Bihua yours", timeout=8, exact=False)
    with journey.step("type and add a synonym"):
        journey.open_tab("Home")
        journey.assert_home()
        journey.start_session()
        seen = False
        for _ in range(8):
            if journey.exists("Session complete", timeout=0.4):
                break
            if journey.exists("What does it mean?", timeout=0.3):
                seen = True
                field = journey.d(className="android.widget.EditText")
                if not field.exists:
                    raise JourneyFailure("meaning answer field missing")
                field.click()
                field.set_text("a personal wording")
                journey.tap("Show answer")
                journey.wait("Add a synonym", timeout=6)
                journey.tap_visible("Add a synonym")
                time.sleep(0.4)
                draft = journey.d(className="android.widget.EditText")
                if draft.exists:
                    draft.set_text("a personal wording")
                journey.tap("Add")
                journey.wait("Also accepted", timeout=6)
                journey.wait("a personal wording", timeout=4)
                journey.screenshot("meaning-answer")
            else:
                if journey.exists("Show answer", timeout=1):
                    journey.tap("Show answer")
            journey.tap_visible("Good")
            time.sleep(0.4)
        if not seen:
            raise JourneyFailure("no meaning review appeared")
    with journey.step("leave"):
        if journey.exists("Session complete", timeout=1):
            journey.tap("Back to home")
            journey.assert_home()
        else:
            journey.leave_session("End session")


def meaning_anki(journey: Journey, state: ScenarioState) -> None:
    """Anki meaning has no typing field and still offers synonyms after reveal."""
    with journey.step("meaning reveal-only"):
        journey.open_tab("Home")
        journey.assert_home()
        journey.start_session()
        seen = False
        for _ in range(8):
            if journey.exists("Session complete", timeout=0.4):
                break
            if journey.exists("What does it mean?", timeout=0.3):
                seen = True
                if journey.exists("In your own words", timeout=0.2):
                    raise JourneyFailure("Anki meaning showed a typing field")
                journey.tap("Show answer")
                journey.wait("Add a synonym", timeout=6)
                journey.screenshot("meaning-anki-answer")
            elif journey.exists("Show answer", timeout=1):
                journey.tap("Show answer")
            journey.tap_visible("Good")
            time.sleep(0.4)
        if not seen:
            raise JourneyFailure("no meaning review appeared")
    with journey.step("leave"):
        if journey.exists("Session complete", timeout=1):
            journey.tap("Back to home")
            journey.assert_home()
        else:
            journey.leave_session("End session")


def synonyms(journey: Journey, state: ScenarioState) -> None:
    """Add and remove a synonym from character detail."""
    with journey.step("open a character detail"):
        journey.open_tab("Characters")
        journey.wait("Character, meaning, or reading", timeout=8, exact=False)
        _search_character(journey, "爱")
        if journey.ensure_visible("Your synonyms", max_scrolls=8) is None:
            raise JourneyFailure("synonym card missing from character detail")
    with journey.step("add a synonym"):
        journey.tap_any(["Add synonym", "Add"], timeout=6)
        journey.wait("Add a synonym", timeout=6)
        journey.screenshot("character-synonym-dialog")
        # On wide layouts the character search field is still behind the
        # dialog. Focus the dialog field by tapping its placeholder first.
        placeholder = journey.d(text="For example, affection")
        if placeholder.exists:
            placeholder.click()
        field = journey.d(className="android.widget.EditText", focused=True)
        if not field.exists or field.get_text().strip() == "爱":
            fields = journey.d(className="android.widget.EditText")
            if fields.count < 1:
                raise JourneyFailure("synonym field missing")
            field = fields[fields.count - 1]
            field.click()
        field.set_text("affection")
        journey.tap("Add")
        journey.wait("affection", timeout=6)
        journey.screenshot("character-synonyms")
    with journey.step("remove a synonym"):
        journey.tap("Remove affection")
        time.sleep(0.5)
        if journey.exists("affection", timeout=0.5):
            raise JourneyFailure("synonym was not removed")
    with journey.step("bottom of detail is reachable"):
        if journey.ensure_visible("Reset this character", max_scrolls=24) is None:
            raise JourneyFailure("detail bottom is unreachable")
        journey.screenshot("character-detail-bottom")
    journey.assert_no_crash()


def tour(journey: Journey, state: ScenarioState) -> None:
    """Skip the invite, then replay Mò's live overlay tour from Settings."""
    with journey.step("invite is optional"):
        journey.assert_home()
        journey.wait("Need a hand?", timeout=8)
        journey.assert_text("One minute with Mò")
        journey.screenshot("tour-invite")
        journey.tap("Not now")
        time.sleep(0.4)
        if journey.exists("Need a hand?", timeout=0.4):
            raise JourneyFailure("tour invite did not dismiss")
    with journey.step("start from help"):
        _settings_section(journey, "Help & tour")
        journey.wait("Grading", timeout=8)
        journey.tap_visible("Start tour", max_scrolls=10)
        journey.wait("Hi, I’m Mò", timeout=8)
        journey.screenshot("tour-1")
    with journey.step("tap the dialogue to move on"):
        time.sleep(2.8)
        journey.tap("Tap to continue")
        time.sleep(2.4)
        journey.wait("A new character", timeout=8)
        journey.assert_text("you grade it")
        journey.screenshot("tour-2")
    steps = [
        ("Why things come back", "hard means sooner"),
        ("Look anything up", "strokes and words"),
        ("Practice without pressure", "exactly where they are"),
        ("The four buttons", "Easy: instant"),
        ("No pen today?", "Off by default"),
        ("Paper and lists", "Settings → Study"),
        ("That’s the tour", "Go write something"),
    ]
    for index, (title, body) in enumerate(steps, start=3):
        journey.tap("Next")
        time.sleep(2.4)
        journey.wait(title, timeout=8)
        journey.assert_text(body)
        journey.screenshot(f"tour-{index}")
    journey.tap("Back to app")
    journey.assert_home()


def settings_outfit(journey: Journey, state: ScenarioState) -> None:
    """Pin a widget outfit from Display, then return it to Auto."""
    with journey.step("pin a widget outfit"):
        _settings_section(journey, "Display")
        for _ in range(8):
            if journey.exists_text("Mò's widget outfit"):
                break
            journey.scroll_down()
            time.sleep(0.3)
        if not journey.exists_text("Mò's widget outfit"):
            raise JourneyFailure("widget outfit row missing")
        journey.tap_text("Change")
        journey.wait("Mò's outfit", timeout=6)
        issues = journey.a11y_unlabeled()
        if issues:
            raise JourneyFailure(f"outfit dialog has unlabeled clickables: {issues[:3]}")
        journey.screenshot("widget-outfit-dialog")
        journey.tap("Witch hat")
        time.sleep(0.5)
        if journey.exists("Mò's outfit", timeout=0.5):
            raise JourneyFailure("outfit dialog did not close")
        if not journey.exists_text("Witch hat"):
            raise JourneyFailure("outfit did not persist")
        journey.screenshot("widget-outfit-pinned")
    with journey.step("leave the section"):
        journey.d.press("back")
        journey.wait("Make Bihua yours", timeout=8, exact=False)


def nav_gap(journey: Journey, state: ScenarioState) -> None:
    """The scroll viewport meets the floating bottom bar without a leftover gap."""
    with journey.step("bottom bar sits against content"):
        dpi = journey.density_dpi()
        for tab in ("Home", "Characters", "Progress", "Settings"):
            journey.open_tab(tab)
            time.sleep(0.8)
            gap_px = journey.bottom_nav_gap_px()
            if gap_px is None:
                continue
            gap_dp = gap_px * 160 / dpi
            if gap_dp > 14:
                raise JourneyFailure(f"{tab} leaves {gap_dp:.1f}dp above the nav bar")
            if gap_dp < -2:
                raise JourneyFailure(f"{tab} content overlaps the nav bar by {-gap_dp:.1f}dp")
        journey.screenshot("nav-gap-settings")


def progress_empty(journey: Journey, state: ScenarioState) -> None:
    """An untouched course has calm, non-punitive empty states."""
    with journey.step("empty progress"):
        journey.open_tab("Progress")
        journey.wait("Momentum", timeout=8)
        journey.assert_text("New window")
        journey.assert_text("No practice in this window yet")
        journey.screenshot("progress-empty-top")
        for target in (
            "No reviews yet. Grade a few characters and this fills in.",
            "Nothing scheduled yet",
            "No memory data yet",
            "No reviews in this window yet",
            "Nothing is stuck right now",
        ):
            for _ in range(12):
                if journey.exists_text(target, exact=False):
                    break
                journey.scroll_down()
                time.sleep(0.3)
            if not journey.exists_text(target, exact=False):
                raise JourneyFailure(f"missing empty-state text: {target}")
        journey.screenshot("progress-empty-bottom")


def progress_ranges(journey: Journey, state: ScenarioState) -> None:
    """Progress stats follow the selected window."""
    with journey.step("switch the range"):
        journey.open_tab("Progress")
        journey.wait("Progress", timeout=8)
        streak = journey.value_above("Streak")
        if not streak:
            raise JourneyFailure("streak tile value missing")
        for short, label in (("7D", "Last 7 days"), ("90D", "Last 90 days"), ("1Y", "Last year")):
            journey.tap_text(short)
            time.sleep(0.8)
            journey.assert_text(label)
            journey.assert_text("Momentum")
            if not journey.exists_text("Streak"):
                raise JourneyFailure(f"streak tile missing for {label}")
            if journey.value_above("Streak") != streak:
                raise JourneyFailure(f"streak was capped by {label}")
        journey.tap_text("30D")
        time.sleep(0.8)
        journey.assert_text("Last 30 days")
        journey.screenshot("progress-ranges")


def grade_help(journey: Journey, state: ScenarioState) -> None:
    """The session grade prompt opens the plain-word legend."""
    with journey.step("open the grade legend"):
        journey.assert_home()
        journey.start_session()
        journey.wait("Show answer", timeout=10)
        journey.tap("Show answer")
        if (
            journey.ensure_visible("How did that feel?", max_scrolls=6) is None
            and journey.ensure_visible("Choose a grade.", max_scrolls=6) is None
        ):
            raise JourneyFailure("grade prompt did not appear after the reveal")
        journey.tap("What do the grades mean?")
        journey.wait("What the grades mean", timeout=6)
        issues = journey.a11y_unlabeled()
        if issues:
            raise JourneyFailure(f"grade dialog has unlabeled clickables: {issues[:3]}")
        journey.assert_text("It slipped away.")
        journey.assert_text("Instant and effortless.")
        journey.screenshot("grade-help")
        journey.tap("Got it")
    with journey.step("grade and leave"):
        journey.tap_visible("Good")
        journey.leave_session("End session")


def accessibility(journey: Journey, state: ScenarioState) -> None:
    issues: list[str] = []
    for screen in ("Home", "Characters", "Progress", "Practice", "Settings"):
        with journey.step(f"a11y {screen}"):
            journey.open_tab(screen)
            time.sleep(0.3)
            found = journey.a11y_unlabeled()
            if found:
                issues.append(f"{screen}: {len(found)} unlabeled clickable nodes")
            journey.screenshot(f"a11y-{screen.lower()}")
    journey.artifact_dir.joinpath("a11y.txt").write_text("\n".join(issues) or "no unlabeled clickable nodes")


SPELL_FAMILIES = [
    "Water", "Ice", "Snow", "Rain", "Fire", "Lightning", "Wind", "Cloud", "Sun",
    "Moon", "Light", "Shadow", "Earth", "Stone", "Mountain", "Wood", "Metal",
    "Creature", "Spark", "Gesture", "Vessel", "Mind", "Dream", "Sound", "Time",
]


FAMILY_IDS = {
    "Water": "water", "Ice": "ice", "Snow": "snow", "Rain": "rain",
    "Fire": "fire", "Lightning": "lightning", "Wind": "wind", "Cloud": "cloud",
    "Sun": "sun", "Moon": "moon", "Light": "light", "Shadow": "shadow",
    "Earth": "earth", "Stone": "stone", "Mountain": "mountain", "Wood": "wood",
    "Metal": "metal", "Creature": "animal", "Spark": "person", "Gesture": "hand",
    "Vessel": "boat", "Mind": "mind", "Dream": "dream", "Sound": "sound",
    "Time": "time",
}


def _spell_character_for(label: str) -> str | None:
    """First character of a family in the shipped spellbook."""
    import csv

    family = FAMILY_IDS.get(label)
    if family is None:
        return None
    path = Path(__file__).resolve().parents[1] / "app/src/main/assets/spellbook.csv"
    with path.open(encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            if row["family"] == family:
                return row["character"]
    return None


def spell_entry(journey: Journey, state: ScenarioState) -> None:
    """The spell quest is one tap from Home, framed as extra practice."""
    with journey.step("open from the Home study tools card"):
        journey.open_tab("Home")
        journey.assert_home()
        time.sleep(1.2)
        journey.tap_visible("Spell quest", max_scrolls=8)
        journey.wait("The road ahead", timeout=10)
        journey.screenshot("spell-entry")
        journey.tap_visible("Leave", max_scrolls=4)
    journey.assert_no_crash()


def spell_quest(journey: Journey, state: ScenarioState) -> None:
    """The endless road, reached from Study tools, casting from the book."""
    with journey.step("open the spell quest"):
        journey.open_tab("Home")
        journey.assert_home()
        journey.tap_visible("Manage study tools")
        journey.wait("Study tools", timeout=8)
        journey.tap("Spell quest")
        journey.wait("The road ahead", timeout=10)
        issues = journey.a11y_unlabeled()
        if issues:
            raise JourneyFailure(f"spell gate has unlabeled controls: {issues[:3]}")
        journey.screenshot("spell-gate")
    with journey.step("start the road"):
        journey.tap_visible("Start the road", max_scrolls=8)
        journey.wait("Draw", timeout=10)
        journey.screenshot("spell-scene")
    with journey.step("gift from the book"):
        # "From the book" is a reading gift: the real strokes are traced into the
        # circle, then the circle waits until the learner continues.
        journey.wait("Grimoire", timeout=8)
        journey.tap_visible("Grimoire")
        journey.wait("Close book", timeout=10)
        issues = journey.a11y_unlabeled()
        if issues:
            raise JourneyFailure(f"grimoire has unlabeled controls: {issues[:3]}")
        journey.screenshot("spell-kinds")
        # The book opens on the cover; the Flame tab turns to its spells.
        journey.tap_visible("火")
        time.sleep(0.8)
        # Chapter seals sit in the tabs above the page; the spell tiles come later
        # in the tree, so take the last character on screen.
        seals = set("水火云石马桥心时")
        glyphs = [
            text for text in journey.texts()
            if text and len(text) == 1 and "\u4e00" <= text <= "\u9fff" and text not in seals
        ]
        if not glyphs:
            raise JourneyFailure(f"no spell pages in the grimoire. texts={journey.texts()}")
        glyph = glyphs[-1]
        journey.tap_visible(glyph)
        # Mid-drawing: the guide is playing stroke by stroke.
        time.sleep(1.6)
        journey.screenshot("spell-gift-draw")
        # A gift is one step: the result is already on screen while the circle draws.
        deadline = time.time() + 25
        while time.time() < deadline:
            texts = journey.texts()
            if any("It worked" in text or "A different way" in text for text in texts):
                break
            time.sleep(0.3)
        else:
            raise JourneyFailure(f"gift produced no result. texts={journey.texts()}")
        journey.screenshot("spell-result")
    with journey.step("continue the road"):
        if journey.exists("Walk on", timeout=2):
            journey.tap("Walk on")
        else:
            # A wrong pick shows the right answer first, then Walk on.
            journey.tap("Show me one answer")
            journey.wait("Walk on", timeout=25)
            journey.tap("Walk on")
        journey.wait("Draw", timeout=10)
        journey.screenshot("spell-next")
    with journey.step("draw and cast"):
        # The drawing path itself: draw in the circle with touch, then cast.
        journey.tap_visible("Draw", max_scrolls=8)
        journey.wait("Draw in the circle first", timeout=8)
        issues = journey.a11y_unlabeled()
        if issues:
            raise JourneyFailure(f"cast overlay has unlabeled controls: {issues[:3]}")
        # Draw a real spell with the content database's guide strokes, the same
        # way the review journeys write. A plus can be read as 十, which is not
        # a spell in this book.
        guide = character_guide("水", state.settings.get("studyMode", "SIMPLIFIED"))
        if not guide:
            raise JourneyFailure("no guide strokes for 水")
        if not journey.draw_guide(guide):
            raise JourneyFailure("no handwriting canvas to draw on")
        journey.wait("Cast the spell", timeout=8)
        journey.screenshot("spell-drawn")
        journey.tap("Cast the spell")
        deadline = time.time() + 30
        outcome = None
        while time.time() < deadline and outcome is None:
            texts = journey.texts()
            if any("A different way" in text for text in texts):
                outcome = "nudge"
            elif any("It worked" in text for text in texts):
                outcome = "worked"
            elif any("is not in this book yet" in text or "Mò read" in text for text in texts):
                outcome = "unreadable"
            else:
                time.sleep(0.4)
        if outcome is None:
            raise JourneyFailure(f"cast produced no result. texts={journey.texts()}")
        journey.screenshot("spell-cast")
        if outcome == "unreadable":
            # A readable character that is not a spell here: back to the drawing.
            journey.tap("Go back")
        else:
            journey.tap("Walk on for now" if outcome == "nudge" else "Walk on")
        journey.wait("Draw", timeout=10)
    journey.assert_no_crash()


RUNNERS = {
    "fresh": fresh,
    "daily_sample": daily_sample,
    "navigation": navigation,
    "navigation_deep": navigation_deep,
    "resume": resume,
    "process_death": process_death,
    "completion": completion,
    "drain": drain,
    "learning_state": learning_state,
    "vacation": vacation,
    "ease_back": ease_back,
    "fold_transition": fold_transition,
    "accessibility": accessibility,
    "reminders": reminders,
    "worksheet": worksheet,
    "dictionary": dictionary,
    "word_guide": word_guide,
    "stroke_coach": stroke_coach,
    "stroke_clean": stroke_clean,
    "review_modes": review_modes,
    "pronounce_kana": pronounce_kana,
    "draw_recognition": draw_recognition,
    "paper_compare": paper_compare,
    "study_lists": study_lists,
    "appearance": appearance,
    "word_audio": word_audio,
    "anki": anki,
    "meaning": meaning,
    "meaning_anki": meaning_anki,
    "synonyms": synonyms,
    "tour": tour,
    "settings_outfit": settings_outfit,
    "grade_help": grade_help,
    "spell_quest": spell_quest,
    "spell_entry": spell_entry,
    "progress_ranges": progress_ranges,
    "progress_empty": progress_empty,
    "nav_gap": nav_gap,
}
