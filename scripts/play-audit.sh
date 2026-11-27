#!/usr/bin/env bash
# Release-only Play audit. This is intentionally stricter than the debug APK
# verifier: it checks the signed APK/AAB, permissions, version, ABI, and key.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
. scripts/env.sh

APK="${1:-composeApp/build/outputs/apk/release/composeApp-release.apk}"
AAB="${2:-composeApp/build/outputs/bundle/release/composeApp-release.aab}"
FAIL=0
ok() { printf '  ok:   %s\n' "$1"; }
bad() { printf '  FAIL: %s\n' "$1"; FAIL=$((FAIL + 1)); }
warn() { printf '  warn: %s\n' "$1"; }

[ -f "$APK" ] || { echo "play-audit: missing $APK" >&2; exit 2; }
[ -f "$AAB" ] || { echo "play-audit: missing $AAB" >&2; exit 2; }

BT="$(ls -d "$ANDROID_HOME"/build-tools/* 2>/dev/null | sort -V | tail -1)"
[ -x "$BT/aapt" ] || { echo "play-audit: aapt not found" >&2; exit 2; }
[ -x "$BT/apksigner" ] || { echo "play-audit: apksigner not found" >&2; exit 2; }
[ -x "$JAVA_HOME/bin/jarsigner" ] || { echo "play-audit: jarsigner not found" >&2; exit 2; }

echo "bihua play audit"
echo "  apk: $APK"
echo "  aab: $AAB"

BADGING="$("$BT/aapt" dump badging "$APK" 2>/dev/null | head -1)"
VERSION_CODE="$(printf '%s\n' "$BADGING" | sed -n "s/.*versionCode='\([^']*\)'.*/\1/p")"
VERSION_NAME="$(printf '%s\n' "$BADGING" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p")"
TARGET_SDK="$("$BT/aapt" dump badging "$APK" 2>/dev/null | sed -n "s/.*targetSdkVersion:'\([^']*\)'.*/\1/p" | head -1)"
MIN_SDK="$("$BT/aapt" dump badging "$APK" 2>/dev/null | sed -n "s/.*sdkVersion:'\([^']*\)'.*/\1/p" | head -1)"
[ -n "$VERSION_CODE" ] && ok "versionCode $VERSION_CODE" || bad "versionCode missing"
[ -n "$VERSION_NAME" ] && ok "versionName $VERSION_NAME" || bad "versionName missing"
[ "$TARGET_SDK" = "36" ] && ok "targetSdk $TARGET_SDK" || bad "targetSdk is ${TARGET_SDK:-missing}, expected 36"
[ "$MIN_SDK" = "29" ] && ok "minSdk $MIN_SDK" || warn "minSdk is ${MIN_SDK:-missing}, expected 29"

ALLOWED_PERMISSIONS="$(cat <<'EOF'
android.permission.INTERNET
android.permission.ACCESS_NETWORK_STATE
android.permission.POST_NOTIFICATIONS
android.permission.WAKE_LOCK
android.permission.RECEIVE_BOOT_COMPLETED
android.permission.FOREGROUND_SERVICE
com.bassmd.bihua.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
EOF
)"
PERMISSIONS="$("$BT/aapt" dump permissions "$APK" 2>/dev/null | sed -n "s/^uses-permission: name='\([^']*\)'/\1/p")"
while IFS= read -r permission; do
  [ -z "$permission" ] && continue
  if grep -Fxq "$permission" <<< "$ALLOWED_PERMISSIONS"; then
    ok "permission $permission"
  else
    bad "unexpected permission $permission"
  fi
done <<< "$PERMISSIONS"

MANIFEST_TREE="$("$BT/aapt" dump xmltree "$APK" AndroidManifest.xml 2>/dev/null || true)"
if grep -q 'android:debuggable(0x0101000f)=(type 0x12)0xffffffff' <<< "$MANIFEST_TREE"; then
  bad "android:debuggable is true"
else
  ok "android:debuggable is absent/false"
fi

APK_CERTS="$("$BT/apksigner" verify --print-certs "$APK" 2>/dev/null || true)"
if grep -q 'CN=Android Debug' <<< "$APK_CERTS"; then
  bad "APK is signed with the Android debug key"
else
  ok "APK is not debug-signed"
fi
"$BT/apksigner" verify "$APK" >/dev/null 2>&1 && ok "APK signature verifies" || bad "APK signature does not verify"

if "$JAVA_HOME/bin/jarsigner" -verify "$AAB" >/tmp/bihua-play-aab-verify.txt 2>&1; then
  ok "AAB signature verifies"
else
  bad "AAB signature does not verify (see /tmp/bihua-play-aab-verify.txt)"
fi

AAB_ENTRIES="$(unzip -l "$AAB" 2>/dev/null || true)"
if grep -q 'base/lib/arm64-v8a/' <<< "$AAB_ENTRIES"; then
  ok "AAB contains arm64-v8a"
else
  bad "AAB has no arm64-v8a libraries"
fi
if grep -Eq 'base/lib/(armeabi|armeabi-v7a|x86|x86_64)/' <<< "$AAB_ENTRIES"; then
  bad "AAB contains a non-arm64 emulator/32-bit ABI"
else
  ok "AAB is arm64-only"
fi

echo
echo "  exported components to review manually:"
"$BT/aapt" dump xmltree "$APK" AndroidManifest.xml 2>/dev/null \
  | awk '
      /E: (activity|activity-alias|receiver|service|provider)/ {
        comp=$0; name=""
      }
      /A: android:name\(/ {
        if (comp != "") name=$0
      }
      /A: android:exported\(0x01010010\)=\(type 0x12\)0xffffffff/ {
        print "    " comp
        if (name != "") print "      " name
      }
    ' || true

echo
if [ "$FAIL" -ne 0 ]; then
  echo "play-audit: $FAIL problem(s)" >&2
  exit 1
fi
echo "play-audit: all checks passed"
