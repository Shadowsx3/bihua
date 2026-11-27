# Privacy

Bihua is designed to work without an account and without sending learner data to
a Bihua server. This is the policy for the Android app and the iOS app. The
public page is `docs/privacy.html` and
<https://shadowsx3.github.io/bihua/privacy.html>.

## What stays on the device

- The chosen built-in pet avatar. Bihua does not ask for a name and does not open the photo library.
- Settings and study preferences
- Review history, schedule, XP, notes, study list, grade reasons, memory hooks,
  palace pieces, and word-practice history
- Reminder time and selected days, when study reminders are on
- Local crash files on Android, until they are sent or copied in a report
- Downloaded handwriting models
- Exported backup files and PDF reports chosen by the learner

The pet choice is part of the settings snapshot. It is included when the
learner exports a full backup or sends a settings sync, but only when the
learner starts that action. A legacy photo path from a 1.1.x settings file
may remain in the JSON, but current builds never read or display it.

### Android

Android cloud backup and device-to-device app-data transfer are disabled in the
manifest (`allowBackup="false"` plus exclusion rules for every data domain). The
operating system does not copy the database, notes, profile, models, or crash
files into a Google backup or OS migration. Bihua has its own export and local
sync instead.

### iOS

App data is written to the app's Application Support directory and excluded
from iCloud backup, so the promise above holds on iOS too. `UIFileSharingEnabled`
is false, so the app's data is not browsable in the Files app. Bihua has no
CloudKit, iCloud sync, push, or sign-in. The first iOS build has no home-screen
or lock-screen widgets. A backup leaves the app only when the
learner chooses Export and picks a destination in the system file picker.

Data remains on the device until the learner deletes it in the app, removes the
relevant item, or uninstalls the app.

## Network access

Bihua does not operate a server. Network access is used for:

1. **Handwriting model download.** ML Kit Digital Ink downloads the recognition
   model for the active script. The model runs on the device. The download uses
   HTTPS, may use mobile data, is about 20 MB per script, and is optional: the
   app stays usable without it and still gives local shape and stroke-order
   feedback. On Android the download is handled by Google Play services; on iOS
   it is handled by the Google ML Kit pod.
2. **Local Wi-Fi sync, Android only.** Two Android devices on the same network
   can exchange a backup over a short-lived TLS connection. While the sync
   screen is open, Bihua advertises a temporary service through Android network
   service discovery; the advertisement stops when sync ends. The sender shows
   a 10-character pairing code derived from a per-session certificate. The
   receiver types the code and pins that certificate, so another device on the
   network cannot read or impersonate the transfer. Traffic is direct
   device-to-device and is not relayed through the internet. iOS has no local
   sync and never asks for local network access.
3. **Read-aloud.** Bihua uses the text-to-speech engine installed on the device.
   Depending on the engine and downloaded voices, that engine's provider may
   process the text under its own policy. Bihua does not send read-aloud text to
   a Bihua server.
4. **Links the learner taps.** Source, licence, privacy, rating, and help links
   open the browser, Google Play, or the App Store. Those sites have their own
   policies. Bihua does not load web pages inside the app. The App Store rating
   row sends nothing until the learner taps it.

Bihua has no developer-run analytics, advertising, or crash-reporting service.
It does not include a separate analytics, crash-reporting, or advertising SDK.
Google ML Kit sends limited diagnostics and usage metrics to Google as described
below. A crash may be saved locally on Android in app-private storage. It stays
on the device until the learner includes it in a report and taps Send or Copy.
Nothing is sent automatically. iOS does not attach crash files to the report
composer.

## Permissions

### Android

- Internet and network state: handwriting-model downloads and local sync.
- Notifications: optional study reminders, off by default and requested only
  when the learner turns them on.
- Wake lock, receive boot completed, and foreground service: transitive
  WorkManager declarations for the one-shot reminder. No long-running
  foreground service is started.
- No storage, camera, microphone, contacts, location, SMS, call log, calendar,
  body sensor, activity recognition, exact alarm, overlay, accessibility,
  device-admin, package-query, all-files, or advertising ID permission is
  requested. The avatar is drawn by the app; no photo picker or photo-library
  permission is used.

