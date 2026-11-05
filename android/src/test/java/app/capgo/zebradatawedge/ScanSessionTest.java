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
        assertEquals(2, call.result.optInt("version"));
        assertTrue(call.result.optBoolean("renewableLease"));
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

    @Test
    public void renewalExtendsLivenessWithoutBroadcastsButNotAbsoluteDeadline() {
        Call begun = begin(25000);
        int count = broadcasts().size();
        for (int i = 0; i < 3; i++) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(7));
            Call renew = token(begun);
            plugin.renewScanSession(renew);
            assertNull(renew.error);
            assertTrue(renew.result.optBoolean("active"));
            assertEquals(count, broadcasts().size());
        }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        Call state = token(begun);
        plugin.getScanSessionState(state);
        assertFalse(state.result.optBoolean("active"));
        assertEquals("max_duration", state.result.optString("reason"));
        assertTargetedDisable(lastBroadcast());
    }

    @Test
    public void missingHeartbeatExpiresBeforeAbsoluteDeadlineAndCannotRevive() {
        Call begun = begin(300000);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10));
        Call renew = token(begun);
        plugin.renewScanSession(renew);
        assertEquals("SESSION_INACTIVE", renew.error);
        Call state = token(begun);
        plugin.getScanSessionState(state);
        assertEquals("heartbeat_expired", state.result.optString("reason"));
        assertEquals(0L, state.result.optLong("remainingMs"));
        assertTargetedDisable(lastBroadcast());
    }

    @Test
    public void renewalChecksExpiredClockEvenBeforeHandlerRunnableExecutes() {
        Call begun = begin(300000);
        org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofSeconds(11));
        Call renew = token(begun);
        plugin.renewScanSession(renew);
        assertEquals("SESSION_INACTIVE", renew.error);
        assertTargetedDisable(lastBroadcast());
    }

    @Test
    public void lateActiveProfileAcknowledgementCannotTriggerAnExpiredLease() {
        Call begun = begin(300000);
        Call trigger = token(begun);
        plugin.triggerScanSession(trigger);
        org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofSeconds(11));
        // Deliver directly before the delayed timeout callbacks get a chance to run.
        Intent reply = new Intent(ZebraDataWedge.RESULT_ACTION)
            .putExtra(ZebraDataWedge.RESULT_GET_ACTIVE_PROFILE, profile)
            .putExtra("COMMAND_IDENTIFIER", lastBroadcast().getStringExtra("COMMAND_IDENTIFIER"));
        plugin.deliverResultIntentForTest(application, reply, android.os.Process.myUid(), ZebraDataWedge.DATAWEDGE_PACKAGE);
        assertEquals("SESSION_CANCELLED", trigger.error);
        assertFalse(
            broadcasts()
                .stream()
                .anyMatch((i) -> "START_SCANNING".equals(i.getStringExtra(ZebraDataWedge.CMD_SOFT_SCAN_TRIGGER)))
        );
    }

    @Test
    public void oldTokenCannotRenewReplacementSession() {
        Call first = begin(300000);
        plugin.endScanSession(token(first));
        receive(resultFor(lastBroadcast()));
        Call second = begin(300000);
        Call stale = token(first);
        plugin.renewScanSession(stale);
        assertEquals("SESSION_INACTIVE", stale.error);
        Call renew = token(second);
        plugin.renewScanSession(renew);
        assertTrue(renew.result.optBoolean("active"));
    }

    @Test
    public void pauseAndResumeNeverRenewOrReopenTheLease() {
        Call begun = begin(300000);
        plugin.pause();
        plugin.resume();
        Call renew = token(begun);
        plugin.renewScanSession(renew);
        assertEquals("SESSION_INACTIVE", renew.error);
        Call state = token(begun);
        plugin.getScanSessionState(state);
        assertEquals("background", state.result.optString("reason"));
    }

    @Test
    public void heartbeatBoundsAreValidatedBeforeAnyDataWedgeCommand() {
        int count = broadcasts().size();
        for (int timeout : new int[] { 0, 1999, 60001 }) {
            Call invalid = new Call(new JSObject().put("profileName", profile).put("leaseTimeoutMs", timeout));
            plugin.beginScanSession(invalid);
            assertEquals("PARAMETER_INVALID", invalid.error);
        }
        assertEquals(count, broadcasts().size());
    }

    @Test
    public void readOnlyStateAndNativeScansDoNotExtendLiveness() {
        Call begun = begin(300000);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6));
        Call state = token(begun);
        plugin.getScanSessionState(state);
        assertTrue(state.result.optBoolean("active"));
        receive(new Intent(begun.result.getString("intentAction")).putExtra(ZebraDataWedge.EXTRA_SCAN_DATA, "test"));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        Call expired = token(begun);
        plugin.getScanSessionState(expired);
        assertEquals("heartbeat_expired", expired.result.optString("reason"));
    }

    @Test
    public void nativeClosureReportsItsFirstReasonOnceWithoutConfirmingCleanup() {
        plugin.destroy();
        final ArrayList<JSObject> events = new ArrayList<>();
        plugin = new TestPlugin() {
            @Override
            protected void notifyListeners(String name, JSObject payload) {
                if ("scanSessionClosed".equals(name)) events.add(payload);
            }
        };
        plugin.load();
        Call begun = begin(300000);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10));
        assertEquals(1, events.size());
        assertEquals(begun.result.getString("sessionId"), events.get(0).optString("sessionId"));
        assertFalse(events.get(0).optBoolean("active"));
        assertEquals("heartbeat_expired", events.get(0).optString("reason"));
        plugin.pause();
        plugin.resume();
        Call end = token(begun);
        plugin.endScanSession(end);
        assertEquals(1, events.size());
        assertEquals(0, end.completions);
        receive(resultFor(lastBroadcast()));
        assertTrue(end.result.optBoolean("confirmed"));
    }

    @Test
    public void renewalIsIndependentOfPendingDataWedgeConfiguration() {
        Call begun = begin(300000);
        Call config = new Call(new JSObject().put("profileName", profile).put("sessionId", begun.result.getString("sessionId")));
        plugin.setConfig(config);
        int count = broadcasts().size();
        Call renewal = token(begun);
        plugin.renewScanSession(renewal);
        assertEquals(1, renewal.completions);
        assertTrue(renewal.result.optBoolean("active"));
        assertEquals(0, config.completions);
        assertEquals(count, broadcasts().size());
        receive(resultFor(lastBroadcast()));
        assertNull(config.error);
    }
}
