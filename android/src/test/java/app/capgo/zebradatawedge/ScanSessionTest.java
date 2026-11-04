package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import com.getcapacitor.JSObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

public class ScanSessionTest extends DataWedgeTestSupport {

    private Intent association(String owner) {
        Bundle app = new Bundle();
        app.putString("PACKAGE_NAME", owner);
        app.putStringArrayList("ACTIVITY_LIST", new ArrayList<>(Arrays.asList("*")));
        Bundle config = new Bundle();
        config.putString("PROFILE_NAME", profile);
        config.putParcelableArrayList("APP_LIST", new ArrayList<>(Arrays.asList(app)));
        return new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_CONFIG, config);
    }

    private Call begin(int timeout) {
        Call call = new Call(new JSObject().put("profileName", profile).put("timeoutMs", timeout));
        plugin.beginScanSession(call);
        receiveCorrelated(association(application.getPackageName()));
        assertNull(call.error);
        assertEquals(1, call.completions);
        return call;
    }

    private Call token(Call begun) {
        return new Call(new JSObject().put("sessionId", begun.result.getString("sessionId")));
    }

    private void assertTargetedDisable(Intent command) {
        Bundle config = command.getBundleExtra(ZebraDataWedge.CMD_SET_CONFIG);
        assertEquals(profile, config.getString("PROFILE_NAME"));
        assertEquals("UPDATE", config.getString("CONFIG_MODE"));
        assertEquals("true", config.getString("PROFILE_ENABLED"));
        Bundle params = config.<Bundle>getParcelableArrayList("PLUGIN_CONFIG").get(0).getBundle("PARAM_LIST");
        assertEquals("false", params.getString("scanner_input_enabled"));
        assertEquals("true", params.getString("configure_all_scanners"));
    }

    @Test
    public void capabilityReadDoesNotSendACommand() {
        int count = broadcasts().size();
        Call call = new Call(new JSObject());
        plugin.getScanSessionCapabilities(call);
        assertEquals(1, call.result.optInt("version"));
        assertEquals(count, broadcasts().size());
    }

    @Test
    public void customProfileGetsAnOpaqueTokenAndUniqueAction() {
        profile = "ExistingWarehouseProfile";
        Call first = begin(1000);
        assertEquals(profile, first.result.getString("profileName"));
        assertNotNull(first.result.getString("intentAction"));
        Call end = token(first);
        plugin.endScanSession(end);
        receive(resultFor(lastBroadcast()));
        Call second = begin(1000);
        assertNotEquals(first.result.getString("sessionId"), second.result.getString("sessionId"));
        assertNotEquals(first.result.getString("intentAction"), second.result.getString("intentAction"));
    }

    @Test
    public void foreignAssociationIsRejectedWithoutMutatingItsProfile() {
        Call call = new Call(new JSObject().put("profileName", profile));
        plugin.beginScanSession(call);
        receiveCorrelated(association("org.example.other"));
        assertEquals("SESSION_OWNERSHIP", call.error);
        assertFalse(
            broadcasts()
                .stream()
                .anyMatch((i) -> i.hasExtra(ZebraDataWedge.CMD_SET_CONFIG))
        );
        begin(1000); // Rejection released the reservation.
    }

    @Test
    public void cancelSettlesPendingTriggerOnceAndAwaitsDisableAcknowledgement() {
        Call begun = begin(1000);
        Call trigger = token(begun);
        plugin.triggerScanSession(trigger);
        receiveCorrelated(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_ACTIVE_PROFILE, profile));
        Intent start = lastBroadcast();
        assertEquals("START_SCANNING", start.getStringExtra(ZebraDataWedge.CMD_SOFT_SCAN_TRIGGER));
        Call end = token(begun);
        plugin.endScanSession(end);
        assertEquals("SESSION_CANCELLED", trigger.error);
        assertEquals(0, end.completions);
        Intent disable = lastBroadcast();
        assertTargetedDisable(disable);
        receive(resultFor(start));
        receive(resultFor(disable));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals(1, trigger.completions);
        assertEquals(1, end.completions);
        assertTrue(end.result.getBoolean("confirmed", false));
    }

    @Test
    public void physicalScanWaitIsCancelledWithTheSession() {
        Call begun = begin(1000);
        Call scan = new Call(new JSObject().put("intentAction", begun.result.getString("intentAction")));
        plugin.softScanTrigger(scan);
        plugin.endScanSession(token(begun));
        assertEquals("SESSION_CANCELLED", scan.error);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(16));
        assertEquals(1, scan.completions);
    }

    @Test
    public void pauseOnlyDisablesTheOwnedProfileAndKeepsTheLeaseUnconfirmed() {
        Call begun = begin(1000);
        plugin.triggerScanSession(token(begun));
        int count = broadcasts().size();
        plugin.pause();
        assertEquals(count + 1, broadcasts().size());
        assertTargetedDisable(lastBroadcast());
        plugin.resume();
        assertTargetedDisable(lastBroadcast());
        Call another = new Call(new JSObject().put("profileName", profile));
        plugin.beginScanSession(another);
        assertEquals("SESSION_BUSY", another.error);
        Call end = token(begun);
        plugin.endScanSession(end);
        receive(resultFor(lastBroadcast()));
        assertNull(end.error);
    }

    @Test
    public void nativeDeadlineBlocksNewWorkUntilAcknowledgedCleanup() {
        Call begun = begin(1000);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
        assertTargetedDisable(lastBroadcast());
        Call trigger = token(begun);
        plugin.triggerScanSession(trigger);
        assertEquals("SESSION_INACTIVE", trigger.error);
        Call another = new Call(new JSObject().put("profileName", profile));
        plugin.beginScanSession(another);
        assertEquals("SESSION_BUSY", another.error);
    }

    @Test
    public void failedCleanupCanBeRetriedAndCompletedEndIsIdempotent() {
        Call begun = begin(1000);
        Call end = token(begun);
        plugin.endScanSession(end);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals("DATAWEDGE_TIMEOUT", end.error);
        Call retry = token(begun);
        plugin.endScanSession(retry);
        receive(resultFor(lastBroadcast()));
        assertTrue(retry.result.getBoolean("confirmed", false));
        int count = broadcasts().size();
        Call repeat = token(begun);
        plugin.endScanSession(repeat);
        assertEquals(count, broadcasts().size());
        assertTrue(repeat.result.getBoolean("confirmed", false));
    }

    @Test
    public void wrongTokenCannotTriggerOrEndTheSession() {
        begin(1000);
        int count = broadcasts().size();
        Call bad = new Call(new JSObject().put("sessionId", "not-the-owner"));
        plugin.endScanSession(bad);
        assertEquals("SESSION_INACTIVE", bad.error);
        assertEquals(count, broadcasts().size());
    }

    @Test
    public void configurationCannotReenableAnEndedSession() {
        Call begun = begin(1000);
        Call config = new Call(
            new JSObject().put("profileName", profile).put("profileEnabled", true).put("sessionId", begun.result.getString("sessionId"))
        );
        plugin.setConfig(config);
        Intent pendingConfig = lastBroadcast();
        Call end = token(begun);
        plugin.endScanSession(end);
        assertEquals("SESSION_CANCELLED", config.error);
        receive(resultFor(pendingConfig));
        receive(resultFor(lastBroadcast()));
        Call stale = new Call(
            new JSObject().put("profileName", profile).put("profileEnabled", true).put("sessionId", begun.result.getString("sessionId"))
        );
        int count = broadcasts().size();
        plugin.setConfig(stale);
        assertEquals("SESSION_INACTIVE", stale.error);
        assertEquals(count, broadcasts().size());
    }

    @Test
    public void ownershipQueryTimeoutReleasesReservationWithoutDisablingAnything() {
        Call begin = new Call(new JSObject().put("profileName", profile));
        plugin.beginScanSession(begin);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9));
        assertEquals("DATAWEDGE_TIMEOUT", begin.error);
        assertFalse(
            broadcasts()
                .stream()
                .anyMatch((i) -> i.hasExtra(ZebraDataWedge.CMD_SET_CONFIG))
        );
        begin(1000);
    }

    @Test
    public void pauseDuringOwnershipQueryCannotActivateTheLateReply() {
        Call begin = new Call(new JSObject().put("profileName", profile));
        plugin.beginScanSession(begin);
        Intent query = lastBroadcast();
        plugin.pause();
        receive(association(application.getPackageName()).putExtra("COMMAND_IDENTIFIER", query.getStringExtra("COMMAND_IDENTIFIER")));
        assertEquals("SESSION_CANCELLED", begin.error);
        assertEquals(1, begin.completions);
        assertFalse(
            broadcasts()
                .stream()
                .anyMatch((i) -> i.hasExtra(ZebraDataWedge.CMD_SET_CONFIG))
        );
    }

    @Test
    public void deadlineIsBoundedAndLegacyConfigurationRemainsAvailable() {
        Call invalid = new Call(new JSObject().put("profileName", profile).put("timeoutMs", 600001));
        plugin.beginScanSession(invalid);
        assertEquals("PARAMETER_INVALID", invalid.error);
        Call config = new Call(new JSObject().put("profileName", profile).put("profileEnabled", true));
        plugin.setConfig(config);
        receive(resultFor(lastBroadcast()));
        assertNull(config.error);
        assertEquals(1, config.completions);
    }

    @Test
    public void triggerCannotStartAnotherActiveProfile() {
        Call begun = begin(1000);
        Call trigger = token(begun);
        plugin.triggerScanSession(trigger);
        receiveCorrelated(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_ACTIVE_PROFILE, "OtherProfile"));
        assertEquals("SESSION_PROFILE_INACTIVE", trigger.error);
        assertFalse(
            broadcasts()
                .stream()
                .anyMatch((i) -> "START_SCANNING".equals(i.getStringExtra(ZebraDataWedge.CMD_SOFT_SCAN_TRIGGER)))
        );
    }
}
