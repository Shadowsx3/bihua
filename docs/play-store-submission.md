# Play Store submission

This is the working checklist for the 1.1.2 upload. It records what the app
actually does so the Play Console answers stay honest. Update it with the
release checklist whenever the manifest, SDKs, or data flows change.

## Build identity

- Package: `com.bassmd.bihua`
- Version: `1.1.2` (`versionCode 23`)
- Min SDK: 29 (Android 10)
- Target SDK: 36 (Android 16)
- ABI: `arm64-v8a` only; Play accepts a 64-bit-only app.
- Signed with the local release key (`keystore.properties`), never committed.
- Upload artifact: `store/play-assets/upload-1.1.2/bihua-1.1.2.aab`
  (SHA-256 `e66b5b85…`)
- Test APK: `docs/download/bihua-1.1.2.apk` (SHA-256 `1bcc530d…`)
- Privacy policy: `https://shadowsx3.github.io/bihua/privacy.html`
- Support: the GitHub repository and its issue tracker.

## Reviewer notes

Paste these into App access / review notes:

- **No account or credentials.** There is no sign-in, subscription, purchase,
  or test account. Every feature is available on first launch.
- **A clean offline device is still usable.** The handwriting model is an
  optional one-time download. On a fresh device with no network it starts in
  the background, never blocks the primary action, and setup can be skipped.
  Home still opens and the writing queue still runs. If the reader is not
  available, Check still returns local stroke and shape feedback and Show
  answer is always there. Spell quest scenes can be answered from the
  grimoire, which ships with the app and needs no download.
- **A quiet model notice is expected.** It explains the optional download; it
  is not an error and does not stop the learner. The download can complete
  later when the device is online.
- **Crash reports are local and user-initiated.** A crash may be saved in
  app-private storage. Nothing leaves the device until the learner opens
  Settings → About → Report, reviews the text, and taps Send or Copy. Send
  opens Android's share sheet so the learner chooses the app; there is no developer crash
  service.
- **The app is arm64-only.** Use a 64-bit device or emulator; there is no
  32-bit ABI by design to keep the local model size down.

## Permissions

| Permission | Why | Sensitive? |
|---|---|---|
| `INTERNET` | ML Kit model download; local Wi-Fi sync | No |
| `ACCESS_NETWORK_STATE` | Choose the Wi-Fi sync path and report offline state | No |
| `POST_NOTIFICATIONS` | Optional study reminder, off by default | Runtime permission |
| `WAKE_LOCK` | WorkManager runs the one-shot reminder | Normal |
| `RECEIVE_BOOT_COMPLETED` | WorkManager survives a reboot | Normal |
| `FOREGROUND_SERVICE` | Transitive WorkManager declaration; the reminder worker is a short one-shot job and does not start a long-running foreground service | Normal |

No storage, camera, microphone, contacts, location, SMS, call log, calendar,
body sensors, activity recognition, exact-alarm, overlay, accessibility,
device-admin, package-query, or all-files permission is requested. Profile
photos use the Android system photo picker, so no photo permission is needed.

## Data safety answers

The app has no account, no developer-run analytics, no advertising SDK, and no
Bihua server.

- **Does the app collect or share user data?** The app itself does not collect
  data. Google ML Kit on-device handwriting recognition may send limited
  diagnostics and usage metrics to Google, as disclosed in the privacy policy
  and Google's ML Kit data disclosure. Declare **Diagnostics** (and any
  ML Kit-reported device/usage metrics) consistently with the current ML Kit
  disclosure; do not claim "no data collected" without checking the current
  SDK disclosure.
- **Data types:** diagnostics/usage metrics, if the current ML Kit disclosure
  requires them. No name, email, phone, address, contacts, calendar, photos,
  files, location, health, financial, messages, or web browsing data is sent to
  a Bihua server.
- **Data shared:** Google ML Kit as a service provider for the handwriting
  model and diagnostics. Local sync is device-to-device over a short-lived TLS
  connection; it is not relayed through the internet.
- **Encrypted in transit:** yes for model downloads and local sync (TLS with a
  per-session certificate pin).
- **Data deletion:** there is no account and no server profile. The learner can
  clear progress in Settings, clear app storage in Android, or delete exported
  backups. There is nothing for the developer to delete on a server.
- **Android backup:** disabled in the manifest (`allowBackup="false"` plus
  exclusion rules for every data domain). Progress, notes, profile, models, and
  crash files are not copied into Google backup or OS migration. The app's own
  Export and local sync are the only transfer paths.
- **Crash files stay on the device:** a crash may be saved in app-private
  storage. It is uploaded only if the learner opens Settings → About → Report,
  reviews it, and taps Send or Copy. That opens Android's share sheet; it
  is not a developer crash service and is not automatic. Local crash files are
  excluded from backups and settings sync.
