# Native diagnostics

Diagnostics provide native environment information and an opt-in bounded trace of requests sent through the acknowledged-command transport. They do not configure a scanner, enable DataWedge, or prove that enterprise policy allows its APIs.

```ts
await ZebraDataWedge.configureDiagnostics({
  enabled: true,
  maxEntries: 100,
  reset: true,
});

try {
  await ZebraDataWedge.getVersionInfo();
} finally {
  const report = await ZebraDataWedge.getDiagnostics();
  // Display or export the report using the application's own UI/download helpers.
  // Decide which manufacturer/model/package information should be shared.
  await ZebraDataWedge.configureDiagnostics({ enabled: false });
}
```

Tracing is disabled by default. `maxEntries` accepts 1–1000 and defaults to 100. Each dispatch and terminal outcome is an entry. When full, the ring drops the oldest entry, including an earlier dispatch if necessary, so recent terminal outcomes can still be inspected. `droppedEntries` reports how many entries were discarded. Reads do not reset the trace; use `reset: true` explicitly. Disabling tracing preserves previous entries until reset.

The trace contains generated command identifiers, sanitized outcomes and elapsed milliseconds. It excludes barcode values, arbitrary intent/configuration bodies and exception messages. Notification dispatch and the scan-result wait used by `softScanTrigger` use separate transport paths and are excluded from the command trace. Applications can combine existing read-only DataWedge queries independently and retain partial results when some fail.

The environment snapshot reports manufacturer, model, Android version and DataWedge package lookup status/version. `notFound` means Android could not find the package; `unavailable` means the lookup failed for another reason. Check service status and managed-device policy separately. Exact package visibility is declared for DataWedge; Zebra Device Manager is not queried or required.

`getDiagnostics` and `configureDiagnostics` are Android-only and reject as unavailable on iOS/web. Applications provide their own report display and export controls.

Successful delivery of a reply is not sufficient to record command success. The response parser and any configuration-result validation must finish first. Rejected DataWedge/validation results record `FAILURE`; unexpected parsing/registration errors record `INVALID_RESPONSE`, without copying exception messages into the trace.
