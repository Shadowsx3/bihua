# UI test coverage map

Status: living document, updated 2026-11-24 for 1.1.0. A = automated
journey/unit test, P = partly automated or manual visual review, G = gap.

| Area | Covered by | Status |
|---|---|---|
| Onboarding and restore | `fresh`, `resume` | A |
| Phone shell, Settings Scaffold, bottom bar | `nav-gap` at 1× and compact 1.5×; `navigation-deep` on phone/fold/tablet | A |
| Home counts, caught-up, learning | `day1`, `daily20`, `caught-up`, `learning` | A |
| Progress windows and analytics | `progress-ranges`, `progress-empty`, `ProgressStoreModeTest`, `StudyStreakTest`, `daily100`, `goal30` | A |
| Mixed sessions and resume | `daily20`, `resume`, `process-death` | A |
| Writing and pronunciation review modes | `review-modes`, `review-modes-long`, `pronounce-kana` | A |
| Anki writing and pronunciation | `anki` | A |
| Meaning review, meaning Anki, synonyms | `meaning`, `meaning-anki`, `synonyms` | A |
| Grade legend | `grade-help` | A |
| Tour, invite, Help & tour | `tour`, `tour-dark`; compact 1.5× manual | A/P |
| Large-text navigation and invite | 1.5× tour, `navigation-deep`, `synonyms`, `settings-outfit` | A/P |
| Character grid, detail, notes, skip/suspend | `navigation-deep`, `synonyms` | A |
| Dictionary, word practice, Word of the Day audio | `dictionary`, `word-guide`, `word-audio` | A |
| Compare, Paper check, worksheets | `paper-compare`, `worksheet` | A |
| Study lists | `study-lists`, `StudyListsTest`, `StudyListImportTest` | A |
| Settings sections and back stack | `navigation-deep`, `appearance` | A |
| Widget outfit setting | `settings-outfit` at 1× and compact 1.5×; 2× scrolling visual pass | A |
| Widgets on the launcher | `WidgetCopyTest` plus manual launcher pass | P |
| Backup, import, sync | `SettingsTransferTest`, `BackupCodecTest`, `BackupArchiveTest`, `ProgressStoreImportTest`; file picker manual | P |
| Reminders | `reminders`, `ReminderTimeTest`; notification visual manual | P |
| Spell quest | `spell-quest`, `spell-quest-ja`, `spell-quest-ko`, `spell-quest-traditional`, `spell-entry`, `spell-entry-ja`, `spell-entry-ko` | A |
| Spell quest flow clarity | `spell-flow` on phone, tablet, and fold | A |
| Spell quest theme and motion | `spell-quest-dark`, `spell-quest-calm`, `--font-scale 1.5` | A/P |
| Spell quest progress | `ProgressStoreSpellQuestTest`; return-to-road step in `spell-quest` | A |
| Accessibility labels | `accessibility` | P |
| TalkBack at 2× | manual only | G |
| Physical stylus pressure and palm | manual only | G |
| Widget resize, dark wallpaper, launcher font | manual only | G |
| Import/export system file picker | manual only | G |
| Visual motion, sound, haptics | manual only | G |

## Gaps to close

1. Enable TalkBack on a device at 2× and walk Settings, sessions, tour, Compare, Paper, and dialogs.
2. Run the physical stylus pass for pressure, taper, palm rejection, and stylus-only.
3. Exercise the system file picker for TXT/CSV/JSON import and `.txt` export.
4. Resize both widgets and check a dark wallpaper and 1.5× launcher font.
5. Add screenshot/golden checks for the tour card and Mò poses if the project is ready for image diffing.
