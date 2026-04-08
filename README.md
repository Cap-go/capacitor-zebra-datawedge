# @capgo/capacitor-zebra-datawedge

<a href="https://capgo.app/">
  <img
    src="https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png"
    alt="Capgo - Instant updates for capacitor"
  />
</a>

<div align="center">
  <h2>
    <a href="https://capgo.app/?ref=plugin_zebra_datawedge"> ➡️ Get instant updates for your app with Capgo</a>
  </h2>
  <h2>
    <a href="https://capgo.app/consulting/?ref=plugin_zebra_datawedge">
      Missing a feature? We’ll build the plugin for you.
    </a>
  </h2>
</div>

Capgo's Zebra DataWedge plugin brings Zebra's Android barcode and RFID capture APIs to Capacitor.

It is designed as an open alternative to Ionic's enterprise Zebra Scanner offering and exposes the same high-level groups:

- `ZebraConfiguration` for profile management
- `ZebraNotification` for scanner/profile status notifications
- `ZebraQuery` for profile, scanner, and DataWedge queries
- `ZebraRuntime` for enabling/disabling scanners and triggering scans

## Documentation

The complete documentation is published here: https://capgo.app/docs/plugins/zebra-datawedge/

## Compatibility

| Plugin version | Capacitor compatibility | Maintained |
| -------------- | ----------------------- | ---------- |
| `v8.*.*`       | `v8.*.*`                | ✅         |

> This plugin is Android-only in practice because Zebra DataWedge itself runs on Zebra Android devices.

## Install

```bash
bun add @capgo/capacitor-zebra-datawedge
bunx cap sync
```

## Zebra setup

Configure a DataWedge profile on the Zebra device before scanning:

1. Associate the profile with your app.
2. Enable `Intent Output`.
3. Set `Intent delivery` to `Broadcast Intent`.
4. Choose an intent action such as `app.capgo.zebra.SCAN`.

## Basic usage

```ts
import {
  DataWedgeConfigMode,
  DataWedgePlugin,
  ZebraConfiguration,
  ZebraDataWedge,
  ZebraRuntime,
} from '@capgo/capacitor-zebra-datawedge';

const intentAction = 'app.capgo.zebra.SCAN';

await ZebraConfiguration.setConfig({
  profileName: 'CapgoZebraProfile',
  profileEnabled: true,
  configMode: DataWedgeConfigMode.CREATE_IF_NOT_EXIST,
  appList: [
    {
      packageName: 'com.example.app',
      activityList: ['*'],
    },
  ],
  pluginConfigs: [
    {
      pluginName: DataWedgePlugin.BARCODE,
      resetConfig: true,
      paramList: {
        scanner_selection: 'auto',
        scanner_input_enabled: 'true',
      },
    },
    {
      pluginName: DataWedgePlugin.INTENT,
      resetConfig: true,
      paramList: {
        intent_output_enabled: 'true',
        intent_action: intentAction,
        intent_delivery: 2,
      },
    },
  ],
});

await ZebraDataWedge.addListener('scan', (result) => {
  console.log('Scanned', result.data, result.labelType);
});

const result = await ZebraRuntime.softScanTrigger(intentAction);
console.log(result);
```

## Notes

- Query and configuration APIs depend on Zebra DataWedge being installed and enabled.
- `softScanTrigger()` waits for the next scan broadcast on the intent action you provide.
- `softRfidTrigger()` uses the first registered scan intent action. Register a scan action first if you need RFID reads.
- Web and iOS return "not available" errors for all DataWedge operations.

## API

<docgen-index>

