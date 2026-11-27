# Agent playbook

Practical workflows and traps learned while shipping Bihua. Read this with
`AGENTS.md`; this file is the "how" and the sharp edges.

## 1. Start from a known state

```bash
cd /Users/bassmd/Documents/bihua
git status --short
source scripts/env.sh
make check
```

If `git status` is not clean, identify whether the files are user work before
editing or deleting. Do not commit `screenshots/`, APKs, or keystores.

## 2. Release APK build

```bash
make check
make lint
make audit
source scripts/env.sh
./gradlew :composeApp:assembleRelease --console=plain -q
shasum -a 256 composeApp/build/outputs/apk/release/app-release.apk
```

Verify the APK signature before installing it. The release build is invalid if a
source file changed after it was assembled.

When the landing page hosts the APK, update `docs/download/bihua-1.1.2.apk`,
`docs/download/SHA256SUMS.txt`, the landing hash, and the `?v=` cache-bust query
together. A Pages deploy can keep the old binary at the unversioned URL, so
compare the hosted bytes with `curl -sL <url> | shasum -a 256`.

## 3. Content database workflow

```bash
tools/fetch-content.sh
make content-bump
make audit
```

`make content-bump` rebuilds the database and increments `contentVersion` in
`ContentRepository`. A schema change in `bihua_content.db` also requires updating
the Kotlin queries and `ContentDatabaseTest`.

Content rules:

- A content change never touches the progress database.
- Word `word` is the script text. For Korean it is the Hanja spelling; `reading`
  is Hangul. Speech uses the Hangul reading for Korean.
- The word examples are context, not a second test.
- Explicit-only words are tagged `mature` and hidden by default.
- Japanese and Korean examples are ranked by FrequencyWords; Chinese keeps
  HSK/level ordering.
- Shape notes are honest structural notes, never invented historical etymology.
- Missing data stays missing rather than being guessed.

Useful checks:

```bash
sqlite3 composeApp/src/androidMain/assets/bihua_content.db \
  "select mode,count(*),sum(mature) from word_context group by mode;"
sqlite3 composeApp/src/androidMain/assets/bihua_content.db \
  "pragma table_info(word_context);"
```

Do not use SQL `LIKE` for regex-shaped audits. Use Python regex in
`tools/audit_content.py`.

When joining `char_meta` to `character`, remember a character can appear in
several scripts; that join multiplies rows. Query `char_meta` alone for its count.

## 4. Testing layers

1. **Pure unit tests** for algorithms: FSRS, queue, session progression, pinyin,
   romanization, reading checks, stroke grading, formatting, backup codec.
2. **Robolectric tests** for SQLite, migration, content database, import
   validation, reset, settings backup.
3. **Content audit** for shipped data.
4. **Emulator smoke** for screens and device-only behaviour.
5. **Physical device** for stylus pressure and real IMEs.

Hard-won test details:

- Robolectric test methods must return `Unit`. An expression body returning a
  Boolean fails with `InvalidTestClassError`.
- `ContentDatabaseTest` resolves `src/androidMain/assets/bihua_content.db` and
  `composeApp/src/androidMain/assets/bihua_content.db` so it works from the module or root.
- Test the backup round trip when adding a setting. A new `AppSettings` field is
  easy to forget.
- Test Progress queries with a second course in the database. `gradeMix`,
  `practiceTimeMs`, `writingPace`, `taskStats`, `activity`, and
  `learnedHistory` all take a mode; a null mode is global and must not be used
  by the Progress page. `ProgressStoreModeTest` is the guard.
- The streak is a 366-day current run (`studyStreak`), not a count inside the
  selected range. `StudyStreakTest` covers today-empty, gaps, and learned-only
  days; `progress-ranges` checks the 7D/30D value stays the same.
- Test default-off settings explicitly.
- If a builder pipeline adds a word/character field, add a content test that
  would fail if the field silently becomes empty or unsafe.
- Test the two-button grade scale when adding anything to `GradeChoices`.
- `make check` runs tests + audit + debug APK + verify. `make test` alone is not
  a release check.


