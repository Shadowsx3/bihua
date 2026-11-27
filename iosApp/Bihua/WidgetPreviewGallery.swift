import SwiftUI

#if DEBUG
/// A development-only gallery that renders the real WidgetKit views at the
/// system sizes, so every family can be checked without adding widgets to the
/// Home Screen by hand. Launch with `-widget-gallery today|week|word|all`.
struct WidgetPreviewGallery: View {
    private var page: String {
        let args = ProcessInfo.processInfo.arguments
        guard let index = args.firstIndex(of: "-widget-gallery"), index + 1 < args.count else {
            return "all"
        }
        return args[index + 1]
    }

    var body: some View {
        GeometryReader { proxy in
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                header
                if page == "today" || page == "all" { today }
                if page == "week" || page == "all" { week }
                if page == "word" || page == "all" { word }
                Text("Preview only. On device these are real WidgetKit widgets.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 4)
            }
            .padding(20)
            .padding(.top, proxy.safeAreaInsets.top)
        }
        .background(
            LinearGradient(
                colors: [Color(hex: 0xFFF8EC), Color(hex: 0xFFEFD9), Color(hex: 0xE9F8E7)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Bihua widgets")
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundStyle(Color(hex: 0x1F2B23))
            Text("Today · Week ahead · Word of the day")
                .font(.subheadline)
                .foregroundStyle(Color(hex: 0x4C5F51))
        }
    }

    private var today: some View {
        section("Today", "Morning, afternoon, evening, and night palettes.") {
            HStack(alignment: .top, spacing: 16) {
                preview(
                    TodayWidgetView(snapshot: .sample, family: .systemSmall, hour: 9, container: false),
                    width: 170, height: 170, caption: "Small · morning"
                )
                preview(
                    TodayWidgetView(snapshot: .sample, family: .systemSmall, hour: 22, container: false),
                    width: 170, height: 170, caption: "Small · night"
                )
            }
            preview(
                TodayWidgetView(snapshot: .sample, family: .systemMedium, hour: 9, container: false),
                width: 364, height: 170, caption: "Medium · morning"
            )
            preview(
                TodayWidgetView(snapshot: .clear, family: .systemMedium, hour: 18, container: false),
                width: 364, height: 170, caption: "Medium · all clear, evening"
            )
        }
    }

    private var week: some View {
        section("Week ahead", "Seven bars, the total, and the lesson count.") {
            preview(
                WeekWidgetView(snapshot: .sample, family: .systemMedium, hour: 13, container: false),
                width: 364, height: 170, caption: "Medium · afternoon"
            )
            preview(
                WeekWidgetView(snapshot: .sample, family: .systemLarge, hour: 20, container: false),
                width: 364, height: 382, caption: "Large · evening"
            )
        }
    }

    private var word: some View {
        section("Word of the day", "The character, its reading, and one quiet line.") {
            HStack(alignment: .top, spacing: 16) {
                preview(
                    WordWidgetView(snapshot: .sample, family: .systemSmall, hour: 8, container: false),
                    width: 170, height: 170, caption: "Small · morning"
                )
                preview(
                    WordWidgetView(snapshot: .sample, family: .systemSmall, hour: 23, container: false),
                    width: 170, height: 170, caption: "Small · night"
                )
            }
            preview(
                WordWidgetView(snapshot: .sample, family: .systemMedium, hour: 16, container: false),
                width: 364, height: 170, caption: "Medium · afternoon"
            )
        }
    }

    private func section<Content: View>(
        _ title: String,
        _ subtitle: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.system(size: 20, weight: .bold, design: .rounded))
                .foregroundStyle(Color(hex: 0x1F2B23))
            Text(subtitle)
                .font(.footnote)
                .foregroundStyle(Color(hex: 0x4C5F51))
            content()
        }
    }

    private func preview<Content: View>(
        _ content: Content,
        width: CGFloat,
        height: CGFloat,
        caption: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            content
                .frame(width: width, height: height)
                .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
                .shadow(color: Color(hex: 0x1F2B23).opacity(0.14), radius: 16, y: 8)
            Text(caption)
                .font(.caption2.weight(.medium))
                .foregroundStyle(Color(hex: 0x4C5F51))
        }
    }
}
#endif
