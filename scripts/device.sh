#!/usr/bin/env bash
# Device, UI, database, content, and release commands in one place.
#
#   scripts/device.sh [-s SERIAL] status
#   scripts/device.sh [-s SERIAL] install | run | launch | stop
#   scripts/device.sh [-s SERIAL] log | crash
#   scripts/device.sh [-s SERIAL] shot [name]
#   scripts/device.sh [-s SERIAL] ui [file]
#   scripts/device.sh [-s SERIAL] find "<text>" [nth]
#   scripts/device.sh [-s SERIAL] tap "<text>" [nth]
#   scripts/device.sh [-s SERIAL] type "<text>"
#   scripts/device.sh [-s SERIAL] swipe x1 y1 x2 y2
#   scripts/device.sh [-s SERIAL] draw x1 y1 x2 y2 [x3 y3 ...]
#   scripts/device.sh [-s SERIAL] key <keycode>
#   scripts/device.sh [-s SERIAL] shell <command...>
#
#   scripts/device.sh db-pull [file]
#   scripts/device.sh db-cards
#   scripts/device.sh db-reviews [limit]
#   scripts/device.sh db-reset
#   scripts/device.sh seed-review <character> [mode] [reps] [dueMinutes]
#   scripts/device.sh seed-learning <character> [mode] [reps] [dueMinutes]
#
#   scripts/device.sh content            rebuild the content DB
#   scripts/device.sh content --bump     rebuild and bump contentVersion
#   scripts/device.sh release            minified release APK
#   scripts/device.sh install-release    install the release APK
#
# With more than one device connected, set BIHUA_DEVICE or pass -s.
# The DB commands need a debug build (run-as); release builds are not debuggable.

set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$BIHUA_ROOT"

DEVICE="${BIHUA_DEVICE:-}"
ARGS=()
while [ $# -gt 0 ]; do
  case "$1" in
    -s|--device)
      [ $# -ge 2 ] || { echo "missing value for $1" >&2; exit 2; }
      DEVICE="$2"; shift 2 ;;
    *) ARGS+=("$1"); shift ;;
  esac
done
set -- ${ARGS[@]+"${ARGS[@]}"}

resolve_device() {
  [ -n "$DEVICE" ] && return 0
  local list count first
  list=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')
  count=$(printf '%s\n' "$list" | grep -c . || true)
  if [ "$count" -eq 0 ]; then
    echo "No device. Connect one, unlock it, and accept the debugging prompt." >&2
    exit 1
  fi
  if [ "$count" -gt 1 ]; then
    echo "More than one device:" >&2
    printf '  %s\n' $list >&2
    echo "Set BIHUA_DEVICE=<serial> or pass -s <serial>." >&2
    exit 1
  fi
  first=$(printf '%s\n' "$list" | head -1)
  DEVICE="$first"
}

adb_cmd() {
  resolve_device
  adb -s "$DEVICE" "$@"
}

require_apk() {
  [ -f "$APK_DEBUG" ] || { echo "No debug APK. Run scripts/check.sh first." >&2; exit 1; }
}

require_run_as() {
  resolve_device
  if ! adb -s "$DEVICE" shell "run-as $APP_ID true" >/dev/null 2>&1; then
    echo "This build is not debuggable. Install the debug build for database commands." >&2
    exit 1
  fi
}

launch() {
  adb_cmd shell am force-stop "$APP_ID" >/dev/null
  adb_cmd shell am start -n "$APP_ID/.MainActivity" >/dev/null
  echo "launched $APP_ID on $DEVICE"
}

dump_ui() {
  local target="${1:-$BIHUA_ROOT/build/ui.xml}"
  mkdir -p "$(dirname "$target")"
  adb_cmd shell uiautomator dump /sdcard/bihua-ui.xml >/dev/null 2>&1
  adb_cmd exec-out cat /sdcard/bihua-ui.xml > "$target"
  echo "$target"
}

