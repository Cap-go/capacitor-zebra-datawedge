import { expect, it } from 'bun:test';

import { ZebraDataWedgeWeb } from '../src/web';

it('rejects native configuration feature negotiation on web', () => {
  expect(() => new ZebraDataWedgeWeb().getConfigurationCapabilities()).toThrow('only available on Android');
});
