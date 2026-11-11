import type { Plugin, PluginListenerHandle } from '@capacitor/core';

export interface PluginVersionResult {
  version: string;
}

export enum DataWedgeConfigMode {
  CREATE_IF_NOT_EXIST = 'CREATE_IF_NOT_EXIST',
  OVERWRITE = 'OVERWRITE',
  REMOVE = 'REMOVE',
  UPDATE = 'UPDATE',
}

export enum DataWedgeNotificationType {
  CONFIGURATION_UPDATE = 'CONFIGURATION_UPDATE',
  PROFILE_SWITCH = 'PROFILE_SWITCH',
  SCANNER_STATUS = 'SCANNER_STATUS',
}

export enum DataWedgePlugin {
  ADF = 'ADF',
  BARCODE = 'BARCODE',
  BDF = 'BDF',
  DCP = 'DCP',
  EKB = 'EKB',
  INTENT = 'INTENT',
  IP = 'IP',
  KEYSTROKE = 'KEYSTROKE',
  MSR = 'MSR',
  RFID = 'RFID',
  SERIAL = 'SERIAL',
  TOKENS = 'TOKENS',
  VOICE = 'VOICE',
}

export enum ScannerIdentifier {
  AUTO = 'AUTO',
  BLUETOOTH_DS3678 = 'BLUETOOTH_DS3678',
  BLUETOOTH_RS6000 = 'BLUETOOTH_RS6000',
  BLUETOOTH_SSI = 'BLUETOOTH_SSI',
  INTERNAL_CAMERA = 'INTERNAL_CAMERA',
  INTERNAL_IMAGER = 'INTERNAL_IMAGER',
  INTERNAL_LASER = 'INTERNAL_LASER',
  PLUGABLE_SSI = 'PLUGABLE_SSI',
  PLUGABLE_SSI_RS5000 = 'PLUGABLE_SSI_RS5000',
  SERIAL_SSI = 'SERIAL_SSI',
  USB_SSI_DS3608 = 'USB_SSI_DS3608',
}

export enum ScannerStatus {
  CONNECTED = 'CONNECTED',
  DISABLED = 'DISABLED',
  DISCONNECTED = 'DISCONNECTED',
  IDLE = 'IDLE',
  SCANNING = 'SCANNING',
  WAITING = 'WAITING',
}

export enum ZebraErrorCodes {
  APP_ALREADY_ASSOCIATED = 'APP_ALREADY_ASSOCIATED',
  APP_ALREADY_IN_DISABLED_LIST = 'APP_ALREADY_IN_DISABLED_LIST',
  BUNDLE_EMPTY = 'BUNDLE_EMPTY',
  CANNOT_READ_FILE = 'CANNOT_READ_FILE',
  CONFIG_FILE_NOT_EXIST = 'CONFIG_FILE_NOT_EXIST',
  DATAWEDGE_ALREADY_DISABLED = 'DATAWEDGE_ALREADY_DISABLED',
  DATAWEDGE_ALREADY_ENABLED = 'DATAWEDGE_ALREADY_ENABLED',
  DATAWEDGE_DISABLED = 'DATAWEDGE_DISABLED',
  DEVICE_NOT_CONNECTED = 'DEVICE_NOT_CONNECTED',
  DEVICE_NOT_SUPPORTED = 'DEVICE_NOT_SUPPORTED',
  INPUT_NOT_ENABLED = 'INPUT_NOT_ENABLED',
  INVALID_CONFIG_FILE = 'INVALID_CONFIG_FILE',
  INVALID_FILE_NAME = 'INVALID_FILE_NAME',
  INVALID_FOLDER_PATH = 'INVALID_FOLDER_PATH',
  OPERATION_NOT_ALLOWED = 'OPERATION_NOT_ALLOWED',
  PARAMETER_INVALID = 'PARAMETER_INVALID',
  PLUGIN_BUNDLE_INVALID = 'PLUGIN_BUNDLE_INVALID',
  PLUGIN_DISABLED = 'PLUGIN_DISABLED',
  PLUGIN_DISABLED_IN_CONFIG = 'PLUGIN_DISABLED_IN_CONFIG',
  PLUGIN_NOT_SUPPORTED = 'PLUGIN_NOT_SUPPORTED',
  PROFILE_ALREADY_EXISTS = 'PROFILE_ALREADY_EXISTS',
  PROFILE_ALREADY_SET = 'PROFILE_ALREADY_SET',
  PROFILE_DISABLED = 'PROFILE_DISABLED',
  PROFILE_HAS_APP_ASSOCIATION = 'PROFILE_HAS_APP_ASSOCIATION',
  PROFILE_NAME_EMPTY = 'PROFILE_NAME_EMPTY',
  PROFILE_NOT_FOUND = 'PROFILE_NOT_FOUND',
  SCANNER_ALREADY_DISABLED = 'SCANNER_ALREADY_DISABLED',
  SCANNER_ALREADY_ENABLED = 'SCANNER_ALREADY_ENABLED',
  SCANNER_DISABLE_FAILED = 'SCANNER_DISABLE_FAILED',
  SCANNER_ENABLE_FAILED = 'SCANNER_ENABLE_FAILED',
  UNKNOWN = 'UNKNOWN',
}

