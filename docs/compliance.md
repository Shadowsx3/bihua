# Store compliance

One place for the answers Google Play and the App Store ask, the code or
document that proves them, and the checks that still need a person. The
automated part is `make compliance`; the manual part is listed at the end.

Status: Android 1.1.2 is built and held until iOS is submitted. The iOS client
is a working study app: onboarding, real content, persisted FSRS progress,
review, Characters, Dictionary, Practice, Word practice, Progress, Settings,
study lists, speech, reminders, and JSON backup export/import. Widgets, sync,
PDF, spell quest, compare, the ML Kit reader, and sounds are absent. The iOS 1.0
binary does not link ML Kit, so App Privacy is **Data Not Collected**.

## Product facts (both stores)

| Question | Answer |
|---|---|
| Account or sign-in | None |
| Name collected | None. The app has no name field. |
| Photo collected | None. Avatars are twelve Compose-drawn pets; no photo library or picker. |
| Ads or ad ID | None |
| In-app purchases / subscription / donation | None |
| Developer analytics or crash service | None |
| Third-party SDK that processes data | Google ML Kit Digital Ink, on-device recognition; limited diagnostics and usage metrics |
| Tracking | No. No ATT prompt, no advertising identifier, no tracking domains |
| User-generated public content | None. Notes and lists stay local. |
| Widgets | Android only. Deferred from the first iOS build; no App Group, no `.appex`, no widget screenshots. |
| Account deletion | Not applicable; there is no account |
| Age rating | 16+; mature dictionary glosses off by default and gated |
| Encryption | Standard HTTPS for the optional model download; `ITSAppUsesNonExemptEncryption=false` |

## Google Play

| Requirement | Answer | Proof |
|---|---|---|
| Privacy policy URL | `https://shadowsx3.github.io/bihua/privacy.html` | `PRIVACY.md`, `docs/privacy.html` (deploy `docs/` before upload) |
| Data safety | No developer collection. Android ML Kit diagnostics declared, collected, not shared; encryption yes; deletion by Start over/uninstall | `store/PLAY-CONSOLE-POLICY.md` |
| Permissions | Internet, network state, notifications, WorkManager declarations only; no camera/mic/location/photos/contacts/all files/exact alarm/ad ID | `make compliance`, `make play-audit` |
| Ads | No ads, no ad ID | Manifest, listing |
| Families | Not directed to children; 16+ | Listing, policy |
| Account deletion | No account | Policy |
| Target API | 36 | `composeApp/build.gradle.kts` |
| Signing / ABI | Upload-key signed, arm64-only AAB | `make play-audit` |
| Store listing | Real screenshots, no Mnemonic palace, no misleading claims | `store/play-assets/` |

## App Store

**App Store Connect → App Privacy → Data Collection: choose “No, we do not
collect data from this app” for the first iOS build.** The binary contains no
ML Kit pod, no analytics SDK, no ads, no account, and no tracking. The policy
must say the same thing (it does: the iOS 1.0 build does not include ML Kit).
If a later build links ML Kit, change the answer to Yes → Diagnostics, not
linked, not tracking, purposes App Functionality and Analytics, and update the
policy in the same release.


| Requirement | Answer | Proof |
|---|---|---|
| Privacy policy | Same page, iOS-aware | `PRIVACY.md`, `docs/privacy.html` |
| App Privacy | **Data Not Collected.** The iOS 1.0 build does not link ML Kit, has no account, analytics, ads, or tracking. Android separately declares ML Kit **Diagnostics** in Play Data safety. | `docs/ios-app-store-checklist.md`, `PRIVACY.md` |
| Privacy manifest | `PrivacyInfo.xcprivacy`, tracking false; user defaults, file timestamps, disk space, boot time | Built bundle, `make compliance` |
| Dynamic Type | All twelve content-size categories, xS to AX5, live update | `IosDynamicType`, `iosSimulatorArm64Test`, Home screenshot at Accessibility Large |
| Pointer / trackpad | Indirect input events on | `Info.plist`, `make compliance` |
| Permissions | Notifications only; no camera, microphone, photos, location, local network, ATT | `Info.plist`, `make compliance` |
| Other platforms | The iOS UI has no "Android", Play badge, or Android-only row | Swift scan in `make compliance`; shared UI string audit in P5 |
| Age rating | 16+, infrequent/mild sexual content and nudity; Shared Location no, Dating no | Checklist |
| Export compliance | Standard HTTPS only | `Info.plist` |
| IAP / login | None | Listing |
| Account deletion | Not applicable | Checklist |
| DSA trader | Decision pending; EU storefronts may be omitted for 1.0 | Checklist |
| HIG | Native tab bar/navigation, Dynamic Type, VoiceOver, Reduce Motion, safe areas, keyboard, system sheets | P3/P6 design checklist |
| Model download disclosure | Optional reader, size named, app usable offline | Shared notice in P4/P5 |
| TestFlight | Signed build on real iPhone and iPad; account renewed through Nov 2027 | Device runs |

## Automated audit

```bash
make compliance
```

It checks the store documents, policy wording (no name/photo), Android
version/target/permissions against an allowlist, iOS Info.plist privacy and
export keys, sensitive usage strings, the privacy manifest, the iPhone/iPad
target, the 1024 icon, pointer events, the Dynamic Type bridge, the absence of a widget extension and
App Group in the first build, the absence of
photo/camera/contact/location/tracking APIs in iOS sources, and other-platform
wording in Swift.

It does not prove the App Privacy/Data safety answers are accepted by the
stores, replace a real-device pass, or run the UI accessibility matrix.

## Manual before submission

0. Follow `docs/ios-release.md` for the archive, export, TestFlight, and the
   exact App Store Connect answers.

1. Deploy `docs/` so the privacy URL is current.
2. Run the signed build on a real iPhone and iPad; TestFlight internal test.
3. Walk the HIG checklist with VoiceOver and 2.0x text; fix any clipped screen.
4. Confirm the App Privacy answers and the Play Data safety form match this page.
5. Decide DSA trader status or leave EU storefronts out of 1.0.
6. Capture final screenshots from the frozen build; no personal names, widgets,
   or Mnemonic palace.
7. Run `make check`, `make lint`, `make compliance`, `make play-audit`.
