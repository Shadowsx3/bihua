import SwiftUI
import UIKit
import UniformTypeIdentifiers
import BihuaKit

/// The native iPhone/iPad shell. UITabBarController keeps the tab stacks and
/// UINavigationController keeps the push history; each screen inside is the
/// shared Compose content, never a second Compose tab bar.
struct RootTabsView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UITabBarController {
        let controller = UITabBarController()
        let pickerHost = DocumentPickerHost()
        let routes = ["home", "characters", "practice", "progress", "settings"]
        let select: (String) -> Void = { [weak controller] route in
            guard let controller, let index = routes.firstIndex(of: route) else { return }
            controller.selectedIndex = index
        }
        let pop: () -> Void = { [weak controller] in
            (controller?.selectedViewController as? UINavigationController)?.popViewController(animated: true)
        }
        let shareFile: (String) -> Void = { [weak controller] path in
            guard
                let controller,
                let navigation = controller.selectedViewController as? UINavigationController
            else { return }
            let activity = UIActivityViewController(
                activityItems: [URL(fileURLWithPath: path)],
                applicationActivities: nil
            )
            if let popover = activity.popoverPresentationController {
                popover.sourceView = navigation.view
                popover.sourceRect = CGRect(
                    x: navigation.view.bounds.midX,
                    y: navigation.view.bounds.midY,
                    width: 1,
                    height: 1
                )
                popover.permittedArrowDirections = []
            }
            navigation.present(activity, animated: true)
        }
        let onboardingChanged: (KotlinBoolean) -> Void = { [weak controller] visible in
            controller?.tabBar.isHidden = visible.boolValue
        }
        let requestFilePicker: () -> Void = { [weak controller] in
            guard
                let controller,
                let navigation = controller.selectedViewController as? UINavigationController
            else { return }
            let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.json], asCopy: true)
            let delegate = DocumentPickerDelegate()
            pickerHost.delegate = delegate
            picker.delegate = delegate
            navigation.present(picker, animated: true)
        }
        let applyTheme: (KotlinBoolean) -> Void = { [weak controller] dark in
            let isDark = dark.boolValue
            let style: UIUserInterfaceStyle = isDark ? .dark : .light
            let title: UIColor = isDark
                ? .white
                : UIColor(red: 0.12, green: 0.17, blue: 0.14, alpha: 1)
            controller?.overrideUserInterfaceStyle = style
            controller?.tabBar.tintColor = isDark
                ? UIColor(red: 0.96, green: 0.95, blue: 0.90, alpha: 1)
                : UIColor(red: 0.12, green: 0.17, blue: 0.14, alpha: 1)
            controller?.tabBar.unselectedItemTintColor = isDark
                ? UIColor(red: 0.72, green: 0.78, blue: 0.74, alpha: 1)
                : UIColor(red: 0.30, green: 0.37, blue: 0.32, alpha: 1)
            controller?.viewControllers?.forEach { child in
                child.overrideUserInterfaceStyle = style
                guard let navigation = child as? UINavigationController else { return }
                navigation.overrideUserInterfaceStyle = style
                navigation.navigationBar.overrideUserInterfaceStyle = style
                let appearance = UINavigationBarAppearance()
                appearance.configureWithDefaultBackground()
                appearance.largeTitleTextAttributes = [.foregroundColor: title]
                appearance.titleTextAttributes = [.foregroundColor: title]
                navigation.navigationBar.standardAppearance = appearance
                navigation.navigationBar.scrollEdgeAppearance = appearance
            }
        }
        var push: ((String) -> Void)!
        push = { [weak controller] route in
            guard
                let controller,
                let navigation = controller.selectedViewController as? UINavigationController
            else { return }
            let screen = MainViewControllerKt.MainViewController(
                route: route,
                onSelectTab: select,
                onPush: push,
                onThemeChange: applyTheme,
                onPop: pop,
                onShareFile: shareFile,
                onRequestFilePicker: requestFilePicker,
                onOnboardingChanged: onboardingChanged
            )
            screen.navigationItem.title = Self.title(for: route)
            screen.hidesBottomBarWhenPushed = route == "review"
            screen.navigationItem.largeTitleDisplayMode = .never
            navigation.pushViewController(screen, animated: true)
        }
        controller.viewControllers = [
            tab(title: "Home", icon: "house.fill", route: "home", hidesBar: true, onSelect: select, onPush: push, onThemeChange: applyTheme, onShareFile: shareFile, onRequestFilePicker: requestFilePicker, onOnboardingChanged: onboardingChanged),
            tab(title: "Characters", icon: "square.grid.2x2.fill", route: "characters", hidesBar: false, onSelect: select, onPush: push, onThemeChange: applyTheme, onShareFile: shareFile, onRequestFilePicker: requestFilePicker, onOnboardingChanged: onboardingChanged),
            tab(title: "Practice", icon: "paintbrush.pointed.fill", route: "practice", hidesBar: false, onSelect: select, onPush: push, onThemeChange: applyTheme, onShareFile: shareFile, onRequestFilePicker: requestFilePicker, onOnboardingChanged: onboardingChanged),
            tab(title: "Progress", icon: "chart.bar.xaxis", route: "progress", hidesBar: false, onSelect: select, onPush: push, onThemeChange: applyTheme, onShareFile: shareFile, onRequestFilePicker: requestFilePicker, onOnboardingChanged: onboardingChanged),
            tab(title: "Settings", icon: "gearshape.fill", route: "settings", hidesBar: false, onSelect: select, onPush: push, onThemeChange: applyTheme, onShareFile: shareFile, onRequestFilePicker: requestFilePicker, onOnboardingChanged: onboardingChanged),
        ]

        let ink = UIColor(red: 0.12, green: 0.17, blue: 0.14, alpha: 1)
        controller.tabBar.tintColor = ink
        controller.tabBar.unselectedItemTintColor = UIColor(red: 0.30, green: 0.37, blue: 0.32, alpha: 1)
        controller.view.backgroundColor = UIColor(red: 0.984, green: 0.976, blue: 0.937, alpha: 1)
        #if DEBUG
        // Screenshot and QA helper: `-tab 2` opens that tab; `-push
        // character/SIMPLIFIED:爱` opens a detail route.
        let args = ProcessInfo.processInfo.arguments
        if let index = args.firstIndex(of: "-tab"), index + 1 < args.count,
           let selected = Int(args[index + 1]), selected < controller.viewControllers?.count ?? 0 {
            controller.selectedIndex = selected
        }
        if let index = args.firstIndex(of: "-push"), index + 1 < args.count {
            let route = args[index + 1]
            DispatchQueue.main.async { push(route) }
        }
        #endif
        return controller
    }

    func updateUIViewController(_ uiViewController: UITabBarController, context: Context) {}

    private func tab(
        title: String,
        icon: String,
        route: String,
        hidesBar: Bool,
        onSelect: @escaping (String) -> Void,
        onPush: @escaping (String) -> Void,
        onThemeChange: @escaping (KotlinBoolean) -> Void,
        onShareFile: @escaping (String) -> Void,
        onRequestFilePicker: @escaping () -> Void,
        onOnboardingChanged: @escaping (KotlinBoolean) -> Void
    ) -> UINavigationController {
        var screen: UIViewController!
        let navigation = RootNavigationController()
        navigation.hidesBarAtRoot = hidesBar
        screen = MainViewControllerKt.MainViewController(
            route: route,
            onSelectTab: onSelect,
            onPush: onPush,
            onThemeChange: onThemeChange,
            onPop: { [weak navigation] in
                navigation?.popViewController(animated: true)
            },
            onShareFile: onShareFile,
            onRequestFilePicker: onRequestFilePicker,
            onOnboardingChanged: onOnboardingChanged
        )
        screen.navigationItem.title = title
        navigation.setViewControllers([screen], animated: false)
        navigation.navigationBar.prefersLargeTitles = true
        navigation.navigationBar.tintColor = UIColor(red: 0.12, green: 0.17, blue: 0.14, alpha: 1)
        navigation.setNavigationBarHidden(hidesBar, animated: false)
        navigation.tabBarItem = UITabBarItem(
            title: title,
            image: UIImage(systemName: icon),
            selectedImage: UIImage(systemName: icon)
        )
        if route == "characters" {
            let dictionary = UIBarButtonItem(
                image: UIImage(systemName: "character.book.closed.fill"),
                primaryAction: UIAction { _ in onPush("dictionary") }
            )
            dictionary.accessibilityLabel = "Dictionary"
            screen.navigationItem.rightBarButtonItem = dictionary
        }
        return navigation
    }

    private static func title(for route: String) -> String {
        if route == "review" { return "Review" }
        if route == "dictionary" { return "Dictionary" }
        if route == "word-practice" { return "Word practice" }
        if route == "lists" { return "Study lists" }
        if route.hasPrefix("list/") { return "Study list" }
        if route.hasPrefix("stroke/") { return "Stroke order" }
        let last = route.split(separator: "/").last.map(String.init) ?? route
        return last.split(separator: ":").last.map(String.init) ?? last
    }
}

