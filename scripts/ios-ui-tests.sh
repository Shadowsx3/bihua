#!/usr/bin/env bash
# Run each XCUITest in its own runner process. The iOS 26 simulator can drop a
# long UI-test session after a dozen app relaunches (the duplicate WebKit
# accessibility class warning), which looks like a product failure. A fresh
# runner per test keeps the evidence honest.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
. scripts/env.sh

NAME="${1:-${BIHUA_IOS_DEVICE:-iPhone 17 Pro}}"
UDID="$(xcrun simctl list -j devices available | python3 -c '
import json, sys
name = sys.argv[1]
data = json.load(sys.stdin)
for runtime, devices in data["devices"].items():
    if "iOS" not in runtime:
        continue
    for device in devices:
        if device.get("name") == name and device.get("isAvailable"):
            print(device["udid"])
            raise SystemExit(0)
raise SystemExit(f"no available simulator named {name}")
' "$NAME")"
DERIVED="$ROOT/iosApp/DerivedData"
PROJECT="$ROOT/iosApp/Bihua.xcodeproj"

echo "== building UI tests for $NAME =="
xcodebuild build-for-testing \
  -project "$PROJECT" \
  -scheme Bihua \
  -destination "id=$UDID" \
  -derivedDataPath "$DERIVED" \
  CODE_SIGNING_ALLOWED=NO >/dev/null

TESTS=()
while IFS= read -r test; do
  TESTS+=("$test")
done < <(grep -o 'func test[A-Za-z0-9_]*' iosApp/BihuaUITests/BihuaUITests.swift | sed 's/func //')
FAILED=()
for test in "${TESTS[@]}"; do
  printf '  %-48s ' "$test"
  if xcodebuild test-without-building \
      -project "$PROJECT" \
      -scheme Bihua \
      -destination "id=$UDID" \
      -derivedDataPath "$DERIVED" \
      -only-testing:"BihuaUITests/BihuaUITests/$test" \
      CODE_SIGNING_ALLOWED=NO >/tmp/bihua-ui-test.log 2>&1; then
    echo "ok"
  else
    echo "FAIL (log: /tmp/bihua-ui-test.log)"
    FAILED+=("$test")
  fi
done

echo
if [ "${#FAILED[@]}" -ne 0 ]; then
  printf 'failed: %s\n' "${FAILED[*]}"
  exit 1
fi
echo "ui tests: ${#TESTS[@]} passed on $NAME"
