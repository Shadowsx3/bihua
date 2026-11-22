#!/usr/bin/env python3
"""Check the release artifact, docs, content version, and store copy agree.

Run after `assembleRelease` and `bundleRelease`, before uploading.
"""
from __future__ import annotations

import hashlib
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APK = ROOT / "app/build/outputs/apk/release/app-release.apk"
AAB = ROOT / "app/build/outputs/bundle/release/app-release.aab"
DOWNLOAD_SUMS = ROOT / "docs/download/SHA256SUMS.txt"
INDEX = ROOT / "docs/index.html"
GRADLE = ROOT / "app/build.gradle.kts"
REPOSITORY = ROOT / "app/src/main/java/com/bassmd/bihua/content/ContentRepository.kt"
CONTENT = ROOT / "app/src/main/assets/bihua_content.db"


errors: list[str] = []
warnings: list[str] = []


def fail(message: str) -> None:
    errors.append(message)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def main() -> int:
    gradle = GRADLE.read_text()
    code = re.search(r"versionCode\s*=\s*(\d+)", gradle)
    name = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
    if not code or not name:
        fail("could not read versionCode/versionName from app/build.gradle.kts")
        return report()
    version_code, version_name = int(code.group(1)), name.group(1)
    download_apk = ROOT / f"docs/download/bihua-{version_name}.apk"
    notes = ROOT / f"store/release-notes-{version_name}.txt"

    content_version = re.search(
        r"private val contentVersion = (\d+)",
        REPOSITORY.read_text(),
    )
    if not content_version:
        fail("could not read contentVersion from ContentRepository.kt")
        return report()
    content_version = int(content_version.group(1))

    db = sqlite3.connect(CONTENT)
    words = db.execute("SELECT COUNT(*) FROM word_context").fetchone()[0]
    structures = db.execute("SELECT COUNT(*) FROM char_meta").fetchone()[0]
    db.close()

    index = INDEX.read_text()
    index_version = re.search(r"content version (\d+)", index)
    index_hash = re.search(r"<code>([a-f0-9]{64})</code>", index)
    index_name = re.search(r"version (\d+\.\d+\.\d+) \((\d+)\)", index)
    if not index_version:
        fail("docs/index.html has no content version")
    elif int(index_version.group(1)) != content_version:
        fail(
            f"docs content version {index_version.group(1)} != code {content_version}"
        )
    if not index_name:
        fail("docs/index.html has no version line")
    elif (index_name.group(1), int(index_name.group(2))) != (version_name, version_code):
        fail(
            f"docs version {index_name.group(1)} ({index_name.group(2)}) "
            f"!= code {version_name} ({version_code})"
        )

    if APK.exists():
        apk_hash = sha256(APK)
        if not index_hash:
            fail("docs/index.html has no SHA-256 code block")
        elif index_hash.group(1) != apk_hash:
            fail("docs/index.html SHA-256 does not match the release APK")
        if DOWNLOAD_SUMS.exists():
            download_hash = DOWNLOAD_SUMS.read_text().split()[0]
            if download_hash != apk_hash:
                fail("docs/download/SHA256SUMS.txt does not match the release APK")
        else:
            fail("docs/download/SHA256SUMS.txt is missing")
        if download_apk.exists() and sha256(download_apk) != apk_hash:
            fail("docs/download APK does not match the release APK")
        newest = max(
            (path.stat().st_mtime for path in (ROOT / "app/src/main").rglob("*") if path.is_file()),
            default=0,
        )
        if newest > APK.stat().st_mtime:
            fail("app/src/main changed after the release APK was built")
        print(f"APK  {apk_hash}  {APK.stat().st_size / (1024 * 1024):.1f} MiB")
    else:
        warnings.append("release APK is missing; run assembleRelease")

    if AAB.exists():
        print(f"AAB  {sha256(AAB)}  {AAB.stat().st_size / (1024 * 1024):.1f} MiB")
    else:
        warnings.append("release AAB is missing; run bundleRelease")

    if notes.exists():
        text = notes.read_text()
        inner = text.split("<en-US>")[1].split("</en-US>")[0].strip()
        if len(inner) > 500:
            fail(f"release notes are {len(inner)} characters; Play limit is 500")
        print(f"Notes {len(inner)} characters")
    else:
        warnings.append("release notes file is missing")

    print(f"Version {version_name} ({version_code}) · content {content_version}")
    print(f"Data {words:,} words · {structures:,} structure rows")
    return report()


def report() -> int:
    for warning in warnings:
        print(f"WARN  {warning}")
    for error in errors:
        print(f"FAIL  {error}")
    if errors:
        print("release check failed")
        return 1
    print("release check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
