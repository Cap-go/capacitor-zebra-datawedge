package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowBuild;

/** Exercise the SDK boundary and actual reply receiver, with only outbound delivery doubled. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = { 33, 34 }, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class OrderedDispatchTest {

    private Application application;
    private Context transport;
    private TestPlugin plugin;
    private final List<Intent> outgoing = new ArrayList<>();
    private final List<Boolean> ordered = new ArrayList<>();
    private boolean failDelivery;

    private class TestPlugin extends ZebraDataWedgePlugin {

        @Override
        public Context getContext() {
            return transport;
        }

        void destroy() {
            handleOnDestroy();
        }
    }

    private static class Call extends PluginCall {

        int completions;
        JSObject result;
        String error;

        Call(JSObject data) {
            super(null, "ZebraDataWedge", "dispatch", "dispatch", data);
        }

        @Override
        public void resolve() {
            completions++;
        }

        @Override
        public void resolve(JSObject data) {
            result = data;
            completions++;
        }

        @Override
        public void reject(String message, String code, Exception exception, JSObject data) {
            error = code == null ? message : code;
            completions++;
        }
    }

    @Before
    public void setup() {
        ShadowBuild.setManufacturer("Zebra Technologies");
        application = RuntimeEnvironment.getApplication();
        transport = new ContextWrapper(application) {
            @Override
            public void sendBroadcast(Intent intent) {
                record(intent, false);
            }

            @Override
            public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
                assertNull(receiverPermission);
                record(intent, true);
            }

            private void record(Intent intent, boolean isOrdered) {
                if (failDelivery) throw new IllegalStateException("Transport unavailable");
                assertEquals("com.symbol.datawedge", intent.getPackage());
                assertEquals(ZebraDataWedge.ACTION, intent.getAction());
                outgoing.add(new Intent(intent));
                ordered.add(isOrdered);
            }
        };
        plugin = new TestPlugin();
        plugin.load();
    }

    @After
    public void teardown() {
        plugin.destroy();
        for (boolean isOrdered : ordered) assertEquals(Build.VERSION.SDK_INT >= 34, isOrdered);
    }

    @Test
    public void queryNeedsItsTypedDataWedgeReplyAfterAndroidDelivery() {
        Call query = new Call(new JSObject());
        plugin.getDatawedgeStatus(query);
        Intent command = outgoing.get(outgoing.size() - 1);
        assertNotNull(command.getStringExtra("COMMAND_IDENTIFIER"));
        assertEquals(0, query.completions);
        Intent reply = new Intent(ZebraDataWedge.RESULT_ACTION)
            .putExtra("COMMAND_IDENTIFIER", command.getStringExtra("COMMAND_IDENTIFIER"))
            .putExtra("RESULT", "SUCCESS");
        application.sendBroadcast(reply);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, query.completions); // Delivery/empty acknowledgement cannot replace the query payload.
        reply.putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled");
        application.sendBroadcast(reply);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, query.completions);
        assertTrue(query.result.getBoolean("isEnabled", false));
    }

    @Test
    public void notificationsAndLegacyTriggersUseTheSameDeliveryModeAsQueries() {
        plugin.registerForNotification(new Call(new JSObject().put("notificationType", "SCANNER_STATUS")));
        plugin.unRegisterForNotification(new Call(new JSObject().put("notificationType", "SCANNER_STATUS")));
        plugin.softScanTrigger(new Call(new JSObject().put("intentAction", application.getPackageName() + ".test.scan")));
        assertTrue(outgoing.stream().anyMatch((i) -> i.hasExtra(ZebraDataWedge.CMD_REGISTER_FOR_NOTIFICATION)));
        assertTrue(outgoing.stream().anyMatch((i) -> i.hasExtra(ZebraDataWedge.CMD_UNREGISTER_FOR_NOTIFICATION)));
        assertTrue(outgoing.stream().anyMatch((i) -> i.hasExtra(ZebraDataWedge.CMD_SOFT_SCAN_TRIGGER)));
    }

    @Test
    public void dispatchFailureRejectsThePendingQueryOnce() {
        failDelivery = true;
        Call query = new Call(new JSObject());
        plugin.getDatawedgeStatus(query);
        assertEquals("DATAWEDGE_TRANSPORT", query.error);
        assertEquals(1, query.completions);
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(9));
        assertEquals(1, query.completions);
    }
}
