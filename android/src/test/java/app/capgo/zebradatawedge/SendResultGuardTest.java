package app.capgo.zebradatawedge;

import static org.junit.Assert.*;

import android.content.Intent;
import com.getcapacitor.JSObject;
import org.junit.Test;

public class SendResultGuardTest extends DataWedgeTestSupport {

    @Test
    public void sendCommandDefaultsToLastResultWhenAbsent() {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_CREATE_PROFILE, profile);
        plugin.sendCommandForTest(new Call(new JSObject()), intent, "create-profile");
        assertEquals(ZebraDataWedge.VALUE_LAST_RESULT, lastBroadcast().getStringExtra(ZebraDataWedge.EXTRA_SEND_RESULT));
    }

    @Test
    public void sendCommandPreservesExistingSendResult() {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_CREATE_PROFILE, profile);
        intent.putExtra(ZebraDataWedge.EXTRA_SEND_RESULT, "COMPLETE_RESULT");
        plugin.sendCommandForTest(new Call(new JSObject()), intent, "create-profile");
        assertEquals("COMPLETE_RESULT", lastBroadcast().getStringExtra(ZebraDataWedge.EXTRA_SEND_RESULT));
    }
}
