## 1.1.2 (in progress)

- Report a problem, a mistake, or an idea from Settings. Crash reports stay on
  your phone until you send one, and the composer shows exactly what will be
  sent. In landscape the sheet is centred on the screen and keeps its words and
  actions above the keyboard. Rate Bihua from Settings, About.
- The Today widget now shows the day's review and lesson amounts and the next
  hours. A dedicated Week ahead widget shows the next seven days; Word of the
  Day remains its own card.
- Progress gained All time, a GitHub-style year heatmap, and honest bucketing
  for long histories. The range selector now moves Recent mistakes and
  Recently learned too, and tapping or dragging the cumulative line reads any
  day's total.
- The splash now hands off from the system icon without moving it, stays light
  in the dark theme, and no longer shows the ink-reader notice over the welcome.
- Phones in landscape use the tablet two-page Home and the book-style Practice
  layout, so the canvas and the study action stay visible. The spell quest,
  its draw screen, the gate, and the grimoire use their wide spreads too.
- A landscape review keeps Show answer and the grade bar visible, the fold outer
  display gets the two-pane review, and rotating no longer loses the review
  card, report draft, Settings section, or Practice selection.
- The Progress range can be a preset or an exact start and end day. A single
  Monday, last week, and last month all work; the charts, recent mistakes, and
  recently learned follow the chosen window.
- Drawing can be switched off in Settings when pronunciation or meaning is on.
  Lessons skip the trace step, reviews use the enabled checks, and the app keeps
  at least one practice mode on.
- After Again or Hard, a small optional "Add a reason" note can record stroke
  order, direction, a missing stroke, spacing, the reading, the tone, or the
  meaning. The grade is saved first; the note never changes the schedule.
- A Mnemonic palace gives Chinese sounds a reusable actor, place, and action,
  and gives Japanese kanji and Korean Hanja reusable part pictures. Kana and
  Hangul do not show the palace. The method is off by default and opens from a
  small book button in a lesson or review, or from the character page.
- Level progress sits under Momentum, with HSK bands, Jōyō grades, or Hanja
  levels, and a plain "started, not mastered" note.
- Sync can merge two devices instead of replacing one: reviews join, each card
  keeps its newer state, notes keep their newer text, and settings remain an
  explicit choice.
- A fresh device with no network stays usable: setup can be skipped, Home opens,
  and a review starts. The handwriting reader downloads in the background and
  never gates the first action; Check still returns local ink feedback.
- The spell-quest guide is now a chapter-coloured underlay at 65%, and the
  grimoire gift and result strokes no longer carry a dark halo, so the learner's
  own ink stays readable while tracing over them.
- Word practice opens in one-page mode for multi-character words. Write the
  whole word on a wide blank page, optionally show the guide, reveal one next
  stroke, or play the word's stroke order. Check reads the whole ink on the
  device; without the handwriting model, word practice stays in the safe
  one-character-at-a-time mode.
- Progress exports a clean A4 PDF for the selected range. It includes the
  summary numbers, cumulative characters, course coverage, level rings, review
  quality, review types, the recent period table, the characters touched, and a
  study log. Empty and first-session reports get their own guided layout.
- Study tools now includes Character PDF, so the A4/Letter worksheet builder is
  one tap from Home instead of only Settings.
- The guided tour keeps its spotlight on the real control after a script or
  theme change, scrolls a help card into view, and moves beside its target in
  landscape. The invitation appears at every window size, and a paused session
  keeps the spell-quest link.
- Character review history and the words list collapse independently, so a long
  detail page stays readable. The optional grade note is now a small header
  button after a grade, not a full-width row above the card.

# Changelog

## 1.1.1 (in progress)

- The word of the day is now a shared calendar slot: the same study day and
  course return the same word for every learner, with no install-time or
  personal seed. A month of days does not repeat, and each course walks its own
  sequence.
- Spell-quest scenes keep their sense: obstacles and places now share place
  kinds, so a frozen lake no longer appears inside a copper market. Every
  obstacle still has many honest places, and family puzzles still have several
  fitting answers. A solved scene names its own outcome, and one chapter does
  not repeat the same obstacle.
- Mò's note has a visible close button, so it is obvious how to return to the
  hint at any text size.
- A writing review always offers Show answer, even before ink or when
  recognition is slow. A learner is never stuck behind Check & grade.
