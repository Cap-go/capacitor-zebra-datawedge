package app.capgo.zebradatawedge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import com.getcapacitor.JSObject;
import java.time.Duration;
import org.junit.Test;

public class ProfileAssociationsTest extends DataWedgeTestSupport {

    @Test
    public void parcelableAppListIsNotMistakenForAnUnassociatedProfile() {
        Call call = new Call(new JSObject().put("profileName", profile));
        plugin.getConfig(call);
        Bundle app = new Bundle();
        app.putString("PACKAGE_NAME", "customer.otherApp");
        app.putStringArray("ACTIVITY_LIST", new String[] { "*" });
        Bundle config = new Bundle();
        config.putString("PROFILE_NAME", profile);
        config.putParcelableArray("APP_LIST", new android.os.Parcelable[] { app });
        receiveCorrelated(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_CONFIG, config));
        assertEquals("customer.otherApp", call.result.optJSONArray("appList").optJSONObject(0).optString("packageName"));
    }

    @Test
    public void configExplicitlyRequestsAssociationsAndReadsActivityArrayLists() {
        Call call = new Call(new JSObject().put("profileName", profile));
        plugin.getConfig(call);
        Bundle request = lastBroadcast().getBundleExtra(ZebraDataWedge.CMD_GET_CONFIG);
        assertTrue(request.containsKey("APP_LIST"));
        assertEquals("", request.getString("APP_LIST"));
        Bundle app = new Bundle();
        app.putString("PACKAGE_NAME", application.getPackageName());
        app.putStringArrayList("ACTIVITY_LIST", new java.util.ArrayList<>(java.util.Arrays.asList("*")));
        Bundle config = new Bundle();
        config.putString("PROFILE_NAME", profile);
        config.putParcelableArrayList("APP_LIST", new java.util.ArrayList<>(java.util.Arrays.asList(app)));
        receiveCorrelated(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_CONFIG, config));
        assertEquals("*", call.result.optJSONArray("appList").optJSONObject(0).optJSONArray("activityList").optString(0));
        plugin.getAssociatedApps(new Call(new JSObject().put("profileName", profile)));
        assertTrue(lastBroadcast().getBundleExtra(ZebraDataWedge.CMD_GET_CONFIG).containsKey("APP_LIST"));
    }
}
