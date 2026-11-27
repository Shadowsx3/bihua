#!/usr/bin/env bash
# One command to answer "does it compile, do the tests pass, and is the APK sound?"
#
#   scripts/check.sh            compile + unit tests + debug APK + APK verification
#   scripts/check.sh --quick    compile only (what the pre-commit hook runs)
#   scripts/check.sh --lint     also run Android lint (slower)
#
# Output is short on success. On failure it prints only the compiler and test
# errors, not the whole Gradle log, so a failure is readable at a glance.

set -uo pipefail
source "$(dirname "$0")/env.sh"
cd "$BIHUA_ROOT"

QUICK=0
LINT=0
for arg in "$@"; do
  case "$arg" in
    --quick) QUICK=1 ;;
    --lint) LINT=1 ;;
    *) echo "unknown option: $arg" >&2; exit 2 ;;
  esac
done

LOG="$(mktemp -t bihua-check.XXXX)"
trap 'rm -f "$LOG"' EXIT

run() {
  local label="$1"; shift
  local start end
  start=$(date +%s)
  if "$@" >"$LOG" 2>&1; then
    end=$(date +%s)
    printf "  %-22s ok (%ds)\n" "$label" $((end - start))
    return 0
  fi
  end=$(date +%s)
  printf "  %-22s FAILED (%ds)\n" "$label" $((end - start))
  grep -E "^e: |^w: .*bihua|FAILED|What went wrong|Test.*FAILED|AssertionError|expected:" -A2 "$LOG" \
    | grep -v "^--$" | head -60
  echo "  full log: $LOG"
  trap - EXIT
  exit 1
}

echo "bihua check"
run "compile" ./gradlew :composeApp:compileDebugKotlin --console=plain -q

if [ "$QUICK" -eq 1 ]; then
  echo "bihua check: compile ok"
  exit 0
fi

run "unit tests" ./gradlew :composeApp:testDebugUnitTest --console=plain -q
run "content audit" python3 tools/audit_content.py
run "debug apk" ./gradlew :composeApp:assembleDebug --console=plain -q

if [ "$LINT" -eq 1 ]; then
  run "lint" ./gradlew :composeApp:lintDebug --console=plain -q
fi

if ! "$BIHUA_ROOT/scripts/verify-apk.sh" >"$LOG" 2>&1; then
  printf "  %-22s FAILED\n" "apk verification"
  cat "$LOG"
  trap - EXIT
  exit 1
fi
printf "  %-22s ok\n" "apk verification"
echo "bihua check: all green"
