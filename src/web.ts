import { WebPlugin } from '@capacitor/core';

import type {
  CloneProfileOptions,
  CreateProfileOptions,
  DataWedgeNotifyOptions,
  DataWedgeStatusResult,
  DeleteProfileOptions,
  GetConfigOptions,
  IgnoreDisabledProfilesResult,
  ImportConfigOptions,
  PluginVersionResult,
  RegisterForNotificationOptions,
  RenameProfileOptions,
  ReportingOptions,
  SetConfigOptions,
  SetDisabledAppListOptions,
  SwitchScannerParamsOptions,
  ZebraDataWedgePlugin,
  ZebraProfileConfiguration,
  ZebraScanResult,
} from './definitions';

const unavailable = () => {
  throw new Error('Zebra DataWedge is only available on Android devices with Zebra DataWedge installed.');
};

export class ZebraDataWedgeWeb extends WebPlugin implements ZebraDataWedgePlugin {
  cloneProfile(_options: CloneProfileOptions): Promise<void> {
    return unavailable();
  }

  createProfile(_options: CreateProfileOptions): Promise<void> {
    return unavailable();
  }

  deleteProfile(_options: DeleteProfileOptions): Promise<void> {
    return unavailable();
  }

  importConfig(_options: ImportConfigOptions): Promise<void> {
    return unavailable();
  }

  renameProfile(_options: RenameProfileOptions): Promise<void> {
    return unavailable();
  }

  restoreConfig(): Promise<void> {
    return unavailable();
  }

  setConfig(_options: SetConfigOptions): Promise<void> {
    return unavailable();
  }

  setDisabledAppList(_options: SetDisabledAppListOptions): Promise<void> {
    return unavailable();
  }

  setIgnoreDisabledProfiles(_options: IgnoreDisabledProfilesResult): Promise<void> {
    return unavailable();
  }

  registerForNotification(_options: Omit<RegisterForNotificationOptions, 'callback'>): Promise<void> {
    return unavailable();
  }

  unRegisterForNotification(_options: Omit<RegisterForNotificationOptions, 'callback'>): Promise<void> {
    return unavailable();
  }

  enumerateScanners(): Promise<{ scanners: never[] }> {
    return unavailable();
  }

  getActiveProfile(): Promise<{ profileName: string }> {
    return unavailable();
  }

  getAssociatedApps(_options: { profileName: string }): Promise<{ appList: never[] }> {
    return unavailable();
  }

  getConfig(_options: GetConfigOptions): Promise<ZebraProfileConfiguration> {
    return unavailable();
  }

  getDatawedgeStatus(): Promise<DataWedgeStatusResult> {
    return unavailable();
  }

  getDisabledAppList(): Promise<{ appList: never[] }> {
    return unavailable();
  }

  getIgnoreDisabledProfiles(): Promise<IgnoreDisabledProfilesResult> {
    return unavailable();
  }

  getProfilesList(): Promise<{ profiles: string[] }> {
    return unavailable();
  }

  getScannerStatus(): Promise<{ status: string }> {
    return unavailable();
  }

  getVersionInfo(): Promise<Record<string, never>> {
    return unavailable();
  }

  disableDatawedge(): Promise<void> {
    return unavailable();
  }

  disableScannerInput(): Promise<void> {
    return unavailable();
  }

  enableDatawedge(): Promise<void> {
    return unavailable();
  }

  enableScannerInput(): Promise<void> {
    return unavailable();
  }

  enumerateTriggers(): Promise<{ triggers: string[] }> {
    return unavailable();
  }

  notify(_options: DataWedgeNotifyOptions): Promise<void> {
    return unavailable();
  }

  resetDefaultProfile(): Promise<void> {
    return unavailable();
  }

  setDefaultProfile(_options: { profileName: string }): Promise<void> {
    return unavailable();
  }

  setReportingOptions(_options: ReportingOptions): Promise<void> {
    return unavailable();
  }

  softRfidTrigger(): Promise<ZebraScanResult> {
    return unavailable();
  }

  softScanTrigger(_options: { intentAction: string }): Promise<ZebraScanResult> {
    return unavailable();
  }

  switchScanner(_options: { scannerIndex?: string; scannerIdentifier?: string }): Promise<void> {
    return unavailable();
  }

  switchScannerParams(_options: SwitchScannerParamsOptions): Promise<void> {
    return unavailable();
  }

  switchToProfile(_options: { profileName: string }): Promise<void> {
    return unavailable();
  }

  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: 'web' };
  }
}
