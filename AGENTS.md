# Bihua — agent guide

This file is the shared context for a new chat. Read it before changing code.

## What this project is

Bihua is an offline-first Android app for learning to write Simplified Chinese,
Traditional Chinese, Japanese, and Korean. Characters are the study items;
readings and meanings are cues. Writing is the default recall task, with an
optional Reveal review. Pronunciation is an optional, separate review task.
FSRS-7 or SBD schedules each script, with optional sharing between identical
glyphs. No streak penalties, no account, no ads, no developer-run analytics.

The Korean course contains Hangul and Hanja. Do not describe it as Hanja only.

## Repository layout

- `composeApp/src/commonMain/kotlin` shared domain and, from P3, shared Compose UI.
- `composeApp/src/commonTest/kotlin` tests that run on Android and iOS.
- `composeApp/src/androidMain/kotlin` Android code, `androidMain/res`, `androidMain/assets`,
  and `androidMain/AndroidManifest.xml`.
- `composeApp/src/androidUnitTest` JVM/Robolectric tests; `composeApp/src/androidDebug` the QA
  bridge and debug manifest.
- `composeApp/src/iosMain/kotlin` the Compose entry point and Foundation services.
- `iosApp/` XcodeGen project, Swift host, Info.plist, privacy manifest, Podfile.
- `qa/ios/` the idb simulator driver; `scripts/ios.sh` the build and device loop.

## Current state (1.1.2 + iOS port branch)

- Android is the shipped client. Version 1.1.2 (code 23) is the release
  candidate; the signed RC is built and audited but not uploaded. Release is on
  hold.
- The iOS port lives on `main` in the same repository: `composeApp/` is the
  shared KMP module and `iosApp/` is the Xcode host plus the deferred WidgetKit
  code. Do not create a second branch or a separate app folder.
- iOS 1.0 is now a working study app: onboarding, real content, persisted
  FSRS-7 progress, review, Characters, Dictionary, Practice, Word practice,
  Progress, Settings, study lists, speech, reminders, and JSON backup
  export/import. Widgets, sync, PDF, spell quest, compare, reader, and sounds
  are absent from the first build and must stay out of the metadata.
- Shared today: theme, components, Home, Progress, Settings, pet avatars,
  calendar, charts, content and settings models, FSRS, spellbook, and the
  platform seams (`AppInfo`, `AppFiles`, `Capabilities`, `LinkOpener`). The
  architecture and migration ledger are in `docs/ios-architecture.md`.
- iOS uses a native `UITabBarController` + `UINavigationController` shell; the
  screen content is Compose. iOS hides sync, widgets, foldable layouts, and
  crash attachment through `Capabilities`; it never labels an absent feature
  "Android only". The parity table is `docs/ios-feature-parity.md`.
- iOS WidgetKit code is **deferred from the first iOS build**. It lives in
  `iosApp/BihuaWidgets` and `iosApp/WidgetShared` but is not an Xcode target and
  is not embedded. Do not add it to the first archive; the checklist is in
  `iosApp/BihuaWidgets/README.md`. Android widgets are unaffected.
- ML Kit iOS is **not linked in the first build**, so App Privacy is Data Not
  Collected. If a later build adds it, declare Diagnostics and update the
  policy in the same release. The pod has no arm64 simulator slice, so it can
  only be tested on a device.
- The first iOS build has no App Group and no widget extension, so the previous
  App Groups signing blocker is gone. A later widget release needs the App
  Group `group.com.bassmd.bihua` on both bundle ids and a provisioning profile
  that includes it; do not add the entitlement without enabling the capability
  in the Apple Developer account.
- New in 1.1.2: Today, Week ahead, and Word widgets on Android; Progress All
  time and the year
  heatmap; local crash files and the report composer; the seamless splash;
  two-page Home/Practice and spell-quest landscape; the 65% chapter-coloured
  guide; and the offline first-run path.
- The live maintenance checklist is `docs/backlog.md`. Keep it current.

## iOS verification loop

```bash
make verify          # Android check/lint + compliance + iOS content + iOS unit tests
make verify-ui       # the above plus all XCUITest journeys on iPhone and iPad
make ios-content-check
make ios-screenshots # store captures in .qa/ios/store
```

- Never mark an iOS-only change done without `make check`: the shared module
  compiles for Android too.
- Regenerate `iosApp/Bihua/Content/*.json` after a content DB change with
  `python3 scripts/export-ios-content.py`; `make ios-content-check` fails when
  the shipped JSON is stale.

## iOS work

```bash
make ios            # iPhone 17 Pro: build, boot, install, launch
make ios-ipad       # iPad Pro 13-inch
make ios-test       # Android unit tests + iosSimulatorArm64Test
make ios-check      # native tests + the Xcode Debug build
make ios-pods       # CocoaPods, simulator-safe (no ML Kit)
make ios-mlkit-pods # ML Kit for a device build
make ios-shot NAME=home
make ios-ui-test
make verify
make verify-ui
make ios-content-check
make ios-screenshots
make compliance      # Play + App Store policy checks
make ios-device      # signed build, install, launch on the connected iPhone
make ios-device-build
make ios-device-id
make ios-ipad-device  # signed build, install, launch on the connected iPad
make ios-ipad-device-shot NAME=home
python3 qa/ios/smoke.py
```

- XcodeGen owns `iosApp/Bihua.xcodeproj`; after changing `project.yml` run
  `cd iosApp && xcodegen generate && pod install`.
- `qa/ios/driver.py` mirrors the Android Journey verbs for `idb`. Rotation,
  keyboard permission sheets, and VoiceOver stay XCUITest.
- Do not grow the P0 probe screen. New evidence comes from real screens as they
  move to `commonMain`.
- Keep Android `make check` green after every move; a move is behaviour
  preserving or behind a capability.
- App Store questions and answers: `docs/ios-app-store-checklist.md`; the
  store matrix is `docs/compliance.md`; the local listing draft is
  `store/ios-listing.md`; the iOS guideline review list is
  `docs/ios-design-guidelines.md`.
- The app has no name and no photo. Profile is a name-free pet avatar from the
  twelve drawn looks; never add a name field, photo picker, or photo-library
  permission without a store review and a policy change.
- Privacy and store copy move together: `PRIVACY.md`, `docs/privacy.html`,
  Google Play Data safety, App Store App Privacy, and
  `PrivacyInfo.xcprivacy` must agree. Never claim an iOS feature before the
  build ships; mark Android-only features on the landing page, not inside the
  app.

## Product feel

Keep this in mind for every screen:

- Warm paper and forest ink, with vivid accents. Lively, not muted or generic.
- Juicy but comfy: soft rounded shapes, generous breathing room, clear hierarchy,
  gentle motion, and satisfying feedback.
- The learner should feel ownership: their pace, their course, their finish
  date, their words, their progress.
- This is a small personal project made with care in Uruguay. Aim for warm,
  specific copy and quiet delight, not a faceless product or a generated demo.
- Keep the writing surface calm and paper like. No decorative color or shapes
  under the ink. The guide lines stay clear in light and dark themes.
- One clear next action per screen. Put explanations in dialogs, not paragraphs.
- No AI sounding copy, no em dashes in user-facing strings, no cringe slogans.
- Prefer short sentences and real verbs. Error copy says what happened and what
  to try next. It does not blame the learner.

## Development loop

Follow this loop for every feature, fix, and content change. It is the quality
bar, not a suggestion. Stop only when the evidence is green.

### 1. Frame the change

- Write one sentence: who benefits, what changes, and which screens/data move.
- Find the invariant it touches (schedule, script purity, content source,
  privacy, large text, day boundary, backup compatibility).
