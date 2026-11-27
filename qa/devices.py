#!/usr/bin/env python3
"""AVD lifecycle and device configuration for the local journey harness."""
from __future__ import annotations

import base64
import fcntl
import json
import os
import shutil
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
QA = ROOT / ".qa"
APP_ID = "com.bassmd.bihua"
DEBUG_APK = ROOT / "composeApp/build/outputs/apk/debug/composeApp-debug.apk"
ANDROID_HOME = Path(os.environ.get("ANDROID_HOME", str(Path.home() / "Library/Android/sdk")))


def adb(*args: str, serial: str | None = None, check: bool = True) -> subprocess.CompletedProcess:
    cmd = ["adb"]
    if serial:
        cmd += ["-s", serial]
    cmd += list(args)
    return subprocess.run(cmd, text=True, capture_output=True, check=check)


def emulator_path() -> Path:
    return ANDROID_HOME / "emulator/emulator"


def adb_devices() -> list[str]:
    out = adb("devices").stdout.splitlines()[1:]
    return [line.split()[0] for line in out if line.strip().endswith("device")]


def avd_name(serial: str) -> str | None:
    try:
        out = adb("emu", "avd", "name", serial=serial, check=False).stdout
        for line in out.splitlines():
            line = line.strip()
            if line and line != "OK":
                return line
    except Exception:
        pass
    return None


def running_avd(name: str) -> str | None:
    for serial in adb_devices():
        if avd_name(serial) == name:
            return serial
    return None


