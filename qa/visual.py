#!/usr/bin/env python3
"""Local visual regression and contact sheets for the journey harness.

Goldens live in ``qa/goldens/<profile>/<scenario>/<shot>.png``. They are
generated from the real app on the emulator and can be reviewed and committed.
The status bar and gesture bar are masked because the clock and system icons
change between runs; everything below them is compared.
"""
from __future__ import annotations

import html
import re
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

QA = Path(__file__).resolve().parent
ROOT = QA.parent
GOLDENS = QA / "goldens"
# Store and compare at half resolution: enough to catch clipped controls and
# overflows, small enough to keep hundreds of baselines practical.
GOLDEN_SCALE = 0.5

# Screens with a modal or sheet have a live, sometimes slightly scrolled
# background behind them. Compare the surface itself, not the wallpaper.
CROPS: dict[str, tuple[float, float, float, float]] = {
    "screen-study-tools": (0.0, 0.26, 1.0, 1.0),
    "screen-stroke-rules": (0.0, 0.18, 1.0, 0.96),
}

SKIP_NAMES = re.compile(
    r"^(failed|failure-|final$|spell-effect-|spell-gift-draw|spell-cast$|screen-scene$|screen-grimoire$|screen-stroke-rules$|screen-cast$|screen-study-tools$|screen-study-lists$)"
)


@dataclass
class VisualResult:
    profile: str
    scenario: str
    name: str
    status: str
    golden: Path
    actual: Path
    diff: Path | None = None
    mismatch: int = 0
    total: int = 0
    fraction: float = 0.0
    detail: str = ""

    def as_dict(self) -> dict:
        return {
            "profile": self.profile,
            "scenario": self.scenario,
            "name": self.name,
            "status": self.status,
            "golden": str(self.golden),
            "actual": str(self.actual),
            "diff": str(self.diff) if self.diff else None,
            "mismatch": self.mismatch,
            "total": self.total,
            "fraction": self.fraction,
            "detail": self.detail,
        }


@dataclass
class VisualReport:
    results: list[VisualResult] = field(default_factory=list)

    @property
    def failures(self) -> list[VisualResult]:
        return [r for r in self.results if r.status in {"diff", "missing"}]