export type DataWedgeScalar = string | number | boolean | null;
export type DataWedgeParameterValue =
  | DataWedgeScalar
  | DataWedgeScalar[]
  | DataWedgePluginParameters
  | DataWedgePluginParameters[];

export interface DataWedgePluginParameters {
  [key: string]: DataWedgeParameterValue;
}

export interface CloneProfileOptions {
  destProfileName: string;
  sourceProfileName: string;
}

export interface CreateProfileOptions {
  profileName: string;
}

export interface DataWedgeAppConfig {
  activityList: string[];
  packageName: string;
}

export interface DataWedgeNotifyOptions {
  deviceIdentifier: string;
  notificationSettings: number[];
}

export interface DataWedgePluginConfig {
  outputPluginName?: DataWedgePlugin;
  paramList: DataWedgePluginParameters;
  pluginName: DataWedgePlugin;
  resetConfig?: boolean;
}

export interface DeleteProfileOptions {
  profileNames: string[];
}

export interface PluginConfigOptions {
  outputPluginName?: DataWedgePlugin;
  pluginName: DataWedgePlugin;
}

export interface GetConfigOptions {
  pluginConfigs?: PluginConfigOptions[];
  profileName: string;
}

export interface ImportConfigOptions {
  fileList?: string[];
  folderPath: string;
}

export interface RegisterForNotificationOptions {
  appName?: string;
  callback?: (notification: ZebraNotificationEvent) => void;
  intentAction?: string;
  notificationType: DataWedgeNotificationType;
}

export interface RegisterScanIntentActionOptions {
  /** DataWedge intent action used for barcode scan broadcasts (physical trigger or soft scan). */
  intentAction: string;
}

export interface RenameProfileOptions {
  currentProfileName: string;
  newProfileName: string;
}

export interface ReportingOptions {
  reportingEnabled: boolean;
  reportingGenerateOption?: 'manual' | 'auto' | 'both';
  reportingShowForManualImport?: boolean;
}

export interface SetConfigOptions {
  /** Required for profiles enrolled in an active opt-in native scan session. */
  sessionId?: string;
  appList?: DataWedgeAppConfig[];
  configMode?: DataWedgeConfigMode;
  pluginConfigs?: DataWedgePluginConfig[];
  profileEnabled?: boolean;
  profileName: string;
  /**
   * Registers this intent action in the plugin receiver after a successful `setConfig` (runtime only;
   * call again on every app start). `INTENT` entries in `pluginConfigs` also register their
   * `intent_action` values automatically after a successful `setConfig`, whether or not
   * `scanIntentAction` is supplied.
   */
  scanIntentAction?: string;
}

export interface SetDisabledAppListOptions {
  appList?: DataWedgeAppConfig[];
  configMode: DataWedgeConfigMode;
}

export interface SwitchScannerParamsOptions {
  scannerIdentifier: ScannerIdentifier;
  scannerParams: DataWedgePluginParameters;
}

export interface ZebraDeviceScanner {
  connectionState: boolean;
  id?: string;
  index: number;
  name: string;
}

export interface ZebraDeviceVersionInfo {
  barcodeVersion?: string;
  dataWedgeVersion?: string;
  decoderVersion?: string;
  scannerFirmwareVersions?: string[];
  simulScanVersion?: string;
}

