# User journey and screen risk audit

Scope: 1.0.4 through 1.0.7 on phone, small phone, and tablet; light and dark
theme; fresh install, returning learner, paused session, backup/import, and
local sync.

Status:

- **Fixed** — code changed in this pass.
- **Verified** — exercised on the emulator or covered by a test.
- **Watch** — known limitation, acceptable for 1.0.4.
- **Open** — still needs work before it can be called complete.

## Startup and content

| Risk | Status | Action |
|---|---|---|
| A corrupt or missing content database leaves the app on an endless “Loading characters…” screen. | Fixed | Load failures now show a clear message and a Retry button. |
| The first-run Home screen can flash before onboarding because settings load after a delay. | Fixed | The app now waits for the first settings read and shows a neutral loading screen. |
| A low-storage asset copy can fail without telling the learner. | Fixed | The same content error state covers copy and open failures. |
| The content copy is keyed by version, not byte length. | Verified | `contentVersion` is now 28 and bumps on every asset change. |
| A content update can wipe progress. | Verified | Content and progress are separate databases. |
| Switching scripts can show the old script while the new list loads. | Watch | The previous list stays visible for one frame; no wrong data is written. |

## Onboarding

| Risk | Status | Action |
|---|---|---|
| System back exits the app in the middle of onboarding. | Fixed | Back now moves to the previous page. |
| Rotating the device resets the selected script, pace, brush, cue, or pronunciation choice. | Fixed | The choices and page number are saveable across recreation. |
| A failed backup restore is silent or unclear. | Fixed | Validation errors are shown in the onboarding message. |
| “Skip setup” hides the chosen-language step. | Watch | It intentionally lands on Simplified with pronunciation on. The mode is one tap away. |
| The pace estimate is generic rather than per course. | Watch | The numbers are conservative and explained as an estimate; the real FSRS simulator exists in Settings. |

## Home

| Risk | Status | Action |
|---|---|---|
| All caught up, vacation, ease-back, empty calendar, and no-new states. | Verified | Distinct copy and actions are present. |
| A skipped or suspended card still shows in the due count. | Verified | The Home due count filters skipped and suspended IDs. |
| A study list with no available cards starts an empty session. | Fixed | The toast now says the list is empty or skipped instead of claiming nothing is due. |
| The tablet right column has “Start with these” and a compact calendar. | Verified | Two-column layout is present. |

## Review session

| Risk | Status | Action |
|---|---|---|
| Handwriting model missing or downloading. | Verified | The review continues with a clear “check unavailable” panel; the learner grades. |
| Recognition returns no candidates. | Verified | A non-blocking “no clear reading” panel appears. |
| Empty ink cannot be checked. | Verified | Check stays disabled and the action bar explains why. |
| Grade undo, resume after process death, and exit to Home. | Verified | Session state is persisted; Home offers Resume or Discard. |
| The character can leak through a word cue. | Verified | Japanese cues mask the target; Chinese and Korean cues show the reading and meaning, not the character. |
| A word cue picks a one-character word and falls back to the meaning. | Fixed | Cue-word selection now requires a real multi-character word. |
| Korean pronunciation needs a Korean keyboard. | Fixed | Input accepts Hangul or romaja, normalises common variants, and previews Hangul while typing. |
| The reading input is covered by the keyboard on phones. | Verified | The field sits in the upper third of the review screen. `adjustResize` shrinks the window and the top-aligned card does not shift, so the field stays above the IME. |
| Japanese pronunciation accepts romaji and kana. | Verified | Both work; a kana preview appears for Latin input. |
| Mature vocabulary can appear in a review cue. | Fixed | Mature words are excluded before the cue is chosen. |
| The last card in a session can be lost by back navigation. | Verified | The pending state is saved continuously; Home offers Resume. |

## Characters

