#!/usr/bin/env bash
# Shared environment for every script in this repo. Source it, don't run it.
#   source scripts/env.sh
#
# Gradle 8.11 cannot run on the JDK 25 bundled with Android Studio, so use JDK 17.

BIHUA_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export BIHUA_ROOT

if [ -z "${JAVA_HOME:-}" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "17'; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
fi

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/build-tools/36.0.0:$PATH"

APK_DEBUG="$BIHUA_ROOT/composeApp/build/outputs/apk/debug/composeApp-debug.apk"
APP_ID="com.bassmd.bihua"
export APK_DEBUG APP_ID
