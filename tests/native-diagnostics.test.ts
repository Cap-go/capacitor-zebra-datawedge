import { describe, expect, it } from 'bun:test';

import { ZebraDataWedgeWeb } from '../src/web';

describe('native diagnostics platform availability', () => {
  it('rejects configuration on web', () => {
    expect(() => new ZebraDataWedgeWeb().configureDiagnostics({ enabled: true })).toThrow('only available on Android');
  });
  it('rejects native snapshot reads on web', () => {
    expect(() => new ZebraDataWedgeWeb().getDiagnostics()).toThrow('only available on Android');
  });
});
