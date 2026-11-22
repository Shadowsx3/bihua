# User journey and screen risk audit

Scope: 1.0.4 on phone, small phone, and tablet; light and dark theme; fresh
install, returning learner, paused session, backup/import, and local sync.

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
| Content version and test counts drift from the docs. | Fixed | 207 unit tests, contentVersion 28, 34,362 words are reflected in the README and AGENTS notes. |
