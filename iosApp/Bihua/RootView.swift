import SwiftUI
import UIKit
import WidgetKit
import BihuaKit

/// The only SwiftUI in the app: a small host that mounts the native tab shell.
/// Each tab's screen is shared Compose; the tab bar and navigation stack are
/// UIKit. The widget extension reads the App Group snapshot and draws itself
/// in SwiftUI.
struct RootView: View {
    private var showsWidgetGallery: Bool {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("-widget-gallery")
        #else
        false
        #endif
    }

    var body: some View {
        Group {
            #if DEBUG
            if showsWidgetGallery {
                WidgetPreviewGallery()
            } else {
                shell
            }
            #else
            shell
            #endif
        }
        .onAppear {
            // The app publishes the snapshot before the first Compose frame;
            // ask WidgetKit to redraw with it.
            WidgetCenter.shared.reloadAllTimelines()
        }
    }

    private var shell: some View {
        RootTabsView()
            .ignoresSafeArea()
    }
}
