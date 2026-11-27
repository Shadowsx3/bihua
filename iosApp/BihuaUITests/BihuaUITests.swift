import XCTest

final class BihuaUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    private func launch(_ arguments: [String] = []) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-reset-settings", "-reset-progress", "-skip-onboarding"] + arguments
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

    func testFirstRunOnboardingReachesHome() {
        let app = XCUIApplication()
        app.launchArguments = ["-reset-settings", "-reset-progress"]
        app.launch()
        XCTAssertTrue(app.staticTexts["Write it. Keep it."].waitForExistence(timeout: 15))
        for _ in 0..<3 {
            app.buttons["Continue"].tap()
        }
        let start = app.buttons["Start studying"]
        XCTAssertTrue(start.waitForExistence(timeout: 5))
        start.tap()
        XCTAssertTrue(element(app, "Learn 5 new").waitForExistence(timeout: 15))
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

        let first = element(app, "爱, ài, to love, New")
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

        XCTAssertTrue(element(app, "爱, ài, to love, New").waitForExistence(timeout: 10))
        XCTAssertFalse(element(app, "吧, ba, particle indicating polite suggestion, New").exists)
    }

    func testPetChoicePersistsAcrossLaunch() {
        let app = launch()
        tabButton(app, "Settings").tap()
        XCTAssertTrue(app.staticTexts["Appearance"].waitForExistence(timeout: 10))

        let reader = containing(app, "Reader")
        for _ in 0..<8 where !reader.isHittable {
            app.swipeUp()
        }
        XCTAssertTrue(reader.waitForExistence(timeout: 5))
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
        let start = element(app, "Learn 5 new")
        XCTAssertTrue(start.waitForExistence(timeout: 15))
        XCTAssertTrue(start.isHittable)
    }

    func testSwipeBackFromReview() {
        let app = launch()
        let learn = element(app, "Learn 5 new")
        XCTAssertTrue(learn.waitForExistence(timeout: 15))
        learn.tap()
        XCTAssertTrue(app.staticTexts["Write from the cue"].waitForExistence(timeout: 10))
        let edge = app.coordinate(withNormalizedOffset: CGVector(dx: 0.01, dy: 0.5))
        let target = app.coordinate(withNormalizedOffset: CGVector(dx: 0.85, dy: 0.5))
        edge.press(forDuration: 0.05, thenDragTo: target)
        XCTAssertTrue(element(app, "Learn 5 new").waitForExistence(timeout: 10))
    }

    func testSwipeBackFromCharacterDetail() {
        let app = launch()
        tabButton(app, "Characters").tap()
        let first = element(app, "爱, ài, to love, New")
        XCTAssertTrue(first.waitForExistence(timeout: 10))
        first.tap()
        XCTAssertTrue(app.staticTexts["Example words"].waitForExistence(timeout: 10))

        let edge = app.coordinate(withNormalizedOffset: CGVector(dx: 0.01, dy: 0.5))
        let target = app.coordinate(withNormalizedOffset: CGVector(dx: 0.85, dy: 0.5))
        edge.press(forDuration: 0.05, thenDragTo: target)
        XCTAssertTrue(element(app, "Search characters").waitForExistence(timeout: 10))
    }

    func testWordOfTheDayOpensWordPractice() {
        let app = launch()
        let write = element(app, "Write")
        XCTAssertTrue(write.waitForExistence(timeout: 15))
        write.tap()
        XCTAssertTrue(app.staticTexts["Whole word · not scheduled"].waitForExistence(timeout: 10))
    }

    func testDictionarySearchesTheOfflineWords() {
        let app = launch()
        tabButton(app, "Characters").tap()
        element(app, "Dictionary").tap()
        XCTAssertTrue(app.staticTexts["Words in Simplified"].waitForExistence(timeout: 10))
        let field = containing(app, "Search a word")
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        let origin = app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
        origin.withOffset(CGVector(dx: field.frame.midX, dy: field.frame.midY)).tap()
        app.typeText("love")
        XCTAssertTrue(element(app, "爱情").waitForExistence(timeout: 10))
    }

    func testStrokeOrderGuidePlays() {
        let app = launch(["-push", "stroke/SIMPLIFIED:U+7231"])
        XCTAssertTrue(app.buttons["Play again"].waitForExistence(timeout: 15))
        XCTAssertTrue(containing(app, "Stroke").waitForExistence(timeout: 5))
    }

    func testStartOverClearsProgress() {
        let app = launch()
        element(app, "Learn 5 new").tap()
        XCTAssertTrue(app.staticTexts["Write from the cue"].waitForExistence(timeout: 10))
        element(app, "Show answer").tap()
        element(app, "Good").tap()
        XCTAssertTrue(app.staticTexts["2 / 5"].waitForExistence(timeout: 5))
        app.navigationBars.buttons.firstMatch.tap()

        tabButton(app, "Settings").tap()
        XCTAssertTrue(app.staticTexts["Appearance"].waitForExistence(timeout: 10))
        let startOver = app.buttons["Start over"]
        for _ in 0..<8 where !startOver.isHittable {
            app.swipeUp()
        }
        XCTAssertTrue(startOver.isHittable)
        usleep(500_000)
        startOver.tap()
        let confirm = app.buttons["Confirm start over"]
        XCTAssertTrue(confirm.waitForExistence(timeout: 5))
        confirm.tap()

        tabButton(app, "Home").tap()
        XCTAssertTrue(element(app, "Learn 5 new").waitForExistence(timeout: 10))
    }

    func testStudyListCanBeCreatedAndFilled() {
        let app = launch(["-push", "lists"])
        XCTAssertTrue(app.buttons["New list"].waitForExistence(timeout: 15))
        app.buttons["New list"].tap()
        XCTAssertTrue(app.staticTexts["List name"].waitForExistence(timeout: 10))

        let addField = containing(app, "Add a character")
        XCTAssertTrue(addField.waitForExistence(timeout: 5))
        let origin = app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
        origin.withOffset(CGVector(dx: addField.frame.midX, dy: addField.frame.midY)).tap()
        app.typeText("ai")
        let add = app.buttons["Add"].firstMatch
        XCTAssertTrue(add.waitForExistence(timeout: 10))
        add.tap()
        XCTAssertTrue(app.staticTexts["1 character"].waitForExistence(timeout: 10))
    }

    func testBackupExportOpensTheShareSheet() {
        let app = launch()
        tabButton(app, "Settings").tap()
        XCTAssertTrue(app.staticTexts["Appearance"].waitForExistence(timeout: 10))
        let export = app.buttons["Export backup"]
        for _ in 0..<8 where !export.isHittable {
            app.swipeUp()
        }
        XCTAssertTrue(export.isHittable)
        usleep(500_000)
        export.tap()
        sleep(2)
        let sheet = app.sheets.firstMatch
        let activity = app.otherElements["ActivityListView"]
        XCTAssertTrue(sheet.waitForExistence(timeout: 10) || activity.waitForExistence(timeout: 2))
    }

    func testBackupImportOpensTheFilePicker() {
        let app = launch()
        tabButton(app, "Settings").tap()
        XCTAssertTrue(app.staticTexts["Appearance"].waitForExistence(timeout: 10))
        let importButton = app.buttons["Import backup"]
        for _ in 0..<8 where !importButton.isHittable {
            app.swipeUp()
        }
        XCTAssertTrue(importButton.isHittable)
        usleep(500_000)
        importButton.tap()
        XCTAssertTrue(app.staticTexts["Recents"].waitForExistence(timeout: 10))
        app.buttons["Cancel"].tap()
    }

    func testSettingsShowsThePracticeControls() {
        let app = launch()
        tabButton(app, "Settings").tap()
        XCTAssertTrue(app.staticTexts["Appearance"].waitForExistence(timeout: 10))
        for _ in 0..<3 {
            app.swipeUp()
        }
        XCTAssertTrue(app.staticTexts["Daily new lessons"].waitForExistence(timeout: 5))
        
        let mature = containing(app, "Mature examples")
        let reduce = containing(app, "Reduce motion")
        for _ in 0..<6 where !mature.exists {
            app.swipeUp()
        }
        XCTAssertTrue(containing(app, "Read aloud").exists)
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
