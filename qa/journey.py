#!/usr/bin/env python3
"""One-command local journey runner."""
from __future__ import annotations

import argparse
import html
import json
import os
import subprocess
import sys
import time
import traceback
import xml.etree.ElementTree as ET
from collections import OrderedDict
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
QA = ROOT / ".qa"
sys.path.insert(0, str(Path(__file__).resolve().parent))

from devices import AvdLock, Device, boot  # noqa: E402
from driver import Journey, JourneyFailure  # noqa: E402
from scenarios import RUNNERS, SCENARIOS, ScenarioSpec, scenario_by_name  # noqa: E402
from state import ScenarioState, build_state, write_settings_files  # noqa: E402


@dataclass
class RunResult:
    profile: str
    scenario: str
    status: str
    seconds: float
    steps: list = field(default_factory=list)
    detail: str = ""
    artifact_dir: str = ""


def load_profiles() -> dict:
    return json.loads(Path(__file__).with_name("profiles.json").read_text())


def canonical(profile: str) -> bool:
    return profile in {"phone", "fold-inner", "tablet"}


def select_scenarios(preset: str, profile: str, group: str, explicit: list[str] | None) -> list[ScenarioSpec]:
    if explicit:
        return [scenario_by_name(name) for name in explicit]
    out = []
    for spec in SCENARIOS:
        if spec.group and spec.group != group:
            continue
        if preset == "quick":
            if spec.level == "quick":
                out.append(spec)
        elif preset == "standard":
            if profile == "phone":
                if spec.level in {"quick", "standard"}:
                    out.append(spec)
            elif spec.name in {"review-modes", "draw-recognition"}:
                out.append(spec)
            elif spec.group == "fold" and group == "fold":
                out.append(spec)
            elif spec.all_profiles and spec.level == "quick":
                out.append(spec)
        else:
            if canonical(profile):
                out.append(spec)
            elif spec.all_profiles:
                out.append(spec)
            elif spec.group == "fold" and group == "fold":
                out.append(spec)
    return out


def build_apk(no_build: bool) -> None:
    if no_build:
        return
    print("building debug APK")
    result = subprocess.run(
        [str(ROOT / "gradlew"), ":app:assembleDebug", "-q", "--console=plain"],
        cwd=ROOT,
        text=True,
        capture_output=True,
    )
    if result.returncode != 0:
        print(result.stdout[-3000:])
        print(result.stderr[-3000:])
        raise SystemExit("debug build failed")


def device_now(device: Device) -> tuple[int, str]:
    raw = device.shell("date +%s").strip().splitlines()
    seconds = int(raw[0]) if raw and raw[0].isdigit() else int(time.time())
    tz = device.shell("getprop persist.sys.timezone").strip() or "UTC"
    return seconds * 1000, tz


def run_scenario(
    device: Device,
    profile: str,
    profile_cfg: dict,
    spec: ScenarioSpec,
    args,
    run_dir: Path,
) -> RunResult:
    artifact = run_dir / profile / spec.name
    artifact.mkdir(parents=True, exist_ok=True)
    started = time.time()
    journey = Journey(device=device, artifact_dir=artifact, screenshots=args.screenshots)
    detail = ""
    status = "ok"
    try:
        device.clear_logcat()
        device.force_stop()
        device.shell("am force-stop com.google.android.documentsui", check=False)
        device.shell("input keyevent 3", check=False)
        device.shell(
            "pm grant com.bassmd.bihua android.permission.POST_NOTIFICATIONS",
            check=False,
        )
        state: ScenarioState | None = None
        if spec.runner == "fresh":
            device.clear_app()
        else:
            now, tz = device_now(device)
            kwargs = dict(spec.state)
            if args.reviews is not None:
                kwargs["reviews"] = args.reviews
            if args.new is not None:
                kwargs["new"] = args.new
            state = build_state(f"{profile}-{spec.name}", now, tz, **kwargs)
            settings_path, profile_path = write_settings_files(state)
            device.push_db(state.db_path)
            device.apply_settings(settings_path, profile_path)
        journey.launch()
        RUNNERS[spec.runner](journey, state)  # type: ignore[arg-type]
        journey.assert_no_crash()
        if args.screenshots:
            journey.screenshot("final")
    except Exception as error:
        status = "failed"
        detail = str(error)
        try:
            journey.assert_no_crash()
        except Exception as crash:
            detail += f"\n{crash}"
        artifact.joinpath("error.txt").write_text(traceback.format_exc())
        journey.screenshot("failed")
        journey.dump_ui("failed")
        journey.logcat()
        print(f"      FAILED: {detail.splitlines()[0] if detail else error}")
    seconds = time.time() - started
    result = RunResult(profile, spec.name, status, seconds, journey.steps, detail, str(artifact))
    artifact.joinpath("result.json").write_text(json.dumps({
        "profile": profile,
        "scenario": spec.name,
        "status": status,
        "seconds": seconds,
        "detail": detail,
    }, indent=2))
    return result