### iOS

- Notifications: optional study reminders only; the system prompt appears when
  the learner turns them on.
- Files: access only through the system document picker, when the learner
  imports or exports a file.
- Speech: the system speech voices, used only for read-aloud.
- No camera, microphone, contacts, location, photo library, tracking, or local
  network permission is requested. Bihua does not show an App Tracking
  Transparency prompt because it does not track.
- The app has no account, no purchase, no chat, and no user-generated public
  content.

## Third-party SDKs and AI

On Android, Bihua includes Google ML Kit Digital Ink for handwriting candidates.
The first iOS build does not link ML Kit; its review is self-graded against the
reference and downloads no recognition model. The ink and
the recognition candidates are processed on the device. Bihua does not send the
ink itself, notes, review history, or study list to Google.

ML Kit may contact Google servers to receive bug fixes, updated models, and
hardware-accelerator compatibility information. It also sends metrics about its
performance and utilisation. According to Google's ML Kit data disclosure this
can include device information (manufacturer, model, OS version, ML
accelerators), app package name and version, per-installation identifiers,
performance metrics, configured recognition languages, API configuration,
feature input and output size, feature version, event type, and error codes.
ML Kit encrypts this data with HTTPS and does not transfer it to third parties.
Google processes it as a service provider for the SDK; Google's own privacy
policy also applies.