- The spell quest honors the system remove-animations switch, caps ambient
  motion at 30fps, and caches the immutable puzzle parts, so repeat visits do
  not reopen and reparse the database.
- Widgets refresh at day-part and study-day boundaries, on app changes, and on
  clock/date changes, with a six-hour safety net instead of a half-hour poll.
- Each drawing tool now keeps its own brush preset. Switching Ink, Pen, Pencil,
  and Marker restores that tool's saved width, pressure, speed, and taper; the
  presets travel in backups.
- The Reminders page has one clear title, an explicit On/Off state, a preview
  card, and the test notification action whether reminders are on or off.
- Wide tablets use the navigation rail and the two-page dashboard instead of a
  single stretched phone hero.
- A guided tour with Mò, the paper-ink pet. It runs as a live overlay over the
  real app: a spotlight on the real control, a typewriter line, small pet
  gestures and props, and a gentle sound cue. The Home invite is optional, and
  Settings → Help & tour can replay it.
- Settings now has a Help & tour section with plain-word grading help, the exact
  paths to printable sheets and saved lists, show-answer help, and the replay.
- Optional meaning reviews: type the meaning in your own words, show the clean
  gloss, see a real word that uses the character, and grade. Off by default.
- User synonyms for meanings, with add/remove editors in the review and on
  character detail. Wordings are trimmed, deduplicated, and capped.
- Meaning data is sanitized before it is taught: blank, placeholder, and
  structural glosses such as "variant of" or "surname" are skipped.
- Optional Reveal reviews for writing, pronunciation, or meaning. The cue
  and the character appear first, then Show answer and grade. No canvas, typing,
  or choices; scheduling and XP stay the same. Off by default.
- System back from Paper check or Compare returns to the character detail
  instead of dropping to the grid.
- Reveal new cards go Meet → Reveal and skip Trace; the step tabs match.
- The pronunciation answer card shows the reading instead of leaking a raw
  result object.
- Large-text layouts stay readable: the Home Study tools card, Compare header,
  and Paper check header no longer squeeze titles into vertical strips.
- Study tools now has one clear Study action and separate Draw and Paper actions.
- Study list import handles Windows byte-order marks and nested JSON `name`
  fields without losing the first character or renaming the list.
- Long study list names stay unique instead of collapsing to the same string.
- Word practice starts again with a clean canvas, and the single-word back arrow
  returns to the character.
- Leaving a word-practice queue with the system back gesture no longer exits the app.
- Practice clears the previous character's ink when the target changes.
- Widget counts now match Home for skipped and suspended characters, and widgets
  refresh when the app goes to the background.
- Recognition and handwriting-model downloads time out with a clear message
  instead of spinning forever.
- The first read-aloud tap now queues during voice startup and reports a missing
  voice instead of staying silent.
- Onboarding cannot be finished while a backup restore is still running.
- Stylus copy is consistent, and no-stylus screens stop mentioning a pen.
- Small synthesised sound effects now cover checking, grading, adding to a list
  or synonym set, changing tabs, and the tour. One Settings switch turns them off.
- Mò now dresses for the desk: a study cap in the morning or afternoon, a
  monocle on the Word widget, a witch hat in the evening, a vampire at night, a
  nightcap on vacation, and a party hat when reviews and lessons are clear. The
  session-complete panel uses the party hat too. Settings → Display can pin a
  widget outfit.
- Home-screen widgets now have hour-aware backgrounds, review/lesson stat
  blocks, a bigger pet, and a stable layout at 1.5× launcher text.
- The Word widget keeps the word as the hero and moves the review/lesson chips
  below the definition.
- The all-clear Home card loses the duplicate “All caught up” pill and the
  decorative pet.
- The session grade prompt has a “?” that opens the plain-word legend.
- Progress now has 7-day, 30-day, 90-day, and 1-year windows. Momentum, reviews,
  learned characters, practice time, pass rate, streak, best day, growth, and
  daily rhythm all follow the selected window. Coverage rings are calmer, and
  the memory card no longer frames the rest of the course as debt.
- Progress analytics are scoped to the active course. A Simplified review no
  longer appears in Traditional, Japanese, or Korean totals or pace.
- Empty Progress windows read calmly: New window, no grades yet, no memory data,
  nothing scheduled, and the first session starts the chart. No more 0% passed.