- [`cloneProfile(...)`](#cloneprofile)
- [`createProfile(...)`](#createprofile)
- [`deleteProfile(...)`](#deleteprofile)
- [`importConfig(...)`](#importconfig)
- [`renameProfile(...)`](#renameprofile)
- [`restoreConfig()`](#restoreconfig)
- [`setConfig(...)`](#setconfig)
- [`setDisabledAppList(...)`](#setdisabledapplist)
- [`setIgnoreDisabledProfiles(...)`](#setignoredisabledprofiles)
- [`registerForNotification(...)`](#registerfornotification)
- [`unRegisterForNotification(...)`](#unregisterfornotification)
- [`enumerateScanners()`](#enumeratescanners)
- [`getActiveProfile()`](#getactiveprofile)
- [`getAssociatedApps(...)`](#getassociatedapps)
- [`getConfig(...)`](#getconfig)
- [`getDatawedgeStatus()`](#getdatawedgestatus)
- [`getDisabledAppList()`](#getdisabledapplist)
- [`getIgnoreDisabledProfiles()`](#getignoredisabledprofiles)
- [`getProfilesList()`](#getprofileslist)
- [`getScannerStatus()`](#getscannerstatus)
- [`getVersionInfo()`](#getversioninfo)
- [`disableDatawedge()`](#disabledatawedge)
- [`disableScannerInput()`](#disablescannerinput)
- [`enableDatawedge()`](#enabledatawedge)
- [`enableScannerInput()`](#enablescannerinput)
- [`enumerateTriggers()`](#enumeratetriggers)
- [`notify(...)`](#notify)
- [`resetDefaultProfile()`](#resetdefaultprofile)
- [`setDefaultProfile(...)`](#setdefaultprofile)
- [`setReportingOptions(...)`](#setreportingoptions)
- [`softRfidTrigger()`](#softrfidtrigger)
- [`softScanTrigger(...)`](#softscantrigger)
- [`switchScanner(...)`](#switchscanner)
- [`switchScannerParams(...)`](#switchscannerparams)
- [`switchToProfile(...)`](#switchtoprofile)
- [`getPluginVersion()`](#getpluginversion)
- [`addListener('scan', ...)`](#addlistenerscan-)
- [`addListener('notification', ...)`](#addlistenernotification-)
- [Interfaces](#interfaces)
- [Type Aliases](#type-aliases)
- [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### cloneProfile(...)

```typescript
cloneProfile(options: CloneProfileOptions) => Promise<void>
```

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code><a href="#cloneprofileoptions">CloneProfileOptions</a></code> |

---

### createProfile(...)

```typescript
createProfile(options: CreateProfileOptions) => Promise<void>
```

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#createprofileoptions">CreateProfileOptions</a></code> |

---

### deleteProfile(...)

```typescript
deleteProfile(options: DeleteProfileOptions) => Promise<void>
```

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#deleteprofileoptions">DeleteProfileOptions</a></code> |

---

### importConfig(...)

```typescript
importConfig(options: ImportConfigOptions) => Promise<void>
```

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code><a href="#importconfigoptions">ImportConfigOptions</a></code> |

---

### renameProfile(...)

```typescript
renameProfile(options: RenameProfileOptions) => Promise<void>
```

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#renameprofileoptions">RenameProfileOptions</a></code> |

---

### restoreConfig()

```typescript
restoreConfig() => Promise<void>
```

---

### setConfig(...)

```typescript
setConfig(options: SetConfigOptions) => Promise<void>
```

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#setconfigoptions">SetConfigOptions</a></code> |

---

### setDisabledAppList(...)

```typescript
setDisabledAppList(options: SetDisabledAppListOptions) => Promise<void>
```

| Param         | Type                                                                            |
| ------------- | ------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#setdisabledapplistoptions">SetDisabledAppListOptions</a></code> |

---

### setIgnoreDisabledProfiles(...)

```typescript
setIgnoreDisabledProfiles(options: IgnoreDisabledProfilesResult) => Promise<void>
```

| Param         | Type                                                                                  |
| ------------- | ------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#ignoredisabledprofilesresult">IgnoreDisabledProfilesResult</a></code> |

---

### registerForNotification(...)

```typescript
registerForNotification(options: Omit<RegisterForNotificationOptions, 'callback'>) => Promise<void>
```

| Param         | Type                                                                                                                                  |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#omit">Omit</a>&lt;<a href="#registerfornotificationoptions">RegisterForNotificationOptions</a>, 'callback'&gt;</code> |

---

### unRegisterForNotification(...)

```typescript
unRegisterForNotification(options: Omit<RegisterForNotificationOptions, 'callback'>) => Promise<void>
```

| Param         | Type                                                                                                                                  |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#omit">Omit</a>&lt;<a href="#registerfornotificationoptions">RegisterForNotificationOptions</a>, 'callback'&gt;</code> |

---

### enumerateScanners()

```typescript
enumerateScanners() => Promise<{ scanners: ZebraDeviceScanner[]; }>
```

**Returns:** <code>Promise&lt;{ scanners: ZebraDeviceScanner[]; }&gt;</code>

---

### getActiveProfile()

```typescript
getActiveProfile() => Promise<{ profileName: string; }>
```

**Returns:** <code>Promise&lt;{ profileName: string; }&gt;</code>

---

### getAssociatedApps(...)

```typescript
getAssociatedApps(options: { profileName: string; }) => Promise<{ appList: DataWedgeAppConfig[]; }>
```

| Param         | Type                                  |
| ------------- | ------------------------------------- |
| **`options`** | <code>{ profileName: string; }</code> |

**Returns:** <code>Promise&lt;{ appList: DataWedgeAppConfig[]; }&gt;</code>

---

### getConfig(...)

```typescript
getConfig(options: GetConfigOptions) => Promise<ZebraProfileConfiguration>
```

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#getconfigoptions">GetConfigOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#zebraprofileconfiguration">ZebraProfileConfiguration</a>&gt;</code>

---

### getDatawedgeStatus()

```typescript
getDatawedgeStatus() => Promise<DataWedgeStatusResult>
```

**Returns:** <code>Promise&lt;<a href="#datawedgestatusresult">DataWedgeStatusResult</a>&gt;</code>

---

### getDisabledAppList()

```typescript
getDisabledAppList() => Promise<{ appList: DataWedgeAppConfig[]; }>
```

**Returns:** <code>Promise&lt;{ appList: DataWedgeAppConfig[]; }&gt;</code>

---

### getIgnoreDisabledProfiles()

```typescript
getIgnoreDisabledProfiles() => Promise<IgnoreDisabledProfilesResult>
```

**Returns:** <code>Promise&lt;<a href="#ignoredisabledprofilesresult">IgnoreDisabledProfilesResult</a>&gt;</code>

---

### getProfilesList()

```typescript
getProfilesList() => Promise<{ profiles: string[]; }>
```

**Returns:** <code>Promise&lt;{ profiles: string[]; }&gt;</code>

---

### getScannerStatus()

```typescript
getScannerStatus() => Promise<{ status: ScannerStatus | string; }>
```

**Returns:** <code>Promise&lt;{ status: string; }&gt;</code>

---

### getVersionInfo()

```typescript
getVersionInfo() => Promise<ZebraDeviceVersionInfo>
```

**Returns:** <code>Promise&lt;<a href="#zebradeviceversioninfo">ZebraDeviceVersionInfo</a>&gt;</code>

---

### disableDatawedge()

```typescript
disableDatawedge() => Promise<void>
```

---

### disableScannerInput()

```typescript
disableScannerInput() => Promise<void>
```

---

### enableDatawedge()

```typescript
enableDatawedge() => Promise<void>
```

---

### enableScannerInput()

```typescript
enableScannerInput() => Promise<void>
```

---

### enumerateTriggers()

```typescript
enumerateTriggers() => Promise<{ triggers: string[]; }>
```

**Returns:** <code>Promise&lt;{ triggers: string[]; }&gt;</code>

---

### notify(...)

```typescript
notify(options: DataWedgeNotifyOptions) => Promise<void>
```

| Param         | Type                                                                      |
| ------------- | ------------------------------------------------------------------------- |
| **`options`** | <code><a href="#datawedgenotifyoptions">DataWedgeNotifyOptions</a></code> |

---

### resetDefaultProfile()

```typescript
resetDefaultProfile() => Promise<void>
```

---

### setDefaultProfile(...)

```typescript
setDefaultProfile(options: { profileName: string; }) => Promise<void>
```

| Param         | Type                                  |
| ------------- | ------------------------------------- |
| **`options`** | <code>{ profileName: string; }</code> |

---

### setReportingOptions(...)

```typescript
setReportingOptions(options: ReportingOptions) => Promise<void>
```

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#reportingoptions">ReportingOptions</a></code> |

---

### softRfidTrigger()

```typescript
softRfidTrigger() => Promise<ZebraScanResult>
```

**Returns:** <code>Promise&lt;<a href="#zebrascanresult">ZebraScanResult</a>&gt;</code>

---

### softScanTrigger(...)

```typescript
softScanTrigger(options: { intentAction: string; }) => Promise<ZebraScanResult>
```

| Param         | Type                                   |
| ------------- | -------------------------------------- |
| **`options`** | <code>{ intentAction: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#zebrascanresult">ZebraScanResult</a>&gt;</code>

---

### switchScanner(...)

```typescript
switchScanner(options: { scannerIndex?: string; scannerIdentifier?: ScannerIdentifier; }) => Promise<void>
```

| Param         | Type                                                                                                            |
| ------------- | --------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ scannerIndex?: string; scannerIdentifier?: <a href="#scanneridentifier">ScannerIdentifier</a>; }</code> |

---

### switchScannerParams(...)

```typescript
switchScannerParams(options: SwitchScannerParamsOptions) => Promise<void>
```

| Param         | Type                                                                              |
| ------------- | --------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#switchscannerparamsoptions">SwitchScannerParamsOptions</a></code> |

---

### switchToProfile(...)

```typescript
switchToProfile(options: { profileName: string; }) => Promise<void>
```

| Param         | Type                                  |
| ------------- | ------------------------------------- |
| **`options`** | <code>{ profileName: string; }</code> |

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

---

### addListener('scan', ...)

```typescript
addListener(eventName: 'scan', listenerFunc: (event: ZebraScanResult) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                            |
| ------------------ | ------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'scan'</code>                                                             |
| **`listenerFunc`** | <code>(event: <a href="#zebrascanresult">ZebraScanResult</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

---

### addListener('notification', ...)

```typescript
addListener(eventName: 'notification', listenerFunc: (event: ZebraNotificationEvent) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                                          |
| ------------------ | --------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'notification'</code>                                                                   |
| **`listenerFunc`** | <code>(event: <a href="#zebranotificationevent">ZebraNotificationEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

---

### Interfaces

#### CloneProfileOptions

| Prop                    | Type                |
| ----------------------- | ------------------- |
| **`destProfileName`**   | <code>string</code> |
| **`sourceProfileName`** | <code>string</code> |

#### CreateProfileOptions

| Prop              | Type                |
| ----------------- | ------------------- |
| **`profileName`** | <code>string</code> |

#### DeleteProfileOptions

| Prop               | Type                  |
| ------------------ | --------------------- |
| **`profileNames`** | <code>string[]</code> |

#### ImportConfigOptions

| Prop             | Type                  |
| ---------------- | --------------------- |
| **`fileList`**   | <code>string[]</code> |
| **`folderPath`** | <code>string</code>   |

#### RenameProfileOptions

| Prop                     | Type                |
| ------------------------ | ------------------- |
| **`currentProfileName`** | <code>string</code> |
| **`newProfileName`**     | <code>string</code> |

#### SetConfigOptions

| Prop                 | Type                                                                |
| -------------------- | ------------------------------------------------------------------- |
| **`appList`**        | <code>DataWedgeAppConfig[]</code>                                   |
| **`configMode`**     | <code><a href="#datawedgeconfigmode">DataWedgeConfigMode</a></code> |
| **`pluginConfigs`**  | <code>DataWedgePluginConfig[]</code>                                |
| **`profileEnabled`** | <code>boolean</code>                                                |
| **`profileName`**    | <code>string</code>                                                 |

#### DataWedgeAppConfig

| Prop               | Type                  |
| ------------------ | --------------------- |
| **`activityList`** | <code>string[]</code> |
| **`packageName`**  | <code>string</code>   |

#### DataWedgePluginConfig

| Prop                   | Type                                                                            |
| ---------------------- | ------------------------------------------------------------------------------- |
| **`outputPluginName`** | <code><a href="#datawedgeplugin">DataWedgePlugin</a></code>                     |
| **`paramList`**        | <code><a href="#datawedgepluginparameters">DataWedgePluginParameters</a></code> |
| **`pluginName`**       | <code><a href="#datawedgeplugin">DataWedgePlugin</a></code>                     |
| **`resetConfig`**      | <code>boolean</code>                                                            |

#### DataWedgePluginParameters

#### SetDisabledAppListOptions

| Prop             | Type                                                                |
| ---------------- | ------------------------------------------------------------------- |
| **`appList`**    | <code>DataWedgeAppConfig[]</code>                                   |
| **`configMode`** | <code><a href="#datawedgeconfigmode">DataWedgeConfigMode</a></code> |

#### IgnoreDisabledProfilesResult

| Prop          | Type                 |
| ------------- | -------------------- |
| **`enabled`** | <code>boolean</code> |

#### RegisterForNotificationOptions

| Prop                   | Type                                                                                                   |
| ---------------------- | ------------------------------------------------------------------------------------------------------ |
| **`appName`**          | <code>string</code>                                                                                    |
| **`callback`**         | <code>((notification: <a href="#zebranotificationevent">ZebraNotificationEvent</a>) =&gt; void)</code> |
| **`intentAction`**     | <code>string</code>                                                                                    |
| **`notificationType`** | <code><a href="#datawedgenotificationtype">DataWedgeNotificationType</a></code>                        |

#### ZebraNotificationEvent

| Prop                   | Type                                                             |
| ---------------------- | ---------------------------------------------------------------- |
| **`notificationType`** | <code>string</code>                                              |
| **`profileName`**      | <code>string</code>                                              |
| **`scannerStatus`**    | <code>string</code>                                              |
| **`status`**           | <code>string</code>                                              |
| **`raw`**              | <code><a href="#record">Record</a>&lt;string, unknown&gt;</code> |

#### ZebraDeviceScanner

| Prop                  | Type                 |
| --------------------- | -------------------- |
| **`connectionState`** | <code>boolean</code> |
| **`id`**              | <code>string</code>  |
| **`index`**           | <code>number</code>  |
| **`name`**            | <code>string</code>  |

#### ZebraProfileConfiguration

| Prop                   | Type                                 |
| ---------------------- | ------------------------------------ |
| **`appList`**          | <code>DataWedgeAppConfig[]</code>    |
| **`pluginConfigList`** | <code>DataWedgePluginConfig[]</code> |
| **`profileEnabled`**   | <code>boolean</code>                 |
| **`profileName`**      | <code>string</code>                  |

#### GetConfigOptions

| Prop                | Type                               |
| ------------------- | ---------------------------------- |
| **`pluginConfigs`** | <code>PluginConfigOptions[]</code> |
| **`profileName`**   | <code>string</code>                |

#### PluginConfigOptions

| Prop                   | Type                                                        |
| ---------------------- | ----------------------------------------------------------- |
| **`outputPluginName`** | <code><a href="#datawedgeplugin">DataWedgePlugin</a></code> |
| **`pluginName`**       | <code><a href="#datawedgeplugin">DataWedgePlugin</a></code> |

#### DataWedgeStatusResult

| Prop            | Type                 |
| --------------- | -------------------- |
| **`isEnabled`** | <code>boolean</code> |

#### ZebraDeviceVersionInfo

| Prop                          | Type                  |
| ----------------------------- | --------------------- |
| **`barcodeVersion`**          | <code>string</code>   |
| **`dataWedgeVersion`**        | <code>string</code>   |
| **`decoderVersion`**          | <code>string</code>   |
| **`scannerFirmwareVersions`** | <code>string[]</code> |
| **`simulScanVersion`**        | <code>string</code>   |

#### DataWedgeNotifyOptions

| Prop                       | Type                  |
| -------------------------- | --------------------- |
| **`deviceIdentifier`**     | <code>string</code>   |
| **`notificationSettings`** | <code>number[]</code> |

#### ReportingOptions

| Prop                               | Type                                      |
| ---------------------------------- | ----------------------------------------- |
| **`reportingEnabled`**             | <code>boolean</code>                      |
| **`reportingGenerateOption`**      | <code>'manual' \| 'auto' \| 'both'</code> |
| **`reportingShowForManualImport`** | <code>boolean</code>                      |

#### ZebraScanResult

| Prop               | Type                |
| ------------------ | ------------------- |
| **`data`**         | <code>string</code> |
| **`decodedMode`**  | <code>string</code> |
| **`intentAction`** | <code>string</code> |
| **`labelType`**    | <code>string</code> |
| **`source`**       | <code>string</code> |

#### SwitchScannerParamsOptions

| Prop                    | Type                                                                            |
| ----------------------- | ------------------------------------------------------------------------------- |
| **`scannerIdentifier`** | <code><a href="#scanneridentifier">ScannerIdentifier</a></code>                 |
| **`scannerParams`**     | <code><a href="#datawedgepluginparameters">DataWedgePluginParameters</a></code> |

#### PluginVersionResult

| Prop          | Type                |
| ------------- | ------------------- |
| **`version`** | <code>string</code> |

#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

### Type Aliases

#### Omit

Construct a type with the properties of T except for those in type K.

<code>
  <a href="#pick">Pick</a>&lt;T, <a href="#exclude">Exclude</a>&lt;keyof T, K&gt;&gt;
</code>

#### Pick

From T, pick a set of properties whose keys are in the union K

<code>{
 [P in K]: T[P];
 }</code>

#### Exclude

<a href="#exclude">Exclude</a> from T those types that are assignable to U

<code>T extends U ? never : T</code>

#### Record

Construct a type with a set of properties K of type T

<code>{
 [P in K]: T;
 }</code>

### Enums

#### DataWedgeConfigMode

| Members                   | Value                              |
| ------------------------- | ---------------------------------- |
| **`CREATE_IF_NOT_EXIST`** | <code>'CREATE_IF_NOT_EXIST'</code> |
| **`OVERWRITE`**           | <code>'OVERWRITE'</code>           |
| **`REMOVE`**              | <code>'REMOVE'</code>              |
| **`UPDATE`**              | <code>'UPDATE'</code>              |

#### DataWedgePlugin

| Members         | Value                    |
| --------------- | ------------------------ |
| **`ADF`**       | <code>'ADF'</code>       |
| **`BARCODE`**   | <code>'BARCODE'</code>   |
| **`BDF`**       | <code>'BDF'</code>       |
| **`DCP`**       | <code>'DCP'</code>       |
| **`EKB`**       | <code>'EKB'</code>       |
| **`INTENT`**    | <code>'INTENT'</code>    |
| **`IP`**        | <code>'IP'</code>        |
| **`KEYSTROKE`** | <code>'KEYSTROKE'</code> |
| **`MSR`**       | <code>'MSR'</code>       |
| **`RFID`**      | <code>'RFID'</code>      |
| **`SERIAL`**    | <code>'SERIAL'</code>    |
| **`TOKENS`**    | <code>'TOKENS'</code>    |
| **`VOICE`**     | <code>'VOICE'</code>     |

#### ScannerStatus

| Members            | Value                       |
| ------------------ | --------------------------- |
| **`CONNECTED`**    | <code>'CONNECTED'</code>    |
| **`DISABLED`**     | <code>'DISABLED'</code>     |
| **`DISCONNECTED`** | <code>'DISCONNECTED'</code> |
| **`IDLE`**         | <code>'IDLE'</code>         |
| **`SCANNING`**     | <code>'SCANNING'</code>     |
| **`WAITING`**      | <code>'WAITING'</code>      |

#### DataWedgeNotificationType

| Members                    | Value                               |
| -------------------------- | ----------------------------------- |
| **`CONFIGURATION_UPDATE`** | <code>'CONFIGURATION_UPDATE'</code> |
| **`PROFILE_SWITCH`**       | <code>'PROFILE_SWITCH'</code>       |
| **`SCANNER_STATUS`**       | <code>'SCANNER_STATUS'</code>       |

#### ScannerIdentifier

| Members                   | Value                              |
| ------------------------- | ---------------------------------- |
| **`AUTO`**                | <code>'AUTO'</code>                |
| **`BLUETOOTH_DS3678`**    | <code>'BLUETOOTH_DS3678'</code>    |
| **`BLUETOOTH_RS6000`**    | <code>'BLUETOOTH_RS6000'</code>    |
| **`BLUETOOTH_SSI`**       | <code>'BLUETOOTH_SSI'</code>       |
| **`INTERNAL_CAMERA`**     | <code>'INTERNAL_CAMERA'</code>     |
| **`INTERNAL_IMAGER`**     | <code>'INTERNAL_IMAGER'</code>     |
| **`INTERNAL_LASER`**      | <code>'INTERNAL_LASER'</code>      |
| **`PLUGABLE_SSI`**        | <code>'PLUGABLE_SSI'</code>        |
| **`PLUGABLE_SSI_RS5000`** | <code>'PLUGABLE_SSI_RS5000'</code> |
| **`SERIAL_SSI`**          | <code>'SERIAL_SSI'</code>          |
| **`USB_SSI_DS3608`**      | <code>'USB_SSI_DS3608'</code>      |

</docgen-api>
