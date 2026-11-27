# DSA trader status — decision record

**Decision:** **Yes — trader status applies to Bihua.** Declare the developer
account as a trader for Google Play. Do not select "non-trader".

**Date:** 27 November 2026
**Scope:** Google Play (Android), Bihua 1.1.2 and later.
**Related:** `docs/compliance.md`, `store/PLAY-CONSOLE-POLICY.md`.

## Why yes

- The app is published from the developer's Google Play developer account, and
  that account is a trader account. The account's trader status carries to every
  app published under it, including Bihua.
- Bihua is distributed as a commercial product of that account. It is free, has
  no in-app purchases, and shows no ads, but **free and no-IAP does not by
  itself make an app non-commercial**. The developer publishes and distributes
  the app in the course of a business or professional activity, so the trader
  definition is met.
- Choosing "non-trader" would be a false declaration and risks removal from EU
  storefronts.

The app therefore inherits the account's trader status. There is no separate
per-app non-trader election to make.

## What to enter in Play Console

| Field | Value |
|---|---|
| Trader status | **Yes, I am a trader** |
| Terms of service URL | `https://shadowsx3.github.io/bihua/terms.html` |
| Privacy policy URL | `https://shadowsx3.github.io/bihua/privacy.html` |
| Contact | The developer contact details already on the account (email and address/phone as Play requires) |

Play will display the trader contact information publicly in the EU. Use
contact details the developer is willing to publish; do not use a private home
address if an alternative business address is available.

## Requirements for the ToS link

The terms URL entered for the trader declaration must be:

- **public** — reachable without an account or login;
- **HTTPS** — `https://shadowsx3.github.io/bihua/terms.html`;
- **worldwide** — no country restrictions, no EU-only gate;
- stable — the page stays live for as long as the app is distributed.

GitHub Pages serves the page over HTTPS from the public `docs/` directory, with
no login, no cookies required, and no geo-restriction, so it meets these
requirements. The page also links to the privacy policy and support page.

## Follow-up

- Keep the ToS effective date and app version current when the app changes.
- The developer **remains a trader for the account**. If another app is
  published under the same account, it inherits the same status; changing one
  app to non-trader is not possible without changing the account declaration.
- If the app is later distributed on the App Store, make the same trader
  declaration in App Store Connect and reuse the same ToS URL. Update
  `docs/compliance.md` and `docs/ios-app-store-checklist.md` when that decision
  is recorded (not part of this change).