## Widget and cold-start traps

- **RemoteViews is not Compose.** The launcher inflates the widget layout in its
  own process. A plain `View` is rejected (the preview says “Can't load
  widget”); use `TextView`, `ImageView`, `LinearLayout`, `FrameLayout`, and the
  other allowed classes. Use an empty `TextView` for a coloured accent.
- **Do not read the database on the main thread.** `AppWidgetProvider.onUpdate`
  calls `goAsync()`, loads a `WidgetSnapshot` on `Dispatchers.IO`, updates the
  views, and calls `finish()` in `finally`. Do not add a service or a wake lock.
- **Keep widgets cheap.** Refresh on app resume/stop, on the nearest boundary
  (top of the hour, a day part, or the study-day start), and on a six-hour
  provider period. Every widget shares `WidgetSnapshot.load` and
  `TodayWidgetBinder`; do not duplicate the content, progress, and settings
  reads.
- **The review widgets keep the 1.1.1 shape.** Today shows two big numbers
  (done of total) and one plain next-hours line; Week ahead keeps the numbers
  and swaps the line for the next days. Both keep the pet, the colored labels,
  and the Study button; no bars or chips. The amounts come from
  `ProgressStore.todayLoad`, which uses the same scheduler and skip rules as
  Home. Do not recompute due dates in the provider.
- **The daily word is a showcase.** It is always non-mature, even when the
  dictionary mature setting is on. It must be 2–6 characters and filtered away
  from variants, surnames, place names, archaic/rare/dialect words, and
  abbreviations. Curated words lead each cycle; the common pool is a
  deterministic hash. Keep at least a year of candidates per course.
- **The cold-start welcome needs a floor.** The animation is 620 ms, but a fast
  content read must not close it immediately. Hold it for at least 1,050 ms
  (520 ms with reduce-motion). Tap still skips.
- **Filled guides are expensive to parse.** Parse the outline once per guide in
  `toRenderStroke`; only transform it per frame. Re-parsing every animation
  frame causes visible jank.
- **One pet: Mò.** Use `InkMascot` for the launcher, profile avatar, top icon,
  opening, completion, and guided tour. Do not fork a second mascot drawing.
  The tour adds moods, a brush swing, and small held props; the widgets add
  hats, a monocle, a witch hat, a vampire cape, a nightcap, and a party hat.
  Settings → Display can pin one for the widgets. Still one pet.
- **Bottom bar and scroll viewport are one contract.**
  On phone, the floating bottom bar overlays the shell. The content viewport
  must end at the bar's top, not at a fixed 72 dp and not under the bar. The
  `nav-gap` journey reads the scrollable bounds and the tab bounds; use it before
  and after shell changes. On Settings, the `Scaffold` must not re-apply the
  navigation-bar inset.
- **The tour lives over the real app.** `GuidedTour.kt` uses live `TourAnchors`
  and a spotlight, not screenshots. Keep the overlay in-app so window
  coordinates match the anchors. Use one finite beat per stop; a perpetual
  animation stops TalkBack and UI tests from ever seeing an idle screen.

## 5. Compose and layout traps

- **Modifier order matters.**
  `fillMaxSize().widthIn(max = 760.dp)` does not cap the width because
  `fillMaxSize` fixes it first. Use `widthIn(max = 760.dp).fillMaxWidth()`.
- **Large text changes navigation.**
  The bottom bar shortens "Characters" to "Chars" above 1.15× font scale; the
  tablet rail is 104 dp. Test at 1.3×, 1.5×, and 2×.
- **Phone content meets the floating bar.**
  The shell used to reserve a fixed 72 dp above the bottom bar, which left a
  visible gap at 1× and could overlap at 1.5×. The bar now reports its outer
  height with `onGloballyPositioned` (before the navigation-bar and outer
  padding modifiers) and the content box uses that height. If you add an inset
  there, run `scripts/journey.sh --scenario nav-gap --profile phone` and the
  `phone-compact` run at 1.5×. The scenario fails above 14 dp of gap.
