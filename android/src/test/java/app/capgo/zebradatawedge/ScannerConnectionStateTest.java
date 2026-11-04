package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import com.getcapacitor.JSObject;
import java.time.Duration;
import org.junit.Test;

public class ScannerConnectionStateTest extends DataWedgeTestSupport {

    @Test
    public void scannerEnumerationAcceptsBooleanAndLegacyStringConnectionStates() {
        org.robolectric.shadows.ShadowLog.clear();
        Call call = new Call(new JSObject());
        plugin.enumerateScanners(call);
        java.util.ArrayList<Bundle> scanners = new java.util.ArrayList<>();
        Bundle imager = new Bundle();
        imager.putInt("SCANNER_INDEX", 1);
        imager.putString("SCANNER_IDENTIFIER", "INTERNAL_IMAGER");
        imager.putString("SCANNER_NAME", "2D Barcode Imager");
        imager.putBoolean("SCANNER_CONNECTION_STATE", true);
        scanners.add(imager);
        Bundle bluetooth = new Bundle(imager);
        bluetooth.putString("SCANNER_CONNECTION_STATE", "false");
        scanners.add(bluetooth);
        Bundle disconnected = new Bundle(imager);
        disconnected.putBoolean("SCANNER_CONNECTION_STATE", false);
        scanners.add(disconnected);
        Bundle legacyConnected = new Bundle(imager);
        legacyConnected.putString("SCANNER_CONNECTION_STATE", "true");
        scanners.add(legacyConnected);
        receiveCorrelated(
            new Intent(ZebraDataWedge.RESULT_ACTION).putParcelableArrayListExtra(
                "com.symbol.datawedge.api.RESULT_ENUMERATE_SCANNERS",
                scanners
            )
        );
        assertEquals(1, call.completions);
        assertTrue(call.result.optJSONArray("scanners").optJSONObject(0).optBoolean("connectionState"));
        assertFalse(call.result.optJSONArray("scanners").optJSONObject(1).optBoolean("connectionState"));
        assertFalse(call.result.optJSONArray("scanners").optJSONObject(2).optBoolean("connectionState"));
        assertTrue(call.result.optJSONArray("scanners").optJSONObject(3).optBoolean("connectionState"));
        assertFalse(
            org.robolectric.shadows.ShadowLog.getLogsForTag("Bundle")
                .stream()
                .anyMatch(
                    (log) ->
                        log.throwable instanceof ClassCastException ||
                        log.msg.contains("expected String") ||
                        log.msg.contains("expected Boolean")
                )
        );
    }
}
