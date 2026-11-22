# Recipes source scripts/env.sh, which uses bash syntax. Use bash on every platform.
SHELL := /bin/bash

# Shortcuts for the build, check, and device loop. Run `make help` for the list.

.PHONY: help check quick test lint audit smoke apk verify install run log crash shot status hooks clean clean-shots \
	devices ui find tap type draw swipe key release install-release \
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
	@echo "make apk       debug APK only"
	@echo "make verify    verify the existing APK"
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

apk:
	@. scripts/env.sh && ./gradlew :app:assembleDebug --console=plain -q && echo "apk ok"

verify:
	@scripts/verify-apk.sh

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

release-check:
	@python3 tools/check_release.py

store-images:
	@scripts/render-store-assets.sh

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
