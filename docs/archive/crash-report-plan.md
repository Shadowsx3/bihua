# Report row and rate row in About

Two additions to Settings → About.

1. **Report a problem or idea.** One composer for "something broke", "an idea",
   or "a mistake in a word or character". It can attach the last crash report
   when one exists. The learner writes the words, sees exactly what will be
   sent, and chooses where it goes.
2. **Rate Bihua.** A plain Play link.
3. **One copy line.** The app and the landing page say what is included, without
   the word "free". The Play listing is left alone.

No SDK, no service, no account, no new permission. Nothing leaves the device
until the learner taps Send or Copy.

This replaces the current `Send feedback` mailto button. One path instead of
two, and the body is richer than the prefilled template.

## 1. Frame

- **Who benefits:** a learner whose app crashed, a learner who found a wrong
  gloss or reading, and a learner who wants a feature. All three currently have
  only a mailto link with a form-shaped string.
- **What changes:** a `diagnostics` package, a `BihuaApplication` class, a
  composer dialog, two rows in About, and a debug crash hook for the harness.
- **What does not change:** PRIVACY.md's "no analytics or crash-reporting SDK"
  promise. Local files, a share sheet, and the learner's own mail or messenger.
  No upload, no background job, no service, no permission change (INTERNET
  already exists for model downloads and local sync).
- **Invariants touched:** answer privacy (nothing about the card being studied is
  captured automatically), backup compatibility (crash files stay out of
  exports), main-thread IO (file reads happen off the main thread), large text
  and dark theme in the dialog.

**Non-goals:** automatic sending, `FileProvider` attachments, Logcat capture,
breadcrumbs, session replay, per-event analytics, Crashlytics, Sentry, any BaaS,
In-App Review SDK, rating gates, stored "rated" flags, donations, tip jars, and
any payment or support link.

## 2. Shape

| File | Purpose | Est. lines |
|---|---|---|
| `diagnostics/CrashReport.kt` | Pure header and trace formatter. No Android imports. | 60 |
| `diagnostics/CrashLog.kt` | Crash files: write, list, newest, read, delete, prune. | 80 |
| `diagnostics/CrashHandler.kt` | Installs the uncaught handler, holds the settings context. | 40 |
| `diagnostics/ReportText.kt` | Pure composer body: kind, metadata, words, optional trace, cap. | 90 |
| `BihuaApplication.kt` | Installs the handler at process start. | 20 |
| `settings/ReportDialog.kt` | Composer dialog, chips, preview, send/copy. | 200 |
| `settings/RateLinks.kt` | Pure Play listing URL. | 15 |
| `settings/SettingsScreen.kt` | Two rows in the About card, state wiring. | 60 |
| tests + harness | Format, composer, pruning, IO failure, rate link, journey. | 170 |

Core logic is around 270 lines. UI, tests, and docs account for the rest.

## 3. Crash file

Still the backbone of the crash half. Unchanged by the composer.

### Report format (exact)

```
Bihua 1.1.1 (code 21)
Build: release
Course: JAPANESE
Day start: 4:00
Device: TECNO SPARK 10 Pro
Android: 13 (SDK 33)
Locale: es_ES
When: 2026-11-23T19:02:11-03:00
Thread: main

java.lang.RuntimeException: ...
	at com.bassmd.bihua...
```

- Fixed order, one `key: value` per line. No JSON. A human reads this in an
  email.
- `Build:` is `release` or `debug` from `BuildConfig.DEBUG`.
- `Course:` is `settings.studyMode.name`. `Day start:` is `settings.dayStartHour`
  as `H:00`. Both print `unknown` before settings load.
- `Device:` is `Build.MANUFACTURER` plus `Build.MODEL`, whitespace collapsed. No
  serial, Android ID, advertising ID, install ID, or IP.
- `When:` is ISO offset date time. `Thread:` is the crashing thread.
- Body is `throwable.stackTraceToString()`, hard-capped at 32 KB, cut at a line
  boundary with `\n[trace truncated]\n`.

