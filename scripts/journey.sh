#!/usr/bin/env bash
# Local full-app journey harness. Not a CI job.
#
#   scripts/journey.sh                 quick phone check
#   scripts/journey.sh --standard      phone, fold, tablet core
#   scripts/journey.sh --full          every profile and daily state
#   scripts/journey.sh --scenario daily80 --profile fold-inner
#
# Every run is written under .qa/runs/<timestamp>/ and is ignored by git.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/env.sh"
cd "$ROOT"

VENV="$ROOT/.qa/venv"
if [ ! -x "$VENV/bin/python" ]; then
  python3 -m venv "$VENV"
fi
if ! "$VENV/bin/python" -c "import uiautomator2" >/dev/null 2>&1; then
  "$VENV/bin/pip" install -q -r "$ROOT/qa/requirements.txt"
fi
exec "$VENV/bin/python" "$ROOT/qa/journey.py" "$@"
