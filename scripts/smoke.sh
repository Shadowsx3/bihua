#!/usr/bin/env bash
# Small on-device smoke test. Requires an installed debug build and a connected
# device. It deliberately does not grade cards or change preferences.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/env.sh"
cd "$ROOT"

DEVICE="${BIHUA_DEVICE:-$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')}"
[ -n "$DEVICE" ] || { echo "No device. Connect one, unlock it, and accept the debugging prompt." >&2; exit 1; }
export DEVICE

fail() { echo "smoke failed: $*" >&2; exit 1; }
step() { printf '  %-22s\n' "$1"; }

ui() { scripts/device.sh ui; }
has() { ui | grep -qF "$1"; }
wait_for() {
  local text="$1" i
  for i in $(seq 1 40); do
    if has "$text"; then return 0; fi
    sleep 0.5
  done
  return 1
}
tap() { scripts/device.sh tap "$1" >/dev/null; sleep 1; }

adb -s "$DEVICE" shell am force-stop "$APP_ID" >/dev/null
adb -s "$DEVICE" shell am start -n "$APP_ID/.MainActivity" >/dev/null
sleep 7

echo "bihua smoke"
step "launch"
wait_for "Home" || fail "Home tab is not visible"
if has "Welcome to Bihua"; then
  fail "onboarding is showing; complete it once, then rerun"
fi

step "settings"
tap "Settings"
wait_for "Settings" || fail "Settings did not open"
wait_for "Make Bihua yours" || fail "Settings content did not load"

step "display"
# The category row scrolls; open Display when it is already visible.
if has "Display"; then
  tap "Display"
  wait_for "Theme" || fail "Display section did not load"
  wait_for "System" || fail "Theme control is missing"
fi

step "home"
tap "Home"
wait_for "Hello" || fail "Home did not return"

step "practice"
if has "Practice"; then
  tap "Practice"
  wait_for "Practice" || fail "Practice did not open"
  wait_for "Change" || fail "Practice canvas controls did not load"
fi

step "settings round-trip"
tap "Settings"
wait_for "Settings" || fail "Settings round-trip failed"

echo "bihua smoke: ok"