def write_report(run_dir: Path, results: list[RunResult]) -> None:
    failures = [r for r in results if r.status != "ok"]
    junit = ET.Element("testsuite", {
        "name": "bihua-journeys",
        "tests": str(len(results)),
        "failures": str(len(failures)),
        "time": f"{sum(r.seconds for r in results):.1f}",
    })
    for result in results:
        case = ET.SubElement(junit, "testcase", {
            "classname": f"bihua.{result.profile}",
            "name": result.scenario,
            "time": f"{result.seconds:.1f}",
        })
        if result.status != "ok":
            failure = ET.SubElement(case, "failure", {"message": result.detail[:1000]})
            failure.text = result.detail
    ET.ElementTree(junit).write(run_dir / "junit.xml", encoding="utf-8", xml_declaration=True)

    rows = []
    for result in results:
        color = "#e6f4ea" if result.status == "ok" else "#fde8e8"
        steps = "<br>".join(
            html.escape(f"{step.name} ({step.seconds:.1f}s)")
            for step in result.steps
        )
        rows.append(
            f"<tr style='background:{color}'><td>{html.escape(result.profile)}</td>"
            f"<td>{html.escape(result.scenario)}</td><td>{result.status}</td>"
            f"<td>{result.seconds:.1f}s</td><td>{steps}</td>"
            f"<td><code>{html.escape(result.artifact_dir)}</code></td></tr>"
        )
    page = f"""<!doctype html>
<html><head><meta charset="utf-8"><title>Bihua journeys</title>
<style>body{{font-family:-apple-system,sans-serif;margin:24px;color:#222}}table{{border-collapse:collapse;width:100%}}th,td{{border:1px solid #ddd;padding:6px;vertical-align:top;text-align:left}}th{{background:#f5f5f5}}</style>
</head><body><h1>Bihua journeys</h1>
<p>{len(results)} runs, {len(failures)} failed. {datetime.now().isoformat(timespec="seconds")}</p>
<table><thead><tr><th>Profile</th><th>Scenario</th><th>Status</th><th>Time</th><th>Steps</th><th>Artifacts</th></tr></thead>
<tbody>{''.join(rows)}</tbody></table></body></html>"""
    (run_dir / "index.html").write_text(page)


