# Play Store assets

## Current set — 1.1.1 (drawn spell shots, final listing set)

The final upload set lives in `play-assets/`:

- `play-assets/phone/` — six 1080 × 1920 cards: Home, Practice, Dictionary,
  Practice sheets, Widgets, and the spell quest
- `play-assets/tablet/` — 7-inch 1920 × 1080 and 10-inch 2560 × 1440 Home,
  Practice, and spell captures
- `play-assets/fold/` — three 1:1 cards (Home, Practice, spell), one 4:3 inner
  card, and one 10:16 outer card
- `play-assets/icon/icon-512.png` — 512 × 512 launcher icon
- `play-assets/banner/feature-graphic-1024x500.png` — 1024 × 500 banner
- `play-assets/listing.md` — short and full description, category, tags, contact
- `play-assets/full-description.txt` — plain-text description for the Play box
- `play-assets/README.md` — specs, captions, and rebuild commands

The editable sources are `marketing/cards.html`, `marketing/cards-wide.html`,
`marketing/cards-fold.html`, and `marketing/feature-graphic.html`.
`marketing/README.md` lists the raw captures and the design sources.
The upload staging folder is `play-assets/upload-1.1.1/`.

## Older sets (keep for history, do not upload)

- `phone-1.png` … `phone-7.png` — 1.0.4 phone set
- `tablet-10-*.png`, `tablet-7-*.png` — 1.0.4 tablet sets
- `foldable-*.png` — older foldable set
- `app-shots/` — raw 1.0.4 phone captures
- `tablet-1-home.png` … `tablet-4-progress.png` — raw 1.0.4 tablet captures
- `icon-512.png` — current icon, duplicated into `play-assets/icon/`
- `feature-graphic.png` — current banner, duplicated into `play-assets/banner/`
- `listing.md` — current listing copy, duplicated into `play-assets/`
- `PLAY-UPLOAD.md` — 1.0.7 upload checklist
- `PLAY-CONSOLE-POLICY.md` — Data safety, rating, and declaration answers
- `release-notes-1.0.2.txt` … `release-notes-1.0.7.txt` — closed-test notes
- `phone-*-light.svg`, `feature-graphic.svg`, `icon-square.svg` — older editable
  sources

The app screenshots use a seeded showcase save (`tools/seed_showcase.py`), the
light theme, and a real build. Do not upload a capture that shows a QA profile
name, a debug badge, or the 1.0.6 build.

## Rebuild

```sh
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=1080,1920 \
  --screenshot=store/marketing/shot-1.png \
  "file://$PWD/store/marketing/cards.html?card=1"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=1600,1300 \
  --screenshot=store/marketing/tablet-shot.png \
  "file://$PWD/store/marketing/cards-wide.html?device=tablet&card=home"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=2208,2208 \
  --screenshot=store/marketing/fold-shot.png \
  "file://$PWD/store/marketing/cards-wide.html?device=fold&card=home"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=2400,1800 \
  --screenshot=store/marketing/fold-4x3-shot.png \
  "file://$PWD/store/marketing/cards-fold.html?card=4x3"
```

Copy the rendered cards into `play-assets/` after review.
