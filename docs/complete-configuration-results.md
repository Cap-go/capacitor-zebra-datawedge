# Complete configuration results

DataWedge's default LAST_RESULT reply can report success for the last plugin even if an earlier plugin failed. Existing setConfig calls retain that behavior for compatibility. To validate a multi-plugin update, opt in after checking native support:

```ts
import { ZebraDataWedge, DataWedgeConfigMode, DataWedgePlugin } from '@capgo/capacitor-zebra-datawedge';

const supported = await ZebraDataWedge.getConfigurationCapabilities()
  .then((capabilities) => capabilities.completeConfigResults)
  .catch(() => false);
// Older native binaries: retain separately acknowledged per-plugin updates.
if (supported) {
  await ZebraDataWedge.setConfig({
    profileName: 'WarehousePicking',
    configMode: DataWedgeConfigMode.UPDATE,
    completeResult: true,
    pluginConfigs: [
      {
        pluginName: DataWedgePlugin.BARCODE,
        paramList: { configure_all_scanners: 'true', scanner_input_enabled: 'false' },
      },
      { pluginName: DataWedgePlugin.KEYSTROKE, paramList: { keystroke_output_enabled: 'false' } },
    ],
  });
}
```

The capability read is local/read-only, and requires an updated native Android app. The option sends SEND_RESULT=COMPLETE_RESULT. Resolution requires a correlated RESULT_LIST with valid success rows covering every requested plugin. A bare LAST_RESULT acknowledgement cannot satisfy it; unrelated identifiers or identifier-free mutations cannot complete it. Missing/malformed rows or result codes reject with CONFIG_RESULT_INCOMPLETE/CONFIG_RESULT_INVALID or the sanitized DataWedge failure code. An outer FAILURE rejects immediately. Missing full-list replies retain the existing bounded command timeout.

Validation occurs before local scan-action registration. It does not roll back DataWedge changes and does not establish scanner readiness. For scanner workflows, configure/reset all required plugins with input disabled, await complete success, then separately activate input and verify the active profile/scanner. Keep ownership, lifecycle and cleanup checks. The API does not create profiles, choose decoders, cache configuration or depend on native sessions/diagnostics. Receiver authentication limitations are unchanged (#25).

For an existing profile, specify `configMode: DataWedgeConfigMode.UPDATE`. DataWedge's default when CONFIG_MODE is omitted is OVERWRITE, which resets settings and can remove app associations even if a later parameter fails. Complete-result validation detects failure but cannot undo that reset. This option does not change the underlying configuration-mode default. See [DataWedge SET_CONFIG](https://techdocs.zebra.com/datawedge/15-0/guide/api/setconfig/).
