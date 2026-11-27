import SwiftUI
import WidgetKit

struct WidgetEntry: TimelineEntry {
    var date: Date
    var snapshot: WidgetSnapshot
}

struct BihuaTimeline: TimelineProvider {
    func placeholder(in context: Context) -> WidgetEntry {
        WidgetEntry(date: Date(), snapshot: .sample)
    }

    func getSnapshot(in context: Context, completion: @escaping (WidgetEntry) -> Void) {
        completion(WidgetEntry(date: Date(), snapshot: SnapshotStore.load()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WidgetEntry>) -> Void) {
        let calendar = Calendar.current
        let snapshot = SnapshotStore.load()
        let now = Date()
        let start = calendar.date(
            from: calendar.dateComponents([.year, .month, .day, .hour], from: now)
        ) ?? now
        // One entry per hour for a day lets the palette and greeting change at
        // 5, 11, 17, and 22 without a background wake-up every minute.
        let entries = (0..<24).map { offset in
            WidgetEntry(
                date: calendar.date(byAdding: .hour, value: offset, to: start) ?? now,
                snapshot: snapshot
            )
        }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

private func hour(of date: Date) -> Int {
    Calendar.current.component(.hour, from: date)
}

struct TodayWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    var entry: WidgetEntry

    var body: some View {
        TodayWidgetView(
            snapshot: entry.snapshot,
            family: family,
            hour: hour(of: entry.date),
            container: true
        )
    }
}

struct WeekWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    var entry: WidgetEntry

    var body: some View {
        WeekWidgetView(
            snapshot: entry.snapshot,
            family: family,
            hour: hour(of: entry.date),
            container: true
        )
    }
}

struct WordWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    var entry: WidgetEntry

    var body: some View {
        WordWidgetView(
            snapshot: entry.snapshot,
            family: family,
            hour: hour(of: entry.date),
            container: true
        )
    }
}

struct TodayWidget: Widget {
    private let kind = "BihuaTodayWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: BihuaTimeline()) { entry in
            TodayWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Today")
        .description("The reviews and new lessons waiting now, with Mò.")
        .supportedFamilies([
            .systemSmall, .systemMedium,
            .accessoryCircular, .accessoryRectangular, .accessoryInline,
        ])
    }
}

struct WeekWidget: Widget {
    private let kind = "BihuaWeekWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: BihuaTimeline()) { entry in
            WeekWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Week ahead")
        .description("Seven days of reviews at a glance, with the lessons done.")
        .supportedFamilies([.systemMedium, .systemLarge, .accessoryCircular, .accessoryRectangular])
    }
}

struct WordWidget: Widget {
    private let kind = "BihuaWordWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: BihuaTimeline()) { entry in
            WordWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Word of the day")
        .description("One character, its reading, and its meaning on the paper.")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryCircular, .accessoryRectangular])
    }
}

@main
struct BihuaWidgetBundle: WidgetBundle {
    var body: some Widget {
        TodayWidget()
        WeekWidget()
        WordWidget()
    }
}