- The streak is the real current run from up to a year of study, so it does not
  shrink when the learner switches to the 7-day window.
- The phone bottom bar now meets the scroll content at every font scale instead
  of leaving a fixed 72 dp gap, and Settings no longer applies the navigation-bar
  inset twice.
- Large-text polish: the synonym action shortens to “Add” so the heading stays
  whole, the grade legend is available in writing sessions, and the outfit
  dialog uses readable labeled columns and scrolls at 2×.

## 1.0.9 — 2026-11-23

- A tap now records a dot stroke instead of losing a single-point stroke.
- While a stylus is down, a palm or finger touch no longer starts a second stroke.
- Onboarding pages scroll on small screens and large text.
- The Compare writing drill has a back arrow to the character detail.
- The no-stylus settings note is calmer: "Always on. This device has no stylus support."
- The Word of the Day speaker is now wired to the spoken word. It was visible
  but silent after the Home cleanup.
- The first read-aloud tap is queued while Android initialises the voice instead
  of being dropped.

## 1.0.8 — 2026-11-23

### Added

- Per-stroke coaching now shows the exact repair stroke on the character and
  offers **Show me**, **Trace it**, and **Write alone**. A successful repair
  returns to the whole character with the fixed stroke marked in mint.
- Characters can be collapsed by level section. Each header carries its count,
  search returns a flat list, and tiles switch from level labels to status
  labels while grouped.
- SBD is available as a second memory model: seven parameters, three state
  values, published defaults, and a local fit from the learner's own reviews.
  FSRS-7 and SBD keep state on every card, so switching does not reset progress.
- Target recall is now a 70–97% setting, recommended at 90%, applied to FSRS-7
  and SBD. The onboarding asks three quick questions and recommends a model.
- An optional, user-supplied RWKV-Instant step model can be imported in Settings
  with ONNX Runtime. Bihua does not bundle or advertise the weights; the package
  is validated, run on-device, and kept in app-private storage.
- Free paper check can reveal the character and then show stroke order, and it
  can return to the character's detail page.
- Word practice and Compare have a back arrow to character details; the Compare
  writing drill uses one fixed canvas size and the Off / Faint / Strokes guide
  choice.
- Onboarding is shorter: one schedule choice, target recall, and writing as the
  default. Pronunciation practice stays off until the learner enables it.
- Dark theme keeps the pet's paper-and-ink identity, and the bottom content no
  longer leaves an empty strip above the navigation bar.
- Study lists are named collections of characters and words. They support
  presets, streaming TXT/CSV/TSV/JSON import, `.txt` export, and a quick-add
  list that never changes the review queue. Word lists run free word practice
  with a queue position and a **Next word** button.

### Improved

- Hiragana and katakana guides use a rounder, lighter pen so long curves stay
  legible. Han, Hanja, and Hanzi keep the filled outline and brush style.
- Word practice reveals one character at a time. Completed characters stay
  visible while the next one is written, the Off/Faint/Strokes row keeps a
  fixed play slot, and undo, redo, and clear use the compact character-practice
  history pill.
- Filled-guide playback now reveals the final outline along the stroke path
  instead of switching from a temporary brush polygon to the outline at the
  end. The motion reads as one continuous stroke.
- Home-screen widgets: a Today card with due reviews and new lessons, and a Word
  of the Day card with the pet, reading, and meaning. Both change with the time
  of day and open the app.
- The daily word is always non-mature, short, and free of variants, surnames,
  place names, archaic/rare/dialect words, and abbreviations. Curated words lead
  each cycle, then a deterministic common-word pool with at least a year of
  candidates per course.
- The profile avatar and the small top icon now use the launcher pet, including
  the brush and sparkle.
- The cold-start welcome holds for a fixed beat so a fast phone cannot flash it.
  Filled-guide paths are parsed once per guide, not on every animation frame.
- Free practice now shows the same full feedback as a review: the handwriting
  check, the exact repair stroke, Show me / Trace it / Write alone, shape and
  order evidence, and the mint closure.
- Pinyin readings use tone marks by default. Settings offers a clear Tone marks /
  Numbers choice, and typing still accepts ni3, nǐ, or plain letters either way.
