import { describe, expect, it } from 'bun:test';

import { ZebraDataWedgeWeb } from '../src/web';

describe('scan session platform availability', () => {
  const plugin = new ZebraDataWedgeWeb();
  it('rejects capability reads on web', () => {
    expect(() => plugin.getScanSessionCapabilities()).toThrow('only available on Android');
  });
  it('rejects session creation on web', () => {
    expect(() => plugin.beginScanSession({ profileName: 'ExistingProfile' })).toThrow('only available on Android');
  });
  it('rejects trigger and end on web', () => {
    expect(() => plugin.renewScanSession({ sessionId: 'example' })).toThrow('only available on Android');
    expect(() => plugin.getScanSessionState({ sessionId: 'example' })).toThrow('only available on Android');
    expect(() => plugin.triggerScanSession({ sessionId: 'example' })).toThrow('only available on Android');
    expect(() => plugin.endScanSession({ sessionId: 'example' })).toThrow('only available on Android');
  });
});