### Storage

- `context.filesDir/crash/`, file `crash-<epochMillis>.txt`.
- Keep the newest 3; prune older after a successful write. A crash loop must not
  fill the disk.
- Never in the backup zip, sync package, or settings JSON. Those are
  `backup.json` plus `progress.db` only, so this is automatic, but keep the test.
- Write synchronously with `FileOutputStream` and `use {}`, wrapped in
  `try/catch (Throwable)`. No coroutines, no DataStore, no logger.

### Handler

```kotlin
val previous = Thread.getDefaultUncaughtExceptionHandler()
Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
    runCatching { CrashLog.write(context, CrashReport.format(...)) }
    previous?.uncaughtException(thread, throwable)
}
```

- Always delegate. Play vitals, the system crash dialog, and the process kill
  behave exactly as before. Never swallow, never rethrow, never kill manually.
- Install once from `BihuaApplication.onCreate`, not from `MainActivity`:
  widget broadcasts and workers can crash before the activity exists.
- Metadata comes from a `@Volatile` holder fed by the existing settings flow in
  `MainActivity`. Do not read DataStore inside the handler.

```kotlin
LaunchedEffect(settings.studyMode, settings.dayStartHour) {
    CrashContext.update(settings.studyMode.name, settings.dayStartHour)
}
```

## 4. Report composer (pure part)

`ReportText.build(kind, words, crash, meta)` returns the exact string that will
be sent. `kind` is `PROBLEM`, `IDEA`, or `CONTENT`.

```
Bihua report
Kind: Something broke

Bihua 1.1.1 (code 21)
Build: release
Course: SIMPLIFIED
Day start: 4:00
Device: TECNO SPARK 10 Pro
Android: 13 (SDK 33)
Locale: es_ES
When: 2026-11-23T19:02:11-03:00

Your words:
The app closed when I tapped Check.

Crash report:
Bihua 1.1.1 (code 21)
...
```

Rules:

- `Kind:` first, so triage is one glance. `Your words:` is always present; print
  `(none)` when blank. The `Crash report:` section prints `(not attached)` when
  absent, so the maintainer never wonders.
- Reuse the crash header verbatim. One formatter, two callers. Do not build a
  second metadata format.
- Total cap 48 KB. When the trace pushes past it, trim the trace, not the
  learner's words.
- Trim the learner's words at 8,000 characters and say so with
  `[trimmed]`. Do not silently drop the tail.
- No character, word, reading, note, or list is included unless the learner
  typed it. Do not read the current card.

## 5. About rows

Inside the existing About card (`settingsSection == "About"`, the `SoftCard` at
`SettingsScreen.kt:3028`), above the `FlowRow` of text buttons. Order:

1. Report row (always visible)
2. Rate row (always visible)
3. Existing buttons, minus `Send feedback`

Both rows use the same shape: full-width `SoftCard`, 40dp rounded icon square,
`titleSmall` plus `bodySmall`, whole row clickable, no nested buttons. Keep the
existing identity, credits, and links untouched apart from the one copy change
below.

### The "included" line

The About paragraph currently opens with `No account and no ads.` That is the
only place the app talks about price, and "free" never appears there. Change
that opening to:

> No account, no ads, no subscription. Everything is included.

Keep the rest of the paragraph (the ML Kit sentence, the scheduler credits) as
it is. Do not add a separate row, badge, or price line. Do not write "free",
"free forever", "lifetime", "premium", or "upgrade" anywhere in the app.

No donations, no tip jar, no support link, no payment of any kind, now or
later. That is a product decision, not an accident, so the copy can be plain
about it: everything is included and there is nothing to buy.

### Report row

- Title: `Report a problem or idea`
- Subtitle, no crash: `Tell me what broke, or what would help.`
- Subtitle, crash saved: `A crash was saved Nov 23. You can include it if you like.`
- Icon: `Icons.Rounded.EditNote` in Lilac at 0.5 alpha.
- Chevron: no. It opens a dialog.

