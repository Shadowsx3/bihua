# Flow inventory

Status: living document, started 2026-11-23.

Legend: **A** covered by a journey/unit test, **P** partly covered, **G** gap to automate,
**M** manual-only for now.

## First run

| Flow | Steps | Expected | A |
|---|---|---|---|
| Complete onboarding | Welcome → Script → Pace → Brush → Recall → Start | Settings saved, Home opens, writing only | A |
| Skip onboarding | Welcome → Skip setup | Defaults, Home opens, no typing task | A |
| Restore backup in onboarding | Welcome → Restore → pick file | Settings/progress restored, onboarding closes | P |
| Stylus choice | Brush → Stylus + finger / Stylus only | Canvas follows choice; Settings can change it | G |
| Stylus absent | No stylus feature | No choice shown; finger always works | M |

## Daily study

| Flow | Steps | Expected | A |
|---|---|---|---|
| First lesson | Home → Learn 5 new → Meet/Trace/Write | Mixed session, progress | A |
| Mixed due + new | Home → Start | Review block, lesson after interval | A |
| All caught up | Home | No Review/Start debt | A |
| Pronunciation task | Write/reading | Input accepted, grade recorded | A |
| Grade + undo | Check → grade → Undo | Card, XP, day restored | A |
| Leave session | Close → Resume later / End | State preserved/cleared | A |
| Process death | Kill app → reopen | Resumable session offered | A |
| Completion | Last card | Summary, Home | A |
| Vacation | Settings → Pause → return | Due dates shifted, ease-back | A |
| Skipped / suspended | Detail → skip/suspend → tools | Hidden/resumable | A |

## Drawing and feedback

| Flow | Steps | Expected | A |
|---|---|---|---|
| Finger draw | Finger stroke | Accepted by default, visible | M |
| Stylus draw | S Pen stroke | Accepted, pressure width | M |
| Stylus only | Stylus-only onboarding | Finger ignored on canvas, buttons work | G |
| Anki writing | Settings → Practice → tap to reveal | Cue, Show answer, grade, no canvas; new cards skip Trace | A |
| Anki pronunciation | Settings → Practice → tap to reveal | Character, Show answer, grade, no typing | A |
| Meaning review | Settings → Practice → Meaning | Type wording, Show answer, gloss + word, add synonym, grade | A |
| Meaning Anki | Settings → Practice → Meaning + Anki | Show answer, no typing field, grade | A |
| Palm while pen down | Palm + pen | Stylus wins, no stray stroke | M |
| Dot stroke | Tap canvas | Dot recorded, not lost | G |
| Check feedback | Check | Recognition, coach, shape, order, close | P |
| Repair loop | Show me / Trace it / Write alone | Guide modes, clean closure | P |
| Undo/redo/clear | Toolbar | Ink state only | A |
| Guide modes | Off/Faint/Strokes | Canvas size fixed | P |
| Stroke playback | Play | Flowing guide, numbers | P |

## Characters and words

| Flow | Steps | Expected | A |
|---|---|---|---|
| Search character | Characters → query | Flat results, tile detail | A |
| Collapse level | Header | Counts, collapsed grid | P |
| Detail scroll | Tile | Full detail, nav bar spacing | P |
| Add/remove list | Detail → Add to list | Active list updated | P |
| Pin/skip/suspend | Detail/tools | Queue affected | A |
| Note | Detail → note | Saved, survives restart | M |
| Dictionary | Characters → Dictionary | Search, detail, hear, practise | A |
| Paper check | Detail → Write on paper | Reveal, strokes, back | A |
| Compare | Detail → Compare | Add/remove, write both, back | A |
| Word practice | Word → Write | Per-character, queue, bookmark | P |

## Study lists

| Flow | Steps | Expected | A |
|---|---|---|---|
| Create list | Manager → New list | Named, active | A |
| Preset | Manager → preset | List saved with counts | A |
| Import txt/csv/tsv/json | Manager → Import | Preview, duplicates, words | G |
| Import huge file | Large file | Streams, no cap, UI alive | G |
| Export | List → Export | .txt round trip | G |
| Study list | List → Study | Session limited to chars | A |
| Practise words | List → Words | Queue, next, bookmark | G |
| Rename/duplicate/delete | Overflow | Unique names, active cleared | G |
| Add word | Word practice bookmark | Active list words updated | G |
| Meaning synonyms | Meaning reveal or character detail | Add/remove a wording, clean and persist | A |

## Settings

| Flow | Steps | Expected | A |
|---|---|---|---|
| Change brush | Writing | Preview, canvas matches | P |
| Enable finger on stylus | Writing | Canvas accepts finger | G |
| Target recall | Schedule/onboarding | Future intervals only | P |
| SBD fit | Schedule | Local fit, holdout, accepted | P |
| Weights import/export | Schedule | Validation, restore | M |
| Backup | Data → export | Scope respected | P |
| Restore | Data → import | Settings/progress selected | P |
| Reset | Data | Clears cards, keeps notes/lists | M |
| Sync host/join | Sync | Pair, transfer, error | M |
| Theme | Display | Light/dark immediate, every tab, pet fixed | A |
| Pinyin display | Display | Marks/numbers, input both | A |
| Read aloud | Display | Speaker hidden/shown, system voice | P |

## System

| Flow | Steps | Expected | A |
|---|---|---|---|
| Add widget | Launcher | Picker preview, data, refresh | M |
| Reminder | Settings → enable | Permission, schedule | P |
| Offline start | Airplane mode | Content and study work | A |
| Long file/import | File picker | progress, cancel, error | M |
| Dark + fold + tablet | All screens | Layout, contrast, bar | G |
| Settings back stack | Settings → every section → system back | Returns to Settings, never exits | A |
| Overlay back stack | Compare, Paper, study lists, word queue, study tools | One layer at a time, never exits | A |
| Large font scale | All screens | No clipped controls | G |
