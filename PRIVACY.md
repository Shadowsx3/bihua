# Privacy

Bihua is designed to work without an account and without sending learner data to
a Bihua server. The public policy is `docs/privacy.html` and
<https://shadowsx3.github.io/bihua/privacy.html>.

## What stays on the device

- Profile name and optional photo (the photo is copied into app-private storage)
- Settings and study preferences
- Review history, schedule, XP, notes, study list, grade reasons, memory hooks,
  palace pieces, and word-practice history
- Reminder time and selected days, when study reminders are on
- Local crash files, until they are sent or copied in a report
- Downloaded handwriting models
- Exported backup files and PDF reports chosen by the learner

The profile name and photo are part of the settings snapshot. They are included
when the learner exports a full backup or sends a settings sync, but only when
the learner starts that action.

Android cloud backup and device-to-device app-data transfer are disabled in the
manifest (`allowBackup="false"` plus exclusion rules for every data domain).
The operating system does not copy the database, notes, profile, models, or
crash files into a Google backup or OS migration. The app has its own export
and sync instead.

## Network access

Bihua does not operate a server. Network access is used for:

1. Downloading the ML Kit handwriting-recognition model for the active script.
   This may happen automatically at start-up, after a script switch, or on the
   first handwriting check. Models can also be prepared from Settings.
2. Local Wi-Fi sync between two devices on the same network.

Model downloads go from Google Play services to the device over HTTPS. Local
sync uses a short-lived TLS server on the local network. While the sync screen
is open, Bihua advertises a temporary service through Android network service
discovery; the advertisement stops when sync ends. The sender shows a
10-character pairing code derived from a per-session certificate. The receiver
types the code and pins that certificate, so another device on the network
cannot read or impersonate the transfer. Traffic is direct device-to-device and
is not relayed through the internet.

Version note: Bihua 1.0.4 used a short-lived HTTP connection for local sync.
Bihua 1.0.5 replaced it with TLS and certificate pinning. Current builds keep
that design.

Read-aloud uses the text-to-speech engine installed on the device. Depending on
the engine and downloaded voices, that engine's provider may process the text
under its own policy. Bihua does not send read-aloud text to a Bihua server.

Bihua has no developer-run analytics, advertising, or crash-reporting service.
It does not include a separate analytics, crash-reporting, or advertising SDK.
Google ML Kit sends limited diagnostics and usage metrics to Google as described
in the public policy and Google's ML Kit data disclosure. A crash may be saved
locally in app-private storage. It stays on the device until the learner
includes it in a report and taps Send or Copy. Send opens Android's share sheet;
Copy uses the clipboard. Nothing is sent automatically.

## Permissions

- Internet and network state: handwriting-model downloads and local sync.
- Notifications: optional study reminders, off by default and requested only
  when the learner turns them on.
- Wake lock, receive boot completed, and foreground service: transitive
  WorkManager declarations for the one-shot reminder. No long-running
  foreground service is started.
- No storage, camera, microphone, contacts, location, SMS, call log, calendar,
  body sensor, activity recognition, exact alarm, overlay, accessibility,
  device-admin, package-query, all-files, or advertising ID permission is
  requested. The system content picker returns only the chosen profile photo.

## Reports and PDFs

A progress report PDF and a practice-sheet PDF are built on the device. The
progress report can include the profile name, the selected range, totals, level
progress, and the characters in that range. The practice sheet includes only the
chosen characters. The Android file picker chooses where each file goes; Bihua
does not upload either one.

## Backups

Bihua backups are created only when the learner chooses Export. The zip contains
the settings JSON, the local progress database, and the profile name and photo
when set. Downloaded models and local crash files are not included. The learner
decides where the file is saved and with whom it is shared. Bihua does not
encrypt the exported zip, so the learner should treat it as personal data.

## Study reminders

Study reminders are off by default. If the learner turns them on, the time and
selected days are stored on the device. At that time Bihua checks for due reviews
or a new lesson and posts a local notification with a count, not the learner's
notes or review history. The developer does not receive the schedule or the
notification. Reminders can be turned off in Settings or in Android Settings.

## Children

Bihua is not directed to children. The Play listing targets users aged 16 and
over. It does not create accounts, run advertising, or knowingly collect
personal information from children under 16. Mature dictionary examples are off
by default and require a confirmation.

## Contact

Email: <manuelbuslon22@gmail.com>

Public issues: <https://github.com/Shadowsx3/bihua/issues> (do not include
personal or study data in a public issue).