- Read the nearest section of this file and the matching doc before editing.
- Decide whether it is a new feature, a bug fix, a content change, or a build
  change. Each has a different proof (below).

### 2. Reproduce or inspect before guessing

- For a bug, reproduce on the emulator and capture `device.sh shot` plus
  `device.sh ui` and `logcat`. Use `device.sh find/tap` by label, never
  coordinates from a scaled screenshot.
- For a UI change, screenshot the current screen at 1x and 1.5x. For a Settings
  change, open the exact section, not just the Settings home.
- For a content change, inspect the authored source and the shipped asset.
  `spellbook.csv` and `puzzle_parts.db` are generated; edit `tools/spells/`.
- For a journey failure, read `failed.png`, `failed.xml`, `error.txt`, and
  `logcat.txt` before touching code. Classify: app bug, data bug, harness race,
  or a real device limitation.

### 3. Implement the smallest honest change

- Prefer the existing pattern and component. Keep user copy warm, short, and
  specific.
- Keep data backward compatible: new serialized fields need defaults and a
  `sanitized()` path; new DB columns/tables need a build path and a fallback.
- Do not hand-edit generated files. Change the authoring source, rebuild, and
  commit both source and artifact only where the repo already does.
- Do not add a service, cloud account, analytics SDK, or a new dependency
  without a strong reason.
- For a setting: add the field with a safe default, sanitize it, render it in the
  right Settings section, include it in backup/import, and add a test.
- For a brush/ink setting: update both the active value and its per-tool map.
  See `AppSettings.withTool`, `withBrush`, and `withToolBrush`.
- For a generated puzzle: every obstacle needs 2+ fitting families, 8+ compatible
  place kinds, a success line, and a place-kind row.

### 4. Prove it with tests

- Pure logic: add or extend a unit test. Run `make test` or the focused test.
- Content: run the builder and `make audit`; add a content guard if the bug was
  a bad row. Never let a bad gloss or empty family ship.
- UI: add a journey step or scenario. A new screen gets a screenshot journey;
  a new interaction gets a step that fails when it does not work.
- Edge cases to try by default: empty/zero data, no permission, no stylus, no
  network, large text (1.5x and 2x), dark theme, reduce motion/system animations
  off, rotation, process death, system back, a second course, and a restored
  backup.
- Performance: no database, parsing, or file IO on the main thread; keep
  ambient loops at 30fps or less; honor the system animator scale; cache
  immutable parsed data; avoid waking the device for widgets more than a few
  times a day.
- Report queries are gated by the visible tab and session, and card-wide work is
  skipped during a session unless grade intervals need it. `dueNow`, the forecast,
  and review totals reuse the one card map. Do not add another `allCards()` read
  to a screen that already has `cards`.
- Progress uses WAL and schema-18 report indexes. The content DB is prewarmed
  while onboarding is on screen; do not move the first 30 MB copy back to the
  first Home frame.
- The model-download notice uses a static brush icon, not a frame-driven
  spinner. A perpetual animation in that global Popup makes uiautomator and
  accessibility-idle waits hang for the whole session.

### 5. Run the gates

```bash
make check
make lint
```

Then run the matching journey with screenshots and inspect them:

```bash
# One screen or flow
scripts/journey.sh --scenario spell-quest --profile phone --font-scale 1.5 --screenshots

# A change that touches the shell, Home, or Settings
scripts/journey.sh --scenario nav-gap,settings-pages --profile phone,tablet --screenshots

# The release gate
scripts/journey.sh --standard --screenshots
```

- Do not run `adb ... uiautomator dump` or install another build while a journey
  is running. It can kill the uiautomator service and create a false crash.
- If a journey fails, fix the app first and the harness only when the harness is
  genuinely racing. Add a regression so the same failure cannot return silently.
- A screenshot is evidence: check new-user clarity, one clear action, no clipped
  labels, no content under the navigation bar, and a real daily-use path.

### 6. Review as two users

- **New user:** can they tell what this does and what to tap next? Is the empty
  state honest? Is the optional feature clearly optional?
- **Daily user:** does the common path stay short? Does it remember their
  choices? Does it avoid repeated confirmation, nudges, and motion?
- **Low-end user:** does it stay responsive with large text, reduced motion,
  and no network? Does it survive a small screen and a fold?

### 7. Release only from a clean tree

- Bump `versionCode` and `versionName` for a release; do not rebuild a version
  that is already published.
- `make release` and `./gradlew :composeApp:bundleRelease`, then verify the version and
  signature, install the signed APK, and smoke-test the main flow.
- Update `docs/download/`, `SHA256SUMS.txt`, the landing hash/link, the release
  checklist, roadmap, README, changelog, and `docs/play-store-submission.md`.
- Commit the code, content, tests, docs, and hosted artifact together when they
  describe one release. Push only after the hosted hash matches the local APK.

### Definition of done

- [ ] The behavior is implemented from the authored source, not a generated
      file.
- [ ] A unit test, content guard, or journey would fail without the fix.
- [ ] `make check` and `make lint` are green.
- [ ] The matching journey is green at 1x and 1.5x, on the profiles it touches.
- [ ] Dark, reduce-motion, and low-end behavior were considered.
- [ ] Settings screens and empty/off states are clear and not duplicated.
- [ ] Docs, counts, screenshots, and release notes match the shipped build.
- [ ] `git status` is clean and the release hash matches the hosted APK.

## Mò and the guided tour

- The pet has one name: **Mò** (墨, "ink"). The landing page, launcher, opening,
  profile avatar, tour, and completion screens all use the same `InkMascot`.
  Do not invent a second pet or rename Mò in one screen only.
- The guided tour is a short visual-novel moment, not a slideshow of mock
  screens. It runs as a live overlay over the real app with a spotlight hole
  around real controls, then moves the real tab stack to the next stop.
- Mò talks in first person, uses a typewriter line, and can wave, point, nod,
  scribble, shrug, or cheer. Each stop can give Mò a small prop: card, clock,
  magnifier, grade dots, pencil, scroll, or star. Keep the gestures quiet and
  warm, never a cartoon overload.
- Tour copy is spoken and concrete. Explain reviews without naming SRS jargon:
  the character returns just before you would forget it, the gap grows after an
  easy review, and shrinks after a hard one. Say what each grade means in plain
  words ("Again: it slipped away.").
- Keep the tour optional. The Home invite is dismissible, "Not now" goes
  straight to the app, and Settings → Help & tour can replay it. A finished or
  skipped tour marks `tourInviteDismissed`; it must never gate study.
- Anchor IDs live in `TourAnchors`: `home.start`, `home.reviews`,
  `home.today`, `characters.search`, `practice.canvas`, `help.grades`,
  `help.modes`, `help.sheets`, `about.free`. If a stop moves, move its anchor,
  not the copy around a missing hole.
- Respect reduce-motion and TalkBack: no typewriter, confetti, bobbing, or
  pulse when motion is reduced; screen readers get the full line at once.
- Sound cues are optional (`soundEffects`) and synthesised in `audio/Sfx.kt`.
  Use them for meaningful feedback, not every tap.
- The Android widget pet wears one tiny costume from the same Mò palette: a graduation
  hat while work waits by day, a monocle on the Word widget, a witch hat in the
  evening, a vampire at night, a nightcap on vacation, and a party hat when the
  desk is clear. Settings → Display → Mò's widget outfit opens a preview-and-
  confirm dialog: tapping an outfit updates the real widget pet preview, and
  Use this outfit saves it. Auto keeps the hour/state choice. The in-app
  completion panel still uses the party hat. Keep the widget layer-lists aligned to the 1024 art; do not draw a
  second widget pet. Hats sit on the 1024 head line, not over the eyes.
