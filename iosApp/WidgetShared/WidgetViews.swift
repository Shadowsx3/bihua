import SwiftUI
import WidgetKit

/// A quiet paper card with a soft glow in the corner. The widget draws inside
/// the system's content margins; the gallery passes `container: false` and adds
/// the same inset itself.
private struct PaperSurface: ViewModifier {
    var palette: WidgetPalette
    var container: Bool

    func body(content: Content) -> some View {
        content
            // The system insets widget content; the gallery supplies the same
            // inset itself so the preview matches the Home Screen.
            .padding(container ? 0 : 16)
            .overlay(alignment: .topTrailing) {
                Circle()
                    .fill(palette.accent.opacity(palette.isDark ? 0.16 : 0.22))
                    .frame(width: 150, height: 150)
                    .blur(radius: 24)
                    .offset(x: 44, y: -56)
                    .allowsHitTesting(false)
            }
            .widgetSurface(palette, container: container)
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }
}

extension View {
    @ViewBuilder
    func widgetSurface(_ palette: WidgetPalette, container: Bool) -> some View {
        if container {
            if #available(iOS 17.0, *) {
                self.containerBackground(for: .widget) { palette.background }
            } else {
                self.background(palette.background)
            }
        } else {
            self.background(palette.background)
        }
    }

    func paperSurface(_ palette: WidgetPalette, container: Bool) -> some View {
        modifier(PaperSurface(palette: palette, container: container))
    }
}

private extension Text {
    func numeral(_ size: CGFloat) -> Text {
        font(.system(size: size, weight: .bold, design: .rounded))
            .monospacedDigit()
    }
}

// MARK: - Today

struct TodayWidgetView: View {
    var snapshot: WidgetSnapshot
    var family: WidgetFamily
    var hour: Int
    var container: Bool

    private var palette: WidgetPalette { .of(DayPart(hour: hour)) }
    private var part: DayPart { DayPart(hour: hour) }
    private var copy: TodayCopy {
        WidgetCopy.today(
            due: snapshot.due,
            newLeft: snapshot.newLeft,
            vacation: snapshot.vacation,
            hour: hour
        )
    }

    var body: some View {
        switch family {
        case .systemSmall:
            small
        case .systemMedium:
            medium
        case .accessoryCircular:
            circular
        case .accessoryRectangular:
            rectangular
        case .accessoryInline:
            Text("\(copy.greeting) · \(snapshot.due) reviews · \(snapshot.newLeft) new")
        default:
            medium
        }
    }

