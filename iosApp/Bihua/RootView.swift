import SwiftUI
import UIKit
import BihuaKit

/// The only SwiftUI in the app: a 20-line host that mounts the shared
/// Compose view controller. Screens live in Kotlin.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct RootView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.container, edges: .all)
    }
}
