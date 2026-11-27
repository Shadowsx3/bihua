import SwiftUI
import UIKit
import BihuaKit

/// The only SwiftUI in the app: a small host that mounts the native tab shell.
/// Each tab's screen is shared Compose; the tab bar and navigation stack are
/// UIKit. Widgets are deferred from the first iOS build.
struct RootView: View {
    var body: some View {
        shell
    }

    private var shell: some View {
        RootTabsView()
            .ignoresSafeArea()
    }
}
