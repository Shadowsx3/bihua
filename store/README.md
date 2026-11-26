# Play Store assets

## Current set — 1.1.2 (versionCode 23)

The final upload set lives in `play-assets/`:

- `play-assets/phone/` — eight 1080 × 1920 cards: Home, Practice, Dictionary,
  Practice sheets, Widgets, Spell quest, Progress, and the report composer
- `play-assets/tablet/` — 7-inch 1920 × 1080 and 10-inch 2560 × 1440 cards
- `play-assets/fold/` — the required 1:1, 4:3, and 10:16 cards
- `play-assets/icon/icon-512.png` — 512 × 512 launcher icon
- `play-assets/banner/feature-graphic-1024x500.png` — 1024 × 500 banner
- `play-assets/listing.md` — short and full description, category, tags, contact
- `play-assets/full-description.txt` — plain-text description for the Play box
- `play-assets/README.md` — specs, captions, and rebuild commands
- `play-assets/upload-1.1.2/` — renamed upload staging, including the current
  AAB slot

The editable sources are `marketing/cards.html`, `marketing/cards-tablet.html`,
`marketing/cards-fold.html`, `marketing/widget-card.html`, and
`marketing/feature-graphic.html`. `marketing/README.md` lists the raw captures
and design sources.

The widget card is drawn from the real widget layout: the same top row, two
numbers, detail line, and Study button for Today and Week ahead, and the same
word, chips, pet, tip, and hint for Word of the Day.

## Older sets

The 1.0.2–1.1.1 binaries, raw 1.0.4 captures, and the 1.0.7/1.1.1 upload
folders were removed during the 1.1.2 cleanup. Play Console keeps the release
history; this repository keeps only the current set and its editable sources.

`PLAY-CONSOLE-POLICY.md` is the current Data safety, rating, and declaration
answer sheet. `PLAY-UPLOAD.md` is the current upload order.

## Rebuild

```sh
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=1080,1920 \
  --screenshot=store/marketing/shot-1.png \
  "file://$PWD/store/marketing/cards.html?card=1"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=2560,1440 \
  --screenshot=store/marketing/tablet-shot.png \
  "file://$PWD/store/marketing/cards-tablet.html?card=home"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=2208,2208 \
  --screenshot=store/marketing/fold-shot.png \
  "file://$PWD/store/marketing/cards-fold.html?card=home"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=1080,1920 \
  --screenshot=store/marketing/widget-card.png \
  "file://$PWD/store/marketing/widget-card.html"
```

Copy the rendered cards into `play-assets/` after review.