export interface ZebraError {
  code: ZebraErrorCodes | string;
  message: string;
}

export interface ZebraProfileConfiguration {
  appList?: DataWedgeAppConfig[];
  pluginConfigList: DataWedgePluginConfig[];
  profileEnabled: boolean;
  profileName: string;
}

export interface ZebraScanResult {
  data: string;
  decodedMode?: string;
  intentAction?: string;
  labelType?: string;
  source?: string;
}

export interface ZebraNotificationEvent {
  notificationType: string;
  profileName?: string;
  scannerStatus?: ScannerStatus | string;
  status?: string;
  raw: Record<string, unknown>;
}

export interface DataWedgeStatusResult {
  isEnabled: boolean;
}

export interface IgnoreDisabledProfilesResult {
  enabled: boolean;
}

export interface BeginScanSessionOptions {
  /** Existing profile associated exclusively with this application. No naming suffix is required. */
  profileName: string;
  /** Absolute session duration, 1..600000 ms. Default: 300000 (five minutes). Renewal never extends it. */
  timeoutMs?: number;
  /** Native liveness deadline, 2000..60000 ms. Default: 10000. Starts after association validation. */
  leaseTimeoutMs?: number;
  /** Advanced use only: disable the built-in JS heartbeat and call renewScanSession yourself. Default: true. */
  autoRenew?: boolean;
}

export interface ScanSession {
  /** Snapshot of the associations validated by begin; avoids a redundant getConfig query. */
  appList: DataWedgeAppConfig[];
  sessionId: string;
  profileName: string;
  /** Configure this action in the profile's INTENT output before triggering. */
  intentAction: string;
  timeoutMs: number;
  leaseTimeoutMs: number;
}

export interface ScanSessionState {
  sessionId: string;
  active: boolean;
  /** Present after closure; does not mean cleanup was acknowledged by DataWedge. */
  reason?: 'ended' | 'background' | 'destroyed' | 'heartbeat_expired' | 'max_duration';
  /** Milliseconds until the earlier native deadline; zero when closing. */
  remainingMs: number;
}

