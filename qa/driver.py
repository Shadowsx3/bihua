#!/usr/bin/env python3
"""UI driver and assertions built on uiautomator2."""
from __future__ import annotations

import re
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

import uiautomator2 as u2

from devices import Device

APP_ID = "com.bassmd.bihua"


class JourneyFailure(AssertionError):
    pass


@dataclass
class StepResult:
    name: str
    seconds: float
    status: str = "ok"
    detail: str = ""


@dataclass
class Journey:
    device: Device
    artifact_dir: Path
    screenshots: bool = False
    steps: list[StepResult] = field(default_factory=list)

    def __post_init__(self) -> None:
        self.artifact_dir.mkdir(parents=True, exist_ok=True)
        self.d = u2.connect(self.device.serial)
        self.d.settings["wait_timeout"] = 12.0
        self.d.settings["operation_delay"] = (0.03, 0.06)

    # ---------- selection ----------

    def _selectors(self, label: str, exact: bool = True):
        selectors = []
        if exact:
            selectors += [
                self.d(description=label),
                self.d(text=label),
            ]
        else:
            selectors += [
                self.d(descriptionContains=label),
                self.d(textContains=label),
            ]
        return selectors

    def find(self, label: str, timeout: float = 8.0, exact: bool = True):
        deadline = time.time() + timeout
        while time.time() < deadline:
            for selector in self._selectors(label, exact):
                try:
                    if selector.exists:
                        return selector
                except Exception:
                    continue
            time.sleep(0.2)
        return None

    # ---------- hierarchy fallback ----------
    # uiautomator's selector queries wait for an idle window. A live tour,
    # confetti, or a typewriter can keep the window busy even though the node is
    # present and tappable. Dump the hierarchy and click the bounds instead.

    def _hierarchy_nodes(self) -> list[tuple[str, tuple[int, int, int, int], bool]]:
        try:
            xml = self.d.dump_hierarchy(compressed=False)
            root = ET.fromstring(xml)
        except Exception:
            return []
        out = []
        for node in root.iter("node"):
            value = node.attrib.get("text") or node.attrib.get("content-desc") or ""
            if not value or node.attrib.get("visible-to-user") != "true":
                continue
            match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
            if not match:
                continue
            x1, y1, x2, y2 = (int(part) for part in match.groups())
            out.append((value, (x1, y1, x2, y2), node.attrib.get("clickable") == "true"))
        return out

    def _match_hierarchy(self, label: str, exact: bool) -> list[tuple[str, tuple[int, int, int, int], bool]]:
        found = []
        for value, bounds, clickable in self._hierarchy_nodes():
            if ((value == label) if exact else (label in value)):
                found.append((value, bounds, clickable))
        return found

    def _hierarchy_text_nodes(self) -> list[tuple[str, tuple[int, int, int, int]]]:
        try:
            root = ET.fromstring(self.d.dump_hierarchy(compressed=False))
        except Exception:
            return []
        out = []
        for node in root.iter("node"):
            value = node.attrib.get("text") or ""
            if not value or node.attrib.get("visible-to-user") != "true":
                continue
            match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
            if not match:
                continue
            out.append((value, tuple(int(part) for part in match.groups())))
        return out

    def _match_text_hierarchy(self, label: str, exact: bool) -> list[tuple[str, tuple[int, int, int, int]]]:
        return [
            (value, bounds)
            for value, bounds in self._hierarchy_text_nodes()
            if ((value == label) if exact else (label in value))
        ]

    def exists_text(self, label: str, timeout: float = 0.5, exact: bool = True) -> bool:
        deadline = time.time() + timeout
        selector = self.d(text=label) if exact else self.d(textContains=label)
        while time.time() < deadline:
            try:
                if selector.exists:
                    return True
            except Exception:
                pass
            if self._match_text_hierarchy(label, exact):
                return True
            time.sleep(0.2)
        return False

    def tap_text(self, label: str, timeout: float = 8.0, exact: bool = True) -> None:
        deadline = time.time() + timeout
        selector = self.d(text=label) if exact else self.d(textContains=label)
        while time.time() < deadline:
            try:
                if selector.exists:
                    selector.click()
                    time.sleep(0.12)
                    return
            except Exception:
                pass
            matches = self._match_text_hierarchy(label, exact)
            if matches:
                _value, (x1, y1, x2, y2) = matches[0]
                self.d.click((x1 + x2) // 2, (y1 + y2) // 2)
                time.sleep(0.12)
                return
            time.sleep(0.2)
        raise JourneyFailure(f"could not tap text: {label}")

    def _tap_hierarchy(self, label: str, timeout: float, exact: bool) -> bool:
        deadline = time.time() + timeout
        while time.time() < deadline:
            matches = self._match_hierarchy(label, exact)
            if matches:
                # Prefer a clickable node, then the nearest parent bounds.
                value, (x1, y1, x2, y2), _ = matches[0]
                self.d.click((x1 + x2) // 2, (y1 + y2) // 2)
                time.sleep(0.12)
                return True
            time.sleep(0.2)
        return False

    def wait(self, label: str, timeout: float = 8.0, exact: bool = True) -> None:
        if self.find(label, timeout=min(timeout, 2.0), exact=exact) is not None:
            return
        if self._match_hierarchy(label, exact):
            return
        if self._wait_hierarchy(label, timeout - 2.0, exact):
            return
        raise JourneyFailure(f"not visible: {label}")

    def _wait_hierarchy(self, label: str, timeout: float, exact: bool) -> bool:
        deadline = time.time() + max(timeout, 0.0)
        while time.time() < deadline:
            if self._match_hierarchy(label, exact):
                return True
            time.sleep(0.2)
        return False

    def tap(self, label: str, timeout: float = 8.0, exact: bool = True) -> None:
        selector = self.find(label, timeout=min(timeout, 2.0), exact=exact)
        if selector is not None:
            selector.click()
            time.sleep(0.12)
            return
        if self._tap_hierarchy(label, timeout - 2.0, exact):
            return
        raise JourneyFailure(f"could not tap: {label}")

    def tap_any(self, labels: list[str], timeout: float = 8.0) -> str:
        deadline = time.time() + timeout
        while time.time() < deadline:
            for label in labels:
                selector = self.find(label, timeout=0.1)
                if selector is None:
                    selector = self.find(label, timeout=0.1, exact=False)
                if selector is not None:
                    selector.click()
                    time.sleep(0.12)
                    return label
        raise JourneyFailure(f"none visible: {', '.join(labels)}")

    def exists(self, label: str, timeout: float = 0.5, exact: bool = True) -> bool:
        return self.find(label, timeout=timeout, exact=exact) is not None

    # ---------- screen text ----------

    def value_above(self, label: str, exact: bool = True) -> str | None:
        """The text drawn directly above a label, used by stat tiles."""
        nodes = self._hierarchy_text_nodes()
        labels = [bounds for value, bounds in nodes if (value == label if exact else label in value)]
        for x1, y1, x2, y2 in labels:
            closest = None
            for value, (cx1, cy1, cx2, cy2) in nodes:
                if not value.strip() or value == label:
                    continue
                if cy2 > y1 or y1 - cy2 > 160:
                    continue
                if cx2 <= x1 or cx1 >= x2:
                    continue
                if closest is None or cy2 > closest[0]:
                    closest = (cy2, value)
            if closest is not None:
                return closest[1]
        return None

    def texts(self) -> list[str]:
        xml = self.d.dump_hierarchy(compressed=False)
        root = ET.fromstring(xml)
        out = []
        for node in root.iter("node"):
            text = node.attrib.get("text") or node.attrib.get("content-desc") or ""
            if text:
                out.append(text)
        return out

    def text_contains(self, needle: str) -> bool:
        return any(needle in text for text in self.texts())

    def assert_text(self, needle: str) -> None:
        if self.find(needle, timeout=2, exact=False) is not None:
            return
        if self._match_hierarchy(needle, exact=False):
            return
        if self._wait_hierarchy(needle, 2.0, exact=False):
            return
        raise JourneyFailure(f"missing text: {needle}\ntexts={self.texts()}")

    def assert_home(self) -> None:
        self.wait("Home", timeout=12)
        self.wait("Hello", timeout=12, exact=False)

    def home_counts(self) -> tuple[int, int]:
        text = " | ".join(self.texts())
        due = 0
        new = 0
        match = re.search(r"(\d[\d,]*)\s+to review", text)
        if match:
            due = int(match.group(1).replace(",", ""))
        match = re.search(r"(\d[\d,]*)\s+new today", text)
        if match:
            new = int(match.group(1).replace(",", ""))
        match = re.search(r"(\d[\d,]*)\s+new characters ready", text)
        if match:
            new = int(match.group(1).replace(",", ""))
        return due, new

    def assert_home_counts(self, due: int | None = None, new: int | None = None) -> None:
        deadline = time.time() + 8
        actual_due = 0
        actual_new = 0
        while time.time() < deadline:
            actual_due, actual_new = self.home_counts()
            if (due is None or actual_due == due) and (new is None or actual_new == new):
                return
            time.sleep(0.25)
        if due is not None and actual_due != due:
            raise JourneyFailure(f"Home due {actual_due}, expected {due}. texts={self.texts()}")
        raise JourneyFailure(f"Home new {actual_new}, expected {new}. texts={self.texts()}")

    # ---------- actions ----------

    def launch(self) -> None:
        self.device.shell(f"am force-stop {APP_ID}", check=False)
        self.device.shell(
            f"am start -W -n {APP_ID}/.MainActivity",
            check=False,
        )
        time.sleep(0.8)

    def force_stop(self) -> None:
        self.d.app_stop(APP_ID)

    def scroll_down(self) -> None:
        width, height = self.d.window_size()
        self.d.swipe(width // 2, int(height * 0.76), width // 2, int(height * 0.32), 0.2)
        time.sleep(0.2)

    def scroll_up(self) -> None:
        width, height = self.d.window_size()
        self.d.swipe(width // 2, int(height * 0.28), width // 2, int(height * 0.82), 0.2)
        time.sleep(0.2)

    def ensure_visible(self, label: str, max_scrolls: int = 5):
        selector = self.find(label, timeout=0.5)
        for _ in range(max_scrolls):
            if selector is not None:
                return selector
            self.scroll_down()
            selector = self.find(label, timeout=0.5)
        return selector

    def tap_visible(self, label: str, max_scrolls: int = 5) -> None:
        selector = self.ensure_visible(label, max_scrolls)
        if selector is None:
            raise JourneyFailure(f"could not find after scrolling: {label}")
        selector.click()
        time.sleep(0.15)

    def open_tab(self, name: str) -> None:
        self.tap(name)
        time.sleep(0.4)

    def start_session(self) -> None:
        label = self.tap_any(
            [
                "Start studying",
                "Practice instead",
                "Learn ",
            ],
            timeout=10,
        )
        if label == "Practice instead":
            raise JourneyFailure("Home offered Practice instead, no study queue")
        deadline = time.time() + 12
        while time.time() < deadline:
            if self.exists("Review 1 of", timeout=0.1, exact=False):
                return
            if self.exists("Lesson 1 of", timeout=0.1, exact=False):
                return
            if self.exists("Session complete", timeout=0.1):
                return
            time.sleep(0.25)
        raise JourneyFailure("session did not start")

    def session_counts(self) -> tuple[int, int] | None:
        text = " | ".join(self.texts())
        match = re.search(r"(Review|Lesson)\s+(\d+)\s+of\s+(\d+)", text)
        if not match:
            return None
        return int(match.group(2)), int(match.group(3))

    def session_hint(self) -> str | None:
        for text in self.texts():
            if text.startswith("Next lesson in") or text in {
                "Final review block.",
                "New lesson. The next block starts after it.",
            }:
                return text
        return None

    def _canvas(self):
        for label in ("Handwriting canvas",):
            selector = self.d(descriptionContains=label)
            if selector.exists:
                return selector
        return None

    def draw_guide(self, strokes: list[list[tuple[float, float]]]) -> bool:
        canvas = self._canvas()
        if canvas is None or not strokes:
            return False
        info = canvas.info
        bounds = info.get("bounds") or {}
        left, top = bounds.get("left", 0), bounds.get("top", 0)
        right, bottom = bounds.get("right", 0), bounds.get("bottom", 0)
        width, height = right - left, bottom - top
        if width <= 0 or height <= 0:
            return False
        margin_x, margin_y = width * 0.08, height * 0.08
        inner_w, inner_h = width - margin_x * 2, height - margin_y * 2
        for stroke in strokes:
            points = [
                (
                    int(left + margin_x + x * inner_w),
                    int(top + margin_y + y * inner_h),
                )
                for x, y in stroke
            ]
            if len(points) >= 2:
                self.draw_points(points)
        return True

    def draw_points(self, points: list[tuple[int, int]]) -> None:
        if len(points) < 2:
            return
        commands = [f"input motionevent DOWN {points[0][0]} {points[0][1]}"]
        commands += [f"input motionevent MOVE {x} {y}" for x, y in points[1:]]
        commands.append(f"input motionevent UP {points[-1][0]} {points[-1][1]}")
        self.device.shell("; ".join(commands))

    def _draw_once(self) -> bool:
        canvas = self._canvas()
        if canvas is None:
            return False
        try:
            info = canvas.info
            bounds = info.get("bounds") or {}
            left, top = bounds.get("left", 0), bounds.get("top", 0)
            right, bottom = bounds.get("right", 0), bounds.get("bottom", 0)
            if right <= left or bottom <= top:
                return False
            west = left + (right - left) * 0.32
            east = left + (right - left) * 0.68
            north = top + (bottom - top) * 0.35
            south = top + (bottom - top) * 0.65
            self.draw_points([
                (int(west), int(north)),
                (int(east), int(south)),
            ])
            time.sleep(0.1)
            return True
        except Exception:
            return False

    def grade_one(self, fast: bool = False) -> str:
        if self.d(text="Session complete").exists:
            return "complete"
        if fast:
            if self.d(text="Show answer").click_exists(timeout=1.0):
                if self.d(text="Good").click_exists(timeout=5.0):
                    return "graded"
                return self.grade_one(fast=False)
            if self.d(text="Good").click_exists(timeout=0.0):
                return "graded"
            return self.grade_one(fast=False)
        if self.exists("Session complete", timeout=0.2):
            return "complete"
        if self.exists("Next: trace", timeout=0.2):
            self.tap("Next: trace")
            return "step"
        if self.exists("Next: write", timeout=0.2):
            selector = self.find("Next: write", timeout=0.1)
            if selector is not None and not selector.info.get("enabled", True):
                self._draw_once()
            self.tap("Next: write")
            return "step"
        if self.exists("Show answer", timeout=0.2):
            self.tap("Show answer")
            time.sleep(0.3)
            self.tap_any(["Good", "Easy", "Hard", "Again"], timeout=6)
            return "graded"
        if self.exists("Check & grade", timeout=0.2):
            self._draw_once()
            selector = self.find("Check & grade", timeout=0.1)
            if selector is not None and selector.info.get("enabled", True):
                selector.click()
                time.sleep(0.25)
            if self.exists("Show answer", timeout=0.2):
                self.tap("Show answer")
            time.sleep(0.3)
            self.tap_any(["Good", "Easy", "Hard", "Again"], timeout=6)
            return "graded"
        # The reveal can appear after recognition settles.
        if self.exists("Good", timeout=0.5):
            self.tap("Good")
            return "graded"
        raise JourneyFailure(f"no grade control visible. texts={self.texts()}")

    def drain_session(self, max_cards: int = 250, fast: bool = False) -> int:
        done = 0
        deadline = time.time() + max(60, max_cards * 4)
        while time.time() < deadline:
            if self.d(text="Session complete").exists:
                self.screenshot("session-complete")
                self.tap("Back to home")
                self.assert_home()
                return done
            self.grade_one(fast=fast)
            done += 1
            if done > max_cards:
                raise JourneyFailure(f"session exceeded {max_cards} actions")
        raise JourneyFailure("session drain timed out")

    def leave_session(self, action: str = "Resume later") -> None:
        self.tap("Close session")
        self.wait("Leave this session?", timeout=6, exact=False)
        self.tap(action)
        self.assert_home()

    def resume_session(self) -> None:
        self.tap("Resume")
        deadline = time.time() + 10
        while time.time() < deadline:
            if self.exists("Review ", timeout=0.1, exact=False):
                return
            if self.exists("Lesson ", timeout=0.1, exact=False):
                return
            time.sleep(0.2)
        raise JourneyFailure("session did not resume")

    # ---------- artifacts and checks ----------

    def screenshot(self, name: str) -> Path:
        path = self.artifact_dir / f"{name}.png"
        try:
            self.d.screenshot(str(path))
        except Exception:
            pass
        return path

    def dump_ui(self, name: str) -> Path:
        path = self.artifact_dir / f"{name}.xml"
        try:
            path.write_text(self.d.dump_hierarchy(compressed=False))
        except Exception:
            path.write_text("<missing />")
        return path

    def logcat(self, name: str = "logcat.txt") -> Path:
        path = self.artifact_dir / name
        path.write_text(self.device.logcat())
        return path

    def assert_no_crash(self) -> None:
        log = self.device.logcat()
        bad = [
            line for line in log.splitlines()
            if "FATAL EXCEPTION" in line
            or ("ANR in " in line and APP_ID in line)
        ]
        if bad:
            raise JourneyFailure("crash or ANR: " + " | ".join(bad[-5:]))

    def a11y_unlabeled(self) -> list[str]:
        xml = self.d.dump_hierarchy(compressed=False)
        root = ET.fromstring(xml)

        def bounds(node) -> tuple[int, int, int, int] | None:
            match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
            return tuple(int(part) for part in match.groups()) if match else None

        def inside(inner, outer) -> bool:
            return (
                inner[0] >= outer[0] - 2
                and inner[1] >= outer[1] - 2
                and inner[2] <= outer[2] + 2
                and inner[3] <= outer[3] + 2
            )

        labeled = []
        for node in root.iter("node"):
            label = (node.attrib.get("content-desc") or node.attrib.get("text") or "").strip()
            box = bounds(node)
            if label and box:
                labeled.append(box)

        issues = []
        for node in root.iter("node"):
            if node.attrib.get("clickable") != "true":
                continue
            label = (node.attrib.get("content-desc") or node.attrib.get("text") or "").strip()
            if label:
                continue
            box = bounds(node)
            # Compose often puts the visible word on a child node. A clickable
            # surface with a labeled child is still understandable.
            if box and any(inside(child, box) for child in labeled):
                continue
            issues.append(node.attrib.get("bounds", "?"))
        return issues

    def step(self, name: str):
        return _Step(self, name)


class _Step:
    def __init__(self, journey: Journey, name: str):
        self.journey = journey
        self.name = name
        self.start = 0.0

    def __enter__(self):
        self.start = time.time()
        print(f"    - {self.name}")
        return self

    def __exit__(self, exc_type, exc, tb):
        elapsed = time.time() - self.start
        status = "ok" if exc is None else "failed"
        detail = "" if exc is None else str(exc)[:500]
        self.journey.steps.append(StepResult(self.name, elapsed, status, detail))
        if exc is not None:
            self.journey.screenshot(f"failure-{len(self.journey.steps)}")
            self.journey.dump_ui(f"failure-{len(self.journey.steps)}")
            self.journey.logcat()
        return False
