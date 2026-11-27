# iOS release runbook (first build)

This is the local, reproducible path from the tested tree to App Store Connect.
Nothing here uploads by itself; the upload is a separate, confirmed action.

## Before archiving

1. `make verify-ui` is green on iPhone and iPad simulators.
2. `make ios-content-check` is green.
3. `make compliance` is green; no widget `.appex`, no App Group, no ML Kit in
   the first build.
4. The live policy matches the binary:
   - Privacy policy: <https://shadowsx3.github.io/bihua/privacy.html>
   - User privacy choices: <https://shadowsx3.github.io/bihua/privacy.html#rights>
5. App Store Connect:
   - **Data Collection:** “No, we do not collect data from this app.”
   - **Age rating:** 16+ (mature glosses are off by default and gated).
   - Privacy policy URL and choices URL above.
   - Screenshots from `.qa/ios/store/iPhone-17-Pro-Max/` and
     `.qa/ios/store/iPad-Pro-13-inch-M5/`, captured from the test build.
6. Clean install test with no network: onboarding → Home → Learn 5 new →
   write → reveal → grade → relaunch → progress kept.

## Archive and export

```bash
DEVELOPMENT_TEAM=DUST62RX27 scripts/ios-archive.sh
```

The script generates the Xcode project, archives `Release`, writes an
`ExportOptions.plist` for App Store Connect, and exports the IPA to
`build/ios/export/`.

If Xcode reports “No Accounts”, sign in under Xcode → Settings → Accounts and
enable automatic signing for `com.bassmd.bihua`. The first iOS build needs no
App Group and no special capability.

## Upload

Use Xcode Organizer, Transporter, or `altool` with an App Store Connect API key.
The key file and issuer id stay outside the repository (`*.p8` is ignored).
The key id from the filename is only half of the credential; the issuer id is
in App Store Connect → Users and Access → Integrations.

```bash
xcrun altool --upload-app -f build/ios/export/Bihua.ipa -t ios \
  --apiKey "$ASC_KEY_ID" --apiIssuer "$ASC_ISSUER_ID"
```

Then create an internal TestFlight group and test on the device that will be
used for review, including a clean install with no network.

## What not to claim

The first iOS build has no widgets, local sync, foldables, PDF reports, spell
quest, compare, ML Kit reader, or sounds. The listing and screenshots must not
show or mention them. Post-1.0 work is tracked in `docs/ios-feature-parity.md`.
