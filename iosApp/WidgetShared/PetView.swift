import SwiftUI

/// The same ink pet as the app, drawn with SwiftUI shapes so a widget never
/// needs a raster avatar or an image loader. The proportions stay fixed; the
/// caller only chooses the size, the mood prop, and the accent.
struct PetView: View {
    var prop: PetProp = .hat
    var accent: Color = Color(hex: 0xF3C85A)
    var showBrush: Bool = true

    private let ink = Color(hex: 0x1F2B23)
    private let cream = Color(hex: 0xFFFDF4)
    private let gold = Color(hex: 0xC9973B)
    private let leaf = Color(hex: 0x2E7D5B)
    private let lilac = Color(hex: 0xA78BFA)
    private let coral = Color(hex: 0xE86A7A)
    private let blush = Color(hex: 0xF7B9B0)

    var body: some View {
        GeometryReader { proxy in
            let s = min(proxy.size.width, proxy.size.height)
            ZStack {
                Circle()
                    .stroke(accent.opacity(0.42), lineWidth: max(1, s * 0.045))
                    .frame(width: s * 0.96, height: s * 0.96)

                if prop == .monocle {
                    Circle()
                        .stroke(gold, lineWidth: max(1, s * 0.018))
                        .frame(width: s * 0.22, height: s * 0.22)
                        .offset(x: s * 0.13, y: -s * 0.03)
                }

                face(s)

                if prop == .monocle {
                    Path { path in
                        path.move(to: CGPoint(x: s * 0.74, y: s * 0.53))
                        path.addCurve(
                            to: CGPoint(x: s * 0.82, y: s * 0.80),
                            control1: CGPoint(x: s * 0.82, y: s * 0.62),
                            control2: CGPoint(x: s * 0.84, y: s * 0.72)
                        )
                    }
                    .stroke(gold, style: StrokeStyle(lineWidth: max(1, s * 0.012), lineCap: .round))
                }

                headwear(s)

                if showBrush {
                    brush(s)
                }
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .accessibilityHidden(true)
    }

    private func face(_ s: CGFloat) -> some View {
        ZStack {
            Circle()
                .fill(cream)
                .overlay(Circle().stroke(ink, lineWidth: max(1.2, s * 0.045)))
                .frame(width: s * 0.70, height: s * 0.70)

            // Leaf and stem.
            Ellipse()
                .fill(leaf)
                .frame(width: s * 0.17, height: s * 0.085)
                .rotationEffect(.degrees(-32))
                .offset(x: s * 0.035, y: -s * 0.385)
            Capsule()
                .fill(ink)
                .frame(width: max(1, s * 0.016), height: s * 0.055)
                .rotationEffect(.degrees(20))
                .offset(x: s * 0.005, y: -s * 0.335)

            // Eyes and blush.
            Circle().fill(ink)
                .frame(width: s * 0.070, height: s * 0.070)
                .offset(x: -s * 0.115, y: -s * 0.030)
            Circle().fill(ink)
                .frame(width: s * 0.070, height: s * 0.070)
                .offset(x: s * 0.115, y: -s * 0.030)

            Ellipse().fill(blush.opacity(0.95))
                .frame(width: s * 0.105, height: s * 0.055)
                .offset(x: -s * 0.180, y: s * 0.075)
            Ellipse().fill(blush.opacity(0.95))
                .frame(width: s * 0.105, height: s * 0.055)
                .offset(x: s * 0.180, y: s * 0.075)

            // Smile.
            Path { path in
                path.move(to: CGPoint(x: s * 0.44, y: s * 0.55))
                path.addQuadCurve(
                    to: CGPoint(x: s * 0.56, y: s * 0.55),
                    control: CGPoint(x: s * 0.50, y: s * 0.605)
                )
            }
            .stroke(ink, style: StrokeStyle(lineWidth: max(1.2, s * 0.028), lineCap: .round))
        }
    }

    @ViewBuilder
    private func headwear(_ s: CGFloat) -> some View {
        switch prop {
        case .hat:
            ZStack {
                RoundedRectangle(cornerRadius: s * 0.018)
                    .fill(ink)
                    .frame(width: s * 0.30, height: s * 0.045)
                    .offset(y: -s * 0.305)
                RoundedRectangle(cornerRadius: s * 0.018)
                    .fill(ink)
                    .frame(width: s * 0.16, height: s * 0.105)
                    .offset(y: -s * 0.370)
                Capsule()
                    .fill(gold)
                    .frame(width: max(1, s * 0.012), height: s * 0.11)
                    .offset(x: s * 0.075, y: -s * 0.365)
                Circle()
                    .fill(gold)
                    .frame(width: s * 0.035, height: s * 0.035)
                    .offset(x: s * 0.085, y: -s * 0.305)
            }
        case .nightcap:
            ZStack {
                Path { path in
                    path.move(to: CGPoint(x: s * 0.35, y: s * 0.37))
                    path.addQuadCurve(
                        to: CGPoint(x: s * 0.68, y: s * 0.30),
                        control: CGPoint(x: s * 0.44, y: s * 0.16)
                    )
                    path.addQuadCurve(
                        to: CGPoint(x: s * 0.68, y: s * 0.37),
                        control: CGPoint(x: s * 0.62, y: s * 0.35)
                    )
                    path.closeSubpath()
                }
                .fill(lilac)
                Circle().fill(cream)
                    .frame(width: s * 0.08, height: s * 0.08)
                    .offset(x: s * 0.20, y: -s * 0.205)
                Capsule().fill(cream.opacity(0.85))
                    .frame(width: s * 0.34, height: s * 0.045)
                    .offset(y: -s * 0.315)
            }
        case .party:
            ZStack {
                Path { path in
                    path.move(to: CGPoint(x: s * 0.40, y: s * 0.34))
                    path.addLine(to: CGPoint(x: s * 0.52, y: s * 0.13))
                    path.addLine(to: CGPoint(x: s * 0.62, y: s * 0.34))
                    path.closeSubpath()
                }
                .fill(coral)
                Circle().fill(gold)
                    .frame(width: s * 0.06, height: s * 0.06)
                    .offset(x: s * 0.02, y: -s * 0.205)
                Circle().fill(cream.opacity(0.9))
                    .frame(width: s * 0.035, height: s * 0.035)
                    .offset(x: s * 0.03, y: -s * 0.145)
            }
        case .witch:
            ZStack {
                Ellipse().fill(ink)
                    .frame(width: s * 0.38, height: s * 0.07)
                    .offset(y: -s * 0.315)
                Path { path in
                    path.move(to: CGPoint(x: s * 0.39, y: s * 0.32))
                    path.addQuadCurve(
                        to: CGPoint(x: s * 0.60, y: s * 0.32),
                        control: CGPoint(x: s * 0.57, y: s * 0.08)
                    )
                    path.closeSubpath()
                }
                .fill(ink)
                Circle().fill(gold)
                    .frame(width: s * 0.035, height: s * 0.035)
                    .offset(x: s * 0.02, y: -s * 0.345)
            }
        case .vampire:
            HStack(spacing: s * 0.055) {
                Fang().frame(width: s * 0.042, height: s * 0.070)
                Fang().frame(width: s * 0.042, height: s * 0.070)
            }
            .offset(y: s * 0.135)
        case .monocle:
            EmptyView()
        }
    }

    private func brush(_ s: CGFloat) -> some View {
        ZStack {
            Capsule()
                .fill(Color(hex: 0xE8C07A))
                .frame(width: s * 0.055, height: s * 0.34)
                .offset(x: s * 0.30, y: s * 0.02)
            RoundedRectangle(cornerRadius: s * 0.012)
                .fill(ink)
                .frame(width: s * 0.075, height: s * 0.085)
                .offset(x: s * 0.30, y: -s * 0.155)
            RoundedRectangle(cornerRadius: s * 0.008)
                .fill(cream)
                .frame(width: s * 0.062, height: s * 0.040)
                .offset(x: s * 0.30, y: -s * 0.112)
        }
        .rotationEffect(.degrees(16))
    }
}

private struct Fang: View {
    var body: some View {
        GeometryReader { proxy in
            Path { path in
                path.move(to: .zero)
                path.addLine(to: CGPoint(x: proxy.size.width, y: 0))
                path.addLine(to: CGPoint(x: proxy.size.width / 2, y: proxy.size.height))
                path.closeSubpath()
            }
            .fill(Color(hex: 0xFFFDF4))
        }
    }
}
