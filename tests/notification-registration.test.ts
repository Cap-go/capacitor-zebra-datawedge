import { describe, expect, it } from 'bun:test';

import { DataWedgeNotificationType, type ZebraNotificationEvent } from '../src/definitions';
import { createZebraNotification } from '../src/notification-registration';

function fixture() {
  const listeners = new Set<(event: ZebraNotificationEvent) => void>();
  const operations: string[] = [];
  const state = {
    dispatch: async () => undefined as void,
    unregister: async () => undefined as void,
    listenerError: undefined as Error | undefined,
    removeError: undefined as Error | undefined,
  };
  const bridge = {
    async registerForNotification() {
      operations.push('register');
      await state.dispatch();
    },
    async unRegisterForNotification() {
      operations.push('unregister');
      await state.unregister();
    },
    async addListener(_event: 'notification', listener: (event: ZebraNotificationEvent) => void) {
      operations.push('listen');
      if (state.listenerError) throw state.listenerError;
      listeners.add(listener);
      return {
        async remove() {
          operations.push('remove');
          if (state.removeError) throw state.removeError;
          listeners.delete(listener);
        },
      };
    },
  };
  return {
    api: createZebraNotification(bridge),
    state,
    operations,
    listeners,
    emit(type: string = DataWedgeNotificationType.SCANNER_STATUS) {
      for (const listener of listeners) listener({ notificationType: type, raw: {} });
    },
  };
}

const options = { notificationType: DataWedgeNotificationType.SCANNER_STATUS };

describe('notification callback registration', () => {
  it('installs the listener before dispatch and receives an immediate native notification', async () => {
    const f = fixture();
    let count = 0;
    f.state.dispatch = async () => {
      f.emit();
    };
    await f.api.registerForNotification({
      ...options,
      callback: () => {
        count += 1;
      },
    });
    expect(f.operations).toEqual(['listen', 'register']);
    expect(count).toBe(1);
    f.emit(DataWedgeNotificationType.PROFILE_SWITCH);
    expect(count).toBe(1);
  });
  it('removes the provisional listener if native dispatch fails', async () => {
    const f = fixture();
    const error = new Error('dispatch failed');
    let count = 0;
    f.state.dispatch = async () => {
      throw error;
    };
    await expect(
      f.api.registerForNotification({
        ...options,
        callback: () => {
          count += 1;
        },
      }),
    ).rejects.toBe(error);
    expect(f.listeners.size).toBe(0);
    expect(f.operations).toEqual(['listen', 'register', 'remove']);
    f.emit();
    expect(count).toBe(0);
  });
  it('retains the original dispatch error and disables the callback when removal fails', async () => {
    const f = fixture();
    const error = new Error('dispatch failed');
    let count = 0;
    f.state.dispatch = async () => {
      throw error;
    };
    f.state.removeError = new Error('remove failed');
    await expect(
      f.api.registerForNotification({
        ...options,
        callback: () => {
          count += 1;
        },
      }),
    ).rejects.toBe(error);
    f.emit();
    expect(count).toBe(0);
  });
  it('does not dispatch if listener installation fails', async () => {
    const f = fixture();
    const error = new Error('listener failed');
    f.state.listenerError = error;
    await expect(f.api.registerForNotification({ ...options, callback: () => undefined })).rejects.toBe(error);
    expect(f.operations).toEqual(['listen']);
  });
  it('restores the previous callback if replacement dispatch fails', async () => {
    const f = fixture();
    let oldCount = 0;
    let newCount = 0;
    await f.api.registerForNotification({
      ...options,
      callback: () => {
        oldCount += 1;
      },
    });
    f.state.dispatch = async () => {
      throw new Error('replacement failed');
    };
    await expect(
      f.api.registerForNotification({
        ...options,
        callback: () => {
          newCount += 1;
        },
      }),
    ).rejects.toThrow('replacement failed');
    f.emit();
    expect(oldCount).toBe(1);
    expect(newCount).toBe(0);
    expect(f.listeners.size).toBe(1);
  });
  it('delivers to only the new callback during successful replacement dispatch', async () => {
    const f = fixture();
    let oldCount = 0;
    let newCount = 0;
    await f.api.registerForNotification({
      ...options,
      callback: () => {
        oldCount += 1;
      },
    });
    f.state.dispatch = async () => {
      f.emit();
    };
    await f.api.registerForNotification({
      ...options,
      callback: () => {
        newCount += 1;
      },
    });
    expect(oldCount).toBe(0);
    expect(newCount).toBe(1);
    expect(f.listeners.size).toBe(1);
  });
  it('serializes unregister behind pending registration and removes its listener', async () => {
    const f = fixture();
    let release!: () => void;
    let entered!: () => void;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    const started = new Promise<void>((resolve) => {
      entered = resolve;
    });
    f.state.dispatch = async () => {
      entered();
      await gate;
    };
    const registration = f.api.registerForNotification({ ...options, callback: () => undefined });
    await started;
    const unregister = f.api.unRegisterForNotification(options);
    expect(f.operations).toEqual(['listen', 'register']);
    release();
    await Promise.all([registration, unregister]);
    expect(f.operations).toEqual(['listen', 'register', 'unregister', 'remove']);
    expect(f.listeners.size).toBe(0);
  });
  it('keeps the callback if unregister dispatch fails and allows retry', async () => {
    const f = fixture();
    let count = 0;
    await f.api.registerForNotification({
      ...options,
      callback: () => {
        count += 1;
      },
    });
    f.state.unregister = async () => {
      throw new Error('unregister failed');
    };
    await expect(f.api.unRegisterForNotification(options)).rejects.toThrow('unregister failed');
    f.emit();
    expect(count).toBe(1);
    f.state.unregister = async () => undefined;
    await f.api.unRegisterForNotification(options);
    expect(f.listeners.size).toBe(0);
  });
  it('does not install a callback listener for native-only registration', async () => {
    const f = fixture();
    await f.api.registerForNotification(options);
    expect(f.operations).toEqual(['register']);
  });
  it('retains an inactive handle when unregister cleanup fails and retries removal', async () => {
    const f = fixture();
    let count = 0;
    await f.api.registerForNotification({
      ...options,
      callback: () => {
        count += 1;
      },
    });
    const error = new Error('remove failed');
    f.state.removeError = error;
    await expect(f.api.unRegisterForNotification(options)).rejects.toBe(error);
    f.emit();
    expect(count).toBe(0);
    expect(f.listeners.size).toBe(1);
    f.state.removeError = undefined;
    await f.api.unRegisterForNotification(options);
    expect(f.listeners.size).toBe(0);
  });
  it('does not reactivate an unregistered callback when a later replacement fails', async () => {
    const f = fixture();
    let oldCount = 0;
    let newCount = 0;
    await f.api.registerForNotification({
      ...options,
      callback: () => {
        oldCount += 1;
      },
    });
    f.state.removeError = new Error('remove failed');
    await expect(f.api.unRegisterForNotification(options)).rejects.toThrow('remove failed');
    f.state.removeError = undefined;
    f.state.dispatch = async () => {
      throw new Error('replacement failed');
    };
    await expect(
      f.api.registerForNotification({
        ...options,
        callback: () => {
          newCount += 1;
        },
      }),
    ).rejects.toThrow('replacement failed');
    f.emit();
    expect(oldCount).toBe(0);
    expect(newCount).toBe(0);
    await f.api.unRegisterForNotification(options);
    expect(f.listeners.size).toBe(0);
  });
});