class AvdLock:
    """One harness per AVD. Two runs must not reset the same emulator."""

    def __init__(self, avd: str):
        self.path = QA / "locks" / f"{avd}.lock"
        self.handle = None

    def acquire(self) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.handle = self.path.open("w")
        try:
            fcntl.flock(self.handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            self.handle.close()
            self.handle = None
            raise SystemExit(
                f"another journey is already using AVD {self.path.stem}; "
                "wait for it or use a different profile"
            )

    def release(self) -> None:
        if self.handle is not None:
            fcntl.flock(self.handle.fileno(), fcntl.LOCK_UN)
            self.handle.close()
            self.handle = None


def free_port() -> int:
    used = set(adb_devices())
    for port in range(5554, 5700, 2):
        if f"emulator-{port}" not in used:
            return port
    raise SystemExit("no free emulator port")


@dataclass
class Device:
    profile: str
    avd: str
    serial: str
    booted_by_harness: bool = False
    keep_open: bool = False

    def shell(self, command: str, check: bool = True) -> str:
        result = adb("shell", command, serial=self.serial, check=check)
        return result.stdout

    def run_as(self, command: str, check: bool = False) -> subprocess.CompletedProcess:
        return adb("shell", f"run-as {APP_ID} {command}", serial=self.serial, check=check)

    def push_db(self, db_path: Path | None) -> None:
        if db_path is None:
            return
        remote = "/data/local/tmp/bihua-qa.db"
        adb("push", str(db_path), remote, serial=self.serial)
        self.shell(f"run-as {APP_ID} mkdir -p databases")
        self.shell(
            f"run-as {APP_ID} sh -c 'cat {remote} > databases/bihua_progress.db'"
        )
        self.shell(
            f"run-as {APP_ID} rm -f databases/bihua_progress.db-journal "
            "databases/bihua_progress.db-wal databases/bihua_progress.db-shm"
        )
        self.shell(f"rm -f {remote}")

    def force_stop(self) -> None:
        adb("shell", f"am force-stop {APP_ID}", serial=self.serial)

    def clear_app(self) -> None:
        adb("shell", f"am force-stop {APP_ID}", serial=self.serial)
        adb("shell", f"pm clear {APP_ID}", serial=self.serial)

    def apply_settings(self, settings_path: Path, profile_path: Path | None, clear_session: bool = True) -> None:
        settings_b64 = base64.b64encode(settings_path.read_bytes()).decode()
        profile_b64 = base64.b64encode(profile_path.read_bytes()).decode() if profile_path else ""
        self.shell(f"run-as {APP_ID} rm -f files/qa/applied.txt files/qa/failed.txt", check=False)
        args = [
            "shell", "am", "broadcast", "-a", "com.bassmd.bihua.qa.APPLY",
            "-n", f"{APP_ID}/.qa.QaBridgeReceiver",
            "--es", "settings_b64", settings_b64,
        ]
        if profile_b64:
            args += ["--es", "profile_b64", profile_b64]
        if clear_session:
            args += ["--ez", "clear_session", "true"]
        result = adb(*args, serial=self.serial, check=False)
        if "Broadcast completed" not in (result.stdout + result.stderr) and result.returncode != 0:
            raise RuntimeError(f"QA settings broadcast failed: {result.stdout} {result.stderr}")
        marker = f"files/qa/{'applied.txt'}"
        for _ in range(40):
            if self.run_as(f"cat {marker}").returncode == 0:
                self.run_as(f"rm -f {marker}")
                return
            time.sleep(0.1)
        failed = self.run_as("cat files/qa/failed.txt")
        message = failed.stdout.strip() if failed.returncode == 0 else ""
        raise RuntimeError(f"QA settings bridge did not confirm the write: {message or 'no marker'}")

    def install(self) -> None:
        if not DEBUG_APK.is_file():
            raise SystemExit(f"missing debug APK: {DEBUG_APK}")
        result = adb("install", "-r", "-t", str(DEBUG_APK), serial=self.serial, check=False)
        if result.returncode != 0 and "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in (result.stdout + result.stderr):
            adb("uninstall", APP_ID, serial=self.serial, check=False)
            result = adb("install", "-t", str(DEBUG_APK), serial=self.serial)
        if result.returncode != 0:
            raise RuntimeError(f"could not install debug APK: {result.stdout} {result.stderr}")

    def display_rotation(self) -> int:
        """0/1/2/3 for ROTATION_0/90/180/270."""
        out = self.shell(
            "dumpsys window displays | grep mCurrentRotation | head -1",
            check=False,
        )
        for step in (3, 2, 1, 0):
            if f"ROTATION_{(step * 90) % 360}" in out:
                return step
        return 0

    def ensure_rotation(self, landscape: bool) -> None:
        # `wm size` alone leaves some emulators in the app's natural
        # orientation. Rotate the emulator with auto-rotation on, then lock the
        # result so the launched app cannot fall back to portrait.
        for _ in range(3):
            if (self.display_rotation() % 2 == 1) == landscape:
                break
            self.shell("settings put system accelerometer_rotation 1")
            adb("emu", "rotate", serial=self.serial, check=False)
            time.sleep(0.9)
        self.shell("settings put system accelerometer_rotation 0")
        self.shell(f"settings put system user_rotation {1 if landscape else 0}")

    def configure(self, profile: dict, headless: bool) -> None:
        self.shell("settings put global window_animation_scale 0")
        self.shell("settings put global transition_animation_scale 0")
        self.shell("settings put global animator_duration_scale 0")
        self.shell("settings put global stay_on_while_plugged_in 3")
        self.shell("svc power stayon true", check=False)
        self.shell("input keyevent 82", check=False)
        self.shell("wm dismiss-keyguard", check=False)
        posture = profile.get("posture")
        if posture:
            adb("emu", posture, serial=self.serial, check=False)
            time.sleep(1.0)
        width, height = profile.get("width"), profile.get("height")
        density = profile.get("density")
        rotation = profile.get("rotation")
        group = profile.get("group", "")
        if rotation is not None and group in {"phone", "fold"}:
            # Natural size plus a real device rotation. A `wm size` swap
            # alone can keep the app in its natural orientation.
            self.shell("wm size reset", check=False)
            if density:
                self.shell(f"wm density {density}")
            self.shell("settings put system accelerometer_rotation 1")
            self.ensure_rotation(rotation % 2 == 1)
        else:
            if width and height:
                self.shell(f"wm size {width}x{height}")
            if density:
                self.shell(f"wm density {density}")
            if group in {"phone", "fold"}:
                # Portrait profiles must not inherit the previous run's
                # landscape rotation.
                self.shell("settings put system accelerometer_rotation 1")
                self.ensure_rotation(False)
            else:
                # Tablets are naturally wide; the size override defines the
                # orientation. Keep their physical rotation at 0.
                self.shell("settings put system accelerometer_rotation 0")
                self.shell("settings put system user_rotation 0")
        self.shell("settings put system font_scale 1.0")

    def fold(self, folded: bool) -> None:
        adb("emu", "fold" if folded else "unfold", serial=self.serial, check=False)
        time.sleep(1.0)

    def font_scale(self, scale: float) -> None:
        self.shell(f"settings put system font_scale {scale}")
        self.shell(f"am force-stop {APP_ID}")

    def reset_screen(self) -> None:
        self.shell("wm size reset", check=False)
        self.shell("wm density reset", check=False)
        self.shell("settings put system accelerometer_rotation 0", check=False)
        self.shell("settings put system user_rotation 0", check=False)
        self.ensure_rotation(False)
        self.shell("settings put system font_scale 1.0", check=False)

    def clear_logcat(self) -> None:
        adb("logcat", "-c", serial=self.serial, check=False)

    def logcat(self, lines: int = 3000) -> str:
        result = adb("logcat", "-d", "-v", "time", "-t", str(lines), serial=self.serial, check=False)
        return result.stdout

    def close(self) -> None:
        self.reset_screen()
        if "fold" in self.avd:
            adb("emu", "unfold", serial=self.serial, check=False)
        if self.booted_by_harness and not self.keep_open:
            adb("emu", "kill", serial=self.serial, check=False)


def boot(profile: dict, headless: bool = True, keep: bool = False) -> Device:
    name = profile["avd"]
    serial = running_avd(name)
    booted = False
    if serial is None:
        port = free_port()
        serial = f"emulator-{port}"
        QA.joinpath("logs").mkdir(parents=True, exist_ok=True)
        log = QA / "logs" / f"emulator-{name}-{port}.log"
        handle = log.open("w")
        cmd = [
            str(emulator_path()), "-avd", name, "-port", str(port),
            "-no-audio", "-no-snapshot-save", "-no-boot-anim",
            # The emulator's default resolver can go stale; Play Services then
            # cannot fetch the handwriting model even though raw IPs work.
            "-dns-server", "8.8.8.8",
        ]
        if headless:
            cmd.append("-no-window")
        subprocess.Popen(cmd, stdout=handle, stderr=subprocess.STDOUT, start_new_session=True)
        adb("wait-for-device", serial=serial, check=False)
        booted = True
        deadline = time.time() + 180
        while time.time() < deadline:
            out = adb("shell", "getprop sys.boot_completed", serial=serial, check=False).stdout.strip()
            if out == "1":
                break
            time.sleep(1)
        else:
            raise RuntimeError(f"emulator {name} did not boot; see {log}")
    return Device(
        profile=profile.get("name", name),
        avd=name,
        serial=serial,
        booted_by_harness=booted,
        keep_open=keep,
    )
