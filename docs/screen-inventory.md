# Screen inventory

Status: living document, started 2026-11-23 for the 1.0.9 automation pass.

Legend: **A** automated, **P** partially automated, **M** manual pass required, **–** not applicable.

Every screen should be checked on phone, fold/phone-width, tablet, light, and dark where the
layout can differ. Landscape and large font scale are separate passes.

## App shell

| Screen / surface | Entry | Main states | A |
|---|---|---|---|
| Cold start / opening ink | Launch | fast device, slow content, tap to skip, reduced motion | P |
| Global header | All tabs | four scripts, long name, avatar, tablet rail | A |
| Bottom navigation | Phone/fold | five tabs, active state, nav-bar inset, 3-button nav, content viewport meets the bar at 1x/1.5x | A |
| Navigation rail | Tablet | five tabs, window resize | A |
| Busy dialog | Starting a session | loading, failure | P |

## Onboarding

| Screen | Entry | Main states | A |
|---|---|---|---|
| Welcome | First launch | normal, skip setup, restore backup | A |
| Script | Continue | four scripts, preview, share progress | A |
| Pace | Continue | 5/10/15/20 new, load estimate | A |
| Brush | Continue | four tools, stylus + finger / stylus only, scroll, small screen | P |
| Recall | Continue | cue choices, pronunciation off/on | A |
| Finish | Start writing | defaults, restored settings | A |

## Home and daily loop

| Screen | Entry | Main states | A |
|---|---|---|---|
| Home | Tab | due/new, no reviews, lesson in progress, vacation, ease-back, all done | A |
| Today card | Home | start, resume, counts | A |
| Word of the Day | Home | word, reading, meaning, speaker, Write | P |
| Stat cards | Home | zero, loaded, long numbers | A |
| Practice calendar | Home | empty, partial month, 35-day range | A |
| Study list card | Home | empty, active list, words, Study/Draw/Paper | A |
| Study tools card | Home | counts, Manage | A |
| Widget picker preview | Launcher | Today/Word, small/large, dark, pet costume, day-part background, stat blocks | M |
| Tour invite | Home | first run, dismissed, replay available | A |

## Guided tour

| Screen / surface | Entry | Main states | A |
|---|---|---|---|
| Tour overlay | Home invite, Settings → Help & tour | 10 stops, spotlight hole, Mò gesture and prop, typewriter, Back/Next/Skip, final confetti | A |
| Tour stops | Overlay | Home desk, new character, why things return, spell road, search, practice, grades, no pen, paper/lists, About | A |
| Tour accessibility | Overlay | 1.5x, dark, tablet, reduce motion, TalkBack full-line | P |
| Grade legend | Session grade prompt | “?” opens the four plain-word grades | A |
| Widget outfit | Settings → Display | Auto or a pinned study/monocle/witch/vampire/nightcap/party costume | P |
| Help & tour section | Settings | grading legend, sheets/lists, guided tour, show-answer help | A |

## Characters

| Screen | Entry | Main states | A |
|---|---|---|---|
| Character grid | Tab | grouped, flat search, filters, empty filter | A |
| Level headers | Grid | expanded, collapsed, counts | P |
| Character tile | Grid | new, learning, review, known, leech, selected | A |
| Search | Grid | empty, no result, reading, meaning, pinyin | A |
| Filters | Grid | status, level, frequency, writing only | P |
| Character detail | Tile | new/known, note, family, memory level, synonyms, list/pin/skip/suspend | A |
| Draw search | Grid | valid/empty drawing | M |
| Dictionary entry | Grid/detail | search, results, detail, practise | A |
| Compare picker | Detail/compare | add, remove, max four, no result | P |
| Compare | Detail/family | 2–4 characters, play, numbers, write both, back | A |
| Paper check | Detail | reveal, strokes, next, back, done | A |
| Word practice | Detail/word | per-character, queue, bookmark, back | P |
| Reset progress dialog | Detail | confirm, cancel | M |

## Learn sessions

| Screen | Entry | Main states | A |
|---|---|---|---|
| Meet | Session | new character, guide, story, word | A |
| Trace | Session | full guide, slow trace, hide guide | A |
| Write | Session | blank canvas, recognition, check, grade | A |
| Pronunciation | Session | type reading, hint, correct/wrong | A |
| Meaning | Session | type a wording, reveal gloss, example word, synonyms, grade | A |
| Reveal meaning | Session | reveal-only, no typing field | A |
| Grade buttons | Write | four/two-button scale, interval labels | A |
| Stroke coach | Check | waiting, missing, reversal, repair modes, clean | P |
| Leave session | Close | resume later, end, keep studying | A |
| Resume session | Home | after process death, after app restart | A |
| Completion | Last card | summary, continue | A |
| Drill | Study tools | guided writing batch | P |

