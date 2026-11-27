#!/usr/bin/env bash
# Store-compliance audit for Android (Google Play) and iOS (App Store).
# Checks the manifests, privacy files, permissions, and listing documents that
# the stores actually ask about. Run after any permission, privacy, or store
# copy change.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
. scripts/env.sh

FAIL=0
ok() { printf "  ok:   %s\n" "$1"; }
bad() { printf "  FAIL: %s\n" "$1"; FAIL=$((FAIL + 1)); }
warn() { printf "  warn: %s\n" "$1"; }

echo "bihua compliance"

# ---------- shared documents ----------
for file in PRIVACY.md docs/privacy.html docs/ios-app-store-checklist.md \
            store/PLAY-CONSOLE-POLICY.md store/PLAY-UPLOAD.md store/ios-listing.md; do
  if [ -f "$file" ]; then ok "document $file"; else bad "missing $file"; fi
done

if grep -qi "no account" PRIVACY.md; then ok "policy states no account"; else bad "policy is missing no account"; fi
if grep -qi "ML Kit" PRIVACY.md; then ok "policy discloses ML Kit"; else bad "policy does not disclose ML Kit"; fi
if grep -qi "track" PRIVACY.md; then ok "policy covers tracking"; else warn "policy has no tracking paragraph"; fi
if grep -qi "iOS" PRIVACY.md && grep -qi "Android" PRIVACY.md; then
  ok "policy covers both platforms"
else
  bad "policy does not cover both platforms"
fi
if grep -Eqi "profile name|profile photo" PRIVACY.md docs/privacy.html; then
  bad "policy still mentions a profile name/photo"
else
  ok "policy has no profile name/photo"
fi

# ---------- Android ----------
APK="$APK_DEBUG"
if [ -f "$APK" ]; then
  BT="$(ls -d "$ANDROID_HOME"/build-tools/* 2>/dev/null | sort -V | tail -1)"
  BADGING_FULL="$("$BT/aapt" dump badging "$APK" 2>/dev/null)"
  BADGING="$(printf '%s\n' "$BADGING_FULL" | head -1)"
  CODE="$(printf '%s\n' "$BADGING" | sed -n "s/.*versionCode='\([^']*\)'.*/\1/p")"
  NAME="$(printf '%s\n' "$BADGING" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p")"
  SDK="$(printf '%s\n' "$BADGING_FULL" | sed -n "s/.*targetSdkVersion:'\([^']*\)'.*/\1/p" | head -1)"
  [ -n "$CODE" ] && ok "Android versionCode $CODE" || bad "Android versionCode missing"
  [ -n "$NAME" ] && ok "Android versionName $NAME" || bad "Android versionName missing"
  [ "$SDK" = "36" ] && ok "Android targetSdk 36" || bad "Android targetSdk ${SDK:-missing}"

  ALLOWED="android.permission.INTERNET
android.permission.ACCESS_NETWORK_STATE
android.permission.POST_NOTIFICATIONS
android.permission.WAKE_LOCK
android.permission.RECEIVE_BOOT_COMPLETED
android.permission.FOREGROUND_SERVICE
com.bassmd.bihua.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
  PERMISSIONS="$("$BT/aapt" dump permissions "$APK" 2>/dev/null | sed -n "s/^uses-permission: name='\([^']*\)'/\1/p")"
  while IFS= read -r permission; do
    [ -z "$permission" ] && continue
    if grep -Fxq "$permission" <<< "$ALLOWED"; then
      ok "Android permission $permission"
    else
      bad "unexpected Android permission $permission"
    fi
  done <<< "$PERMISSIONS"
  for forbidden in CAMERA RECORD_AUDIO READ_CONTACTS ACCESS_FINE_LOCATION \
                   READ_MEDIA_IMAGES READ_MEDIA_VIDEO MANAGE_EXTERNAL_STORAGE \
                   QUERY_ALL_PACKAGES SCHEDULE_EXACT_ALARM AD_ID; do
    if printf '%s\n' "$PERMISSIONS" | grep -qi "$forbidden"; then
      bad "forbidden Android permission $forbidden"
    fi
  done
  ok "Android forbidden permissions absent"
else
  warn "debug APK missing; run make check for the Android build checks"
fi

