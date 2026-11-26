#!/usr/bin/env bash
# iOS build and simulator loop. P0 spike: link the Kotlin framework, build the
# Swift host, install on a booted simulator, and capture evidence.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
. scripts/env.sh

DEVICE="${BIHUA_IOS_DEVICE:-iPhone 17 Pro}"
DERIVED="$ROOT/iosApp/DerivedData"
APP="$DERIVED/Build/Products/Debug-iphonesimulator/Bihua.app"
BUNDLE_ID="com.bassmd.bihua"

udid() {
  xcrun simctl list -j devices available | python3 -c '
import json, sys
name = sys.argv[1]
data = json.load(sys.stdin)["devices"]
print(next(d["udid"] for devices in data.values() for d in devices if d["name"] == name))
' "$DEVICE"
}

framework() {
  ./gradlew :app:linkDebugFrameworkIosSimulatorArm64 --console=plain
}

build() {
  framework
  xcodebuild \
    -project iosApp/Bihua.xcodeproj \
    -scheme Bihua \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination "id=$(udid)" \
    -derivedDataPath "$DERIVED" \
    build
}

boot() {
  xcrun simctl boot "$DEVICE" 2>/dev/null || true
  open -a Simulator --args -CurrentDeviceUDID "$(udid)" >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$(udid)" -b >/dev/null
}

run() {
  build
  boot
  xcrun simctl install booted "$APP"
  xcrun simctl launch booted "$BUNDLE_ID"
}

shot() {
  local name="${1:-ios.png}"
  xcrun simctl io booted screenshot "$name"
}

reset() {
  xcrun simctl uninstall booted "$BUNDLE_ID" 2>/dev/null || true
}

case "${1:-run}" in
  framework) framework ;;
  build) build ;;
  boot) boot ;;
  run) run ;;
  shot) shot "${2:-/tmp/bihua-ios.png}" ;;
  reset) reset ;;
  *)
    echo "usage: scripts/ios.sh [framework|build|boot|run|shot <path>|reset]" >&2
    exit 2
    ;;
esac
