#!/usr/bin/env bash
# One command for the shared Android + iOS gate. It never uploads anything.
#   scripts/verify.sh          Android + compliance + iOS unit tests
#   scripts/verify.sh ui       the above, then the XCUITest journeys
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
. scripts/env.sh

echo "== Android and shared gate =="
make check
make lint
make compliance
python3 scripts/ios-content-check.py

echo "== iOS unit tests =="
./gradlew :composeApp:iosSimulatorArm64Test --console=plain

if [ "${1:-}" = "ui" ]; then
  echo "== iPhone UI journeys =="
  scripts/ios-ui-tests.sh "${BIHUA_IOS_DEVICE:-iPhone 17 Pro}"
  echo "== iPad UI journeys =="
  scripts/ios-ui-tests.sh "${BIHUA_IOS_PAD:-iPad Pro 13-inch (M5)}"
fi

echo "verify: all green"
