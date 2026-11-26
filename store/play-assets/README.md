# Bihua 1.1.2 Play Store assets (candidate set — release on hold)

The store images deliberately show only the core learning surfaces. The
Mnemonic palace is available in the app but is not part of the store image set.

The 1.1.2 set updates the previous pack for versionCode 23. Every form factor now
uses the same warm-paper message cards as the phone. The phone set adds Progress
and the report composer; the tablet set is rebuilt with captions at 7" and 10";
the fold set adds Progress and the report composer. The phone widget card in
`phone/05-widgets.png` uses the real Today, Week ahead, and Word of the Day
layout. The signed AAB and the renamed upload copies are in `upload-1.1.2/`.

## Phone — 1080 × 1920, 9:16

| # | File | Caption | Screen |
|---|------|---------|--------|
| 1 | `phone/01-write-from-memory.png` | Write it from memory. | Home with today's plan |
| 2 | `phone/02-every-stroke.png` | Every stroke, in order. | Practice with 谢 and the filled brush guide |
| 3 | `phone/03-dictionary.png` | A dictionary that teaches. | 爱 entry with example words |
| 4 | `phone/04-worksheet.png` | One tap. A4 PDF. | Practice sheets builder |
| 5 | `phone/05-widgets.png` | A little desk on your home screen. | Today, Week ahead, and Word of the Day widgets |
| 6 | `phone/06-spell-quest.png` | Cast what you remember. | Spell result with a met page and one way through |
| 7 | `phone/07-progress.png` | See the shape of your memory. | Exact dates, level progress, heatmap, and cumulative line |
| 8 | `phone/08-report.png` | Tell me what broke. | Report composer with a saved crash attached |

All eight are 24-bit PNG with no alpha, under 2 MB.

## Tablet — captioned cards, 16:9

Every tablet image is a warm-paper card with a caption, a note, and the current
app screen. The 10" files are 2560 × 1440; the 7" files are the same card
downscaled to 1920 × 1080 so the message stays readable.

| File | Size | Message |
|------|------|---------|
| `tablet/7-home.png`, `tablet/10-home.png` | 1920 × 1080 / 2560 × 1440 | Room for the whole desk. |
| `tablet/7-practice.png`, `tablet/10-practice.png` | 1920 × 1080 / 2560 × 1440 | The square stays wrist-sized. |
| `tablet/7-characters.png`, `tablet/10-characters.png` | 1920 × 1080 / 2560 × 1440 | The whole dictionary, in reach. |
| `tablet/7-spell.png`, `tablet/10-spell.png` | 1920 × 1080 / 2560 × 1440 | A quiet spell road. |
| `tablet/7-progress.png`, `tablet/10-progress.png` | 1920 × 1080 / 2560 × 1440 | See the shape of your memory. |
| `tablet/7-report.png`, `tablet/10-report.png` | 1920 × 1080 / 2560 × 1440 | Tell me what broke. |

## Foldable — 1:1, 4:3, 10:16

| File | Size | Message |
|------|------|---------|
| `fold/01-home.png` | 2208 × 2208 | Open it like a book. |
| `fold/02-practice.png` | 2208 × 2208 | Two pages. One square. |
| `fold/03-inner-4x3.png` | 2400 × 1800 | The page becomes the desk. |
| `fold/04-outer-10x16.png` | 1200 × 1920 | Folded. Still yours. |
| `fold/05-spell.png` | 2208 × 2208 | A spell book that remembers. |
| `fold/06-progress.png` | 2208 × 2208 | Every range, folded open. |
| `fold/07-report.png` | 2208 × 2208 | Nothing leaves until you send it. |

The seven cover the required 1:1, 4:3, and 10:16 shapes.

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
`store/marketing/cards-tablet.html`
(`?card=home|practice|characters|spell|progress|report`),
`store/marketing/cards-fold.html`
(`?card=4x3|10x16|spell|progress|report`), and
`store/marketing/widget-card.html` (widgets). The banner source is
`store/marketing/feature-graphic.html`. Render the tablet card at 2560 × 1440,
then downscale it to 1920 × 1080 for the 7" slot.

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

## Before upload

- All screenshots are 24-bit PNG with no alpha and under 2 MB.
- The tablet and fold cards carry the same captions as the phone and name
  1.1.2 in the footer.
- No screenshot shows a personal name, notification, debug badge, or an older build.
- The listing says A4 or Letter, mentions the four courses, and has the spell quest.

## Upload record 1.1.2

`upload-1.1.2/` is the staging slot. The signed bundle was rebuilt from the
final 1.1.2 tree on 26 November 2026 (versionCode 23):

- `bihua-1.1.2.aab` SHA-256:
  `99f0fa7420e6cfd14e4a0c019587034138c6f76e83fd12b88e8831ce0a11c4c1`
- `app-release.apk` SHA-256:
  `1abfa45fca0f5180d0ff416506a43329d878e11765ba92d25454384ae3ffc305`

The release name for the closed test is `1.1.2 closed test`. Notes live in
`store/release-notes-1.1.2.txt`. The renamed image set is:

- `bihua-1.1.2-phone-01-home.png` … `-08-report.png`
- `bihua-1.1.2-tablet-7-home.png`, `-7-practice.png`, `-7-characters.png`,
  `-7-spell.png`, `-7-progress.png`, `-7-report.png`
- `bihua-1.1.2-tablet-10-home.png`, `-10-practice.png`, `-10-characters.png`,
  `-10-spell.png`, `-10-progress.png`, `-10-report.png`
- `bihua-1.1.2-fold-1x1-home.png`, `-1x1-practice.png`, `-1x1-spell.png`,
  `-1x1-progress.png`, `-1x1-report.png`, `-4x3.png`, `-10x16.png`
- `bihua-1.1.2-feature-graphic.png`
- `bihua-1.1.2-icon-512.png` (canonical launcher icon)

The tablet and fold upload copies are the captioned cards, not bare screen
captures. The phone card at `-05-widgets.png` is the current three-widget set.
