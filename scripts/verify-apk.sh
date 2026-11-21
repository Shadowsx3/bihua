#!/usr/bin/env bash
# Checks a built APK for the things we rely on, without installing it.
#   scripts/verify-apk.sh [path/to/app.apk]
#
# Fails if a required class is missing, the package or SDK levels are wrong,
# or the APK is suspiciously large. Each new feature adds its entry-point
# class to REQUIRED_CLASSES so a silently-skipped source file is caught.

set -euo pipefail
source "$(dirname "$0")/env.sh"

APK="${1:-$APK_DEBUG}"
APKANALYZER="$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer"

if [ ! -f "$APK" ]; then
  echo "verify-apk: no APK at $APK. Run scripts/check.sh first." >&2
  exit 1
fi

REQUIRED_CLASSES=(
  "com.bassmd.bihua.MainActivity"
  "com.bassmd.bihua.ui.BihuaAppKt"
  "com.bassmd.bihua.ui.HomeScreenKt"
  "com.bassmd.bihua.ui.CharactersScreenKt"
  "com.bassmd.bihua.ui.DashboardScreenKt"
  "com.bassmd.bihua.learn.LearnScreenKt"
  "com.bassmd.bihua.practice.PracticeScreenKt"
  "com.bassmd.bihua.settings.SettingsScreenKt"
  "com.bassmd.bihua.content.ContentRepository"
  "com.bassmd.bihua.content.SqliteContentRepository"
  "com.bassmd.bihua.content.CharacterEntry"
  "com.bassmd.bihua.ai.InkRecognitionService"
  "com.bassmd.bihua.backup.BackupCodec"
  "com.bassmd.bihua.ink.InkCanvasKt"
  "com.bassmd.bihua.ink.InkState"
  "com.bassmd.bihua.ui.theme.ThemeKt"
)

failures=0
fail() { echo "  FAIL: $*"; failures=$((failures + 1)); }
pass() { echo "  ok:   $*"; }

echo "verify-apk: $APK"

dex_classes="$("$APKANALYZER" dex packages "$APK" 2>/dev/null | awk '$1 == "C" {print $NF}' || true)"
for cls in "${REQUIRED_CLASSES[@]}"; do
  if grep -qx "$cls" <<<"$dex_classes"; then
    pass "class $cls"
  else
    fail "missing class $cls"
  fi
done

manifest="$("$APKANALYZER" manifest print "$APK")"
pkg="$(grep -o 'package="[^"]*"' <<<"$manifest" | head -1 | cut -d'"' -f2 || true)"
app_id="$("$APKANALYZER" manifest application-id "$APK")"
min_sdk="$("$APKANALYZER" manifest min-sdk "$APK")"
target_sdk="$("$APKANALYZER" manifest target-sdk "$APK")"

[ "$app_id" = "$APP_ID" ] && pass "application id $app_id" || fail "application id is $app_id, expected $APP_ID"
[ "$min_sdk" -ge 29 ] && pass "minSdk $min_sdk" || fail "minSdk $min_sdk is below 29"
pass "targetSdk $target_sdk"

size_mb=$(( $(stat -f%z "$APK") / 1024 / 1024 ))
if [ "$size_mb" -lt 200 ]; then
  pass "size ${size_mb} MB"
else
  fail "APK is ${size_mb} MB, over the 200 MB budget"
fi

if [ "$failures" -gt 0 ]; then
  echo "verify-apk: $failures problem(s)"
  exit 1
fi
echo "verify-apk: all checks passed"