def main() -> int:
    profiles = load_profiles()
    parser = argparse.ArgumentParser(description="Run Bihua daily journeys locally.")
    preset = parser.add_mutually_exclusive_group()
    preset.add_argument("--quick", action="store_const", const="quick", dest="preset")
    preset.add_argument("--standard", action="store_const", const="standard", dest="preset")
    preset.add_argument("--full", action="store_const", const="full", dest="preset")
    parser.set_defaults(preset="quick")
    parser.add_argument("--profile", help="comma-separated profile names")
    parser.add_argument("--scenario", help="comma-separated scenario names")
    parser.add_argument("--reviews", type=int)
    parser.add_argument("--new", type=int)
    parser.add_argument("--drain", action="store_true", help="drain all reviews for selected daily scenarios")
    parser.add_argument("--font-scale", type=float, default=1.0)
    parser.add_argument("--headed", action="store_true", help="show emulator windows")
    parser.add_argument("--keep-emulators", action="store_true", help="keep AVDs alive after the run")
    parser.add_argument("--shut-down", action="store_true", help="kill AVDs started by this run")
    parser.add_argument("--no-build", action="store_true")
    parser.add_argument("--screenshots", action="store_true")
    parser.add_argument("--fail-fast", action="store_true")
    parser.add_argument("--parallel", type=int, help="AVDs to run at once (default 1 for quick, 2 otherwise)")
    parser.add_argument("--list", action="store_true")
    args = parser.parse_args()

    if args.list:
        for spec in SCENARIOS:
            print(f"{spec.level:8} {spec.name:18} {spec.runner}")
        return 0

    if args.profile:
        selected_profiles = [p.strip() for p in args.profile.split(",") if p.strip()]
    elif args.preset == "quick":
        selected_profiles = ["phone"]
    elif args.preset == "standard":
        selected_profiles = ["phone", "fold-inner", "fold-outer", "tablet"]
    else:
        selected_profiles = list(profiles)

    unknown = [p for p in selected_profiles if p not in profiles]
    if unknown:
        raise SystemExit(f"unknown profile(s): {', '.join(unknown)}")

    explicit = [s.strip() for s in args.scenario.split(",")] if args.scenario else None
    build_apk(args.no_build)

    stamp = datetime.now().strftime("%Y%m%d-%H%M%S") + f"-{os.getpid()}"
    run_dir = QA / "runs" / stamp
    run_dir.mkdir(parents=True, exist_ok=True)
    groups: OrderedDict[str, list[str]] = OrderedDict()
    for profile in selected_profiles:
        avd = profiles[profile]["avd"]
        groups.setdefault(avd, []).append(profile)

    def run_group(avd: str, group_profiles: list[str]) -> list[RunResult]:
        lock = AvdLock(avd)
        lock.acquire()
        device = None
        out: list[RunResult] = []
        try:
            first = group_profiles[0]
            first_cfg = dict(profiles[first])
            first_cfg["name"] = first
            keep = args.keep_emulators or not args.shut_down
            device = boot(first_cfg, headless=not args.headed, keep=keep)
            device.configure(first_cfg, headless=not args.headed)
            device.install()
            for profile in group_profiles:
                cfg = dict(profiles[profile])
                cfg["name"] = profile
                specs = select_scenarios(args.preset, profile, cfg.get("group", ""), explicit)
                if args.drain:
                    for spec in specs:
                        if spec.runner in {"daily_sample", "completion"}:
                            spec.runner = "drain"
                            spec.state.update({
                                "pronunciation": True,
                                "task_mode": "PRONOUNCE",
                                "review_order": "OLDEST_FIRST",
                            })
                if not specs:
                    continue
                print(f"\n== {profile} ({cfg['avd']}) ==", flush=True)
                device.configure(cfg, headless=not args.headed)
                device.shell(
                    "pm grant com.bassmd.bihua android.permission.POST_NOTIFICATIONS",
                    check=False,
                )
                # Always set the scale. Leaving a previous run's 1.5x on the AVD
                # makes a following phone run start in the wrong layout.
                device.font_scale(args.font_scale)
                for spec in specs:
                    print(f"  {spec.name}", flush=True)
                    result = run_scenario(device, profile, cfg, spec, args, run_dir)
                    out.append(result)
                    if result.status != "ok" and args.fail_fast:
                        raise SystemExit(1)
            return out
        finally:
            if device is not None:
                device.close()
            lock.release()

    workers = args.parallel or (1 if args.preset == "quick" else 2)
    workers = max(1, min(workers, len(groups)))
    results: list[RunResult] = []
    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = {
            pool.submit(run_group, avd, profs): avd
            for avd, profs in groups.items()
        }
        for future in as_completed(futures):
            avd = futures[future]
            try:
                results.extend(future.result())
            except Exception as error:
                print(f"  GROUP FAILED {avd}: {error}")
                results.append(RunResult(
                    profile=avd,
                    scenario="group-setup",
                    status="failed",
                    seconds=0,
                    detail=str(error),
                    artifact_dir=str(run_dir),
                ))

    write_report(run_dir, results)
    failed = [r for r in results if r.status != "ok"]
    print(f"\n{len(results)} runs, {len(failed)} failed")
    print(f"report: {run_dir / 'index.html'}")
    for result in failed:
        print(f"  FAIL {result.profile}/{result.scenario}: {result.detail.splitlines()[0][:160]}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
