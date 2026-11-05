import type { PluginListenerHandle } from '@capacitor/core';

import type { RegisterForNotificationOptions, ZebraNotificationEvent } from './definitions';

type NativeOptions = Omit<RegisterForNotificationOptions, 'callback'>;

interface NotificationBridge {
  registerForNotification(options: NativeOptions): Promise<void>;
  unRegisterForNotification(options: NativeOptions): Promise<void>;
  addListener(event: 'notification', listener: (event: ZebraNotificationEvent) => void): Promise<PluginListenerHandle>;
}

interface Registration {
  handle: PluginListenerHandle;
  active: boolean;
}

/** Keep listener installation and native dispatch in one ordered operation per registration key. */
export function createZebraNotification(native: NotificationBridge): {
  registerForNotification(options: RegisterForNotificationOptions): Promise<void>;
  /** Unregister and remove the local listener; failed cleanup retains an inactive handle for retry. */
  unRegisterForNotification(options: NativeOptions): Promise<void>;
} {
  const registrations = new Map<string, Registration>();
  const pendingOperations = new Map<string, Promise<void>>();
  const keyFor = (options: NativeOptions) =>
    `${options.appName ?? ''}:${options.intentAction ?? ''}:${options.notificationType}`;

  function serialize(key: string, operation: () => Promise<void>): Promise<void> {
    const previous = pendingOperations.get(key) ?? Promise.resolve();
    const pending = previous.catch(() => undefined).then(operation);
    pendingOperations.set(key, pending);
    function clear() {
      if (pendingOperations.get(key) === pending) pendingOperations.delete(key);
    }
    // Both handlers resolve: cleaning the queue must not create an unhandled rejection.
    void pending.then(clear, clear);
    return pending;
  }

  return {
    registerForNotification(options: RegisterForNotificationOptions): Promise<void> {
      const nativeOptions = {
        appName: options.appName,
        intentAction: options.intentAction,
        notificationType: options.notificationType,
      };
      const callback = options.callback;
      const key = keyFor(nativeOptions);
      return serialize(key, async () => {
        if (!callback) {
          await native.registerForNotification(nativeOptions);
          return;
        }
        const previous = registrations.get(key);
        const previousActive = previous?.active ?? false;
        let active = true;
        // Subscribe first: DataWedge may emit status before native dispatch resolves.
        const handle = await native.addListener('notification', (event) => {
          if (active && event.notificationType === nativeOptions.notificationType) callback(event);
        });
        const registration = {
          handle,
          get active() {
            return active;
          },
          set active(value: boolean) {
            active = value;
          },
        };
        if (previous) previous.active = false;
        registrations.set(key, registration);
        try {
          await native.registerForNotification(nativeOptions);
        } catch (error) {
          registration.active = false;
          if (previous) {
            previous.active = previousActive;
            registrations.set(key, previous);
          } else {
            registrations.delete(key);
          }
          // Preserve the dispatch error even if listener cleanup itself fails.
          await handle.remove().catch(() => undefined);
          throw error;
        }
        if (previous) {
          // An inactive old callback cannot fire even if removing its native handle fails.
          await previous.handle.remove().catch(() => undefined);
        }
      });
    },
    unRegisterForNotification(options: NativeOptions): Promise<void> {
      const key = keyFor(options);
      return serialize(key, async () => {
        await native.unRegisterForNotification(options);
        const registration = registrations.get(key);
        if (registration) {
          registration.active = false;
          // Preserve the wrapper's cleanup contract: retain an inactive handle if removal fails,
          // so a later unregister can retry instead of forgetting the still-installed listener.
          await registration.handle.remove();
          registrations.delete(key);
        }
      });
    },
  };
}
