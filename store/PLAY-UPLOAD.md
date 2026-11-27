# Play Console upload — Bihua 1.1.2 (versionCode 23)

The AAB is uploaded by hand. The artifact in the slot above was rebuilt from
the final shared tree on 27 November 2026 and passed `make play-audit`
(versionCode 23, target 36, arm64-only, signed). Record the same SHA-256 in
`store/play-assets/README.md` before upload.

## Artifact

- AAB slot: `store/play-assets/upload-1.1.2/bihua-1.1.2.aab`
- AAB SHA-256:
  `416471f612735844dde0677290c3ee1d99d953f98f1f2e7ab54919fa4dfa5b06`
- Test APK: `docs/download/bihua-1.1.2.apk`
- Test APK SHA-256:
  `7e3bea1baf80fad3a67cea7ce4f216d5889bbe10c4ae00e120a95e9b2b2adfd4`
- Release name: `1.1.2 closed test`
- Release notes: `store/release-notes-1.1.2.txt` (`<en-US>` block)

Do not reuse a hash from an earlier build. A source or resource change makes the
old binary stale.

## Upload order

Upload in this order so the first phone image becomes the listing cover. The
renamed files live in `store/play-assets/upload-1.1.2/`.

### Phone (8)

1. `bihua-1.1.2-phone-01-home.png` — today's plan
2. `bihua-1.1.2-phone-02-practice.png` — writing 谢 with the guide
3. `bihua-1.1.2-phone-03-dictionary.png` — dictionary entry
4. `bihua-1.1.2-phone-04-worksheet.png` — print-ready sheets
5. `bihua-1.1.2-phone-05-widgets.png` — the real Today, Week ahead, and Word
   of the Day widgets
6. `bihua-1.1.2-phone-06-spell-quest.png` — spell result
7. `bihua-1.1.2-phone-07-progress.png` — exact ranges, level progress, heatmap
8. `bihua-1.1.2-phone-08-report.png` — the local report composer

### Tablet (6 at each size)

7-inch 1920 × 1080 and 10-inch 2560 × 1440: Home, Practice, Characters, Spell,
Progress, and Report.

### Foldable (7)

1:1 Home, Practice, Spell, Progress, and Report; one 4:3 inner card; one 10:16
outer card.

## Before pressing publish

- Confirm the privacy policy at
  <https://shadowsx3.github.io/bihua/privacy.html> is the 1.1.2 policy.
- Confirm the Data safety answers in `PLAY-CONSOLE-POLICY.md`.
- Confirm the phone widget card matches the real widget layout.
- Confirm no store image shows the Mnemonic palace.
- Confirm the AAB and uploaded images come from the final build.
