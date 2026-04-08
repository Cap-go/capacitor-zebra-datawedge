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

const unavailable = (...args: unknown[]) => {
  void args;
  throw new Error('Zebra DataWedge is only available on Android devices with Zebra DataWedge installed.');
};

export class ZebraDataWedgeWeb extends WebPlugin implements ZebraDataWedgePlugin {
  cloneProfile(options: CloneProfileOptions): Promise<void> {
    return unavailable(options);
  }

  createProfile(options: CreateProfileOptions): Promise<void> {
    return unavailable(options);
  }

  deleteProfile(options: DeleteProfileOptions): Promise<void> {
    return unavailable(options);
  }

  importConfig(options: ImportConfigOptions): Promise<void> {
    return unavailable(options);
  }

  renameProfile(options: RenameProfileOptions): Promise<void> {
    return unavailable(options);
  }

  restoreConfig(): Promise<void> {
    return unavailable();
  }

  setConfig(options: SetConfigOptions): Promise<void> {
    return unavailable(options);
  }

  setDisabledAppList(options: SetDisabledAppListOptions): Promise<void> {
    return unavailable(options);
  }

  setIgnoreDisabledProfiles(options: IgnoreDisabledProfilesResult): Promise<void> {
    return unavailable(options);
  }

  registerForNotification(options: Omit<RegisterForNotificationOptions, 'callback'>): Promise<void> {
    return unavailable(options);
  }

  unRegisterForNotification(options: Omit<RegisterForNotificationOptions, 'callback'>): Promise<void> {
    return unavailable(options);
  }

  enumerateScanners(): Promise<{ scanners: never[] }> {
    return unavailable();
  }

  getActiveProfile(): Promise<{ profileName: string }> {
    return unavailable();
  }

  getAssociatedApps(options: { profileName: string }): Promise<{ appList: never[] }> {
    return unavailable(options);
  }

  getConfig(options: GetConfigOptions): Promise<ZebraProfileConfiguration> {
    return unavailable(options);
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

  notify(options: DataWedgeNotifyOptions): Promise<void> {
    return unavailable(options);
  }

  resetDefaultProfile(): Promise<void> {
    return unavailable();
  }

  setDefaultProfile(options: { profileName: string }): Promise<void> {
    return unavailable(options);
  }

  setReportingOptions(options: ReportingOptions): Promise<void> {
    return unavailable(options);
  }

  softRfidTrigger(): Promise<ZebraScanResult> {
    return unavailable();
  }

  softScanTrigger(options: { intentAction: string }): Promise<ZebraScanResult> {
    return unavailable(options);
  }

  switchScanner(options: { scannerIndex?: string; scannerIdentifier?: string }): Promise<void> {
    return unavailable(options);
  }

  switchScannerParams(options: SwitchScannerParamsOptions): Promise<void> {
    return unavailable(options);
  }

  switchToProfile(options: { profileName: string }): Promise<void> {
    return unavailable(options);
  }

  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: 'web' };
  }
}
