# iOS App Store checklist

Status: P0 simulator build. Nothing here is uploaded. This is the list of
questions App Store Connect and App Review ask, the answer Bihua will give, and
where the proof lives. Keep it current as the port moves through P3-P7.

Plan: `docs/ios-kmp-plan.md`. The local listing draft is `store/ios-listing.md`
(never committed; `store/` is private).

## App information

| Question | Answer | Evidence |
|---|---|---|
| App name (30 chars) | Bihua | `iosApp/Bihua/Info.plist` |
| Subtitle (30 chars) | Characters by hand, offline | `store/ios-listing.md` |
| Primary category | Education | `LSApplicationCategoryType` |
| Secondary category | None | — |
| Price | Free, no in-app purchase, no subscription, no donation link | Plan D9 |
| Copyright | © 2026 Shadowsx3 | `NSHumanReadableCopyright` |
| Bundle id | `com.bassmd.bihua` | Xcode target |
| Version / build | 1.0 / 1 | Xcode project |
| Devices | iPhone and iPad, universal, all orientations | `TARGETED_DEVICE_FAMILY=1,2`, Info.plist |
| Minimum iOS | 16.0 | deployment target |

## Age rating

| Question | Answer |
|---|---|
| Made for Kids / Kids Category | No |
| Age rating | 16+ |
| Sexual content or nudity | Infrequent/Mild (52 mature dictionary entries, off by default and gated) |
| Profanity or crude humour | No |
| Horror/fear | No |
| Violence | No |
| Alcohol, tobacco, drugs | No |
| Gambling | No |
| Medical/treatment information | No |
| User-generated content shared publicly | No |
| Messaging or chat | No |
| Advertising | No |
| Unrestricted web access | No |
| Shared Location (2026 descriptor) | No |
| Dating (2026 descriptor) | No |
| Social media capability | No, so the Declared Age Range API is not required |

Avoid 18+: several regions now block 18+ downloads without adult confirmation.

## Privacy

| Question | Answer | Evidence |
|---|---|---|
| Privacy policy URL | `https://shadowsx3.github.io/bihua/privacy.html`, now rewritten for Android and iOS (26 November 2026) | `docs/privacy.html`, `PRIVACY.md` |
| Support URL | `https://github.com/shadowsx3/bihua` | repository |
| Marketing URL | `https://shadowsx3.github.io/bihua/` | landing page |
| Data collected by the developer | None | Architecture: no developer analytics, no account, no ads |
| Third-party SDK collection | ML Kit may send limited diagnostics and usage analytics; disclosed in About, the policy, and the listing | About paragraph, `PRIVACY.md` |
| Data linked to the user | No account, so no identity link; declare App info/performance Diagnostics for ML Kit per Google's guidance | App Privacy answers |
| Tracking | No; no advertising identifier, no ATT prompt | `PrivacyInfo.xcprivacy` (`NSPrivacyTracking=false`) |
| Tracking domains | None | manifest |
| Privacy manifest | Shipped at `iosApp/Bihua/PrivacyInfo.xcprivacy`; the ML Kit pods ship their own | build bundle |
| Required-reason APIs | File timestamps (`C617.1`), disk space (`E174.1`), system boot time (`35F9.1`). Add user defaults (`CA92.1`) only if NSUserDefaults is used. Trim with the first ITMS-91053 report | manifest |
| iCloud backup | App data is written outside iCloud backup and `UIFileSharingEnabled=false` | Info.plist, Files seam |

## Encryption and export compliance

| Question | Answer |
|---|---|
| Does the app use encryption? | Standard HTTPS only for the optional model download |
| Exempt? | Yes, `ITSAppUsesNonExemptEncryption=false` in Info.plist |
| Custom cryptography? | No |
| Local sync? | Absent on iOS (Android only) |

## Content rights and third-party content

| Question | Answer | Evidence |
|---|---|---|
| Does the app contain third-party content? | Dictionary and character data come from open licensed sources; no third-party marks in the app | `THIRD_PARTY_NOTICES.md`, `CONTENT-LICENSE.md` |
| Is the content licensed? | Yes; licences are listed in the app under About → Licences and sources | About screen |
| Does the app show other platforms' names or logos? | No. Android-only features are absent, not labelled inside the app (2.3.10) | string audit test to add |
| Does the report composer print platform metadata? | On iOS it prints `iOS` and the device model, never Android | P4/P5 |

## Review notes (what App Review needs to know)

1. Bihua has no account, sign-in, purchase, subscription, or ads. Open the app
   and it is ready.
2. The first launch works offline: skip the optional setup, reach Home, and
   start a review.
3. The handwriting model is an optional download of about 20 MB. The app names
   the size and stays usable without it; shape and stroke-order feedback runs
   locally. This is data, not executable code (2.5.2).
4. The reader uses Google ML Kit on-device. It receives no learner text or ink
   off the device; diagnostics are disclosed in the policy and About.
5. Local sync, home-screen widgets, and foldable layouts are Android-only and
   are absent from the iOS build, not shown as "Android only" inside the app.
6. The report composer ships without crash-file attachment on iOS and says so.
7. The app is universal: test on iPhone and iPad, portrait and landscape.
8. Mature dictionary glosses are off by default and gated behind a
   confirmation; the rating is 16+.

## Screenshots and artwork

- iPhone 6.9-inch and iPad 13-inch, light and dark, from the frozen build.
- Real captures of Home, a review, the spell quest, Progress, and the report
  composer. No splash, no widget art, no Mnemonic palace.
- App icon: 1024 × 1024, no alpha. The current file is a 2× render of the
  existing Android icon; re-render from the vector source before submission.
- Feature graphic is Play-only and must not be reused as an App Store asset.

## DSA trader status (EU)

Required for EU distribution. Apps without it are removed from all 27 EU
storefronts. Decision: either declare trader (a public contact address appears
in the EU) or do not distribute in the EU. **Decision pending; first release may
ship without EU storefronts.**

## Build and submission gates

- Built with Xcode 26+ and the iOS 26 SDK (Xcode 27.2 here).
- No downloaded executable code; the content database is bundled.
- Public APIs only (Compose, Skia, UIKit, AVFoundation, ML Kit).
- `PrivacyInfo.xcprivacy` is inside the app bundle.
- `CADisableMinimumFrameDurationOnPhone=true` is in `Info.plist` for 120 Hz
  iPhones; Compose enforces it at launch.
- No other platform's name, icon, or imagery in the iOS UI or metadata.
- TestFlight build runs on a real iPhone and iPad before submission.
- `docs/privacy.html` names iOS storage, permissions, and no-iCloud behaviour.
- `store/ios-listing.md` is current and local.

## Known gaps for the first submission

- ML Kit's digital-ink framework has no arm64 iOS Simulator slice, so the
  simulator build omits the pod (`BIHUA_MLKIT=1 pod install` for device work).
  Rosetta 2 is installed; the iOS 26.5 runtime is still arm64-only, so an
  x86_64 simulator app cannot run. Recognition is proven on the connected
  iPhone (zh-Hans, 一); real-stroke and iPad accuracy still need the P4 seam.
- The Apple Developer Program is renewed through 27 November 2027 (team
  `DUST62RX27`). A paired iPhone 17e (iOS 27.2, Developer Mode on) is connected.
  The login keychain has an Apple Development identity, so a signed Debug build
  can run on it. TestFlight is unblocked once the archive is ready.
- The local privacy page and landing are updated for both platforms. The live
  GitHub Pages copy must be deployed before the listing is sent for review.
