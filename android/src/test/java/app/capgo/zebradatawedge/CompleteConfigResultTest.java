package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import com.getcapacitor.JSObject;
import java.time.Duration;
import java.util.ArrayList;
import org.junit.Test;

public class CompleteConfigResultTest extends DataWedgeTestSupport {

    private Call completeConfigCall() {
        org.json.JSONArray plugins = new org.json.JSONArray();
        plugins.put(
            new JSObject()
                .put("pluginName", "BARCODE")
                .put("paramList", new JSObject().put("configure_all_scanners", "true").put("scanner_input_enabled", "false"))
        );
        plugins.put(
            new JSObject()
                .put("pluginName", "INTENT")
                .put("paramList", new JSObject().put("intent_action", profile + ".scan").put("intent_output_enabled", "true"))
        );
        Call call = new Call(new JSObject().put("profileName", profile).put("pluginConfigs", plugins).put("completeResult", true));
        plugin.setConfig(call);
        assertEquals("COMPLETE_RESULT", lastBroadcast().getStringExtra("SEND_RESULT"));
        return call;
    }

    private Bundle configResult(String module, String result, String code) {
        Bundle row = new Bundle();
        row.putString("MODULE", module);
        row.putString("RESULT", result);
        if (code != null) row.putString("RESULT_CODE", code);
        return row;
    }

    @Test
    public void completeConfigurationWaitsForAllResultsAndRejectsEarlierPluginFailure() {
        Call call = completeConfigCall();
        Intent command = lastBroadcast();
        receive(resultFor(command));
        assertEquals(0, call.completions);
        java.util.ArrayList<Bundle> results = new java.util.ArrayList<>();
        results.add(configResult("BARCODE", "FAILURE", "PARAMETER_INVALID"));
        results.add(configResult("INTENT", "SUCCESS", null));
        receive(resultFor(command).putParcelableArrayListExtra("RESULT_LIST", results));
        assertEquals("PARAMETER_INVALID", call.error);
        assertEquals(1, call.completions);
    }

    @Test
    public void completeConfigurationRejectsMissingPluginSuccess() {
        Call call = completeConfigCall();
        java.util.ArrayList<Bundle> results = new java.util.ArrayList<>();
        results.add(configResult("INTENT", "SUCCESS", null));
        receive(resultFor(lastBroadcast()).putParcelableArrayListExtra("RESULT_LIST", results));
        assertEquals("CONFIG_RESULT_INCOMPLETE", call.error);
    }

    @Test
    public void completeConfigurationAcceptsAllPluginSuccessesAndIgnoresUnknownIdentifiers() {
        Call call = completeConfigCall();
        Intent command = lastBroadcast();
        java.util.ArrayList<Bundle> results = new java.util.ArrayList<>();
        results.add(configResult("BARCODE", "SUCCESS", null));
        results.add(configResult("INTENT", "SUCCESS", null));
        receive(resultFor(command).putParcelableArrayListExtra("RESULT_LIST", results).putExtra("COMMAND_IDENTIFIER", "unknown"));
        assertEquals(0, call.completions);
        receive(resultFor(command).putParcelableArrayListExtra("RESULT_LIST", results));
        assertNull(call.error);
        assertEquals(1, call.completions);
    }

    @Test
    public void completeConfigurationCannotBeAcknowledgedByAnIdentifierFreeReply() {
        Call call = completeConfigCall();
        java.util.ArrayList<Bundle> results = new java.util.ArrayList<>();
        results.add(configResult("BARCODE", "SUCCESS", null));
        results.add(configResult("INTENT", "SUCCESS", null));
        receive(new Intent(ZebraDataWedge.RESULT_ACTION).putParcelableArrayListExtra("RESULT_LIST", results));
        assertEquals(0, call.completions);
    }

    @Test
    public void configurationCapabilityIsReadOnly() {
        int before = broadcasts().size();
        Call call = new Call(new JSObject());
        plugin.getConfigurationCapabilities(call);
        assertTrue(call.result.optBoolean("completeConfigResults"));
        assertEquals(before, broadcasts().size());
    }

    @Test
    public void legacyConfigurationKeepsLastResultDefault() {
        Call call = new Call(new JSObject().put("profileName", profile));
        plugin.setConfig(call);
        assertEquals("LAST_RESULT", lastBroadcast().getStringExtra("SEND_RESULT"));
        receive(resultFor(lastBroadcast()));
        assertNull(call.error);
        assertEquals(1, call.completions);
    }

    @Test
    public void bareAcknowledgementCannotPreventTimeout() {
        Call call = completeConfigCall();
        receive(resultFor(lastBroadcast()));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals("DATAWEDGE_TIMEOUT", call.error);
        assertEquals(1, call.completions);
    }

