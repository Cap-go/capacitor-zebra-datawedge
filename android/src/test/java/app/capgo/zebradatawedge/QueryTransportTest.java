package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import com.getcapacitor.JSObject;
import java.time.Duration;
import org.junit.Test;

public class QueryTransportTest extends DataWedgeTestSupport {

    @Test
    public void queryWaitsForPayloadRatherThanAnEmptySuccessAcknowledgement() {
        Call call = new Call(new JSObject());
        plugin.getVersionInfo(call);
        Intent command = lastBroadcast();
        receive(resultFor(command));
        assertEquals(0, call.completions);
        Bundle version = new Bundle();
        version.putString("DATAWEDGE", "13.0.1");
        // Official query examples return typed payloads without a command identifier.
        receive(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_VERSION_INFO, version));
        assertEquals("13.0.1", call.result.getString("dataWedgeVersion"));
        receive(resultFor(command));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals(1, call.completions);
    }

    @Test
    public void commandFailureIsCorrelatedAndSettledOnce() {
        Call call = new Call(new JSObject().put("profileName", profile));
        plugin.createProfile(call);
        Intent command = lastBroadcast();
        Bundle error = new Bundle();
        error.putString("RESULT_CODE", "OPERATION_NOT_ALLOWED");
        Intent response = resultFor(command).putExtra("RESULT", "FAILURE").putExtra("RESULT_INFO", error);
        receive(response);
        receive(response);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals("OPERATION_NOT_ALLOWED", call.error);
        assertEquals(1, call.completions);
    }

    @Test
    public void missingQueryTimesOutAndLatePayloadCannotCompleteIt() {
        Call call = new Call(new JSObject());
        plugin.getDatawedgeStatus(call);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        receive(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled"));
        assertNotNull(call.error);
        assertEquals(1, call.completions);
    }

    @Test
    public void unknownCommandIdentifierCannotAcknowledgeAnotherRequest() {
        Call call = new Call(new JSObject().put("profileName", profile));
        plugin.createProfile(call);
        Intent command = lastBroadcast();
        receive(resultFor(command).putExtra("COMMAND_IDENTIFIER", "other-command"));
        assertEquals(0, call.completions);
        receive(resultFor(command));
        assertEquals(1, call.completions);
    }

    @Test
    public void failedCommandDispatchDoesNotLeaveATimeoutOrSecondCompletion() {
        transportContext = new android.content.ContextWrapper(application) {
            @Override
            public void sendBroadcast(Intent intent) {
                throw new IllegalStateException("fixture transport failure");
            }
        };
        Call call = new Call(new JSObject().put("profileName", profile));
        plugin.createProfile(call);
        assertEquals("DATAWEDGE_TRANSPORT", call.error);
        assertEquals(1, call.completions);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals(1, call.completions);
    }

    @Test
    public void noIdentifierPayloadCannotChooseBetweenTwoSameTypeQueries() {
        Call first = new Call(new JSObject());
        Call second = new Call(new JSObject());
        plugin.getVersionInfo(first);
        plugin.getVersionInfo(second);
        Bundle version = new Bundle();
        version.putString("DATAWEDGE", "15.0.2");
        receive(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_VERSION_INFO, version));
        assertEquals(0, first.completions);
        assertEquals(0, second.completions);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals(1, first.completions);
        assertEquals(1, second.completions);
        assertNotNull(first.error);
        assertNotNull(second.error);
    }

    @Test
    public void differentQueryTypesResolveIndependentlyWithoutIdentifiers() {
        Call versionCall = new Call(new JSObject());
        Call statusCall = new Call(new JSObject());
        plugin.getVersionInfo(versionCall);
        plugin.getDatawedgeStatus(statusCall);
        receive(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled"));
        assertEquals(1, statusCall.completions);
        assertEquals(0, versionCall.completions);
        Bundle version = new Bundle();
        version.putString("DATAWEDGE", "15.0.2");
        receive(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_VERSION_INFO, version));
        assertEquals("15.0.2", versionCall.result.getString("dataWedgeVersion"));
        assertEquals(1, versionCall.completions);
    }

    @Test
    public void correlatedQueryFailureRejectsWithoutWaitingForTypedPayload() {
        Call query = new Call(new JSObject());
        plugin.getVersionInfo(query);
        Intent command = lastBroadcast();
        Bundle error = new Bundle();
        error.putString("RESULT_CODE", "OPERATION_NOT_ALLOWED");
        receive(resultFor(command).putExtra("RESULT", "FAILURE").putExtra("RESULT_INFO", error));
        assertEquals("OPERATION_NOT_ALLOWED", query.error);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals(1, query.completions);
    }

    @Test
    public void scannerStatusPayloadWithoutIdentifierUsesTheDocumentedExtra() {
        Call call = new Call(new JSObject());
        plugin.getScannerStatus(call);
        receive(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra("com.symbol.datawedge.api.RESULT_SCANNER_STATUS", "WAITING"));
        assertEquals("WAITING", call.result.getString("status"));
        assertEquals(1, call.completions);
    }

    @Test
    public void remainingQueryFamiliesMatchTheirTypedResultsWithoutIdentifiers() {
        Call disabled = new Call(new JSObject());
        plugin.getDisabledAppList(disabled);
        receive(
            new Intent(ZebraDataWedge.RESULT_ACTION).putParcelableArrayListExtra(
                ZebraDataWedge.RESULT_GET_DISABLED_APP_LIST,
                new java.util.ArrayList<Bundle>()
            )
        );
        assertEquals(1, disabled.completions);
        assertEquals(0, disabled.result.optJSONArray("appList").length());
        Call ignored = new Call(new JSObject());
        plugin.getIgnoreDisabledProfiles(ignored);
        receive(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_IGNORE_DISABLED_PROFILES, "true"));
        assertEquals(1, ignored.completions);
        assertTrue(ignored.result.getBoolean("enabled", false));
        Call triggers = new Call(new JSObject());
        plugin.enumerateTriggers(triggers);
        receive(
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_ENUMERATE_TRIGGERS, new String[] { "fixture-trigger" })
        );
        assertEquals(1, triggers.completions);
        assertEquals("fixture-trigger", triggers.result.optJSONArray("triggers").optString(0));
    }
}
