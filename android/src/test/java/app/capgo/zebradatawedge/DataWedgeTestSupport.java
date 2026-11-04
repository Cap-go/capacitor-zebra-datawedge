package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import java.time.Duration;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowBuild;

/** Shared plugin fixtures; receive injects trusted metadata rather than exercising onReceive. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 34, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
abstract class DataWedgeTestSupport {

    protected TestPlugin plugin;
    protected Application application;
    protected String profile;
    protected Context transportContext;

    protected class TestPlugin extends ZebraDataWedgePlugin {

        @Override
        public Context getContext() {
            return transportContext;
        }

        void destroy() {
            handleOnDestroy();
        }
    }

    protected static class Call extends PluginCall {

        int completions;
        JSObject result;
        String error;
        Exception cause;

        Call(JSObject data) {
            super(null, "ZebraDataWedge", "test", "test", data);
        }

        @Override
        public void resolve() {
            completions++;
        }

        @Override
        public void resolve(JSObject value) {
            result = value;
            completions++;
        }

        @Override
        public void reject(String message, String code, Exception exception, JSObject data) {
            error = code == null ? message : code;
            cause = exception;
            completions++;
        }
    }

    @Before
    public void setup() {
        ShadowBuild.setManufacturer("Zebra Technologies");
        application = RuntimeEnvironment.getApplication();
        transportContext = application;
        profile = application.getPackageName() + ".datawedge.test";
        plugin = new TestPlugin();
        plugin.load();
    }

    @After
    public void teardown() {
        plugin.destroy();
    }

    protected List<Intent> broadcasts() {
        return shadowOf(application).getBroadcastIntents();
    }

    protected Intent lastBroadcast() {
        return broadcasts().get(broadcasts().size() - 1);
    }

    protected void receive(Intent response) {
        // Real receiver/API compatibility is covered separately by ReceiverCompatibilityTest.
        plugin.deliverResultIntentForTest(application, response, android.os.Process.myUid(), ZebraDataWedge.DATAWEDGE_PACKAGE);
        shadowOf(Looper.getMainLooper()).idle();
    }

    protected Intent resultFor(Intent command) {
        return new Intent(ZebraDataWedge.RESULT_ACTION)
            .putExtra("COMMAND_IDENTIFIER", command.getStringExtra("COMMAND_IDENTIFIER"))
            .putExtra("RESULT", "SUCCESS");
    }

    protected void receiveCorrelated(Intent response) {
        response.putExtra("COMMAND_IDENTIFIER", lastBroadcast().getStringExtra("COMMAND_IDENTIFIER"));
        receive(response);
    }
}
