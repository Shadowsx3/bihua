#!/usr/bin/env python3
"""Daily learner journeys and expected states."""
from __future__ import annotations

from pathlib import Path

import random
import re
import subprocess
import time
from dataclasses import dataclass, field

from driver import Journey, JourneyFailure
from state import ScenarioState, character_guide, character_reading, character_variants

RESULT_LABELS = ("It worked", "A different way", "Not that shape", "Another chapter")


@dataclass
class ScenarioSpec:
    name: str
    level: str = "quick"
    all_profiles: bool = False
    group: str | None = None
    state: dict = field(default_factory=dict)
    runner: str = "daily_sample"
    font_scale: float | None = None
    fresh_data: bool = False


SCENARIOS: list[ScenarioSpec] = [
    ScenarioSpec("fresh", level="quick", all_profiles=True, runner="fresh"),
    ScenarioSpec("day1", level="quick", all_profiles=True, state={"reviews": 0, "new": 5, "days": 1}, runner="daily_sample"),
    ScenarioSpec("daily20", level="quick", all_profiles=True, state={"reviews": 20, "new": 5, "days": 7, "known": 8}, runner="daily_sample"),
    ScenarioSpec("daily80", level="quick", all_profiles=True, state={"reviews": 80, "new": 5, "days": 30, "known": 40}, runner="daily_sample"),
    ScenarioSpec("navigation", level="quick", all_profiles=True, state={"reviews": 20, "new": 5, "days": 10}, runner="navigation"),
    ScenarioSpec("navigation-deep", level="standard", state={"reviews": 20, "new": 5, "days": 10}, runner="navigation_deep"),
    ScenarioSpec("characters-scroll", level="standard", state={"reviews": 20, "new": 5, "days": 10}, runner="characters_scroll"),
    ScenarioSpec("study-tools-pdf", level="standard", state={"reviews": 12, "new": 3, "days": 20}, runner="study_tools_pdf"),
    ScenarioSpec("monkey", level="full", all_profiles=True, state={"reviews": 20, "new": 5, "days": 10}, runner="monkey"),
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
    ScenarioSpec("writing-reveal", level="standard", state={"reviews": 1, "new": 0, "goal": 0, "days": 7, "task_mode": "WRITE", "review_order": "OLDEST_FIRST"}, runner="writing_reveal"),
    ScenarioSpec("brush-presets", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="brush_presets"),
    ScenarioSpec("practice-check", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="practice_check"),
    ScenarioSpec("practice-slow", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="practice_slow"),
    ScenarioSpec("spell-offline", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="spell_offline"),
    ScenarioSpec("model-notice", level="standard", state={"reviews": 3, "new": 2, "days": 3}, runner="model_notice"),
    ScenarioSpec("offline-first-run", level="full", state={"onboarded": False, "reviews": 5, "new": 2, "days": 5}, runner="offline_first_run"),
    ScenarioSpec("model-offline-switch", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="model_offline_switch"),
    ScenarioSpec("rotation", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="rotation"),
    ScenarioSpec("rotation-state", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="rotation_state"),
    ScenarioSpec("rotation-onboarding", level="standard", state={"onboarded": False, "reviews": 5, "new": 2, "days": 5}, runner="rotation_onboarding"),
    ScenarioSpec("screen-pass", level="standard", state={"reviews": 3, "new": 0, "goal": 0, "days": 5, "task_mode": "WRITE", "review_order": "OLDEST_FIRST"}, runner="screen_pass", fresh_data=True),
    ScenarioSpec("screen-pass-dark", level="full", state={"reviews": 3, "new": 0, "goal": 0, "days": 5, "task_mode": "WRITE", "review_order": "OLDEST_FIRST", "theme": "DARK"}, runner="screen_pass", fresh_data=True),
    ScenarioSpec("screen-pass-large", level="full", state={"reviews": 3, "new": 0, "goal": 0, "days": 5, "task_mode": "WRITE", "review_order": "OLDEST_FIRST"}, runner="screen_pass", fresh_data=True, font_scale=1.5),
    ScenarioSpec("screen-pass-ja", level="full", state={"reviews": 3, "new": 0, "goal": 0, "days": 5, "task_mode": "WRITE", "review_order": "OLDEST_FIRST", "mode": "JAPANESE"}, runner="screen_pass", fresh_data=True),
    ScenarioSpec("screen-pass-ko", level="full", state={"reviews": 3, "new": 0, "goal": 0, "days": 5, "task_mode": "WRITE", "review_order": "OLDEST_FIRST", "mode": "KOREAN"}, runner="screen_pass", fresh_data=True),
    ScenarioSpec("screen-pass-traditional", level="full", state={"reviews": 3, "new": 0, "goal": 0, "days": 5, "task_mode": "WRITE", "review_order": "OLDEST_FIRST", "mode": "TRADITIONAL"}, runner="screen_pass", fresh_data=True),
    ScenarioSpec("settings-pages", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="settings_pages"),
    ScenarioSpec("settings-writing-mode", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="settings_writing_mode"),
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
    ScenarioSpec("reveal-review", level="standard", state={"reviews": 8, "new": 2, "days": 7, "pronunciation": True, "reveal_writing": True, "reveal_pronunciation": True}, runner="reveal_review"),
    ScenarioSpec("meaning", level="standard", state={"reviews": 8, "new": 0, "days": 7, "meaning_practice": True, "reveal_writing": True}, runner="meaning"),
    ScenarioSpec("meaning-reveal", level="standard", state={"reviews": 8, "new": 0, "days": 7, "meaning_practice": True, "reveal_writing": True, "reveal_meaning": True}, runner="meaning_reveal"),
    ScenarioSpec("synonyms", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="synonyms"),
    ScenarioSpec("tour", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="tour"),
    ScenarioSpec("tour-dark", level="standard", state={"reviews": 5, "new": 2, "days": 5, "theme": "DARK"}, runner="tour"),
    ScenarioSpec("settings-outfit", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="settings_outfit"),
    ScenarioSpec("report-flow", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="report_flow"),
    ScenarioSpec("report-flow-dark", level="standard", state={"reviews": 5, "new": 2, "days": 5, "theme": "DARK"}, runner="report_flow"),
    ScenarioSpec("report-flow-large", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="report_flow", font_scale=1.5),
    ScenarioSpec("progress-ranges", level="standard", state={"reviews": 20, "new": 2, "days": 12}, runner="progress_ranges"),
    ScenarioSpec("progress-screen", level="standard", state={"reviews": 30, "new": 5, "known": 20, "days": 30}, runner="progress_screen"),
    ScenarioSpec("progress-all-time", level="standard", state={"reviews": 30, "new": 5, "known": 20, "days": 800}, runner="progress_all_time"),
    ScenarioSpec("progress-custom-range", level="standard", state={"reviews": 30, "new": 5, "known": 20, "days": 30}, runner="progress_custom_range"),
    ScenarioSpec("progress-custom-single-day", level="standard", state={"reviews": 30, "new": 5, "known": 20, "days": 30}, runner="progress_custom_single_day"),
    ScenarioSpec("progress-scripts", level="standard", state={"reviews": 30, "new": 5, "known": 20, "days": 30}, runner="progress_scripts"),
    ScenarioSpec("script-switch-loading", level="standard", state={"reviews": 30, "new": 5, "known": 20, "days": 30}, runner="script_switch_loading"),
    ScenarioSpec("settings-writing-model", level="standard", state={"reviews": 20, "new": 5, "days": 10}, runner="settings_writing_model"),
    ScenarioSpec("known-list-preview", level="standard", state={"reviews": 20, "new": 5, "days": 10}, runner="known_list_preview"),
    ScenarioSpec("progress-pdf-rich", level="standard", state={"reviews": 600, "new": 120, "known": 80, "days": 420, "goal": 10, "review_reasons": True}, runner="progress_pdf_rich"),
    ScenarioSpec("progress-pdf-rich-ja", level="standard", state={"mode": "JAPANESE", "reviews": 600, "new": 120, "known": 80, "days": 420, "goal": 10, "review_reasons": True}, runner="progress_pdf_rich"),
    ScenarioSpec("progress-pdf-rich-ko", level="standard", state={"mode": "KOREAN", "reviews": 600, "new": 120, "known": 80, "days": 420, "goal": 10, "review_reasons": True}, runner="progress_pdf_rich"),
    ScenarioSpec("progress-pdf-empty", level="standard", state={"reviews": 0, "new": 0, "known": 0, "days": 0, "goal": 0}, runner="progress_pdf_rich"),
    ScenarioSpec("progress-pdf-start", level="standard", state={"reviews": 1, "new": 1, "known": 0, "days": 1, "goal": 3}, runner="progress_pdf_rich"),
    ScenarioSpec("progress-pdf-save", level="standard", state={"reviews": 30, "new": 5, "known": 20, "days": 30}, runner="progress_pdf_save"),
    ScenarioSpec("progress-pdf-share", level="standard", state={"reviews": 30, "new": 5, "known": 20, "days": 30}, runner="progress_pdf_share"),
    ScenarioSpec("modes-extra-only", level="standard", state={"reviews": 8, "new": 2, "days": 10, "pronunciation": True, "meaning_practice": True, "writing": False}, runner="study_modes"),
    ScenarioSpec("modes-pronounce-only", level="standard", state={"reviews": 6, "new": 2, "days": 10, "pronunciation": True, "writing": False}, runner="study_modes"),
    ScenarioSpec("modes-meaning-only", level="standard", state={"reviews": 6, "new": 2, "days": 10, "meaning_practice": True, "writing": False}, runner="study_modes"),
    ScenarioSpec("modes-all", level="standard", state={"reviews": 16, "new": 2, "days": 10, "pronunciation": True, "meaning_practice": True}, runner="study_modes"),
    ScenarioSpec("grade-reasons", level="standard", state={"reviews": 6, "new": 0, "days": 10, "review_reasons": True}, runner="grade_with_reason"),
    ScenarioSpec("mnemonic-studio", level="standard", state={"reviews": 5, "new": 2, "days": 10}, runner="mnemonic_studio"),
    ScenarioSpec("characters-rotation", level="standard", state={"reviews": 5, "new": 2, "days": 10}, runner="characters_rotation"),
    ScenarioSpec("word-practice-home-back", level="standard", state={"reviews": 5, "new": 2, "days": 10}, runner="word_practice_home_back"),
    ScenarioSpec("word-whole-canvas", level="standard", state={"reviews": 5, "new": 2, "days": 10}, runner="word_whole_canvas"),
    ScenarioSpec("word-one-by-one-no-model", level="standard", state={"reviews": 5, "new": 2, "days": 10}, runner="word_one_by_one_no_model"),
    ScenarioSpec("mnemonic-rotation", level="standard", state={"reviews": 5, "new": 2, "days": 10}, runner="mnemonic_rotation"),
    ScenarioSpec("mnemonic-japanese", level="standard", state={"reviews": 5, "new": 2, "days": 10, "mode": "JAPANESE"}, runner="mnemonic_japanese"),
    ScenarioSpec("mnemonic-korean", level="standard", state={"reviews": 5, "new": 2, "days": 10, "mode": "KOREAN"}, runner="mnemonic_korean"),
    ScenarioSpec("mnemonic-kana-hidden", level="standard", state={"reviews": 5, "new": 2, "days": 10, "mode": "JAPANESE", "memory_hooks": True}, runner="mnemonic_script_hidden"),
    ScenarioSpec("mnemonic-hangul-hidden", level="standard", state={"reviews": 5, "new": 2, "days": 10, "mode": "KOREAN", "memory_hooks": True}, runner="mnemonic_script_hidden"),
    ScenarioSpec("lesson-hook", level="standard", state={"reviews": 0, "new": 2, "days": 10, "memory_hooks": True}, runner="lesson_hook"),
    ScenarioSpec("progress-screen-dark", level="standard", state={"reviews": 30, "new": 5, "known": 20, "days": 30, "theme": "DARK"}, runner="progress_screen"),
    ScenarioSpec("progress-screen-large", level="standard", state={"reviews": 30, "new": 5, "known": 20, "days": 30}, runner="progress_screen", font_scale=1.5),
    ScenarioSpec("nav-gap", level="standard", state={"reviews": 20, "new": 2, "days": 12}, runner="nav_gap"),
    ScenarioSpec("progress-empty", level="standard", state={"reviews": 0, "new": 0, "goal": 0, "known": 0, "days": 0}, runner="progress_empty"),
    ScenarioSpec("grade-help", level="standard", state={"reviews": 5, "new": 0, "goal": 0, "days": 7, "reveal_writing": True}, runner="grade_help"),
    ScenarioSpec("spell-quest", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="spell_quest"),
    ScenarioSpec("spell-entry", level="quick", state={"reviews": 5, "new": 2, "days": 5}, runner="spell_entry"),
    ScenarioSpec("spell-flow", level="standard", state={"reviews": 5, "new": 2, "days": 5}, runner="spell_flow"),
    ScenarioSpec("spell-long", level="full", state={"reviews": 5, "new": 2, "days": 5}, runner="spell_long"),
    ScenarioSpec("spell-onboarding", level="standard", state={"onboarded": False, "reviews": 5, "new": 2, "days": 5}, runner="spell_onboarding"),
    ScenarioSpec("onboarding-pages", level="standard", state={"onboarded": False, "reviews": 5, "new": 2, "days": 5}, runner="onboarding_pages"),
    ScenarioSpec("spell-quest-dark", level="full", state={"reviews": 5, "new": 2, "days": 5, "theme": "DARK"}, runner="spell_quest"),
    ScenarioSpec("spell-quest-calm", level="full", state={"reviews": 5, "new": 2, "days": 5, "reduce_motion": True}, runner="spell_quest"),
    ScenarioSpec("spell-quest-ja", level="full", state={"reviews": 5, "new": 2, "days": 5, "mode": "JAPANESE"}, runner="spell_quest"),
    ScenarioSpec("spell-quest-ko", level="full", state={"reviews": 5, "new": 2, "days": 5, "mode": "KOREAN"}, runner="spell_quest"),
    ScenarioSpec("spell-quest-traditional", level="full", state={"reviews": 5, "new": 2, "days": 5, "mode": "TRADITIONAL"}, runner="spell_quest"),
    ScenarioSpec("spell-entry-ja", level="quick", state={"reviews": 5, "new": 2, "days": 5, "mode": "JAPANESE"}, runner="spell_entry"),
    ScenarioSpec("spell-entry-ko", level="quick", state={"reviews": 5, "new": 2, "days": 5, "mode": "KOREAN"}, runner="spell_entry"),
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


def _tap_reason_or_skip(journey: Journey, choose: bool) -> bool:
    """Handle the optional post-grade note. Returns true when it was present."""
    if not journey.exists("Add note for the last review", timeout=0.8):
        return False
    journey.tap("Add note for the last review")
    journey.wait("hard?", timeout=4, exact=False)
    if choose:
        labels = [
            "Stroke order", "Direction", "Missing stroke", "Spacing",
            "Forgot reading", "Tone", "Forgot meaning", "Confused with another",
        ]
        for label in labels:
            if journey.exists(label, timeout=0.2):
                journey.tap(label)
                return True
    journey.tap("Not now")
    return True


def study_modes(journey: Journey, state: ScenarioState) -> None:
    """Walk real lessons and reviews with only the enabled task modes."""
    writing = bool(state.settings.get("writingPractice", True))
    pronunciation = bool(state.settings.get("pronunciationPractice", False))
    meaning = bool(state.settings.get("meaningPractice", False))
    reasons = bool(state.settings.get("reviewReasonsEnabled", False))
    seen: set[str] = set()
    lines: list[str] = []

    with journey.step("start the enabled modes"):
        journey.assert_home()
        journey.start_session()
        time.sleep(0.8)

    with journey.step("walk every lesson and review screen"):
        for index in range(28):
            texts = journey.texts()
            if "Session complete" in texts:
                break
            intro = next((text for text in texts if text.startswith("Next:")), None)
            if intro is not None:
                if not writing and "trace" in intro.lower():
                    raise JourneyFailure("drawing-off lesson still offered a trace step")
                journey.tap(intro)
                time.sleep(0.7)
                journey.screenshot(f"modes-intro-{index}")
                lines.append(f"intro: {intro}")
                continue

            if "Type the reading" in texts or "Pronunciation" in texts:
                seen.add("pronunciation")
                if journey.exists("Show answer", timeout=0.8):
                    journey.tap("Show answer")
                    time.sleep(0.5)
                journey.screenshot(f"modes-pronunciation-{index}")
                journey.tap_visible("Good", max_scrolls=12)
                lines.append("pronunciation: revealed and graded")
            elif "What does it mean?" in texts:
                seen.add("meaning")
                if journey.exists("Show answer", timeout=0.8):
                    journey.tap("Show answer")
                    time.sleep(0.5)
                journey.screenshot(f"modes-meaning-{index}")
                journey.tap_visible("Good", max_scrolls=12)
                lines.append("meaning: revealed and graded")
            elif journey.exists("Check & grade", timeout=0.8):
                if not writing:
                    raise JourneyFailure("a writing canvas appeared while drawing is off")
                seen.add("writing")
                _write_card(journey, state, full_guide=True)
                lines.append("writing: drawn, checked, graded")
                journey.screenshot(f"modes-writing-{index}")
            else:
                if journey.exists("Good", timeout=0.8):
                    journey.tap_visible("Good", max_scrolls=12)
                    lines.append("reveal: graded")
                else:
                    raise JourneyFailure(f"unrecognised review screen: {texts}")

            if _tap_reason_or_skip(journey, choose=reasons):
                lines.append("reason: chip row handled")
                journey.screenshot(f"modes-reason-{index}")
            time.sleep(0.35)

    if not writing and "writing" in seen:
        raise JourneyFailure("drawing is off but a writing review appeared")
    if writing and "writing" not in seen:
        raise JourneyFailure("drawing is on but no writing review appeared")
    if pronunciation and "pronunciation" not in seen:
        raise JourneyFailure("pronunciation is on but no pronunciation review appeared")
    if meaning and "meaning" not in seen:
        raise JourneyFailure("meaning is on but no meaning review appeared")
    journey.artifact_dir.joinpath("study-modes.txt").write_text("\n".join(lines))
    with journey.step("leave the session"):
        if journey.exists("Session complete", timeout=0.5):
            journey.tap("Back to home")
            journey.assert_home()
        else:
            journey.leave_session("End session")
    journey.assert_no_crash()


def grade_with_reason(journey: Journey, state: ScenarioState) -> None:
    """Again/Hard offers one optional reason chip, and the session continues."""
    with journey.step("grade and open the reason row"):
        journey.assert_home()
        journey.start_session()
        journey.wait("Show answer", timeout=10)
        journey.tap("Show answer")
        time.sleep(0.5)
        if not journey.exists("Again", timeout=6):
            raise JourneyFailure("grade choices did not appear")
        journey.tap_visible("Again", max_scrolls=10)
        journey.wait("Add note for the last review", timeout=6)
        journey.screenshot("reason-note")
        journey.tap("Add note for the last review")
        journey.wait("hard?", timeout=6, exact=False)
        journey.screenshot("reason-sheet")
        journey.tap("Stroke order")
        time.sleep(0.8)
        if journey.exists("Add note for the last review", timeout=0.5):
            raise JourneyFailure("the reason note stayed after a diagnosis")
        journey.screenshot("reason-recorded")
    with journey.step("a second failure can skip the reason"):
        if not journey.exists("Show answer", timeout=6):
            raise JourneyFailure("the failed card did not return")
        journey.tap("Show answer")
        time.sleep(0.4)
        journey.tap_visible("Hard", max_scrolls=10)
        if journey.exists("Add note for the last review", timeout=2):
            journey.tap("Add note for the last review")
            journey.wait("hard?", timeout=6, exact=False)
            journey.tap("Not now")
            journey.screenshot("reason-skipped")
    with journey.step("leave"):
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
        journey.tap("Off. Nothing is scheduled", exact=False)
        journey.wait("On at", timeout=12, exact=False)
        # The minute control is real, not a fixed :00.
        journey.tap("00")
        journey.wait("05", timeout=4)
        journey.tap("30")
        journey.wait("On at 20:30", timeout=6, exact=False)
        journey.wait("Send a test notification", timeout=8)
        journey.screenshot("reminders-on")
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
        journey.wait("Off. Nothing is scheduled", timeout=10, exact=False)
        if journey.exists("On at", timeout=0.5, exact=False):
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
        journey.wait("Save to Files", timeout=6)
        journey.screenshot("worksheet-export-choice")
        journey.tap("Save to Files")
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
        # Paper always uses tone marks. Rendering the PDF back to text catches
        # a regression in the builder or the display setting.
        try:
            import pypdfium2
            document = pypdfium2.PdfDocument(str(local))
            text = "\n".join(
                document[page].get_textpage().get_text_bounded()
                for page in range(len(document))
            )
            for marked in ("yī", "èr", "sān", "shí", "rén", "dà"):
                if marked not in text:
                    raise JourneyFailure(f"worksheet is missing tone-mark reading {marked}")
            for numbered in ("yi1", "er4", "san1", "shi2", "ren2", "da4"):
                if numbered in text:
                    raise JourneyFailure(f"worksheet printed numbered pinyin: {numbered}")
        except ImportError:
            pass
        # Visual golden for the printed page itself, including tone marks.
        try:
            import pypdfium2
            document = pypdfium2.PdfDocument(str(local))
            page = document[1]
            page.render(scale=3.0).to_pil().save(journey.artifact_dir / "worksheet-page-2.png")
            journey.compare_image_file("worksheet-page-2", journey.artifact_dir / "worksheet-page-2.png")
        except ImportError:
            pass
        journey.screenshot("worksheet-saved")
    with journey.step("close"):
        journey.d.press("back")
        time.sleep(0.4)
    journey.assert_no_crash()


def _tap_first_character_tile(journey: Journey) -> None:
    """Tap a real character tile by its a11y label, not a guessed coordinate."""
    import re as _re
    width, height = journey.d.window_size()
    for attempt in range(6):
        for value, box, clickable in journey._hierarchy_nodes():
            if not clickable or value.count(",") < 2:
                continue
            if not _re.search(r"[\u3400-\u9fff\u3040-\u30ff\uac00-\ud7af]", value):
                continue
            x1, y1, x2, y2 = box
            if x2 - x1 < 20 or y2 - y1 < 20:
                continue
            # A lazy grid can expose a tile that is under the floating bottom
            # bar. Scrolling it into the content area avoids a click on the bar.
            if y1 < 0 or y2 > height * 0.84:
                continue
            journey.d.click((x1 + x2) // 2, (y1 + y2) // 2)
            time.sleep(0.4)
            return
        # The grid is the left pane on fold/tablet two-pane layouts.
        journey.d.swipe(
            int(width * 0.3),
            int(height * 0.72),
            int(width * 0.3),
            int(height * 0.40),
            0.25,
        )
        time.sleep(0.35)
    raise JourneyFailure("no character tile found in the grid")


def _tap_detail_action(journey: Journey, label: str) -> None:
    """Scroll the detail pane, which can be the right-hand pane in landscape."""
    width, height = journey.d.window_size()
    x = int(width * 0.78)
    # Return the detail pane to its top, then step down slowly. A previous
    # action can leave the pane at the bottom, and a short button can hide
    # between two larger swipes.
    for _ in range(6):
        journey.d.swipe(x, int(height * 0.34), x, int(height * 0.74), 0.3)
        time.sleep(0.1)
    for _ in range(30):
        selector = journey.find(label, timeout=0.35)
        if selector is not None:
            selector.click()
            time.sleep(0.25)
            return
        journey.d.swipe(x, int(height * 0.58), x, int(height * 0.50), 0.6)
        time.sleep(0.12)
    raise JourneyFailure(f"could not reach detail action: {label}")


def screen_pass(journey: Journey, state: ScenarioState) -> None:
    """Screenshot every main surface and dialog in the current script.

    Run it across phone, phone-landscape, fold-inner, fold-outer, and tablet,
    plus the four mode variants. The saved images are the visual review set.
    """
    with journey.step("home and tabs"):
        # Start from a clean app task so a previous scenario cannot leave a
        # quest gate or dialog under the tabs.
        journey.launch()
        journey.assert_home()
        # Let the one-time reader finish so its floating notice cannot swallow
        # uiautomator's hierarchy while this pass moves between screens.
        deadline = time.time() + 120
        while time.time() < deadline:
            ready = journey.exists("Start studying", timeout=1) or journey.exists("Learn ", timeout=1)
            notice = journey.exists("Preparing the ink reader", timeout=0, exact=False)
            if ready and not notice:
                break
            time.sleep(1)
        time.sleep(0.4)
        journey.screenshot("screen-home")
        for tab in ("Characters", "Progress", "Practice", "Settings"):
            journey.open_tab(tab)
            time.sleep(1.2)
            journey.screenshot(f"screen-{tab.lower()}")
    with journey.step("settings sections"):
        for section in ("Practice", "Study", "Reminders", "Schedule", "Writing", "Display", "Data", "Sync"):
            _settings_section(journey, section)
            time.sleep(0.8)
            journey.screenshot(f"screen-settings-{section.lower()}")
            journey.d.press("back")
            time.sleep(0.5)
    with journey.step("settings dialogs"):
        journey.open_tab("Settings")
        journey.tap_visible("About", max_scrolls=14)
        journey.wait("About", timeout=6, exact=False)
        journey.tap_visible("Licences and sources", max_scrolls=10)
        journey.wait("Licences and sources", timeout=6)
        journey.screenshot("screen-licences")
        journey.tap("Full texts")
        journey.wait("Full licence texts", timeout=6)
        journey.screenshot("screen-full-notices")
        journey.tap("Close")
        journey.tap("Close")
        # Close any remaining licence layer, then leave About explicitly. The
        # About page is a subpage, not a dialog; a single back is not enough.
        for _ in range(3):
            if journey.exists("Make Bihua yours", timeout=0.6, exact=False):
                break
            if journey.exists("Back to settings", timeout=0.6):
                journey.tap("Back to settings")
            else:
                journey.d.press("back")
            time.sleep(0.4)
        journey.wait("Make Bihua yours", timeout=8, exact=False)
        journey.tap_visible("Writing", max_scrolls=14)
        journey.wait("Brush", timeout=8)
        try:
            journey.tap("Stroke order rules")
        except JourneyFailure:
            journey.tap_visible("Stroke order rules", max_scrolls=6)
        journey.wait("Stroke order rules", timeout=6)
        time.sleep(0.8)
        journey.screenshot("screen-stroke-rules")
        journey.tap("Close")
    with journey.step("characters and tools"):
        journey.open_tab("Characters")
        journey.wait("Character, meaning, or reading", timeout=10, exact=False)
        # The status chips are the visual surface here; the Filters sheet is
        # covered by characters-scroll. Open the detail before the draw-search
        # detour so the grid is still on All and the tile target is stable.
        _tap_first_character_tile(journey)
        journey.wait("of 2 to Known", timeout=8, exact=False)
        journey.screenshot("screen-character-detail")
        width, height = journey.d.window_size()
        if width <= height:
            _tap_detail_action(journey, "Compare")
            time.sleep(0.8)
            journey.screenshot("screen-compare")
            journey.d.press("back")
            time.sleep(0.6)
            _tap_detail_action(journey, "Write on paper")
            time.sleep(0.8)
            journey.screenshot("screen-paper-check")
            journey.d.press("back")
            time.sleep(0.6)
            journey.d.press("back")
        else:
            # The two-pane detail pane in landscape is much taller than the
            # viewport. Its Compare/paper actions are covered by the portrait
            # goldens; this sweep stays about layout and the main tabs.
            journey.d.press("back")
        journey.wait("Character, meaning, or reading", timeout=8, exact=False)
        journey.tap("Dictionary")
        journey.wait("Dictionary", timeout=8, exact=False)
        journey.screenshot("screen-dictionary")
        journey.d.press("back")
        time.sleep(0.6)
        journey.tap("Search by drawing")
        journey.wait("Draw a character", timeout=6, exact=False)
        time.sleep(0.5)
        journey.screenshot("screen-draw-search")
        journey.d.press("back")
        time.sleep(0.5)
    with journey.step("study tools"):
        # A visual sweep should not depend on the previous screen's back stack.
        # A clean task also makes the Study tools step deterministic after the
        # character/dictionary detour.
        journey.launch()
        journey.assert_home()
        for _ in range(6):
            journey.scroll_up()
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        time.sleep(1.0)
        journey.screenshot("screen-study-tools")
        journey.tap("Named study lists")
        journey.wait("Study lists", timeout=8, exact=False)
        time.sleep(0.6)
        journey.screenshot("screen-study-lists")
        journey.d.press("back")
        time.sleep(0.6)
        journey.d.press("back")
        time.sleep(0.6)
    with journey.step("learn reveal"):
        # The study-tools sheet and its subpage can leave the task on another
        # tab. A clean launch is cheap and makes the reveal deterministic.
        journey.launch()
        journey.assert_home()
        journey.scroll_to_top()
        journey.start_session()
        journey.wait("From memory", timeout=10)
        journey.screenshot("screen-recall")
        width, height = journey.d.window_size()
        if width > height:
            _tap_detail_action(journey, "Show answer")
        else:
            journey.tap("Show answer")
        journey.wait("Good", timeout=10)
        journey.screenshot("screen-answer")
        if width > height and not journey.exists("Good", timeout=0.5):
            _tap_detail_action(journey, "Good")
        else:
            journey.tap("Good")
        journey.tap("Close session")
        journey.wait("Leave this session?", timeout=6, exact=False)
        journey.assert_controls_visible(["Resume later", "End session", "Keep studying"])
        journey.screenshot("screen-leave-dialog")
        journey.tap("End session")
        journey.assert_home()
    with journey.step("game surfaces"):
        journey.open_tab("Home")
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        journey.tap("Spell quest")
        journey.wait("The road ahead", timeout=10)
        time.sleep(0.8)
        journey.screenshot("screen-gate")
        journey.tap_any(["Start the road", "Walk on"])
        time.sleep(1.5)
        journey.screenshot("screen-scene")
        if journey.exists("Draw", timeout=3):
            journey.tap("Draw")
            journey.wait("Cast", timeout=6)
            journey.assert_controls_visible(["Undo", "Clear", "Cast", "Go back"])
            journey.screenshot("screen-cast")
        if journey.exists("Grimoire", timeout=2) or journey.exists("Open the grimoire", timeout=0.5):
            journey.tap_any(["Grimoire", "Open the grimoire"])
            journey.wait("Close book", timeout=10)
        time.sleep(0.8)
        journey.screenshot("screen-grimoire")
        if journey.exists("Close book", timeout=1):
            journey.tap("Close book")
        try:
            journey.tap_any(["Leave the road", "Leave"])
        except JourneyFailure:
            journey.d.press("back")
    journey.assert_no_crash()


def monkey(journey: Journey, state: ScenarioState) -> None:
    """A deterministic random walk across the busiest screens.

    This is deliberately not a screenshot test. It checks the invariants that
    survive arbitrary taps, scrolls, tab changes, and form-factor transitions:
    the app stays foreground, the hierarchy stays populated, and every audited
    screen has no clipped/off-screen/overlapping action controls.
    """
    rng = random.Random(20261125)
    tabs = ["Home", "Characters", "Progress", "Practice"]
    safe_targets = {
        "Dictionary", "Filters", "Change", "Guide", "Order", "Draw first",
        "Show answer", "Good", "Easy", "Hard", "Again", "Practice",
        "Write", "Hear the word", "Compare", "Write on paper",
    }
    with journey.step("random walk"):
        for index in range(60):
            choice = rng.random()
            if choice < 0.25:
                journey.open_tab(rng.choice(tabs))
            elif choice < 0.55:
                width, height = journey.d.window_size()
                x = rng.randint(int(width * 0.2), int(width * 0.8))
                up = rng.random() < 0.5
                y1, y2 = (0.72, 0.28) if up else (0.28, 0.72)
                journey.d.swipe(
                    x, int(height * y1), x, int(height * y2),
                    rng.choice([0.08, 0.2, 0.6]),
                )
            else:
                nodes = [
                    (value, box) for value, box, clickable in journey._hierarchy_nodes()
                    if clickable and value in safe_targets and box[2] > box[0] and box[3] > box[1]
                ]
                if nodes:
                    value, (x1, y1, x2, y2) = rng.choice(nodes)
                    journey.d.click((x1 + x2) // 2, (y1 + y2) // 2)
            time.sleep(rng.uniform(0.15, 0.45))
            journey.assert_foreground()
            if index % 10 == 0:
                journey.audit_layout(f"monkey-{index}")
        journey.assert_foreground()
        journey.assert_layout_clean()
    journey.assert_no_crash()


def study_tools_pdf(journey: Journey, state: ScenarioState) -> None:
    """The character PDF is one of the study tools, not only a Settings extra."""
    with journey.step("open the sheet"):
        journey.launch()
        journey.assert_home()
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        if not journey.exists("Character PDF", timeout=2, exact=False):
            raise JourneyFailure("Character PDF is missing from Study tools")
        journey.screenshot("study-tools-pdf-card")
    with journey.step("open the practice sheets"):
        journey.tap_visible("Character PDF", max_scrolls=8)
        journey.wait("Practice sheets", timeout=8, exact=False)
        journey.screenshot("study-tools-practice-sheets")
        if not journey.exists("Export PDF", timeout=2, exact=False):
            raise JourneyFailure("the practice sheet builder has no export action")
        journey.tap("Close")
        time.sleep(0.5)
        journey.open_tab("Home")
        journey.scroll_to_top()
        journey.assert_home()
    journey.assert_no_crash()


def characters_scroll(journey: Journey, state: ScenarioState) -> None:
    """Fling the character grid hard, then open a card from deep in the list.

    The grid is the densest scrolling surface in the app, so this guards both
    fast flings and the tap target after a long scroll.
    """
    with journey.step("open the grid"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Characters")
        journey.wait("Character, meaning, or reading", timeout=10, exact=False)
        journey.screenshot("characters-top")
    with journey.step("fling deep"):
        width, height = journey.d.window_size()
        for _ in range(8):
            journey.device.shell(
                f"input swipe {width // 2} {int(height * 0.78)} "
                f"{width // 2} {int(height * 0.24)} 60"
            )
            time.sleep(0.15)
        time.sleep(1.2)
        journey.screenshot("characters-flung")
        # A fling must never leave the app on an empty or stale hierarchy.
        # Tiles expose their concise a11y label, e.g. "蓝, lan2, blue, New".
        labels = journey.texts()
        tiles = [label for label in labels if label.count(",") >= 2]
        if not any(label.startswith("All ") for label in labels) or len(tiles) < 4:
            raise JourneyFailure(
                f"grid lost its content after flinging ({len(tiles)} tiles): {labels[:20]}"
            )
    with journey.step("switch script resets the scroll"):
        # The mode chips live in the fixed header, so they stay reachable while
        # the grid is deep. A new script starts at its first level, never at the
        # old list index.
        journey.tap("Traditional")
        deadline = time.time() + 10
        while time.time() < deadline:
            if journey.exists("HSK 1", timeout=0.4, exact=False):
                break
            time.sleep(0.2)
        else:
            raise JourneyFailure(
                "switching script did not return the grid to the top: "
                + str(journey.texts()[:20])
            )
        journey.screenshot("characters-switched-top")
        journey.tap("Simplified")
        journey.wait("Character, meaning, or reading", timeout=8, exact=False)
    with journey.step("open a card"):
        _tap_first_character_tile(journey)
        journey.wait("of 2 to Known", timeout=8, exact=False)
        journey.screenshot("characters-detail")
        width, height = journey.d.window_size()
        if width >= 1800:
            found = False
            for _ in range(16):
                if journey.exists("Collapse words", timeout=0.4):
                    found = True
                    break
                journey.d.swipe(
                    int(width * 0.82), int(height * 0.78),
                    int(width * 0.82), int(height * 0.32), 0.2,
                )
                time.sleep(0.12)
            if not found:
                raise JourneyFailure("could not find the words card in the detail pane")
            journey.tap("Collapse words")
            journey.tap("Expand words")
        else:
            journey.tap_visible("Collapse words", max_scrolls=14)
            journey.tap_visible("Expand words", max_scrolls=5)
        journey.screenshot("characters-detail-expanded")
        journey.d.press("back")
        journey.wait("Character, meaning, or reading", timeout=8, exact=False)
    journey.assert_no_crash()


def rotation(journey: Journey, state: ScenarioState) -> None:
    """Turn the device while the book is open; nothing should clip or vanish."""
    with journey.step("home before the turn"):
        journey.open_tab("Home")
        journey.assert_home()
        journey.screenshot("rotation-home-a")
    with journey.step("turn the device"):
        journey.device.shell("settings put system accelerometer_rotation 0")
        journey.device.shell("settings put system user_rotation 1")
        time.sleep(2.0)
        journey.wait("Hello", timeout=10, exact=False)
        journey.screenshot("rotation-home-b")
    with journey.step("the gate and grimoire reflow"):
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        journey.tap("Spell quest")
        journey.wait("The road ahead", timeout=10)
        journey.screenshot("rotation-gate")
        journey.tap_any(["Start the road", "Walk on"])
        time.sleep(1.5)
        journey.tap_any(["Grimoire", "Open the grimoire"])
        journey.wait("Close book", timeout=10)
        journey.screenshot("rotation-grimoire")
        journey.tap("Close book")
        try:
            journey.tap_any(["Leave the road", "Leave"])
        except JourneyFailure:
            journey.d.press("back")
    with journey.step("turn back"):
        journey.device.shell("settings put system user_rotation 0")
        time.sleep(2.0)
        journey.assert_home()
        journey.screenshot("rotation-home-c")
    journey.assert_no_crash()


def _set_rotation(journey: Journey, landscape: bool) -> None:
    journey.device.shell("settings put system accelerometer_rotation 0")
    journey.device.shell(
        "settings put system user_rotation " + ("1" if landscape else "0"),
    )
    time.sleep(2.0)


def rotation_state(journey: Journey, state: ScenarioState) -> None:
    """Rotation must not lose a review, a draft, a section, or a canvas."""
    try:
        with journey.step("review keeps its card and answer"):
            journey.open_tab("Home")
            journey.assert_home()
            journey.start_session()
            journey.wait("From memory", timeout=12)
            width, height = journey.d.window_size()
            journey.d.swipe(
                width // 2,
                int(height * 0.56),
                width // 2 + 120,
                int(height * 0.62),
                0.3,
            )
            time.sleep(0.6)
            journey.screenshot("rotation-review-portrait")
            _set_rotation(journey, landscape=True)
            journey.wait("From memory", timeout=10)
            journey.screenshot("rotation-review-landscape")
            journey.tap_visible("Show answer", max_scrolls=6)
            journey.wait("Good", timeout=8)
            _set_rotation(journey, landscape=False)
            journey.wait("Good", timeout=8)
            journey.screenshot("rotation-review-answer")
            journey.tap("Good")
            journey.tap("Close session")
            journey.wait("Leave this session?", timeout=6, exact=False)
            journey.tap("End session")
            journey.assert_home()
        with journey.step("report draft survives"):
            journey.open_tab("Settings")
            journey.tap_visible("About", max_scrolls=14)
            journey.tap_visible("Report a problem or idea", max_scrolls=10)
            journey.wait("Send a report", timeout=6)
            field = journey.d(className="android.widget.EditText")
            field.click()
            field.send_keys("rotation draft")
            time.sleep(0.4)
            _set_rotation(journey, landscape=True)
            journey.wait("Send a report", timeout=8)
            if not journey.exists_text("rotation draft"):
                raise JourneyFailure("the report draft did not survive rotation")
            journey.screenshot("rotation-report-landscape")
            journey.tap("Cancel")
            _set_rotation(journey, landscape=False)
        with journey.step("settings section survives"):
            journey.d.press("back")
            journey.wait("Make Bihua yours", timeout=8, exact=False)
            journey.tap_visible("Display", max_scrolls=14)
            journey.wait("Display", timeout=8)
            _set_rotation(journey, landscape=True)
            journey.wait("Display", timeout=8)
            journey.screenshot("rotation-settings-landscape")
            _set_rotation(journey, landscape=False)
            journey.d.press("back")
            journey.wait("Make Bihua yours", timeout=8, exact=False)
        with journey.step("practice keeps its selection"):
            journey.open_tab("Practice")
            journey.wait("Practice", timeout=8)
            journey.screenshot("rotation-practice-portrait")
            _set_rotation(journey, landscape=True)
            journey.wait("Practice", timeout=8)
            journey.screenshot("rotation-practice-landscape")
            if not journey.exists("Guide", timeout=4) and not journey.exists("Order", timeout=1):
                raise JourneyFailure("practice controls disappeared after rotation")
            _set_rotation(journey, landscape=False)
    finally:
        _set_rotation(journey, landscape=False)
    journey.assert_no_crash()


def rotation_onboarding(journey: Journey, state: ScenarioState) -> None:
    """The onboarding step and choices survive a turn before setup is finished."""
    try:
        with journey.step("script page survives a turn"):
            journey.wait("Write characters that stay", timeout=15)
            journey.tap("Continue")
            journey.wait("What are you learning?", timeout=8)
            journey.screenshot("rotation-onboarding-portrait")
            _set_rotation(journey, landscape=True)
            journey.wait("What are you learning?", timeout=8)
            journey.screenshot("rotation-onboarding-landscape")
            _set_rotation(journey, landscape=False)
            journey.wait("What are you learning?", timeout=8)
        with journey.step("finish setup"):
            # The welcome already advanced to the script page. Three more
            # Continues reach the recall page, where the action changes.
            for _ in range(3):
                journey.tap_visible("Continue", max_scrolls=4)
                time.sleep(0.5)
            journey.tap_visible("Start writing", max_scrolls=4)
            journey.assert_home()
    finally:
        _set_rotation(journey, landscape=False)
    journey.assert_no_crash()


def model_offline_switch(journey: Journey, state: ScenarioState) -> None:
    """A downloaded script stays usable, but switching to a missing one offline
    must fail at once, show the offline notice, and leave Learn enabled."""
    try:
        with journey.step("open the downloaded script online"):
            journey.device.shell("am force-stop com.bassmd.bihua")
            journey.device.shell("am start -n com.bassmd.bihua/.MainActivity")
            journey.wait("Hello", timeout=15, exact=False)
            journey.device.shell("cmd connectivity airplane-mode enable")
            time.sleep(1.0)
            journey.device.shell("am force-stop com.bassmd.bihua")
            journey.device.shell(
                "am start -n com.bassmd.bihua/.MainActivity " +
                "--es qa_missing_mode KOREAN"
            )
            journey.wait("Hello", timeout=15, exact=False)
        with journey.step("switch offline to the missing script"):
            journey.tap("Korean")
            journey.wait("Korean", timeout=8, exact=False)
            deadline = time.time() + 12
            while time.time() < deadline:
                if journey.exists("Preparing the ink reader…", timeout=0.5):
                    raise JourneyFailure("the offline switch is still preparing")
                if journey.exists("Start studying", timeout=0.5) or journey.exists("Learn", timeout=0.5, exact=False):
                    break
                time.sleep(0.4)
            else:
                raise JourneyFailure(
                    "Learn stayed blocked after the offline language switch: "
                    + str(journey.texts())
                )
            journey.screenshot("offline-switch")
    finally:
        journey.device.shell("cmd connectivity airplane-mode disable")
        journey.device.shell("svc wifi enable")
        journey.device.shell("svc data enable")
        time.sleep(1.0)
    journey.assert_no_crash()


def offline_first_run(journey: Journey, state: ScenarioState) -> None:
    """A clean device with no network can skip setup and start a review."""
    try:
        journey.device.shell("cmd connectivity airplane-mode enable")
        journey.device.shell("svc wifi disable")
        journey.device.shell("svc data disable")
        time.sleep(1.0)
        journey.device.shell("am force-stop com.bassmd.bihua")
        journey.device.shell("am start -n com.bassmd.bihua/.MainActivity")
        with journey.step("welcome without a network"):
            journey.wait("Write characters that stay", timeout=20)
            journey.screenshot("offline-welcome")
            journey.tap("Skip setup")
            journey.assert_home()
        with journey.step("the primary action never waits"):
            journey.screenshot("offline-home")
            journey.start_session()
            deadline = time.time() + 15
            while time.time() < deadline:
                if (
                    journey.exists("Review ", timeout=0.4, exact=False)
                    or journey.exists("Lesson ", timeout=0.4, exact=False)
                    or journey.exists("Check & grade", timeout=0.4, exact=False)
                ):
                    break
                time.sleep(0.4)
            else:
                raise JourneyFailure(
                    "the offline first run never reached a review: "
                    + str(journey.texts())
                )
            journey.screenshot("offline-session")
    finally:
        journey.device.shell("cmd connectivity airplane-mode disable")
        journey.device.shell("svc wifi enable")
        journey.device.shell("svc data enable")
        time.sleep(1.0)
    journey.assert_no_crash()


def model_notice(journey: Journey, state: ScenarioState) -> None:
    """The one-time reader download is visible, follows the learner, and never blocks.

    Compose Popups are a separate window, so uiautomator cannot reliably see the
    pill in a hierarchy dump. The screenshots are the review artifact; the state
    itself is covered by ModelPreparationTest, and the real download by a fresh
    spell-quest boot.
    """
    with journey.step("launch while the reader downloads"):
        journey.device.shell("am force-stop com.bassmd.bihua")
        journey.device.shell(
            "am start -n com.bassmd.bihua/.MainActivity " +
            "--ez qa_no_ink_model true --el qa_download_notice_ms 14000"
        )
        journey.wait("Hello", timeout=15, exact=False)
        # The Home button is greyed with its normal label; the floating notice
        # is the one place that says the reader is being prepared.
        if not journey.exists("Start studying", timeout=5) and not journey.exists("Learn", timeout=2, exact=False):
            raise JourneyFailure(f"Home has no study action: {journey.texts()}")
        # The notice has a short post-splash grace period, so give it a beat
        # before taking the review artifact.
        time.sleep(2.0)
        journey.screenshot("model-notice-home")
    with journey.step("the notice does not block the app"):
        journey.open_tab("Settings")
        journey.wait("Make Bihua yours", timeout=8, exact=False)
        journey.screenshot("model-notice-settings")
        journey.open_tab("Home")
        journey.assert_home()
    with journey.step("it falls back and clears on its own"):
        deadline = time.time() + 18
        while time.time() < deadline:
            if journey.exists("Start studying", timeout=1) or journey.exists("Learn ", timeout=1):
                break
            time.sleep(0.5)
        else:
            raise JourneyFailure(f"study never re-enabled. texts={journey.texts()}")
        time.sleep(2)
        journey.screenshot("model-notice-gone")
        journey.assert_no_crash()


def spell_offline(journey: Journey, state: ScenarioState) -> None:
    """No reading model: the road is still playable through the grimoire."""
    with journey.step("launch without a reading model"):
        journey.device.shell("am force-stop com.bassmd.bihua")
        journey.device.shell(
            "am start -n com.bassmd.bihua/.MainActivity --ez qa_no_ink_model true"
        )
        journey.wait("Hello", timeout=15, exact=False)
        journey.assert_home()
    with journey.step("the gate offers the book"):
        journey.tap_visible("Extra practice · Spell quest", max_scrolls=8)
        journey.wait("Walk with the book", timeout=10)
        if journey.exists("Draw the spell", timeout=1):
            raise JourneyFailure("the gate still offers drawing without a reader")
        journey.tap("About reading ink")
        journey.wait("Reading the ink", timeout=6)
        journey.screenshot("spell-offline-info")
        journey.tap("Back to the road")
    with journey.step("the scene hides draw"):
        journey.tap("Start the road")
        journey.wait("Open the grimoire", timeout=10)
        if journey.exists("Draw", timeout=1, exact=True):
            raise JourneyFailure("the scene still offers drawing without a reader")
        journey.screenshot("spell-offline-scene")
        journey.tap("Open the grimoire")
        journey.wait("Close book", timeout=10)
        journey.screenshot("spell-offline-book")
        journey.tap("Close book")
        journey.tap_visible("Leave the road", max_scrolls=8)
    with journey.step("practice falls back"):
        journey.open_tab("Practice")
        journey.wait("Draw freely", timeout=8, exact=False)
        if not journey.draw_guide([[(0.2, 0.2), (0.6, 0.7)]]):
            raise JourneyFailure("no practice canvas for the offline check")
        journey.tap("Check")
        deadline = time.time() + 8
        texts: list[str] = []
        while time.time() < deadline:
            texts = journey.texts()
            if any("handwriting check is unavailable" in text for text in texts) or "Close" in texts:
                break
            time.sleep(0.2)
        journey.screenshot("practice-offline")
        if "Close" in texts:
            journey.tap("Close")
        journey.wait("Check", timeout=6)
    with journey.step("restore the normal app"):
        journey.device.shell("am force-stop com.bassmd.bihua")
        journey.device.shell("am start -n com.bassmd.bihua/.MainActivity")
        journey.wait("Hello", timeout=12, exact=False)
    journey.assert_no_crash()


def practice_slow(journey: Journey, state: ScenarioState) -> None:
    """The 1.0.9 report: a slow reader must never leave Check spinning forever."""
    with journey.step("open practice with a slow reader"):
        journey.device.shell("am force-stop com.bassmd.bihua")
        journey.device.shell(
            "am start -n com.bassmd.bihua/.MainActivity --el qa_slow_ink_ms 60000"
        )
        journey.wait("Hello", timeout=15, exact=False)
        journey.open_tab("Practice")
        journey.wait("Draw freely", timeout=8, exact=False)
    with journey.step("stop the check and carry on"):
        if not journey.draw_guide([[(0.2, 0.2), (0.6, 0.7)]]):
            raise JourneyFailure("no practice canvas to draw on")
        journey.wait("Check", timeout=5)
        journey.tap("Check")
        # If the reader is ready but slow, Stop/Close must cancel it. If it is
        # not ready, the local feedback path is already showing; either is a
        # valid, non-stuck outcome.
        if journey.exists("Stop reading", timeout=6):
            journey.screenshot("practice-slow-reading")
        journey.tap("Close")
        deadline = time.time() + 5
        while time.time() < deadline:
            if not journey.exists("Stop reading", timeout=0.5):
                break
            time.sleep(0.3)
        else:
            raise JourneyFailure("closing the check did not stop the reader")
        journey.wait("Check", timeout=8)
        journey.screenshot("practice-slow-recovered")
    with journey.step("leave cleanly"):
        journey.open_tab("Home")
        journey.assert_home()
    journey.assert_no_crash()


def practice_check(journey: Journey, state: ScenarioState) -> None:
    """Practice check must never trap the learner on a spinning reading state."""
    with journey.step("open practice"):
        journey.open_tab("Practice")
        journey.wait("Draw freely", timeout=8, exact=False)
    with journey.step("check and recover"):
        if not journey.draw_guide([[(0.2, 0.2), (0.6, 0.7)]]):
            raise JourneyFailure("no practice canvas to draw on")
        journey.wait("Check", timeout=5)
        journey.tap("Check")
        deadline = time.time() + 5
        state_texts: list[str] = []
        while time.time() < deadline:
            state_texts = journey.texts()
            if "Stop reading" in state_texts or "Close" in state_texts:
                break
            time.sleep(0.2)
        journey.screenshot("practice-check")
        if "Close" in state_texts:
            journey.tap("Close")
            time.sleep(0.3)
            if journey.exists("Stop reading", timeout=1):
                journey.tap("Stop reading")
        elif "Stop reading" in state_texts:
            journey.tap("Stop reading")
        else:
            raise JourneyFailure(f"practice check never offered a way out. texts={state_texts}")
        journey.wait("Check", timeout=8)
    with journey.step("leave"):
        journey.open_tab("Home")
        journey.assert_home()
    journey.assert_no_crash()


def brush_presets(journey: Journey, state: ScenarioState) -> None:
    """Each brush tool restores its own saved feel in Settings."""
    with journey.step("open brush settings"):
        _settings_section(journey, "Writing")
        journey.wait("Brush", timeout=8)
    with journey.step("each tool restores its preset"):
        journey.tap("Pen")
        journey.wait("1.8dp", timeout=5, exact=False)
        journey.tap("Ink brush")
        journey.wait("2.4dp", timeout=5, exact=False)
        journey.tap("Pencil brush")
        journey.wait("1.4dp", timeout=5, exact=False)
        journey.tap("Marker")
        journey.wait("5.0dp", timeout=5, exact=False)
        journey.screenshot("brush-presets")
    with journey.step("leave"):
        journey.d.press("back")
        journey.wait("Make Bihua yours", timeout=8, exact=False)
    journey.assert_no_crash()


def writing_reveal(journey: Journey, state: ScenarioState) -> None:
    """A writing review can reveal the answer before any ink, then grade."""
    with journey.step("start writing review"):
        journey.assert_home()
        journey.start_session()
        journey.wait("From memory", timeout=10)
    with journey.step("show the answer without drawing"):
        journey.wait("Show answer", timeout=5)
        journey.tap("Show answer")
        journey.wait("Good", timeout=10)
        journey.screenshot("writing-reveal")
        journey.tap("Good")
    with journey.step("return home"):
        journey.leave_session("End session")
        journey.assert_home()
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
        journey.tap_visible("Show me", max_scrolls=6)
        journey.wait("Watch stroke", timeout=6, exact=False)
        journey.screenshot("stroke-watch")
        journey.tap("Trace it")
        journey.wait("Trace stroke", timeout=6, exact=False)
        journey.screenshot("stroke-trace")
        journey.tap("Close")
        time.sleep(0.4)
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
    # A previous section may have left the list scrolled. Start every section
    # from the same place so screenshots and dialogs are deterministic.
    journey.scroll_to_top()
    # Settings rows use text; the bottom navigation uses content descriptions.
    # "Practice" exists in both, so use the row text explicitly. The hierarchy
    # fallback matters because uiautomator's idle wait can fail after a theme
    # switch even though the row is on screen.
    for _ in range(14):
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


def report_flow(journey: Journey, state: ScenarioState) -> None:
    """The report composer, the saved crash, and the rate link."""
    with journey.step("About without a crash"):
        journey.d.shell("run-as com.bassmd.bihua rm -rf files/crash")
        journey.launch()
        journey.assert_home()
        journey.open_tab("Settings")
        journey.tap_visible("About", max_scrolls=14)
        journey.wait("Made by Shadowsx3", timeout=8, exact=False)
        journey.screenshot("report-about")
        journey.tap_visible("Report a problem or idea", max_scrolls=10)
        journey.wait("Send a report", timeout=6)
        send = journey.find("Send", timeout=2)
        if send is not None and send.info.get("enabled"):
            raise JourneyFailure("Send was enabled with no words and no crash")
        if journey.exists("Include the saved report", timeout=0.5, exact=False):
            raise JourneyFailure("crash checkbox appeared without a crash")
        journey.tap("An idea")
        field = journey.d(className="android.widget.EditText")
        field.click()
        time.sleep(0.2)
        field.send_keys("A week view for the widget.")
        time.sleep(0.3)
        send = journey.find("Send", timeout=3)
        if send is None or not send.info.get("enabled"):
            raise JourneyFailure("Send stayed disabled after writing words")
        journey.screenshot("report-composer")
        journey.tap("Copy")
        journey.wait("Copied. Paste it wherever you like.", timeout=6, exact=False)
    with journey.step("a saved crash can be attached"):
        journey.d.shell("am force-stop com.bassmd.bihua")
        journey.d.shell(
            "am start -n com.bassmd.bihua/.MainActivity --ez qa_crash true",
        )
        time.sleep(2)
        journey.d.shell("am force-stop com.bassmd.bihua")
        journey.d.shell("logcat -c")
        journey.launch()
        journey.open_tab("Settings")
        journey.tap_visible("About", max_scrolls=14)
        named = False
        for _ in range(10):
            if journey.exists("A crash was saved", timeout=0.5, exact=False):
                named = True
                break
            journey.scroll_down()
        if not named:
            raise JourneyFailure("the About row did not name the saved crash")
        journey.screenshot("report-about-crash")
        journey.tap_visible("Report a problem or idea", max_scrolls=10)
        journey.wait("Send a report", timeout=6)
        if not journey.exists("Include the saved report", timeout=2, exact=False):
            raise JourneyFailure("the saved crash was not offered")
        journey.screenshot("report-with-crash")
        journey.tap("See what will be sent")
        journey.wait("What will be sent", timeout=6)
        journey.screenshot("report-preview")
        journey.tap("Close")
        time.sleep(0.4)
        journey.tap("Send")
        time.sleep(1.2)
        if journey.d.app_current().get("package") == "com.bassmd.bihua":
            raise JourneyFailure("Send did not leave the app")
        journey.d.press("back")
        time.sleep(0.8)
    with journey.step("the rate row leaves the app"):
        journey.open_tab("Settings")
        # Send leaves the app on the About page, so the row may already be
        # visible and the About title above the viewport.
        if not journey.exists("Rate Bihua", timeout=1.5):
            journey.tap_visible("About", max_scrolls=14)
        journey.wait("Rate Bihua", timeout=8)
        journey.tap_visible("Rate Bihua", max_scrolls=10)
        time.sleep(1.2)
        if journey.d.app_current().get("package") == "com.bassmd.bihua":
            raise JourneyFailure("Rate did not leave the app")
    journey.assert_no_crash()


def settings_pages(journey: Journey, state: ScenarioState) -> None:
    """Visual pass over every Settings section, with a stable screenshot each."""
    with journey.step("settings home"):
        journey.open_tab("Settings")
        journey.wait("Make Bihua yours", timeout=8, exact=False)
        journey.screenshot("settings-home")
    for label, marker in SETTINGS_MARKERS:
        slug = label.lower().replace(" ", "-").replace("&", "and")
        with journey.step(f"settings {slug}"):
            _settings_section(journey, label)
            journey.wait(marker, timeout=8, exact=False)
            journey.screenshot(f"settings-{slug}")
            journey.d.press("back")
            journey.wait("Make Bihua yours", timeout=8, exact=False)
    journey.assert_no_crash()


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
        open_study_tools(journey)
        journey.wait("Pinned", timeout=8)
        journey.d.press("back")
        for _ in range(3):
            journey.scroll_up()
        journey.assert_home()
    journey.assert_no_crash()


def reveal_review(journey: Journey, state: ScenarioState) -> None:
    """Reveal-only reviews never show the canvas or the reading field."""
    with journey.step("settings explain the option"):
        _settings_section(journey, "Practice")
        if journey.ensure_visible("Reveal review") is None:
            raise JourneyFailure("Reveal review settings card missing")
        journey.assert_text("Writing: tap to reveal")
        journey.assert_text("Pronunciation: tap to reveal")
        journey.screenshot("reveal-settings")
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
                raise JourneyFailure("Reveal writing showed the canvas action")
            if journey.exists("Type the reading", timeout=0.2):
                raise JourneyFailure("Reveal pronunciation showed the typing field")
            if journey.exists("Next: trace", timeout=0.2):
                raise JourneyFailure("Reveal writing still asked for a trace")
            if journey.exists("Next: show answer", timeout=0.2):
                journey.tap("Next: show answer")
            if journey.exists("Show answer", timeout=3):
                journey.tap("Show answer")
                journey.screenshot(f"reveal-answer-{graded}")
            journey.tap_any(["Good", "Easy", "Hard", "Again"], timeout=6)
            graded += 1
            time.sleep(0.4)
        if graded == 0:
            raise JourneyFailure("no Reveal review was graded")
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


def meaning_reveal(journey: Journey, state: ScenarioState) -> None:
    """Reveal meaning has no typing field and still offers synonyms after reveal."""
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
                    raise JourneyFailure("Reveal meaning showed a typing field")
                journey.tap("Show answer")
                journey.wait("Add a synonym", timeout=6)
                journey.screenshot("meaning-reveal-answer")
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
        ("A quiet spell road", "never changes your reviews"),
        ("Look anything up", "strokes and words"),
        ("Practice without pressure", "exactly where they are"),
        ("The four buttons", "Easy: instant"),
        ("No pen today?", "Off by default"),
        ("Paper and lists", "Settings → Study"),
        ("Phone and tablet, together", "choose Merge"),
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
        journey.tap_visible("Change", max_scrolls=8)
        journey.wait("Mò's outfit", timeout=6)
        issues = journey.a11y_unlabeled()
        if issues:
            raise JourneyFailure(f"outfit dialog has unlabeled clickables: {issues[:3]}")
        journey.screenshot("widget-outfit-dialog")
        journey.tap_visible("Witch hat outfit", max_scrolls=8)
        time.sleep(0.4)
        if not journey.exists("Use this outfit", timeout=2):
            raise JourneyFailure("outfit preview did not offer a confirm action")
        journey.screenshot("widget-outfit-picked")
        journey.tap("Use this outfit")
        time.sleep(0.5)
        if journey.exists("Mò's outfit", timeout=0.5):
            raise JourneyFailure("outfit dialog did not close")
        if not journey.exists_text("Witch hat"):
            raise JourneyFailure("outfit did not persist")
        journey.screenshot("widget-outfit-pinned")
        journey.tap_visible("Change", max_scrolls=8)
        journey.wait("Mò's outfit", timeout=6)
        journey.tap_visible("Auto outfit", max_scrolls=8)
        time.sleep(0.3)
        journey.tap("Use this outfit")
        time.sleep(0.5)
        if not journey.exists_text("Auto"):
            raise JourneyFailure("Auto outfit did not persist")
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


def settings_writing_model(journey: Journey, state: ScenarioState) -> None:
    """The handwriting model section shows status and can cancel a slow batch."""
    with journey.step("open the handwriting model section"):
        journey.device.shell("am force-stop com.bassmd.bihua")
        journey.device.shell(
            "am start -n com.bassmd.bihua/.MainActivity --el qa_slow_ink_ms 3000"
        )
        journey.wait("Hello", timeout=15, exact=False)
        journey.assert_home()
        _settings_section(journey, "Writing")
        button = journey.ensure_visible("Download all", max_scrolls=16)
        if button is None:
            button = journey.ensure_visible("Download again", max_scrolls=3)
        if button is None:
            raise JourneyFailure("the handwriting model actions are missing")
        journey.screenshot("settings-writing-models")
    with journey.step("a batch download can be stopped"):
        button.click()
        journey.wait("Cancel", timeout=4)
        journey.screenshot("settings-writing-downloading")
        journey.tap_visible("Cancel", max_scrolls=4)
        journey.wait("Model download stopped.", timeout=5, exact=False)
        journey.screenshot("settings-writing-cancelled")
    with journey.step("leave"):
        journey.d.press("back")
        journey.wait("Make Bihua yours", timeout=8, exact=False)
    journey.assert_no_crash()


def known_list_preview(journey: Journey, state: ScenarioState) -> None:
    """A known-list import previews the count and rejects prose before writing."""
    with journey.step("open a known list"):
        local = journey.artifact_dir / "bihua-known.txt"
        local.write_text("爱\n你好\n今天天气很好\n", encoding="utf-8")
        journey.device.shell("mkdir -p /sdcard/Download", check=False)
        subprocess.run(
            [
                "adb", "-s", journey.device.serial, "push",
                str(local), "/sdcard/Download/bihua-known.txt",
            ],
            capture_output=True,
            check=False,
        )
        journey.launch()
        journey.assert_home()
        _settings_section(journey, "Data")
        journey.tap_visible("Import a known list (.txt)", max_scrolls=24)
        time.sleep(1.5)
        if journey.exists("Downloads", timeout=2):
            journey.tap("Downloads")
        journey.tap_visible("bihua-known.txt", max_scrolls=12)
        journey.wait("Mark characters as known?", timeout=10)
        journey.screenshot("known-list-preview")
        journey.assert_text("This file marks 3 characters as known.")
        if not journey.exists_text("Skipped 1 line", exact=False):
            raise JourneyFailure("the preview did not report the skipped prose line")
    with journey.step("confirm the import"):
        journey.tap("Mark as known")
        journey.wait("Marked 3 characters as known", timeout=8, exact=False)
        journey.screenshot("known-list-applied")
    with journey.step("leave"):
        journey.d.press("back")
        journey.wait("Make Bihua yours", timeout=8, exact=False)
    journey.assert_no_crash()


def settings_writing_mode(journey: Journey, state: ScenarioState) -> None:
    """The drawing switch and the checks keep at least one mode usable."""
    with journey.step("open Practice modes"):
        journey.launch()
        journey.assert_home()
        _settings_section(journey, "Practice")
        journey.ensure_visible("Writing", max_scrolls=8)
        journey.screenshot("writing-mode-on")

    with journey.step("drawing cannot be the only mode that is off"):
        journey.tap("Writing")
        journey.wait("Turn on Pronunciation or Meaning", timeout=4, exact=False)
        journey.screenshot("writing-mode-blocked")

    with journey.step("a check unlocks recognition-only"):
        journey.tap("Pronunciation")
        time.sleep(0.4)
        journey.tap("Writing")
        time.sleep(0.5)
        if not journey.exists_text(
            "Drawing is off. Reviews ask only for pronunciation.",
            exact=False,
        ):
            raise JourneyFailure("the drawing-off status did not name pronunciation")
        journey.screenshot("writing-mode-off")

    with journey.step("the last check cannot be turned off"):
        journey.tap("Pronunciation")
        journey.wait("Keep one practice mode on", timeout=4, exact=False)

    with journey.step("meaning can carry the session"):
        journey.tap("Meaning")
        time.sleep(0.4)
        journey.tap("Pronunciation")
        time.sleep(0.5)
        if not journey.exists_text(
            "Drawing is off. Reviews ask only for meaning.",
            exact=False,
        ):
            raise JourneyFailure("the drawing-off status did not name meaning")
        journey.screenshot("writing-mode-meaning")

    with journey.step("drawing comes back"):
        journey.tap("Writing")
        time.sleep(0.4)
        journey.d.press("back")
        journey.wait("Make Bihua yours", timeout=8, exact=False)
    journey.assert_no_crash()


def progress_screen(journey: Journey, state: ScenarioState) -> None:
    """The Progress tab as a real report: stats, charts, and every range."""
    with journey.step("open the report"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Progress")
        journey.wait("Progress", timeout=8)
        journey.scroll_to_top()
        for label in ("Reviews", "Learned", "Practice", "Streak"):
            if journey.value_above(label) in (None, ""):
                raise JourneyFailure(f"progress stat has no value: {label}")
        journey.screenshot("progress-top")
        wanted = {
            "Momentum",
            "Level progress",
            "Practice days",
            "Review quality",
            "Upcoming reviews",
            "Memory",
        }
        seen: set[str] = set()
        for _ in range(18):
            for label in wanted:
                if label not in seen and journey.exists_text(label, timeout=0.4):
                    seen.add(label)
            if seen == wanted:
                break
            journey.scroll_down()
            time.sleep(0.25)
        missing = wanted - seen
        if missing:
            raise JourneyFailure(f"progress card missing: {', '.join(sorted(missing))}")
    with journey.step("every range renders"):
        journey.scroll_to_top()
        for short, label in (
            ("7D", "Last 7 days"),
            ("30D", "Last 30 days"),
            ("90D", "Last 90 days"),
            ("1Y", "Last year"),
            ("All", "All time"),
        ):
            journey.tap_text(short)
            time.sleep(0.9)
            journey.assert_text(label)
            journey.assert_text("Momentum")
            journey.screenshot(f"progress-{short.lower()}")
        journey.tap_text("30D")
        time.sleep(0.6)
    with journey.step("scroll the report"):
        journey.scroll_to_top()
        # The lower cards do not fit one viewport; collect them (and a shot of
        # each chart) as they pass.
        wanted = ("Practice days", "Daily reviews", "Recently learned")
        shots = {
            "Daily reviews": "progress-daily",
            "Upcoming reviews": "progress-forecast",
            "characters learned in total": "progress-growth",
            "Recently learned": "progress-bottom",
        }
        seen: set[str] = set()
        shot_seen: set[str] = set()
        for _ in range(26):
            for label in wanted:
                if label not in seen and journey.exists_text(label, exact=False):
                    seen.add(label)
            for text, shot in shots.items():
                if shot not in shot_seen and journey.exists_text(text, exact=False):
                    shot_seen.add(shot)
                    journey.screenshot(shot)
            if len(seen) == len(wanted) and len(shot_seen) == len(shots):
                break
            journey.scroll_down()
            time.sleep(0.15)
        if "progress-bottom" not in shot_seen:
            journey.screenshot("progress-bottom")
        missing = [label for label in wanted if label not in seen]
        if missing:
            raise JourneyFailure(f"progress lower card missing: {', '.join(missing)}")
    journey.assert_no_crash()


def progress_all_time(journey: Journey, state: ScenarioState) -> None:
    """All time keeps work that is older than the one-year window."""
    with journey.step("all time outgrows the year"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Progress")
        journey.wait("Progress", timeout=8)
        journey.tap_text("1Y")
        time.sleep(0.9)
        journey.assert_text("Last year")
        year = journey.value_above("Reviews")
        journey.tap_text("All")
        time.sleep(0.7)
        journey.assert_text("All time")
        everything = None
        for _ in range(12):
            everything = journey.value_above("Reviews")
            if year and everything and int(everything) > int(year):
                break
            time.sleep(0.4)
        if not year or not everything:
            raise JourneyFailure("range review totals are missing")
        if int(everything) <= int(year):
            raise JourneyFailure(
                f"all time ({everything}) did not outgrow the year ({year})"
            )
        journey.assert_text("across everything you have done")
        journey.assert_text("study days with ink")
        journey.screenshot("progress-all-time")
        # The all-time charts bucket years of history; make sure they render.
        for _ in range(10):
            journey.scroll_down()
            time.sleep(0.15)
        journey.screenshot("progress-all-time-bottom")
    journey.assert_no_crash()


def progress_ranges(journey: Journey, state: ScenarioState) -> None:
    """Progress stats follow the selected window."""
    with journey.step("switch the range"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Progress")
        journey.wait("Progress", timeout=8)
        streak = journey.value_above("Streak")
        if not streak:
            raise JourneyFailure("streak tile value missing")
        for short, label in (
            ("7D", "Last 7 days"),
            ("90D", "Last 90 days"),
            ("1Y", "Last year"),
            ("All", "All time"),
        ):
            journey.tap_text(short)
            time.sleep(0.8)
            journey.assert_text(label)
            journey.assert_text("Momentum")
            if not journey.exists_text("Streak"):
                raise JourneyFailure(f"streak tile missing for {label}")
            if journey.value_above("Streak") != streak:
                raise JourneyFailure(f"streak was capped by {label}")
        journey.tap_text("All")
        time.sleep(0.8)
        journey.assert_text("All time")
        journey.screenshot("progress-all")
        journey.tap_text("30D")
        time.sleep(0.8)
        journey.assert_text("Last 30 days")
        journey.screenshot("progress-ranges")
    with journey.step("the recent strips follow the range"):
        journey.tap_text("7D")
        time.sleep(0.8)
        for _ in range(16):
            if journey.exists_text("Recently learned"):
                break
            journey.scroll_down()
            time.sleep(0.15)
        else:
            raise JourneyFailure("Recently learned was not reachable")
        journey.assert_text("Last 7 days")
        journey.screenshot("progress-range-strips")


def progress_pdf_save(journey: Journey, state: ScenarioState) -> None:
    """Save the progress PDF through the real system file picker."""
    with journey.step("open progress and tap PDF"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Progress")
        journey.wait("Level progress", timeout=10)
        journey.tap_visible("PDF", max_scrolls=24)
        journey.wait("Save to Files", timeout=8)
        journey.screenshot("progress-pdf-choice")
        journey.tap("Save to Files")
        try:
            journey.tap_any(["SAVE", "Save"], timeout=8)
        except Exception:
            raise JourneyFailure("the system file picker did not offer Save")
        journey.screenshot("progress-pdf-saved")
        time.sleep(1.2)
    with journey.step("find the saved report"):
        listing = journey.device.shell(
            "ls -t /sdcard/Download/bihua-progress-*.pdf 2>/dev/null | head -1",
            check=False,
        )
        if not listing.strip():
            raise JourneyFailure("no saved progress PDF was found in Downloads")
        remote = listing.strip()
        local = journey.artifact_dir / "saved-progress-report.pdf"
        subprocess.run(
            ["adb", "-s", journey.device.serial, "pull", remote, str(local)],
            capture_output=True,
            check=False,
        )
        if not local.exists() or local.stat().st_size < 12_000:
            raise JourneyFailure("the saved progress PDF is missing or too small")
    journey.assert_no_crash()


def progress_pdf_share(journey: Journey, state: ScenarioState) -> None:
    """Share the progress PDF directly and keep a complete cached copy."""
    with journey.step("choose share"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Progress")
        journey.wait("Level progress", timeout=10)
        journey.tap_visible("PDF", max_scrolls=24)
        journey.wait("Share PDF", timeout=8)
        journey.screenshot("progress-pdf-choice")
        journey.tap("Share PDF")
        time.sleep(1.5)
        journey.screenshot("progress-pdf-share-sheet")
    with journey.step("the shared PDF is complete"):
        listing = journey.device.shell(
            "run-as com.bassmd.bihua sh -c 'ls -t cache/pdf/*.pdf 2>/dev/null | head -1'",
            check=False,
        ).strip()
        if not listing:
            raise JourneyFailure("no cached progress PDF was prepared for sharing")
        name = listing.split()[-1].split("/")[-1]
        local = journey.artifact_dir / "shared-progress-report.pdf"
        with local.open("wb") as handle:
            subprocess.run(
                [
                    "adb", "-s", journey.device.serial, "exec-out",
                    "run-as", "com.bassmd.bihua", "cat", f"cache/pdf/{name}",
                ],
                stdout=handle,
                check=False,
            )
        if not local.exists() or local.stat().st_size < 12_000:
            raise JourneyFailure("the shared progress PDF is missing or too small")
        if b"%%EOF" not in local.read_bytes()[-2048:]:
            raise JourneyFailure("the shared progress PDF is incomplete")
    with journey.step("leave"):
        journey.d.press("back")
        time.sleep(0.8)
    journey.assert_no_crash()


def progress_pdf_rich(journey: Journey, state: ScenarioState) -> None:
    """Generate the progress PDF with a full range and pull the files for review."""
    remote = "/sdcard/Android/data/com.bassmd.bihua/files"
    with journey.step("open progress and write the default range"):
        journey.device.shell(
            f"rm -f {remote}/bihua-progress-report*.pdf",
            check=False,
        )
        journey.device.shell("am force-stop com.bassmd.bihua")
        journey.device.shell(
            "am start -n com.bassmd.bihua/.MainActivity --ez qa_report true"
        )
        journey.wait("Hello", timeout=15, exact=False)
        journey.assert_home()
        journey.open_tab("Progress")
        journey.wait("Level progress", timeout=10)
        time.sleep(3.0)
        journey.screenshot("progress-pdf-default")
    with journey.step("year and all time"):
        journey.tap("1Y")
        time.sleep(3.0)
        journey.screenshot("progress-pdf-year")
        journey.tap("All")
        time.sleep(3.0)
        journey.screenshot("progress-pdf-all")
    with journey.step("pull the reports"):
        listing = journey.device.shell(
            f"find {remote} -maxdepth 1 -name 'bihua-progress-report-*.pdf' 2>/dev/null",
            check=False,
        )
        names = [
            line.strip().split("/")[-1]
            for line in listing.splitlines()
            if line.strip().endswith(".pdf")
        ]
        if len(names) < 2:
            raise JourneyFailure(f"expected at least two progress PDFs, got {names}")
        for name in names:
            local = journey.artifact_dir / name
            subprocess.run(
                ["adb", "-s", journey.device.serial, "pull", f"{remote}/{name}", str(local)],
                capture_output=True,
                check=False,
            )
            if not local.exists() or local.stat().st_size < 12_000:
                raise JourneyFailure(f"the progress PDF is missing or too small: {name}")
    journey.assert_no_crash()


def progress_custom_single_day(journey: Journey, state: ScenarioState) -> None:
    """A custom window can be one day, and the UI says so."""
    with journey.step("pick one day"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Progress")
        journey.wait("Progress", timeout=8)
        journey.tap("Custom range")
        journey.wait("Choose a range", timeout=8)
        journey.tap("Yesterday")
        time.sleep(0.5)
        journey.tap("Apply range")
        time.sleep(1.0)
        journey.assert_text("Momentum")
        if not journey.exists_text("1 day", exact=False):
            raise JourneyFailure("a single-day range did not say 1 day")
        journey.screenshot("progress-single-day")
    with journey.step("leave"):
        journey.open_tab("Home")
        journey.assert_home()
    journey.assert_no_crash()


def progress_scripts(journey: Journey, state: ScenarioState) -> None:
    """Level progress names the real script levels, not another exam."""
    with journey.step("Chinese levels"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Progress")
        journey.wait("Level progress", timeout=8)
        journey.assert_text("HSK 1")
        journey.screenshot("progress-levels-zh")
    with journey.step("Japanese levels are Jōyō"):
        journey.tap("Japanese")
        time.sleep(1.2)
        journey.scroll_to_top()
        if journey.ensure_visible("JŌYŌ 1", max_scrolls=10) is None:
            raise JourneyFailure("Japanese level progress did not show JŌYŌ 1")
        if journey.exists("JLPT", timeout=0.5):
            raise JourneyFailure("the Japanese level card claims JLPT")
        journey.screenshot("progress-levels-ja")
    with journey.step("Korean levels are Hangul and Hanja"):
        journey.tap("Korean")
        time.sleep(1.2)
        journey.scroll_to_top()
        if journey.ensure_visible("HANJA", max_scrolls=10) is None:
            raise JourneyFailure("Korean level progress did not show HANJA")
        if journey.exists("TOPIK", timeout=0.5):
            raise JourneyFailure("the Korean level card claims TOPIK")
        journey.screenshot("progress-levels-ko")
    with journey.step("back to Simplified"):
        journey.tap("Simplified")
        time.sleep(1.0)
        journey.open_tab("Home")
        journey.assert_home()
    journey.assert_no_crash()


def script_switch_loading(journey: Journey, state: ScenarioState) -> None:
    """A script switch never shows the previous course while the new one loads."""
    with journey.step("switch with a slow course load"):
        journey.device.shell("am force-stop com.bassmd.bihua")
        journey.device.shell(
            "am start -n com.bassmd.bihua/.MainActivity --el qa_content_delay_ms 4000"
        )
        journey.wait("Hello", timeout=15, exact=False)
        journey.assert_home()
        journey.tap("Japanese")
        # The course query is held for four seconds. Opening Characters now must
        # show the loading state, never a leftover Chinese character.
        journey.open_tab("Characters")
        journey.wait("Loading characters…", timeout=3)
        if journey.exists("爱", timeout=0.5):
            raise JourneyFailure("the previous script stayed visible during a script switch")
        journey.screenshot("script-switch-loading")
    with journey.step("the new course arrives and stays"):
        deadline = time.time() + 12
        while time.time() < deadline:
            if not journey.exists("Loading characters…", timeout=0.5):
                break
            time.sleep(0.3)
        else:
            raise JourneyFailure("the Japanese course never finished loading")
        if journey.exists("爱", timeout=0.5):
            raise JourneyFailure("a Simplified character remained after the switch")
        journey.screenshot("script-switch-loaded")
    with journey.step("back to Simplified"):
        journey.tap("Simplified")
        time.sleep(1.2)
        journey.open_tab("Home")
        journey.assert_home()
    journey.assert_no_crash()


def progress_custom_range(journey: Journey, state: ScenarioState) -> None:
    """Pick a real start and end day and make every Progress card follow it."""
    with journey.step("open a custom range"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Progress")
        journey.wait("Progress", timeout=8)
        journey.tap("Custom range")
        journey.wait("Choose a range", timeout=8)
        journey.tap("Last week")
        time.sleep(0.6)
        journey.screenshot("progress-custom-sheet")
        journey.tap("Apply range")
        time.sleep(1.0)
        journey.assert_text("Momentum")
        labels = sorted(
            {
                text.split(" · ")[0]
                for text in journey.texts()
                if "–" in text and not text.startswith("Custom range")
            },
            key=len,
        )
        if not labels:
            raise JourneyFailure("the custom chip did not show a date range")
        journey.screenshot("progress-custom")
    with journey.step("the recent strips follow the custom range"):
        for _ in range(18):
            if journey.exists_text("Recently learned"):
                break
            journey.scroll_down()
            time.sleep(0.15)
        else:
            raise JourneyFailure("Recently learned was not reachable")
        journey.assert_text(labels[0])
        journey.screenshot("progress-custom-strips")
    journey.assert_no_crash()


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


def reveal_wide_home_card(journey: Journey, label: str, tries: int = 12) -> None:
    """Reveal an item in the wide Home's right column.

    The tablet and landscape layouts scroll each page on its own, so a centre
    swipe never reaches the Study tools card below the calendar.
    """
    width, height = journey.d.window_size()
    if width <= height:
        return
    x = int(width * 0.78)
    for _ in range(tries):
        if journey.exists(label, timeout=0.4):
            return
        journey.d.swipe(x, int(height * 0.68), x, int(height * 0.38), 0.2)
        time.sleep(0.2)


def spell_entry(journey: Journey, state: ScenarioState) -> None:
    """The spell quest is one tap from Home, and also in Study tools, in every script."""
    with journey.step("open from the top extra-practice link"):
        journey.open_tab("Home")
        journey.assert_home()
        time.sleep(1.2)
        journey.tap_visible("Extra practice · Spell quest", max_scrolls=8)
        journey.wait("The road ahead", timeout=10)
        journey.screenshot("spell-entry-top")
        journey.tap_visible("Leave", max_scrolls=4)
    with journey.step("open from the Home study tools card"):
        journey.open_tab("Home")
        journey.assert_home()
        time.sleep(1.2)
        reveal_wide_home_card(journey, "Spell quest")
        journey.tap_visible("Spell quest", max_scrolls=8)
        journey.wait("The road ahead", timeout=10)
        journey.screenshot("spell-entry")
        journey.tap_visible("Leave", max_scrolls=4)
    with journey.step("open from the Study tools sheet"):
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        if not journey.exists("Spell quest", timeout=3):
            raise JourneyFailure("Spell quest is missing from Study tools")
        journey.screenshot("spell-entry-sheet")
        journey.tap("Spell quest")
        journey.wait("The road ahead", timeout=10)
        journey.tap_visible("Leave", max_scrolls=4)
    journey.assert_no_crash()


def onboarding_pages(journey: Journey, state: ScenarioState) -> None:
    """Every onboarding page, on any width, with the final page scrolled."""
    pages = [
        ("welcome", "Write characters that stay"),
        ("script", "What are you learning?"),
        ("pace", "How many new characters a day?"),
        ("brush", "Pick a writing feel"),
        ("recall", "Set up your reviews"),
    ]
    with journey.step("welcome"):
        journey.wait(pages[0][1], timeout=15)
        journey.screenshot("onboarding-welcome")
        journey.tap("Continue")
    for slug, title in pages[1:]:
        with journey.step(slug):
            journey.wait(title, timeout=10)
            journey.screenshot(f"onboarding-{slug}")
            if slug != "recall":
                journey.tap("Continue")
    with journey.step("the optional quest is visible"):
        width, height = journey.d.window_size()
        for _ in range(6):
            if journey.exists_text("Extra practice"):
                break
            # A short landscape page can put the bottom bar under the gesture.
            journey.d.swipe(width // 2, int(height * 0.55), width // 2, int(height * 0.22), 0.2)
            time.sleep(0.25)
        if not journey.exists_text("Extra practice"):
            raise JourneyFailure(f"onboarding never mentions extra practice. texts={journey.texts()}")
        journey.screenshot("onboarding-recall-scrolled")
        journey.tap_visible("Start writing", max_scrolls=4)
        journey.assert_home()
    journey.assert_no_crash()


def spell_onboarding(journey: Journey, state: ScenarioState) -> None:
    """First run explains the optional spell quest, and the top Home link opens it."""
    with journey.step("finish onboarding"):
        journey.wait("Write characters that stay", timeout=15)
        for _ in range(4):
            journey.tap_visible("Continue")
            time.sleep(0.6)
        journey.wait("Set up your reviews", timeout=8)
        width, height = journey.d.window_size()
        for _ in range(5):
            if journey.exists_text("Extra practice"):
                break
            # On a short landscape page a centre swipe can start on the
            # bottom bar; keep the gesture inside the scrolling content.
            journey.d.swipe(width // 2, int(height * 0.55), width // 2, int(height * 0.22), 0.2)
            time.sleep(0.3)
        if not journey.exists_text("Extra practice"):
            raise JourneyFailure(f"onboarding never mentions extra practice. texts={journey.texts()}")
        journey.assert_text("spell quest")
        journey.screenshot("spell-onboarding")
        journey.tap_visible("Start writing")
        journey.assert_home()
    with journey.step("open from the top Home link"):
        time.sleep(0.8)
        journey.tap_visible("Extra practice · Spell quest", max_scrolls=8)
        journey.wait("The road ahead", timeout=10)
        journey.screenshot("spell-onboarding-gate")
        journey.tap_visible("Leave")
    journey.assert_no_crash()


def spell_long(journey: Journey, state: ScenarioState) -> None:
    """A very long scene problem: only the body scrolls, actions stay fixed."""
    with journey.step("open a long problem"):
        journey.launch(["--ez", "qa_long_problem", "true"])
        journey.assert_home()
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        journey.tap("Spell quest")
        journey.wait("The road ahead", timeout=12)
        journey.tap_any(["Start the road", "Walk on"], timeout=10)
        wait_for_draw_door(journey)
        journey.wait("Draw", timeout=10)
        time.sleep(0.8)
        journey.screenshot("long-problem-top")
    with journey.step("scroll only the problem"):
        scroller = journey.d(scrollable=True)
        if not scroller.exists:
            raise JourneyFailure("the long problem has no scrollable body")
        bounds = scroller.info.get("bounds") or {}
        left, top = bounds.get("left", 0), bounds.get("top", 0)
        right, bottom = bounds.get("right", 1080), bounds.get("bottom", 2400)
        x = (left + right) // 2
        y1 = top + int((bottom - top) * 0.8)
        y2 = top + int((bottom - top) * 0.2)
        before = next(
            (box for value, box in journey._hierarchy_text_nodes()
             if "A long problem line 10" in value),
            None,
        )
        if before is None:
            raise JourneyFailure("the injected long problem is missing")
        for _ in range(2):
            journey.d.swipe(x, y1, x, y2, 1.2)
            time.sleep(0.3)
        after = next(
            (box for value, box in journey._hierarchy_text_nodes()
             if "A long problem line 10" in value),
            None,
        )
        if after is None or after[1] >= before[1] - 4:
            raise JourneyFailure(
                f"the problem body did not scroll: before={before} after={after}"
            )
        journey.screenshot("long-problem-scrolled")
        # The title and the two actions must stay put while the body moves.
        journey.assert_controls_visible(["Draw", "Grimoire"])
        journey.d.press("back")
        time.sleep(0.4)
        if journey.exists("Leave the road", timeout=1):
            journey.tap("Leave the road")
    journey.assert_no_crash()


def spell_flow(journey: Journey, state: ScenarioState) -> None:
    """The screen explains itself: every control is labelled, and no step is a dead end."""
    with journey.step("read the gate"):
        journey.open_tab("Home")
        journey.assert_home()
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        journey.tap("Spell quest")
        journey.wait("The road ahead", timeout=10)
        texts = journey.texts()
        for line in ("Draw the spell", "Or open the book", "Ask if you are stuck"):
            if line not in texts:
                raise JourneyFailure(f"gate missing {line}: {texts}")
        journey.tap_any(["Start the road", "Walk on"])
        wait_for_draw_door(journey)
        journey.wait("Draw", timeout=10)
    with journey.step("the draw tools all say what they do"):
        journey.tap_visible("Draw")
        journey.wait("Cast", timeout=8)
        for label in (
            "Hear", "Info", "Guide", "Clue", "Ink brush", "Pen brush", "Pencil brush", "Marker brush",
            "Undo", "Clear", "Go back", "Grimoire", "Cast",
        ):
            if not journey.exists(label, timeout=2):
                raise JourneyFailure(f"cast overlay missing {label}")
        journey.tap("Info")
        journey.wait("Close note", timeout=6)
        journey.tap("Close note")
        journey.wait("Cast", timeout=6)
        journey.tap("Hear")
        time.sleep(0.4)
        journey.tap("Guide")
        time.sleep(0.6)
        journey.screenshot("spell-flow-tools")
        journey.d.press("back")
        journey.wait("Draw", timeout=8)
    with journey.step("back closes one layer at a time"):
        journey.tap_visible("Draw")
        journey.wait("Cast", timeout=8)
        journey.tap_visible("Grimoire")
        journey.wait("Close book", timeout=10)
        journey.d.press("back")
        journey.wait("Cast", timeout=8)
        journey.d.press("back")
        journey.wait("Draw", timeout=8)
    with journey.step("the grimoire names every chapter"):
        journey.tap_visible("Grimoire")
        journey.wait("Close book", timeout=10)
        for name in ("Tides", "Flame", "Sky", "Ground", "Creatures", "Craft", "Mind", "Time", "Arcana", "Spirit"):
            if not journey.exists(f"{name} chapter", timeout=2):
                raise JourneyFailure(f"grimoire missing {name} chapter")
        journey.screenshot("spell-flow-book")
        journey.tap("Close book")
        journey.wait("Draw", timeout=8)
    with journey.step("leave cleanly"):
        journey.tap_visible("Leave the road")
        # Home keeps its scroll position, so the hero may be above the viewport.
        # Scroll back to the top before using Start as the closed-quest anchor.
        journey.open_tab("Home")
        for _ in range(3):
            journey.scroll_up()
        journey.wait("Start studying", timeout=10)
    journey.assert_no_crash()


def mnemonic_script_hidden(journey: Journey, state: ScenarioState) -> None:
    """Kana and Hangul do not show the Han character palace."""
    with journey.step("open a phonetic character"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Characters")
        journey.wait("Character, meaning, or reading", timeout=10, exact=False)
        _tap_first_character_tile(journey)
        journey.wait("Details", timeout=8)
        if journey.exists("Mnemonic palace", timeout=1):
            raise JourneyFailure("a phonetic character still shows the mnemonic palace")
        journey.screenshot("phonetic-no-palace")
    with journey.step("a phonetic lesson has no hook button"):
        journey.d.press("back")
        time.sleep(0.4)
        journey.open_tab("Home")
        journey.assert_home()
        journey.start_session()
        time.sleep(1.0)
        if journey.d(description="Add a memory hook").exists:
            raise JourneyFailure("a phonetic lesson still shows the hook button")
        journey.leave_session("End session")
    journey.assert_no_crash()


def mnemonic_japanese(journey: Journey, state: ScenarioState) -> None:
    """Japanese keeps on/kun/parts/kana separate and can add a part."""
    with journey.step("read the Japanese guide"):
        journey.launch()
        journey.assert_home()
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        journey.tap_visible("Mnemonic palace", max_scrolls=8)
        journey.wait("The palace is for kanji", timeout=8)
        journey.tap("Next")
        time.sleep(0.5)
        journey.wait("Name the pieces", timeout=6)
        journey.tap("Next")
        time.sleep(0.5)
        journey.wait("Then give a character a page", timeout=6)
        journey.tap("Build my palace")
        time.sleep(0.8)
        journey.wait("Kanji parts", timeout=8)
        for wrong in ("On readings", "Kun readings", "Kana"):
            if journey.exists(wrong, timeout=0.4):
                raise JourneyFailure(f"Japanese palace still shows the sound grid: {wrong}")
        journey.screenshot("mnemonic-ja-palace")
    with journey.step("add a kanji part"):
        journey.tap("Kanji parts")
        time.sleep(0.4)
        journey.tap("Add")
        journey.wait("Kanji part", timeout=6)
        fields = journey.d(className="android.widget.EditText")
        if not fields.exists:
            raise JourneyFailure("the Japanese part fields are missing")
        journey.d(className="android.widget.EditText", instance=0).set_text("氵")
        journey.d(className="android.widget.EditText", instance=1).set_text("water")
        journey.tap("Keep it")
        time.sleep(0.8)
        journey.wait("water", timeout=6, exact=False)
        journey.screenshot("mnemonic-ja-part")
    with journey.step("leave"):
        journey.d.press("back")
        time.sleep(0.6)
        journey.scroll_to_top()
        journey.assert_home()
    journey.assert_no_crash()


def mnemonic_korean(journey: Journey, state: ScenarioState) -> None:
    """Korean Hanja uses parts; Hangul is left alone."""
    with journey.step("read the Korean guide"):
        journey.launch()
        journey.assert_home()
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        journey.tap_visible("Mnemonic palace", max_scrolls=8)
        journey.wait("The palace is for Hanja", timeout=8)
        journey.tap("Next")
        time.sleep(0.5)
        journey.wait("Name the pieces", timeout=6)
        journey.tap("Next")
        time.sleep(0.5)
        journey.wait("Then give a character a page", timeout=6)
        journey.tap("Build my palace")
        time.sleep(0.8)
        journey.wait("Hanja parts", timeout=8)
        for wrong in ("First sounds", "Vowels", "Final sounds"):
            if journey.exists(wrong, timeout=0.4):
                raise JourneyFailure(f"Korean palace still shows the sound grid: {wrong}")
        journey.screenshot("mnemonic-ko-palace")
    with journey.step("add a Hanja part"):
        journey.tap("Add")
        journey.wait("Hanja part", timeout=6)
        fields = journey.d(className="android.widget.EditText")
        if not fields.exists:
            raise JourneyFailure("the Korean part fields are missing")
        journey.d(className="android.widget.EditText", instance=0).set_text("氵")
        journey.d(className="android.widget.EditText", instance=1).set_text("water")
        journey.tap("Keep it")
        time.sleep(0.8)
        journey.wait("water", timeout=6, exact=False)
        journey.screenshot("mnemonic-ko-assigned")
    with journey.step("leave"):
        journey.d.press("back")
        time.sleep(0.6)
        journey.scroll_to_top()
        journey.assert_home()
    journey.assert_no_crash()


def characters_rotation(journey: Journey, state: ScenarioState) -> None:
    """A character page stays open when the phone turns."""
    with journey.step("open a character"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Characters")
        journey.wait("Character, meaning, or reading", timeout=10, exact=False)
        _tap_first_character_tile(journey)
        if not journey.exists_text("to love", timeout=6):
            raise JourneyFailure("the character page did not open")
        journey.screenshot("characters-detail-portrait")
    with journey.step("turn the phone"):
        journey.device.shell("settings put system accelerometer_rotation 0")
        journey.device.shell("settings put system user_rotation 1")
        time.sleep(1.8)
        if not journey.exists_text("to love", timeout=6):
            raise JourneyFailure("the character page closed on rotation")
        if journey.exists("Pick a character", timeout=0.5):
            raise JourneyFailure("rotation fell back to the empty detail pane")
        journey.screenshot("characters-detail-landscape")
    with journey.step("turn back and close"):
        journey.device.shell("settings put system user_rotation 0")
        time.sleep(1.4)
        if not journey.exists_text("to love", timeout=6):
            raise JourneyFailure("the character page closed turning back")
        journey.d.press("back")
        time.sleep(0.5)
        journey.wait("Character, meaning, or reading", timeout=6, exact=False)
        journey.open_tab("Home")
        journey.assert_home()
    journey.assert_no_crash()


def mnemonic_rotation(journey: Journey, state: ScenarioState) -> None:
    """The palace page and its draft survive a turn."""
    with journey.step("open a page and type a scene"):
        journey.launch()
        journey.assert_home()
        journey.open_tab("Characters")
        journey.wait("Character, meaning, or reading", timeout=10, exact=False)
        _tap_first_character_tile(journey)
        width, height = journey.d.window_size()
        if width >= 1800:
            # The detail is the right pane; return it to the top before
            # scrolling down, otherwise a previous position can hide the card.
            for _ in range(4):
                journey.d.swipe(
                    int(width * 0.82), int(height * 0.24),
                    int(width * 0.82), int(height * 0.84),
                    0.12,
                )
                time.sleep(0.1)
            found = False
            for _ in range(16):
                if journey.exists("Mnemonic palace", timeout=0.4):
                    found = True
                    break
                journey.d.swipe(
                    int(width * 0.82), int(height * 0.72),
                    int(width * 0.82), int(height * 0.38),
                    0.2,
                )
                time.sleep(0.12)
            if not found:
                raise JourneyFailure("could not find the palace card in the detail pane")
            journey.tap("Mnemonic palace")
        else:
            journey.scroll_to_top()
            journey.tap_visible("Mnemonic palace", max_scrolls=20)
        journey.wait("A page for", timeout=8, exact=False)
        field = journey.d(className="android.widget.EditText", instance=0)
        field.click()
        field.set_text("Nora")
        time.sleep(0.4)
    with journey.step("turn the screen"):
        journey.device.shell("settings put system accelerometer_rotation 0")
        journey.device.shell("settings put system user_rotation 1")
        time.sleep(1.8)
        if not journey.exists("A page for", timeout=6, exact=False):
            raise JourneyFailure("the palace page closed on rotation")
        if not journey.exists_text("Nora", exact=False):
            raise JourneyFailure("the palace draft was lost on rotation")
        journey.screenshot("mnemonic-rotation-landscape")
        journey.device.shell("settings put system user_rotation 0")
        time.sleep(1.4)
        if not journey.exists_text("Nora", exact=False):
            raise JourneyFailure("the palace draft was lost turning back")
    with journey.step("leave"):
        journey.d.press("back")
        time.sleep(0.4)
        journey.d.press("back")
        time.sleep(0.5)
        journey.open_tab("Home")
        journey.assert_home()
    journey.assert_no_crash()


def _draw_word(
    journey: Journey,
    word: str,
    bounds: dict,
    mode: str = "SIMPLIFIED",
) -> None:
    left = float(bounds["left"])
    top = float(bounds["top"])
    right = float(bounds["right"])
    bottom = float(bounds["bottom"])
    width = right - left
    height = bottom - top
    slot = width / max(1, len(word))
    side = min(slot * 0.86, height * 0.86)
    for index, character in enumerate(word):
        cell_left = left + index * slot + (slot - side) / 2
        cell_top = top + (height - side) / 2
        for stroke in character_guide(character, mode):
            if len(stroke) < 2:
                continue
            points = [
                (cell_left + x * side, cell_top + y * side)
                for x, y in stroke
            ]
            journey.draw_points(points, duration=0.06)
            time.sleep(0.08)


def word_one_by_one_no_model(journey: Journey, state: ScenarioState) -> None:
    """Without the ink model, word practice stays in the safe one-by-one mode."""
    with journey.step("open a word without the model"):
        journey.device.shell("am force-stop com.bassmd.bihua")
        journey.device.shell(
            "am start -n com.bassmd.bihua/.MainActivity "
            "--es qa_word 白天 --ez qa_no_ink_model true"
        )
        journey.wait("Write the word", timeout=15)
        deadline = time.time() + 12
        while time.time() < deadline:
            if journey.exists("Write character 1", timeout=0.5, exact=False):
                break
            time.sleep(0.2)
        else:
            raise JourneyFailure("word practice did not fall back to one by one")
        if journey.exists("Whole word mode", timeout=0.8):
            raise JourneyFailure("whole-word mode is offered without the ink model")
        if not journey.d(
            descriptionContains="Handwriting canvas for the current word character"
        ).exists:
            raise JourneyFailure("the one-by-one canvas is missing")
        journey.screenshot("word-one-by-one-no-model")
    with journey.step("back returns home"):
        journey.d.press("back")
        journey.assert_home()
    journey.assert_no_crash()


def word_whole_canvas(journey: Journey, state: ScenarioState) -> None:
    """Draw a two-character word on one wide page and check the reading."""
    with journey.step("open a known word"):
        journey.device.shell("am force-stop com.bassmd.bihua")
        journey.device.shell(
            "am start -n com.bassmd.bihua/.MainActivity --es qa_word 白天"
        )
        journey.wait("Write the word", timeout=15)
        journey.screenshot("word-whole-start")
    with journey.step("draw the whole word"):
        journey.wait("Write it across the page", timeout=4, exact=False)
        # The brush menu is one compact icon, not a full-width tray.
        journey.d(descriptionContains="Brush:").click()
        time.sleep(0.4)
        journey.screenshot("word-brush-menu")
        journey.tap("Pencil")
        time.sleep(0.3)
        journey.d(descriptionContains="Brush:").click()
        time.sleep(0.3)
        journey.tap("Ink")
        time.sleep(0.3)
        # Whole word is the default. Check both sides of the segmented control.
        journey.tap("One by one mode")
        journey.wait("Write character 1", timeout=4, exact=False)
        if journey.exists("Numbers", timeout=0.6):
            raise JourneyFailure("stroke numbers are offered without the stroke guide")
        journey.tap("Strokes")
        journey.wait("Numbers", timeout=3)
        journey.screenshot("word-numbers")
        journey.tap("Whole word mode")
        journey.wait("Write it across the page", timeout=4, exact=False)
        # The clues are optional; exercise them before the clean attempt.
        journey.tap("Guide")
        journey.tap("Next stroke")
        journey.tap("Play word guide")
        time.sleep(0.8)
        journey.tap("Clear")
        canvas = journey.d(descriptionContains="Handwriting canvas for the whole word")
        if not canvas.exists:
            raise JourneyFailure("the whole-word canvas is missing")
        _draw_word(journey, "白天", canvas.info["bounds"])
        journey.screenshot("word-whole-drawn")
    with journey.step("check the whole word"):
        journey.tap("Check word")
        journey.wait("Handwriting check", timeout=30, exact=False)
        journey.screenshot("word-whole-check")
        if not journey.exists("matches", timeout=75.0):
            raise JourneyFailure("the whole-word check did not match 白天")
    with journey.step("back returns home"):
        journey.d.press("back")
        journey.assert_home()
    journey.assert_no_crash()


def word_practice_home_back(journey: Journey, state: ScenarioState) -> None:
    """Word of the Day returns Home, not to the character grid."""
    with journey.step("open the word from home"):
        journey.launch()
        journey.assert_home()
        journey.tap_visible("Write", max_scrolls=12)
        journey.wait("Write the word", timeout=8)
        journey.screenshot("word-practice-home")
    with journey.step("the arrow returns home"):
        journey.tap("Back")
        journey.assert_home()
    with journey.step("system back also returns home"):
        journey.tap_visible("Write", max_scrolls=12)
        journey.wait("Write the word", timeout=8)
        journey.d.press("back")
        journey.assert_home()
    journey.assert_no_crash()


def lesson_hook(journey: Journey, state: ScenarioState) -> None:
    """The lesson book button opens the character page without leaving the lesson."""
    with journey.step("open the hook from a lesson"):
        journey.launch()
        journey.assert_home()
        journey.start_session()
        journey.wait("Next: trace", timeout=10)
        journey.d(description="Add a memory hook").click()
        journey.wait("A page for", timeout=8, exact=False)
        journey.screenshot("lesson-hook")
        journey.tap_visible("Person or animal", max_scrolls=6)
        time.sleep(0.6)
        journey.screenshot("lesson-hook-keyboard")
        journey.d.press("back")
        time.sleep(0.4)
        journey.tap_visible("Not now", max_scrolls=4)
        time.sleep(0.5)
    with journey.step("leave"):
        journey.leave_session("End session")
    journey.assert_no_crash()


def mnemonic_studio(journey: Journey, state: ScenarioState) -> None:
    """The guide, the sound palace, and one saved association."""
    with journey.step("open the palace guide"):
        journey.launch()
        journey.assert_home()
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        journey.tap_visible("Mnemonic palace", max_scrolls=8)
        journey.wait("A role for every sound", timeout=8)
        journey.screenshot("mnemonic-guide")
    with journey.step("walk the guide"):
        journey.tap("Next")
        time.sleep(0.5)
        journey.wait("Give each part a job", timeout=6)
        journey.tap("Next")
        time.sleep(0.5)
        journey.wait("Then give a character a page", timeout=6)
        journey.screenshot("mnemonic-enable")
        journey.tap("Build my palace")
        time.sleep(0.8)
        journey.wait("Give each sound a role", timeout=8, exact=False)
        journey.screenshot("mnemonic-palace")
    with journey.step("assign a sound"):
        journey.tap_visible("n", max_scrolls=10)
        journey.wait("Who is n?", timeout=8)
        field = journey.d(className="android.widget.EditText")
        if not field.exists:
            raise JourneyFailure("the sound note field is missing")
        field.set_text("Nora the nurse")
        journey.tap("Keep it")
        time.sleep(0.8)
        journey.wait("Nora the nurse", timeout=6, exact=False)
        journey.screenshot("mnemonic-assigned")
    with journey.step("a character can use the palace"):
        journey.d.press("back")
        time.sleep(0.6)
        journey.scroll_to_top()
        journey.open_tab("Characters")
        journey.wait("Character, meaning, or reading", timeout=10, exact=False)
        _tap_first_character_tile(journey)
        width, height = journey.d.window_size()
        for _ in range(14):
            if journey.exists("Mnemonic palace", timeout=0.5):
                break
            if width >= 1800:
                journey.d.swipe(
                    int(width * 0.82), int(height * 0.78),
                    int(width * 0.82), int(height * 0.30),
                    0.25,
                )
            else:
                journey.scroll_down()
            time.sleep(0.15)
        journey.tap_visible("Mnemonic palace", max_scrolls=12)
        journey.wait("A page for", timeout=8, exact=False)
        journey.wait("From your palace", timeout=6, exact=False)
        journey.assert_text("Nora the nurse")
        journey.screenshot("mnemonic-character-page")
        journey.d.press("back")
        time.sleep(0.5)
    with journey.step("leave"):
        journey.d.press("back")
        time.sleep(0.5)
        journey.open_tab("Home")
        time.sleep(0.4)
        journey.scroll_to_top()
        journey.assert_home()
    journey.assert_no_crash()


def open_study_tools(journey: Journey) -> None:
    """Reach the Study tools manager on phone and wide layouts.

    The wide Home labels the button "Manage"; the phone header may be below the
    fold, so scroll instead of assuming it is already on screen.
    """
    width, height = journey.d.window_size()
    dpi = journey.density_dpi()
    wide_home = width * 160 / dpi >= 700
    if wide_home:
        # The wide Home has two independent columns; the Study tools card is in
        # the right one, below the calendar. The calendar can swallow a swipe,
        # so try the right margin, the page gutter, and the column.
        for xf in (0.95, 0.5, 0.78):
            x = int(width * xf)
            for _ in range(8):
                if journey.exists("Manage study tools", timeout=0.4) or journey.exists("Manage", timeout=0.4):
                    break
                journey.d.swipe(x, int(height * 0.82), x, int(height * 0.20), 0.3)
                time.sleep(0.2)
            for label in ("Manage study tools", "Manage"):
                try:
                    journey.tap(label)
                    return
                except JourneyFailure:
                    continue
        raise JourneyFailure(f"could not tap Manage. texts={journey.texts()}")
    for _ in range(4):
        journey.scroll_up()
    for label in ("Manage study tools", "Manage"):
        try:
            journey.tap_visible(label, max_scrolls=10)
            return
        except JourneyFailure:
            continue
    # Some fold/landscape shapes mix the phone shell with a two-page Home.
    # Try scrolling each plausible pane before giving up.
    for xf in (0.78, 0.2, 0.5):
        x = int(width * xf)
        for _ in range(8):
            for label in ("Manage study tools", "Manage"):
                try:
                    journey.tap(label)
                    return
                except JourneyFailure:
                    pass
            journey.d.swipe(x, int(height * 0.72), x, int(height * 0.38), 0.2)
            time.sleep(0.2)
    raise JourneyFailure(f"could not reach Study tools. texts={journey.texts()}")


def wait_for_draw_door(journey: Journey, timeout: float = 90) -> None:
    """A fresh install may still be fetching the one-time reading model.

    The quest opens on the book path while the download runs. Wait for the
    draw door rather than treating a download as a broken screen.
    """
    deadline = time.time() + timeout
    while time.time() < deadline:
        if journey.exists("Draw", timeout=0.5):
            return
        if journey.exists("Open the grimoire", timeout=0.2):
            time.sleep(1.0)
            continue
        time.sleep(0.5)
    raise JourneyFailure(
        "the reading model did not arrive and the scene has no way forward: "
        f"{journey.texts()}"
    )


def spell_quest(journey: Journey, state: ScenarioState) -> None:
    """The endless road, reached from Study tools, casting from the book."""
    with journey.step("open the spell quest"):
        journey.open_tab("Home")
        journey.assert_home()
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        journey.tap("Spell quest")
        journey.wait("The road ahead", timeout=10)
        issues = journey.a11y_unlabeled()
        if issues:
            raise JourneyFailure(f"spell gate has unlabeled controls: {issues[:3]}")
        journey.screenshot("spell-gate")
    with journey.step("start the road"):
        journey.tap_visible("Start the road", max_scrolls=8)
        wait_for_draw_door(journey)
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
        journey.screenshot("spell-chapter")
        # Chapter seals sit in the tabs above the page; the spell tiles come later
        # in the tree, so take the last character on screen.
        seals = set("水火云石马桥心时魔灵雲馬橋時靈霊")
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
            if any(any(label in text for label in RESULT_LABELS) for text in texts):
                break
            time.sleep(0.3)
        else:
            raise JourneyFailure(f"gift produced no result. texts={journey.texts()}")
        # Mid-effect frames are kept for the motion check, but the listing
        # screenshot waits until the character and the flourish have finished.
        for frame in (1, 2, 3):
            time.sleep(0.25)
            journey.screenshot(f"spell-effect-{frame}")
        deadline = time.time() + 25
        while time.time() < deadline:
            texts = journey.texts()
            if any(
                label in texts
                for label in ("Walk on", "Walk on for now", "Show me one answer")
            ):
                break
            time.sleep(0.3)
        else:
            raise JourneyFailure(f"gift actions never unlocked. texts={journey.texts()}")
        time.sleep(1.6)
        journey.screenshot("spell-result")
        journey.tap("Note")
        journey.wait("Mò's note", timeout=8)
        time.sleep(0.5)
        journey.screenshot("spell-note")
        try:
            journey.tap("Close note")
            time.sleep(0.3)
        except JourneyFailure:
            journey.d.press("back")
    with journey.step("one answer, then cast it"):
        mode = state.settings.get("studyMode", "SIMPLIFIED")
        worked = any("It worked" in text for text in journey.texts())
        if not worked:
            answer_button = next(
                (label for label in ("Show me one answer", "One answer") if journey.exists(label, timeout=1)),
                None,
            )
            if answer_button is None:
                raise JourneyFailure(f"no answer action after a nudge. texts={journey.texts()}")
            journey.tap(answer_button)
            deadline = time.time() + 25
            answer = None
            while time.time() < deadline and answer is None:
                answer = next(
                    (
                        text.split(": ", 1)[1]
                        for text in journey.texts()
                        if text.startswith("One answer: ")
                    ),
                    None,
                )
                time.sleep(0.3)
            if not answer:
                raise JourneyFailure(f"the answer was not named. texts={journey.texts()}")
            time.sleep(1.2)
            journey.screenshot("spell-answer")
            # Draw the shape the hint named: the exact same character must be
            # accepted, not just any spell in the chapter.
            journey.tap_visible("Draw", max_scrolls=8)
            journey.wait("Cast", timeout=8)
            # The fold/tablet book must keep its action cluster on screen. This
            # guards the clipped third row that shipped in an earlier 1.1.1 build.
            journey.assert_controls_visible(["Undo", "Clear", "Cast", "Go back"])
            # The reference shape stays as a coloured, translucent underlay.
            if journey.exists("Guide", timeout=1):
                journey.tap("Guide")
                time.sleep(1.6)
                journey.screenshot("spell-guide")
                journey.tap("Hide guide")
                time.sleep(0.3)
            guide = character_guide(answer, mode)
            if not guide:
                raise JourneyFailure(f"no guide strokes for the answer {answer}")
            if not journey.draw_guide(guide):
                raise JourneyFailure("no handwriting canvas for the answer")
            # Keep the inked circle before the cast: this is the listing shot.
            time.sleep(0.6)
            journey.screenshot("spell-drawn")
            journey.wait("Cast", timeout=8)
            journey.tap("Cast")
            deadline = time.time() + 30
            while time.time() < deadline:
                if "It worked" in journey.texts():
                    break
                time.sleep(0.4)
            else:
                raise JourneyFailure(f"the named answer did not solve it. texts={journey.texts()}")
            # The result line appears before the circle finishes. The listing
            # screenshot waits for the flourish and the result actions.
            deadline = time.time() + 25
            while time.time() < deadline:
                if any(
                    label in journey.texts()
                    for label in ("Walk on", "Walk on for now")
                ):
                    break
                time.sleep(0.3)
            else:
                raise JourneyFailure(f"success actions never unlocked. texts={journey.texts()}")
        time.sleep(1.2)
        journey.screenshot("spell-success")
        journey.tap_any(["Walk on", "Walk on for now"])
        journey.wait("Draw", timeout=10)
        journey.screenshot("spell-next")
    with journey.step("draw and cast"):
        # The drawing path itself: draw in the circle with touch, then cast.
        journey.tap_visible("Draw", max_scrolls=8)
        journey.wait("Cast", timeout=8)
        if journey.exists("Guide", timeout=1):
            journey.tap("Guide")
            time.sleep(1.6)
            journey.screenshot("spell-guide")
            journey.tap("Hide guide")
            time.sleep(0.3)
        issues = journey.a11y_unlabeled()
        if issues:
            raise JourneyFailure(f"cast overlay has unlabeled controls: {issues[:3]}")
        # The quest has its own brush tray; the choice stays inside the game.
        for tool in ("Ink brush", "Pen brush", "Pencil brush", "Marker brush"):
            if not journey.exists(tool, timeout=2):
                raise JourneyFailure(f"quest brush tray missing {tool}")
        journey.tap("Pencil brush")
        time.sleep(0.4)
        journey.screenshot("spell-brush")
        journey.tap("Ink brush")
        # Info is a popup with the character's reading and meaning, the same
        # anchored card as a highlighted word. It never edits the scene.
        journey.tap("Info")
        journey.wait("Close note", timeout=6)
        journey.screenshot("spell-meaning")
        journey.tap("Close note")
        # Draw a real spell with the content database's guide strokes, the same
        # way the review journeys write. A plus can be read as 十, which is not
        # a spell in this book.
        mode = state.settings.get("studyMode", "SIMPLIFIED")
        guide = character_guide("水", mode)
        if not guide:
            raise JourneyFailure("no guide strokes for 水")
        if not journey.draw_guide(guide):
            raise JourneyFailure("no handwriting canvas to draw on")
        journey.wait("Cast", timeout=8)
        journey.screenshot("spell-drawn")
        journey.tap("Cast")
        deadline = time.time() + 30
        outcome = None
        while time.time() < deadline and outcome is None:
            texts = journey.texts()
            if any(any(label in text for label in RESULT_LABELS[1:]) for text in texts):
                outcome = "nudge"
            elif any("It worked" in text for text in texts):
                outcome = "worked"
            elif any("is not in this book yet" in text or "Mò read" in text for text in texts):
                outcome = "unreadable"
            else:
                time.sleep(0.4)
        if outcome is None:
            raise JourneyFailure(f"cast produced no result. texts={journey.texts()}")
        if outcome == "unreadable":
            # A readable character that is not a spell here: back to the drawing.
            journey.screenshot("spell-cast")
            journey.tap("Go back")
        else:
            deadline = time.time() + 25
            while time.time() < deadline:
                labels = journey.texts()
                if any(
                    label in labels
                    for label in ("Walk on", "Walk on for now", "Show me one answer")
                ):
                    break
                time.sleep(0.3)
            else:
                raise JourneyFailure(f"cast actions never unlocked. texts={journey.texts()}")
            time.sleep(0.8)
            journey.screenshot("spell-cast")
            journey.tap_any(["Walk on for now", "Walk on"])
        journey.wait("Draw", timeout=10)
    with journey.step("return to the road"):
        # Quest progress lives in the progress database, so closing and reopening
        # should show the pages met and the saved chapter in the gate.
        journey.tap_visible("Leave the road")
        open_study_tools(journey)
        journey.wait("Study tools", timeout=8)
        journey.tap("Spell quest")
        journey.wait("The road ahead", timeout=10)
        texts = journey.texts()
        if not any("met" in text and "spell" in text for text in texts):
            raise JourneyFailure(f"gate did not show met progress. texts={texts}")
        journey.screenshot("spell-return")
        journey.tap_any(["Open the grimoire", "Grimoire"])
        journey.wait("Close book", timeout=10)
        journey.screenshot("spell-met-book")
        journey.tap("Close book")
        journey.tap_visible("Leave", max_scrolls=4)
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
    "writing_reveal": writing_reveal,
    "brush_presets": brush_presets,
    "practice_check": practice_check,
    "practice_slow": practice_slow,
    "spell_offline": spell_offline,
    "model_notice": model_notice,
    "offline_first_run": offline_first_run,
    "model_offline_switch": model_offline_switch,
    "rotation": rotation,
    "rotation_state": rotation_state,
    "rotation_onboarding": rotation_onboarding,
    "characters_scroll": characters_scroll,
    "study_tools_pdf": study_tools_pdf,
    "monkey": monkey,
    "screen_pass": screen_pass,
    "settings_pages": settings_pages,
    "settings_writing_mode": settings_writing_mode,
    "settings_writing_model": settings_writing_model,
    "known_list_preview": known_list_preview,
    "report_flow": report_flow,
    "review_modes": review_modes,
    "pronounce_kana": pronounce_kana,
    "draw_recognition": draw_recognition,
    "study_modes": study_modes,
    "grade_with_reason": grade_with_reason,
    "mnemonic_studio": mnemonic_studio,
    "characters_rotation": characters_rotation,
    "word_practice_home_back": word_practice_home_back,
    "word_whole_canvas": word_whole_canvas,
    "word_one_by_one_no_model": word_one_by_one_no_model,
    "mnemonic_rotation": mnemonic_rotation,
    "mnemonic_japanese": mnemonic_japanese,
    "mnemonic_korean": mnemonic_korean,
    "mnemonic_script_hidden": mnemonic_script_hidden,
    "lesson_hook": lesson_hook,
    "paper_compare": paper_compare,
    "study_lists": study_lists,
    "appearance": appearance,
    "word_audio": word_audio,
    "reveal_review": reveal_review,
    "meaning": meaning,
    "meaning_reveal": meaning_reveal,
    "synonyms": synonyms,
    "tour": tour,
    "settings_outfit": settings_outfit,
    "grade_help": grade_help,
    "spell_quest": spell_quest,
    "spell_flow": spell_flow,
    "spell_long": spell_long,
    "spell_onboarding": spell_onboarding,
    "onboarding_pages": onboarding_pages,
    "spell_entry": spell_entry,
    "progress_ranges": progress_ranges,
    "progress_screen": progress_screen,
    "progress_all_time": progress_all_time,
    "progress_custom_range": progress_custom_range,
    "progress_custom_single_day": progress_custom_single_day,
    "progress_scripts": progress_scripts,
    "script_switch_loading": script_switch_loading,
    "progress_pdf_rich": progress_pdf_rich,
    "progress_pdf_save": progress_pdf_save,
    "progress_pdf_share": progress_pdf_share,
    "progress_empty": progress_empty,
    "nav_gap": nav_gap,
}