- Widgets use `dp` text sizes, not `sp`, so the 2×2 and 4×2 cells do not clip at
  1.5× launcher font scale. App screens still use `sp`.
- Widget backgrounds follow the hour: warm morning, mint afternoon, lilac
  evening, and deep night with light text. Decorative glows sit at the top-right
  behind the pet. Keep text out of the motif area.
- The Today widget shows the review and lesson counts as two big stat blocks,
  then one flavor line and a bottom action. The Week ahead widget uses the same
  binder with `weekly = true` and the next seven days; it is a separate
  provider, not a mode toggle. The Word widget keeps the word as the hero on the
  left, the pet on the right, review/lesson chips below the definition, and the
  tip/footer at the bottom. Do not put a status line between the reading and the
  definition.
- The all-clear hero says “All done for today” once. Do not add a duplicate
  “All caught up” pill or a decorative pet that makes the card taller.
- The grade prompt in a session has a “?” that opens the plain-word legend.
  Keep it out of the way of the grade buttons.

## Home and review feel

- Home should feel like a lived-in study desk, not a dashboard for managers.
  Keep the mode-coloured hero or Continue card, the Word of the day, the three
  colourful stat cards, and the practice calendar above the fold. Study tools,
  level, recent characters, and browse may sit below or inside More.
- The Word of the day is a real word from the current script, filtered by the
  mature setting. It has audio and a Write action. Never show a made-up word.
- Grade interval labels are optional and off by default. When enabled, use the
  real FSRS interval for the current card and only the buttons the learner can
  actually use.
- Leave-session flow: back or X opens Resume later / End session / Keep
  studying. Resume later keeps the queue; End session clears it deliberately.
- The Compare writing drill is one character at a time with a large canvas,
  switchable character chips, a guide toggle, and Next/Done. It appears only
  when at least two characters are selected.
- Character detail for a known character should show one real word with audio
  and Write the word before the full word list.
- Settings stays compact. Explanations belong in dialogs. Mature words are off
  by default and require a confirmation when enabled.

## Offline first run and reader readiness

- A fresh device with no network must skip setup, open Home, and start a review.
  The reader model is an optional background download; never gate the primary
  action on it.
- If the model is missing, Check still returns local stroke and shape feedback,
  and Show answer is always available. Spell quest scenes can be answered from
  the grimoire.
- `offline-first-run` is the proof. The Play reviewer notes must say this.
- The only expected network use is the ML Kit model download.

## Progress and analytics

- Every Progress query is scoped to the active course (`mode.name`). A review
  in Simplified must never appear in Traditional, Japanese, or Korean totals.
  The range-dependent store calls (`gradeMix`, `practiceTimeMs`, `writingPace`,
  `taskStats`, `activity`, `learnedHistory`) take a nullable mode; pass the
  active course from `BihuaApp`, never leave it null on a screen.
- The page has five windows: 7D, 30D, 90D, 1Y, All. All range-dependent
  queries and the selected label rerun when the window changes. The 35-day
  calendar is allowed for longer windows and must say “Last 35 days”.
- All time is a real window, not a renamed 1Y. Do not clamp it to 366 days.
- Year and All use the GitHub-style 7-row heatmap with month labels, an M/W/F
  gutter, a legend, and horizontal scrolling. Auto-open on the newest weeks.
  Do not shrink a long history into tiny cells.
- Empty windows are calm, not punitive: “New window”, “No grades yet”, “No
  practice in this window yet”, “No memory data yet”, and “Nothing scheduled
  yet”. Never show `0% passed` before there is a grade.
- Streak is the real current run, not the selected window. `studyStreak` reads
  up to 366 days, allows an untouched today, and counts a day with a review or a
  newly learned card. Do not recompute the streak from `rangeActivity`; that
  capped a 12-day streak at 7 days on the 7D window.
- The range summary keeps Reviews, Learned, Practice, Streak, and the
  practiced-days/best-day line. The review-quality card owns the grade split,
  pass rate, average interval, pace trend, and task mix. Do not duplicate those
  numbers in the header.

## Scheduling and research models

- FSRS-7 and SBD are the advertised schedulers. Both keep a state on every card
  and must stay current; a target-recall change affects future intervals only.
- Target recall is a 70–97% setting with 90% as the recommendation. Do not
  hard-code 0.90 in review, interval-preview, or load-estimate paths.
- FSRS-7 and SBD are the only schedulers. RWKV, ONNX, Piper, and neural voice
  support were removed before 1.0.8. Do not add a research model back as a side
  effect of another feature; a return needs a full port, a reviewed package
  plan, and landing/store wording.
- Credits for FSRS and SBD research belong in the README and Settings → About.
  Keep the licensing wording precise.

## Study lists

- A study list is a collection, not a filter. Studying a list limits the session
  to its characters. Grades still count in the schedule; the rest of the daily
  queue is unchanged. Word practice stays free and never creates cards.
- Lists hold characters and words together. Words stay as text, resolve their
  meaning from the dictionary, and fall back to writing-only practice.
- Imports stream line by line (TXT/CSV/TSV) or through the streaming JSON string
  scanner. Do not read a whole file into memory or cap the number of items.
  Strip a UTF-8 byte-order mark before the first line. Only the root JSON object
  may name the list; nested item `name` fields must not rename it.
- Keep copy plain and professional. Name lists for what they hold, not for a
  mood.

## Feedback and readings

- Feedback colours are semantic, not decorative. Mint means clean, peach means
  turn the stroke around, sky means order or a different reading, and butter
  means a stroke is waiting. Do not use red for a missing stroke or a
  recognition difference; red reads as failure.
- Free practice shows the same handwriting check, coach card, repair modes, and
  shape evidence as a review. The shared component is
  `ui/components/StrokeCoachFeedback.kt`; do not fork it.
- Pinyin display defaults to tone marks (`nǐhǎo`). Numbers (`ni3hao3`) are an
  opt-in Settings choice. Pronunciation input accepts numbered pinyin, tone
  marks, or plain letters regardless of the display setting. Typing practice
  always shows and expects numbered pinyin, and printed worksheets always use
  tone marks; the display setting changes cards and cues only.
- One-time migration `pinyinToneMigrated` moves old installs from the 1.0.1
  numbered default to tone marks. A choice made after the migration sticks.
- The character grid resets its scroll when the script changes. Do not keep the
  old index: the same index is a different character in another course.

## Reminders and foldable

- Study reminders are off by default. Settings → Reminders has a switch, an hour,
  and day chips. Turning them on requests `POST_NOTIFICATIONS` on Android 13+.
  The app never asks before the learner opts in.
- `ReminderWorker` is a one-shot WorkManager job at the next selected hour and
  day. It schedules the next one after it runs and checks for due reviews or an
  allowed new lesson. If there is nothing to study, it stays quiet.
- The worker reads settings and progress locally and sends no data. The
  notification contains only counts, never a character, word, or meaning.
- Fold state comes from `androidx.window` in `ui/FoldState.kt`. A flat vertical
  fold is BOOK; a half-open horizontal fold is TABLETOP.
- On BOOK, the rail is replaced by a centered dock. Home is a two-page spread
  (action and word on the left, stats and calendar on the right). Learn and
  Practice use the wide canvas/controls layouts, Characters uses grid/detail,
  and filters wrap. The outer folded screen keeps the phone layout.
- Do not let the Home hero span the fold. Do not stretch a phone layout across
  the inner display.
