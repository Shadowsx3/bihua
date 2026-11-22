# Play Console upload checklist — Bihua 1.0.2

Play Console does **not** sync from this repository. Every item below must be
uploaded or pasted in the Play Console by hand.

## Phone screenshots

Delete or replace the old phone screenshots, then upload these in this order.
All are 1080 × 1920, 9:16, under 8 MB.

1. `store/phone-2.png` — Home
2. `store/phone-1.png` — Practice
3. `store/phone-6.png` — Answer and grade
4. `store/phone-3.png` — Character detail
5. `store/phone-4.png` — Word practice
6. `store/phone-5.png` — Characters
7. `store/phone-7.png` — Progress

Do not keep the old five alongside these; they show the 1.0.1 layout.

## Tablet screenshots

### 7-inch

- `store/tablet-7-home.png`
- `store/tablet-7-detail.png`
- `store/tablet-7-practice.png`
- `store/tablet-7-progress.png`

1920 × 1080, under 8 MB.

### 10-inch

- `store/tablet-10-home.png`
- `store/tablet-10-detail.png`
- `store/tablet-10-practice.png`
- `store/tablet-10-progress.png`

2560 × 1440, under 8 MB.

Foldable, desktop, and Android XR can stay empty for this submission.

## Full description

The current Play Console copy is the old 2,735-character version. Replace it with
`store/full-description.txt` (2,801 characters), or add this sentence to the
existing copy:

> Recognition is evidence, not a verdict: messy or partial strokes can be
> missed or misread. It never presses the grade button.

The full description is plain text. Google Play does not render Markdown, HTML,
bold, or links.

Do not use AI listing translations while the app UI is English-only.

## App icon and feature graphic

Already uploaded; keep them:

- `store/icon-512.png`
- `store/feature-graphic.png`

## Release

1. Go to **Test and release → Testing → Closed testing**.
2. Create a new release or update the existing closed-test release.
3. Upload `app/build/outputs/bundle/release/app-release.aab` (versionCode 12,
   version 1.0.2).
4. Release name: `1.0.2 closed test`.
5. Release notes: copy `store/release-notes-1.0.2.txt`, including the
   `<en-US>` tags.
6. Save, review, and roll out to the existing closed-test track.
7. Keep the same 12 testers opted in continuously for the 14-day window.

## Store listing save

After changing the description, screenshots, or tablet assets, scroll to the
bottom of the Store listing page and click **Save**. Then use **Review** to
publish the listing changes. “No updates here” usually means the page has not
been saved yet.