Links: [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure),
[ML Kit terms](https://developers.google.com/ml-kit/terms), and the
[Google Privacy Policy](https://policies.google.com/privacy).

Bihua does not include a separate analytics SDK, crash-reporting service, or
advertising SDK. ML Kit is the only third-party SDK that processes data for its
own diagnostics. The app does not include an advertising ID permission and does
not use the advertising identifier on either platform.

For Google Play Data safety, the Android ML Kit diagnostics above are declared as
collected by the app through the SDK and not shared. For the App Store privacy
labels, the same diagnostics are declared under App info and performance, not
linked to identity and not used for tracking. Bihua itself has no server and
receives none of that diagnostic data.

## Sharing, sale, and retention

Bihua does not sell, rent, or trade personal data. It does not share data for
advertising. The developer does not receive notes, review history, or study
data. A backup leaves the app only when the learner chooses a destination in the
system file picker. A settings sync on Android sends only the parts selected
directly to the other device on the local network; the developer does not
receive that transfer. A full backup or settings sync can include the profile
pet when set. Google ML Kit receives the diagnostic data described
above; Google does not receive notes or study history through Bihua.

Local data is retained until the learner deletes it, clears it through
Settings → Data → Start over, or uninstalls the app. Downloaded models remain in
app storage until the app is uninstalled or its storage is cleared. Android
crash files remain until a report is sent or copied, or the app prunes old
files. Exported backups remain wherever they were saved until deleted. If a
report is sent to the developer, it is kept only as long as needed to read and
respond, then deleted when practical. Bihua has no user accounts, so there is no
server account to delete.

## Backups

A full backup is created only when the learner chooses Export. It contains the
settings JSON, the local progress database, and the pet choice when
set. Downloaded models and local crash files are not included. The Android file
picker or the iOS Files sheet chooses the destination; Bihua does not upload the
file. The learner controls where it goes and who can open it.

Bihua does not encrypt the exported zip. Treat it like any other file that
contains study history and a profile: save it somewhere trusted and delete it
when it is no longer needed.

## Reports and PDFs

A progress report PDF and a practice-sheet PDF are built on the device. The
progress report can include the selected range, totals, level
progress, and the characters in that range. The practice sheet includes only the
chosen characters. On Android the system file picker chooses where each file
goes; on iOS the Files sheet or the share sheet does. Bihua does not upload
either one.

## Crash reports and feedback

On Android, a crash can be saved locally in app-private storage. It stays on the
device until the learner includes it in a report from Settings → About and taps
Send or Copy. The composer shows the app version, build type, course, day-start
hour, device manufacturer and model, Android release and SDK, locale, timestamp,
thread, the learner's words, and the crash trace if included. Send opens the
system share sheet; Copy puts the same text on the clipboard. Nothing is sent
automatically. The local crash file is deleted after a successful send or copy.
Crash files are not part of a backup or a settings sync.

On iOS, the report composer ships without crash-file attachment. It prints the
iOS version and device model, the course, the learner's words, and the chosen
kind, never an Android release or SDK. Send opens the system share sheet. The
developer receives only the text the learner chose to include.

## Study reminders

Study reminders are off by default. If turned on, the time and selected days are
stored on the device. At that time Bihua checks for due reviews or a new lesson
and posts a local notification with a count, such as "3 characters are waiting".
The notification does not include notes or review history, but the system may
show it on the lock screen unless the learner changes that in system settings.
The developer does not receive the schedule or the notification. Reminders can
be turned off in Settings → Reminders, in Android Settings, or in iOS Settings.

## Security

Local data sits in the app's private storage inside the platform sandbox. Model
downloads use HTTPS. Android local sync uses TLS with a per-session certificate
and a pairing code, and the traffic never passes through a Bihua server. iOS has
no local sync. Bihua does not operate a server that could hold study data. No
method of storage or transmission is completely secure, so keep the device
locked and protect any exported backup.

## Children and age

Bihua is a general study tool, not a children's app. It is intended for users
aged 16 and over and is not directed to children under 16. The developer does
not knowingly collect personal information from children under 16. If the
learner is under the age of digital consent in their country, Bihua should be
used only with a parent or guardian. Mature dictionary examples are off by
default and require a confirmation before they appear. There is no account to
delete; clearing the app's storage or uninstalling it removes the local data.

## Your choices and rights

- Delete individual data in the app where available, or use Settings → Data →
  Start over to clear cards, reviews, XP, and confusions. Notes, study lists,
  hooks, reasons, and settings stay until removed.
- Choose a different pet or clear the name in Settings → Profile.
- Uninstall the app to remove its local database, downloaded models, and local
  crash files.
- Turn off read-aloud, sound effects, notifications, and local sync (Android) at
  any time.
- Delete exported backups wherever they were saved.

Where the GDPR or UK GDPR applies, the learner has the right to access, correct,
delete, restrict, or object to processing, and the right to data portability.
Consent can be withdrawn at any time for actions that rely on it, such as
sending a report or starting a sync. Because Bihua has no account, most data is
already under the learner's control on the device. For data sent to the
developer, contact us to request access or deletion. There is also the right to
complain to a local data-protection authority.

## Legal bases

Where data-protection law requires a legal basis, the developer relies on its
legitimate interests in providing and securing a functioning app, and on consent
or an explicit request for optional actions: downloading a handwriting model,
starting a local sync, exporting a backup, or sending a report. ML Kit's
diagnostics are processed by Google under Google's terms as a service provider;
the developer does not receive them. Bihua does not use data for automated
decisions, profiling, or advertising.

## International transfers

Google may process ML Kit diagnostics in the United States and other countries
under its own safeguards and terms. If a report is sent to the developer, the
email or messaging provider chosen by the learner may process it in another
country. Android local sync is direct between the devices, and a backup goes
only to the destination chosen by the learner. Bihua does not transfer study
data internationally on its own.

## California privacy

Bihua does not sell personal information and does not share it for cross-context
behavioural advertising. It does not knowingly collect personal information from
consumers under 16. The app has no account; local data can be deleted in
Settings or by uninstalling. For a request about a report sent to the developer,
use the contact details below.

## Changes to this policy

This policy describes the current Android app and the iOS app. If a future
version adds a new network service, data source, or SDK, this page and both
store declarations (Google Play Data safety and App Store App Privacy) will be
updated before that release. The "Last updated" date at the top shows the
current version.

## Contact

Bihua is developed by Shadowsx3 in Uruguay. For privacy questions or a request
about a report sent to the developer, email
<manuelbuslon22@gmail.com> or open an issue at
<https://github.com/Shadowsx3/bihua/issues>. Do not include personal or study
data in a public issue.
