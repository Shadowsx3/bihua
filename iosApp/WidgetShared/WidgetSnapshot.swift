import Foundation
import SwiftUI

/// Wire contract with `SharedWidgetSnapshot.kt`. The app writes this document
/// to the App Group; the widget extension only reads it.
struct WidgetSnapshot: Codable {
    var version: Int
    var mode: String
    var due: Int
    var newLeft: Int
    var reviewsDone: Int
    var lessonsDone: Int
    var vacation: Bool
    var dayStartHour: Int
    var updatedAtEpochMs: Int64
    var petId: String
    var word: WidgetWord?
    var week: [WidgetDay]
    var hours: [WidgetHour]

    var reviewsTotal: Int { reviewsDone + due }
    var lessonsTotal: Int { lessonsDone + newLeft }
    var weekTotal: Int { week.reduce(0) { $0 + $1.count } }

    static let sample = WidgetSnapshot(
        version: 1,
        mode: "SIMPLIFIED",
        due: 4,
        newLeft: 2,
        reviewsDone: 2,
        lessonsDone: 0,
        vacation: false,
        dayStartHour: 4,
        updatedAtEpochMs: 0,
        petId: "pet-mo",
        word: WidgetWord(
            character: "温柔",
            reading: "wēnróu",
            meaning: "gentle and soft; tender"
        ),
        week: (0..<7).map { index in
            WidgetDay(date: "2026-11-\(20 + index)", count: [8, 5, 12, 7, 3, 9, 4][index])
        },
        hours: [
            WidgetHour(hour: 0, count: 1),
            WidgetHour(hour: 2, count: 1),
            WidgetHour(hour: 4, count: 2),
        ]
    )

    static let clear = WidgetSnapshot(
        version: 1,
        mode: "SIMPLIFIED",
        due: 0,
        newLeft: 0,
        reviewsDone: 6,
        lessonsDone: 2,
        vacation: false,
        dayStartHour: 4,
        updatedAtEpochMs: 0,
        petId: "pet-mo",
        word: WidgetWord(character: "安", reading: "ān", meaning: "peace; quiet"),
        week: (0..<7).map { index in
            WidgetDay(date: "2026-11-\(20 + index)", count: [4, 6, 3, 8, 5, 7, 2][index])
        },
        hours: []
    )
}

struct WidgetWord: Codable {
    var character: String
    var reading: String
    var meaning: String
}

struct WidgetDay: Codable, Identifiable {
    var date: String
    var count: Int
    var id: String { date }
}

struct WidgetHour: Codable {
    var hour: Int
    var count: Int
}

enum SnapshotStore {
    static let suite = "group.com.bassmd.bihua"
    static let key = "widget_snapshot"

    static func load() -> WidgetSnapshot {
        guard
            let defaults = UserDefaults(suiteName: suite),
            let text = defaults.string(forKey: key),
            let data = text.data(using: .utf8),
            let snapshot = try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
        else {
            return .sample
        }
        return snapshot
    }
}

enum DayPart: Equatable {
    case morning, afternoon, evening, night

    init(hour: Int) {
        switch hour {
        case 5...10: self = .morning
        case 11...16: self = .afternoon
        case 17...21: self = .evening
        default: self = .night
        }
    }

    var greeting: String {
        switch self {
        case .morning: "Good morning"
        case .afternoon: "Good afternoon"
        case .evening: "Good evening"
        case .night: "Good night"
        }
    }
}

enum PetProp {
    case hat, monocle, party, nightcap, witch, vampire
}

struct TodayCopy {
    var greeting: String
    var headline: String
    var detail: String
    var action: String
}

enum WidgetCopy {
    static func today(due: Int, newLeft: Int, vacation: Bool, hour: Int) -> TodayCopy {
        if vacation {
            return TodayCopy(
                greeting: "Vacation",
                headline: "Reviews are paused",
                detail: "The desk will wait.",
                action: "Open"
            )
        }
        let part = DayPart(hour: hour)
        let action = due > 0 || newLeft > 0 ? "Study" : "Open"
        let reviews = due == 1 ? "1 review waiting" : "\(due) reviews waiting"
        let lessons = newLeft == 0
            ? "No new lessons today."
            : (newLeft == 1 ? "1 new lesson." : "\(newLeft) new lessons.")

        switch part {
        case .morning, .afternoon:
            if due > 0 {
                return TodayCopy(greeting: part.greeting, headline: reviews, detail: lessons, action: action)
            }
            if newLeft > 0 {
                return TodayCopy(greeting: part.greeting, headline: "A fresh page", detail: lessons, action: action)
            }
            return TodayCopy(greeting: part.greeting, headline: "All clear", detail: part == .morning ? "A calm start." : "The desk is tidy.", action: action)
        case .evening:
            if due > 0 {
                return TodayCopy(greeting: part.greeting, headline: reviews, detail: "One quiet review.", action: action)
            }
            if newLeft > 0 {
                return TodayCopy(greeting: part.greeting, headline: "A new character is waiting", detail: "A gentle end to the day.", action: action)
            }
            return TodayCopy(greeting: part.greeting, headline: "All clear", detail: "Close the desk.", action: action)
        case .night:
            if due > 0 {
                return TodayCopy(greeting: part.greeting, headline: reviews, detail: "They will keep until morning.", action: action)
            }
            if newLeft > 0 {
                return TodayCopy(greeting: part.greeting, headline: "A new character is waiting", detail: "Tomorrow is fine.", action: action)
            }
            return TodayCopy(greeting: part.greeting, headline: "All clear", detail: "Rest is part of the work.", action: action)
        }
    }