find_text() {
  local text="$1" nth="${2:-1}" xml
  xml=$(mktemp -t bihua-ui.XXXXXX)
  dump_ui "$xml" >/dev/null
  python3 - "$xml" "$text" "$nth" <<'PY'
import re, sys
path, text, nth = sys.argv[1], sys.argv[2], int(sys.argv[3])
s = open(path, encoding="utf-8").read()
hits = []
for tag in re.finditer(r'<node\b[^>]*>', s):
    body = tag.group(0)
    if f'text="{text}"' not in body and f'content-desc="{text}"' not in body:
        continue
    bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', body)
    if not bounds:
        continue
    hits.append(((int(bounds.group(1)) + int(bounds.group(3))) // 2,
                 (int(bounds.group(2)) + int(bounds.group(4))) // 2))
if len(hits) >= nth:
    print(hits[nth - 1][0], hits[nth - 1][1])
PY
  rm -f "$xml"
}

wait_text() {
  local text="$1" nth="${2:-1}" i coords
  for i in $(seq 1 40); do
    coords=$(find_text "$text" "$nth" || true)
    if [ -n "$coords" ]; then echo "$coords"; return 0; fi
    sleep 0.25
  done
  return 1
}

db_pull() {
  require_run_as
  local out="${1:-$BIHUA_ROOT/build/bihua_progress.db}"
  mkdir -p "$(dirname "$out")"
  adb_cmd exec-out run-as "$APP_ID" cat databases/bihua_progress.db > "$out"
  echo "$out"
}

db_push() {
  require_run_as
  local source="$1"
  [ -f "$source" ] || { echo "No such DB: $source" >&2; exit 1; }
  adb_cmd shell am force-stop "$APP_ID" >/dev/null
  adb_cmd shell "run-as $APP_ID sh -c 'cat > databases/bihua_progress.db'" < "$source"
  adb_cmd shell "run-as $APP_ID rm -f databases/bihua_progress.db-journal databases/bihua_progress.db-wal databases/bihua_progress.db-shm" >/dev/null
  adb_cmd shell am start -n "$APP_ID/.MainActivity" >/dev/null
  echo "database pushed; app restarted"
}

db_query() {
  local sql="$1"
  local db
  db=$(db_pull "$(mktemp -t bihua-db.XXXXXX)")
  sqlite3 -header -column "$db" "$sql"
  rm -f "$db"
}

seed_card() {
  local kind="$1"
  local character="${2:-}"
  local mode="${3:-SIMPLIFIED}"
  local reps="${4:-2}"
  local minutes="${5:-0}"
  [ -n "$character" ] || { echo "usage: seed-$kind <character> [mode] [reps] [dueMinutes]" >&2; exit 1; }
  require_run_as

  local content="$BIHUA_ROOT/app/src/androidMain/assets/bihua_content.db"
  local row
  row=$(sqlite3 -separator '|' "$content" \
    "SELECT id, glyph_group_id, mode FROM character WHERE character='$character' AND mode='$mode' LIMIT 1;")
  if [ -z "$row" ]; then
    echo "Character '$character' not found in $mode" >&2
    exit 1
  fi
  local cid glyph cmode
  IFS='|' read -r cid glyph cmode <<< "$row"

  adb_cmd shell am force-stop "$APP_ID" >/dev/null
  local db
  db=$(mktemp -t bihua-progress.XXXXXX)
  db_pull "$db" >/dev/null
  python3 - "$db" "$cid" "$glyph" "$cmode" "$reps" "$minutes" <<'PY'
import sqlite3, sys, time
db, cid, glyph, mode, reps, minutes = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], int(sys.argv[5]), int(sys.argv[6])
now = int(time.time() * 1000)
due = now + minutes * 60_000
conn = sqlite3.connect(db)
conn.execute(
    "INSERT OR REPLACE INTO card(id,character_id,glyph_group_id,mode,s_long,s_short,difficulty,"
    "due_at,last_review_at,reps,lapses,introduced_at,known_dots,last_grade) "
    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
    ("char:" + cid, cid, glyph, mode, 2.0, 1.6, 5.0, due, now - 86_400_000,
     reps, 0, now - 2 * 86_400_000, 1, 3),
)
conn.commit()
conn.close()
print(f"{cid} {mode} reps={reps} due={minutes}min")
PY
  db_push "$db"
  rm -f "$db"
}

cmd="${1:-status}"
shift || true

case "$cmd" in
  status)
    adb devices -l
    resolve_device
    echo "selected: $DEVICE"
    if adb -s "$DEVICE" shell pm list packages | grep -q "$APP_ID"; then
      adb -s "$DEVICE" shell dumpsys package "$APP_ID" | grep -E "versionName|versionCode" | head -2
      adb -s "$DEVICE" shell dumpsys activity activities 2>/dev/null | grep -m1 "mResumedActivity" || true
    else
      echo "$APP_ID is not installed"
    fi
    ;;

  install)
    resolve_device
    require_apk
    adb -s "$DEVICE" install -r -t "$APK_DEBUG" >/dev/null
    launch
    ;;

  run)
    scripts/check.sh
    "$0" -s "$(resolve_device; echo "$DEVICE")" install
    ;;

  launch) launch ;;
  stop) adb_cmd shell am force-stop "$APP_ID" >/dev/null && echo "stopped $APP_ID" ;;

  log)
    resolve_device
    pid=$(adb -s "$DEVICE" shell pidof -s "$APP_ID" 2>/dev/null || true)
    if [ -z "$pid" ]; then
      echo "app not running; buffered package log" >&2
      adb -s "$DEVICE" logcat -v time | grep -i "bihua" || true
    else
      adb -s "$DEVICE" logcat -v time --pid="$pid"
    fi
    ;;

  crash)
    resolve_device
    adb -s "$DEVICE" logcat -d -v time | grep -E "FATAL EXCEPTION|ANR in|$APP_ID" | tail -40
    ;;

  shot)
    resolve_device
    name="${1:-screen-$(date +%Y%m%d-%H%M%S)}"
    case "$name" in
      /*) target="$name"; mkdir -p "$(dirname "$target")" ;;
      *.png) target="$BIHUA_ROOT/screenshots/$name"; mkdir -p "$(dirname "$target")" ;;
      *) target="$BIHUA_ROOT/screenshots/$name.png"; mkdir -p "$(dirname "$target")" ;;
    esac
    remote="/data/local/tmp/bihua-shot-$$.png"
    adb -s "$DEVICE" shell screencap -p "$remote" >/dev/null
    adb -s "$DEVICE" pull "$remote" "$target" >/dev/null
    adb -s "$DEVICE" shell rm -f "$remote" >/dev/null
    echo "$target"
    ;;
  a11y)
    resolve_device
    target=$(dump_ui "$BIHUA_ROOT/build/a11y.xml")
    python3 - "$target" <<'PY'
import re, sys
s = open(sys.argv[1], encoding="utf-8").read()
empty = []
for tag in re.finditer(r"<node\b[^>]*>", s):
    body = tag.group(0)
    if 'clickable="true"' not in body:
        continue
    text = re.search(r'text="([^"]*)"', body)
    desc = re.search(r'content-desc="([^"]*)"', body)
    bounds = re.search(r'bounds="([^"]*)"', body)
    label = (desc.group(1) if desc else "") or (text.group(1) if text else "")
    if not label:
        empty.append(bounds.group(1) if bounds else "?")
print(f"clickable nodes without a spoken label: {len(empty)}")
for item in empty[:20]:
    print(" ", item)
PY
    ;;
  keyboard)
    resolve_device
    mode="${1:-toggle}"
    if [ "$mode" = "toggle" ]; then
      current=$(adb -s "$DEVICE" shell settings get secure show_ime_with_hard_keyboard | tr -d '\r')
      if [ "$current" = "1" ]; then mode=off; else mode=on; fi
    fi
    case "$mode" in
      on) adb -s "$DEVICE" shell settings put secure show_ime_with_hard_keyboard 1 >/dev/null; echo "soft keyboard forced on" ;;
      off) adb -s "$DEVICE" shell settings put secure show_ime_with_hard_keyboard 0 >/dev/null; echo "soft keyboard follows hardware" ;;
      *) echo "usage: device.sh keyboard on|off|toggle" >&2; exit 2 ;;
    esac
    ;;
  session-clear)
    resolve_device
    adb -s "$DEVICE" shell run-as "$APP_ID" rm -f shared_prefs/bihua_session.xml 2>/dev/null || true
    echo "cleared saved lesson session" ;;

  key)
    resolve_device
    adb -s "$DEVICE" shell input keyevent "${1:?keycode}" ;;

  ui)
    resolve_device
    target=$(dump_ui "${1:-$BIHUA_ROOT/build/ui.xml}")
    python3 - "$target" <<'PY'
import re, sys
s = open(sys.argv[1], encoding="utf-8").read()
rows = []
for tag in re.finditer(r"<node\b[^>]*>", s):
    body = tag.group(0)
    text = re.search(r'text="([^"]*)"', body)
    desc = re.search(r'content-desc="([^"]*)"', body)
    bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', body)
    label = (desc.group(1) if desc else "") or (text.group(1) if text else "")
    if not label or not bounds:
        continue
    rows.append(
        f"{label}  [{(int(bounds.group(1))+int(bounds.group(3)))//2},{(int(bounds.group(2))+int(bounds.group(4)))//2}]"
    )
print("\n".join(rows[:400]))
PY
    ;;

  find)
    resolve_device
    coords=$(wait_text "${1:-}" "${2:-1}") || { echo "not found: ${1:-}" >&2; exit 1; }
    echo "$coords"
    ;;

  tap)
    resolve_device
    coords=$(wait_text "${1:-}" "${2:-1}") || { echo "not found: ${1:-}" >&2; exit 1; }
    echo "tap ${1} at $coords"
    adb -s "$DEVICE" shell input tap $coords
    ;;

  type)
    resolve_device
    text="${1:-}"
    escaped=${text// /%s}
    adb -s "$DEVICE" shell input text "$escaped"
    ;;

  swipe)
    resolve_device
    [ $# -eq 4 ] || { echo "usage: swipe x1 y1 x2 y2" >&2; exit 2; }
    adb -s "$DEVICE" shell input swipe "$1" "$2" "$3" "$4" 300
    ;;

  draw)
    resolve_device
    [ $# -ge 4 ] && [ $(( $# % 2 )) -eq 0 ] || {
      echo "usage: draw x1 y1 x2 y2 [x3 y3 ...]" >&2; exit 2;
    }
    coords=("$@")
    adb -s "$DEVICE" shell input motionevent DOWN "${coords[0]}" "${coords[1]}"
    i=2
    while [ "$i" -lt "${#coords[@]}" ]; do
      adb -s "$DEVICE" shell input motionevent MOVE "${coords[$i]}" "${coords[$((i+1))]}"
      i=$((i + 2))
    done
    last=$(( ${#coords[@]} - 2 ))
    adb -s "$DEVICE" shell input motionevent UP "${coords[$last]}" "${coords[$((last+1))]}"
    ;;

  key)
    resolve_device
    adb -s "$DEVICE" shell input keyevent "${1:?keycode}" ;;

  shell)
    resolve_device
    adb -s "$DEVICE" shell "$@" ;;

  db-pull) db_pull "${1:-}" ;;
  db-push) db_push "${1:?path to DB}" ;;
  db-cards) db_query "SELECT id, mode, reps, lapses, known_dots, last_grade, due_at FROM card ORDER BY mode, id;" ;;
  db-reviews)
    limit="${1:-20}"
    db_query "SELECT datetime(reviewed_at/1000,'unixepoch','localtime') AS at, character_id, grade, task, round(interval_days,1) AS days, elapsed_ms FROM review_log ORDER BY reviewed_at DESC LIMIT $limit;" ;;
  db-reset)
    resolve_device
    read -r -p "Delete cards, reviews, XP, and confusions on $DEVICE? Type 'yes': " answer
    [ "$answer" = "yes" ] || { echo "cancelled"; exit 1; }
    adb_cmd shell am force-stop "$APP_ID" >/dev/null
    db=$(mktemp -t bihua-reset.XXXXXX)
    db_pull "$db" >/dev/null
    sqlite3 "$db" "DELETE FROM card; DELETE FROM review_log; DELETE FROM xp_log; DELETE FROM confusion; DELETE FROM word_practice; DELETE FROM player; INSERT OR IGNORE INTO player(id,xp) VALUES(1,0);"
    db_push "$db"
    rm -f "$db"
    ;;

  seed-review) seed_card review "${1:-}" "${2:-SIMPLIFIED}" "${3:-2}" "${4:-0}" ;;
  seed-learning) seed_card learning "${1:-}" "${2:-SIMPLIFIED}" "${3:-1}" "${4:-10}" ;;

  content)
    python3 tools/import/build_content_db.py
    ls -lh app/src/androidMain/assets/bihua_content.db
    sqlite3 app/src/androidMain/assets/bihua_content.db "SELECT key || '=' || value FROM meta WHERE key IN ('structureEntries','words','glyphGroups');"
    if [ "${1:-}" = "--bump" ]; then
      python3 - <<'PY'
import re
p = 'app/src/main/java/com/bassmd/bihua/content/ContentRepository.kt'
s = open(p).read()
m = re.search(r'contentVersion = (\d+)', s)
if not m:
    raise SystemExit('contentVersion not found')
new = int(m.group(1)) + 1
s = s[:m.start(1)] + str(new) + s[m.end(1):]
open(p, 'w').write(s)
print(f'contentVersion -> {new}')
PY
    fi
    ;;

  release)
    ./gradlew :app:assembleRelease --console=plain 2>&1 | grep -E "^e: |FAILED|BUILD" | tail -5
    apk=app/build/outputs/apk/release/app-release.apk
    [ -f "$apk" ] || { echo "release APK missing" >&2; exit 1; }
    python3 - <<'PY'
import json
p = 'app/build/outputs/apk/release/output-metadata.json'
d = json.load(open(p))['elements'][0]
print(f"release {d.get('versionName')} ({d.get('versionCode')})")
PY
    ls -lh "$apk"
    if [ -f keystore.properties ]; then
      echo "signed with keystore.properties"
    else
      echo "warning: no keystore.properties; this release is debug-signed"
    fi
    ;;

  install-release)
    resolve_device
    apk=app/build/outputs/apk/release/app-release.apk
    [ -f "$apk" ] || { echo "Run scripts/device.sh release first." >&2; exit 1; }
    adb -s "$DEVICE" install -r "$apk" >/dev/null
    launch
    ;;

  *)
    echo "unknown command: $cmd" >&2
    sed -n '2,40p' "$0" >&2
    exit 2
    ;;
esac