- Kinder colour semantics: a missing stroke is butter, a recognition difference
  is sky blue with a compare mark, and red is no longer used for a practice
  mismatch or a waiting stroke.

## 1.0.7

### Added

- A full worksheet studio: print-ready A4 or Letter PDF export from course
  order, picked characters, an imported .txt/.csv list, study list, recently
  learned, recent mistakes, or confusion groups. The export button is the first
  action and stays visible while the options scroll.
- Templates for trace and write, two traces, model and copy, memory, trace
  only, alternate, and a one-character poster. Fine-tune grid, boxes per
  character, characters per page, paper size, reading/meaning, stroke count,
  level, stroke numbers, cover, and reverse order.
- Pick characters from a searchable grid, import one character per line or the
  first CSV column, start the course at any character number, or choose all.
- The sheet builder uses filled source and option pills, a clearer start-at
  field, warm pick/import tiles, and a larger character preview. The panel is
  inset from the screen edge so the options no longer feel cramped.
- A full-screen dictionary with debounced live search, recent words, a detail
  page with per-character reading, meaning and stroke count, audio, and a direct
  path into word practice.
- Word practice guide modes: off, faint outline, and real numbered strokes with
  playback, stroke count, reading, and meaning.
- A new round paper-ink mascot, coral launcher icon, matching system splash, and
  a short cold-start brush animation that settles into the app.
- Stroke coach polish: a one-repair-at-a-time card, a focused repair pad, and
  clearer per-stroke evidence.
- Tapered brush guides with a paper halo and numbered start badges. The paper
  grid now sits behind the guide, so it no longer cuts through the strokes.
- A smoother cold-start hand-off: the system splash icon settles into the
  in-app welcome, waits for the first real screen, then fades into Home.

### Changed

- Stroke matching is order- and shape-aware. A correct stroke is no longer
  paired with a neighbouring guide stroke and mislabelled as reversed.
- Reversal feedback requires swapped ends, an opposing direction, and a path
  that actually travels backwards. Short and U-shaped strokes are never guessed.
- Out-of-order feedback now says the stroke belongs earlier, which is the true
  repair direction.
- Ink is compared with the guide the learner sees, including the visible 6%
  inset.
- Shape evidence measures coverage as well as precision, so ink stacked on one
  stroke cannot look like a complete character.
- Pronunciation reviews mask kana and Hangul meanings that spell their own
  reading ("hiragana for a", "syllable ga").
- The journey harness fails if a writing recall renders its target character.
- Full-screen word practice and drill routes now provide content, progress,
  pinyin, and Hangul locals, so their guides load reliably.
- Single-character dictionary entries show compact stroke and level pills instead
  of repeating a one-row character table.

### Fixed

- The repair pad no longer accepts extra or reversed strokes as correct.
- A correct full trace is covered by a regression journey and must report a
  clean stroke order.
- The dictionary search field no longer leaves the keyboard covering results
  after a word is opened.
- The cold-start welcome waits for the real first screen instead of flashing a
  bare loading state.
- Worksheet grid boxes were filled with the grid color; they now print as clean
  paper cells with a stroked border.
- The practice page now centres short character groups and gives the model and
  trace cells distinct, printable tints.
- The worksheet preview uses the character with the most strokes, not always the
  first one.


## 1.0.6

### Changed

- Version 1.0.6, versionCode 16.
- New learners start with five new characters a day, in a mixed queue where
  reviews lead to the next new lesson.
- The session progress bar measures the review block and fills until the next
  new lesson, then starts again.
- Onboarding and Settings say that five a day is the recommended start.

### Fixed

- The mixed-session bar now fills as reviews pass instead of sitting at zero
  because the pending queue shrank.
- "Skip setup" uses the recommended five new characters, not ten.
- Turning off shared glyphs no longer keeps history only for the first script;
  old shared cards remain readable until each script earns its own card.
- The first settings frame can no longer trigger a destructive shared-glyph
  migration before the saved settings load.
- Free practice follows the Outline guide setting on phones as well as tablets.
- Progress export uses a consistent database snapshot without closing the live
  store.
- Archive import caps the file and entry size, and temporary fallback files are
  cleaned after a successful restore.
- Restored mixed-session intervals are clamped to the settings range.
- Reminder scheduling waits for the first real settings read.
- Reminders no longer count kana or Hangul hidden by the script filters, or
  characters that are skipped or suspended.
