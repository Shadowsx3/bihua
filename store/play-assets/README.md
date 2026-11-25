# Bihua 1.1.1 Play Store assets (rebuilt fix set)

Final set for the 1.1.1 spell-quest build (versionCode 22), rebuilt with the
practice, reader-readiness, settings, and new grimoire work. Nothing here is
committed to the app APK.

## Phone — 1080 × 1920, 9:16

| # | File | Caption | Screen |
|---|------|---------|--------|
| 1 | `phone/01-write-from-memory.png` | Write it from memory. | Home with today's plan |
| 2 | `phone/02-every-stroke.png` | Every stroke, in order. | Practice with 谢 and the filled brush guide |
| 3 | `phone/03-dictionary.png` | A dictionary that teaches. | 爱 entry with example words |
| 4 | `phone/04-worksheet.png` | One tap. A4 PDF. | Practice sheets builder |
| 5 | `phone/05-widgets.png` | A small quiet reminder. | Widgets and pinned outfits |
| 6 | `phone/06-spell-quest.png` | Cast what you remember. | Spell result with a met page and one way through |

All six are 24-bit PNG with no alpha, under 2 MB.

## Tablet — 16:9

The Play upload uses 16:9 captures from the 2560 × 1600 tablet emulator. The
spell shots show a real 水 written in the ring, not an empty circle:

| File | Size | Slot |
|------|------|------|
| `tablet/7-home.png` | 1920 × 1080 | 7-inch Home |
| `tablet/7-practice.png` | 1920 × 1080 | 7-inch Practice with 谢 |
| `tablet/7-spell.png` | 1920 × 1080 | 7-inch spell book with a drawn 水 |
| `tablet/7-characters.png` | 1920 × 1080 | 7-inch character grid |
| `tablet/10-home.png` | 2560 × 1440 | 10-inch Home |
| `tablet/10-practice.png` | 2560 × 1440 | 10-inch Practice with 谢 |
| `tablet/10-spell.png` | 2560 × 1440 | 10-inch spell book with a drawn 水 |
| `tablet/10-characters.png` | 2560 × 1440 | 10-inch character grid |

The marketing card at 1600 × 1300 is `store/marketing/tablet-spell-shot.png`;
it is not part of the Play set.

## Foldable — 1:1, 4:3, 10:16

| File | Size | Slot |
|------|------|------|
| `fold/01-home.png` | 2208 × 2208 | inner Home spread, 1:1 |
| `fold/02-practice.png` | 2208 × 2208 | inner Practice spread, 1:1 |
| `fold/03-inner-4x3.png` | 2400 × 1800 | inner Practice, 4:3 |
| `fold/04-outer-10x16.png` | 1200 × 1920 | outer Practice, 10:16 portrait |
| `fold/05-spell.png` | 2208 × 2208 | inner spell result, 1:1 |

The five cover the required 1:1, 4:3, and 10:16 shapes.

## Identity

- `icon/icon-512.png` — 512 × 512 launcher icon, cream paper, coral ring,
  forest ink, brush. No alpha.
- `banner/feature-graphic-1024x500.png` — 1024 × 500 feature graphic with the
  mascot, the 谢 guide, a printed sheet, and feature chips including Spell quest.

## Listing copy

- `listing.md` — Play name, short and full description, category, tags, contact.
- `full-description.txt` — plain-text description for the Play text box.

## Rebuild

The source layouts are `store/marketing/cards.html` (phone),
`store/marketing/cards-wide.html` (`?device=tablet|fold&card=home|practice|spell`),
and `store/marketing/cards-fold.html` (`?card=4x3|10x16|spell`). The banner source is
`store/marketing/feature-graphic.html`.

```sh
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=1080,1920 \
  --screenshot=store/marketing/shot-6-spell.png \
  "file://$PWD/store/marketing/cards.html?card=6"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=2400,1800 \
  --screenshot=store/marketing/fold-4x3-shot.png \
  "file://$PWD/store/marketing/cards-fold.html?card=4x3"

"$CHROME" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=1024,500 \
  --screenshot=store/feature-graphic.png \
  "file://$PWD/store/marketing/feature-graphic.html"
```

## Upload record

The renamed staging copies live in `upload-1.1.1/`:

- `bihua-1.1.1-phone-01-home.png` … `-06-spell-quest.png`
- `bihua-1.1.1-tablet-7-home.png`, `-7-practice.png`, `-7-spell.png`,
  `-7-characters.png`, `-10-home.png`, `-10-practice.png`, `-10-spell.png`,
  `-10-characters.png`
- `bihua-1.1.1-fold-1x1-home.png`, `-1x1-practice.png`, `-1x1-spell.png`,
  `-4x3.png`, `-10x16.png`
- `bihua-1.1.1-feature-graphic.png`

The app icon keeps the canonical name `icon-512.png`.

## Before upload

- All screenshots are 24-bit PNG with no alpha and under 2 MB.
- No screenshot shows a personal name, notification, debug badge, or an older build.
- The listing says A4 or Letter, mentions the four courses, and has the spell quest.