/// A navigation stack that hides its bar only at the tab root, so Home keeps
/// its own header but a pushed review still gets a native back button.
final class RootNavigationController: UINavigationController,
    UINavigationControllerDelegate, UIGestureRecognizerDelegate {
    var hidesBarAtRoot = false

    override func viewDidLoad() {
        super.viewDidLoad()
        delegate = self
        // Keep the edge swipe aligned with the visible navigation bar. The
        // default delegate disables it while the bar is hidden, which would
        // strand the learner on a pushed review or character page.
        interactivePopGestureRecognizer?.delegate = self
    }

    func navigationController(
        _ navigationController: UINavigationController,
        willShow viewController: UIViewController,
        animated: Bool
    ) {
        let atRoot = viewController === viewControllers.first
        setNavigationBarHidden(hidesBarAtRoot && atRoot, animated: animated)
    }

    func navigationController(
        _ navigationController: UINavigationController,
        didShow viewController: UIViewController,
        animated: Bool
    ) {
        // Never let the root be popped by the edge gesture, even after a
        // cancelled transition.
        interactivePopGestureRecognizer?.isEnabled = viewControllers.count > 1
    }

    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        viewControllers.count > 1 && transitionCoordinator == nil
    }
}

private final class DocumentPickerHost {
    var delegate: DocumentPickerDelegate?
}

private final class DocumentPickerDelegate: NSObject, UIDocumentPickerDelegate {
    func documentPicker(
        _ controller: UIDocumentPickerViewController,
        didPickDocumentsAt urls: [URL]
    ) {
        IosFileImportKt.deliverFilePickerPath(path: urls.first?.path)
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        IosFileImportKt.deliverFilePickerPath(path: nil)
    }
}