- A reminder enabled seconds before its hour now fires that day instead of
  moving to the next selected day.
- The reminder worker closes its content database when it finishes.
- Recently learned resolves shared-glyph cards, so the row fills when identical
  glyphs share one memory.
- Progress totals and the due forecast use the same card lookup as the queue.
- The forecast hides suspended characters and characters skipped for the due
  day.
- Shared glyphs count toward every script's daily new limit while sharing is on.
- A camera-sized profile photo is bounded and rotated before it becomes an
  avatar.
- Restored brush values, timers, and profile fields are clamped to valid ranges.
- A corrupt settings blob in an archive no longer leaves a partial import
  dialog behind.
- The local sync certificate lives for 24 hours instead of one hour, so a setup
  screen can stay open without the pin expiring.
- The sync pairing field uses a letter-capable keyboard; the code is base32, not
  only digits.
- IPv6 link-local sync addresses escape the zone id, so the HTTPS URL is valid
  on networks where mDNS resolves an IPv6 address first.
- A lesson step bar now reaches full at the last step instead of stopping at
  two thirds.
- "Back to home" and system back after a finished session return straight Home
  instead of asking to resume or discard an empty queue.
- The completion screen no longer says part of a finished queue was left over
  when the learner passed after an initial miss.
- Progress files with schema version 0 are rejected before SQLite tries an
  invalid upgrade.
- Home's five main controls expose spoken labels to TalkBack.

## 1.0.5

### Added

- Encrypted device-to-device sync on the same Wi-Fi, using a per-session
  certificate pinned by a 10-character pairing code.
- Off-by-default study reminders with a chosen hour and days, plus a test
  notification.
- Foldable book layouts for Home, Learn, Practice, and Characters.

### Changed

- Version 1.0.5, versionCode 15.
- Local sync is TLS only; cleartext traffic is removed.

### Fixed

- The accessibility crash caused by reading `contentDescription` inside
  `clearAndSetSemantics` in the mode switch, character tile, and character peek.

## 1.0.4 (2026-11-22)

### Added

- Korean word examples from the Korean Wiktionary extract, with Hanja
  spellings, Hangul readings, and safer English glosses. 11,266 examples cover
  1,684 of the 1,799 Hanja in the course.
- Korean pronunciation accepts Hangul or romaja and previews the composed Hangul
  while the learner types.
- Optional FSRS interval labels on the grade buttons: Again, Hard, Good, and
  Easy show their next review gap. Off by default.
- A Word of the day card on Home with audio and a one-tap writing drill.
- A leave-session sheet with Resume later, End session, and Keep studying.
- A guided Compare writing drill for confusion pairs: spot the difference, then
  write each character side by side.
- A Mature words setting, off by default, for explicit dictionary examples. It
  is confirmed once and filters review cues, character cards, word search, and
  recent practice.
- Common-word flags and FrequencyWords ranking for Japanese and Korean examples.
- A shipped-content test, import validation, and a user-journey risk audit.

### Changed

- Version 1.0.4, versionCode 14.
- The review cue defaults to meaning + reading, with a compact Hint popup.
- Japanese word cues show ruby and mask the character to write.
- The writing grid gets more room on small phones; the tablet Home and Meet
  layouts are balanced for their screens.
- Settings content is capped to a readable width on tablets.
- Word glosses are tidied: no `X|Y` pairs, bracketed notes, braces, dangling
  parentheses, or truncated dictionary apparatus.
- 587 structure rows without a source story now use an honest “Shape” note built
  from the decomposition and radical.

### Fixed

- Korean word cues align the highlighted syllable when the word uses a sound
  change.
- Japanese word cues mask every occurrence of the target character.
- Known list import reads supplementary Han characters and reports unreadable
  files.
- Empty review forecasts no longer list fourteen zero rows.
- Home restores the vivid stat cards and practice calendar, keeps the new word
  card and Study tools, and moves level, recent characters, and browse into a
  secondary section.
- A corrupt content database shows a retry screen instead of an endless loader.
- Importing a newer or foreign progress database is rejected before it replaces
  local progress.
- Reset all progress clears word practice history as well.
- FSRS handles a corrupt zero-stability card without NaN intervals.
- Onboarding back navigation and rotation no longer lose the current page.

## 1.0.3 (2026-11-22)