- **Data collection is optional:** handwriting recognition is used for grading
  hints, never to press the grade button. The learner can grade without it.
- The reading model is a one-time download from Google Play services. It may
  use mobile data on a new install; after that it runs offline. No learner text
  or stroke data is sent to a Bihua server.

## Content rating and audience

- Target audience: **16+**. The app is not designed for children under 16 and
  has no child-directed ads, purchases, or social features.
- Content rating questionnaire: learning tool; no graphic violence or
  depictions, no user-generated public content, no gambling. Fantasy vocabulary
  includes words such as "blood", "death", or "poison" as dictionary meanings;
  there are no graphic scenes. If the mature dictionary's examples include
  sexual or explicit language, declare **Mature/suggestive themes** and the
  appropriate language category in the IARC questionnaire; do not select
  "Everyone" just because the toggle is off. The default experience is
  non-mature, and the opt-in is behind a confirmation dialog.
- The app is **16+**. Do not opt into the Families program.
- No ads, no in-app purchases, no subscriptions, no loot boxes.
- No user-to-user chat, public profiles, or content sharing. Study lists and
  backups stay local or go to the learner's own file provider.
- Privacy policy and data safety links are present in the app and on the
  landing page.

## Foreground service and background work

- Reminders use a **one-shot WorkManager** job; the worker is short and does
  not call `setForeground`.
- Widget refresh uses one inexact `AlarmManager` alarm at the next day-part or
  study-day boundary, plus a six-hour XML fallback. No exact-alarm permission,
  no polling service.
- If Play asks about foreground-service use, answer that the app does not run a
  long-running foreground service; the permission is a transitive WorkManager
  declaration. If the Console refuses the upload because of the permission,
  remove the unused declaration and service with a manifest override rather
  than adding a service type. Do not add a data-sync FGS just to silence the
  prompt.
- Do not add a background location, media playback, camera, microphone, or
  data-sync foreground service without a policy review.

## Store listing

- Release notes (1.1.2): “Today and Week ahead widgets, All time and a year
  heatmap in Progress, recent mistakes and recently learned that follow the
  selected range, a report composer that keeps crash files on your phone until
  you send, and a splash that no longer jumps. Landscape phones use the
  two-page Home and Practice layouts, and the spell quest, its draw screen, and
  the grimoire use their wide spreads.”
- Title, short description, full description, and screenshots match the
  current build. Do not claim features that are not in 1.1.2.
- The screenshot set is in `store/play-assets/upload-1.1.2/` (local, gitignored):
  eight phone cards, six captioned 7-inch and six captioned 10-inch tablet
  cards, seven fold cards, the feature graphic, and the icon. Progress and the
  report composer appear on phone, tablet, and fold.
- The signed test APK on the landing page is for testers, not the Play build.
- Upload the AAB to an internal testing track first, install from Play, and run
  the release smoke. Promote only after that smoke passes.

## Policy checks before upload

- [ ] Target SDK 36 and versionCode 23.
- [ ] `make check`, `make lint`, and the release journeys are green, including `offline-first-run`.
- [ ] AAB is signed and the Play upload certificate matches the upload key.
- [ ] No debug build, `android:debuggable`, test-only flag, or debug signing in
      the uploaded AAB.
- [ ] No new permission, SDK, network endpoint, analytics, ad, or purchase was
      added without updating this file and the privacy policy.
- [ ] Data safety and content rating answers match the shipped behavior.
- [ ] Privacy policy URL, support URL, and developer contact are live.
- [ ] App access instructions say "No account. All features are available on
      first launch." Reviewer notes explain that a clean offline device can
      skip setup and start a review while the optional reader downloads.
- [ ] Ads declaration says "No ads".
- [ ] In-app purchases declaration says "No".
- [ ] News, health, finance, government, and gambling declarations are "No".
- [ ] Families policy is not selected (16+ audience).
- [ ] The store description does not promise clinical or literacy outcomes.
- [ ] Store screenshots and feature graphic are current and do not show a
      different build.
- [ ] Play App Signing is configured; the local keystore is backed up offline.
- [ ] A staged rollout plan and rollback build are ready.

## Review notes for Google

- No login is required. Open the app, choose a script, and start.
- Handwriting runs on-device with ML Kit; the review device may download a
  model on first use. If the model is unavailable, the app still lets the
  learner grade.
- Study reminders are off by default and use a local notification.
- Sync is optional and only between two devices on the same local network; it
  uses a pairing code and a pinned TLS certificate.
- The spell quest is optional extra practice and is not graded or scheduled.

## Known manual QA before public rollout

- Physical stylus pressure and palm rejection.
- Full TalkBack sweep at 2×, including the quest note and the bottom bar.
- Launcher widget add/resize on a real launcher and low-RAM device.
- System file picker for backup export/import and worksheet PDF.
- Play-installed release upgrade from the previous public version.
