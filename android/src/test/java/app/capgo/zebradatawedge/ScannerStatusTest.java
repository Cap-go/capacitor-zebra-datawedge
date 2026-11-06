package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import com.getcapacitor.JSObject;
import java.time.Duration;
import org.junit.Test;

public class ScannerStatusTest extends DataWedgeTestSupport {

    @Test
    public void scannerStatusUsesTheDocumentedResultExtraRatherThanTheCommandName() {
        Call call = new Call(new JSObject());
        plugin.getScannerStatus(call);
        receiveCorrelated(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra("com.symbol.datawedge.api.RESULT_SCANNER_STATUS", "WAITING"));
        assertEquals(1, call.completions);
        assertEquals("WAITING", call.result.getString("status"));
    }
}