## Practice

| Screen | Entry | Main states | A |
|---|---|---|---|
| Free practice | Tab | search, character picker, blank canvas, feedback | A |
| Practice feedback | Check | recognition, coach, shape, order, close | P |
| Word practice queue | Study list/word | 1..n, next, done, bookmark | P |
| Paper check | Detail/list | reveal, strokes, back | P |
| Compare write drill | Compare | fixed canvas, Off/Faint/Strokes, undo/redo/clear, back | P |

## Extra practice

| Screen | Entry | Main states | A |
|---|---|---|---|
| Spell quest gate | Home Study tools / Study tools sheet | rules, met count and chapter, start/walk on, grimoire, leave; phone portrait and landscape/BOOK two-page | A |
| Spell quest scene | Start the road | problem, draw, gift, hint, result, walk on; story and circle share a spread in landscape | A |
| Cast overlay | Draw | rune circle, clue card above, shape/reading/meaning reveals, quest-only brush tray, undo/clear, cast, grimoire | A |
| Grimoire | Scene or gate | cover, index, 10 chapter spreads, script-specific seals, met stars, fold hinge, short landscape page, light/dark | A |

## Progress

| Screen | Entry | Main states | A |
|---|---|---|---|
| Dashboard | Tab | new user, active, caught up, long history; 7D/30D/90D/1Y | A |
| Range summary | Dashboard | empty window, no grades, active streak, best day | A |
| Forecast | Dashboard | empty, loaded | P |
| History / streak | Dashboard | empty, partial, full; actual streak across windows | A |
| Mistakes / confusions | Dashboard | empty, loaded | P |
| Memory buckets | Dashboard | empty, loaded | P |
| Writing pace | Dashboard | first week, loaded | M |

## Settings

| Screen | Entry | Main states | A |
|---|---|---|---|
| Settings index | Tab | search, no match, all groups, back from every section | A |
| Profile | Settings | name, photo, restore/remove | M |
| Practice | Settings | task mix, pronunciation, meaning, Reveal writing/pronunciation/meaning, synonyms | A |
| Study | Settings | list entry, day boundary, vacation, cues, words | A |
| Reminders | Settings | off, permission needed, scheduled | P |
| Schedule | Settings | FSRS/SBD, target recall, weights fit/import/export | P |
| Writing | Settings | brush, grid, size, models, finger drawing | P |
| Display | Settings | theme, orientation, pinyin, voice, sound | P |
| Data | Settings | backup/import scope, CSV, reset | P |
| Sync | Settings | host, join, error, success | M |
| About | Settings | version, report a problem or idea, rate, licences, privacy, source | A |
| Report | About | kind chips, words, saved crash, privacy note, Send/Copy/Cancel, compact landscape with keyboard | A |
| What will be sent | Report → See what will be sent | app and device header, words, optional crash trace, Close | A |
| Licences | About | list, full notices, links | P |

## Study lists

| Screen | Entry | Main states | A |
|---|---|---|---|
| List manager | Settings/Home | empty, active, many lists, long names | P |
| Presets | Manager | Starter, First 100, level, Everyday words | A |
| Import | Manager | txt/csv/tsv/json, empty, invalid, duplicates, BOM, nested names, huge | P |
| Import preview | Manager | characters, words, rename, cancel | P |
| Export | Manager | txt, cancel, unreadable name | M |
| List card | Manager | study, words, rename, duplicate, export, delete | A |

## System surfaces

| Screen | Entry | Main states | A |
|---|---|---|---|
| File picker | Import/backup/export | cancel, unreadable, huge file | M |
| Keyboard | Search/pronunciation | show/hide, rotate | P |
| System TTS | Speaker | installed/missing voice, first tap | M |
| Notifications | Reminder | permission denied/granted | P |
| Android backup / sync | Data | corrupt, newer schema, merge | P |
| Widgets | Launcher | Today, Word, small/large, refresh, dark | M |

## Large-text pass

Checked at 1.5x on a 1080 x 2400 phone: Home, Characters, Progress, Practice,
Settings, Compare, Paper check, Word practice, Learn, and onboarding. The Home
Study tools card, Compare header, Paper check header, synonym header, and widget
outfit dialog use stacked titles and wrapping action rows so they stay readable.
The phone content viewport meets the floating bottom bar at 1x and 1.5x; the
Settings Scaffold no longer double-applies the navigation-bar inset. A 2.0x
TalkBack pass is still open, including the scrolling outfit dialog.
