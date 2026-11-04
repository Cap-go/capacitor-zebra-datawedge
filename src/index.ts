import { registerPlugin } from '@capacitor/core';

import type {
  CloneProfileOptions,
  DataWedgeAppConfig,
  CreateProfileOptions,
  DataWedgeNotifyOptions,
  DataWedgeStatusResult,
  GetConfigOptions,
  ImportConfigOptions,
  RenameProfileOptions,
  ReportingOptions,
  ScannerStatus,
  SetConfigOptions,
  SetDisabledAppListOptions,
  SwitchScannerParamsOptions,
  ZebraDeviceScanner,
  ZebraDataWedgePlugin,
  ZebraDeviceVersionInfo,
  ZebraProfileConfiguration,
  ZebraScanResult,
} from './definitions';
import { createZebraNotification } from './notification-registration';

const ZebraDataWedge = registerPlugin<ZebraDataWedgePlugin>('ZebraDataWedge', {
  web: () => import('./web').then((m) => new m.ZebraDataWedgeWeb()),
});

export const ZebraConfiguration = {
  cloneProfile: (options: CloneProfileOptions): Promise<void> => ZebraDataWedge.cloneProfile(options),
  createProfile: (options: CreateProfileOptions): Promise<void> => ZebraDataWedge.createProfile(options),
  deleteProfile: (options: { profileNames: string[] }): Promise<void> => ZebraDataWedge.deleteProfile(options),
  importConfig: (options: ImportConfigOptions): Promise<void> => ZebraDataWedge.importConfig(options),
  renameProfile: (options: RenameProfileOptions): Promise<void> => ZebraDataWedge.renameProfile(options),
  restoreConfig: (): Promise<void> => ZebraDataWedge.restoreConfig(),
  setConfig: (options: SetConfigOptions): Promise<void> => ZebraDataWedge.setConfig(options),
  setDisabledAppList: (options: SetDisabledAppListOptions): Promise<void> => ZebraDataWedge.setDisabledAppList(options),
  setIgnoreDisabledProfiles: (doIgnoreDisabledProfiles: boolean): Promise<void> =>
    ZebraDataWedge.setIgnoreDisabledProfiles({ enabled: doIgnoreDisabledProfiles }),
};

export const ZebraNotification = createZebraNotification(ZebraDataWedge);

export const ZebraQuery = {
  enumerateScanners: async (): Promise<ZebraDeviceScanner[]> => (await ZebraDataWedge.enumerateScanners()).scanners,
  getActiveProfile: async (): Promise<string> => (await ZebraDataWedge.getActiveProfile()).profileName,
  getAssociatedApps: async (profileName: string): Promise<DataWedgeAppConfig[]> =>
    (await ZebraDataWedge.getAssociatedApps({ profileName })).appList,
  getConfig: (options: GetConfigOptions): Promise<ZebraProfileConfiguration> => ZebraDataWedge.getConfig(options),
  getDatawedgeStatus: (): Promise<DataWedgeStatusResult> => ZebraDataWedge.getDatawedgeStatus(),
  getDisabledAppList: async (): Promise<DataWedgeAppConfig[]> => (await ZebraDataWedge.getDisabledAppList()).appList,
  getIgnoreDisabledProfiles: async (): Promise<boolean> => (await ZebraDataWedge.getIgnoreDisabledProfiles()).enabled,
  getProfilesList: async (): Promise<string[]> => (await ZebraDataWedge.getProfilesList()).profiles,
  getScannerStatus: async (): Promise<ScannerStatus | string> => (await ZebraDataWedge.getScannerStatus()).status,
  getVersionInfo: (): Promise<ZebraDeviceVersionInfo> => ZebraDataWedge.getVersionInfo(),
};

export const ZebraRuntime = {
  registerScanIntentAction: (intentAction: string): Promise<void> =>
    ZebraDataWedge.registerScanIntentAction({ intentAction }),
  disableDatawedge: (): Promise<void> => ZebraDataWedge.disableDatawedge(),
  disableScannerInput: (): Promise<void> => ZebraDataWedge.disableScannerInput(),
  enableDatawedge: (): Promise<void> => ZebraDataWedge.enableDatawedge(),
  enableScannerInput: (): Promise<void> => ZebraDataWedge.enableScannerInput(),
  enumerateTriggers: async (): Promise<string[]> => (await ZebraDataWedge.enumerateTriggers()).triggers,
  notify: (options: DataWedgeNotifyOptions): Promise<void> => ZebraDataWedge.notify(options),
  resetDefaultProfile: (): Promise<void> => ZebraDataWedge.resetDefaultProfile(),
  setDefaultProfile: (profileName: string): Promise<void> => ZebraDataWedge.setDefaultProfile({ profileName }),
  setReportingOptions: (options: ReportingOptions): Promise<void> => ZebraDataWedge.setReportingOptions(options),
  softRfidTrigger: (): Promise<ZebraScanResult> => ZebraDataWedge.softRfidTrigger(),
  softScanTrigger: (intentAction: string): Promise<ZebraScanResult> => ZebraDataWedge.softScanTrigger({ intentAction }),
  switchScanner: (scanerIndex: string): Promise<void> => ZebraDataWedge.switchScanner({ scannerIndex: scanerIndex }),
  switchScannerParams: (options: SwitchScannerParamsOptions): Promise<void> =>
    ZebraDataWedge.switchScannerParams(options),
  switchToProfile: (profileName: string): Promise<void> => ZebraDataWedge.switchToProfile({ profileName }),
};

export * from './definitions';
export { ZebraDataWedge };