- Test foldables with `adb emu unfold` and `adb emu fold`. `scripts/device.sh
  shot` captures to a device file and pulls it, which avoids the multi-display
  warning on foldables.

## Home-screen widgets

- Widgets are **RemoteViews**, not Compose. Only a limited view set is allowed:
  `LinearLayout`, `TextView`, `ImageView`, and a few others. A plain `View` is
  rejected by the launcher. Use an empty `TextView` for a coloured accent.
- Two providers live in `widget/`: `TodayWidgetProvider` and
  `WordWidgetProvider`. They read a `WidgetSnapshot` on `Dispatchers.IO` through
  `goAsync()` and update the launcher. Do not add a background service.
- The Today copy changes by day part (morning, afternoon, evening, night) and by
  due count, new lessons, vacation, and all-clear. At night it never pushes; it
  says the reviews will keep until morning.
- The Word widget uses the same `featuredWord` as Home and the same study day
  (`settings.today()`, not `LocalDate.now()`). The daily word is always
  non-mature, 2–6 characters, and filtered away from variants, surnames, place
  names, archaic/rare/dialect words, and abbreviations. The curated list is
  shuffled once per course, then a deterministic hash of the common pool; the
  cycle index is the study day only. The same day and course are shared by every
  install and do not change with progress or the mature setting. Never let
  mature content leak into the daily word, even when the dictionary mature
  setting is on.
- `WidgetUpdater.updateAll` refreshes on app resume, on stop, and when the
  system date/time/timezone changes. `WidgetRefresh` schedules one inexact
  alarm at the next day-part or study-day boundary (5/11/17/22 plus the
  day-start hour); the XML `updatePeriodMillis` is a six-hour safety net, not a
  half-hour poll. Keep the update cheap and do not add a service.
- Widget counts must use the same hidden-character filter as Home: skipped and
  suspended cards never appear in the widget's due or new counts.
- The widget pet is a static `layer-list`: the 1024 art plus a 1024-viewport
  accessory vector. `WidgetLook.pet(PetProp)` picks the hat, monocle, or party
  overlay. Keep new accessories in the same square viewport so they align
  without per-widget offsets.
- The default profile avatar and the small top icon use Mò, the launcher pet:
  `InkMascot` with the brush, plus the sparkle on the profile avatar. Do not fork
  a second pet.
- The cold-start welcome holds for at least 1,050 ms (520 ms with reduce-motion)
  so a fast device does not flash it. A tap still skips.

## Content QA backlog

Every character, word, reading, meaning, and story users see must be checked
before the next content release. The working checklist is
`docs/content-qa-backlog.md`. The screen and journey risk list is
`docs/archive/user-journey-risk-audit.md`. Known gaps: Korean coverage for 115 rare
Hanja, 29 rows without a decomposition, a full TalkBack/large-text pass, and
some etymology quality.
- Dependency policy: `docs/dependency-updates.md`.
- Practical agent playbook and traps: `docs/agent-playbook.md`.
- Local journey harness: `qa/README.md`; debug-only setup bridge is `composeApp/src/androidDebug`.
- Flow galleries: `scripts/flow-gallery.sh <flow> --name <run>` writes
  `qa/gallery/<run>/phone|tablet|fold/<flow>.png` and an `INDEX.md`; use it
  for visual reviews, not for store assets.
- Learning-science proposals and the joy roadmap: `docs/learning-proposals.md`.
- Current release plan and maintenance checklist: `docs/backlog.md`. The iOS/KMP plan is parked at `docs/ios-kmp-plan.md`.
- Spell quest is the extra-practice spellbook: four scripts (Simplified,
  Traditional, Japanese kanji, Korean Hanja), 35 families, 10 chapters, ~350
  authored choices per script, forge-only puzzles, and two-page tablet/fold
  layouts. Every puzzle is a family door, a whole-chapter door, or one exact
  shape; chapters mix all three. Working state and traps:
  `docs/spellbook-progress.md`. Design intent: `docs/spellbook-game-plan.md`.
  Keep it a quiet extra tool; the main path stays writing review.
- Screen, flow, and feature inventories: `docs/screen-inventory.md`,
  `docs/flow-inventory.md`, `docs/feature-inventory.md`.
- Test coverage: `docs/test-matrix.md`; risk audit: `docs/archive/user-journey-risk-audit.md`.

## Crash reports and About

- `diagnostics/CrashHandler` writes at most three files to app-private
  `filesDir/crash`. They are excluded from backups and sync. There is no crash
  SDK, and nothing is uploaded automatically.
- Settings → About → Report opens a composer with kind chips, words, an optional
  saved crash, a preview, and Send/Copy through the learner's own app. The crash
  file is removed after send or copy. A rate row opens the Play listing.
- The kind labels are `Something broke`, `An idea`, and `Content looks wrong`.
  Keep the content chip generic; do not make it word-only.
- The report sheet must be centred in landscape and keep its actions above the
  keyboard. `report-flow` covers phone, landscape, tablet, fold, dark, and 1.5x.
- The iOS launch reports no crash attachment; do not promise crash traces there.

## Answer privacy

- A writing recall must never show the target character, a word containing it,
  or a stroke guide unless the learner explicitly asks for help. The journey
  harness checks this.
- A pronunciation review must never show the expected reading, including inside
  a meaning string. `learn/ReadingPrivacy.kt` masks kana and Hangul self-describing
  meanings ("hiragana for a", "syllable ga"). Keep this covered by tests.
- Recognition results and grade buttons appear only after the learner commits to
  an answer and asks to check.

## Reveal review

- Settings → Practice has two independent switches: **Writing: tap to reveal**
  and **Pronunciation: tap to reveal**. Both default off.
- Writing reveal-only: the cue appears first, then **Show answer** shows the
  character, reading, meaning, and grades. There is no canvas. New cards go
  Meet → Reveal, skipping Trace.
- Pronunciation reveal-only: the character appears, then **Show answer** shows
  the reading and grades. There is no text field. Hide the "You typed" chip and
  do not suggest a grade.
- The answer reveal is still a real review. Grades schedule normally, XP counts,
  and the session order is unchanged. Do not mark these as practice-only.
- Keep the switch between writing and pronunciation tasks independent. A learner
  may want to type readings but not draw, or draw but not type.
- `ReviewTask.revealsAnswer` is the single mapping; do not duplicate it in the UI.

## Meaning checks and synonyms

- Meaning is an optional review task, off by default. Writing stays the default
  and keeps at least half of the reviews when meaning and pronunciation are on.
- New cards never start with meaning. Meet introduces the meaning first; reviews
  can ask for it.
- The meaning task is self-graded. Do not auto-grade English synonyms. The
  learner types a wording if they want, taps **Show answer**, compares it with
  the clean gloss and a real example word, then grades.
- Sanitize before teaching: `settings/MeaningSanitizer.kt` rejects blank,
  placeholder, overlong, and structural glosses such as "variant of", "surname",
  "see ...", "used in ...", "radical", or "component". Example words with those
  glosses are skipped. Phonetic self-descriptions such as "hiragana for a" or
  "syllable ga" are not meaning checks; those cards fall back to writing and
  pronunciation. A character with no teachable gloss also falls back to writing.
- Synonyms are user wordings keyed by character id in
  `AppSettings.meaningSynonyms`. They can be added or removed in the meaning
  reveal and in character detail; keep the two editors consistent. They are trimmed, deduplicated ignoring case,
  capped at 12 per character and 60 characters each, cleaned on restore, and
  removed when the learner taps the chip's x. They travel in settings backups.
