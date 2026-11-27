import SwiftUI
import UIKit
import BihuaKit

/// The native iPhone/iPad shell. UITabBarController keeps the tab stacks and
/// UINavigationController keeps the push history; each screen inside is the
/// shared Compose content, never a second Compose tab bar.
struct RootTabsView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UITabBarController {
        let controller = UITabBarController()
        let routes = ["home", "characters", "progress", "settings"]
        let select: (String) -> Void = { [weak controller] route in
            guard let controller, let index = routes.firstIndex(of: route) else { return }
            controller.selectedIndex = index
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
                onPush: push
            )
            screen.navigationItem.title = Self.title(for: route)
            screen.navigationItem.largeTitleDisplayMode = .never
            navigation.pushViewController(screen, animated: true)
        }
        controller.viewControllers = [
            tab(title: "Home", icon: "house.fill", route: "home", hidesBar: true, onSelect: select, onPush: push),
            tab(title: "Characters", icon: "square.grid.2x2.fill", route: "characters", hidesBar: false, onSelect: select, onPush: push),
            tab(title: "Progress", icon: "chart.bar.xaxis", route: "progress", hidesBar: false, onSelect: select, onPush: push),
            tab(title: "Settings", icon: "gearshape.fill", route: "settings", hidesBar: false, onSelect: select, onPush: push),
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
        onPush: @escaping (String) -> Void
    ) -> UINavigationController {
        let screen = MainViewControllerKt.MainViewController(
            route: route,
            onSelectTab: onSelect,
            onPush: onPush
        )
        screen.navigationItem.title = title
        let navigation = UINavigationController(rootViewController: screen)
        navigation.navigationBar.prefersLargeTitles = true
        navigation.navigationBar.tintColor = UIColor(red: 0.12, green: 0.17, blue: 0.14, alpha: 1)
        if hidesBar {
            navigation.setNavigationBarHidden(true, animated: false)
        }
        navigation.tabBarItem = UITabBarItem(
            title: title,
            image: UIImage(systemName: icon),
            selectedImage: UIImage(systemName: icon)
        )
        return navigation
    }

    private static func title(for route: String) -> String {
        let last = route.split(separator: "/").last.map(String.init) ?? route
        return last.split(separator: ":").last.map(String.init) ?? last
    }
}