    static func todayPet(due: Int, newLeft: Int, vacation: Bool, hour: Int) -> PetProp {
        if vacation { return .nightcap }
        if due == 0 && newLeft == 0 { return .party }
        switch DayPart(hour: hour) {
        case .morning, .afternoon: return .hat
        case .evening: return .witch
        case .night: return .vampire
        }
    }

    static func wordPet(due: Int, newLeft: Int, vacation: Bool) -> PetProp {
        if vacation { return .nightcap }
        if due == 0 && newLeft == 0 { return .party }
        return .monocle
    }

    static func wordStatus(due: Int, newLeft: Int, vacation: Bool) -> String {
        if vacation { return "Reviews paused" }
        if due > 0 && newLeft > 0 {
            return "\(due) \(due == 1 ? "review" : "reviews") · \(newLeft) \(newLeft == 1 ? "lesson" : "lessons")"
        }
        if due > 0 { return "\(due) \(due == 1 ? "review" : "reviews") waiting" }
        if newLeft > 0 { return "\(newLeft) new \(newLeft == 1 ? "lesson" : "lessons")" }
        return "All clear"
    }

    static func wordLabel(hour: Int) -> String { "Word of the day" }

    static func wordNote(hour: Int) -> String {
        switch DayPart(hour: hour) {
        case .morning: "A gentle start."
        case .afternoon: "A page for the afternoon."
        case .evening: "A word before rest."
        case .night: "A word for quiet hours."
        }
    }

    static func forecast(_ snapshot: WidgetSnapshot, hourly: Bool) -> String? {
        if snapshot.vacation { return nil }
        if hourly {
            guard !snapshot.hours.isEmpty else { return nil }
            let shown = snapshot.hours.prefix(2)
            let rest = max(0, snapshot.due - shown.reduce(0) { $0 + $1.count })
            var parts = shown.map { slot in
                slot.hour <= 1 ? "\(slot.count) now" : "\(slot.count) at \(slot.hour)h"
            }
            if rest > 0 { parts.append("\(rest) later") }
            return parts.joined(separator: " · ")
        }
        return "Next 7 days"
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }
}

struct WidgetPalette {
    var top: Color
    var bottom: Color
    var ink: Color
    var soft: Color
    var review: Color
    var lesson: Color
    var accent: Color
    var nut: Color
    var isDark: Bool

    var background: LinearGradient {
        LinearGradient(colors: [top, bottom], startPoint: .topLeading, endPoint: .bottomTrailing)
    }

    static func of(_ part: DayPart) -> WidgetPalette {
        switch part {
        case .morning:
            return WidgetPalette(
                top: Color(hex: 0xFFE4C2), bottom: Color(hex: 0xFFF7E5),
                ink: Color(hex: 0x1F2B23), soft: Color(hex: 0x4C5F51),
                review: Color(hex: 0xA83E56), lesson: Color(hex: 0x2E7D5B),
                accent: Color(hex: 0xF3C85A), nut: Color(hex: 0xFFD39B), isDark: false
            )
        case .afternoon:
            return WidgetPalette(
                top: Color(hex: 0xD8F5E2), bottom: Color(hex: 0xF7FFF9),
                ink: Color(hex: 0x1F2B23), soft: Color(hex: 0x4C5F51),
                review: Color(hex: 0xA83E56), lesson: Color(hex: 0x2E7D5B),
                accent: Color(hex: 0x57DFA0), nut: Color(hex: 0x9FE3BD), isDark: false
            )
        case .evening:
            return WidgetPalette(
                top: Color(hex: 0xE7D7FF), bottom: Color(hex: 0xFFF0DD),
                ink: Color(hex: 0x1F2B23), soft: Color(hex: 0x4C5F51),
                review: Color(hex: 0xA83E56), lesson: Color(hex: 0x2E7D5B),
                accent: Color(hex: 0xA78BFA), nut: Color(hex: 0xCDB7F0), isDark: false
            )
        case .night:
            return WidgetPalette(
                top: Color(hex: 0x0E1913), bottom: Color(hex: 0x24382C),
                ink: Color(hex: 0xF7F2E6), soft: Color(hex: 0xC8D6CC),
                review: Color(hex: 0xF3A7B8), lesson: Color(hex: 0x9CE7C6),
                accent: Color(hex: 0x0389C1), nut: Color(hex: 0x3E5B49), isDark: true
            )
        }
    }
}
