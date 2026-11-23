#!/usr/bin/env python3
"""Daily learner journeys and expected states."""
from __future__ import annotations

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
    with journey.step("show the real stroke guide"):
        journey.tap("Strokes")
        journey.wait("Watch the stroke order", timeout=6)
        journey.tap("Play")
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


RUNNERS = {
    "fresh": fresh,
    "daily_sample": daily_sample,
    "navigation": navigation,
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
}
