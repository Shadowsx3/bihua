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

Bihua does not operate a server. Network access is used only when the learner
explicitly starts one of these optional actions:

1. Downloading an ML Kit handwriting-recognition model.
2. Downloading a local assistant model from the selected model source.
3. Local Wi-Fi sync between two devices on the same network.

Model downloads go from the source to the device. Local sync uses a short-lived
HTTP server on the local network and a pairing code. No analytics, advertising,
or crash-reporting SDK is included.

## Permissions

- Internet and network state: optional model downloads and local sync.
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