    private var small: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                Label(copy.greeting, systemImage: icon)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(palette.soft)
                    .labelStyle(.titleAndIcon)
                Spacer(minLength: 4)
                PetView(
                    prop: WidgetCopy.todayPet(
                        due: snapshot.due,
                        newLeft: snapshot.newLeft,
                        vacation: snapshot.vacation,
                        hour: hour
                    ),
                    accent: palette.accent
                )
                .frame(width: 46, height: 46)
                .offset(y: -4)
            }

            Spacer(minLength: 2)

            if snapshot.vacation || (snapshot.due == 0 && snapshot.newLeft == 0) {
                Text(snapshot.vacation ? "Paused" : "All clear")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundStyle(palette.ink)
                Text(copy.detail)
                    .font(.system(size: 10))
                    .foregroundStyle(palette.soft)
                    .lineLimit(1)
            } else {
                HStack(alignment: .firstTextBaseline, spacing: 14) {
                    count(snapshot.due, "reviews", palette.review)
                    if snapshot.newLeft > 0 {
                        count(snapshot.newLeft, "new", palette.lesson)
                    }
                }
                Text(WidgetCopy.forecast(snapshot, hourly: true) ?? copy.detail)
                    .font(.system(size: 9.5))
                    .foregroundStyle(palette.soft)
                    .lineLimit(1)
                    .padding(.top, 4)
            }

            Spacer(minLength: 4)
            pill(copy.action)
        }
        .paperSurface(palette, container: container)
    }

    private var medium: some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 0) {
                Label(copy.greeting, systemImage: icon)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(palette.soft)
                Text(copy.headline)
                    .font(.system(size: 20, weight: .bold, design: .rounded))
                    .foregroundStyle(palette.ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                    .padding(.top, 5)
                Text(WidgetCopy.forecast(snapshot, hourly: true) ?? copy.detail)
                    .font(.system(size: 10))
                    .foregroundStyle(palette.soft)
                    .lineLimit(2)
                    .padding(.top, 2)
                Spacer(minLength: 4)
                pill(copy.action)
            }
            Spacer(minLength: 0)
            VStack(spacing: 6) {
                PetView(
                    prop: WidgetCopy.todayPet(
                        due: snapshot.due,
                        newLeft: snapshot.newLeft,
                        vacation: snapshot.vacation,
                        hour: hour
                    ),
                    accent: palette.accent
                )
                .frame(width: 78, height: 78)
                if !snapshot.vacation {
                    HStack(spacing: 8) {
                        miniChip("\(snapshot.due)", "due", palette.review)
                        miniChip("\(snapshot.newLeft)", "new", palette.lesson)
                    }
                }
            }
        }
        .paperSurface(palette, container: container)
    }

    private func count(_ value: Int, _ label: String, _ tint: Color) -> some View {
        VStack(alignment: .leading, spacing: -2) {
            Text("\(value)").numeral(34).foregroundStyle(palette.ink)
            Text(label)
                .font(.system(size: 9, weight: .semibold))
                .foregroundStyle(tint)
                .textCase(.uppercase)
        }
    }

    private func miniChip(_ value: String, _ label: String, _ tint: Color) -> some View {
        VStack(spacing: -1) {
            Text(value).font(.system(size: 15, weight: .bold, design: .rounded)).monospacedDigit()
            Text(label).font(.system(size: 8, weight: .semibold)).textCase(.uppercase)
        }
        .foregroundStyle(tint)
        .frame(minWidth: 40)
        .padding(.horizontal, 5)
        .padding(.vertical, 4)
        .background(
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(tint.opacity(palette.isDark ? 0.16 : 0.12))
        )
    }

    private func pill(_ title: String) -> some View {
        HStack(spacing: 5) {
            Text(title).font(.system(size: 11, weight: .semibold))
            Image(systemName: "arrow.right")
                .font(.system(size: 9, weight: .bold))
        }
        .foregroundStyle(pillText)
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Capsule().fill(pillFill))
    }

    private var pillFill: Color { palette.isDark ? Color(hex: 0xF7F2E6) : Color(hex: 0x1F2B23) }
    private var pillText: Color { palette.isDark ? Color(hex: 0x1F2B23) : Color(hex: 0xFFFDF4) }

    private var icon: String {
        switch part {
        case .morning: "sun.max.fill"
        case .afternoon: "leaf.fill"
        case .evening, .night: "moon.stars.fill"
        }
    }

    private var circular: some View {
        ZStack {
            AccessoryWidgetBackground()
            Circle()
                .stroke(palette.accent.opacity(0.35), lineWidth: 4)
            VStack(spacing: -1) {
                Text("\(snapshot.due)")
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .monospacedDigit()
                Text(snapshot.vacation ? "paused" : "reviews")
                    .font(.system(size: 8, weight: .semibold))
            }
        }
    }

    private var rectangular: some View {
        HStack(spacing: 8) {
            PetView(prop: .hat, accent: palette.accent, showBrush: false)
                .frame(width: 30, height: 30)
            VStack(alignment: .leading, spacing: 1) {
                Text(copy.greeting).font(.system(size: 10, weight: .semibold))
                Text("\(snapshot.due) reviews · \(snapshot.newLeft) new")
                    .font(.system(size: 12, weight: .bold, design: .rounded))
            }
        }
    }
}

// MARK: - Week

struct WeekWidgetView: View {
    var snapshot: WidgetSnapshot
    var family: WidgetFamily
    var hour: Int
    var container: Bool

    private var palette: WidgetPalette { .of(DayPart(hour: hour)) }
    private var maxCount: Int { max(1, snapshot.week.map(\.count).max() ?? 1) }

