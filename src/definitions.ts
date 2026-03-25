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

export interface ZebraDataWedgePlugin extends Plugin {
  cloneProfile(options: CloneProfileOptions): Promise<void>;
  createProfile(options: CreateProfileOptions): Promise<void>;
  deleteProfile(options: DeleteProfileOptions): Promise<void>;
  importConfig(options: ImportConfigOptions): Promise<void>;
  renameProfile(options: RenameProfileOptions): Promise<void>;
  restoreConfig(): Promise<void>;
  setConfig(options: SetConfigOptions): Promise<void>;
  setDisabledAppList(options: SetDisabledAppListOptions): Promise<void>;
  setIgnoreDisabledProfiles(options: IgnoreDisabledProfilesResult): Promise<void>;
  registerForNotification(options: Omit<RegisterForNotificationOptions, 'callback'>): Promise<void>;
  unRegisterForNotification(options: Omit<RegisterForNotificationOptions, 'callback'>): Promise<void>;
  enumerateScanners(): Promise<{ scanners: ZebraDeviceScanner[] }>;
  getActiveProfile(): Promise<{ profileName: string }>;
  getAssociatedApps(options: { profileName: string }): Promise<{ appList: DataWedgeAppConfig[] }>;
  getConfig(options: GetConfigOptions): Promise<ZebraProfileConfiguration>;
  getDatawedgeStatus(): Promise<DataWedgeStatusResult>;
  getDisabledAppList(): Promise<{ appList: DataWedgeAppConfig[] }>;
  getIgnoreDisabledProfiles(): Promise<IgnoreDisabledProfilesResult>;
  getProfilesList(): Promise<{ profiles: string[] }>;
  getScannerStatus(): Promise<{ status: ScannerStatus | string }>;
  getVersionInfo(): Promise<ZebraDeviceVersionInfo>;
  disableDatawedge(): Promise<void>;
  disableScannerInput(): Promise<void>;
  enableDatawedge(): Promise<void>;
  enableScannerInput(): Promise<void>;
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
