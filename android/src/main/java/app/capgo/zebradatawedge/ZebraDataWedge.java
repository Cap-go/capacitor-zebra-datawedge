package app.capgo.zebradatawedge;

import android.content.Intent;
import android.os.Bundle;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ZebraDataWedge {

    private ZebraDataWedge() {}

    public static final String DATAWEDGE_PACKAGE = "com.symbol.datawedge";

    public static final String ACTION = "com.symbol.datawedge.api.ACTION";
    public static final String RESULT_ACTION = "com.symbol.datawedge.api.RESULT_ACTION";
    public static final String NOTIFICATION_ACTION = "com.symbol.datawedge.api.NOTIFICATION_ACTION";

    public static final String EXTRA_SCAN_DATA = "com.symbol.datawedge.data_string";
    public static final String EXTRA_SCAN_LABEL_TYPE = "com.symbol.datawedge.label_type";
    public static final String EXTRA_SCAN_SOURCE = "com.symbol.datawedge.source";
    public static final String EXTRA_SCAN_DECODE_MODE = "com.symbol.datawedge.decode_mode";
    public static final String EXTRA_NOTIFICATION = "com.symbol.datawedge.api.NOTIFICATION";

    public static final String EXTRA_SEND_RESULT = "SEND_RESULT";
    public static final String EXTRA_COMMAND_IDENTIFIER = "COMMAND_IDENTIFIER";
    public static final String EXTRA_COMMAND = "COMMAND";
    public static final String EXTRA_RESULT = "RESULT";
    public static final String EXTRA_RESULT_INFO = "RESULT_INFO";
    public static final String EXTRA_RESULT_LIST = "RESULT_LIST";

    public static final String CMD_CLONE_PROFILE = "com.symbol.datawedge.api.CLONE_PROFILE";
    public static final String CMD_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE";
    public static final String CMD_DELETE_PROFILE = "com.symbol.datawedge.api.DELETE_PROFILE";
    public static final String CMD_IMPORT_CONFIG = "com.symbol.datawedge.api.IMPORT_CONFIG";
    public static final String CMD_RENAME_PROFILE = "com.symbol.datawedge.api.RENAME_PROFILE";
    public static final String CMD_RESTORE_CONFIG = "com.symbol.datawedge.api.RESTORE_CONFIG";
    public static final String CMD_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG";
    public static final String CMD_SET_DISABLED_APP_LIST = "com.symbol.datawedge.api.SET_DISABLED_APP_LIST";
    public static final String CMD_SET_IGNORE_DISABLED_PROFILES = "com.symbol.datawedge.api.SET_IGNORE_DISABLED_PROFILES";
    public static final String CMD_REGISTER_FOR_NOTIFICATION = "com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION";
    public static final String CMD_UNREGISTER_FOR_NOTIFICATION = "com.symbol.datawedge.api.UNREGISTER_FOR_NOTIFICATION";
    public static final String CMD_ENUMERATE_SCANNERS = "com.symbol.datawedge.api.ENUMERATE_SCANNERS";
    public static final String CMD_GET_ACTIVE_PROFILE = "com.symbol.datawedge.api.GET_ACTIVE_PROFILE";
    public static final String CMD_GET_CONFIG = "com.symbol.datawedge.api.GET_CONFIG";
    public static final String CMD_GET_DATAWEDGE_STATUS = "com.symbol.datawedge.api.GET_DATAWEDGE_STATUS";
    public static final String CMD_GET_DISABLED_APP_LIST = "com.symbol.datawedge.api.GET_DISABLED_APP_LIST";
    public static final String CMD_GET_IGNORE_DISABLED_PROFILES = "com.symbol.datawedge.api.GET_IGNORE_DISABLED_PROFILES";
    public static final String CMD_GET_PROFILES_LIST = "com.symbol.datawedge.api.GET_PROFILES_LIST";
    public static final String CMD_GET_SCANNER_STATUS = "com.symbol.datawedge.api.GET_SCANNER_STATUS";
    public static final String CMD_GET_VERSION_INFO = "com.symbol.datawedge.api.GET_VERSION_INFO";
    public static final String CMD_ENABLE_DATAWEDGE = "com.symbol.datawedge.api.ENABLE_DATAWEDGE";
    public static final String CMD_SCANNER_INPUT_PLUGIN = "com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN";
    public static final String CMD_ENUMERATE_TRIGGERS = "com.symbol.datawedge.api.ENUMERATE_TRIGGERS";
    public static final String CMD_NOTIFY = "com.symbol.datawedge.api.NOTIFY";
    public static final String CMD_RESET_DEFAULT_PROFILE = "com.symbol.datawedge.api.RESET_DEFAULT_PROFILE";
    public static final String CMD_SET_DEFAULT_PROFILE = "com.symbol.datawedge.api.SET_DEFAULT_PROFILE";
    public static final String CMD_SET_REPORTING_OPTIONS = "com.symbol.datawedge.api.SET_REPORTING_OPTIONS";
    public static final String CMD_SOFT_RFID_TRIGGER = "com.symbol.datawedge.api.SOFT_RFID_TRIGGER";
    public static final String CMD_SOFT_SCAN_TRIGGER = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER";
    public static final String CMD_SWITCH_SCANNER = "com.symbol.datawedge.api.SWITCH_SCANNER";
    public static final String CMD_SWITCH_SCANNER_PARAMS = "com.symbol.datawedge.api.SWITCH_SCANNER_PARAMS";
    public static final String CMD_SWITCH_TO_PROFILE = "com.symbol.datawedge.api.SWITCH_TO_PROFILE";

    public static final String RESULT_GET_PROFILES_LIST = "com.symbol.datawedge.api.RESULT_GET_PROFILES_LIST";
    public static final String RESULT_GET_ACTIVE_PROFILE = "com.symbol.datawedge.api.RESULT_GET_ACTIVE_PROFILE";
    public static final String RESULT_GET_VERSION_INFO = "com.symbol.datawedge.api.RESULT_GET_VERSION_INFO";
    public static final String RESULT_GET_CONFIG = "com.symbol.datawedge.api.RESULT_GET_CONFIG";
    public static final String RESULT_GET_CONFIG_LEGACY = "GET_CONFIG_RESULT";
    public static final String RESULT_GET_DATAWEDGE_STATUS = "com.symbol.datawedge.api.RESULT_GET_DATAWEDGE_STATUS";
    public static final String RESULT_GET_DISABLED_APP_LIST = "com.symbol.datawedge.api.RESULT_GET_DISABLED_APP_LIST";
    public static final String RESULT_GET_IGNORE_DISABLED_PROFILES = "com.symbol.datawedge.api.RESULT_GET_IGNORE_DISABLED_PROFILES";
    public static final String RESULT_GET_SCANNER_STATUS = "com.symbol.datawedge.api.RESULT_GET_SCANNER_STATUS";
    public static final String RESULT_ENUMERATE_SCANNERS = "com.symbol.datawedge.api.RESULT_ENUMERATE_SCANNERS";
    public static final String RESULT_ENUMERATED_SCANNER_LIST = "DWAPI_KEY_ENUMERATEDSCANNERLIST";
    public static final String RESULT_ENUMERATE_TRIGGERS = "com.symbol.datawedge.api.RESULT_ENUMERATE_TRIGGERS";

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILURE = "FAILURE";

    public static final String VALUE_LAST_RESULT = "LAST_RESULT";
    public static final String VALUE_ENABLE_PLUGIN = "ENABLE_PLUGIN";
    public static final String VALUE_DISABLE_PLUGIN = "DISABLE_PLUGIN";
    public static final String VALUE_START_SCANNING = "START_SCANNING";

    public static Intent newIntent() {
        Intent intent = new Intent(ACTION);
        intent.setPackage(DATAWEDGE_PACKAGE);
        return intent;
    }

    public static String newCommandIdentifier(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    public static JSObject parseScanIntent(Intent intent) {
        JSObject ret = new JSObject();
        ret.put("data", intent.getStringExtra(EXTRA_SCAN_DATA));
        ret.put("labelType", intent.getStringExtra(EXTRA_SCAN_LABEL_TYPE));
        ret.put("source", intent.getStringExtra(EXTRA_SCAN_SOURCE));
        ret.put("decodedMode", intent.getStringExtra(EXTRA_SCAN_DECODE_MODE));
        ret.put("intentAction", intent.getAction());
        return ret;
    }

    public static JSObject bundleToJSObject(Bundle bundle) {
        JSObject ret = new JSObject();
        if (bundle == null) {
            return ret;
        }
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            Object value = bundle.get(key);
            if (
                value instanceof String ||
                value instanceof Integer ||
                value instanceof Long ||
                value instanceof Double ||
                value instanceof Boolean
            ) {
                ret.put(key, value);
            } else if (value instanceof Bundle) {
                ret.put(key, bundleToJSObject((Bundle) value));
            } else if (value instanceof String[]) {
                ret.put(key, stringArrayToJSArray((String[]) value));
            } else if (value instanceof int[]) {
                ret.put(key, intArrayToJSArray((int[]) value));
            } else if (value instanceof ArrayList<?>) {
                ret.put(key, listToJSArray((ArrayList<?>) value));
            } else if (value instanceof Object[]) {
                ret.put(key, objectArrayToJSArray((Object[]) value));
            } else if (value != null) {
                ret.put(key, value.toString());
            }
        }
        return ret;
    }

    public static JSArray stringArrayToJSArray(String[] values) {
        JSArray array = new JSArray();
        if (values == null) {
            return array;
        }
        for (String value : values) {
            array.put(value);
        }
        return array;
    }

    public static JSArray intArrayToJSArray(int[] values) {
        JSArray array = new JSArray();
        if (values == null) {
            return array;
        }
        for (int value : values) {
            array.put(value);
        }
        return array;
    }

    public static JSArray objectArrayToJSArray(Object[] values) {
        JSArray array = new JSArray();
        if (values == null) {
            return array;
        }
        for (Object value : values) {
            array.put(normalizeToJSON(value));
        }
        return array;
    }

    public static JSArray listToJSArray(List<?> values) {
        JSArray array = new JSArray();
        if (values == null) {
            return array;
        }
        for (Object value : values) {
            array.put(normalizeToJSON(value));
        }
        return array;
    }

    public static Bundle jsonObjectToBundle(JSObject object) {
        if (object == null) {
            return new Bundle();
        }
        return jsonToBundle(object);
    }

    public static Bundle jsonToBundle(JSONObject jsonObject) {
        Bundle bundle = new Bundle();
        if (jsonObject == null) {
            return bundle;
        }
        JSONArray names = jsonObject.names();
        if (names == null) {
            return bundle;
        }
        for (int i = 0; i < names.length(); i++) {
            try {
                String key = names.getString(i);
                Object value = jsonObject.get(key);
                putValue(bundle, key, value);
            } catch (JSONException ignored) {}
        }
        return bundle;
    }

    public static String[] jsonArrayToStringArray(JSONArray array) {
        if (array == null) {
            return new String[0];
        }
        String[] values = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            values[i] = array.optString(i);
        }
        return values;
    }

    public static int[] jsonArrayToIntArray(JSONArray array) {
        if (array == null) {
            return new int[0];
        }
        int[] values = new int[array.length()];
        for (int i = 0; i < array.length(); i++) {
            values[i] = array.optInt(i);
        }
        return values;
    }

    private static void putValue(Bundle bundle, String key, Object value) {
        if (value == null || value == JSONObject.NULL) {
            return;
        }
        if (value instanceof String) {
            bundle.putString(key, (String) value);
            return;
        }
        if (value instanceof Integer) {
            bundle.putInt(key, (Integer) value);
            return;
        }
        if (value instanceof Long) {
            bundle.putLong(key, (Long) value);
            return;
        }
        if (value instanceof Double) {
            bundle.putDouble(key, (Double) value);
            return;
        }
        if (value instanceof Boolean) {
            bundle.putBoolean(key, (Boolean) value);
            return;
        }
        if (value instanceof JSONObject) {
            bundle.putBundle(key, jsonToBundle((JSONObject) value));
            return;
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            if (array.length() == 0) {
                bundle.putStringArrayList(key, new ArrayList<>());
                return;
            }
            Object first = array.opt(0);
            if (first instanceof JSONObject) {
                ArrayList<Bundle> bundles = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    Object item = array.opt(i);
                    if (item instanceof JSONObject) {
                        bundles.add(jsonToBundle((JSONObject) item));
                    }
                }
                bundle.putParcelableArrayList(key, bundles);
                return;
            }
            if (first instanceof Integer) {
                bundle.putIntArray(key, jsonArrayToIntArray(array));
                return;
            }
            String[] values = jsonArrayToStringArray(array);
            bundle.putStringArray(key, values);
        }
    }

    private static Object normalizeToJSON(Object value) {
        if (value instanceof Bundle) {
            return bundleToJSObject((Bundle) value);
        }
        if (value instanceof String[]) {
            return stringArrayToJSArray((String[]) value);
        }
        if (value instanceof int[]) {
            return intArrayToJSArray((int[]) value);
        }
        if (value instanceof ArrayList<?>) {
            return listToJSArray((ArrayList<?>) value);
        }
        if (value instanceof Object[]) {
            return objectArrayToJSArray((Object[]) value);
        }
        return value;
    }
}
