import XCTest

final class BihuaUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func launch(_ arguments: [String] = []) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-reset-settings", "-reset-progress"] + arguments
        app.launch()
        return app
    }

    private func element(_ app: XCUIApplication, _ label: String) -> XCUIElement {
        app.descendants(matching: .any)[label].firstMatch
    }

    /// iPhone exposes the native tab bar as `tabBars`; iPadOS 26 draws it as a
    /// top control. Accept either, then fall back to any element with the label.
    private func containing(_ app: XCUIApplication, _ label: String) -> XCUIElement {
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS %@", label))
            .firstMatch
    }

    private func tabButton(_ app: XCUIApplication, _ label: String) -> XCUIElement {
        let tab = app.tabBars.buttons[label]
        if tab.waitForExistence(timeout: 2) { return tab }
        let segmented = app.segmentedControls.buttons[label]
        if segmented.waitForExistence(timeout: 2) { return segmented }
        return app.buttons[label].firstMatch
    }

    func testHomeStartsAReviewAndGradesIt() {
        let app = launch()
        let start = element(app, "Learn 5 new")
        XCTAssertTrue(start.waitForExistence(timeout: 15))
        start.tap()

        XCTAssertTrue(app.staticTexts["Write from the cue"].waitForExistence(timeout: 10))
        let answer = element(app, "Show answer")
        XCTAssertTrue(answer.waitForExistence(timeout: 5))
        answer.tap()

        XCTAssertTrue(app.staticTexts["How did it feel?"].waitForExistence(timeout: 5))
        let good = element(app, "Good")
        XCTAssertTrue(good.waitForExistence(timeout: 5))
        good.tap()

        XCTAssertTrue(app.staticTexts["2 / 5"].waitForExistence(timeout: 5))

        // The grade is written to the progress file, not just to local state.
        app.terminate()
        app.launchArguments = []
        app.launch()
        XCTAssertTrue(app.staticTexts["4 new characters ready"].waitForExistence(timeout: 15))
    }

    func testTabsAndSettingsPersistACourseChoice() {
        let app = launch()
        let settings = tabButton(app, "Settings")
        XCTAssertTrue(settings.waitForExistence(timeout: 15))
        settings.tap()

        XCTAssertTrue(app.staticTexts["Appearance"].waitForExistence(timeout: 10))
        let japanese = element(app, "Japanese")
        XCTAssertTrue(japanese.waitForExistence(timeout: 5))
        japanese.tap()

        // The choice is stored, not just highlighted in this composition.
        app.terminate()
        app.launchArguments = []
        app.launch()
        tabButton(app, "Settings").tap()
        XCTAssertTrue(element(app, "Japanese").waitForExistence(timeout: 10))
    }

    func testCharactersPushADetailAndReturn() {
        let app = launch()
        let characters = tabButton(app, "Characters")
        XCTAssertTrue(characters.waitForExistence(timeout: 15))
        characters.tap()

        let first = element(app, "爱, ài, to love, Review")
        XCTAssertTrue(first.waitForExistence(timeout: 10))
        first.tap()

        XCTAssertTrue(app.staticTexts["Example words"].waitForExistence(timeout: 10))
        let back = app.navigationBars.buttons.firstMatch
        XCTAssertTrue(back.waitForExistence(timeout: 5))
        back.tap()
        XCTAssertTrue(element(app, "Search characters").waitForExistence(timeout: 10))
    }

    func testCharactersSearchFiltersTheGrid() {
        let app = launch()
        tabButton(app, "Characters").tap()
        let field = element(app, "Search characters")
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        // Compose exposes the field as a UITextView without a test label. Tap
        // the visible placeholder's frame and type into the focused field.
        let origin = app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
        origin.withOffset(CGVector(dx: field.frame.midX, dy: field.frame.midY)).tap()
        app.typeText("ai")

        XCTAssertTrue(element(app, "爱, ài, to love, Review").waitForExistence(timeout: 10))
        XCTAssertFalse(element(app, "吧, ba, particle indicating polite suggestion, Review").exists)
    }

    func testPetChoicePersistsAcrossLaunch() {
        let app = launch()
        tabButton(app, "Settings").tap()
        XCTAssertTrue(app.staticTexts["Appearance"].waitForExistence(timeout: 10))

        let reader = element(app, "Reader")
        XCTAssertTrue(reader.waitForExistence(timeout: 5))
        for _ in 0..<6 where !reader.isHittable {
            app.swipeUp()
        }
        XCTAssertTrue(reader.isHittable)
        reader.tap()
        let preview = app.staticTexts.containing(
            NSPredicate(format: "label CONTAINS %@", "Mò")
        ).firstMatch
        XCTAssertTrue(preview.waitForExistence(timeout: 5))

        app.terminate()
        app.launchArguments = []
        app.launch()
        tabButton(app, "Settings").tap()
        XCTAssertTrue(app.staticTexts["Appearance"].waitForExistence(timeout: 10))
        for _ in 0..<6 where !preview.exists {
            app.swipeUp()
        }
        XCTAssertTrue(preview.waitForExistence(timeout: 5))
    }

    func testLandscapeKeepsTheHomeActionVisible() {
        let app = launch()
        XCUIDevice.shared.orientation = .landscapeLeft
        defer { XCUIDevice.shared.orientation = .portrait }
        let start = element(app, "Start studying")
        XCTAssertTrue(start.waitForExistence(timeout: 15))
        XCTAssertTrue(start.isHittable)
    }

    func testDictionarySearchesTheOfflineWords() {
        let app = launch()
        tabButton(app, "Characters").tap()
        element(app, "Dictionary").tap()
        XCTAssertTrue(app.staticTexts["Words in Simplified"].waitForExistence(timeout: 10))
        let field = element(app, "Search a word, reading, or mean...")
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        let origin = app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
        origin.withOffset(CGVector(dx: field.frame.midX, dy: field.frame.midY)).tap()
        app.typeText("love")
        XCTAssertTrue(element(app, "爱情").waitForExistence(timeout: 10))
    }

    func testSettingsShowsThePracticeControls() {
        let app = launch()
        tabButton(app, "Settings").tap()
        XCTAssertTrue(app.staticTexts["Appearance"].waitForExistence(timeout: 10))
        for _ in 0..<3 {
            app.swipeUp()
        }
        XCTAssertTrue(app.staticTexts["Daily new lessons"].waitForExistence(timeout: 5))
        let write = containing(app, "Write the character")
        let mature = containing(app, "Mature examples")
        let reduce = containing(app, "Reduce motion")
        for _ in 0..<6 where !mature.exists {
            app.swipeUp()
        }
        XCTAssertTrue(write.exists)
        XCTAssertTrue(mature.exists)
        XCTAssertTrue(reduce.exists)
    }

    func testPracticeTabOpensTheFreePad() {
        let app = launch()
        tabButton(app, "Practice").tap()
        XCTAssertTrue(app.staticTexts["Free practice"].waitForExistence(timeout: 10))
        XCTAssertTrue(element(app, "Undo stroke").waitForExistence(timeout: 5))
        XCTAssertTrue(element(app, "Hide guide").waitForExistence(timeout: 5))
    }

    func testDarkModeKeepsTheNativeChromeReadable() {
        let app = launch(["-theme-dark"])
        let settings = tabButton(app, "Settings")
        XCTAssertTrue(settings.waitForExistence(timeout: 15))
        settings.tap()

        XCTAssertTrue(app.staticTexts["Appearance"].waitForExistence(timeout: 10))
        // The navigation title and the tab item are drawn by UIKit; a screenshot
        // is attached for the visual check that they are light on dark.
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.lifetime = .keepAlways
        add(shot)
    }
}