- **Settings has its own Scaffold.**
  The parent shell already owns window insets. Keep
  `contentWindowInsets = WindowInsets(0, 0, 0, 0)` on the Settings Scaffold;
  without it, the navigation-bar inset is applied twice and the Settings list
  ends short of the bottom bar.
- **Settings sections are LazyColumn content.**
  Their bottom padding is not a substitute for shell insets. Do not add another
  fixed bottom gap to hide the bar; fix the shell measurement instead.
- **Clickable Surface semantics.**
  `Surface(onClick = ...)` plus a child `semantics { contentDescription }` can
  produce an empty clickable node with a separate description child. For custom
  cards, use `Modifier.clickable(...).clearAndSetSemantics { ...; onClick { ... } }`.
- **Keyboard layout.**
  The pronunciation input sits in the upper third of the card. The screen uses
  `adjustResize`; content is top-aligned so it does not jump. Do not centre the
  input vertically or put it behind the IME.
- **Tablet columns.**
  Cap forms/settings at a readable width; do not stretch rows across 2560 px.
  Characters uses a detail side panel; Practice uses a canvas and control column
  with the title above both.
- **Home hierarchy.**
  Keep the hero/Continue, Word of the day, stat cards, and calendar above the
  fold. More holds level, recent characters, and browse. Do not turn Home back
  into an executive dashboard.
- **Canvas purity.**
  Never draw decoration under the writing sheet. The guide must remain readable
  in light and dark.
- **Animated essentials.**
  System animation scale 0 stops Compose animations. Stroke playback and compare
  use `withFrameNanos`, not `Animatable`.

## 6. Emulator and UI automation

The emulators are slow to cold start. A screenshot 5 seconds after launch may
show the launcher or "Preparing your course" while the app is still starting.
Wait 15–25 seconds before calling it a crash.

Use `scripts/device.sh`:

```bash
scripts/device.sh status
scripts/device.sh shot name
scripts/device.sh ui /tmp/ui.xml
scripts/device.sh a11y
scripts/device.sh keyboard on
scripts/device.sh session-clear
scripts/device.sh tap "Start studying"
scripts/device.sh find "Resume"
scripts/device.sh draw 300 500 500 700
scripts/device.sh swipe 540 1900 540 700
scripts/device.sh seed-review 山 KOREAN 3 0
scripts/device.sh seed-learning 一 SIMPLIFIED 1 10
scripts/device.sh db-pull /tmp/progress.db
```

Tricks:

- Screenshots are scaled. Do not derive `adb input tap` coordinates from a
  screenshot; use the UI dump bounds or tap by label.
- A soft keyboard may stay hidden on the emulator if a hardware keyboard is
  attached. Use the keyboard command in `device.sh` when testing IME layout.
- If the app is on a saved session, seeding a new due card will not show on Home.
  Discard the saved session or test in a script without a session.
- `screenshots/` is local-only and not committed.

## 7. Accessibility and large text

- Test the phone bottom bar and tablet rail at 1.3× first; that is where labels
  wrap.
- Give character cards and calendar days a full spoken label, not just a glyph
  or number.
- Grade buttons should read the grade; the interval is an optional second line.
- The writing canvas has a content description.
- TalkBack is improved but not fully certified. A full sweep is still required
  before claiming accessibility complete.

## 8. Dependency updates

See `docs/dependency-updates.md`.

- Actions updates are one grouped PR.
- Gradle minor/patch is one grouped PR.
- Gradle/AGP/Kotlin/Compose majors are a single deliberate migration branch.
- Never merge one toolchain major on its own.
- Do not rebuild a submitted versionCode with dependency changes.

## 9. Definition of done for a UI change

- Phone and tablet at normal and 1.3× text.
- Light and dark.
- Empty and populated states.
- Back navigation, keyboard, and session restore.
- No content under the bottom bar or IME.
- `make check`, `make lint`, and `make audit` green.
