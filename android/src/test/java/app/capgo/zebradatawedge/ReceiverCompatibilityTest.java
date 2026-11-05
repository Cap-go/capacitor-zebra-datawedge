package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import com.getcapacitor.JSObject;
import org.junit.Test;
import org.robolectric.annotation.Config;

/** Exercise onReceive itself, not the trusted-sender injection used by transport fixtures. */
@Config(sdk = { 24, 33, 34 })
public class ReceiverCompatibilityTest extends DataWedgeTestSupport {

    @Test
    public void realReceiverProcessesCorrelatedResultsAcrossSupportedAndroidVersions() {
        Call call = new Call(new JSObject());
        plugin.getDatawedgeStatus(call);
        Intent command = lastBroadcast();
        application.sendBroadcast(resultFor(command).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled"));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, call.completions);
        assertNull(call.error);
        assertTrue(call.result.getBoolean("isEnabled", false));
    }

    @Test
    public void realReceiverAcceptsIdentifierFreeTypedRepliesWhenSenderIdentityIsUnavailable() {
        Call call = new Call(new JSObject());
        plugin.getDatawedgeStatus(call);
        application.sendBroadcast(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled"));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, call.completions);
        assertNull(call.error);
        assertTrue(call.result.getBoolean("isEnabled", false));
    }

    @Test
    public void realReceiverDoesNotUseUnknownIdentifiersForPayloadFallback() {
        Call call = new Call(new JSObject());
        plugin.getDatawedgeStatus(call);
        application.sendBroadcast(
            new Intent(ZebraDataWedge.RESULT_ACTION)
                .putExtra("COMMAND_IDENTIFIER", "not-the-pending-query")
                .putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled")
        );
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, call.completions);
        application.sendBroadcast(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled"));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, call.completions);
        assertNull(call.error);
    }

    @Test
    public void realReceiverDoesNotGuessBetweenIdentifierFreeQueryOwners() {
        Call first = new Call(new JSObject());
        Call second = new Call(new JSObject());
        plugin.getDatawedgeStatus(first);
        Intent firstCommand = lastBroadcast();
        plugin.getDatawedgeStatus(second);
        application.sendBroadcast(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled"));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, first.completions);
        assertEquals(0, second.completions);
        application.sendBroadcast(resultFor(firstCommand).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled"));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, first.completions);
        assertEquals(0, second.completions);
    }

    @Test
    public void realReceiverStillRequiresTheExpectedPayloadTypeWithoutIdentity() {
        Call call = new Call(new JSObject());
        plugin.getDatawedgeStatus(call);
        application.sendBroadcast(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, true));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, call.completions);
        application.sendBroadcast(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled"));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, call.completions);
        assertTrue(call.result.getBoolean("isEnabled", false));
    }
}
