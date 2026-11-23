# Privacy

Bihua is designed to work without an account and without sending learner data to
a server.

## What stays on the device

- Profile name and optional photo
- Settings and study preferences
- Review history, schedule, XP, notes, and study list
- Word-practice history
- Reminder time and selected days, when study reminders are on
- Exported backup files chosen by the learner

The profile name and photo are part of the settings snapshot. They are included
when the learner exports a full backup or sends a settings sync, but only when
the learner starts that action.

## Network access

Bihua does not operate a server. Network access is used for:

1. Downloading the ML Kit handwriting-recognition model for the active script.
   This may happen automatically at start-up, after a script switch, or on the
   first handwriting check. Models can also be prepared from Settings.
2. Local Wi-Fi sync between two devices on the same network.

Model downloads go from Google Play services to the device. Local sync uses a
short-lived TLS server on the local network. The sender shows a 10-character
pairing code derived from a per-session certificate. The receiver types the code
and pins that certificate, so another device on the network cannot read or
impersonate the transfer. The traffic is direct device-to-device and is not
relayed through the internet.

Version note: Bihua 1.0.4 used a short-lived HTTP connection for local sync.
Bihua 1.0.5 replaces it with TLS and certificate pinning.

Read-aloud uses the text-to-speech engine installed on the device. Depending on
the engine and downloaded voices, that engine's provider may process the text
under its own policy. Bihua does not send read-aloud text to a Bihua server.

Bihua includes no developer-run analytics, advertising, or crash-reporting SDK.
It does not include Firebase, Crashlytics, or an advertising ID permission.
Google ML Kit sends limited diagnostics and usage metrics to Google as described
in the app's privacy policy and Google's ML Kit data disclosure.

## Permissions

- Internet and network state: handwriting-model downloads and local sync.
- No storage, camera, microphone, contacts, or location permission is requested.
  The system photo picker returns only the image the learner chooses for their
  profile.

## Backups

Backups are created only when the learner chooses Export. The zip contains the
settings JSON, the local progress database, and the profile name and photo when
set. The learner decides where the file is saved and with whom it is shared.

## Study reminders

Study reminders are off by default. If the learner turns them on, the time and
selected days are stored on the device. At that time Bihua checks for due reviews
or a new lesson and posts a local notification. The developer does not receive
the schedule or the notification. Reminders can be turned off in Settings or in
Android Settings.

## Children

Bihua does not create accounts or collect personal information. It is not
directed to children under 13. Mature dictionary examples are off by default and
require a confirmation. A parent or guardian controls the device and any
exported backup.

## Contact

This is a private project. Use the repository owner's GitHub profile for
questions.