- The reveal shows the official gloss, the learner's words if typed, accepted
  synonyms, and one or two non-mature example words. Highlight the target
  character inside the word. Keep the tone warm: "Your words", "Also accepted",
  "In a word", "How did that feel?".
- Record the task as `MEANING` in the review log. Show it in task stats and
  character history. Do not count it as writing pace or strokes.

## Navigation and loading

- `qa/scenarios.py:navigation-deep` is the back-stack regression: every Settings
  section, Compare, Paper, Practice picker, study lists, and the study tools
  sheet. Run it on phone and tablet after changing headers or overlays.
- Phone chrome is part of the layout contract. The floating bottom bar overlays
  the shell; the content viewport must meet its top edge, not a fixed `72.dp`
  inset. `BihuaApp` measures the bar outer height with `onGloballyPositioned`
  before the `appNavigationBarsPadding`/outer padding modifiers and applies that
  height to the content box. Do not hard-code the old inset again.
- Settings is wrapped in a `Scaffold`. Set
  `contentWindowInsets = WindowInsets(0, 0, 0, 0)` because the shell already
  owns status/navigation insets. Leaving the default double-applies the bottom
  inset and leaves the Settings list short of the bar.
- `nav-gap` is the shell regression. It measures the scroll viewport against
  the visible bottom tabs at 1× and 1.5× and fails above 14 dp of gap or below
  −2 dp of overlap. Run it after touching the shell, Settings Scaffold, insets,
  navigation labels, or font scale.
- System back must close overlays one layer at a time: Settings section, then
  Settings; paper/Compare, then the character; word queue, then the character or
  Home. Never leave a screen that exits the app while an overlay is open.
- Loading must end in success, an empty state, or a readable error. Handwriting
  model downloads and recognition have timeouts. Content loading has Retry.
  The first read-aloud tap is queued while the voice engine starts, and a voice
  engine that never answers fails after 5 s instead of queueing forever. A review
  or practice check uses the 900 ms local readiness probe, never a 20 s download
  wait: if the reader is not ready, shape and order feedback still show.
- Reader readiness is cached per script in `ModelDownloads`; a positive result
  is instant, a missing or stale one gets one short probe. Do not add per-screen
  `isModelReady` loops, repeated Play Services checks, or download polling.
  A completed download or a returning network re-checks the active script.
- A script switch hides the previous course until the new one loads; never let
  Home counts, Characters, or due counts flash the old script.
- Backup decoding, import writes, exports, and the onboarding restore run off
  the main thread. Widgets refresh on a short debounce after reviews rather than
  only when the app is backgrounded.
- Large text is a layout target, not an afterthought. Check 1.5x on Home Study
  tools, Compare, Paper, Settings, character detail, and session. Titles should
  wrap, not become a vertical strip; action rows may move below the title.
- On character detail, the synonyms header keeps “Your synonyms” whole by
  shortening the action to “Add” above 1.25×. Do not let a narrow title column
  break a word into pieces.
- The widget outfit dialog is two columns through 1.7×, then one column with a
  scrollable body. Keep every option visible or reachable; do not clip labels.
  Use `settings-outfit` at 1×, 1.5×, and a 2× visual pass.

## Dependency updates

See `docs/dependency-updates.md`. GitHub Actions and Gradle minor/patch updates
arrive as grouped PRs. Gradle, AGP, Kotlin, and Compose toolchain majors are
manual migrations and must not be merged one at a time. Keep app dependency PRs
separate from behavior changes.

## Build, test, install

```bash
source scripts/env.sh          # JDK 17
make check                     # compile, unit tests, content audit, debug APK, verify
make quick                     # compile only
make test                      # unit tests
make lint                      # Android lint
make run                       # check, install, launch on selected device
make journey                   # quick phone journeys
make journey-standard          # phone, fold, tablet core journeys
make journey-full              # every profile and daily state
make nav-gap                   # bottom bar/content inset at 1x and 1.5x
make release                   # signed minified release APK
make play-audit                # release APK/AAB permissions, signature, version, ABI
make smoke                     # on-device launch/navigation smoke test
```

GitHub Actions runs **unit tests, content audit, and lint only**. It does not
assemble or upload an APK. The debug APK is a local build artifact.

The Makefile sets `SHELL := /bin/bash` because `scripts/env.sh` uses
`BASH_SOURCE`. Do not remove it: Ubuntu CI runs recipes with `/bin/sh`, and
`source` plus `BASH_SOURCE` fail there. Use `. scripts/env.sh` inside recipes.

Use the device tools instead of ad-hoc adb:

```bash
scripts/device.sh status
scripts/device.sh ui                          # visible labels + coordinates
scripts/device.sh find "Start studying"
scripts/device.sh tap "Start studying"
scripts/device.sh type "ai4"
scripts/device.sh draw 300 500 500 700 700 900 # DOWN/MOVE/UP pen path
scripts/device.sh swipe 540 1900 540 700
scripts/device.sh shot before
scripts/device.sh a11y                         # clickable nodes with no spoken label
scripts/device.sh keyboard on                  # force the soft keyboard on
scripts/device.sh session-clear                # drop a saved lesson session
scripts/device.sh db-cards
scripts/device.sh db-reviews 20
scripts/device.sh seed-review 爱 SIMPLIFIED 2 0 # due now, reps=2
scripts/device.sh seed-learning 一 KOREAN 1 10 # due in 10 minutes
scripts/device.sh content [--bump]
```

With more than one device connected, set `BIHUA_DEVICE=<serial>` or pass
`-s <serial>`. DB commands need a debug build. The progress DB is
`databases/bihua_progress.db`.

## Rotation and shell swaps

- Rotation is a configuration change, not an activity recreation. The manifest
  handles orientation, screen size, layout, keyboard, UI mode, and
  `fontWeightAdjustment`; without that last flag, the activity is recreated on
  some devices and local state disappears.
- The phone and tablet shells are separate composition branches. Any state that
  must survive a rotation across the shell breakpoint has to live above the
  branch or in a stable saveable holder. The Settings section, report-open
  state, report words, and report kind are hoisted to `BihuaApp`.
- The review uses the two-pane layout from 760 dp on a non-book screen and
  700 dp on a book. This keeps the canvas visible on the fold outer display and
  tablet portrait instead of letting a long prompt squeeze it away.
- Keep the review session, ink, typed answer, and reveal state above the wide
  branch. `rotation-state` proves the card, ink, answer, report draft, Settings
  section, and Practice survive a turn.
- The wide review uses the sticky action bar for Trace/Recall and the sticky
  grade bar after the reveal. Do not put Show answer or Check back inside the
  scrollable side panel on a short landscape screen.
- The Home hero is compact under 560 dp so the primary study action stays above
  the floating bottom bar.
- `rotation-onboarding` proves the script page and step survive a turn.
- `screen-pass` starts from clean app data (`fresh_data`); do not let a stale
  grid filter or scroll position leak between profiles. Stay on the character
  grid before the draw-search detour, and try the hierarchy fallback for short
  Settings rows.

## Device and screenshot traps

1. **Never derive tap coordinates from a screenshot.** Use `device.sh ui/find`
   or tap by label. The screenshot is scaled.
2. **Tap by label.** The bottom navigation now has fixed positions and labels for
   every tab, but dialogs and keyboards still move content.
3. **Prefer `device.sh draw`.** `input swipe` works for touch on most devices but
   can be unreliable for stylus input. `device.sh draw` sends motionevents.
4. **Emulators and desktop input.** `hasStylus` treats emulator/goldfish/ranchu as
   touch, and `InkCanvas` accepts mouse pointers. Finger drawing is on by default
   everywhere, including stylus phones; onboarding and Settings offer a
   stylus-only choice. A pressed stylus wins over a palm or finger.
