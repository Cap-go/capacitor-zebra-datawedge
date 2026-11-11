import { registerPlugin } from '@capacitor/core';
import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';

import type { ScanSession, ScanSessionState, ZebraDataWedgePlugin } from '../src/definitions';
import { withManagedScanSessions } from '../src/scan-session-controller';

describe('built-in session heartbeat', () => {
  const session: ScanSession = {
    sessionId: 'one',
    profileName: 'Picking',
    intentAction: 'scan.one',
    timeoutMs: 300000,
    leaseTimeoutMs: 10000,
  };
  let timers: Map<number, { callback: () => void; delay: number }>;
  let windowEvents: EventTarget;
  let documentEvents: EventTarget & { hidden: boolean };
  let native: ZebraDataWedgePlugin;
  let plugin: ZebraDataWedgePlugin;
  let closed: (event: ScanSessionState) => void;
  let remove: ReturnType<typeof mock>;
  let begin: ReturnType<typeof mock>;
  let renew: ReturnType<typeof mock>;
  let end: ReturnType<typeof mock>;
  let originalDocument: PropertyDescriptor | undefined;
  let originalWindow: PropertyDescriptor | undefined;

  const flush = async () => {
    for (let i = 0; i < 12; i++) await Promise.resolve();
  };
  const tickHeartbeat = async () => {
    const entry = [...timers.entries()].find(([, timer]) => timer.delay === 1000);
    expect(entry).toBeDefined();
    timers.delete(entry![0]);
    entry![1].callback();
    await flush();
  };

  beforeEach(() => {
    timers = new Map();
    let next = 0;
    spyOn(globalThis, 'setTimeout').mockImplementation(((callback: () => void, delay: number) => {
      timers.set(++next, { callback, delay });
      return next;
    }) as typeof setTimeout);
    spyOn(globalThis, 'clearTimeout').mockImplementation(((id: number) => {
      timers.delete(id);
    }) as typeof clearTimeout);
    windowEvents = new EventTarget();
    documentEvents = Object.assign(new EventTarget(), { hidden: false });
    originalDocument = Object.getOwnPropertyDescriptor(globalThis, 'document');
    originalWindow = Object.getOwnPropertyDescriptor(globalThis, 'window');
    Object.defineProperty(globalThis, 'document', { configurable: true, value: documentEvents });
    Object.defineProperty(globalThis, 'window', { configurable: true, value: windowEvents });
    remove = mock(async () => undefined);
    begin = mock(async () => session);
    renew = mock(async () => ({ sessionId: session.sessionId, active: true, remainingMs: 10000 }));
    end = mock(async () => ({ confirmed: true }));
    native = {
      beginScanSession: begin,
      renewScanSession: renew,
      endScanSession: end,
      addListener: mock(async (_name: string, callback: typeof closed) => {
        closed = callback;
        return { remove };
      }),
      getPluginVersion: mock(async () => ({ version: 'test' })),
    } as unknown as ZebraDataWedgePlugin;
    plugin = withManagedScanSessions(native);
  });

  afterEach(() => {
    mock.restore();
    if (originalDocument) Object.defineProperty(globalThis, 'document', originalDocument);
    else Reflect.deleteProperty(globalThis, 'document');
    if (originalWindow) Object.defineProperty(globalThis, 'window', originalWindow);
    else Reflect.deleteProperty(globalThis, 'window');
  });

  it('renews automatically and preserves unrelated lazy plugin methods', async () => {
    expect(await plugin.beginScanSession({ profileName: 'Picking' })).toEqual(session);
    await tickHeartbeat();
    expect(renew).toHaveBeenCalledWith({ sessionId: 'one' });
    expect([...timers.values()].filter((timer) => timer.delay === 1000)).toHaveLength(1);
    expect(await plugin.getPluginVersion()).toEqual({ version: 'test' });
    await plugin.endScanSession({ sessionId: 'one' });
    expect(timers.size).toBe(0);
    expect(remove).toHaveBeenCalledTimes(1);
    windowEvents.dispatchEvent(new Event('pagehide'));
    documentEvents.hidden = true;
    documentEvents.dispatchEvent(new Event('visibilitychange'));
    expect(end).toHaveBeenCalledTimes(1);
  });

  it('never queues a second renewal while a reply is pending or restarts after end', async () => {
    let reply!: (value: ScanSessionState) => void;
    renew.mockImplementation(
      () =>
        new Promise((resolve) => {
          reply = resolve;
        }),
    );
    await plugin.beginScanSession({ profileName: 'Picking' });
    await tickHeartbeat();
    expect([...timers.values()].some((timer) => timer.delay === 1000)).toBe(false);
    await plugin.endScanSession({ sessionId: 'one' });
    reply({ sessionId: 'one', active: true, remainingMs: 10000 });
    await flush();
    expect(timers.size).toBe(0);
  });

  it('stops on native closure without treating it as acknowledged end', async () => {
    await plugin.beginScanSession({ profileName: 'Picking' });
    closed({ sessionId: 'other', active: false, remainingMs: 0 });
    expect(timers.size).toBe(2);
    closed({ sessionId: 'one', active: false, remainingMs: 0, reason: 'heartbeat_expired' });
    expect(timers.size).toBe(0);
    expect(end).not.toHaveBeenCalled();
    await plugin.endScanSession({ sessionId: 'one' });
    expect(end).toHaveBeenCalled();
  });

  it('stops and attempts cleanup when renewal rejects, without an unhandled rejection', async () => {
    renew.mockRejectedValue(new Error('expired'));
    end.mockRejectedValue(new Error('cleanup unconfirmed'));
    await plugin.beginScanSession({ profileName: 'Picking' });
    await tickHeartbeat();
    expect(timers.size).toBe(0);
    expect(end).toHaveBeenCalledWith({ sessionId: 'one' });
  });

  it('cleans up an inactive renewal response', async () => {
    renew.mockResolvedValue({ sessionId: 'one', active: false, remainingMs: 0 });
    await plugin.beginScanSession({ profileName: 'Picking' });
    await tickHeartbeat();
    expect(timers.size).toBe(0);
    expect(end).toHaveBeenCalled();
  });

  it.each(['visibilitychange', 'pagehide'])('handles %s and never auto-resumes', async (name) => {
    await plugin.beginScanSession({ profileName: 'Picking' });
    documentEvents.hidden = true;
    (name === 'pagehide' ? windowEvents : documentEvents).dispatchEvent(new Event(name));
    await flush();
    expect(timers.size).toBe(0);
    expect(end).toHaveBeenCalledWith({ sessionId: 'one' });
    documentEvents.hidden = false;
    documentEvents.dispatchEvent(new Event('visibilitychange'));
    expect(begin).toHaveBeenCalledTimes(1);
  });

  it('supports expert-managed renewal without a heartbeat timer', async () => {
    await plugin.beginScanSession({ profileName: 'Picking', autoRenew: false });
    expect([...timers.values()].some((timer) => timer.delay === 1000)).toBe(false);
    await plugin.renewScanSession({ sessionId: 'one' });
    expect(renew).toHaveBeenCalledTimes(1);
  });

  it('releases JS resources on the absolute deadline even if a bridge reply hangs', async () => {
    renew.mockImplementation(() => new Promise(() => undefined));
    await plugin.beginScanSession({ profileName: 'Picking' });
    await tickHeartbeat();
    const deadline = [...timers.values()].find((timer) => timer.delay === session.timeoutMs)!;
    deadline.callback();
    await flush();
    expect(timers.size).toBe(0);
    expect(end).toHaveBeenCalled();
  });

  it('rejects concurrent begins without replacing the active owner', async () => {
    await plugin.beginScanSession({ profileName: 'Picking' });
    await expect(plugin.beginScanSession({ profileName: 'Other' })).rejects.toMatchObject({ code: 'SESSION_BUSY' });
    expect(begin).toHaveBeenCalledTimes(1);
    expect(timers.size).toBe(2);
  });

  it('keeps a matching controller alive when end is called with a wrong token', async () => {
    await plugin.beginScanSession({ profileName: 'Picking' });
    await plugin.endScanSession({ sessionId: 'wrong' });
    expect(timers.size).toBe(2);
  });

  it('propagates cleanup rejection and permits explicit retry without renewing', async () => {
    await plugin.beginScanSession({ profileName: 'Picking' });
    end.mockRejectedValueOnce(new Error('unconfirmed'));
    await expect(plugin.endScanSession({ sessionId: 'one' })).rejects.toThrow('unconfirmed');
    expect(timers.size).toBe(0);
    expect(await plugin.endScanSession({ sessionId: 'one' })).toEqual({ confirmed: true });
  });

  it('releases listener and reservation after begin fails', async () => {
    begin.mockRejectedValueOnce(new Error('ownership'));
    await expect(plugin.beginScanSession({ profileName: 'Picking' })).rejects.toThrow('ownership');
    expect(remove).toHaveBeenCalledTimes(1);
    expect(timers.size).toBe(0);
    await plugin.beginScanSession({ profileName: 'Picking' });
  });

  it('ends a late token when the page disappears during native begin', async () => {
    let reply!: (value: ScanSession) => void;
    begin.mockImplementation(
      () =>
        new Promise((resolve) => {
          reply = resolve;
        }),
    );
    const pending = plugin.beginScanSession({ profileName: 'Picking' });
    await flush();
    windowEvents.dispatchEvent(new Event('pagehide'));
    reply(session);
    await expect(pending).rejects.toMatchObject({ code: 'SESSION_CANCELLED' });
    expect(end).toHaveBeenCalledWith({ sessionId: 'one' });
    expect(timers.size).toBe(0);
  });

  it('does not return a session closed before its begin response', async () => {
    let reply!: (value: ScanSession) => void;
    begin.mockImplementation(
      () =>
        new Promise((resolve) => {
          reply = resolve;
        }),
    );
    const pending = plugin.beginScanSession({ profileName: 'Picking' });
    await flush();
    closed({ sessionId: 'one', active: false, remainingMs: 0, reason: 'background' });
    reply(session);
    await expect(pending).rejects.toMatchObject({ code: 'SESSION_CANCELLED' });
    expect(timers.size).toBe(0);
  });

  it('does not create a native session when internal listener registration fails', async () => {
    native.addListener = mock(async () => {
      throw new Error('listener unavailable');
    }) as typeof native.addListener;
    await expect(plugin.beginScanSession({ profileName: 'Picking' })).rejects.toThrow('listener unavailable');
    expect(begin).not.toHaveBeenCalled();
    expect(timers.size).toBe(0);
  });

  it('refuses hidden-page creation before registering listeners', async () => {
    documentEvents.hidden = true;
    await expect(plugin.beginScanSession({ profileName: 'Picking' })).rejects.toMatchObject({
      code: 'SESSION_INACTIVE',
    });
    expect(native.addListener).not.toHaveBeenCalled();
    expect(begin).not.toHaveBeenCalled();
  });
  it('works through Capacitor registration rather than requiring a plain object implementation', async () => {
    plugin = withManagedScanSessions(
      registerPlugin<ZebraDataWedgePlugin>('ManagedSessionRegistrationTest', {
        web: () => native,
      }),
    );
    await plugin.beginScanSession({ profileName: 'Picking' });
    await tickHeartbeat();
    expect(renew).toHaveBeenCalledWith({ sessionId: 'one' });
    await plugin.endScanSession({ sessionId: 'one' });
    expect(timers.size).toBe(0);
    expect(await plugin.getPluginVersion()).toEqual({ version: 'test' });
  });

  it('does not begin after page hiding during internal listener registration', async () => {
    let reply!: (value: { remove: typeof remove }) => void;
    native.addListener = mock(
      () =>
        new Promise((resolve) => {
          reply = resolve;
        }),
    ) as typeof native.addListener;
    const pending = plugin.beginScanSession({ profileName: 'Picking' });
    windowEvents.dispatchEvent(new Event('pagehide'));
    reply({ remove });
    await expect(pending).rejects.toMatchObject({ code: 'SESSION_CANCELLED' });
    expect(begin).not.toHaveBeenCalled();
    expect(remove).toHaveBeenCalledTimes(1);
    expect(timers.size).toBe(0);
  });

  it('does not leak a rejection when native listener removal fails', async () => {
    remove.mockRejectedValueOnce(new Error('bridge gone'));
    await plugin.beginScanSession({ profileName: 'Picking' });
    await plugin.endScanSession({ sessionId: 'one' });
    await flush();
    expect(timers.size).toBe(0);
  });
  it('shares automatic lifecycle cleanup with an explicit end until its acknowledgement', async () => {
    let acknowledge!: (value: { confirmed: boolean }) => void;
    end.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          acknowledge = resolve;
        }),
    );
    await plugin.beginScanSession({ profileName: 'Picking' });
    windowEvents.dispatchEvent(new Event('pagehide'));
    const explicit = plugin.endScanSession({ sessionId: 'one' });
    await flush();
    expect(end).toHaveBeenCalledTimes(1);
    acknowledge({ confirmed: true });
    expect(await explicit).toEqual({ confirmed: true });
  });

  it.each([false, true])('rejects a second explicit end while cleanup is pending (automatic=%s)', async (automatic) => {
    let acknowledge!: (value: { confirmed: boolean }) => void;
    end.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          acknowledge = resolve;
        }),
    );
    await plugin.beginScanSession({ profileName: 'Picking' });
    if (automatic) windowEvents.dispatchEvent(new Event('pagehide'));
    const first = plugin.endScanSession({ sessionId: 'one' });
    await expect(plugin.endScanSession({ sessionId: 'one' })).rejects.toMatchObject({ code: 'SESSION_BUSY' });
    await flush();
    expect(end).toHaveBeenCalledTimes(1);
    acknowledge({ confirmed: true });
    expect(await first).toEqual({ confirmed: true });
    expect(await plugin.endScanSession({ sessionId: 'one' })).toEqual({ confirmed: true });
  });

  it('exposes a late cancelled token when cleanup fails so end remains retryable', async () => {
    let reply!: (value: ScanSession) => void;
    begin.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          reply = resolve;
        }),
    );
    end.mockRejectedValueOnce(new Error('disable unconfirmed'));
    const pending = plugin.beginScanSession({ profileName: 'Picking' });
    await flush();
    windowEvents.dispatchEvent(new Event('pagehide'));
    reply(session);
    await expect(pending).rejects.toMatchObject({
      code: 'SESSION_CANCELLED',
      sessionId: 'one',
      cleanupConfirmed: false,
    });
    expect(await plugin.endScanSession({ sessionId: 'one' })).toEqual({ confirmed: true });
    expect(end).toHaveBeenCalledTimes(2);
    expect(timers.size).toBe(0);
  });
});
