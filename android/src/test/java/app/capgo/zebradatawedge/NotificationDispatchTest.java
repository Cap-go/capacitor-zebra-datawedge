package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import com.getcapacitor.JSObject;
import java.time.Duration;
import org.junit.Test;

public class NotificationDispatchTest extends DataWedgeTestSupport {

    @Test
    public void notificationDispatchCompletesWithoutACommandAcknowledgement() {
        for (boolean register : new boolean[] { true, false }) {
            Call call = new Call(new JSObject().put("notificationType", "SCANNER_STATUS"));
            if (register) plugin.registerForNotification(call);
            else plugin.unRegisterForNotification(call);
            assertTrue(
                lastBroadcast().hasExtra(
                    register ? ZebraDataWedge.CMD_REGISTER_FOR_NOTIFICATION : ZebraDataWedge.CMD_UNREGISTER_FOR_NOTIFICATION
                )
            );
            assertEquals(1, call.completions);
            assertNull(call.error);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
            assertEquals(1, call.completions);
        }
    }

    @Test
    public void notificationDispatchExceptionsRejectOnce() {
        transportContext = new android.content.ContextWrapper(application) {
            @Override
            public void sendBroadcast(Intent intent) {
                throw new IllegalStateException("fixture transport failure");
            }
        };
        for (boolean register : new boolean[] { true, false }) {
            Call call = new Call(new JSObject().put("notificationType", "SCANNER_STATUS"));
            if (register) plugin.registerForNotification(call);
            else plugin.unRegisterForNotification(call);
            assertEquals("DATAWEDGE_TRANSPORT", call.error);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
            assertEquals(1, call.completions);
        }
    }
}
