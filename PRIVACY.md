# Privacy

Bihua is designed to work without an account and without sending learner data to
a server.

## What stays on the device

- Profile name and optional photo
- Settings and study preferences
- Review history, schedule, XP, notes, and study list
- Word-practice history
- Exported backup files chosen by the learner

## Network access

Bihua does not operate a server. Network access is used for:

1. Downloading the ML Kit handwriting-recognition model for the active script.
   This may happen automatically at start-up, after a script switch, or on the
   first handwriting check. Models can also be prepared from Settings.
2. Local Wi-Fi sync between two devices on the same network.

Model downloads go from Google Play services to the device. Local sync uses a
short-lived HTTP server on the local network and a pairing code.

Bihua includes no developer-run analytics, advertising, or crash-reporting SDK.
Google ML Kit sends limited diagnostics and usage metrics to Google as described
in the app's privacy policy and Google's ML Kit data disclosure.

## Permissions

- Internet and network state: handwriting-model downloads and local sync.
- No storage or camera permission is requested. The system photo picker returns
  only the image the learner chooses for their profile.

## Backups

Backups are created only when the learner chooses Export. The zip contains the
settings JSON and the local progress database. The learner decides where the
file is saved and with whom it is shared.

## Children

Bihua does not create accounts or collect personal information. A parent or
guardian controls the device and any exported backup.

## Contact

This is a private project. Use the repository owner's GitHub profile for
questions.
