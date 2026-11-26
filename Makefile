# Recipes source scripts/env.sh, which uses bash syntax. Use bash on every platform.
SHELL := /bin/bash

# Shortcuts for the build, check, and device loop. Run `make help` for the list.

.PHONY: help check quick test lint audit smoke journey journey-standard journey-full nav-gap apk verify play-audit install run log crash shot status hooks clean clean-shots quality quality-full goldens monkey \
	devices ui find tap type draw swipe key release install-release visual-matrix \
	db-pull db-cards db-reviews db-reset seed-review seed-learning content content-bump

TEXT ?=
CHAR ?= 爱
MODE ?= SIMPLIFIED
REPS ?= 2
MINUTES ?= 10
NAME ?=
PTS ?=
NTH ?= 1

help:
	@echo "make check     compile, unit tests, content audit, debug APK, verification"
	@echo "make quick     compile only (fastest feedback)"
	@echo "make test      unit tests only"
	@echo "make audit     content audit (fails only on critical data problems)"
	@echo "make lint      Android lint"
	@echo "make audit     content integrity and quality audit"
	@echo "make smoke     on-device launch/navigation smoke test"
	@echo "make journey   local full-app quick journey check"
	@echo "make journey-standard  phone, fold, tablet core journeys"
	@echo "make journey-full  local full-app journey matrix"
	@echo "make nav-gap   bottom bar meets content at 1x and 1.5x"
	@echo "make quality   local check + phone visual/layout pass"
	@echo "make quality-full  local check + phone/fold/tablet visual matrix"
	@echo "make goldens   regenerate visual baselines (phone screen pass)"
	@echo "make monkey    deterministic random walk on the phone"
	@echo "make perf      frame snapshot for Characters and a daily session"
	@echo "make visual-matrix  every mode on phone/fold/tablet in both rotations"
	@echo "make apk       debug APK only"
	@echo "make verify    verify the existing APK"
	@echo "make play-audit  release APK/AAB permissions, signature, version, ABI"
	@echo "make run       check, then install and launch on the selected device"
	@echo "make install   install and launch the existing APK"
	@echo "make log       live logcat for the app"
	@echo "make crash     recent crashes for the app"
	@echo "make shot      screenshot ([NAME=...])"
	@echo "make status    device and install state"
	@echo "make devices   connected devices"
	@echo "make ui        dump visible labels and coordinates"
	@echo "make find TEXT='Review'   print the center of a label"
	@echo "make tap TEXT='Start studying' [NTH=1]"
	@echo "make type TEXT='ai4'      type into the focused field"
	@echo "make draw PTS='100 200 300 400'  simulated pen path"
	@echo "make release   minified release APK"
	@echo "make install-release  install the release APK"
	@echo "make db-pull | db-cards | db-reviews [limit=20] | db-reset"
	@echo "make seed-review CHAR=爱 [MODE=SIMPLIFIED] [REPS=2]"
	@echo "make seed-learning CHAR=爱 [MODE=KOREAN] [MINUTES=10]"
	@echo "make content | content-bump  rebuild the content database"
	@echo "make hooks     install the git pre-commit hook"
	@echo "make clean     remove build output (keeps screenshots)"
	@echo "make clean-shots  delete screenshots"
	@echo "Set BIHUA_DEVICE=<serial> when more than one device is connected."

check:
	@scripts/check.sh

quick:
	@scripts/check.sh --quick

test:
	@. scripts/env.sh && ./gradlew :app:testDebugUnitTest --console=plain -q && echo "unit tests ok"

lint:
	@. scripts/env.sh && ./gradlew :app:lintDebug --console=plain -q && echo "lint ok"

audit:
	@python3 tools/audit_content.py

smoke:
	@scripts/smoke.sh

journey:
	@scripts/journey.sh

journey-standard:
	@scripts/journey.sh --standard

journey-full:
	@scripts/journey.sh --full

nav-gap:
	@scripts/journey.sh --scenario nav-gap --profile phone --screenshots
	@scripts/journey.sh --scenario nav-gap --profile phone-compact --font-scale 1.5 --screenshots

gallery:
	scripts/flow-gallery.sh $(SCENARIO)

quality:
	@scripts/check.sh
	@scripts/journey.sh --scenario screen-pass --profile phone --screenshots --goldens --require-goldens --layout-audit --no-build
	@scripts/journey.sh --scenario spell-long --profile phone --screenshots --layout-audit --no-build
	@scripts/journey.sh --scenario monkey --profile phone --layout-audit --no-build

quality-full:
	@scripts/check.sh
	@scripts/journey.sh --scenario screen-pass --profile phone,fold-inner,tablet --screenshots --goldens --require-goldens --layout-audit --no-build
	@scripts/journey.sh --scenario monkey --profile phone,fold-inner,tablet --layout-audit --no-build

goldens:
	@scripts/journey.sh --scenario screen-pass --profile phone,fold-inner,tablet --screenshots --update-goldens --layout-audit --no-build

monkey:
	@scripts/journey.sh --scenario monkey --profile phone --screenshots --layout-audit --no-build

perf:
	@scripts/journey.sh --scenario characters-scroll,daily20 --profile phone --perf --no-build

visual-matrix:
	@scripts/journey.sh --scenario screen-pass,screen-pass-traditional,screen-pass-ja,screen-pass-ko --profile phone,phone-landscape,fold-inner,fold-inner-portrait,fold-outer,fold-outer-landscape,tablet,tablet-portrait --screenshots --layout-audit --parallel 3 --no-build

apk:
	@. scripts/env.sh && ./gradlew :app:assembleDebug --console=plain -q && echo "apk ok"

verify:
	@scripts/verify-apk.sh

play-audit:
	@scripts/play-audit.sh

run:
	@scripts/device.sh run

install:
	@scripts/device.sh install

log:
	@scripts/device.sh log

crash:
	@scripts/device.sh crash

shot:
	@scripts/device.sh shot $(NAME)

devices:
	@adb devices -l

ui:
	@scripts/device.sh ui

find:
	@scripts/device.sh find "$(TEXT)" "$(NTH)"

tap:
	@scripts/device.sh tap "$(TEXT)" "$(NTH)"

type:
	@scripts/device.sh type "$(TEXT)"

draw:
	@scripts/device.sh draw $(PTS)

release:
	@scripts/device.sh release

install-release:
	@scripts/device.sh install-release

db-pull:
	@scripts/device.sh db-pull

db-cards:
	@scripts/device.sh db-cards

db-reviews:
	@scripts/device.sh db-reviews $(limit)

db-reset:
	@scripts/device.sh db-reset

seed-review:
	@scripts/device.sh seed-review "$(CHAR)" "$(MODE)" "$(REPS)" 0

seed-learning:
	@scripts/device.sh seed-learning "$(CHAR)" "$(MODE)" "$(REPS)" "$(MINUTES)"

content:
	@scripts/device.sh content

content-bump:
	@scripts/device.sh content --bump

status:
	@scripts/device.sh status

hooks:
	@git config core.hooksPath .githooks && echo "pre-commit hook installed"

clean:
	@. scripts/env.sh && ./gradlew clean -q && echo "clean"

clean-shots:
	@rm -rf screenshots && echo "screenshots removed"