    var body: some View {
        switch family {
        case .systemLarge:
            large
        case .accessoryRectangular:
            HStack(spacing: 8) {
                VStack(alignment: .leading, spacing: 0) {
                    Text("This week").font(.system(size: 10, weight: .semibold))
                    Text("\(snapshot.weekTotal) reviews")
                        .font(.system(size: 13, weight: .bold, design: .rounded))
                }
                Spacer()
                miniBars(height: 24, width: 5, spacing: 3)
            }
        case .accessoryCircular:
            ZStack {
                AccessoryWidgetBackground()
                VStack(spacing: -1) {
                    Text("\(snapshot.weekTotal)")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                    Text("this week").font(.system(size: 7.5, weight: .semibold))
                }
            }
        default:
            medium
        }
    }

    private var medium: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 0) {
                Text("This week")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(palette.soft)
                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    Text("\(snapshot.weekTotal)")
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(palette.ink)
                    Text("reviews")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(palette.soft)
                }
                Text("\(snapshot.lessonsDone) of \(snapshot.lessonsTotal) lessons · \(snapshot.due) due today")
                    .font(.system(size: 9.5))
                    .foregroundStyle(palette.soft)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
                Spacer(minLength: 4)
                Text("A steady seven days")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(palette.ink.opacity(0.85))
            }
            Spacer(minLength: 0)
            VStack(spacing: 6) {
                miniBars(height: 62, width: 10, spacing: 5)
                HStack(spacing: 5) {
                    ForEach(Array(["S", "M", "T", "W", "T", "F", "S"].enumerated()), id: \.offset) { _, letter in
                        Text(letter)
                            .font(.system(size: 7.5, weight: .semibold))
                            .foregroundStyle(palette.soft.opacity(0.8))
                            .frame(width: 10)
                    }
                }
            }
            .frame(width: 100)
        }
        .paperSurface(palette, container: container)
    }

    private var large: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("This week")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(palette.soft)
                    Text("\(snapshot.weekTotal) reviews")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundStyle(palette.ink)
                    Text("\(snapshot.lessonsDone) of \(snapshot.lessonsTotal) lessons done")
                        .font(.system(size: 10))
                        .foregroundStyle(palette.soft)
                }
                Spacer()
                PetView(
                    prop: snapshot.weekTotal == 0 ? .party : .hat,
                    accent: palette.accent
                )
                .frame(width: 58, height: 58)
            }

            Spacer(minLength: 10)

            HStack(alignment: .bottom, spacing: 10) {
                ForEach(Array(snapshot.week.enumerated()), id: \.offset) { index, day in
                    VStack(spacing: 5) {
                        Text("\(day.count)")
                            .font(.system(size: 9, weight: .semibold, design: .rounded))
                            .foregroundStyle(palette.soft)
                            .monospacedDigit()
                        RoundedRectangle(cornerRadius: 6, style: .continuous)
                            .fill(barColor(index))
                            .frame(height: max(8, 96 * CGFloat(day.count) / CGFloat(maxCount)))
                        Text(weekday(index))
                            .font(.system(size: 8, weight: .semibold))
                            .foregroundStyle(palette.soft.opacity(0.85))
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(height: 130, alignment: .bottom)
            .padding(.bottom, 2)

            Spacer(minLength: 8)

            HStack {
                Text("Due today: \(snapshot.due)")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(palette.soft)
                Spacer()
                Text("Next: \(nextDueLine)")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(palette.soft)
            }
        }
        .paperSurface(palette, container: container)
    }

    private func miniBars(height: CGFloat, width: CGFloat, spacing: CGFloat) -> some View {
        HStack(alignment: .bottom, spacing: spacing) {
            ForEach(Array(snapshot.week.enumerated()), id: \.offset) { index, day in
                RoundedRectangle(cornerRadius: width / 2.5, style: .continuous)
                    .fill(barColor(index))
                    .frame(width: width, height: max(4, height * CGFloat(day.count) / CGFloat(maxCount)))
            }
        }
        .frame(height: height, alignment: .bottom)
    }

    private func barColor(_ index: Int) -> Color {
        index == snapshot.week.count - 1 ? palette.accent : palette.ink.opacity(0.78)
    }

    private func weekday(_ index: Int) -> String {
        ["S", "M", "T", "W", "T", "F", "S"][index % 7]
    }

    private var nextDueLine: String {
        snapshot.week.isEmpty ? "tomorrow" : "\(snapshot.week.last?.count ?? 0) tomorrow"
    }
}

