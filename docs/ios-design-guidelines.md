# iOS design guidelines checklist

Bihua's iOS client follows the Apple Human Interface Guidelines and the iOS 26
system appearance. The shell is native UIKit navigation; Compose renders screen
content. Nothing here is a claim about the Android app.

Status: P3. The shared Home, theme, components, calendar, and pet render on
iPhone and iPad; the native tab bar/navigation shell and the remaining screens
are next. Use this page as the review checklist for that pass.

## Shell and navigation

- [ ] `UITabBarController` with Home, Characters, Progress, Practice, Settings.
      One `UINavigationController` per tab keeps each stack.
- [ ] System tab bar and navigation bar. No Compose-drawn bottom bar or title
      bar on iOS.
- [ ] Large titles on top-level routes; inline titles on pushed screens.
- [ ] Interactive swipe-back from every pushed screen, including the Settings
      section stack and character detail.
- [ ] Tab selection restores the last route in that tab; a second tap pops to
      the root, as iOS expects.
- [ ] Modals use the system sheet (`.pageSheet`/`.formSheet`) with a grabber
      where appropriate; destructive confirmations use a system alert.
- [ ] No Android back arrow or "Close" button where the system provides the
      gesture or the sheet's grabber.
- [ ] iPad: split view and Stage Manager work; content uses a readable width
      (max ~700 pt) and the tab bar or a sidebar follows the size class.

## Layout and type

- [ ] Safe areas and the home indicator respected on every screen; the paper
      background extends under the status bar and indicator.
- [ ] Keyboard avoidance on every text field: report composer,
      study-list name, weights import, pairing code, notes, synonyms, meaning.
      The field and its primary action stay above the keyboard.
- [x] Dynamic Type from xS to AX5. `IosDynamicType` maps every
      `UIContentSizeCategory` to the Apple body-text multiplier and updates on
      the system notification; verified on Home at Accessibility Large. Still
      to re-check on review, Settings, and detail.
- [x] Pointer and trackpad support (`UIApplicationSupportsIndirectInputEvents`);
      hover and click work on iPad without a compatibility mode.
- [ ] SF for system chrome; the Bihua serif only where it is intentional and
      still scales. No fixed-height text containers.
- [ ] 44 x 44 pt minimum touch targets; 8 pt spacing grid; no clipped labels.
- [ ] All four orientations on iPhone and iPad; no content under the notch or
      the home indicator.

## Colour and materials

- [ ] Light and dark follow the system; `preferredColorScheme` only when the
      learner explicitly chooses a theme.
- [ ] Chrome uses system materials and semantic colours; content keeps the
      paper and ink identity.
- [ ] Contrast checked for body text, secondary text, and disabled states.
- [ ] No colour-only meaning (grades, due states, errors).

## Controls and feedback

- [ ] Switches, segmented controls, pickers, and steppers are the system
      controls where the platform has one.
- [ ] Context menus on character tiles and list rows; swipe actions for
      destructive list operations.
- [ ] Press feedback is an iOS highlight, not a Material ripple. Haptics only
      for meaningful actions, respecting the system setting.
- [ ] Sheets and alerts use system presentation and corner radii.
- [ ] Loading uses a system spinner or a quiet skeleton, never a perpetual
      decorative animation.

## System integration

- [ ] Share sheet for the report composer and PDFs; document picker for import
      and export; Files access without a custom browser.
- [ ] Notification permission requested only when reminders are turned on, with
      a clear explanation and the system prompt.
- [ ] Speech uses the system voice and does not interrupt other audio
      unexpectedly.
- [ ] No local network permission (no sync); no camera, microphone, contacts,
      photo library, or tracking prompts.
- [ ] App Store rating row opens the listing only after a tap.

## Accessibility

- [ ] Every control has a spoken label, a value where it has one, and a hint
      where the result is not obvious.
- [ ] The ink canvas exposes Check, Show answer, Undo, and Clear as custom
      actions; the rotor reaches headings and form fields.
- [ ] Reduce Motion stops the tour typewriter, confetti, bobbing, and long
      transitions.
- [ ] Bold Text, Increase Contrast, and Button Shapes do not clip or hide
      content.
- [ ] VoiceOver order follows the visual order; no focus traps in sheets.

## Privacy and compliance

- [ ] Profile uses the twelve drawn pet looks in a 3 x 4 grid; no name field,
      photo picker, or photo-library permission.
- [ ] Permission strings are specific and appear only when needed.
- [ ] `PrivacyInfo.xcprivacy` ships; required-reason APIs match the archive
      report.
- [ ] App Privacy answers match `PRIVACY.md`; no tracking, no advertising ID.
- [ ] No other platform's names, icons, or imagery in the iOS UI.
- [ ] Age rating 16+ with the mature-content descriptor; mature glosses remain
      off by default and gated.

## Test matrix

- iPhone SE-class (smallest supported), iPhone 17e, iPad 13-inch; portrait and
  landscape; light and dark; 1.0, 1.5, and 2.0 text; Reduce Motion on and off;
  VoiceOver on for a full review flow.

Evidence: screenshots in `.qa/ios/`, simulator smoke via
`python3 qa/ios/smoke.py`, device build via `make ios-device`, XCUITest for
rotation, keyboard, and VoiceOver in P6.