### Added

- FSRS-7 weights can be imported, exported, or fitted locally from the
  learner's own reviews. Each script can keep its own profile; shared glyphs
  use the shared profile. A fit needs 400 reviews, uses the recent 2,000 by
  default or all reviews if chosen, and keeps the defaults when a time-based
  holdout gets worse.
- The study day can start at midnight or up to 6 AM. The daily goal, calendar,
  review counts, skipped characters, and XP follow that boundary. The app
  refreshes the boundary after being open overnight or resumed days later.
- "Skip always" suspends a character until it is resumed. The one-day skip and
  the permanent suspension are separate lists in Study tools.
- Backups, imports, and local sync can move settings and progress together or
  separately. The receiving device chooses what to apply.
- Slow actions show a blocking progress dialog or an inline progress bar:
  session start, backup restore, sync apply, model download, reset, weight fit,
  backup export, and review CSV export.
- `tools/seed_fsrs_fit.py` creates a large, realistic review history for
  testing the fit across per-script and shared profiles.

### Changed

- Version 1.0.3, versionCode 13.
- The Schedule settings page is now compact: profile menu, status, fit, import,
  export, and a More menu. The long explanation moved to an info dialog.
- Practice and review share one aligned ink toolbar. Undo, redo, and clear sit
  on the left; play, reverse, previous stroke, stroke count, and next stroke sit
  on the right. Practice now has the same stroke playback controls as review.
- After a review is submitted, the canvas is locked. The drawing stays visible
  for inspection, the editor tools are disabled, playback still works, and the
  grade bar's Draw again is the only way to start another attempt. No button is
  placed over the drawing.
- Settings → Study can show only Again and Good. Hard and Easy are hidden from
  the grade bar and pronunciation check; scheduling is unchanged.
- Removed `enableEdgeToEdge()` and the deprecated status-bar, navigation-bar,
  and cutout-mode calls it pulls into the app. Android 15+ still uses the
  platform's edge-to-edge default, and system-bar padding is applied only when
  the window draws behind the bars. Android 10–14 keep their normal inset
  window, and bar icon colors are only overridden on Android 15+.

## 1.0.2 — 2026-11-21
 — 2026-11-21

### Changed

- The four grade buttons now sit in a fixed bar after every writing check, with
  the answer, reading, and meaning beside them. Checking a drawing no longer
  leaves the grade choices below the fold.
- The lesson flow is named Meet, Trace, Write. The first run explains the three
  steps and says that the learner chooses the grade.
- A gentler start inside each HSK/Jōyō/geup level: simple shapes such as 一, 二,
  and 三 come first, while the level order stays intact.
- All settings sections are visible at once instead of hiding Display, Data,
  Sync, and About in a horizontally scrolling row.
- Home says “No reviews due” when new characters are still available.
- Handwriting checks prefer a target character found in the top three readings,
  so a recognisable 一 is not labelled different because of one stray guess.
- Practice, word practice, and paper check explain that they stay outside the
  review schedule.
- “Leech” is now “tricky” in the interface.
- Word rows have a visible Write action; the character detail no longer uses
  SRS/model jargon.
- Stylus and finger drawing controls fit above the canvas on a phone.
- Practice uses the full height of the screen, with the result in a compact
  dialog instead of a long scroll.
- The Meet step centres the character and pins Next to the bottom, instead of
  leaving most of the screen empty.
- Home, Characters, Progress, and Settings no longer add bottom padding on top
  of the navigation bar, so more content fits on screen.
- Character detail has top actions for Write a word and Write on paper.
- Home names the active course in the greeting line.
- The privacy policy now states that handwriting strokes are processed on the
  device and explains ML Kit model updates and metrics.
- Handwriting results now say plainly that a reading can miss or misread a
  stroke. It is a hint; the learner still chooses the grade.
- The active handwriting model and the device voice engine are prepared in the
  background during onboarding, on Home, and after a script switch.
- Practice now defaults to the first curriculum character and its picker has
  search by character, meaning, or reading.
- Discarding a paused lesson asks for confirmation.
- Reset, backup import, and local sync clear paused lessons so a stale session
  cannot reappear.
- Character detail puts Practice and word/paper actions before the sharing
  controls.
- Onboarding input copy matches whether the device has a stylus.
- Settings now opens on a searchable home list with grouped rows, then a
  focused subpage with a back arrow.
