# Production access plan — Bihua 1.0.2

This file separates Google's published rules from community advice. Use the
official rules, then use the practical steps to make the application credible.

## Official requirements

From Play Console Help, "App testing requirements for new personal developer
accounts":

- A **closed test**, not internal or open testing.
- At least **12 testers opted in** when you apply.
- Those testers must have been opted in **continuously for the preceding 14 days**.
- Inviting someone, adding an email, or adding them to a Google Group does not
  count until they open the opt-in link and accept.
- A tester who opts out and back in needs a new 14 consecutive days.
- You must summarize tester feedback and the changes you made.
- Google reviews policy compliance, functional reliability, targeting, content
  rating, and test credentials separately. Review is not a troubleshooting step.
- Review usually takes seven days or less.
- Rejection reasons Google names: fewer than 12 opted-in testers, or insufficient
  tester engagement. Google may ask you to keep testing.

## Community advice: useful, but not official

The article recommends:

- three or more closed-test releases
- 250–300 characters per answer
- fewer than 10 pre-launch report issues

Google does not publish those three thresholds. Treat them as a practical
quality strategy, not a rule:

- **Multiple releases:** good when each one contains real feedback-driven work.
  Do not upload empty builds just to increase a count.
- **Long answers:** write specific, factual answers. Length alone does not help.
- **Pre-launch report:** fix every crash and ANR. Warnings and third-party SDK
  notices are not automatically failures.

## Bihua's 14-day plan

### Day 0 — publish the current build

- Upload versionCode **12** / version 1.0.2 to the existing closed-test track.
- Release name: `1.0.2 closed test`.
- Paste `store/release-notes-1.0.2.txt` with the `<en-US>` tags.
- Keep the same track. Do not pause it or create a new one.
- Confirm all 12 testers are opted in, not merely invited.
- Ask each tester to update from Play.

### Days 1–3 — collect real usage and feedback

Ask testers to cover:

- onboarding and the first lesson
- writing, checking, and choosing a grade
- closing and reopening the app to test Resume
- Practice, character search, and the picker
- word practice and paper check
- Settings: session order, mixed sessions, progress bar, theme
- phone and tablet, light and dark mode

Collect feedback through:

- the in-app **Settings → About → Send feedback** action
- Play Console → Testing feedback
- a group chat or email thread

Keep a log with: tester, date, feature, issue, severity, action, version fixed.

### Days 3–5 — ship the first feedback build

Upload versionCode **13** with real fixes from the log. In the release notes name
the user-visible changes, for example:

- fixed a layout or contrast issue
- clarified a label testers misunderstood
- fixed a crash or resume problem
- improved a tablet layout

Do not describe internal refactors as user fixes.

### Days 7–9 — ship the second feedback build

Upload versionCode **14** with another round of real fixes. Ask testers to retest
the exact issues they reported and confirm they are closed.

### Day 14 — apply for production access

Before applying:

- confirm 12+ testers have been continuously opted in for 14 days
- confirm at least two or three of them used the app regularly across the window
- run the pre-launch report on the latest build
- fix crashes and ANRs
- confirm the store listing, screenshots, privacy policy, and Data safety match
- have the feedback log and version history ready

If refused, continue the closed test. Do not remove testers or pause the track.

## Draft answers

Replace bracketed facts with the truth. Aim for specific, verifiable answers; do
not pad to a character count.

### 1. How did you recruit testers?

> I recruited [12–16] testers through [personal network / a language-learning
> community / a testing group]. I looked for people who already study Chinese,
> Japanese, or Korean, plus a few complete beginners so the onboarding could be
> tested without prior knowledge. Each tester received the Play opt-in link,
> installed Bihua from Google Play, and stayed opted in for the full window. I
> also used Play Console's testing feedback page and the in-app Send feedback
> action in Settings → About.

### 2. How easy was it to recruit testers?

