# Play Store assets

- `icon-512.png` — app icon, 512 × 512
- `feature-graphic.png` — 1024 × 500
- `phone-1.png` … `phone-7.png` — 1080 × 1920, 9:16, captured from 1.0.2
- `tablet-10-*.png` — 2560 × 1440, 16:10 app inside a 16:9 frame, current 1.0.2
- `tablet-7-*.png` — 1920 × 1080, 16:10 app inside a 16:9 frame, current 1.0.2
- `app-shots/` — raw 1080 × 2400 captures of Home, Characters, detail, Practice, word practice, Progress, and the grade sheet
- `tablet-1-home.png` … `tablet-4-progress.png` — raw 2560 × 1600 tablet captures
- `listing.md` — names, descriptions, category, tags, contact
- `PLAY-UPLOAD.md` — exact Play Console upload checklist
- `translations/listing-en-US.csv` — source row for Play import; app UI is English-only
- `release-notes-1.0.2.txt` — Play closed-test release notes
- `phone-*-light.svg` — editable source frames for the phone shots
- `feature-graphic.svg`, `icon-square.svg` — editable sources

The app screenshots use a seeded showcase save (`tools/seed_showcase.py`), the
light theme, and the real app. Phone frames are rendered from the editable
`phone-*-light.svg` sources with headless Chrome:

```sh
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --headless=new --disable-gpu --hide-scrollbars \
  --window-size=1080,1920 \
  --screenshot=store/phone-1.png \
  "file://$PWD/store/phone-1-light.svg"
```

`phone-6` shows the answer/grade sheet; `phone-7` shows Progress. Tablet shots
are current 1.0.2 captures from a 2560 × 1600 Pixel Tablet emulator.