- The new-lesson pace estimate uses a realistic grade mix, a complete-course
  horizon, and reports average and busy-day review load. At 10 new a day it
  now shows roughly 88 average and 113 busy-day reviews, not 30.
- Pronunciation review has a large result card, your-answer/accepted-answer
  comparison, retry and hear-it actions, and hides the grade until the reading
  is checked.
- A small confetti burst marks a correct reading; session completion keeps its
  burst. Both respect reduce motion.
- The session progress bar is taller, animated, and fills through lesson steps
  and graded reviews.
- Settings About discloses Google ML Kit diagnostics and usage analytics and
  offers Send feedback and Source code actions.
- Study now offers reviews first, mixed review/lesson blocks, or lessons first.
  Mixed sessions can use a configurable interval and a top bar that fills to
  the next lesson instead of the whole session.
- The session progress bar is taller and animated through both lesson steps and
  graded reviews.
- The lesson pace estimate now simulates a realistic Good/Hard/Again mix and a
  completed course. At 10 new a day it reports about 88 average and 113 busy
  reviews, and about 11 months to finish.
- Pronunciation review puts the result in a large feedback card with your
  answer, the accepted reading, retry, hear-it, and a small confetti burst.
- Settings is now a searchable home list of grouped rows with focused
  subpages, instead of category chips or a dropdown.
- Long helper copy on Home, Meet, Trace, Write, word practice, paper check, and
  settings was shortened so the canvas and actions keep the space.
- Home shows the course under the greeting; the hero card carries the counts.
- Emulator and mouse input now work, and emulators are no longer treated as
  stylus-only devices.
- The bottom navigation keeps fixed positions and shows every tab label, so
  the icons no longer move when the selected tab changes.
- Tablet screenshots were recaptured from a 2560 × 1600 Pixel Tablet emulator
  for both the 7-inch and 10-inch slots.
- Duplicate variant rows in character details are hidden.
- CI runs unit tests, content audit, and lint only. It no longer assembles or
  uploads an APK; the APK size check is portable across macOS and Linux.
- The Makefile now uses `/bin/bash` for its recipes. `scripts/env.sh` uses bash
  syntax, so `make test`, `make lint`, and `make apk` failed under Ubuntu's
  `/bin/sh` in CI.
- The landing page uses current 1.0.2 light-theme screenshots and a crisp
  ink-and-paper layout. The blurred forest wash, drifting leaves, fireflies,
  and glass cards are gone.
- The landing page has a live ink pad, a four-step recognition diagram, a
  retention graph, an interval ladder, and an FSRS-7 versus FSRS-6 comparison.
  The hero now says “Learn characters by writing them.”
- The landing page, README, method page, and store copy mention stylus pressure.
  A finger or mouse still works.
- The Korean course is described as Hangul and Hanja throughout the site and
  README, matching the app and the store listing.

### Fixed

- The trace and write screens no longer hide the guide and ink tools behind the
  sticky action bar.
- “1 strokes” and similar plural forms are corrected.
- Redrawing a card keeps the same cue instead of changing the question.

## 1.0.1 — 2026-11-21

### Added

- Session resume per script: after process death, Home offers Continue, Resume, or Discard for the current course.
- Word practice history with a Recently practised list and one-tap repeat.
- Full third-party notices and licence texts inside the app.
- Public landing page, methodology page, CI, and release packaging.
- On-device launch/navigation smoke test (`make smoke`).

### Changed

- The optional local AI assistant and LiteRT-LM runtime were removed. The APK is now 38 MB instead of 61 MB.
- The app code is now MIT-licensed; content keeps its upstream terms.
- Korean Hanja now comes from Unihan.
- Korean word examples come from the Korean Wiktionary extract, ranked by
  FrequencyWords. Explicit examples are tagged and hidden by default.
- The public repository and site are now the primary distribution point.

### Fixed

- Word practice records the active script, not a stale session script.
- Character meanings drop dictionary references, classifier lists, and
  Taiwan-pronunciation fragments.
- Japanese KANJIDIC radical-number notes and Korean hexagram notes are cleaned.

## 1.0.0 — 2026-11-21

First signed private beta: four scripts, FSRS-7, stroke feedback, dictionary,
dark mode, transfer practice, and local backup.
