package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Looper;
import com.getcapacitor.JSObject;
import java.time.Duration;
import org.junit.Test;

public class NativeDiagnosticsTest extends DataWedgeTestSupport {

    private JSObject snapshot() {
        Call call = new Call(new JSObject());
        plugin.getDiagnostics(call);
        assertEquals(1, call.completions);
        return call.result;
    }

    private void enable(int capacity) {
        Call call = new Call(new JSObject().put("enabled", true).put("maxEntries", capacity).put("reset", true));
        plugin.configureDiagnostics(call);
        assertNull(call.error);
    }

    @Test
    public void environmentReadsDoNotBroadcastOrResetAnything() {
        int count = broadcasts().size();
        JSObject first = snapshot();
        assertFalse(first.getBoolean("enabled", true));
        assertEquals(1, first.optInt("diagnosticsVersion"));
        assertEquals(count, broadcasts().size());
        enable(10);
        plugin.getVersionInfo(new Call(new JSObject()));
        String trace = snapshot().optJSONArray("commands").toString();
        assertEquals(trace, snapshot().optJSONArray("commands").toString());
    }

    @Test
    public void installedDataWedgeVersionIsReportedWithoutAServiceCommand() {
        PackageInfo info = new PackageInfo();
        info.packageName = "com.symbol.datawedge";
        info.versionName = "15.0.2";
        shadowOf(application.getPackageManager()).installPackage(info);
        int count = broadcasts().size();
        JSObject result = snapshot();
        assertEquals("installed", result.optJSONObject("dataWedgePackage").optString("status"));
        assertEquals("15.0.2", result.optJSONObject("dataWedgePackage").optString("version"));
        assertEquals(count, broadcasts().size());
    }

    @Test
    public void absentPackageIsDistinguishedFromSuccessfulLookup() {
        assertEquals("notFound", snapshot().optJSONObject("dataWedgePackage").optString("status"));
    }

    @Test
    public void boundedRingRetainsTerminalEventsAndCountsDroppedEntries() {
        enable(2);
        plugin.createProfile(new Call(new JSObject().put("profileName", "secret-profile")));
        plugin.createProfile(new Call(new JSObject().put("profileName", "another-secret")));
        Intent request = lastBroadcast();
        receive(resultFor(request));
        JSObject data = snapshot();
        assertEquals(2, data.optJSONArray("commands").length());
        assertEquals(1, data.optInt("droppedEntries"));
        assertEquals("SUCCESS", data.optJSONArray("commands").optJSONObject(1).optString("outcome"));
        assertFalse(data.toString().contains("secret"));
    }

    @Test
    public void snapshotIsIndependentAndDisablingPreservesPreviousEvents() throws Exception {
        enable(10);
        plugin.getVersionInfo(new Call(new JSObject()));
        JSObject old = snapshot();
        old.optJSONArray("commands").getJSONObject(0).put("outcome", "edited");
        assertEquals("SENT", snapshot().optJSONArray("commands").optJSONObject(0).optString("outcome"));
        plugin.configureDiagnostics(new Call(new JSObject().put("enabled", false)));
        plugin.getActiveProfile(new Call(new JSObject()));
        assertEquals(1, snapshot().optJSONArray("commands").length());
        plugin.configureDiagnostics(new Call(new JSObject().put("enabled", true).put("reset", true)));
        assertEquals(0, snapshot().optJSONArray("commands").length());
    }

    @Test
    public void timeoutAndTransportFailureHaveSanitizedOutcomes() {
        enable(10);
        plugin.getVersionInfo(new Call(new JSObject()));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals("TIMEOUT", snapshot().optJSONArray("commands").optJSONObject(1).optString("outcome"));
        transportContext = new android.content.ContextWrapper(application) {
            @Override
            public void sendBroadcast(Intent intent) {
                throw new IllegalStateException("secret-payload");
            }

            @Override
            public void sendOrderedBroadcast(Intent intent, String permission) {
                throw new IllegalStateException("secret-payload");
            }
        };
        plugin.getActiveProfile(new Call(new JSObject()));
        String trace = snapshot().toString();
        assertTrue(trace.contains("TRANSPORT_ERROR"));
        assertFalse(trace.contains("secret-payload"));
    }

    @Test
    public void capacityIsValidatedBeforeEnablingTracing() {
        for (int capacity : new int[] { 0, 1001 }) {
            Call call = new Call(new JSObject().put("enabled", true).put("maxEntries", capacity));
            plugin.configureDiagnostics(call);
            assertEquals("PARAMETER_INVALID", call.error);
        }
        assertFalse(snapshot().getBoolean("enabled", true));
    }

    @Test
    public void failedCommandHasOneSanitizedTerminalOutcome() {
        enable(10);
        Call call = new Call(new JSObject().put("profileName", "secret-profile"));
        plugin.createProfile(call);
        android.os.Bundle info = new android.os.Bundle();
        info.putString("RESULT_CODE", "OPERATION_NOT_ALLOWED");
        receive(resultFor(lastBroadcast()).putExtra("RESULT", "FAILURE").putExtra("RESULT_INFO", info));
        assertEquals("OPERATION_NOT_ALLOWED", call.error);
        assertEquals(1, call.completions);
        assertEquals(2, snapshot().optJSONArray("commands").length());
        assertEquals("FAILURE", snapshot().optJSONArray("commands").optJSONObject(1).optString("outcome"));
        assertFalse(snapshot().toString().contains("secret-profile"));
    }

    @Test
    public void parserFailureIsNotRecordedAsSuccess() {
        enable(10);
        transportContext = new android.content.ContextWrapper(application) {
            @Override
            public Intent registerReceiver(android.content.BroadcastReceiver receiver, android.content.IntentFilter filter, int flags) {
                throw new IllegalStateException("secret-registration-error");
            }

            @Override
            public Intent registerReceiver(
                android.content.BroadcastReceiver receiver,
                android.content.IntentFilter filter,
                String permission,
                android.os.Handler scheduler,
                int flags
            ) {
                throw new IllegalStateException("secret-registration-error");
            }
        };
        org.json.JSONArray plugins = new org.json.JSONArray();
        plugins.put(
            new JSObject()
                .put("pluginName", "INTENT")
                .put("paramList", new JSObject().put("intent_output_enabled", "true").put("intent_action", "secret.scan.action"))
        );
        Call call = new Call(new JSObject().put("profileName", profile).put("pluginConfigs", plugins));
        plugin.setConfig(call);
        receive(resultFor(lastBroadcast()));
        assertEquals(1, call.completions);
        assertNotNull(call.error);
        String trace = snapshot().toString();
        assertTrue(trace.contains("INVALID_RESPONSE"));
        assertFalse(trace.contains("SUCCESS"));
        assertFalse(trace.contains("secret"));
    }
}
