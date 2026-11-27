#!/usr/bin/env bash
# Build an App Store archive and export an IPA. Does not upload.
# Requires a signed-in Xcode account and a valid provisioning profile.
#   DEVELOPMENT_TEAM=DUST62RX27 scripts/ios-archive.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
. scripts/env.sh

: "${DEVELOPMENT_TEAM:?set DEVELOPMENT_TEAM to the Apple team id}"

OUT="${1:-build/ios}"
ARCHIVE="$OUT/Bihua.xcarchive"
EXPORT="$OUT/export"
mkdir -p "$OUT"

(cd iosApp && xcodegen generate >/dev/null && pod install >/dev/null 2>&1)

xcodebuild archive \
  -project iosApp/Bihua.xcodeproj \
  -scheme Bihua \
  -configuration Release \
  -destination "generic/platform=iOS" \
  -archivePath "$ARCHIVE" \
  -allowProvisioningUpdates \
  DEVELOPMENT_TEAM="$DEVELOPMENT_TEAM"

cat > "$OUT/ExportOptions.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>method</key>
  <string>app-store-connect</string>
  <key>signingStyle</key>
  <string>automatic</string>
  <key>teamID</key>
  <string>$DEVELOPMENT_TEAM</string>
  <key>uploadSymbols</key>
  <true/>
  <key>stripSwiftSymbols</key>
  <true/>
</dict>
</plist>
PLIST

xcodebuild -exportArchive \
  -archivePath "$ARCHIVE" \
  -exportPath "$EXPORT" \
  -exportOptionsPlist "$OUT/ExportOptions.plist" \
  -allowProvisioningUpdates

echo "Archive: $ARCHIVE"
echo "IPA:     $(find "$EXPORT" -name '*.ipa' | head -1)"
echo "Upload with Transporter or: xcrun altool --upload-app -f <ipa> -t ios --apiKey <key-id> --apiIssuer <issuer-id>"
