#!/usr/bin/env bash
# iOS build, simulator, and connected-iPhone loop.
#
#   scripts/ios.sh run [simulator]      build, boot, install, launch
#   scripts/ios.sh build                framework + Xcode Debug simulator build
#   scripts/ios.sh test                 Android unit tests + iosSimulatorArm64Test
#   scripts/ios.sh shot [name]          screenshot to .qa/ios/<name>.png
#   scripts/ios.sh boot [simulator]     boot a simulator
#   scripts/ios.sh reset [simulator]    uninstall the app (clean first run)
#   scripts/ios.sh devices              list iPhone and iPad simulators
#   scripts/ios.sh dark|light           simulator appearance
#   scripts/ios.sh pods                 CocoaPods install (simulator-safe)
#   scripts/ios.sh mlkit-pods           install ML Kit for a device build
#   scripts/ios.sh device-build         signed Debug build for the connected iPhone
#   scripts/ios.sh device-run           build, install, and launch on the iPhone
#   scripts/ios.sh device-id            print the connected iPhone UDID
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
. scripts/env.sh

DEVICE="${BIHUA_IOS_DEVICE:-${2:-iPhone 17 Pro}}"
DERIVED="$ROOT/iosApp/DerivedData"
APP="$DERIVED/Build/Products/Debug-iphonesimulator/Bihua.app"
DEVICE_APP="$DERIVED/Build/Products/Debug-iphoneos/Bihua.app"
BUNDLE_ID="com.bassmd.bihua"
SHOTS="$ROOT/.qa/ios"
TEAM="${BIHUA_IOS_TEAM:-DUST62RX27}"

sim_udid() {
  xcrun simctl list -j devices available | python3 -c '
import json, sys
name = sys.argv[1]
data = json.load(sys.stdin)["devices"]
for devices in data.values():
    for device in devices:
        if device["name"] == name and device["isAvailable"]:
            print(device["udid"])
            raise SystemExit(0)
raise SystemExit(f"no available simulator named {name}")
' "$DEVICE"
}

physical_udid() {
  local kind="${1:-${BIHUA_IOS_PHYSICAL:-iphone}}"
  local json=/tmp/bihua-devices.json
  xcrun devicectl list devices --json-output "$json" >/dev/null 2>&1
  python3 -c '
import json, sys
data = json.load(open(sys.argv[1]))
kind = sys.argv[2].lower()
for device in data.get("result", {}).get("devices", []):
    props = device.get("deviceProperties", {})
    hardware = device.get("hardwareProperties", {})
    connection = device.get("connectionProperties", {})
    if hardware.get("reality") != "physical":
        continue
    if connection.get("pairingState") != "paired":
        continue
    if props.get("developerModeStatus") != "enabled":
        continue
    device_type = (hardware.get("deviceType") or "").lower()
    if kind in ("ipad", "tablet") and device_type != "ipad":
        continue
    if kind in ("iphone", "phone") and device_type != "iphone":
        continue
    print(hardware["udid"])
    raise SystemExit(0)
raise SystemExit(f"no paired {kind} with Developer Mode enabled")
' "$json" "$kind"
}

framework() {
  local task
  case "$1" in
    device) task=":app:linkDebugFrameworkIosArm64" ;;
    *) task=":app:linkDebugFrameworkIosSimulatorArm64" ;;
  esac
  ./gradlew "$task" --console=plain
}

build() {
  if [ -d iosApp/Pods/MLKitDigitalInkRecognition ]; then
    echo "ML Kit is installed for a device build. Run 'make ios-pods' before a simulator build." >&2
    exit 2
  fi
  framework simulator
  local project=(-project iosApp/Bihua.xcodeproj)
  if [ -d iosApp/Bihua.xcworkspace ]; then
    project=(-workspace iosApp/Bihua.xcworkspace)
  fi
  xcodebuild \
    "${project[@]}" \
    -scheme Bihua \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination "id=$(sim_udid)" \
    -derivedDataPath "$DERIVED" \
    CODE_SIGNING_ALLOWED=NO \
    build
}

device_build() {
  local kind="${1:-}"
  if [ ! -d iosApp/Pods/MLKitDigitalInkRecognition ]; then
    echo "warning: ML Kit is not installed; run 'make ios-mlkit-pods' for the device reader" >&2
  fi
  framework device
  xcodebuild \
    -workspace iosApp/Bihua.xcworkspace \
    -scheme Bihua \
    -configuration Debug \
    -destination "id=$(physical_udid "$kind")" \
    -derivedDataPath "$DERIVED" \
    -allowProvisioningUpdates \
    DEVELOPMENT_TEAM="$TEAM" \
    build
}

device_run() {
  local kind="${1:-}"
  device_build "$kind"
  local id; id="$(physical_udid "$kind")"
  xcrun devicectl device install app --device "$id" "$DEVICE_APP"
  xcrun devicectl device process launch --device "$id" --terminate-existing "$BUNDLE_ID" || true
}

device_shot() {
  local kind="${2:-}"
  local id; id="$(physical_udid "$kind")"
  mkdir -p "$SHOTS"
  local name="${1:-device}"
  case "$name" in
    *.png) local target="$name" ;;
    *) local target="$SHOTS/$name.png" ;;
  esac
  xcrun devicectl device capture screenshot --device "$id" --destination "$target"
  echo "$target"
}

boot() {
  xcrun simctl boot "$DEVICE" 2>/dev/null || true
  open -a Simulator --args -CurrentDeviceUDID "$(sim_udid)" >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$(sim_udid)" -b >/dev/null
}

run() {
  build
  boot
  xcrun simctl install "$(sim_udid)" "$APP"
  xcrun simctl terminate "$(sim_udid)" "$BUNDLE_ID" 2>/dev/null || true
  xcrun simctl launch "$(sim_udid)" "$BUNDLE_ID"
}

shot() {
  mkdir -p "$SHOTS"
  local name="${1:-screen}"
  case "$name" in
    *.png) local target="$name" ;;
    *) local target="$SHOTS/$name.png" ;;
  esac
  xcrun simctl io "$(sim_udid)" screenshot "$target"
  echo "$target"
}

case "${1:-run}" in
  framework) framework "${2:-simulator}" ;;
  build) build ;;
  boot) boot ;;
  run) run ;;
  test)
    ./gradlew :app:testDebugUnitTest :app:iosSimulatorArm64Test --console=plain
    ;;
  shot) shot "${2:-screen}" ;;
  reset) xcrun simctl uninstall "$(sim_udid)" "$BUNDLE_ID" 2>/dev/null || true ;;
  devices)
    xcrun simctl list devices available | grep -E "iPhone|iPad" | sed 's/^ *//'
    ;;
  dark) xcrun simctl ui "$(sim_udid)" appearance dark ;;
  light) xcrun simctl ui "$(sim_udid)" appearance light ;;
  pods) (cd iosApp && pod install) ;;
  mlkit-pods) (cd iosApp && BIHUA_MLKIT=1 pod install) ;;
  device-build) device_build "${2:-}" ;;
  device-run) device_run "${2:-}" ;;
  device-shot) device_shot "${2:-device}" "${3:-}" ;;
  device-id) physical_udid "${2:-}" ;;
  *)
    echo "usage: scripts/ios.sh [run|build|test|shot|boot|reset|devices|dark|light|pods|mlkit-pods|device-build|device-run|device-shot|device-id] [simulator or name]" >&2
    exit 2
    ;;
esac
