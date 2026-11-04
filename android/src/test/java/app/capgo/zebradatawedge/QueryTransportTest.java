package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import com.getcapacitor.JSObject;
import java.time.Duration;
import java.util.function.Consumer;
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
    public void malformedVersionExtrasDoNotCompleteQueriesBeforeTheirBundleArrives() {
        for (java.io.Serializable malformed : new java.io.Serializable[] { null, "not-a-bundle", true, new String[] { "bad" } }) {
            Bundle extras = new Bundle();
            extras.putSerializable(ZebraDataWedge.RESULT_GET_VERSION_INFO, malformed);
            Bundle version = new Bundle();
            version.putString("DATAWEDGE", "15.0.2");
            assertMalformedPayloadIsIgnored(
                plugin::getVersionInfo,
                new Intent(ZebraDataWedge.RESULT_ACTION).putExtras(extras),
                new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_VERSION_INFO, version)
            );
        }
    }

    @Test
    public void allQueryFamiliesRequireTheirExpectedOuterPayloadTypes() {
        assertMalformedPayloadIsIgnored(
            plugin::getActiveProfile,
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_ACTIVE_PROFILE, new Bundle()),
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_ACTIVE_PROFILE, profile)
        );
        Bundle config = new Bundle();
        config.putString("PROFILE_NAME", profile);
        assertMalformedPayloadIsIgnored(
            plugin::getConfig,
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_CONFIG, "not-a-bundle"),
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_CONFIG, config)
        );
        assertMalformedPayloadIsIgnored(
            plugin::getDatawedgeStatus,
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, true),
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled")
        );
        assertMalformedPayloadIsIgnored(
            plugin::getProfilesList,
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_PROFILES_LIST, "not-an-array"),
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_PROFILES_LIST, new String[] { profile })
        );
        assertMalformedPayloadIsIgnored(
            plugin::getScannerStatus,
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_SCANNER_STATUS, new Bundle()),
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_SCANNER_STATUS, "WAITING")
        );
        assertMalformedPayloadIsIgnored(
            plugin::getIgnoreDisabledProfiles,
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_IGNORE_DISABLED_PROFILES, true),
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_IGNORE_DISABLED_PROFILES, "true")
        );
        assertMalformedPayloadIsIgnored(
            plugin::enumerateTriggers,
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_ENUMERATE_TRIGGERS, "not-an-array"),
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_ENUMERATE_TRIGGERS, new String[] { "trigger" })
        );
        assertMalformedPayloadIsIgnored(
            plugin::getDisabledAppList,
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DISABLED_APP_LIST, new String[] { "bad" }),
            new Intent(ZebraDataWedge.RESULT_ACTION).putParcelableArrayListExtra(
                ZebraDataWedge.RESULT_GET_DISABLED_APP_LIST,
                new java.util.ArrayList<Bundle>()
            )
        );
        assertMalformedPayloadIsIgnored(
            plugin::enumerateScanners,
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_ENUMERATE_SCANNERS, new String[] { "bad" }),
            new Intent(ZebraDataWedge.RESULT_ACTION).putParcelableArrayListExtra(
                ZebraDataWedge.RESULT_ENUMERATE_SCANNERS,
                new java.util.ArrayList<Bundle>()
            )
        );
        assertMalformedPayloadIsIgnored(
            plugin::enumerateScanners,
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_ENUMERATED_SCANNER_LIST, new Bundle()),
            new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(
                ZebraDataWedge.RESULT_ENUMERATED_SCANNER_LIST,
                new String[] { "legacy-scanner" }
            )
        );
    }

    private void assertMalformedPayloadIsIgnored(Consumer<Call> send, Intent malformed, Intent payload) {
        for (boolean correlated : new boolean[] { false, true }) {
            Call call = new Call(new JSObject().put("profileName", profile));
            send.accept(call);
            Intent command = lastBroadcast();
            Intent invalidReply = new Intent(malformed);
            Intent validReply = new Intent(payload);
            if (correlated) {
                String id = command.getStringExtra(ZebraDataWedge.EXTRA_COMMAND_IDENTIFIER);
                invalidReply.putExtra(ZebraDataWedge.EXTRA_COMMAND_IDENTIFIER, id);
                validReply.putExtra(ZebraDataWedge.EXTRA_COMMAND_IDENTIFIER, id);
            }
            receive(invalidReply);
            assertEquals(0, call.completions);
            receive(validReply);
            assertEquals(1, call.completions);
            assertNull(call.error);
            receive(validReply);
            assertEquals(1, call.completions);
        }
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
    public void identifierFreeReplyCompletesReplacementAfterAnEarlierQueryTimedOut() {
        Call expired = new Call(new JSObject());
        plugin.getDatawedgeStatus(expired);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals("DATAWEDGE_TIMEOUT", expired.error);
        Call replacement = new Call(new JSObject());
        plugin.getDatawedgeStatus(replacement);
        Intent replacementCommand = lastBroadcast();
        receive(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled"));
        assertEquals(1, expired.completions);
        assertEquals(1, replacement.completions);
        assertTrue(replacement.result.getBoolean("isEnabled", false));
        receive(resultFor(replacementCommand).putExtra(ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "enabled"));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals(1, replacement.completions);
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
        assertNotNull(call.cause);
        assertEquals("fixture transport failure", call.cause.getMessage());
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

    @Test
    public void configurationAliasesWorkWithAndWithoutIdentifiers() {
        for (String key : new String[] {
            "com.symbol.datawedge.api.RESULT_GET_CONFIG",
            "com.symbol.datawedge.api.GET_CONFIG_RESULT",
            "GET_CONFIG_RESULT"
        }) {
            for (boolean correlated : new boolean[] { true, false }) {
                Call call = new Call(new JSObject().put("profileName", profile));
                plugin.getConfig(call);
                Intent command = lastBroadcast();
                receive(resultFor(command));
                assertEquals(0, call.completions);
                Bundle data = new Bundle();
                data.putString("PROFILE_NAME", profile);
                Intent response = new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(key, data);
                if (correlated) response.putExtra("COMMAND_IDENTIFIER", command.getStringExtra("COMMAND_IDENTIFIER"));
                receive(response);
                assertEquals(1, call.completions);
                assertEquals(profile, call.result.getString("profileName"));
                receive(response);
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
                assertEquals(1, call.completions);
            }
        }
    }

    @Test
    public void existingScalarAndArrayFallbacksRemainReachable() {
        for (boolean correlated : new boolean[] { true, false }) {
            Call active = new Call(new JSObject());
            plugin.getActiveProfile(active);
            Intent response = new Intent(ZebraDataWedge.RESULT_ACTION).putExtra("PROFILE_NAME", profile);
            if (correlated) receiveCorrelated(response);
            else receive(response);
            assertEquals(profile, active.result.getString("profileName"));
            Call service = new Call(new JSObject());
            plugin.getDatawedgeStatus(service);
            response = new Intent(ZebraDataWedge.RESULT_ACTION).putExtra("DATAWEDGE_STATUS", "enabled");
            if (correlated) receiveCorrelated(response);
            else receive(response);
            assertTrue(service.result.getBoolean("isEnabled", false));
            Call scanner = new Call(new JSObject());
            plugin.getScannerStatus(scanner);
            response = new Intent(ZebraDataWedge.RESULT_ACTION).putExtra("SCANNER_STATUS", "WAITING");
            if (correlated) receiveCorrelated(response);
            else receive(response);
            assertEquals("WAITING", scanner.result.getString("status"));
            Call ignore = new Call(new JSObject());
            plugin.getIgnoreDisabledProfiles(ignore);
            response = new Intent(ZebraDataWedge.RESULT_ACTION).putExtra("IGNORE_DISABLED_PROFILES", "true");
            if (correlated) receiveCorrelated(response);
            else receive(response);
            assertTrue(ignore.result.getBoolean("enabled", false));
            Call version = new Call(new JSObject());
            plugin.getVersionInfo(version);
            Bundle data = new Bundle();
            data.putString("DATAWEDGE", "fixture-version");
            response = new Intent(ZebraDataWedge.RESULT_ACTION).putExtra("GET_VERSION_INFO_RESULT", data);
            if (correlated) receiveCorrelated(response);
            else receive(response);
            assertEquals("fixture-version", version.result.getString("dataWedgeVersion"));
            Call triggers = new Call(new JSObject());
            plugin.enumerateTriggers(triggers);
            response = new Intent(ZebraDataWedge.RESULT_ACTION).putExtra("ENUMERATE_TRIGGERS", new String[] { "trigger" });
            if (correlated) receiveCorrelated(response);
            else receive(response);
            assertEquals("trigger", triggers.result.optJSONArray("triggers").optString(0));
            Call enumeration = new Call(new JSObject());
            plugin.enumerateScanners(enumeration);
            response = new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(
                "DWAPI_KEY_ENUMERATEDSCANNERLIST",
                new String[] { "legacy scanner" }
            );
            if (correlated) receiveCorrelated(response);
            else receive(response);
            assertEquals("legacy scanner", enumeration.result.optJSONArray("scanners").optJSONObject(0).optString("name"));
        }
    }

    @Test
    public void aliasesCannotStealACorrelatedReplyOrChooseBetweenOwners() {
        Call first = new Call(new JSObject());
        Call second = new Call(new JSObject());
        plugin.getScannerStatus(first);
        Intent firstRequest = lastBroadcast();
        plugin.getScannerStatus(second);
        Intent response = new Intent(ZebraDataWedge.RESULT_ACTION).putExtra("SCANNER_STATUS", "WAITING");
        receive(response);
        assertEquals(0, first.completions);
        assertEquals(0, second.completions);
        receive(new Intent(response).putExtra("COMMAND_IDENTIFIER", "unknown"));
        assertEquals(0, first.completions);
        assertEquals(0, second.completions);
        receive(new Intent(response).putExtra("COMMAND_IDENTIFIER", firstRequest.getStringExtra("COMMAND_IDENTIFIER")));
        assertEquals(1, first.completions);
        assertEquals(0, second.completions);
    }
}
