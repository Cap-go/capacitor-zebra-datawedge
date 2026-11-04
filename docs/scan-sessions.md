# Native scan sessions

Use a native session for a finite scan flow that must release scanner input on cancel, pause or a native deadline. Existing low-level methods remain available for applications that manage DataWedge themselves.

The caller supplies the existing profile name. The profile must already be associated exclusively with the current Android application; no suffix or manufacturer match is required. Beginning a session queries that association but does not create or overwrite a profile, enable DataWedge or start the scanner. Avoid overlapping GET_CONFIG queries during this validation because DataWedge can omit command identifiers in its replies.

```ts
const session = await ZebraDataWedge.beginScanSession({
  profileName: 'WarehousePicking',
  timeoutMs: 35000,
});

try {
  // Use session.sessionId with setConfig calls targeting this profile.
  // Configure INTENT output to session.intentAction and broadcast delivery,
  // disable keystroke output, select/enable the intended scanner and decoders,
  // and verify profile activation and scanner readiness before triggering.
  // Subscribe to the existing scan event before starting the scanner.
  await ZebraDataWedge.triggerScanSession({ sessionId: session.sessionId });
  // Wait for a scan event whose intentAction matches session.intentAction.
} finally {
  await ZebraDataWedge.endScanSession({ sessionId: session.sessionId });
}
```

`triggerScanSession` checks the active profile and then waits for the software-trigger acknowledgement. It does not return the decoded barcode: scan data continues through the `scan` event. The existing `softScanTrigger` return contract is unchanged.

Ending a session cancels its pending calls, unregisters its scan action and disables Barcode Input in that profile, using the required scanner selector. The profile remains enabled and associated: disabling the whole profile could activate default-profile keystroke input. An acknowledged end returns `{ confirmed: true }`. A failed or unanswered end leaves the session closing and prevents a new session; retry end with the same token. Do not hand off to a camera scanner until cleanup is confirmed. Repeating a completed end is idempotent; a concurrent end while acknowledgement is pending rejects `SESSION_BUSY`.

Pause, destroy and deadline handling do not depend on JavaScript running. They attempt targeted disable and cancel owned pending calls. A global STOP is sent only while this application is foreground and its session triggered scanning, to avoid interrupting another foreground application. These emergency broadcasts are best effort, not proof of hardware release.

Once a profile has been enrolled in a session, `setConfig` requires the matching active token for it for the rest of that process. Do not mutate an active session profile through the other low-level APIs; session IDs coordinate this plugin, not Android authorization or enterprise policy. Low-level APIs for other profiles keep their existing behavior.

The feature does not persist enrollment or run guessed profile repair at startup. Android can kill a process without lifecycle callbacks. Applications requiring recovery after process death must retain and validate their own recovery policy until a separate persistence contract is implemented. Package-prefix conventions alone are not proof of profile ownership.

The generalized implementation is covered by native intent-fixture tests, including cancellation races and cleanup failure. Real DataWedge firmware, scanner readiness and managed-device policy still require device acceptance testing.