Read the crash state in a `LaunchedEffect` with
`withContext(Dispatchers.IO) { CrashLog.newest(context) }`. Never read the file
during composition, and never keep a crash file in Compose state beyond the
entry you need.

### Rate row

- Title: `Rate Bihua`
- Subtitle: `A review helps another learner find their way here.`
- Icon: `Icons.Rounded.StarBorder`, already in `material-icons-extended`, in
  Butter at 0.9 alpha.
- Chevron: yes. It leaves the app.
- Tap: one https URL. No `market://`, no fallback chain.

```kotlin
val url = RateLinks.playListing(BuildConfig.APPLICATION_ID)
val intent = Intent(Intent.ACTION_VIEW, url)
runCatching { context.startActivity(intent) }
    .onFailure { snackbar.showSnackbar("The store did not open. Search for Bihua in Play.") }
```

Android resolves `play.google.com` to the Play Store app through App Links, so a
normal phone opens the store app directly. A device without the store opens a
browser on the same listing. That is why no fallback is needed: the https URL is
already the one address that works on both. The single `runCatching` is an error
guard so a phone with neither a store nor a browser shows a sentence instead of
crashing; it is not a second destination.

Rules:

- **No In-App Review API in this change.** It means a new dependency group, it
  silently no-ops on the sideloaded Pages build, and it has a quota. If it is
  added later, it must still open the same https listing when the native card
  does not appear, so the row never dead-ends.
- **No rating gate.** Do not ask "are you enjoying Bihua?" and only show the
  store to a happy answer. That violates Play's rating policy and is the same
  dark pattern in miniature. One row, one destination, for everyone.
- **No state.** No "rated" flag, no tap count, no satisfaction score. Nothing is
  added to `AppSettings`, so backups, sync, and restore are untouched.
- **No nudge.** No dialog, no badge, no reminder later, no placement in a session
  or on Home.
- Copy stays plain: no stars in the subtitle, no "support the developer", no
  "help us improve", no emoji.

## 6. Report dialog

New file `settings/ReportDialog.kt`. Scrollable body, sticky actions, insets
handled so the keyboard never covers `Send`.

Contents, top to bottom:

1. Title: `Send a report`
2. Chips: `Something broke` / `An idea` / `Content looks wrong`. Default
   `Something broke`. `FlowRow` so they wrap at 1.5x.
3. Field label `Your words`, placeholder
   `What happened, or what would help.` Multiline, 3 to 6 lines, `rememberSaveable`
   draft so rotation and process death keep it.
4. If a crash file exists: a checkbox
   `Include the last crash report (Nov 23)`, checked by default, plus a
   `See what is sent` text button. That button opens a read-only preview of the
   composed body, scrolled to the top. When no crash file exists, no checkbox and
   no preview button.
5. Privacy line:
   `This opens your mail or messenger. Nothing is sent until you tap Send.`

Actions:

- `Send` (primary, disabled until there are words or an attached crash):
  `Intent.ACTION_SEND`, `type = "text/plain"`,
  `EXTRA_SUBJECT = "Bihua report: <kind>"`, `EXTRA_TEXT` is the composed body,
  wrapped in `Intent.createChooser(..., "Send report")`. On success, show
  `Thank you. I read every one.` If nothing handles the intent, copy to the
  clipboard and show `Copied. Paste it wherever you like.`
- `Copy` (text button): copies the same body, shows the same copied message.
  This is the escape hatch for a phone with no mail app and for pasting into a
  chat.
- `Cancel` (dismiss): keeps the draft.

After a successful `Send` or `Copy` with the crash attached, delete that crash
file and refresh the row state. The report is now in the learner's own app, so
there is no reason to keep a second copy on disk. If the send fails, keep it.

### Voice

Friendly here means one person writing to another, not enthusiasm. It is the
same register as the rest of Settings, where an error says `Could not save the
backup. Free up space and try again.`

