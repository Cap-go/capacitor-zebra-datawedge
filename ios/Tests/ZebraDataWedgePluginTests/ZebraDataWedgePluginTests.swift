import XCTest
@testable import ZebraDataWedgePlugin

final class ZebraDataWedgePluginTests: XCTestCase {
    func testUnavailableMessage() {
        let implementation = ZebraDataWedge()
        XCTAssertEqual(
            implementation.unavailableMessage(),
            "Zebra DataWedge is only available on Android devices with Zebra DataWedge installed."
        )
    }
}