5. **System animation scale 0 kills Compose animations.** Essential playback uses
   `withFrameNanos` (`ink/StrokeGuide.kt`, `ui/CompareScreen.kt`). Do not build
   essential behaviour on `Animatable`.
6. **The keyboard moves the layout.** Dismiss it before tapping Check/Grade, or
   re-dump with `device.sh ui`.
7. **Tablets use a navigation rail; phones use a labelled bottom bar.** Test
   both. The wide threshold for Characters is 780 dp; Practice uses 900 dp.
8. **Tablet emulator:** `bihua_tablet`, Pixel Tablet, 2560 × 1600, 320 dpi.
   Phone emulator: `voxbloom`.

## Lesson and review flow

- A lesson is **Meet → Trace → Write** by default.
- With Reveal writing on, a lesson is **Meet → Reveal**; Trace is skipped.
- Trace needs at least one stroke when it is present.
- A reviewed card starts directly at Recall. Do not show Meet or Trace first.
- Write cue is reading, meaning, or a word. The character is never shown before
  the learner reveals it.
- After writing, tap **Check & grade**. A fixed grade bar shows the answer,
  reading, meaning, four grades, the app suggestion, and Draw again.
- Pronunciation shows the character, asks for the reading, then shows a large
  feedback card (`Correct!`, `Almost — the tone`, `Not quite`, or `Answer`).
- One review asks for one skill:
  - `WRITE`: draw from a cue.
  - `PRONOUNCE`: show the character, type the reading.
  - `MEANING`: show the character, type a wording if desired, reveal the gloss.
- New cards write, unless Reveal writing is on. Reviewed cards alternate writing
  and pronunciation from a deterministic phase; pronunciation is opt-in.
- With Reveal pronunciation on, a pronunciation card shows the character, then
  Show answer, then grades. No text field. Do not suggest Again just because
  the answer was revealed.
- A pronunciation cue must never include the reading before the check. Writing
  cues must never include the character.
- Only the first grade is scheduled. Later repeats are practice.
- A failed card returns later in the session. A failed stroke may offer
  `Fix stroke N`, which replays and validates one isolated stroke.
- The handwriting check is a hint. It can miss or misread a stroke. The learner
  always chooses the grade.

## Session order and progress

- Settings → Study has **Session order**: Reviews first, Mixed, Lessons first.
- Mixed uses `mixInterval` (1–10 reviews before one new character).
- **Progress bar** can be Full session or Next lesson. Next lesson fills the
  current block and resets at the next lesson; it shows “Next lesson in N cards.”
- The top bar is animated and semantic; it fills through lesson steps and graded
  reviews.
- The pace estimate uses a realistic Good/Hard/Again mix, a completed course, and
  a 120-day window after completion. At 10 new/day it shows roughly 88 average
  and 113 busy-day reviews, and about 11 months to finish. Do not revert this to
  the old “settles near 30” copy.
- A saved lesson is per script. Discard asks for confirmation. Reset, import, and
  sync clear paused lessons.

## Transfer practice

- Word practice and paper check are free practice: never create cards, XP, or
  review-log rows.
- Paper check hides the character and shows only reading + meaning before reveal.
- Word practice resolves every character in the current script and refuses mixed
  availability.
- Word practice: cue from reading + meaning, keep the word hidden until Show
  answer, one character at a time.
- Character detail has top actions for Write a word and Write on paper before
  the sharing controls.

## Local sync

- The sender generates a per-session self-signed certificate; the pairing code
  is the first 50 bits of the SHA-256 fingerprint. The receiver pins it. No cloud
  service is involved.
- `SyncClientTest` and `SyncProtocolTest` cover the correct pin, wrong pin, 403,
  500, settings-only, progress-only, both, malformed, and empty archives. They
  use a real local TLS socket and need no Wi-Fi or NSD.
- Sync is Android-only in 1.1.2. The iOS launch has no sync, TLS, or custom
  networking.

## Ink and guides

- Stroke order rules are a reference dialog in Settings → Writing; the actual
  character guide is authoritative.
- Canvas is square; guide coordinates scale x/y independently.
- Completed strokes are immutable. `InkCanvas` caches smoothed geometry and
  segment widths by canvas size, density, brush, scale, and tool. Only the live
  stroke is rebuilt per frame. Preserve taper, pencil dots, and playback
  fractions.
- Guide scaffolds: full strokes for new cards, outline for early reviews,
  hidden for mature cards. Initialise visibility synchronously; never flash the
  guide for one frame.
- `StrokeOrderGrader` stroke numbers are 1-based, including the empty-ink path.
- Brush presets: Ink, Pen, Pencil, Marker. Stylus pressure is averaged and eased;
  speed thins; ends taper. Finger input has no pressure. The Spell quest offers a
  brush tray inside its draw overlay; that choice is quest-local and must never
  write back to the app-wide brush setting.
- The spell-quest reference guide is the scene's chapter colour at 65% opacity,
  drawn under the ink canvas. The grimoire gift/result strokes use the same
  source without a dark halo. The transparent paper colour must stay a no-op;
  only the quest calls use `paperGap = false`.
- Spell rows are script-scoped (`SIMPLIFIED`, `TRADITIONAL`, `JAPANESE`,
  `KOREAN`). Never mix script rows in a grimoire or quest; readings and stroke
  guides come from the same course. Spellbook parsing is cached per process.
- Generated copy must stay plain and authored-feeling: short first-gloss notes,
  no "A water spell" formulas, no contradictory time/weather places, and the
  result body only says what happened. Keep the note in the small anchored
  popup, never as a tall card that eats the result line. The scene ring and its
  inner circle share one line weight; the cast ring and its paper share another.
- Spell-quest copy stays kind and neutral. No gendered roles or stereotypes in
  places, obstacles, or outcomes; no adult, substance, or weapon glosses where a
  plainer meaning exists. Dictionary furniture such as "surname", "classifier
  for", or an unmatched bracket never ships as a note. `SpellbookContentTest`
  and `ShippedPuzzlePartsTest` guard both rules.
- The daily word is a shared calendar slot, not a personal pick: seed it from the
  study day and course only. Never use install time, progress, or the mature
  setting. Two fresh installs on the same day must return the same word.
- An obstacle carries place kinds (`obstacle_place`) and a setting carries place
  kinds (`setting_tag`); the forge only pairs settings that share a kind. Keep
  both tables populated for every new obstacle and place.
- Brush state is per tool. `AppSettings.brushPresets` is the saved map;
  `brush` is the active tool's copy. Select with `withTool`, edit with
  `withBrush`, and seed onboarding with `withToolBrush`. Do not write `brush`
  without updating the map, or the next `sanitized()` will overwrite it.
- Korean Hanja and Japanese kanji casts must use the Han recognizer (pass a Han
  target, or `StudyMode.TRADITIONAL` for Korean). Do not let the Hangul/kana
  model decide a Han spell.
- Quest met pages and the chapter live in the progress database
  (`spell_quest_met`, `spell_quest_road`) so export/import/sync carry them.
  Never store quest progress in SharedPreferences. Schema changes need an
  idempotent migration and a `ProgressStore` test.
- Puzzle solutions can be wider or narrower than a family. `Solution` may name
  families, categories, or exact characters; the solver checks the narrowest
  rule first. Keep the scene's own spell able to solve its scene, including when
  a chapter puzzle picks two categories.
- The Practice screen fills the screen: title, character summary, weighted square
  canvas, compact Guide/Order/undo/eraser row, and Check. Result opens in a
  compact scrollable dialog.
