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
        Call associated = new Call(new JSObject().put("profileName", profile));
        plugin.getAssociatedApps(associated);
        assertTrue(lastBroadcast().getBundleExtra(ZebraDataWedge.CMD_GET_CONFIG).containsKey("APP_LIST"));
        receiveCorrelated(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_CONFIG, config));
        assertEquals(1, associated.completions);
        assertNull(associated.error);
        assertEquals(application.getPackageName(), associated.result.optJSONArray("appList").optJSONObject(0).optString("packageName"));
        assertEquals("*", associated.result.optJSONArray("appList").optJSONObject(0).optJSONArray("activityList").optString(0));
    }

    @Test
    public void bothPublicQueriesPreserveAllSupportedAssociationRepresentations() {
        for (boolean associatedQuery : new boolean[] { true, false }) {
            for (boolean parcelledArray : new boolean[] { true, false }) {
                for (boolean activityArray : new boolean[] { true, false }) {
                    Call call = new Call(new JSObject().put("profileName", profile));
                    if (associatedQuery) plugin.getAssociatedApps(call);
                    else plugin.getConfig(call);
                    Bundle app = new Bundle();
                    app.putString("PACKAGE_NAME", application.getPackageName());
                    if (activityArray) app.putStringArray("ACTIVITY_LIST", new String[] { "*" });
                    else app.putStringArrayList("ACTIVITY_LIST", new java.util.ArrayList<>(java.util.Arrays.asList("*")));
                    Bundle config = new Bundle();
                    config.putString("PROFILE_NAME", profile);
                    if (parcelledArray) config.putParcelableArray("APP_LIST", new android.os.Parcelable[] { app });
                    else config.putParcelableArrayList("APP_LIST", new java.util.ArrayList<>(java.util.Arrays.asList(app)));
                    receiveCorrelated(new Intent(ZebraDataWedge.RESULT_ACTION).putExtra(ZebraDataWedge.RESULT_GET_CONFIG, config));
                    assertEquals(1, call.completions);
                    assertNull(call.error);
                    assertEquals(1, call.result.optJSONArray("appList").length());
                    assertEquals(
                        application.getPackageName(),
                        call.result.optJSONArray("appList").optJSONObject(0).optString("packageName")
                    );
                    assertEquals("*", call.result.optJSONArray("appList").optJSONObject(0).optJSONArray("activityList").optString(0));
                }
            }
        }
    }
}
