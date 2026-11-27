import UIKit

/// Draws the Progress report from the JSON the shared Kotlin layer writes. All
/// values are real local counts; the PDF leaves the app only through the share
/// sheet the learner opens.
enum ProgressPdfRenderer {
    static func make(from path: String) -> String? {
        guard
            let data = FileManager.default.contents(atPath: path),
            let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let title = root["title"] as? String,
            let course = root["course"] as? String,
            let generatedOn = root["generatedOn"] as? String,
            let stats = root["stats"] as? [[String: String]],
            let characters = root["characters"] as? [[String: String]]
        else { return nil }

        let page = CGRect(x: 0, y: 0, width: 612, height: 792)
        let renderer = UIGraphicsPDFRenderer(bounds: page)
        let target = FileManager.default.temporaryDirectory
            .appendingPathComponent("Bihua-Progress-\(generatedOn).pdf")
        do {
            try renderer.writePDF(to: target) { context in
                var y: CGFloat = 0
                let margin: CGFloat = 46
                let ink = UIColor(red: 0.12, green: 0.17, blue: 0.14, alpha: 1)
                let soft = UIColor(red: 0.30, green: 0.37, blue: 0.32, alpha: 1)

                func beginPage() {
                    context.beginPage()
                    y = margin
                }

                func draw(_ text: String, size: CGFloat, weight: UIFont.Weight = .regular, color: UIColor = ink) {
                    let font = UIFont.systemFont(ofSize: size, weight: weight)
                    let attributes: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color]
                    let height = (text as NSString).size(withAttributes: attributes).height
                    (text as NSString).draw(
                        in: CGRect(x: margin, y: y, width: page.width - margin * 2, height: height + 2),
                        withAttributes: attributes
                    )
                    y += height + 4
                }

                beginPage()
                draw("Bihua", size: 13, weight: .semibold, color: soft)
                draw(title, size: 28, weight: .bold)
                draw("\(course) · \(generatedOn)", size: 12, color: soft)
                y += 10

                for stat in stats {
                    let label = "\(stat["label"] ?? ""): "
                    let value = stat["value"] ?? ""
                    let font = UIFont.systemFont(ofSize: 12)
                    let bold = UIFont.systemFont(ofSize: 12, weight: .semibold)
                    let text = NSMutableAttributedString(string: label, attributes: [.font: font, .foregroundColor: soft])
                    text.append(NSAttributedString(string: value, attributes: [.font: bold, .foregroundColor: ink]))
                    text.draw(in: CGRect(x: margin, y: y, width: page.width - margin * 2, height: 18))
                    y += 18
                }

                y += 12
                draw("Characters in this course", size: 14, weight: .semibold)
                y += 2

                for entry in characters {
                    if y > page.height - 80 {
                        beginPage()
                        draw("Characters (continued)", size: 14, weight: .semibold)
                    }
                    let character = entry["character"] ?? ""
                    let reading = entry["reading"] ?? ""
                    let meaning = entry["meaning"] ?? ""
                    let status = entry["status"] ?? ""
                    let charAttributes: [NSAttributedString.Key: Any] = [
                        .font: UIFont.systemFont(ofSize: 18, weight: .medium),
                        .foregroundColor: ink,
                    ]
                    (character as NSString).draw(
                        in: CGRect(x: margin, y: y, width: 44, height: 24),
                        withAttributes: charAttributes
                    )
                    let rowAttributes: [NSAttributedString.Key: Any] = [
                        .font: UIFont.systemFont(ofSize: 10),
                        .foregroundColor: soft,
                    ]
                    ("\(reading) · \(meaning)" as NSString).draw(
                        in: CGRect(x: margin + 48, y: y + 1, width: page.width - margin * 2 - 140, height: 23),
                        withAttributes: rowAttributes
                    )
                    (status as NSString).draw(
                        in: CGRect(x: page.width - margin - 86, y: y + 1, width: 86, height: 23),
                        withAttributes: rowAttributes
                    )
                    y += 25
                }

                y = page.height - 34
                draw("Made on this device. Nothing was uploaded.", size: 9, color: soft)
            }
            return target.path
        } catch {
            return nil
        }
    }
}