| Risk | Status | Action |
|---|---|---|
| No search results or no filter results. | Verified | Both show a short empty state instead of a blank grid. |
| A character has no word example. | Verified | The card says so without blocking practice. |
| Tapping a word whose characters are unavailable starts an empty practice screen. | Verified | The app refuses and explains that the word is not fully available. |
| Mature words in detail, search, recent practice, or word practice. | Fixed | Content queries and recent practice history filter them until the setting is on. |
| Known-list import of an unreadable file silently does nothing. | Fixed | It now reports “Could not read that file.” |
| Variants, notes, pin, skip, suspend, study list, compare, and paper practice. | Verified | Each has a visible action and an undo or resume path. |

## Progress

| Risk | Status | Action |
|---|---|---|
| Empty history, no reviews, or no forecast. | Verified | The screen explains what will fill in. |
| Leeches, mistakes, confusions, and recently learned lists are empty. | Verified | Each shows a short empty state. |
| Writing pace or grade mix with too little data. | Verified | The UI hides or explains missing values. |
| Reset all progress leaves paused sessions or word practice history. | Fixed | Both are cleared with the reset. |

## Practice

| Risk | Status | Action |
|---|---|---|
| No characters match the picker search. | Verified | A clear message is shown. |
| A character has no stroke guide. | Verified | The screen says to trace the outline. |
| Word practice on a partially available word. | Verified | Guarded at the call site and the content builder now requires full Korean character coverage. |
| Free practice changes the schedule. | Verified | Practice is outside FSRS. |

## Settings

| Risk | Status | Action |
|---|---|---|
| Mature words can be turned on without context. | Fixed | A confirmation dialog explains what is shown and how to hide it again. |
| Mature words are described as normal content. | Fixed | The row says explicit dictionary examples, off by default. |
| A newer or foreign progress database is imported and crashes queries. | Fixed | Import validates schema version, required tables, and card columns before replacing anything. |
| A failed restore replaces good progress. | Verified | A fallback is exported first and restored on failure. |
| FSRS weight paste, local fit, profile per script, and CSV export. | Verified | Invalid input reports a message; defaults stay on a failed fit. |
| Known-list, CSV, backup, and sync exports have no destinations. | Verified | The system file picker is used. |

## Backup, import, and sync

| Risk | Status | Action |
|---|---|---|
| Settings-only, progress-only, empty, and truncated archives. | Verified | Archive and codec tests cover each shape. |
| Progress from a newer app version. | Fixed | Rejected with a readable message. |
| Progress file that is not a Bihua database. | Fixed | Rejected before the local file is touched. |
| Sync package with a bad progress database. | Fixed | The same import validation runs before applying it. |
| Sync pairing code is mistyped. | Verified | The server returns 403 and the client explains the rejection. |
| Local sync without Wi-Fi or with mDNS unavailable. | Verified | Errors are shown; no partial import occurs. |

## Screen design pass

| Finding | Status | Fix |
|---|---|---|
| Tablet Settings rows stretched across the full width, leaving the label and chevron far apart. | Fixed | The settings list is capped at 760 dp and centred; detail pages use the same column. |
| An empty “Upcoming reviews” card still listed fourteen zero rows. | Fixed | The forecast collapses to the header and “Nothing scheduled yet.” until data exists. |
| The first launch showed a bare spinner on a decorative background. | Fixed | The boot screen now says “Preparing your course…” and waits for the first settings read. |
| Onboarding back navigation and rotation could lose the selected page or script. | Fixed | Back goes to the previous page; page, script, pace, brush, cue, and pronunciation are saveable. |
| Tablet Practice puts the page title in the control column, away from the canvas. | Fixed | The title now sits above the canvas and control columns. |
| Grade buttons ask the learner to guess what the algorithm will do. | Fixed | Optional interval labels show the next gap on each button; off by default. |
| Back from review exits silently. | Fixed | Back opens a leave sheet with Resume later, End session, and Keep studying. |
| Known characters have no contextual word moment. | Fixed | Character detail and Home show a real word with audio and a Write action. |
| Confusion pairs can be compared but not practised together. | Fixed | Compare now has a guided Write both drill. |
| Home lost its colourful learning dashboard in the name of fewer cards. | Fixed | Stat cards, calendar, Word of the day, and Study tools return above the fold; level and browse move into More. |
| Characters search has a draw icon inside the field and a filter icon beside it. | Watch | Both have accessibility labels and distinct icons; no functional overlap. |
| Tablet onboarding keeps a phone-sized column. | Fixed | The onboarding column now grows to 720 dp on tablets. |

