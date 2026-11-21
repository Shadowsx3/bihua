#!/usr/bin/env bash
# Tap a visible label, waiting for it to appear.
#
#   scripts/tap.sh "<text>" [nth]              single connected device
#   scripts/tap.sh SERIAL "<text>" [nth]       explicit device
#
# Delegates to scripts/device.sh, which re-dumps the UI and retries, so a tap
# does not land on a stale layout.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"

if [ $# -ge 3 ] && [[ "$1" != *" "* ]]; then
  exec "$here/device.sh" -s "$1" tap "$2" "$3"
elif [ $# -eq 2 ]; then
  # Either "SERIAL text" or "text nth"; prefer the serial form only if it is a
  # connected device.
  if adb devices | awk 'NR>1 && $2=="device" {print $1}' | grep -qx "$1"; then
    exec "$here/device.sh" -s "$1" tap "$2"
  fi
  exec "$here/device.sh" tap "$1" "$2"
else
  exec "$here/device.sh" tap "${1:?text}"
fi
