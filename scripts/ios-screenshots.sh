#!/usr/bin/env bash
# Capture the shipping screens for a local App Store review. Simulator only.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
# App Store 6.9-inch: iPhone 17 Pro Max. iPad 13-inch: iPad Pro 13-inch (M5).
SIM="${1:-iPhone 17 Pro Max}"
OUT=".qa/ios/store"
mkdir -p "$OUT"
BUNDLE=com.bassmd.bihua

shot() {
  local name="$1"; shift
  xcrun simctl terminate "$SIM" "$BUNDLE" 2>/dev/null || true
  xcrun simctl launch "$SIM" "$BUNDLE" "$@" >/dev/null
  sleep 4
  xcrun simctl io "$SIM" screenshot "$OUT/$name.png" >/dev/null 2>&1
  echo "$OUT/$name.png"
}

shot onboarding -reset-settings -reset-progress
shot home -reset-settings -reset-progress -skip-onboarding
shot characters -reset-settings -reset-progress -skip-onboarding -tab 1
shot practice -reset-settings -reset-progress -skip-onboarding -tab 2
shot progress -reset-settings -reset-progress -skip-onboarding -tab 3
shot settings -reset-settings -reset-progress -skip-onboarding -tab 4
shot review -reset-settings -reset-progress -skip-onboarding -push review -review-answer
shot dictionary -reset-settings -reset-progress -skip-onboarding -push dictionary -dictionary-query love
shot stroke -reset-settings -reset-progress -skip-onboarding -push stroke/SIMPLIFIED:U+7231