## Device and accessibility

| Risk | Status | Action |
|---|---|---|
| Small phone: the writing grid is squeezed by controls. | Verified | Compact prompt/action bar and weighted canvas are in place. |
| Tablet: the canvas stretches across the whole screen. | Verified | Wrist-sized cap and two-column layouts are used. |
| Dark theme grid and paper contrast. | Verified | Palette contrast tests pass. |
| Large text and TalkBack labels. | Improved | 1.3× font scale was checked on phone and tablet. The rail widens, compact labels shorten, and character/day cards have spoken descriptions. A full TalkBack sweep remains. |
| Rotation during a session. | Verified | Session state and orientation lock are handled. |
| Process death during a session. | Verified | Session state is serialised and restored. |

## Content and release

| Risk | Status | Action |
|---|---|---|
| Explicit word examples in the default experience. | Fixed | Glosses are cleaned sense by sense; mature-only words are tagged and hidden. |
| Dictionary artifacts such as `X|Y`, `[notes]`, braces, dangling parentheses, or truncated meanings. | Fixed | The importer tidies every gloss; the audit fails on the pattern if it returns. |
| Korean course has no word examples. | Fixed | 11,266 Korean word examples cover 1,684 of 1,799 Hanja. |
| Rare Korean Hanja with no compound. | Watch | 115 rare characters have no safe example; the character card still works. |
| Japanese word ranking relies on JMdict priority rather than a corpus. | Fixed | FrequencyWords Japanese now ranks the candidates, with JMdict priority as the fallback. 学 leads with 学校. |
| 587 structure rows had no story and 286 had no decomposition. | Fixed / Watch | The 587 now have a neutral shape note labelled “Shape”, never a made-up history. KanjiVG component groups filled 257 decompositions, leaving 29 rare variants; those stay hidden rather than guessed. |
| Content version and test counts drift from the docs. | Fixed | 310 unit tests, contentVersion 34, 34,362 words are reflected in the README and AGENTS notes. |

## 1.0.6 regression pass

| Finding | Status | Fix |
|---|---|---|
| Reminders counted kana and Hangul that were hidden from the queue, plus skipped and suspended characters. | Fixed | `ReminderSelection` applies the same filter as the session. |
| A reminder enabled less than 30 seconds before its hour was moved to the next selected day. | Fixed | The scheduler no longer applies a forward guard. |
| Recently learned was empty when identical glyphs shared one card. | Fixed | Resolve cards by character id or glyph group id. |
| Upcoming reviews counted suspended characters and characters skipped for the due day. | Fixed | The forecast filters by the due date, so a one-day skip still shows later work. |
| Shared glyphs learned in one script did not count toward another script's daily new limit. | Fixed | Shared cards count toward every script while sharing is on. |
| A camera-sized profile photo was decoded at full resolution for a 56 dp circle. | Fixed | Bound the decode, apply EXIF rotation, and save at 768 px. |
| A restored brush could be negative or non-finite and restored timers could be negative. | Fixed | `BrushConfig.sanitized` and timestamp clamps in `AppSettings` and `UserProfile`. |
| A corrupt settings blob in an archive could leave a progress-only import dialog after the error. | Fixed | Both halves decode before the dialog state changes. |
| The daily reminder worker opened a content database and never closed it. | Fixed | Close the worker's repository when the message is built. |
| The sync certificate lived for one hour, shorter than a setup session can take. | Fixed | Certificates live for 24 hours; the pin still bounds the session. |
| `device.sh ui` omitted every node whose label is only a content description. | Fixed | Parse each node once and prefer `content-desc`. |
| The mixed-session bar sat at zero because each pass shrank the pending queue. | Fixed | `SessionBlock` measures the stable original block, so the bar fills 0 to 1 between lessons. |
| "Skip setup" still created ten new characters after the five-a-day recommendation. | Fixed | Onboarding uses one shared recommendation constant. |
| The first settings frame could migrate shared glyph cards before the saved setting loaded. | Fixed | Removed the startup migration; old glyph cards now serve as a safe fallback. |
| Turning off shared glyphs dropped history for every script except the first. | Fixed | A legacy shared card stays readable until each script earns its own card. |
| Phone free practice ignored the Outline guide setting. | Fixed | Both layouts pass the guide mode through. |
| Progress export closed the live database while Home could still query it. | Fixed | Export uses a `VACUUM INTO` snapshot and leaves the store open. |
| A bad backup file could be read into memory without a cap. | Fixed | Raw file and zip entry reads are bounded. |
| Restore fallback and temp files stayed in the cache. | Fixed | Delete them after success or a successful rollback. |
| A finished session still asked whether to resume or discard its empty queue. | Fixed | Completion exits Home from the button and system back. |
| The completion screen could claim part of a finished queue was left over. | Fixed | It reports the finished session without using the first-pass count. |
| The sync pairing field opened a digits-only keyboard for a base32 code. | Fixed | Use the ASCII keyboard and keep normalisation. |
| An IPv6 link-local sync address could produce an invalid HTTPS URL. | Fixed | Bracket it and escape the zone id. |
| Reduce motion still animated the session progress and XP. | Fixed | Animations snap when the setting is on. |