- Speak as the maintainer: `Tell me what broke`, `I read every one`. One
  person runs this app and the copy can say so.
- Address the learner as `you`, never `the user`, `users`, or `we`.
- No exclamation marks, no emoji, no all caps, no cheerleading, no "Oops!".
- Offer, do not press: `You can include it if you like`, not `Please help us
  improve`.
- Say what happened and what to try. Never blame, never promise a fix.
- One idea per line. If a sentence needs a comma to survive, it is two
  sentences.

### Why a chooser, not a fixed address

The learner picks their mail app, a messenger, or notes. Nothing hardcodes an
address in a shipped string, the report is complete in the body, and the flow
works on the sideloaded APK where a Play-only path would dead-end. The existing
`manuelbuslon22@gmail.com` mailto goes away with the old button.

## 7. Debug crash hook

In `MainActivity.onCreate`, next to the `QaFlags` block:

```kotlin
if (BuildConfig.DEBUG && intent?.getBooleanExtra("qa_crash", false) == true) {
    throw IllegalStateException("QA crash on request")
}
```

Manual check:

```bash
adb shell am start -n com.bassmd.bihua/.MainActivity --ez qa_crash true
adb shell am start -n com.bassmd.bihua/.MainActivity
scripts/device.sh tap "Settings"   # then About
```

## 8. Tests

Unit tests, new files unless noted:

1. `CrashReportTest.formatsHeaderInOrder` — fixed clock and device fields.
2. `CrashReportTest.capsLongTrace` — 200 KB trace ends under 32 KB, contains
   `[trace truncated]`, cut at a line break.
3. `CrashReportTest.unknownContextIsHonest` — unknown settings print `unknown`.
4. `CrashReportTest.neverPrintsIdentifiers` — no `ANDROID_ID`, no serial, no
   advertising id, no absolute home path.
5. `CrashLogTest.keepsNewestThree` — write four, oldest is gone.
6. `CrashLogTest.writeFailureDoesNotThrow` — read-only directory, no escape.
7. `CrashLogTest.deleteRemovesAll`.
8. `ReportTextTest.buildsKindAndMetadata` — `Kind:` first, header intact.
9. `ReportTextTest.emptyWordsAndNoCrashIsNotSendable` — `build` still renders,
   the dialog disables Send; assert the flag the dialog reads.
10. `ReportTextTest.includesTraceOnlyWhenAttached` — attached adds the section;
    absent prints the two `(not attached)` and `(none)` lines.
11. `ReportTextTest.trimsTraceBeforeWords` — with a 60 KB trace and long words,
    the words survive and the trace is cut.
12. `ReportTextTest.capsWordsAt8000` — `[trimmed]` marker present.
13. `composeApp/src/test/java/com/bassmd/bihua/settings/RateLinkTest.kt` — the URI is
    exactly
    `https://play.google.com/store/apps/details?id=com.bassmd.bihua`, built from
    `BuildConfig.APPLICATION_ID`, with no `market://` variant anywhere.
14. `CrashLogTest.deleteAfterSendRemovesOnlyThatFile` — keep any newer file that
    arrived while the dialog was open.
15. `BackupArchiveTest` extension — a crash file in `filesDir` is not in the zip
    or the exported settings JSON.

Journey: extend `settings-pages` or add `report-flow`:

1. Fresh install, About, screenshot both rows at 1x and 1.5x.
2. Tap the report row. Assert the chip default, the disabled `Send`, and no
   crash checkbox.
3. Type words, switch to `An idea`, tap `Copy`, assert the snackbar.
4. Cancel, relaunch with `qa_crash`, return to About, assert the subtitle names
   the crash and the checkbox is checked.
5. Tap `Send`, assert the app left the foreground (`dumpsys activity` focused
   package is no longer `com.bassmd.bihua`).
6. Tap the rate row once and assert the same foreground handoff.

