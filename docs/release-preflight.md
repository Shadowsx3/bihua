# Release preflight

A short, evidence-backed pass over the failure classes external testers usually
find. Checked against the 1.1.2 candidate (versionCode 23). This is not a
replacement for a real-device run; it is the list we can verify locally.

## Store and compliance

- **Privacy policy link** — the local `docs/privacy.html` is current for 1.1.2
  and covers local data, network actions, permissions, ML Kit diagnostics,
  retention, backups, crash reports, reminders, security, children and age,
  GDPR/UK GDPR rights and legal bases, international transfers, California, and
  contact. The live GitHub Pages copy still says 1.0.5, so deploy `docs/` before
  the listing is sent for review. A link that opens is not enough if the policy
  is stale.
- **Accounts** — Bihua has no account, sign-in, password, cart, or payment.
  There is no account-deletion flow to add. Settings → Data → Start over clears
  cards, reviews, XP, and confusions; notes, study lists, hooks, and settings
  stay. The privacy page says this plainly.
- **Data safety** — no ads, no developer analytics, no advertising ID. ML Kit
  diagnostics are the only off-device collection: declare **Device or other
  IDs**, **App info and performance: Diagnostics**, and **App activity: Other
  actions**, collected, not shared, not ephemeral. Encryption in transit is yes;
  deletion is yes. The privacy page and the form must match.
- **Android backup** — `allowBackup="false"` plus exclusion rules for every data
  domain. Progress, notes, profile, models, and crash files stay out of Google
  backup and OS migration; Bihua's own Export and local sync are the transfer
  paths.
- **Permissions** — no camera, microphone, contacts, or background location.
  Internet, network state, and notifications are the user-visible ones.
  WorkManager adds wake lock, boot-completed, and foreground-service
  declarations for the one-shot reminder; the app does not start a long-running
  foreground service.
- **Audience** — the listing and policy both target 16 and over. Do not add
  13–15 or opt into the Families program without a separate review.
- **Listing copy** — do not promise JLPT, TOPIK, HSK scores, literacy outcomes,
  or anything the build does not do. Do not show the Mnemonic palace in store
  images.

## Launch, offline, and background

- Cold launch on the debug and signed builds; `offline-first-run` covers a clean
  device with no network, Skip setup, Home, and a review.
- Content, review history, hooks, and reasons are local. No screen depends on a
  network read to render.
- Study reminders and widgets are the only background surfaces. They are local
  and do not hold a foreground service.
- Background/foreground and process death are covered by `resume`,
  `process_death`, `rotation-state`, and `rotation-onboarding`.

## Navigation and input

- Overlays close with Back: report, custom range sheet, outfit dialog, mnemonic
  dialog, reason sheet, study tools, and the palace.
- The review close flow asks before leaving; a completed session returns Home.
- The report composer, mnemonic dialog, and settings search use IME insets so a
  keyboard cannot hide the primary action.
- Search only returns characters and words in the active course and visible
  content set; there is no inactive-product state to land on.
- Long grids and lists use lazy layouts. `characters-scroll` flings the densest
  grid and opens a deep tile.

## Visual and state

- Eight shape profiles: phone, phone landscape, tablet, tablet portrait, fold
  inner, fold inner portrait, fold outer, fold outer landscape. `screen-pass`
  all eight.
- Dark theme: `screen-pass-dark`; new level progress, reason bar, and range
  sheet are theme-aware. The palace is intentionally a light paper notebook.
- Type: `screen-pass-large` at 1.5x and a 2.0x phone sweep for the largest
  accessibility setting.
- Rotation: the review card, ink, answer, report draft, Settings section,
  Practice selection, Progress range, custom dates, and mnemonic dialog fields
  survive a turn.
- `nav-gap` checks the floating bottom bar against content at 1x and 1.5x.

## Core learning

- `modes-pronounce-only`, `modes-meaning-only`, `modes-extra-only`, `modes-all`,
  and `lesson-hook` cover recognition-only lessons and reviews, the no-canvas
  path, and the opt-in hook button.
- `grade-reasons` covers Again/Hard, one saved reason, one skipped, and the
  session continuing.
- `progress-custom-range`, `progress-ranges`, `progress-screen`, and
  `progress-all-time` cover presets, exact dates, recent strips, and the level
  cards.
- `mnemonic-studio`, `mnemonic-japanese`, `mnemonic-korean`,
  `mnemonic-kana-hidden`, and `mnemonic-hangul-hidden` cover the palace and its
  script boundaries.
- `make check` and `make lint` are the gate.

## Open before upload

1. Deploy the local `docs/` site so the privacy link is current.
2. Re-capture the store Progress/offline images from the final build; keep the
   Mnemonic palace out of every store image.
3. Run the final 2.0x font sweep and fix any clipped last row.
4. Run one real phone ↔ tablet sync in both directions: merge, replace, and a
   settings-only transfer.
5. Run the signed-build smoke on the real phone.
