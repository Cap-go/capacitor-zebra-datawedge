import type { PluginListenerHandle } from '@capacitor/core';

import type { BeginScanSessionOptions, ScanSession, ZebraDataWedgePlugin } from './definitions';

const HEARTBEAT_INTERVAL_MS = 1000;

interface ManagedSession {
  session?: ScanSession;
  stopped: boolean;
  closedId?: string;
  heartbeat?: ReturnType<typeof setTimeout>;
  absoluteTimer?: ReturnType<typeof setTimeout>;
  listener?: PluginListenerHandle;
  detach: () => void;
}

/** Internal wrapper: ordinary consumers should only need begin/end, not a heartbeat loop. */
export function withManagedScanSessions(native: ZebraDataWedgePlugin): ZebraDataWedgePlugin {
  let current: ManagedSession | undefined;
  const pendingEnds = new Map<string, { request: Promise<{ confirmed: boolean }>; explicit: boolean }>();

  function finishNativeSession(sessionId: string, explicit = false): Promise<{ confirmed: boolean }> {
    const existing = pendingEnds.get(sessionId);
    if (existing) {
      if (explicit && existing.explicit) {
        return Promise.reject(
          Object.assign(new Error('Cleanup is already awaiting acknowledgement.'), { code: 'SESSION_BUSY' }),
        );
      }
      if (explicit) existing.explicit = true;
      return existing.request;
    }
    const request = Promise.resolve().then(() => native.endScanSession({ sessionId }));
    pendingEnds.set(sessionId, { request, explicit });
    const settled = () => {
      if (pendingEnds.get(sessionId)?.request === request) pendingEnds.delete(sessionId);
    };
    // Attach both handlers without creating an ignored rejecting finally-promise.
    void request.then(settled, settled);
    return request;
  }

  const stop = (owner: ManagedSession) => {
    owner.stopped = true;
    clearTimeout(owner.heartbeat);
    clearTimeout(owner.absoluteTimer);
    owner.detach();
    void owner.listener?.remove().catch(() => undefined);
    owner.listener = undefined;
    if (current === owner) current = undefined;
  };

  const abandon = (owner: ManagedSession) => {
    stop(owner);
    // Best effort only: the caller must explicitly await end before camera/new-session handoff.
    if (owner.session) void finishNativeSession(owner.session.sessionId).catch(() => undefined);
  };

  const scheduleHeartbeat = (owner: ManagedSession) => {
    owner.heartbeat = setTimeout(async () => {
      if (owner.stopped || !owner.session) return;
      try {
        const state = await native.renewScanSession({ sessionId: owner.session.sessionId });
        if (owner.stopped) return;
        if (!state.active) {
          abandon(owner);
          return;
        }
        // Schedule only after acknowledgement: one renewal in flight, no bridge backlog.
        scheduleHeartbeat(owner);
      } catch {
        if (!owner.stopped) abandon(owner);
      }
    }, HEARTBEAT_INTERVAL_MS);
  };

  const begin = async (options: BeginScanSessionOptions): Promise<ScanSession> => {
    if (current) throw Object.assign(new Error('Scanner session is unavailable or busy.'), { code: 'SESSION_BUSY' });
    if (typeof document !== 'undefined' && document.hidden) {
      throw Object.assign(new Error('Cannot begin a scanner session in a hidden document.'), {
        code: 'SESSION_INACTIVE',
      });
    }
    const owner: ManagedSession = { stopped: false, detach: () => undefined };
    current = owner;
    const hide = () => abandon(owner);
    const visibility = () => {
      if (document.hidden) hide();
    };
    owner.detach = () => {
      if (typeof window !== 'undefined') window.removeEventListener('pagehide', hide);
      if (typeof document !== 'undefined') document.removeEventListener('visibilitychange', visibility);
    };
    if (typeof window !== 'undefined') window.addEventListener('pagehide', hide);
    if (typeof document !== 'undefined') document.addEventListener('visibilitychange', visibility);
    try {
      // Subscribe before begin so native closure cannot race the returned token.
      const listener = await native.addListener('scanSessionClosed', (event) => {
        if (!owner.session) owner.closedId = event.sessionId;
        else if (event.sessionId === owner.session.sessionId) stop(owner);
      });
      if (owner.stopped) {
        await listener.remove();
        throw Object.assign(new Error('Session cancelled before creation.'), { code: 'SESSION_CANCELLED' });
      }
      owner.listener = listener;
      const session = await native.beginScanSession(options);
      owner.session = session;
      if (owner.stopped || owner.closedId === session.sessionId) {
        stop(owner);
        let cleanupConfirmed = false;
        try {
          cleanupConfirmed = (await finishNativeSession(session.sessionId)).confirmed === true;
        } catch {
          /* The caller can retry end with the exposed token. */
        }
        throw Object.assign(new Error('Session closed during creation.'), {
          code: 'SESSION_CANCELLED',
          sessionId: session.sessionId,
          cleanupConfirmed,
        });
      }
      // Bound JS resources even if native notifications or a renewal reply never arrive.
      owner.absoluteTimer = setTimeout(() => abandon(owner), session.timeoutMs);
      if (options.autoRenew !== false) scheduleHeartbeat(owner);
      return session;
    } catch (error) {
      stop(owner);
      throw error;
    }
  };

  const end = (options: { sessionId: string }) => {
    if (current?.session?.sessionId === options.sessionId) stop(current);
    return finishNativeSession(options.sessionId, true);
  };

  // Capacitor's registered plugin is itself a lazy proxy. Preserve every other method unchanged.
  return new Proxy(native, {
    get(target, property) {
      if (property === 'beginScanSession') return begin;
      if (property === 'endScanSession') return end;
      return Reflect.get(target, property, target);
    },
  });
}
