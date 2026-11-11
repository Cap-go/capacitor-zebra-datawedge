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

export interface DiagnosticsOptions {
  /** Enable metadata tracing. Disabled by default; disabling does not erase previous entries. */
  enabled: boolean;
  /** Number of recent entries retained, from 1 to 1000. Initially 100; omitted values preserve the current capacity. */
  maxEntries?: number;
  /** Clear retained entries and reset elapsed time and dropped count. Default: false. */
  reset?: boolean;
}

export interface DataWedgeDiagnosticsSnapshot {
  diagnosticsVersion: number;
  enabled: boolean;
  maxEntries: number;
  droppedEntries: number;
  manufacturer: string;
  model: string;
  androidVersion: string;
  /** notFound means Android could not find the package; it is not a service-permission check. */
  dataWedgePackage: { status: 'installed' | 'notFound' | 'unavailable'; version?: string };
  commands: {
    commandIdentifier: string;
    outcome: 'SENT' | 'SUCCESS' | 'FAILURE' | 'TIMEOUT' | 'TRANSPORT_ERROR' | 'CANCELLED' | 'INVALID_RESPONSE';
    elapsedMs: number;
  }[];
}

export interface ZebraDataWedgePlugin extends Plugin {
  /** Configure opt-in native command metadata tracing. Never enables DataWedge or scanner input. */
  configureDiagnostics(options: DiagnosticsOptions): Promise<void>;
  /**
   * Return a read-only native environment and trace snapshot. Does not send any DataWedge commands.
   * Barcode values, intent bodies and arbitrary exception text are excluded. The application decides
   * which device information to include when sharing this snapshot. Android only.
   */
  getDiagnostics(): Promise<DataWedgeDiagnosticsSnapshot>;
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
  addListener(eventName: 'scan', listenerFunc: (event: ZebraScanResult) => void): Promise<PluginListenerHandle>;
  addListener(
    eventName: 'notification',
    listenerFunc: (event: ZebraNotificationEvent) => void,
  ): Promise<PluginListenerHandle>;
}
