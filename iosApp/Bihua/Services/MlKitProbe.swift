#if canImport(MLKitDigitalInkRecognition)
import Foundation
import MLKitDigitalInkRecognition

/// P0 device probe for the ML Kit digital-ink path. It checks the model, downloads
/// it once if needed, recognizes a synthetic horizontal stroke for 一, and reports
/// the top candidate. The real `InkRecognizer` seam arrives in P4.
enum MlKitProbe {
    private static let languageTags = ["zh-Hans", "zh-Hant", "ja", "ko", "en-US"]

    private static var recognizer: DigitalInkRecognizer?

    private static func model() -> (DigitalInkRecognitionModel, String)? {
        for tag in languageTags {
            if let identifier = try? DigitalInkRecognitionModelIdentifier.from(languageTag: tag) {
                return (DigitalInkRecognitionModel(modelIdentifier: identifier), tag)
            }
        }
        return nil
    }

    static func run(completion: @escaping (String) -> Void) {
        guard let (model, tag) = model() else {
            completion("ML Kit: no ink model")
            return
        }
        let manager = ModelManager.modelManager()

        let recognize = {
            let options = DigitalInkRecognizerOptions(model: model)
            // The recognizer must outlive the callback; a local reference is
            // released before ML Kit answers.
            let recognizer = DigitalInkRecognizer.digitalInkRecognizer(options: options)
            Self.recognizer = recognizer
            let stroke = Stroke(points: [
                StrokePoint(x: 20, y: 50, t: 0),
                StrokePoint(x: 90, y: 50, t: 120),
            ])
            let context = DigitalInkRecognitionContext(
                preContext: "",
                writingArea: WritingArea(width: 100, height: 100)
            )
            recognizer.recognize(ink: Ink(strokes: [stroke]), context: context) { result, error in
                Self.recognizer = nil
                if let text = result?.candidates.first?.text {
                    completion("ML Kit \(tag): \(text)")
                } else {
                    completion("ML Kit \(tag): \(error?.localizedDescription ?? "no candidate")")
                }
            }
        }

        if manager.isModelDownloaded(model) {
            recognize()
            return
        }

        completion("ML Kit \(tag): downloading")
        _ = manager.download(
            model,
            conditions: ModelDownloadConditions(
                allowsCellularAccess: true,
                allowsBackgroundDownloading: true
            )
        )
        var tries = 0
        func poll() {
            tries += 1
            if manager.isModelDownloaded(model) {
                recognize()
                return
            }
            if tries > 120 {
                completion("ML Kit \(tag): download timeout")
                return
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { poll() }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { poll() }
    }
}
#endif