// MARK: - Word

struct WordWidgetView: View {
    var snapshot: WidgetSnapshot
    var family: WidgetFamily
    var hour: Int
    var container: Bool

    private var palette: WidgetPalette { .of(DayPart(hour: hour)) }
    private var word: WidgetWord {
        snapshot.word ?? WidgetWord(character: "温", reading: "wēn", meaning: "warm; gentle")
    }
    private var prop: PetProp {
        WidgetCopy.wordPet(due: snapshot.due, newLeft: snapshot.newLeft, vacation: snapshot.vacation)
    }

    var body: some View {
        switch family {
        case .systemSmall:
            small
        case .accessoryRectangular:
            HStack(spacing: 8) {
                Text(word.character)
                    .font(.system(size: 26, weight: .semibold, design: .serif))
                VStack(alignment: .leading, spacing: 0) {
                    Text(word.reading).font(.system(size: 10, weight: .semibold))
                    Text(word.meaning).font(.system(size: 9)).lineLimit(1)
                }
            }
        case .accessoryCircular:
            ZStack {
                AccessoryWidgetBackground()
                Text(word.character)
                    .font(.system(size: 18, weight: .semibold, design: .serif))
            }
        default:
            medium
        }
    }

    private var small: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(alignment: .leading, spacing: 0) {
                Label(WidgetCopy.wordLabel(hour: hour), systemImage: "text.book.closed.fill")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(palette.soft)
                Spacer(minLength: 2)
                Text(word.character)
                    .font(.system(size: 44, weight: .semibold, design: .serif))
                    .foregroundStyle(palette.ink)
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
                Text(word.reading)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(palette.soft)
                Text(word.meaning)
                    .font(.system(size: 9.5))
                    .foregroundStyle(palette.soft)
                    .lineLimit(2)
                    .padding(.top, 1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.trailing, 34)

            PetView(prop: prop, accent: palette.accent)
                .frame(width: 38, height: 38)
                .offset(x: 2, y: 2)
        }
        .paperSurface(palette, container: container)
    }

    private var medium: some View {
        HStack(spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(palette.isDark ? Color.white.opacity(0.06) : Color.white.opacity(0.55))
                Text(word.character)
                    .font(.system(size: 58, weight: .semibold, design: .serif))
                    .foregroundStyle(palette.ink)
                    .minimumScaleFactor(0.5)
                    .lineLimit(1)
                    .padding(6)
            }
            .frame(width: 108, height: 108)

            VStack(alignment: .leading, spacing: 0) {
                Label(WidgetCopy.wordLabel(hour: hour), systemImage: "text.book.closed.fill")
                    .font(.system(size: 9.5, weight: .semibold))
                    .foregroundStyle(palette.soft)
                Text(word.reading)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(palette.ink)
                    .padding(.top, 4)
                Text(word.meaning)
                    .font(.system(size: 11))
                    .foregroundStyle(palette.soft)
                    .lineLimit(2)
                Spacer(minLength: 4)
                HStack(spacing: 6) {
                    chip(WidgetCopy.wordStatus(due: snapshot.due, newLeft: snapshot.newLeft, vacation: snapshot.vacation), palette.review)
                    chip(WidgetCopy.wordNote(hour: hour), palette.lesson)
                }
            }
            Spacer(minLength: 0)
            PetView(prop: prop, accent: palette.accent)
                .frame(width: 66, height: 66)
                .offset(y: 2)
        }
        .paperSurface(palette, container: container)
    }

    private func chip(_ text: String, _ tint: Color) -> some View {
        Text(text)
            .font(.system(size: 8.5, weight: .semibold))
            .foregroundStyle(tint)
            .lineLimit(1)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(Capsule().fill(tint.opacity(palette.isDark ? 0.16 : 0.12)))
    }
}