def _dynamic_masks(size: tuple[int, int], density_dpi: int) -> tuple[int, int]:
    """Masks for the status bar and gesture bar, relative to the image size."""
    width, height = size
    return min(int(height * 0.03) + 2, height // 3), min(int(height * 0.02) + 2, height // 4)


def _comparable(image: Image.Image, density_dpi: int) -> Image.Image:
    top, bottom = _dynamic_masks(image.size, density_dpi)
    width, height = image.size
    return image.crop((0, top, width, height - bottom)).convert("RGB")


def compare_image(
    profile: str,
    scenario: str,
    name: str,
    actual_path: Path,
    density_dpi: int,
    *,
    update: bool = False,
    require: bool = False,
    tolerance: int = 18,
    max_fraction: float = 0.003,
) -> VisualResult:
    golden_dir = GOLDENS / profile / scenario
    golden_path = golden_dir / f"{name}.png"
    actual = Image.open(actual_path).convert("RGB")

    if update or not golden_path.exists():
        if update:
            golden_dir.mkdir(parents=True, exist_ok=True)
            target = actual
            if GOLDEN_SCALE != 1.0:
                target = actual.resize(
                    (max(1, int(actual.width * GOLDEN_SCALE)), max(1, int(actual.height * GOLDEN_SCALE))),
                    Image.Resampling.LANCZOS,
                )
            target.save(golden_path, optimize=True)
            return VisualResult(profile, scenario, name, "new", golden_path, actual_path)
        return VisualResult(
            profile, scenario, name, "missing", golden_path, actual_path,
            detail="no golden; run with --update-goldens to create it",
        )

    expected = Image.open(golden_path).convert("RGB")
    if expected.size != actual.size:
        # Goldens are stored at GOLDEN_SCALE; compare the actual at the same
        # size so old full-size baselines keep working.
        actual = actual.resize(expected.size, Image.Resampling.LANCZOS)
    crop = CROPS.get(name)
    if crop is not None:
        width, height = expected.size
        box = (
            int(width * crop[0]), int(height * crop[1]),
            int(width * crop[2]), int(height * crop[3]),
        )
        expected = expected.crop(box)
        actual = actual.crop(box)

    left = _comparable(expected, density_dpi)
    right = _comparable(actual, density_dpi)
    diff = ImageChops.difference(left, right).convert("L")
    total = left.width * left.height
    # Count pixels above the per-channel tolerance after a small blur-like
    # thresholding. JPEG has no place here; both sides are lossless PNG.
    threshold = diff.point(lambda value: 255 if value > tolerance else 0)
    mismatch = sum(1 for value in threshold.getdata() if value)
    fraction = mismatch / total if total else 0.0

    if fraction <= max_fraction:
        return VisualResult(
            profile, scenario, name, "match", golden_path, actual_path,
            mismatch=mismatch, total=total, fraction=fraction,
        )

    diff_dir = actual_path.parent / "visual"
    diff_dir.mkdir(parents=True, exist_ok=True)
    expected_compare = _comparable(expected, density_dpi)
    actual_compare = _comparable(actual, density_dpi)
    diff_rgb = Image.new("RGB", left.size, (255, 253, 246))
    red = Image.new("RGB", left.size, (196, 64, 54))
    diff_rgb.paste(red, mask=threshold)
    compare = Image.new("RGB", (left.width * 3, left.height), (255, 253, 246))
    compare.paste(expected_compare, (0, 0))
    compare.paste(actual_compare, (left.width, 0))
    compare.paste(diff_rgb, (left.width * 2, 0))
    compare_path = diff_dir / f"{name}.compare.png"
    compare.save(compare_path, optimize=True)
    diff_path = diff_dir / f"{name}.diff.png"
    threshold.save(diff_path, optimize=True)

    status = "diff" if fraction > max_fraction else "match"
    return VisualResult(
        profile, scenario, name, status, golden_path, actual_path,
        diff=compare_path, mismatch=mismatch, total=total, fraction=fraction,
        detail=f"{mismatch}/{total} pixels ({fraction:.3%}) over tolerance; compare={compare_path}",
    )


def write_contact_sheet(run_dir: Path, limit: int = 120) -> Path | None:
    """Build a human-review contact sheet from one run's screenshots."""
    shots: list[tuple[str, Path]] = []
    for path in sorted(run_dir.glob("*/*/*.png")):
        rel = path.relative_to(run_dir)
        if len(rel.parts) < 3:
            continue
        if SKIP_NAMES.match(path.stem):
            continue
        if "visual" in rel.parts:
            continue
        shots.append(("/".join(rel.parts[:-1]), path))
        if len(shots) >= limit:
            break
    if not shots:
        return None

    thumb_w = 320
    thumb_h = 640
    columns = 3
    rows = (len(shots) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * (thumb_w + 18) + 18, rows * (thumb_h + 38) + 18), (255, 253, 246))
    draw = ImageDraw.Draw(sheet)
    for index, (group, path) in enumerate(shots):
        try:
            image = Image.open(path).convert("RGB")
        except Exception:
            continue
        image.thumbnail((thumb_w, thumb_h))
        x = 18 + (index % columns) * (thumb_w + 18)
        y = 18 + (index // columns) * (thumb_h + 38)
        sheet.paste(image, (x, y))
        draw.text((x, y + thumb_h + 6), f"{group} / {path.stem}", fill=(28, 59, 43))
    sheet_path = run_dir / "contact-sheet.png"
    sheet.save(sheet_path, optimize=True)

    rows_html = []
    for group, path in shots:
        rel = path.relative_to(run_dir)
        rows_html.append(
            "<figure><a href='{href}'><img src='{src}' loading='lazy'></a>"
            "<figcaption>{caption}</figcaption></figure>".format(
                href=html.escape(str(rel)),
                src=html.escape(str(rel)),
                caption=html.escape(f"{group} / {path.stem}"),
            )
        )
    page = f"""<!doctype html>
<html><head><meta charset="utf-8"><title>Bihua contact sheet</title>
<style>
body{{font-family:-apple-system,sans-serif;margin:24px;background:#fffdf6;color:#1c3b2b}}
h1{{margin:0 0 6px}}p{{color:#6f6863;margin:0 0 22px}}
.grid{{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}}
figure{{margin:0;background:#fff;border:1px solid #e3d8c8;border-radius:10px;padding:8px}}
img{{width:100%;display:block;border-radius:6px}}
figcaption{{font-size:12px;padding-top:6px;color:#6f6863;word-break:break-word}}
</style></head><body>
<h1>Visual review</h1>
<p>{len(shots)} screenshots from {html.escape(str(run_dir))}. Review the full contact sheet before accepting a release.</p>
<div class="grid">{''.join(rows_html)}</div>
</body></html>"""
    (run_dir / "contact-sheet.html").write_text(page)
    return sheet_path
