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
    @State private var probe = "Compose host"

    var body: some View {
        ZStack(alignment: .bottom) {
            ComposeView()
                .ignoresSafeArea(.container, edges: .all)
            Text(probe)
                .font(.caption2)
                .foregroundStyle(.black.opacity(0.72))
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(.white.opacity(0.78), in: Capsule())
                .padding(.bottom, 18)
        }
        .task {
            #if DEBUG
            #if canImport(MLKitDigitalInkRecognition)
            MlKitProbe.run { result in
                DispatchQueue.main.async { probe = result }
            }
            #else
            probe = "BihuaKit · simulator"
            #endif
            #endif
        }
    }
}
