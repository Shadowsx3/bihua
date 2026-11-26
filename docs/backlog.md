# Bihua maintenance backlog

Updated: 2026-11-26

## Status at 16:40 (2026-11-26)

- The reader now has one shared, cached readiness path; Spell quest no longer
  polls, and review/practice checks use the 900 ms probe. The tab-switch sound
  is gone.
- Write the word: the cue and guide are laid out as one word instead of spaced
  cells, the guide is capped at 150 dp and the brush scales with it, and a small
  brush icon opens a compact tool menu. Stroke numbers only appear with the
  stroke guide.
- PDFs (Progress and practice sheets) now open a choice: Save to Files or Share
  PDF. Sharing uses a FileProvider cache under `cache/pdf`; the Progress export
  card sits at the end of the report.
- Known-list import: short words and character lists are accepted, obvious prose
  lines are skipped, and a preview states “This file marks N characters as
  known” with Confirm/Cancel. Imported known cards get stable 10–45 day due
  dates, a capped per-day load, and a bulk transaction.
- Database: progress schema 18 adds report/review indexes, WAL is enabled, and
  study-day counting is a SQL aggregate. The content builder adds the missing
  mode/list, variant, and mode/word indexes for future content builds.
- Performance: Home/Progress data is gated by the visible tab and session, cards
  are skipped during sessions unless grade intervals are on, tab-only memory and
  report queries do not run on every tab, `dueNow` reuses the card map, and the
  content DB is prewarmed while onboarding is on screen.
- Fixed a regression where the session-time `visibleCharacters` cache captured
  the initial null course and left Home on “Loading characters…”.
- Green: `make check`, `make lint`, the known-list/due-spread tests, schema
  migration, and the focused phone journeys `word-whole-canvas`,
  `progress-pdf-share`, `settings-writing-model`, `known-list-preview`, and
  `progress-screen`, `progress-pdf-save`, and `worksheet`. Tablet
  `screen-pass`/`word-whole-canvas` and fold `word-whole-canvas` pass too.
  The phone full `screen-pass` was interrupted by the harness while the model
  notice spinner kept the system UI busy; the focused phone pass and the two
  larger profiles cover its changed surfaces.
- The word screen’s primary action is now capped at 520 dp and centered, so
  “Show answer” no longer stretches across a tablet; the stroke-number control
  only appears with the stroke guide.
- The download notice uses a static brush mark instead of a frame-driven
  spinner. This fixed the phone `screen-pass` hang (uiautomator never saw an
  idle window while the Popup spun). `model-notice` and the full phone,
  tablet, and fold `screen-pass` suites now pass, as do the dark and 1.5×
  variants. Word practice and the known-list preview also pass at 1.5× text.
- Emulator test matrix is complete. Remaining release gate is real-device
  upgrade/sync/PDF-share smoke, rebuilding signed artifacts and hashes,
  deploying `docs/`, and confirming the store upload/versionCode state.

## Status at 15:35 (2026-11-26)

- Reader work is now one shared, cached readiness path. Home, Settings,
  Spell quest, Word practice, and the check buttons read `ModelDownloads`
  instead of starting their own `isModelReady` loops; Spell quest no longer
  polls every two seconds for five minutes.
- The review/practice/drill/draw checks use the 900 ms local probe only. If the
  reader is still arriving, the card says so and shape/order feedback continues.
- Download notice: 1.5 s grace after the splash, per-script name, per-script
  failure state, tap-to-retry, and no notice during onboarding. The reader
  starts after a script pick, not when the script page opens.
- Downloads use any usable connection; the old Wi-Fi-only setting and plumbing
  are gone. A returning network retries the active script automatically.
- Also in: TTS initialization timeout and `SpeechState`, script-switch content
  tagging (no stale course flash), single settings read at boot, IO for backup
  restore/import/export, parallel cancellable “Download all”, world-readiness
  and close-race cleanup, and a debounced widget refresh after reviews.
- New tests: `OnboardingStartTest`, notice retry copy, per-script failure
  isolation, and readiness cache invalidation. `script-switch-loading` covers the
  slow course reload. `make check` and `make lint` are green.
- Device proof so far: phone `screen-pass`, `onboarding-pages`, `model-notice`,
  `word-whole-canvas`, `grade-reasons`, `practice-check`, and
  `script-switch-loading` pass. Tablet and fold are running for the same set.
- Pending: the tablet/fold pass for this change set. PDF export cancellation and
  a model-size estimate are intentionally deferred: a partial content Uri is
  worse than no cancel, and the real model sizes are not exposed by ML Kit.

## Status at 14:30 (2026-11-26)

- Post-RC work is in: the Progress report PDF (empty, first-session, year, All
  time, Chinese/Japanese/Korean), whole-word practice with optional clues and a
  no-model fallback, the Character PDF entry in Study tools, collapsible
  history/word sections, and the grade note as a small header button. The
  signed AAB/APK hashes in the 09:50 status are stale; rebuild before release.
- PDF exports build on the device and save through the system file picker. The
  live privacy page is still stale and must be deployed before upload.
- Store widget card now uses the real Today, Week ahead, and Word of the Day
  layouts; the phone widget image was regenerated. Do not upload on hold.
