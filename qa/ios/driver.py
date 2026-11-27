"""Small iOS driver for the simulator, mirroring the Android Journey surface.

It shells out to `idb` for the accessibility tree and input and to `xcrun
simctl` for install, launch, appearance, and screenshots. No third-party Python
package is needed. Start with `Simulator.boot/install/launch`, then use the same
verbs as `qa/driver.py`: find, tap_label, text, screenshot.

Known limit: `idb` cannot rotate the device or open the software keyboard
permission sheet. Those stay XCUITest jobs.
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BUNDLE_ID = "com.bassmd.bihua"
APP = ROOT / "iosApp/DerivedData/Build/Products/Debug-iphonesimulator/Bihua.app"
DEVICE = os.environ.get("BIHUA_IOS_DEVICE", "iPhone 17 Pro")


def _idb_binary() -> str:
    explicit = os.environ.get("IDB")
    if explicit:
        return explicit
    found = shutil.which("idb")
    if found:
        return found
    venv = ROOT / ".qa/venv/bin/idb"
    if venv.exists():
        return str(venv)
    raise RuntimeError("idb is not installed. brew install facebook/fb/idb-companion and pip install fb-idb")


class Simulator:
    def __init__(self, device: str = DEVICE, bundle_id: str = BUNDLE_ID, app: Path = APP):
        self.device = device
        self.bundle_id = bundle_id
        self.app = app
        self.idb = _idb_binary()

    # ---------- simctl ----------
    def _simctl(self, *args: str, check: bool = True) -> str:
        result = subprocess.run(
            ["xcrun", "simctl", *args], capture_output=True, text=True
        )
        if check and result.returncode != 0:
            raise RuntimeError(result.stderr.strip() or f"simctl {' '.join(args)} failed")
        return result.stdout

    @property
    def udid(self) -> str:
        data = json.loads(self._simctl("list", "-j", "devices", "available"))
        for devices in data["devices"].values():
            for device in devices:
                if device["name"] == self.device and device["isAvailable"]:
                    return device["udid"]
        raise RuntimeError(f"no available simulator named {self.device}")

    def boot(self) -> None:
        self._simctl("boot", self.device, check=False)
        subprocess.run(
            ["open", "-a", "Simulator", "--args", "-CurrentDeviceUDID", self.udid],
            capture_output=True,
        )
        self._simctl("bootstatus", self.udid, "-b")

    def install(self) -> None:
        self._simctl("install", self.udid, str(self.app))

    def launch(self) -> None:
        self._simctl("launch", self.udid, self.bundle_id)

    def terminate(self) -> None:
        self._simctl("terminate", self.udid, self.bundle_id, check=False)

    def appearance(self, mode: str) -> None:
        self._simctl("ui", self.udid, "appearance", mode)

    def screenshot(self, path: str | Path) -> Path:
        target = Path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        self._simctl("io", self.udid, "screenshot", str(target))
        return target

    def reset(self) -> None:
        self._simctl("uninstall", self.udid, self.bundle_id, check=False)

    # ---------- idb ----------
    def connect(self) -> None:
        subprocess.run(
            [self.idb, "connect", self.udid], capture_output=True, text=True
        )

    def _idb(self, *args: str) -> str:
        result = subprocess.run(
            [self.idb, *args, "--udid", self.udid], capture_output=True, text=True
        )
        if result.returncode != 0:
            raise RuntimeError(result.stderr.strip() or f"idb {' '.join(args)} failed")
        return result.stdout

    def describe(self) -> list[dict]:
        raw = self._idb("ui", "describe-all")
        start = raw.find("[")
        if start < 0:
            return []
        try:
            return json.loads(raw[start:])
        except json.JSONDecodeError:
            return []

    def find(self, label: str, timeout: float = 8.0, contains: bool = True) -> dict | None:
        deadline = time.time() + timeout
        while time.time() < deadline:
            for node in self.describe():
                value = node.get("AXLabel") or node.get("AXValue") or ""
                if not value:
                    continue
                if (label in value) if contains else (value == label):
                    return node
            time.sleep(0.3)
        return None

    def tap(self, x: float, y: float) -> None:
        self._idb("ui", "tap", str(int(x)), str(int(y)))

    def tap_label(self, label: str, timeout: float = 8.0, contains: bool = True) -> None:
        node = self.find(label, timeout=timeout, contains=contains)
        if node is None:
            raise RuntimeError(f"label not found: {label}")
        frame = node.get("frame") or {}
        self.tap(
            frame.get("x", 0) + frame.get("width", 0) / 2,
            frame.get("y", 0) + frame.get("height", 0) / 2,
        )

    def text(self, value: str) -> None:
        self._idb("ui", "text", value)

    def swipe(self, x1: float, y1: float, x2: float, y2: float, duration: float = 0.4) -> None:
        self._idb(
            "ui", "swipe", str(int(x1)), str(int(y1)), str(int(x2)), str(int(y2)),
            "--duration", str(duration),
        )

    def wait(self, label: str, timeout: float = 15.0, contains: bool = True) -> dict:
        node = self.find(label, timeout=timeout, contains=contains)
        if node is None:
            raise RuntimeError(f"timed out waiting for {label}")
        return node