The chooser is not automated past the handoff. Body correctness is covered by
`ReportTextTest`.

## 9. Docs and privacy

- `PRIVACY.md`: add one sentence. A crash can be saved on the device; a report is
  only sent when the learner taps Send or Copy, through their own app. Current
  line 42 stays true.
- `docs/index.html` and `docs/privacy.html`: mirror the crash sentence. Served
  pages, not generated.
- `docs/index.html`, hero: add one sentence to the `lead` at line 80:
  `Everything is included: no account, no ads, no subscription.` Leave the
  `hero-meta` chips as they are. Change the old note at line 441 from
  `Bihua is still free, with no account and no ads.` to
  `Bihua still includes everything, with no account, no ads, and no subscription.`
  so the page and the app agree and the word "free" leaves both.
- `store/listing.md`, `store/play-assets/listing.md`, and
  `docs/play-store-submission.md` are **not** touched for this change. Play
  already shows a Free price label, the listing copy stays focused on the study
  method, and the Data Safety answers do not move.
- `docs/screen-inventory.md`: About gains two rows and a composer dialog; the
  mailto `Send feedback` button is gone; the description opens with the
  "included" line.
- `docs/flow-inventory.md`: add "report a problem or idea" and "rate the app".
- `docs/test-matrix.md`: new unit tests and the journey steps.
- `CHANGELOG.md`:
  `Report a problem, a mistake, or an idea from Settings. Crash reports stay on your phone until you send one.`
  and `Rate Bihua from Settings, About.`

## 10. Gates

```bash
make check
make lint
scripts/journey.sh --scenario settings-pages --profile phone,tablet --font-scale 1.5 --screenshots
scripts/journey.sh --scenario navigation-deep --profile phone --screenshots
scripts/journey.sh --scenario tour --profile phone --screenshots
```

Screenshot review: About at 1x, 1.5x, 2x, dark, with and without a crash file.
Dialog at 1x and 1.5x with the keyboard open, empty and filled, chips wrapped.
Check that `Send` stays visible above the keyboard, the trace preview scrolls
instead of stretching the dialog, and long device names wrap instead of clipping.

Rerun `tour`: the About stop uses the `about.free` anchor on this card, so a
taller card must not push the spotlight hole off screen.

## 11. Definition of done

- [ ] Crash formatter is pure, fixed-clock tested, and shared by both callers.
- [ ] Handler chains to the previous handler; Play vitals still reports.
- [ ] No main-thread IO outside the crash write itself.
- [ ] Crash files never enter backups, sync packages, or export JSON.
- [ ] Composer sends only: kind, version, build type, course, day start, device,
      Android, locale, time, the learner's words, and the trace when attached.
- [ ] No current character, word, reading, note, or list is captured anywhere.
- [ ] Send is disabled with no words and no crash; Copy always works.
- [ ] Trace is trimmed before the learner's words at the 48 KB cap.
- [ ] An attached crash file is deleted after a successful send or copy.
- [ ] Rows read clearly at 2x text; Send stays above the keyboard.
- [ ] Rate row opens the https Play listing directly, shows a readable message
      if nothing handles it, and stores nothing.
- [ ] `Send feedback` mailto button removed; no duplicate report path remains.
- [ ] Every new string follows the voice rules: first person, "you", no
      exclamation marks, no emoji, no blame.
- [ ] About opens with `No account, no ads, no subscription. Everything is
      included.` and no shipped string says "free", "lifetime", "premium", or
      "upgrade".
- [ ] No donation, tip jar, payment, or support link anywhere in the app or on
      the landing page.
- [ ] Landing hero carries the same promise; the 1.0.9 note no longer says
      "free".
- [ ] `store/listing.md`, `store/play-assets/listing.md`, and
      `docs/play-store-submission.md` are unchanged.
- [ ] PRIVACY.md, `privacy.html`, changelog, and inventories updated.
- [ ] `make check` and `make lint` green; journeys green on phone and tablet.
- [ ] `git status` clean.