# ---------- iOS ----------
INFO=iosApp/Bihua/Info.plist
MANIFEST=iosApp/Bihua/PrivacyInfo.xcprivacy
if [ -f "$INFO" ]; then
  plist() { /usr/libexec/PlistBuddy -c "Print :$1" "$INFO" 2>/dev/null; }
  [ "$(plist ITSAppUsesNonExemptEncryption)" = "false" ] && ok "iOS export compliance (no non-exempt encryption)" || bad "iOS export compliance missing"
  [ "$(plist UIFileSharingEnabled)" = "false" ] && ok "iOS file sharing off" || bad "iOS file sharing must be off"
  [ "$(plist CADisableMinimumFrameDurationOnPhone)" = "true" ] && ok "iOS high-refresh plist key" || bad "iOS CADisableMinimumFrameDurationOnPhone missing"
  [ "$(plist UIRequiresFullScreen)" = "false" ] && ok "iOS iPad multitasking" || warn "iOS UIRequiresFullScreen should be false"
  [ "$(plist UIApplicationSupportsIndirectInputEvents)" = "true" ] && ok "iOS pointer/trackpad events" || bad "iOS UIApplicationSupportsIndirectInputEvents missing"
  ICON=iosApp/Bihua/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png
  if [ -f "$ICON" ]; then
    ICON_INFO="$(sips -g hasAlpha -g pixelWidth -g pixelHeight "$ICON" 2>/dev/null)"
    if grep -q "hasAlpha: no" <<< "$ICON_INFO" && grep -q "pixelWidth: 1024" <<< "$ICON_INFO" && grep -q "pixelHeight: 1024" <<< "$ICON_INFO"; then
      ok "iOS 1024 app icon (opaque)"
    else
      bad "iOS app icon must be 1024 x 1024 with no alpha"
    fi
  else
    bad "iOS 1024 app icon missing"
  fi
  for key in NSCameraUsageDescription NSMicrophoneUsageDescription \
             NSPhotoLibraryUsageDescription NSLocationWhenInUseUsageDescription \
             NSLocalNetworkUsageDescription NSUserTrackingUsageDescription; do
    if plist "$key" >/dev/null; then bad "iOS unexpected permission string $key"; fi
  done
  ok "iOS sensitive permission strings absent"
else
  bad "iOS Info.plist missing"
fi

if [ -f "$MANIFEST" ]; then
  grep -q "NSPrivacyTracking" "$MANIFEST" && grep -A1 "NSPrivacyTracking" "$MANIFEST" | grep -q "<false/>" \
    && ok "iOS privacy manifest tracking=false" || bad "iOS privacy manifest tracking missing/false"
  grep -q "NSPrivacyAccessedAPICategoryFileTimestamp" "$MANIFEST" && ok "iOS manifest file timestamps" || warn "iOS file timestamp reason missing"
  grep -q "NSPrivacyAccessedAPICategoryDiskSpace" "$MANIFEST" && ok "iOS manifest disk space" || warn "iOS disk reason missing"
  grep -q "NSPrivacyAccessedAPICategorySystemBootTime" "$MANIFEST" && ok "iOS manifest boot time" || warn "iOS boot-time reason missing"
else
  bad "iOS PrivacyInfo.xcprivacy missing"
fi

if grep -q "deploymentTarget" iosApp/project.yml && grep -q 'iOS: "16.0"' iosApp/project.yml; then
  ok "iOS deployment target 16.0"
else
  bad "iOS deployment target missing"
fi
if grep -q 'TARGETED_DEVICE_FAMILY: "1,2"' iosApp/project.yml; then
  ok "iOS iPhone and iPad target"
else
  bad "iOS TARGETED_DEVICE_FAMILY not 1,2"
fi

# Other-platform names in the iOS UI sources (Swift only for now; shared UI
# strings are audited as they move to commonMain).
if grep -RIn --include='*.swift' -E "Android|Play Store|Google Play" iosApp/Bihua >/dev/null 2>&1; then
  bad "other-platform wording in iOS Swift UI"
else
  ok "no Android/Play wording in iOS Swift UI"
fi

if grep -q "PrivacyInfo.xcprivacy" iosApp/project.yml; then
  ok "privacy manifest in project sources"
fi

# No photo, camera, microphone, contacts, location, or tracking code on iOS.
if grep -RIn --include='*.kt' --include='*.swift' -E "UIImagePickerController|PHPhotoLibrary|AVCaptureDevice|AVAudioRecorder|CNContactStore|CLLocationManager|ATTrackingManager|AppTrackingTransparency" composeApp/src/iosMain iosApp/Bihua >/dev/null 2>&1; then
  bad "iOS source uses a photo/camera/contact/location/tracking API"
else
  ok "iOS source has no photo/camera/contact/location/tracking APIs"
fi

# Dynamic Type: the Compose root must follow the system text size.
if grep -q "IosDynamicType" composeApp/src/iosMain/kotlin/com/bassmd/bihua/MainViewController.kt; then
  ok "iOS Dynamic Type bridge"
else
  bad "iOS Dynamic Type bridge missing"
fi

# The privacy policy must be reachable from inside the app.
if grep -Rq "shadowsx3.github.io/bihua/privacy.html" composeApp/src/androidMain composeApp/src/commonMain; then
  ok "in-app privacy policy link"
else
  bad "in-app privacy policy link missing"
fi

# Profile must stay name-free and pet-drawn on both platforms.
if grep -RIn --include='*.kt' -E "displayName|photoPath|saveProfilePhoto" composeApp/src/commonMain composeApp/src/androidMain composeApp/src/iosMain >/dev/null 2>&1; then
  bad "profile name/photo code returned"
else
  ok "profile has no name/photo code"
fi

echo
if [ "$FAIL" -ne 0 ]; then
  echo "compliance: $FAIL problem(s)" >&2
  exit 1
fi
echo "compliance: all checks passed"