- Green: `make check`, `make lint`, and the new journeys
  `progress-pdf-rich`, `progress-pdf-rich-ja`, `progress-pdf-rich-ko`,
  `progress-pdf-empty`, `progress-pdf-start`, `progress-pdf-save`,
  `word-whole-canvas` (phone, tablet, fold), `word-one-by-one-no-model`,
  `word-practice-home-back`, `study-tools-pdf`, `characters-rotation`,
  `mnemonic-rotation`, `characters-scroll` (phone and tablet), and the broad
  phone pass. Remaining harness fix: rerun the outdated tablet spell-quest note
  after `pm clear`.

## Status at 09:50 (2026-11-26)

- Android 1.1.2 RC is built and audited; it is not uploaded. Release is on hold
  by request.
- RC: versionCode 23, versionName 1.1.2, signed AAB
  `e66b5b85ee2a39d77a87ba1623f90230bfbe046ce0b640987cfb2ff1dd501a51`,
  signed APK
  `1bcc530d27066a646f3c4fb3ce1f17aec1a5700709ef8cf0b05dcd30739a1d94`.
- Green: `make check`, `make lint`, `make play-audit`, all eight
  screen-pass shapes (phone/tablet/fold, portrait and landscape), dark, 1.5x,
  Traditional, Japanese, Korean, settings-pages, report-flow (portrait and
  landscape), spell-quest (portrait and landscape), progress, All time,
  onboarding, offline first run, rotation-state, rotation-onboarding,
  characters-scroll on fold, and study-tools on fold outer landscape.
- Fixed this window: fold outer review canvas, sticky landscape review actions,
  rotation state, compact Home hero, outfit dialog crash (layer-list), report
  sheet centring, 65% guide, local sync client/server tests, stale-grid sweep
  flakiness, and Progress range filtering for Recent mistakes and Recently
  learned.
- Pending: real-phone release smoke; release/upload decision; optional
  code-map and large-file cleanup. iOS is parked in `docs/ios-kmp-plan.md`.
- Store assets refreshed: 8 captioned phone cards, 6 captioned 7-inch and 6
  captioned 10-inch tablet cards, and 7 fold cards. Progress and the report
  composer are included, and the listing copy names the range selector.
- New since the last status: exact Progress start/end ranges, drawing-off
  practice, optional grade reasons, the Mnemonic palace (Chinese sound roles;
  Japanese kanji parts; Korean Hanja parts; hidden for kana/Hangul), Level
  progress under Momentum, and merge-based sync.

This is the working list for the 1.1.2 stabilization window. Keep it short and
honest: move an item to Done only after its proof (test, journey, screenshot, or
release artifact) exists.

## iOS port (`ios/kmp`) — P0 in progress

The port runs on `ios/kmp`, branched from the tagged 1.1.2 candidate
`android-1.1.2-rc23`. The plan and the platform matrix live in
`docs/ios-kmp-plan.md`; Android stays shippable on `main`.

- [x] Freeze the 1.1.2 tree, tag `android-1.1.2-rc23`, branch `ios/kmp`.
- [x] Add Kotlin Multiplatform, Compose Multiplatform 1.9.3, iOS arm64 and
      simulator targets; keep the Android sources at `src/main` through
      `androidMain` source dirs and move only the manifest to `src/androidMain`.
- [x] Android stays green: `make check` and `make lint` pass after the move.
- [ ] P0 toolchain spike: framework link, Compose screen on the simulator,
      ASCII text field, Pencil force probe, ML Kit pod link.
- [ ] P1 source-set split and pure-package moves.
- External: the Apple Developer account is being renewed (simulator work needs
  no account; TestFlight in P7 does).

## P0 — Android 1.1.2 stability and release readiness

- [x] Full screen-pass matrix: phone, phone-landscape, tablet, tablet-portrait,
      fold-inner, fold-inner-portrait, fold-outer, fold-outer-landscape, in all
      four scripts.
- [x] Fix every matrix failure; rerun the affected profile.
- [x] Dedicated `settings-pages`, `report-flow`, `spell-quest`,
      `progress-screen`, `progress-all-time`, `onboarding-pages`, and
      `offline-first-run` passes after the layout fixes.
- [ ] Rerun the tablet spell quest after `pm clear`; the earlier `Hello` timeout
      looked like stale emulator state.
- [x] Run the full release gate after the final code change:
      `make check`, `make lint`, report/spell/onboarding/offline journeys,
      signed AAB/APK, signature and hash checks. The 1.1.2 RC is built and
      audited but not uploaded; release is on hold by request.
- [x] Clean-install release smoke: onboarding, Home, review, Settings/About,
      report, spell quest, landscape, offline. Report survives rotation.
- [ ] Reconnect the real phone and run the release smoke there.
      The final production install/check is still pending.
- [ ] Refresh the store images for the Progress range/Level progress, drawing
      toggle, and report sheet. Do not show the Mnemonic palace in store images.
- [ ] Deploy the local `docs/` site before submission. The live privacy page still
      says 1.0.5; the local page is 1.1.2 and must be the one testers reach.
