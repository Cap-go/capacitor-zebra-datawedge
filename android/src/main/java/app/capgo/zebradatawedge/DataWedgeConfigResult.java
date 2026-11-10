package app.capgo.zebradatawedge;

import android.content.Intent;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Fail closed on partial/malformed COMPLETE_RESULT replies before enabling input. */
final class DataWedgeConfigResult {

    static String validate(Intent reply, JSONArray plugins) {
        Bundle extras = reply.getExtras();
        Object raw = extras == null ? null : extras.get("RESULT_LIST");
        if (!(raw instanceof ArrayList<?>)) return "CONFIG_RESULT_INCOMPLETE";
        if (((ArrayList<?>) raw).isEmpty()) return "CONFIG_RESULT_INCOMPLETE";
        Set<String> covered = new HashSet<>();
        for (Object item : (ArrayList<?>) raw) {
            if (!(item instanceof Bundle)) return "CONFIG_RESULT_INVALID";
            Bundle row = (Bundle) item;
            Object module = row.get("MODULE");
            if (!(module instanceof String)) module = row.get("PLUGIN_NAME");
            Object result = row.get("RESULT");
            Object code = row.get("RESULT_CODE");
            if (code instanceof String && !"SUCCESS".equals(code)) return safeCode((String) code);
            if (code instanceof String[]) {
                for (String value : (String[]) code) if (!"SUCCESS".equals(value)) return safeCode(value);
            } else if (code != null && !(code instanceof String)) return "CONFIG_RESULT_INVALID";
            if (!"SUCCESS".equals(result) && !"SUCCESS".equals(code)) return "CONFIG_RESULT_INVALID";
            if (module instanceof String) covered.add((String) module);
        }
        for (int index = 0; plugins != null && index < plugins.length(); index++) {
            JSONObject plugin = plugins.optJSONObject(index);
            if (plugin == null || !covered.contains(plugin.optString("pluginName"))) return "CONFIG_RESULT_INCOMPLETE";
        }
        return null;
    }

    private static String safeCode(String code) {
        return code != null && code.matches("[A-Z0-9_]{1,64}") ? code : "CONFIG_RESULT_INVALID";
    }

    private DataWedgeConfigResult() {}
}