- The Practice character picker defaults to the first curriculum character and
  has search by character, meaning, or reading.

## Settings

- Settings opens on a searchable home list of grouped rows:
  YOU / LEARNING / WRITING / YOUR DATA / ABOUT. On a phone, Data and Sync are
  below the first screen; scroll or use search.
- Rows open focused subpages with a back arrow. Android Back returns to the
  settings home while a subpage is open.
- All sections are reachable: Profile, Practice, Study, Writing, Display, Data,
  Sync, About.
- Do not reintroduce the old horizontally scrolling category chips or the
  three-row chip grid.
- Writing has the active-script background model download; Prepare models downloads
  all four in parallel and can be cancelled. The reader downloads on any usable
  connection (mobile data is fine), starts only after a script is picked during
  onboarding, and a quick model must not flash its notice right after the splash.
  A failed notice is tappable to retry and is scoped to the active script.
- About is transparent about Google ML Kit diagnostics and has Send feedback and
  Source code actions.
- Schedule is intentionally compact: profile dropdown, status, Fit, Import,
  Export, and a More menu. Long explanations belong in the info dialog, not on
  the page. Do not bring back the chip wall and paragraph stack.
- Known-list import accepts one character or short word per line, or characters
  separated by commas. Latin text, punctuation, and long prose lines are skipped;
  a preview states how many characters will be marked before Confirm. Imported
  cards get stable due dates spread over 10–45 days with a per-day cap, never one
  giant due day. The write is one transaction.
- Data and Sync use `TransferScope`: Everything, Settings, or Progress. Import
  and sync confirm dialogs use checkboxes for the parts present in the file.
- Slow actions must show feedback: `BusyDialog` for blocking work, inline
  progress for the weight fit, and a snackbar after completion. A tap should
  never look ignored.
- The TTS engine is warmed at start/onboarding/script switch; a missing voice
  shows a Toast instead of silently doing nothing. Initialization has a 5 s
  timeout and exposes `SpeechState`, so a dead engine cannot hold a queued tap.
- Rotation/theme/font-scale must be tested. The app supports 1.3×+ text.

## Scheduling and data

- Progress DB schema 18. `review_log` stores `elapsed_ms`, `task`, `strokes`, and the optional `reason`; `word_practice` stores `mature`; `mnemonic` stores a page per character with a 1–5 vividness; `palace_peg` stores reusable parts/sounds. Schema 18 adds report indexes: card mode/due, card mode/introduced, character/time review history, live-time, and training-log indexes. Content DB builder adds mode/list, variant, and mode/word indexes for future content builds.
- Bump the schema and add an upgrade path when tables change.
- Progress ranges and custom date windows share `ProgressWindow`; a custom end date must clamp to today and every query must carry both bounds.
- Content DB contentVersion 34. Bump it whenever the asset changes.
- Keep scripts pure: no simplified-only forms in Traditional, no
  traditional-only forms in Simplified; shared glyphs stay in both.
- Traditional words come from CEDICT word pairs, not character substitution.
- Korean uses Unihan/Hangul. Word examples come from the Korean Wiktionary
  extract, ranked by FrequencyWords, with explicit examples tagged mature.
- Backups include settings and progress. Zip format is `backup.json` +
  `progress.db`; import accepts the zip, a settings-only zip, a progress-only
  zip, and legacy settings-only JSON. `TransferScope` decides what is written.
- Settings and profile are part of every sync package. The receiver chooses
  whether to apply settings, progress, or both.
- Sync defaults to merging progress: cards keep the newer state, review logs
  union without duplicates, notes keep the newer text, and XP is recomputed.
  Settings remain an explicit choice. The incoming progress DB must be on the
  same schema version as the receiver.
- Drawing practice is opt-out. If it is off, at least one of pronunciation or
  meaning stays on, lessons skip the trace step, and the review task plan never
  returns WRITE unless no check is enabled.
- Grade reasons are a note, never a second grade. They are written after the
  review and must not change FSRS, XP, or the review count.
- The Mnemonic palace is opt-in. Chinese uses initial/final/tone roles; Japanese
  uses kanji parts; Korean uses Hanja parts. Kana and Hangul must not show the
  palace card or the lesson/review book button.
- Do not call `enableEdgeToEdge()` and do not set status-bar or navigation-bar
  colors directly. The 1.0.2 build was flagged for
  `Window.setStatusBarColor`, `Window.setNavigationBarColor`, and
  `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`. `LocalEdgeToEdge` in
  `ui/EdgeToEdge.kt` applies system-bar padding only on Android 15+, where the
  platform draws edge-to-edge by default. Keep the `MainActivity`
  inset-controller icon colors on API 35+; older versions use the theme's inset
  bars. After a release build, check the DEX strings for those three APIs.
- After a review is submitted, the canvas is locked (`InkCanvas(enabled=false)`).
  Do not place a Draw again button over the drawing; the grade bar action is the
  only restart. Playback stays available and editor tools are disabled.
- Practice and review use `InkHistoryPlaybackBar` so history and playback are
  aligned. Practice must keep play, reverse, previous, count, and next.
- `AppSettings.onlyAgainAndGood` hides Hard and Easy in the grade bar and
  pronunciation check. Scheduling still accepts all four grades.
- FSRS weights live in `AppSettings`. `fsrsWeights` is the shared profile;
  `fsrsProfiles` overrides it per script. `parametersFor(mode, glyphGroupId)`
  uses the shared profile whenever the glyph is shared, so a shared card never
  changes models between scripts.
- The local fit (`FsrsOptimizer`) needs 400 reviews. It uses the recent 2,000
  by default or all reviews when chosen, checks a time-based holdout, and keeps
  the current weights if the holdout does not improve. It is not the official
  Rust optimizer; do not claim parity.
- `DayBoundary` is the single source for the study date. `dayStartHour` defaults
  to 4 AM and ranges 0–6. New counts, activity, forecasts, study days, XP days,
  skips, and the calendar all use it. `clockTick` and lifecycle resume refresh
  the day when the app stays open.
- `suspendedIds` is permanent. `skippedUntil` is one day. Both are excluded from
  sessions; Study tools shows them separately.
- Reset all learning deletes cards, reviews, XP, confusions, and word-practice
  history; it keeps notes, study list, and settings. Paused sessions are cleared
  by the Settings screen after reset.
- Vacation mode: due count 0 and sessions refuse to start. Ending shifts
  `due_at`/`last_review_at`, never review-log timestamps. Ease-back caps
  sessions at 10 cards for seven days.
- Local sync is opt-in, same Wi-Fi, a 10-character pairing code, and a short-lived
  TLS server with a per-session certificate pinned by that code. Do not add
  cleartext back.
- Android cloud backup is disabled.

## Testing expectations

- Pure logic belongs in a unit test. Current suites include FSRS, queue, session
  progression, pinyin, known lists, stroke grading, order grading, guide
  scaffold, reading checks, task planning, backup archive, search, and formatting.
- `make check` must be green before commit. The pre-commit hook runs compile.
- Verify UI changes with `device.sh shot`; use seeded reviews/learning cards when
  a flow needs progress.
- `tools/seed_fsrs_fit.py <progress.db> [cards-per-mode] [reviews-per-card]`
  writes a large per-script and shared-glyph history. Pull the debug DB, run the
  script, push it back, then fit a profile from Settings → Schedule.
- Test phone and tablet, light and dark, 1.3× text, emulator/mouse, and a real
  stylus device when available.
