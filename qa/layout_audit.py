#!/usr/bin/env python3
"""Layout invariants from a live uiautomator hierarchy dump.

These checks catch the class of bug that is invisible to state assertions:
a control that is clipped, off screen, too small, or overlapping another
control. It is intentionally conservative; warnings are recorded but only
errors fail a journey.
"""
from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass
from pathlib import Path

BOUNDS_RE = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")


@dataclass
class LayoutIssue:
    severity: str
    kind: str
    message: str
    bounds: tuple[int, int, int, int] | None
    label: str = ""

    def as_dict(self) -> dict:
        return asdict(self)


def parse_bounds(value: str) -> tuple[int, int, int, int] | None:
    match = BOUNDS_RE.match(value or "")
    return tuple(int(part) for part in match.groups()) if match else None


def _area(box: tuple[int, int, int, int]) -> int:
    return max(0, box[2] - box[0]) * max(0, box[3] - box[1])


def _intersection(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> int:
    x1, y1 = max(a[0], b[0]), max(a[1], b[1])
    x2, y2 = min(a[2], b[2]), min(a[3], b[3])
    return max(0, x2 - x1) * max(0, y2 - y1)


def _inside(inner: tuple[int, int, int, int], outer: tuple[int, int, int, int], tolerance: int = 2) -> bool:
    return (
        inner[0] >= outer[0] - tolerance
        and inner[1] >= outer[1] - tolerance
        and inner[2] <= outer[2] + tolerance
        and inner[3] <= outer[3] + tolerance
    )


def _label(node: ET.Element) -> str:
    return (node.attrib.get("content-desc") or node.attrib.get("text") or "").strip()


def audit_xml(
    xml: str,
    screen_width: int,
    screen_height: int,
    density_dpi: int = 420,
) -> list[LayoutIssue]:
    try:
        root = ET.fromstring(xml)
    except ET.ParseError as error:
        return [LayoutIssue("error", "parse", f"could not parse hierarchy: {error}", None)]

    screen = (0, 0, screen_width, screen_height)
    min_touch = int(40 * density_dpi / 160)
    issues: list[LayoutIssue] = []
    clickable: list[tuple[ET.Element, tuple[int, int, int, int], str]] = []
    parents: dict[int, ET.Element | None] = {}

    def walk(node: ET.Element, parent: ET.Element | None) -> None:
        parents[id(node)] = parent
        box = parse_bounds(node.attrib.get("bounds", ""))
        visible = node.attrib.get("visible-to-user", "true") == "true"
        if visible and box:
            label = _label(node)
            if node.attrib.get("clickable") == "true":
                width, height = box[2] - box[0], box[3] - box[1]
                parent_box = parse_bounds(parent.attrib.get("bounds", "")) if parent is not None else None
                # A row at the viewport edge is clipped to a one-pixel sliver
                # while it scrolls out; Compose can also report the cell just
                # past its parent. That is normal, not a broken control.
                clipped_at_edge = bool(parent_box) and (
                    not _inside(box, parent_box, 0)
                    or abs(box[1] - parent_box[1]) <= 2
                    or abs(box[3] - parent_box[3]) <= 2
                    or abs(box[0] - parent_box[0]) <= 2
                    or abs(box[2] - parent_box[2]) <= 2
                )
                if (width <= 1 or height <= 1) and not clipped_at_edge:
                    issues.append(LayoutIssue(
                        "error", "zero-size",
                        f"clickable node has no usable size ({width}x{height})",
                        box, label,
                    ))
                elif width > 1 and height > 1:
                    if not _inside(box, screen, 16):
                        issues.append(LayoutIssue(
                            "error", "offscreen",
                            f"clickable node leaves the {screen_width}x{screen_height} screen",
                            box, label,
                        ))
                    # Compose semantics wrappers and minimum touch targets can
                    # legitimately extend a few pixels past the row bounds.
                    # This is review material, not a hard failure.
                    if parent_box and not _inside(box, parent_box, 16):
                        issues.append(LayoutIssue(
                            "warning", "outside-parent",
                            "clickable node extends past its parent bounds",
                            box, label,
                        ))
                    if width < min_touch or height < min_touch:
                        issues.append(LayoutIssue(
                            "warning", "small-touch",
                            f"touch target is {width}x{height}px, below {min_touch}px",
                            box, label,
                        ))
                clickable.append((node, box, label))
        for child in list(node):
            walk(child, node)

    walk(root, None)

    # Sibling overlap is only actionable for named action controls. Compose
    # intentionally overlays wrappers, text-field semantics, and minimum touch
    # targets with matching bounds, so an unlabeled overlap is noise.
    critical = re.compile(
        r"^(undo|redo|clear|cast|continue|next|back|show answer|good|easy|hard|again|"
        r"save|close|start|stop|done|check|grade|leave|resume|end)\b",
        re.IGNORECASE,
    )
    for index, (node_a, box_a, label_a) in enumerate(clickable):
        for node_b, box_b, label_b in clickable[index + 1:]:
            if not label_a or not label_b:
                continue
            if not (critical.match(label_a) or critical.match(label_b)):
                continue
            if "EditText" in (node_a.attrib.get("class") or "") or "EditText" in (node_b.attrib.get("class") or ""):
                continue
            if _inside(box_a, box_b, 0) or _inside(box_b, box_a, 0):
                continue
            overlap = _intersection(box_a, box_b)
            smaller = min(_area(box_a), _area(box_b))
            if smaller > 0 and overlap / smaller > 0.22 and overlap > 180:
                issues.append(LayoutIssue(
                    "error", "overlap",
                    f"clickable controls overlap by {overlap}px² "
                    f"({overlap / smaller:.0%} of the smaller)",
                    box_a, f"{label_a} <-> {label_b}",
                ))

    return issues


def summarize(issues: list[LayoutIssue]) -> dict:
    errors = [issue for issue in issues if issue.severity == "error"]
    warnings = [issue for issue in issues if issue.severity != "error"]
    return {
        "errors": len(errors),
        "warnings": len(warnings),
        "issues": [issue.as_dict() for issue in issues],
    }


def main() -> int:
    if len(sys.argv) < 4:
        print("usage: layout_audit.py hierarchy.xml width height [density]", file=sys.stderr)
        return 2
    xml = Path(sys.argv[1]).read_text()
    width, height = int(sys.argv[2]), int(sys.argv[3])
    density = int(sys.argv[4]) if len(sys.argv) > 4 else 420
    issues = audit_xml(xml, width, height, density)
    print(json.dumps(summarize(issues), indent=2))
    return 1 if any(issue.severity == "error" for issue in issues) else 0


if __name__ == "__main__":
    raise SystemExit(main())