    @Test
    public void malformedOrEmptyResultListIsRejected() {
        for (boolean empty : new boolean[] { true, false }) {
            Call call = completeConfigCall();
            Intent reply = resultFor(lastBroadcast());
            if (empty) reply.putParcelableArrayListExtra("RESULT_LIST", new ArrayList<Bundle>());
            else reply.putExtra("RESULT_LIST", "wrong-type");
            receive(reply);
            assertEquals("CONFIG_RESULT_INCOMPLETE", call.error);
            assertEquals(1, call.completions);
        }
    }

    @Test
    public void malformedResultCodeIsRejected() {
        Call call = completeConfigCall();
        ArrayList<Bundle> rows = new ArrayList<>();
        Bundle row = configResult("BARCODE", "SUCCESS", null);
        row.putBoolean("RESULT_CODE", true);
        rows.add(row);
        rows.add(configResult("INTENT", "SUCCESS", null));
        receive(resultFor(lastBroadcast()).putParcelableArrayListExtra("RESULT_LIST", rows));
        assertEquals("CONFIG_RESULT_INVALID", call.error);
    }

    @Test
    public void resultCodeArrayFailureIsNotHiddenByLastPluginSuccess() {
        Call call = completeConfigCall();
        ArrayList<Bundle> rows = new ArrayList<>();
        Bundle row = configResult("BARCODE", "FAILURE", null);
        row.putStringArray("RESULT_CODE", new String[] { "PARAMETER_INVALID" });
        rows.add(row);
        rows.add(configResult("INTENT", "SUCCESS", null));
        receive(resultFor(lastBroadcast()).putParcelableArrayListExtra("RESULT_LIST", rows));
        assertEquals("PARAMETER_INVALID", call.error);
    }

    @Test
    public void successCodeArrayWithoutResultAcceptsConfiguration() {
        Call call = completeConfigCall();
        ArrayList<Bundle> rows = new ArrayList<>();
        for (String name : new String[] { "BARCODE", "INTENT" }) {
            Bundle row = new Bundle();
            row.putString("MODULE", name);
            row.putStringArray("RESULT_CODE", new String[] { "SUCCESS", "SUCCESS" });
            rows.add(row);
        }
        receive(resultFor(lastBroadcast()).putParcelableArrayListExtra("RESULT_LIST", rows));
        assertNull(call.error);
        assertEquals(1, call.completions);
    }

    @Test
    public void explicitFailureCannotBeOverriddenByScalarOrArraySuccessCode() {
        for (boolean array : new boolean[] { false, true }) {
            Call call = completeConfigCall();
            ArrayList<Bundle> rows = new ArrayList<>();
            Bundle row = configResult("BARCODE", "FAILURE", "SUCCESS");
            if (array) row.putStringArray("RESULT_CODE", new String[] { "SUCCESS" });
            rows.add(row);
            rows.add(configResult("INTENT", "SUCCESS", null));
            receive(resultFor(lastBroadcast()).putParcelableArrayListExtra("RESULT_LIST", rows));
            assertEquals("CONFIG_RESULT_INVALID", call.error);
            assertEquals(1, call.completions);
        }
    }

    @Test
    public void malformedResultOrEmptyCodeArrayCannotEstablishSuccess() {
        for (boolean emptyCodes : new boolean[] { false, true }) {
            Call call = completeConfigCall();
            ArrayList<Bundle> rows = new ArrayList<>();
            Bundle row = configResult("BARCODE", null, "SUCCESS");
            if (emptyCodes) row.putStringArray("RESULT_CODE", new String[0]);
            else row.putBoolean("RESULT", true);
            rows.add(row);
            rows.add(configResult("INTENT", "SUCCESS", null));
            receive(resultFor(lastBroadcast()).putParcelableArrayListExtra("RESULT_LIST", rows));
            assertEquals("CONFIG_RESULT_INVALID", call.error);
            assertEquals(1, call.completions);
        }
    }

    @Test
    public void pluginNameAliasCanIdentifySuccessfulRows() {
        Call call = completeConfigCall();
        ArrayList<Bundle> rows = new ArrayList<>();
        for (String name : new String[] { "BARCODE", "INTENT" }) {
            Bundle row = new Bundle();
            row.putString("PLUGIN_NAME", name);
            row.putString("RESULT", "SUCCESS");
            rows.add(row);
        }
        receive(resultFor(lastBroadcast()).putParcelableArrayListExtra("RESULT_LIST", rows));
        assertNull(call.error);
        assertEquals(1, call.completions);
    }

    @Test
    public void outerFailureDoesNotWaitForCompleteResultList() {
        Call call = completeConfigCall();
        Bundle info = new Bundle();
        info.putString("RESULT_CODE", "OPERATION_NOT_ALLOWED");
        receive(resultFor(lastBroadcast()).putExtra("RESULT", "FAILURE").putExtra("RESULT_INFO", info));
        assertEquals("OPERATION_NOT_ALLOWED", call.error);
        assertEquals(1, call.completions);
    }
}
