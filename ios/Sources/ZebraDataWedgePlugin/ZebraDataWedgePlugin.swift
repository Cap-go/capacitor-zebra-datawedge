import Capacitor
import Foundation

@objc(ZebraDataWedgePlugin)
public class ZebraDataWedgePlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "ZebraDataWedgePlugin"
    public let jsName = "ZebraDataWedge"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "cloneProfile", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createProfile", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteProfile", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "importConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "renameProfile", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "restoreConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setDisabledAppList", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setIgnoreDisabledProfiles", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "registerForNotification", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "unRegisterForNotification", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "enumerateScanners", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getActiveProfile", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getAssociatedApps", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getDatawedgeStatus", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getDisabledAppList", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getIgnoreDisabledProfiles", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getProfilesList", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getScannerStatus", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getVersionInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "disableDatawedge", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "disableScannerInput", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "enableDatawedge", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "enableScannerInput", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "enumerateTriggers", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "notify", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "resetDefaultProfile", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setDefaultProfile", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setReportingOptions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "softRfidTrigger", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "softScanTrigger", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "switchScanner", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "switchScannerParams", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "switchToProfile", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    private let implementation = ZebraDataWedge()

    private func rejectUnavailable(_ call: CAPPluginCall) {
        call.reject(implementation.unavailableMessage())
    }

    @objc public func cloneProfile(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func createProfile(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func deleteProfile(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func importConfig(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func renameProfile(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func restoreConfig(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func setConfig(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func setDisabledAppList(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func setIgnoreDisabledProfiles(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func registerForNotification(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func unRegisterForNotification(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func enumerateScanners(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func getActiveProfile(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func getAssociatedApps(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func getConfig(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func getDatawedgeStatus(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func getDisabledAppList(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func getIgnoreDisabledProfiles(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func getProfilesList(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func getScannerStatus(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func getVersionInfo(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func disableDatawedge(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func disableScannerInput(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func enableDatawedge(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func enableScannerInput(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func enumerateTriggers(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func notify(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func resetDefaultProfile(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func setDefaultProfile(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func setReportingOptions(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func softRfidTrigger(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func softScanTrigger(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func switchScanner(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func switchScannerParams(_ call: CAPPluginCall) { rejectUnavailable(call) }
    @objc public func switchToProfile(_ call: CAPPluginCall) { rejectUnavailable(call) }

    @objc public func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": "ios"])
    }
}
