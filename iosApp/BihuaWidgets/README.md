# Deferred: iOS widgets

These SwiftUI WidgetKit views are **not part of the first iOS build**. The
`BihuaWidgets` target is intentionally absent from `iosApp/project.yml`, the app
does not embed a `.appex`, and the app has no App Group entitlement. This keeps
the first App Store review focused on the core study app.

The code is kept here so the work is not lost. To enable it in a later release:

1. Add the `BihuaWidgets` app-extension target to `project.yml` and embed it in
   `Bihua`.
2. Add the App Group `group.com.bassmd.bihua` to the app and extension
   entitlements, and enable it for both bundle ids in the Apple Developer
   account.
3. Set `Capabilities.widgets = true` in `IosPlatformServices.kt`.
4. Add a small shared snapshot model and publish it to the App Group after the
   shared data layer lands.
5. Update the privacy policy, App Store answers, listing, and parity table.

`WidgetShared/` and `BihuaWidgets/` are outside the Xcode target sources, so they
do not ship or compile.