> Moderately easy. The app is free and has no account, so installing it was
> simple, but finding people who would use it regularly for two weeks took more
> effort than a one-off install. I invited a small buffer above 12 so a dropout
> would not put the test below the threshold. The testers who stayed were
> motivated learners rather than people who only installed the app once.

### 3. Describe tester engagement during the closed test.

> Testers used Bihua across the full window, not just on install day. They
> completed first lessons, traced and wrote characters, checked the answer,
> chose grades, closed and reopened the app to test Resume, tried Practice,
> word practice, paper check, and reviewed Settings. I asked for feedback on
> both phone and tablet layouts, light and dark themes, and the four scripts.
> Their reports led to [number] changes in [number] closed-test updates. I kept
> the feedback thread and Play testing feedback as a record.

### 4. Summarize the feedback and how you collected it.

> Feedback came from Play Console's Testing feedback page, an email/group thread,
> and the in-app Send feedback action. Testers reported [example: the grade
> choices were easy to miss after checking; long helper text reduced canvas
> space; Settings sections were hard to find; mixed sessions needed a clearer
> progress bar; the tablet layout needed checking]. They also asked for
> [example: clearer word/paper actions and a clearer pronunciation result].
> I logged each report, reproduced it, fixed it in a later closed-test build, and
> asked the reporter to retest it.

### 5. What changes did you make from the feedback?

> In 1.0.2 I moved the answer and four grades into a fixed bar after every
> writing check, shortened helper copy on the lesson screens, added a searchable
> Settings home with grouped sections, added reviews-first/mixed/lessons-first
> session order, added a full-session and next-lesson progress mode, and rebuilt
> the pronunciation result as a large feedback card with the typed and accepted
> readings. I also fixed tablet navigation, emulator/mouse input, duplicate
> variant rows, and several layout and accessibility issues. [Add any additional
> tester-reported fixes and the version that shipped them.]

### 6. How did you decide the app was ready for production?

> The latest closed-test build has no known crashes or ANRs. It passes unit
> tests, lint, content audit, APK verification, and release smoke tests on a
> phone and a tablet emulator. I tested onboarding, lessons, grading, Resume,
> Practice, word/paper practice, settings, backup, sync, light/dark themes, and
> large text. Testers retested the issues they reported. The store listing,
> screenshots, privacy policy, and Data safety declarations now match the build.
> The pre-launch report was checked for crashes and ANRs before applying.

### 7. What did you do differently this time?

> I treated the closed test as a feedback loop rather than a countdown. Every
> report went into a log, was reproduced, fixed in a new closed-test release, and
> retested by the person who reported it. I also tested phone and tablet layouts,
> emulator/mouse input, accessibility, and the cases where Google ML Kit cannot
> read a stroke. I kept the 12 testers continuously opted in and did not pause or
> replace the track while shipping updates.

### 8. Target audience

> Teen and adult learners, 13 and older, who want to write Chinese, Japanese, or
> Korean characters by hand. This includes university students, self-study
> learners, heritage speakers rebuilding writing skills, and tutors who want an
> offline practice tool. The app is not directed to children under 13.

### 9. Value proposition

> Bihua is a handwriting-first study app for four script-pure courses. The learner
> writes from a cue on an empty canvas, gets on-device ink evidence, chooses the
> grade, and FSRS-7 schedules the next review. It works offline, has no account,
> no ads, and no developer-run analytics. The writing square stays the main
> surface, and word practice and paper check transfer the skill off the screen.

### 10. First-year install estimate

> Choose the honest range you can support. For a free, niche education app with
> no paid marketing, `1,000–10,000` is a reasonable first-year planning range.
> Do not inflate it; the number is a plan, not a promise.

## Do not do

- Do not use internal testers as the 12 closed testers.
- Do not count invited emails or group members who never opted in.
- Do not pause the closed track during the 14 days.
- Do not upload meaningless builds only to reach a release count.
- Do not claim features or fixes that are not in the reviewed build.
- Do not leave the old 5 phone screenshots or the old description in Play
  Console.
- Do not publish AI translations without a native-speaker check.
