# Complete configuration results

DataWedge's default LAST_RESULT reply can report success for the last plugin even if an earlier plugin failed. `setConfig` uses this reply mode by default. To validate a multi-plugin update, opt in after checking native support:

```ts
import { Capacitor, ExceptionCode } from '@capacitor/core';
import { ZebraDataWedge, DataWedgeConfigMode, DataWedgePlugin } from '@capgo/capacitor-zebra-datawedge';
import type { DataWedgePluginConfig } from '@capgo/capacitor-zebra-datawedge';

if (Capacitor.getPlatform() !== 'android') {
  throw new Error('DataWedge configuration requires Android.');
}

let supported = false;
try {
  supported = (await ZebraDataWedge.getConfigurationCapabilities()).completeConfigResults;
} catch (error) {
  // A native plugin without this capability API can still configure plugins individually.
  if ((error as { code?: string })?.code !== ExceptionCode.Unimplemented) {
    throw error;
  }
}

const config = { profileName: 'WarehousePicking', configMode: DataWedgeConfigMode.UPDATE };
const pluginConfigs: DataWedgePluginConfig[] = [
  {
    pluginName: DataWedgePlugin.BARCODE,
    paramList: { configure_all_scanners: 'true', scanner_input_enabled: 'false' },
  },
  { pluginName: DataWedgePlugin.KEYSTROKE, paramList: { keystroke_output_enabled: 'false' } },
];

if (supported) {
  await ZebraDataWedge.setConfig({ ...config, completeResult: true, pluginConfigs });
} else {
  for (const pluginConfig of pluginConfigs) {
    await ZebraDataWedge.setConfig({ ...config, pluginConfigs: [pluginConfig] });
  }
}
```

`getConfigurationCapabilities()` reads native Android support for complete results without sending a DataWedge command. The option sends SEND_RESULT=COMPLETE_RESULT. Resolution requires a correlated RESULT_LIST with valid success rows covering every requested plugin. A bare LAST_RESULT acknowledgement cannot satisfy it; unrelated identifiers or identifier-free mutations cannot complete it. Rejections distinguish the following cases.

- Missing plugin coverage rejects with `CONFIG_RESULT_INCOMPLETE`.
- Malformed rows, contradictory failure/success fields or invalid result types reject with `CONFIG_RESULT_INVALID`.
- DataWedge failure codes are preserved when they match `[A-Z0-9_]{1,64}`. Other values produce `CONFIG_RESULT_INVALID`.
- An outer `FAILURE` rejects immediately. An unanswered complete-result request reaches the bounded command timeout.

A row can establish success through `RESULT="SUCCESS"` or a successful `RESULT_CODE` value, including a nonempty array of success codes. An explicit failing `RESULT` always rejects, even if its code says success.

Validation occurs before local scan-action registration. It does not roll back DataWedge changes and does not establish scanner readiness. For scanner workflows, configure/reset all required plugins with input disabled, await complete success, then separately activate input and verify the active profile/scanner. Keep ownership, lifecycle and cleanup checks. The API does not create profiles, choose decoders, cache configuration or depend on native sessions/diagnostics. See the [broadcast sender-authentication limitations](https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25) when assessing reply authenticity.

For an existing profile, specify `configMode: DataWedgeConfigMode.UPDATE`. DataWedge's default when CONFIG_MODE is omitted is OVERWRITE, which resets settings and can remove app associations even if a later parameter fails. Complete-result validation detects failure but cannot undo that reset. This option does not change the underlying configuration-mode default. See [DataWedge SET_CONFIG](https://techdocs.zebra.com/datawedge/15-0/guide/api/setconfig/).
