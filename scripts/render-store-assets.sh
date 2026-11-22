#!/usr/bin/env bash
# Render every Play Store frame from its SVG source with headless Chrome.
#
#   scripts/render-store-assets.sh          # all phone + tablet frames
#   scripts/render-store-assets.sh phone    # phone only
#   scripts/render-store-assets.sh tablet   # 7-inch and 10-inch only
#
# Raw app screenshots must already be in place:
#   store/app-shots/*.png             1080x2400
#   store/tablet-home-104.png         2560x1600
#   store/tablet-detail-104.png       2560x1600
#   store/tablet-meet-104.png         2560x1600
#   store/tablet-practice-104.png     2560x1600
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STORE="$ROOT/store"
CHROME="${CHROME_PATH:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
ONLY="${1:-all}"

if [[ ! -x "$CHROME" ]]; then
  echo "Chrome not found. Set CHROME_PATH to the Chrome binary." >&2
  exit 1
fi

render() {
  local source="$1"
  local output="$2"
  local size="$3"
  [[ -f "$STORE/$source" ]] || { echo "missing $source" >&2; exit 1; }
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars \
    --allow-file-access-from-files --force-device-scale-factor=1 \
    --window-size="$size" \
    --screenshot="$STORE/$output" \
    "file://$STORE/$source" >/dev/null 2>&1
  echo "store/$output"
}

if [[ "$ONLY" == "all" || "$ONLY" == "phone" ]]; then
  for number in 1 2 3 4 5 6 7; do
    render "phone-$number-light.svg" "phone-$number.png" "1080,1920"
  done
fi

if [[ "$ONLY" == "all" || "$ONLY" == "tablet" ]]; then
  render "tablet-7-1-home.svg" "tablet-7-home.png" "1920,1080"
  render "tablet-7-2-detail.svg" "tablet-7-detail.png" "1920,1080"
  render "tablet-7-3-practice.svg" "tablet-7-practice.png" "1920,1080"
  render "tablet-7-4-progress.svg" "tablet-7-progress.png" "1920,1080"
  render "tablet-10-1-home.svg" "tablet-10-home.png" "2560,1440"
  render "tablet-10-2-detail.svg" "tablet-10-detail.png" "2560,1440"
  render "tablet-10-3-practice.svg" "tablet-10-practice.png" "2560,1440"
  render "tablet-10-4-progress.svg" "tablet-10-progress.png" "2560,1440"
fi