export interface ZebraDataWedgePlugin extends Plugin {
  /**
   * Read native session API capabilities without sending a DataWedge command.
   * Currently returns version 2 with customProfileNames, renewableLease and sessionAssociationDetails
   * set to true. The version and capability flags identify supported behavior and allow applications
   * to detect future session API changes. Future versions and capabilities will be documented.
   */
  getScanSessionCapabilities(): Promise<{
    version: number;
    customProfileNames: boolean;
    renewableLease: boolean;
    sessionAssociationDetails: boolean;
  }>;
  /**
   * Validate an existing app-owned profile and acquire an opt-in exclusive native session.
   * Automatically renews the native liveness lease from JS until end, native closure, page hiding,
   * renewal failure or the absolute timeout. Application completion/cancellation must still call end.
   * Returns the validated appList snapshot; it is not a cache or permission for later profile mutations.
   * Native begin failures leave no allocated session. Background begin rejects SESSION_INACTIVE;
   * another reserved session rejects SESSION_BUSY; an unassociated profile rejects SESSION_UNASSOCIATED.
   * If JS cancellation races successful allocation, rejection includes sessionId and cleanupConfirmed for cleanup retry.
   * Heartbeat handling requires no framework lifecycle integration. autoRenew: false is for experts.
   * The caller configures INTENT output/decoders and verifies scanner readiness. No profile is created.
   * Profiles enrolled in sessions require the matching sessionId on subsequent setConfig calls during
   * this process. Use session APIs to trigger or end scanning and avoid changing the active session's
   * profile through other low-level APIs.
   */
  beginScanSession(options: BeginScanSessionOptions): Promise<ScanSession>;
  /** Advanced manual renewal; normally called internally. Cannot revive an expired/closing session. */
  renewScanSession(options: { sessionId: string }): Promise<ScanSessionState>;
  /** Inspect the matching session, including closure reason. Does not renew or confirm cleanup. */
  getScanSessionState(options: { sessionId: string }): Promise<ScanSessionState>;
  /** Verify the active profile, then dispatch START_SCANNING for a foreground session. Scan values arrive via the scan event. */
  triggerScanSession(options: { sessionId: string }): Promise<void>;
  /**
   * Cancel owned pending calls, remove the action and await targeted Barcode Input disable.
   * Resolves only after DataWedge acknowledges cleanup. A failed/unanswered end is retryable and
   * blocks new sessions; do not start a camera scanner until confirmed. Concurrent end calls reject
   * SESSION_BUSY; repeating a completed end is idempotent. Pause/deadline cleanup is best effort and
   * still requires explicit end acknowledgement. No persistent process-death recovery is enabled.
   */
  endScanSession(options: { sessionId: string }): Promise<{ confirmed: boolean }>;
  cloneProfile(options: CloneProfileOptions): Promise<void>;
  createProfile(options: CreateProfileOptions): Promise<void>;
  deleteProfile(options: DeleteProfileOptions): Promise<void>;
  importConfig(options: ImportConfigOptions): Promise<void>;
  renameProfile(options: RenameProfileOptions): Promise<void>;
  restoreConfig(): Promise<void>;
  setConfig(options: SetConfigOptions): Promise<void>;
  setDisabledAppList(options: SetDisabledAppListOptions): Promise<void>;
  setIgnoreDisabledProfiles(options: IgnoreDisabledProfilesResult): Promise<void>;
  /**
   * Completes when the notification command is dispatched to DataWedge.
   * This does not confirm registration, policy approval, or scanner readiness;
   * observe notification events to determine the scanner's state.
   */
  registerForNotification(options: Omit<RegisterForNotificationOptions, 'callback'>): Promise<void>;
  /**
   * Registers a DataWedge scan broadcast intent action in the Android plugin receiver (runtime only;
   * call again on every app start). Use when your profile uses a custom `intent_action` and scans
   * come from the physical trigger.
   */
  registerScanIntentAction(options: RegisterScanIntentActionOptions): Promise<void>;
  /**
   * Completes when the unregistration command is dispatched to DataWedge.
   * This does not confirm unregistration or policy approval; notifications may already be queued.
   * Dispose of local listeners separately when using the low-level API.
   */
  unRegisterForNotification(options: Omit<RegisterForNotificationOptions, 'callback'>): Promise<void>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. When sender
   * identity is unavailable (Android API 24 to 33, or API 34+ when the platform withholds it),
   * matching identifier-free replies are accepted for compatibility. If a call times out, a delayed
   * identifier-free reply can still match a later sequential call that expects the same shape and
   * resolve it with stale data. This default may change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  enumerateScanners(): Promise<{ scanners: ZebraDeviceScanner[] }>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. When sender
   * identity is unavailable (Android API 24 to 33, or API 34+ when the platform withholds it),
   * matching identifier-free replies are accepted for compatibility. If a call times out, a delayed
   * identifier-free reply can still match a later sequential call that expects the same shape and
   * resolve it with stale data. This default may change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  getActiveProfile(): Promise<{ profileName: string }>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. For example,
   * `getAssociatedApps()` and `getConfig()` both send `CMD_GET_CONFIG` and expect `CONFIG_RESULTS`, so one
   * pending call to each overlaps. When sender identity is unavailable (Android API 24 to 33, or API
   * 34+ when the platform withholds it), matching identifier-free replies are accepted for
   * compatibility. If a call times out, a delayed identifier-free reply can still match a later
   * sequential call that expects the same shape and resolve it with stale data. This default may
   * change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  getAssociatedApps(options: { profileName: string }): Promise<{ appList: DataWedgeAppConfig[] }>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. For example,
   * `getAssociatedApps()` and `getConfig()` both send `CMD_GET_CONFIG` and expect `CONFIG_RESULTS`, so one
   * pending call to each overlaps. When sender identity is unavailable (Android API 24 to 33, or API
   * 34+ when the platform withholds it), matching identifier-free replies are accepted for
   * compatibility. If a call times out, a delayed identifier-free reply can still match a later
   * sequential call that expects the same shape and resolve it with stale data. This default may
   * change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  getConfig(options: GetConfigOptions): Promise<ZebraProfileConfiguration>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. When sender
   * identity is unavailable (Android API 24 to 33, or API 34+ when the platform withholds it),
   * matching identifier-free replies are accepted for compatibility. If a call times out, a delayed
   * identifier-free reply can still match a later sequential call that expects the same shape and
   * resolve it with stale data. This default may change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  getDatawedgeStatus(): Promise<DataWedgeStatusResult>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. When sender
   * identity is unavailable (Android API 24 to 33, or API 34+ when the platform withholds it),
   * matching identifier-free replies are accepted for compatibility. If a call times out, a delayed
   * identifier-free reply can still match a later sequential call that expects the same shape and
   * resolve it with stale data. This default may change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  getDisabledAppList(): Promise<{ appList: DataWedgeAppConfig[] }>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. When sender
   * identity is unavailable (Android API 24 to 33, or API 34+ when the platform withholds it),
   * matching identifier-free replies are accepted for compatibility. If a call times out, a delayed
   * identifier-free reply can still match a later sequential call that expects the same shape and
   * resolve it with stale data. This default may change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  getIgnoreDisabledProfiles(): Promise<IgnoreDisabledProfilesResult>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. When sender
   * identity is unavailable (Android API 24 to 33, or API 34+ when the platform withholds it),
   * matching identifier-free replies are accepted for compatibility. If a call times out, a delayed
   * identifier-free reply can still match a later sequential call that expects the same shape and
   * resolve it with stale data. This default may change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  getProfilesList(): Promise<{ profiles: string[] }>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. When sender
   * identity is unavailable (Android API 24 to 33, or API 34+ when the platform withholds it),
   * matching identifier-free replies are accepted for compatibility. If a call times out, a delayed
   * identifier-free reply can still match a later sequential call that expects the same shape and
   * resolve it with stale data. This default may change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  getScannerStatus(): Promise<{ status: ScannerStatus | string }>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. When sender
   * identity is unavailable (Android API 24 to 33, or API 34+ when the platform withholds it),
   * matching identifier-free replies are accepted for compatibility. If a call times out, a delayed
   * identifier-free reply can still match a later sequential call that expects the same shape and
   * resolve it with stale data. This default may change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  getVersionInfo(): Promise<ZebraDeviceVersionInfo>;
  disableDatawedge(): Promise<void>;
  disableScannerInput(): Promise<void>;
  enableDatawedge(): Promise<void>;
  enableScannerInput(): Promise<void>;
  /**
   * Android only. DataWedge may omit `COMMAND_IDENTIFIER` on query replies. The plugin accepts
   * identifier-free replies only when the payload matches exactly one pending call that expects the
   * same result payload shape. Avoid overlapping pending calls that share that shape. When sender
   * identity is unavailable (Android API 24 to 33, or API 34+ when the platform withholds it),
   * matching identifier-free replies are accepted for compatibility. If a call times out, a delayed
   * identifier-free reply can still match a later sequential call that expects the same shape and
   * resolve it with stale data. This default may change in the next Capacitor major version; see
   * https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25/.
   */
  enumerateTriggers(): Promise<{ triggers: string[] }>;
  notify(options: DataWedgeNotifyOptions): Promise<void>;
  resetDefaultProfile(): Promise<void>;
  setDefaultProfile(options: { profileName: string }): Promise<void>;
  setReportingOptions(options: ReportingOptions): Promise<void>;
  softRfidTrigger(): Promise<ZebraScanResult>;
  softScanTrigger(options: { intentAction: string }): Promise<ZebraScanResult>;
  switchScanner(options: { scannerIndex?: string; scannerIdentifier?: ScannerIdentifier }): Promise<void>;
  switchScannerParams(options: SwitchScannerParamsOptions): Promise<void>;
  switchToProfile(options: { profileName: string }): Promise<void>;
  getPluginVersion(): Promise<PluginVersionResult>;
  /** Native best-effort closure notification. Always explicitly end to confirm hardware cleanup. */
  addListener(
    eventName: 'scanSessionClosed',
    listenerFunc: (event: ScanSessionState) => void,
  ): Promise<PluginListenerHandle>;
  addListener(eventName: 'scan', listenerFunc: (event: ZebraScanResult) => void): Promise<PluginListenerHandle>;
  addListener(
    eventName: 'notification',
    listenerFunc: (event: ZebraNotificationEvent) => void,
  ): Promise<PluginListenerHandle>;
}
