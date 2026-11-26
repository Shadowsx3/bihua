#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

PYTHON=".qa/venv/bin/python"
if [[ ! -x "$PYTHON" ]]; then
  echo "qa/venv is missing; run the QA setup first (see qa/README.md)." >&2
  exit 1
fi

exec "$PYTHON" qa/gallery.py "$@"