- [ ] Make the journey runner faster later: reuse device sessions, batch state
      pushes, avoid redundant app starts, and parallelise independent profiles.
- [ ] Real two-device sync smoke (phone and tablet, both directions, one merge,
      one replace, and a settings-only transfer).
- [ ] Exam tags when the current batch is done: map HSK 3.0, JLPT, and the
      Korean equivalent to the content metadata and add a filter. Do not claim
      JLPT until the mapping exists.
- [ ] Tag the release candidate after all gates pass.
- [ ] Keep the hotfix playbook current.

## P0 — Bugs and logic hardening

- [ ] Review every matrix failure and classify app bug / harness race / device
      limitation.
- [ ] Add regression coverage for each fixed bug.
- [x] Audit the newest logic for edge cases: empty data, no network, no reader
      model, large text, dark theme, rotation, process death, restored backup.
- [x] Fix the landscape review: sticky Show answer / Check bar and grade bar.
- [x] Make Settings section and report draft survive the phone/wide shell swap
      and rotation; add `fontWeightAdjustment` to the activity config.
- [x] Add `rotation-state` and `rotation-onboarding` journeys.
- [ ] Check the report sheet, guide overlay, and spell quest on all three device
      shapes in both orientations.

## P1 — Sync test coverage

- [x] Add client/server integration tests over a real local TLS socket.
- [x] Cover settings-only, progress-only, both, and empty transfer scope.
- [x] Cover correct code, wrong code, wrong certificate pin, malformed archive,
      and missing/empty entries.
- [x] Extract the server request/response formatting into a testable helper.
- [x] Verify the pairing code and certificate pinning end to end.
- [x] Keep the tests fast and CI-friendly; no real Wi-Fi or NSD required.
- [ ] Add an oversized-entry test if the archive cap can be reached without a
      large fixture; otherwise keep the unit cap covered in `BackupArchiveTest`.

## P1 — Settings outfit dialog

- [x] Redesign the Mò outfit dialog so it matches the newer warm-paper dialogs:
      clear preview, roomy cards, selected state, no cramped tile grid.
- [x] Add a preview-and-confirm flow with `Use this outfit`.
- [x] Verify every outfit at 1x, 1.5x, dark, and landscape.
- [x] Run `settings-outfit` and inspect the new screenshots.
- [x] Verify the widget pet preview and the no-crash layer-list raster path.

## P1 — Agent and developer flow

- [x] Refresh `AGENTS.md`:
      Reveal review naming, Today/Week widgets, Progress All time/heatmap,
      local crash report, landscape layouts, offline first run, 1.1.2,
      release/patch flow, sync test rules, no debug logs in release.
- [x] Update `README.md` for 1.1.2 and the current feature set.
- [x] Keep the new `docs/ios-kmp-plan.md` parked as a long-term plan.
- [ ] Add a short code map for the large files and module boundaries (deferred;
      no refactor during the release freeze).
- [ ] Document the overnight QA flow and the three-device matrix.

## P1 — Test suite and tooling

- [ ] Make the device matrix one command with a clear pass/fail summary.
- [ ] Reduce journey flakes: stale emulator state, uiautomator hierarchy races,
      IME/keyboard timing.
- [ ] Add a release smoke script that uses the signed APK, not the debug build.
- [x] Add a Play compliance check script: permissions, exported components,
      debuggable flag, target SDK, arm64, signing, privacy links.
      Run `make play-audit` on the signed artifacts.
- [ ] Keep screenshot artifacts and failure XML in the run report.

## P2 — Play Store compliance

- [ ] Verify permissions and merged manifest:
      INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, WAKE_LOCK,
      RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE only.
- [ ] Confirm no debug/test flags, no debug signing, no analytics, no ads.
- [ ] Data safety: ML Kit diagnostics, local crash files, no account/server.
- [ ] Content rating and audience: 13+, mature opt-in, no Families.
- [ ] Reviewer notes: no account, clean offline device usable, optional reader
      download, local crash report, arm64-only.
- [ ] Privacy policy and landing page current.
- [x] Store listing and screenshots current; no Anki wording.
- [ ] Internal track, pre-launch report, staged rollout.

## P2 — Code hygiene

- [x] Gate spell-quest diagnostics behind `BuildConfig.DEBUG`.
- [x] Replace `android.media.ExifInterface` with the AndroidX implementation.
- [ ] Review the largest files for safe extraction:
      `SettingsScreen.kt`, `SpellQuestScreen.kt`, `LearnScreen.kt`,
      `BihuaApp.kt`, `CharactersScreen.kt`, `ProgressStore.kt`.
- [ ] Do not refactor during a release freeze unless the change is required.
- [ ] Keep new code in the established package/pattern; no new dependency
      without a test and a clear reason.

## Done

- [x] Wrote the long-term iOS/KMP plan to `docs/ios-kmp-plan.md`.
- [x] Locked the iOS decisions and deferred the work until after user feedback.
- [x] Added the offline first-run scenario and reviewer notes.
- [x] Verified the report sheet centering on phone landscape, tablet, and fold.
- [x] Verified the 65% chapter-coloured spell guide and the no-halo gift/result.
