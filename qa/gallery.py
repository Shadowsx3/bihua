#!/usr/bin/env python3
"""Build a readable screen gallery for one or more QA flows.

The journey harness already saves every capture under `.qa/runs/<run>/<profile>/
<scenario>/`. This tool finds the latest run for each profile and lays every
screen from that flow on one sheet per device, so a design review does not mean
opening a dozen folders.

Examples:
    scripts/flow-gallery.sh progress-screen
    scripts/flow-gallery.sh progress-screen --run
    scripts/flow-gallery.sh word-whole-canvas,grade-reasons
    scripts/flow-gallery.sh screen-pass --profiles phone,tablet

Output:
    qa/gallery/<scenario>/phone.png
    qa/gallery/<scenario>/tablet.png
    qa/gallery/<scenario>/fold-inner.png
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RUNS = ROOT / ".qa" / "runs"
OUT = ROOT / "qa" / "gallery"

PROFILES = {
    "phone": {"label": "Phone", "accent": (47, 143, 99), "height": 420, "columns": 6},
    "tablet": {"label": "Tablet", "accent": (43, 127, 166), "height": 360, "columns": 3},
    "fold-inner": {"label": "Fold", "accent": (198, 90, 50), "height": 380, "columns": 4},
}

DEVICE_FOLDER = {
    "phone": "phone",
    "tablet": "tablet",
    "fold-inner": "fold",
    "fold-outer": "fold-outer",
}
SKIP = {"final.png", "contact-sheet.png", "failed.png"}
FONT_CANDIDATES = [
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/System/Library/Fonts/Supplemental/Helvetica.ttf",
    "/Library/Fonts/Arial.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
]


def font(size: int, bold: bool = False):
    name = "Arial Bold.ttf" if bold else "Arial.ttf"
    candidates = [str(Path(path).with_name(name)) for path in FONT_CANDIDATES]
    candidates += FONT_CANDIDATES
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def latest_run(profile: str, scenario: str) -> Path | None:
    runs = list(RUNS.glob(f"*/{profile}/{scenario}"))
    runs = [run for run in runs if run.is_dir()]
    return max(runs, key=lambda path: path.stat().st_mtime) if runs else None


def captures(run: Path) -> list[Path]:
    found = [
        path for path in run.glob("*.png")
        if path.name not in SKIP and not path.name.startswith("failure")
    ]
    return sorted(found, key=lambda path: natural_key(path.name))


def natural_key(text: str):
    return [int(part) if part.isdigit() else part.lower() for part in re.split(r"(\d+)", text)]


def label_for(path: Path) -> str:
    name = path.stem.replace("screen-", "").replace("-", " ")
    if name == "character detail":
        return "Character detail"
    return name[:1].upper() + name[1:]


def sheet(scenario: str, profile: str, run: Path, images: list[Path], out: Path) -> Image.Image | None:
    meta = PROFILES[profile]
    if not images:
        return None
    thumb_h = meta["height"]
    columns = meta["columns"]
    margin = 28
    gap = 12
    label_h = 26
    cell_w = 0
    prepared: list[tuple[Image.Image, str, int, int]] = []
    for path in images:
        image = Image.open(path).convert("RGB")
        scale = thumb_h / image.height
        width = max(1, int(image.width * scale))
        image = image.resize((width, thumb_h), Image.Resampling.LANCZOS)
        cell_w = max(cell_w, width)
        prepared.append((image, label_for(path), image.width, image.height))
    cell_w = max(cell_w, 170)
    rows = (len(prepared) + columns - 1) // columns
    row_h = thumb_h + label_h
    width = margin * 2 + columns * cell_w + (columns - 1) * gap
    header_h = 108
    height = header_h + rows * row_h + (rows - 1) * gap + margin
    canvas = Image.new("RGB", (width, height), (247, 242, 230))
    draw = ImageDraw.Draw(canvas)

    accent = meta["accent"]
    draw.rectangle([0, 0, width, 8], fill=accent)
    title_font = font(30, bold=True)
    sub_font = font(15)
    label_font = font(14, bold=True)
    foot_font = font(11)
    name = scenario.replace("-", " ")
    draw.text((margin, 22), f"Bihua 1.1.2 · {name}", font=title_font, fill=(28, 43, 32))
    draw.text(
        (margin, 58),
        f"{meta['label']} · {len(prepared)} captures · latest {scenario} run",
        font=sub_font,
        fill=(95, 108, 96),
    )
    draw.text(
        (width - margin - draw.textlength(time.strftime("%Y-%m-%d %H:%M"), font=foot_font), 64),
        time.strftime("%Y-%m-%d %H:%M"),
        font=foot_font,
        fill=(120, 128, 118),
    )

    for index, (image, label, image_w, image_h) in enumerate(prepared):
        column = index % columns
        row = index // columns
        cell_x = margin + column * (cell_w + gap)
        cell_y = header_h + row * (row_h + gap)
        frame = [cell_x, cell_y, cell_x + cell_w, cell_y + thumb_h]
        draw.rounded_rectangle(frame, radius=10, fill=(255, 252, 244), outline=(226, 217, 204), width=1)
        x = cell_x + (cell_w - image_w) // 2
        y = cell_y + (thumb_h - image_h) // 2
        canvas.paste(image, (x, y))
        text = label
        while draw.textlength(text, font=label_font) > cell_w + 4 and len(text) > 4:
            text = text[:-1]
        if text != label:
            text = text[:-1] + "…"
        draw.text((cell_x, cell_y + thumb_h + 5), text, font=label_font, fill=(40, 55, 43))

    draw.text(
        (margin, height - 18),
        "Evaluation sheet · captures from the latest QA run · not a store asset",
        font=foot_font,
        fill=(130, 120, 108),
    )
    out.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(out, "PNG", optimize=True)
    return canvas


def run_flow(profile: str, scenario: str, build: bool) -> None:
    cmd = [
        "scripts/journey.sh", "--scenario", scenario, "--profile", profile,
        "--screenshots", "--layout-audit",
    ]
    if not build:
        cmd.append("--no-build")
    print("  running", " ".join(cmd), flush=True)
    subprocess.run(cmd, cwd=ROOT, check=False)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("scenario", help="comma-separated scenario name(s)")
    parser.add_argument("--profiles", default="phone,tablet,fold-inner")
    parser.add_argument("--run", action="store_true", help="run the flow before collecting")
    parser.add_argument("--build", action="store_true", help="build the debug APK when running")
    parser.add_argument("--out", type=Path, default=OUT)
    parser.add_argument("--name", default="", help="run folder name; defaults to a timestamp")
    args = parser.parse_args()
    profiles = [p.strip() for p in args.profiles.split(",") if p.strip()]
    unknown = [p for p in profiles if p not in PROFILES]
    if unknown:
        parser.error(f"unknown profile(s): {', '.join(unknown)}")
    scenarios = [s.strip() for s in args.scenario.split(",") if s.strip()]
    if not scenarios:
        parser.error("give at least one scenario")
    run_name = (args.name or time.strftime("%Y%m%d-%H%M%S")).strip().replace("/", "-")
    gallery_root = args.out / run_name
    rows: list[tuple[str, str, Path, int]] = []
    built = False
    for scenario in scenarios:
        print(f"\n{scenario}", flush=True)
        for profile in profiles:
            if args.run:
                run_flow(profile, scenario, build=args.build and not built)
                built = built or args.build
            run = latest_run(profile, scenario)
            if run is None:
                print(f"  {profile}: no run found", file=sys.stderr)
                continue
            images = captures(run)
            if not images:
                print(f"  {profile}: no captures in {run.relative_to(ROOT)}", file=sys.stderr)
                continue
            device = DEVICE_FOLDER.get(profile, profile)
            out = gallery_root / device / f"{scenario}.png"
            result = sheet(scenario, profile, run, images, out)
            if result is not None:
                rows.append((device, scenario, out, len(images)))
                print(f"  {profile}: {len(images)} captures -> {out.relative_to(ROOT)} ({result.width}×{result.height})")
    if rows:
        lines = [
            f"# Flow gallery · {run_name}",
            "",
            f"Generated {time.strftime('%Y-%m-%d %H:%M')} from the latest QA runs.",
            "",
        ]
        for device in ["phone", "tablet", "fold", "fold-outer"]:
            group = [row for row in rows if row[0] == device]
            if not group:
                continue
            lines.append(f"## {device.title()}")
            lines.append("")
            for _, scenario, path, count in group:
                lines.append(f"- [{scenario}]({device}/{scenario}.png) · {count} screens")
            lines.append("")
        (gallery_root / "INDEX.md").write_text("\n".join(lines))
        print(f"gallery: {gallery_root.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
