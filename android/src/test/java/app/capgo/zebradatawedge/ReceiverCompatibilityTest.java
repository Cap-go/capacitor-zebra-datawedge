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
}