## 1.0.7 focus pass

| Risk | Status | Action |
|---|---|---|
| A correct stroke is called reversed, or a wrong guide stroke is blamed. | Fixed | Matching is order- and shape-aware; reversal needs swapped ends, opposing direction, and a backwards path; short/U-shaped strokes are never guessed. Covered by `StrokeReversalRegressionTest` and the `stroke-clean` journey. |
| Out-of-order feedback tells the learner the wrong repair direction. | Fixed | The flagged stroke is always one drawn after a later stroke, so the copy now says "belongs earlier". Tests assert "later" never appears. |
| The order grader compares ink to an invisible guide. | Fixed | `centerlines()` applies the same 6% inset as the visible guide. |
| Ink stacked on one stroke can look like the full character. | Fixed | `StrokeGrade` now measures coverage as well as precision. |
| The repair pad accepts extra or reversed strokes. | Fixed | It requires one matched stroke, no extras, no reversal, and shape coverage. |
| Word practice and drill screens silently miss stroke guides. | Fixed | Their full-screen routes now provide the content, progress, pinyin, and Hangul composition locals. |
| Pronunciation reviews can show the answer in the meaning. | Fixed | `ReadingPrivacy` masks the expected reading in kana/Hangul self-descriptions. |
| Recall could show the target character in a cue. | Verified | Cues are masked by design and the journey harness now fails if a CJK target is visible during "From memory". |
| Printable sheets have a model but no traceable step. | Fixed | Trace and trace-twice templates now include pale glyph cells, and the cover lists the sheet contents. |
| Worksheet grid boxes print as solid coral blocks. | Fixed | The border paint was filling the box; grid borders are stroked now, so the paper stays clean. The model and trace cells get light tints instead. |
| The worksheet export action is buried below the fold. | Fixed | Export PDF is the first action, with a live page and character summary. The options scroll beneath it. |
| Stroke guides read as weak, disconnected lines. | Fixed | Guides now use tapered brush polygons, a paper halo, a grid behind them, and numbered start badges. Checked on 一, 学, 谢, and 爱. |
| The guide can invent a reversal or call a correct stroke out of order. | Fixed | Matching is order- and shape-aware; reversal needs swapped ends, opposing direction, and a backwards path. |
| A dictionary result is a dead end. | Fixed | The detail view shows per-character reading, meaning, and stroke count, plus audio, open-character, and word practice. Example words open directly into writing practice. |
| The dictionary opens its keyboard over the tablet landing. | Fixed | The search field no longer requests focus on open; the full landing is readable until the learner taps it. |
| A worksheet can only use a fixed slice of the course. | Fixed | Course order has a start number, an All option, a picked set, and .txt/.csv import. |
| Worksheet templates are a wall of chips. | Fixed | Seven named templates set a full pattern, with a plain-language description and fine-tune controls below. |
| A single-character worksheet says “1 characters.” | Fixed | Counts are pluralised in the builder, preview, export message, and PDF labels. |
| The landing page can keep showing an old screenshot after a UI change. | Fixed | The practice capture was rebuilt from the final build and every content image URL now carries `?v=1.0.7` so a cached copy is not reused. |
| Kana curves were rendered with the Han offset-brush polygon and looked pinched or blobby. | Fixed | Kana now use a round, even stroked path; Han keeps the filled outline and brush. |
| The character grid was a flat list of 3,100 entries. | Fixed | The grid groups by level with collapsible headers; search returns a flat result list so nothing stays hidden. |
| The stroke coach card could push its repair actions below the feedback fold. | Fixed | The actions sit above the summary, the duplicate status chips were removed, and the card fits the phone-width layout. |
| The dictionary listing crop cut the left and right margins. | Fixed | The dictionary card no longer applies the top-crop transform. The print card keeps its status-bar breathing room. |
| Show answer in word practice revealed every character in the word. | Fixed | Only the current character is revealed; completed characters stay visible while the next is written. |
| Switching to Strokes pushed the canvas to make room for the play button. | Fixed | The play slot is always in the mode row, so the canvas does not move. |
| Undo and clear took a full row in word practice. | Fixed | They use the compact character-practice history pill with redo and the eraser. |
| Filled-guide playback swapped from a brush polygon to the outline at the end of each stroke. | Fixed | Playback clips the final outline along the median, so the shape never changes mid-stroke. |
| A widget layout used a plain `View` and the launcher showed “Can't load widget”. | Fixed | RemoteViews only gets allowed views; the accent is an empty `TextView`. |
| Widget data can go stale or wake the app too often. | Fixed | Update on app resume and a 30-minute provider period; no service, no wake lock. |
| The daily word could show an explicit, rare, or cross-reference entry. | Fixed | Always non-mature, 2–6 characters, quality filters, curated words first, then a hashed common pool. |
| A fast phone could flash the cold-start welcome. | Fixed | Hold the finished welcome for at least 1,050 ms (520 ms reduce-motion); tap still skips. |
| The profile pet looked different from the launcher icon. | Fixed | Both use `InkMascot` with the brush; the profile avatar adds the sparkle. |
| Filled-guide playback parsed the outline path every frame. | Fixed | Parse once per guide and only transform per frame. |

## Local journey harness

`scripts/journey.sh` is the full-app daily-state check. It seeds deterministic
progress databases, applies settings through a debug-only bridge, and drives the
real UI with `uiautomator2` on phone, compact phone, landscape phone, fold inner
and outer, tablet, and 7-inch tablet profiles.

- Quick: phone only, six daily journeys.
- Standard: phone, fold, and tablet core journeys.
- Full: all profiles plus 100-review drain, vacation, ease-back, study tools,
  shared glyphs, fold transition, accessibility, typing pronunciation, and
  guide-stroke drawing recognition.
- Long drains use pronunciation tasks for speed. Writing and recognition stay in
  `review-modes` and `draw-recognition`, where the harness draws the real guide
  strokes from the content database and checks the recognition result.
- `review-modes` checks both review tasks: drawing unlocks Check & grade and
  shows recognition feedback; typing shows Not quite, Correct, and tone-only
  messages; both tasks grade and return Home.
- `draw-recognition` exercises Simplified, Japanese, and Korean guides, records
  same, variant, different, no-clear, or unavailable, and includes a deliberate
  wrong guide to exercise the mismatch path.
- Runs are local only and stored under `.qa/runs/`; `.qa/` is ignored by git.