- When adding a Progress query, add it to `ProgressStoreModeTest` so a review
  from another course cannot leak in. When changing shell insets, run
  `nav-gap` at 1× and 1.5×. When changing a large-text header, rerun the
  relevant journey with `--font-scale 1.5` and inspect the screenshot.
- The standard matrix is the release gate. `nav-gap`, `progress-ranges`,
  `progress-empty`, `settings-outfit`, and `grade-help` are part of it.

## Files worth knowing

| Area | Files |
|---|---|
| Shell, tabs, sessions, TTS, bottom bar | `ui/BihuaApp.kt` |
| Home / Characters / Progress | `ui/HomeScreen.kt`, `ui/CharactersScreen.kt`, `ui/DashboardScreen.kt` |
| Lesson, tasks, cues, grading | `learn/LearnScreen.kt`, `learn/SessionProgression.kt`, `learn/ReviewTask.kt`, `learn/ReadingCheck.kt`, `learn/Grading.kt` |
| Ink, guides, grading | `ink/InkCanvas.kt`, `ink/InkModel.kt`, `ink/StrokeGuide.kt`, `ink/StrokePlayback.kt`, `ink/StrokeOrderGrader.kt`, `ink/StrokeGrade.kt`, `ink/StrokeCoach.kt`, `ink/Stylus.kt` |
| Scheduling and progress | `fsrs/Fsrs7.kt`, `fsrs/FsrsWeights.kt`, `fsrs/FsrsOptimizer.kt`, `fsrs/ReviewLoad.kt`, `progress/ProgressStore.kt`, `progress/ReviewQueue.kt`, `settings/DayBoundary.kt` |
| Content and search | `content/ContentRepository.kt`, `content/CharacterSearch.kt`, `content/Romanization.kt`, `content/Pinyin.kt` |
| Backup and sync | `backup/BackupArchive.kt`, `backup/TransferScope.kt`, `sync/Sync.kt` |
| Settings | `settings/SettingsScreen.kt`, `settings/Models.kt`, `settings/DayBoundary.kt` |
| Dictionary and print | `ui/DictionaryScreen.kt`, `settings/WorksheetScreen.kt`, `worksheet/WorksheetPdf.kt` |
| Mascot and opening | `ui/components/Components.kt` (`InkMascot`, Mò), `ui/components/OpeningInk.kt`, `ui/theme/Color.kt` |
| Guided tour | `ui/GuidedTour.kt` (MoActor, anchors, overlay), `ui/BihuaApp.kt`, `settings/SettingsScreen.kt` |
| Widgets and pet costumes | `widget/WidgetCopy.kt`, `widget/WidgetLook.kt`, `res/layout/widget_*.xml`, `res/drawable/widget_pet_*.xml` |
| Tooling | `scripts/device.sh`, `scripts/check.sh`, `scripts/verify-apk.sh`, `scripts/journey.sh`, `scripts/flow-gallery.sh`, `qa/gallery.py`, `Makefile`, `tools/seed_fsrs_fit.py` |
| Coverage map | `docs/ui-test-coverage.md`, `docs/test-matrix.md` |
| Local journeys | `qa/journey.py`, `qa/state.py`, `qa/driver.py`, `qa/scenarios.py` |

## Release assets

Landing-page screenshots, the OG image, the Play feature graphic, and store
screenshots are refreshed **after** the UI and versionCode are frozen. Keep the
source captures and store files local in `store/`; the repo stays app-focused.
See `docs/learning-proposals.md` for the final-asset checklist.

The canonical landing is `docs/index.html`, served at
`https://shadowsx3.github.io/bihua/`. Test APKs live in `docs/download/` and are
force-added because `*.apk` is ignored. `bihua.app` is a separate Vercel site;
changing `docs/` does not update it.

After every source change that reaches a release, rebuild and re-smoke the APK,
then update all three places together:

1. Copy `composeApp/build/outputs/apk/release/app-release.apk` to
   `docs/download/bihua-1.1.2.apk`.
2. Put the new SHA-256 in `docs/download/SHA256SUMS.txt` and the landing card.
3. Change the landing download query to `?v=<first 8 hash chars>` so the Pages
   CDN cannot keep serving the previous binary. Verify the hosted bytes with
   `curl -sL <url> | shasum -a 256`, not just the HTML.

Do not describe an APK as hosted before the hosted checksum matches the local
signed build.

## Release and patch flow

1. Branch the Android candidate (`release/android-1.1.2`) and keep KMP/iOS work
   off it.
2. Run `make check`, `make lint`, the full screen-pass matrix, and the
   report/spell/onboarding/offline journeys.
3. Rebuild the signed AAB/APK after the last code change. Store the mapping,
   native symbols, and hashes.
4. Clean-install release smoke; repeat on the real phone when available.
5. Tag the release candidate. Upload to internal testing, run the pre-launch
   report, then staged rollout.
6. For a hotfix: branch from the live tag, keep the diff minimal, add a
   regression test, bump the versionCode, internal track, smoke, promote.
   Never reuse a published versionCode.

## Known gaps

- The physical stylus pass was done on the SM-X520; recheck after any ink change.
- Full backup/import through the system file picker should be retested on a
  release build before production.
- Robolectric Compose smoke tests and the `qa/` journey harness exist, but there
  is no full on-device instrumentation suite for every screen.
- Sync has robust JVM/TLS tests but no two-device NSD end-to-end test.
- iOS is planned, not started; there is no Apple developer account yet.
- Foldable, desktop, and XR screenshots are intentionally not supplied.
- The 1.1.2 release candidate still needs the full matrix green and a real-phone
  smoke before upload.

## iOS lessons that keep the port stable

- **Compose iOS text fields are `UITextView`.** XCUITest cannot use
  `app.textFields`. Tap the visible placeholder's frame with
  `coordinate(withNormalizedOffset:).withOffset(...)` and call `app.typeText`.
- **Scroll settling matters.** After `swipeUp()` to reach a Settings or list
  button, `usleep(500_000)` before `.tap()` or the tap can land on the previous
  content.
- **iPadOS 26 uses a top tab bar.** `app.tabBars.buttons` works on iPhone but
  not iPad; use the `tabButton` helper that also tries `segmentedControls` and
  `buttons`.
- **Set `.portrait` in `setUpWithError`.** A failed landscape test otherwise
  leaves the next test in landscape and hides Settings rows.
- **Interactive swipe-back needs an explicit delegate.** `RootNavigationController`
  owns `interactivePopGestureRecognizer` and only enables it when
  `viewControllers.count > 1`; test both character detail and review.
- **`SoftCard` content is a Box.** Put multiple children in a `Column`; otherwise
  they overlap. This was the Progress/Settings header bug.
- **The macOS host must be in portrait for store screenshots.** Use
  `scripts/ios-screenshots.sh`, which resets settings/progress and selects each
  route.
- **Nested Kotlin callbacks do not cross to Swift reliably.** Use a small
  pending object plus a top-level function (see `IosFileImport`) for the
  document picker; do not pass a block inside a block.
- **Backup and Start over are real features.** `IosSettingsStore` and
  `IosProgressStore` write JSON in Application Support; `IosStudyListStore`
  writes lists. Keep their file names and sanitizers stable.
- **Long UI sessions can drop the iOS 26 test runner.** A single `xcodebuild
  test` with 20 relaunches can restart after the duplicate WebKit
  accessibility-class warning. Use `scripts/ios-ui-tests.sh` (one fresh runner
  per test, also what `make verify-ui` calls). A restart is a runner failure,
  not an app failure, but do not ignore it.
- **Android is the referee.** Any change to `commonMain` must keep
  `make check`, `make lint`, and the Android APK audit green before an iOS
  change is considered done.
