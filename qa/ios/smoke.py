#!/usr/bin/env python3
"""iOS simulator smoke: boot, install, launch, confirm the shared screen, shot."""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from driver import Simulator  # noqa: E402


def main() -> int:
    sim = Simulator()
    print(f"device: {sim.device} ({sim.udid})")
    sim.boot()
    sim.install()
    sim.terminate()
    sim.launch()
    sim.connect()

    # The P0 host shows "Bihua"; the real shell will show Home or Hello.
    node = sim.find("Bihua", timeout=20) or sim.find("Home", timeout=2) or sim.find("Hello", timeout=2)
    if node is None:
        print("no app label after launch", file=sys.stderr)
        sim.screenshot(".qa/ios/smoke-failed.png")
        return 1

    shot = sim.screenshot(".qa/ios/smoke.png")
    print(f"ok: {node.get('AXLabel')} · {shot}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
