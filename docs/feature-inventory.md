# Feature inventory

Status: living document, started 2026-11-23.

Legend: **A** automated, **P** partly automated, **G** automation gap, **M** manual.

## Content and scripts

| Feature | Behaviour | A |
|---|---|---|
| Four scripts | Simplified, Traditional, Japanese, Korean kept apart | A |
| 3,100+ characters | Level order, search, filters | A |
| Word examples | Dictionary, Word of the Day, word practice | P |
| Stroke guides | Filled brush, kana style, outline, numbers | P |
| Pronunciation cues | Meaning, reading, word, isolated | A |
| Share identical glyphs | Shared/separate memory, overrides | P |

## Scheduling

| Feature | Behaviour | A |
|---|---|---|
| FSRS-7 | Default scheduler, 34 weights per profile | A |
| SBD | Seven weights, per-user fit, defaults | P |
| Target recall | 70–97%, default 90%, future intervals only | P |
| Weights fit | Local holdout fit, accept/reject | P |
| Weights import/export | 34-weight JSON, validation | M |
| Day boundary | 0–6 AM, affects counts/calendar | A |
| Vacation / ease-back | Pause and return | A |
| Skips / suspensions | One-day or permanent | A |
| Resume session | Process death, app restart | A |

## Help and tour

| Feature | Behaviour | A |
|---|---|---|
| Guided tour | Optional live overlay with Mò over real screens; spotlight, gestures, props, typewriter line, sound cues | A |
| Tour invite | Dismissible Home card once; “Not now” goes straight to the app | A |
| Tour replay | Settings → Help & tour → Start tour; Skip/Back/Next and final Back to app | A |
| Grading help | Again/Hard/Good/Easy explained in plain words with colour; “?” in the session too | P |
| Progress windows | 7/30/90/365-day ranges for momentum, reviews, learned, practice time, pass rate, streak, best day, growth, and daily rhythm; course-scoped with calm empty states and a real 366-day streak | A |
| Paper and lists help | Exact Settings paths for printable sheets and saved lists | A |
| Reduce motion + TalkBack | No typewriter, bobbing, pulse, or confetti; full line read at once | P |
| Sound effects | Synthesised tick, check, grade, add, tour, and session cues; one Settings switch | P |
| Widget pet costumes | Study cap or word monocle, witch hat in the evening, vampire at night, nightcap on vacation, party hat when the desk is clear; Settings can pin one | P |
| Widget day-part backgrounds | Warm morning, mint afternoon, lilac evening, deep night with light text and soft glows | P |
| Widget stat blocks | Today shows review and lesson counts; Word shows two chips below the definition | P |
| Completion party hat | Mò wears a party hat on the session-complete panel | P |
| Spell quest | Extra practice: draw a character that fits the scene, or take a gift from the grimoire; nothing graded or scheduled | A/P |
| Spellbook data | 104 spells, 25 families, 8 chapters, 8 category tabs | A |
| Spell quest cast surface | Rune circle, clue card above the drawing, pinned Cast, plain learner ink, tip card with reading/meaning/word | A/P |
| Spell quest sound | Generated pad, fifth, heartbeat, plucks; soft cast/wrong/gift/reveal/walk/chapter cues | P |

## Writing

| Feature | Behaviour | A |
|---|---|---|
| Finger + stylus | Default both, stylus-only option | P |
| Palm safety | Stylus priority | M |
| Pressure and taper | Width, speed thinning, taper | P |
| Dot strokes | Single tap is a stroke | G |
| Anki writing | Optional cue → Show answer → grade with no canvas | A |
| Anki pronunciation | Optional character → Show answer → grade with no typing | A |
| Meaning review | Optional character → type wording → gloss, example word, grade | A |
| Meaning synonyms | Add/remove user wordings in the reveal or character detail, cleaned and capped | A |
| Meaning data sanitizer | Skip blank/structural glosses; filter words | A |
| Eraser / undo / redo / clear | Ink-only edits | A |
| Guide modes | Off/Faint/Strokes, fixed layout | P |
| Stroke coach | Exact stroke, Show/Trace/Write, mint closure | P |
| Recognition | ML Kit evidence, never grades | A |
| Paper check | Reveal, strokes, back | A |
| Compare | 2–4, playback, write drill | A |
| Word practice | Per-character, queue, bookmark | P |

## Learning loop

| Feature | Behaviour | A |
|---|---|---|
| Meet / Trace / Write | Three-stage lesson | A |
| Grading | Four or two buttons, intervals | A |
| Undo | Card/XP/day restore | A |
| Load estimate | New goal simulation | P |
| Widgets | Today, Word, time-aware | M |
| Word of the Day | Curated, non-mature, audio | P |
| Streak/calendar | Local days, no debt | A |

## Lists

| Feature | Behaviour | A |
|---|---|---|
| Named lists | Characters and words, active quick-add | P |
| Presets | Starter, First 100, levels, Everyday words | G |
| Import | Streamed TXT/CSV/TSV/JSON, no cap | G |
| Export | .txt round trip | G |
| Study a list | Session from characters | A |
| Practise words | Free queue, bookmark | G |
| Rename/duplicate/delete | Unique names, active handling | G |

## Settings and data

| Feature | Behaviour | A |
|---|---|---|
| Theme | System/light/dark, every tab, pet fixed | P |
| Pinyin display | Marks default, numbers optional | A |
| Read aloud | System TTS, hidden speaker when off | P |
| Handwriting models | Per-script download, Wi-Fi only | M |
| Backup/import | Selective settings/progress | P |
| Sync | Encrypted local Wi-Fi | M |
| Reset | All progress, keeps notes/lists | M |
| About/licences | Version, credits, notices | A |

## System qualities

| Quality | Behaviour | A |
|---|---|---|
| Offline | No account, no cloud | A |
| Dark pet | Fixed paper/ink identity | G |
| Nav bar | Content reaches bar, no dead strip | P |
| Fold/tablet | Rail, book, phone-width | P |
| Large text | No clipped controls | G |
| Errors | No content, no voice, bad import | P |
| Migration | 1.0.7 → 1.0.9 settings and DB | A |

## Large text and loading

- Large-text layouts are checked at 1.5x on phone. The Home Study tools card,
  Compare header, and Paper check header stack their titles and wraps action
  rows.
- Model downloads and recognition have timeouts. Content loading has Retry.
  The first read-aloud tap is queued while the voice starts.
- The deep navigation journey covers every Settings section and the main
  overlays. Import/export file pickers remain partly manual.
