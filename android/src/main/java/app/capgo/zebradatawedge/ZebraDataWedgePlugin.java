package app.capgo.zebradatawedge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@CapacitorPlugin(name = "ZebraDataWedge")
public class ZebraDataWedgePlugin extends Plugin {

    private static final String PLUGIN_VERSION = "8.0.0";
    private static final long REQUEST_TIMEOUT_MS = 8000L;
    private static final long SCAN_TIMEOUT_MS = 15000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, ScanRequest> pendingScans = new ConcurrentHashMap<>();
    private final Set<String> registeredIntentActions = Collections.synchronizedSet(new LinkedHashSet<>());
    private BroadcastReceiver receiver;
    private boolean receiverRegistered = false;

    @Override
    public void load() {
        ensureReceiver();
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        if (receiverRegistered && receiver != null) {
            try {
                getContext().unregisterReceiver(receiver);
            } catch (IllegalArgumentException ignored) {}
            receiverRegistered = false;
        }
        for (PendingRequest request : pendingRequests.values()) {
            handler.removeCallbacks(request.timeoutRunnable);
            request.call.reject("Timed out waiting for DataWedge response.");
        }
        pendingRequests.clear();
        for (ScanRequest request : pendingScans.values()) {
            handler.removeCallbacks(request.timeoutRunnable);
            request.call.reject("Timed out waiting for a Zebra scan result.");
        }
        pendingScans.clear();
    }

    @PluginMethod
    public void cloneProfile(PluginCall call) {
        String source = requiredString(call, "sourceProfileName");
        String destination = requiredString(call, "destProfileName");
        if (source == null || destination == null) {
            return;
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_CLONE_PROFILE, new String[] { source, destination });
        sendCommand(call, intent, "clone-profile", this::emptyResponse);
    }

    @PluginMethod
    public void createProfile(PluginCall call) {
        String profileName = requiredString(call, "profileName");
        if (profileName == null) {
            return;
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_CREATE_PROFILE, profileName);
        sendCommand(call, intent, "create-profile", this::emptyResponse);
    }

    @PluginMethod
    public void deleteProfile(PluginCall call) {
        JSONArray profileNames = call.getArray("profileNames");
        if (profileNames == null || profileNames.length() == 0) {
            call.reject("profileNames is required.");
            return;
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_DELETE_PROFILE, ZebraDataWedge.jsonArrayToStringArray(profileNames));
        sendCommand(call, intent, "delete-profile", this::emptyResponse);
    }

    @PluginMethod
    public void importConfig(PluginCall call) {
        String folderPath = requiredString(call, "folderPath");
        if (folderPath == null) {
            return;
        }
        Bundle options = new Bundle();
        options.putString("FOLDER_PATH", folderPath);
        JSONArray fileList = call.getArray("fileList");
        if (fileList != null && fileList.length() > 0) {
            options.putStringArray("FILE_LIST", ZebraDataWedge.jsonArrayToStringArray(fileList));
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_IMPORT_CONFIG, options);
        sendCommand(call, intent, "import-config", this::emptyResponse);
    }

    @PluginMethod
    public void renameProfile(PluginCall call) {
        String currentProfileName = requiredString(call, "currentProfileName");
        String newProfileName = requiredString(call, "newProfileName");
        if (currentProfileName == null || newProfileName == null) {
            return;
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_RENAME_PROFILE, new String[] { currentProfileName, newProfileName });
        sendCommand(call, intent, "rename-profile", this::emptyResponse);
    }

    @PluginMethod
    public void restoreConfig(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_RESTORE_CONFIG, "");
        sendCommand(call, intent, "restore-config", this::emptyResponse);
    }

    @PluginMethod
    public void setConfig(PluginCall call) {
        String profileName = requiredString(call, "profileName");
        if (profileName == null) {
            return;
        }
        Bundle config = new Bundle();
        config.putString("PROFILE_NAME", profileName);
        Boolean profileEnabled = call.getBoolean("profileEnabled");
        if (profileEnabled != null) {
            config.putString("PROFILE_ENABLED", Boolean.toString(profileEnabled));
        }
        String configMode = call.getString("configMode");
        if (configMode != null) {
            config.putString("CONFIG_MODE", configMode);
        }
        JSONArray appList = call.getArray("appList");
        if (appList != null && appList.length() > 0) {
            config.putParcelableArray("APP_LIST", parseAppList(appList));
        }
        JSONArray pluginConfigs = call.getArray("pluginConfigs");
        if (pluginConfigs != null && pluginConfigs.length() > 0) {
            config.putParcelableArrayList("PLUGIN_CONFIG", parsePluginConfigs(pluginConfigs));
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SET_CONFIG, config);
        sendCommand(call, intent, "set-config", this::emptyResponse);
    }

    @PluginMethod
    public void setDisabledAppList(PluginCall call) {
        String configMode = requiredString(call, "configMode");
        if (configMode == null) {
            return;
        }
        Bundle config = new Bundle();
        config.putString("CONFIG_MODE", configMode);
        JSONArray appList = call.getArray("appList");
        if (appList != null && appList.length() > 0) {
            config.putParcelableArray("APP_LIST", parseAppList(appList));
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SET_DISABLED_APP_LIST, config);
        sendCommand(call, intent, "set-disabled-app-list", this::emptyResponse);
    }

    @PluginMethod
    public void setIgnoreDisabledProfiles(PluginCall call) {
        Boolean enabled = call.getBoolean("enabled");
        if (enabled == null) {
            call.reject("enabled is required.");
            return;
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SET_IGNORE_DISABLED_PROFILES, enabled);
        sendCommand(call, intent, "set-ignore-disabled-profiles", this::emptyResponse);
    }

    @PluginMethod
    public void registerForNotification(PluginCall call) {
        String notificationType = requiredString(call, "notificationType");
        if (notificationType == null) {
            return;
        }
        String intentAction = call.getString("intentAction");
        if (intentAction != null && !intentAction.isEmpty()) {
            ensureIntentAction(intentAction);
        }
        Bundle options = new Bundle();
        options.putString("APPLICATION_NAME", call.getString("appName", getContext().getPackageName()));
        options.putString("NOTIFICATION_TYPE", notificationType);
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_REGISTER_FOR_NOTIFICATION, options);
        sendCommand(call, intent, "register-for-notification", this::emptyResponse);
    }

    @PluginMethod
    public void unRegisterForNotification(PluginCall call) {
        String notificationType = requiredString(call, "notificationType");
        if (notificationType == null) {
            return;
        }
        Bundle options = new Bundle();
        options.putString("APPLICATION_NAME", call.getString("appName", getContext().getPackageName()));
        options.putString("NOTIFICATION_TYPE", notificationType);
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_UNREGISTER_FOR_NOTIFICATION, options);
        sendCommand(call, intent, "unregister-for-notification", this::emptyResponse);
    }

    @PluginMethod
    public void enumerateScanners(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_ENUMERATE_SCANNERS, "");
        sendCommand(call, intent, "enumerate-scanners", this::parseEnumerateScanners);
    }

    @PluginMethod
    public void getActiveProfile(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_GET_ACTIVE_PROFILE, "");
        sendCommand(call, intent, "get-active-profile", this::parseActiveProfile);
    }

    @PluginMethod
    public void getAssociatedApps(PluginCall call) {
        String profileName = requiredString(call, "profileName");
        if (profileName == null) {
            return;
        }
        Bundle options = new Bundle();
        options.putString("PROFILE_NAME", profileName);
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_GET_CONFIG, options);
        sendCommand(call, intent, "get-associated-apps", (response) -> {
            JSObject config = parseConfig(response);
            JSObject ret = new JSObject();
            ret.put("appList", config.optJSONArray("appList"));
            return ret;
        });
    }

    @PluginMethod
    public void getConfig(PluginCall call) {
        String profileName = requiredString(call, "profileName");
        if (profileName == null) {
            return;
        }
        Bundle options = new Bundle();
        options.putString("PROFILE_NAME", profileName);
        JSONArray pluginConfigs = call.getArray("pluginConfigs");
        if (pluginConfigs != null && pluginConfigs.length() > 0) {
            ArrayList<Bundle> bundles = new ArrayList<>();
            for (int i = 0; i < pluginConfigs.length(); i++) {
                JSONObject jsonObject = pluginConfigs.optJSONObject(i);
                if (jsonObject == null) {
                    continue;
                }
                Bundle config = new Bundle();
                config.putString("PLUGIN_NAME", jsonObject.optString("pluginName"));
                if (jsonObject.has("outputPluginName")) {
                    config.putString("OUTPUT_PLUGIN_NAME", jsonObject.optString("outputPluginName"));
                }
                bundles.add(config);
            }
            options.putParcelableArrayList("PLUGIN_CONFIG", bundles);
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_GET_CONFIG, options);
        sendCommand(call, intent, "get-config", this::parseConfig);
    }

    @PluginMethod
    public void getDatawedgeStatus(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_GET_DATAWEDGE_STATUS, "");
        sendCommand(call, intent, "get-datawedge-status", this::parseDataWedgeStatus);
    }

    @PluginMethod
    public void getDisabledAppList(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_GET_DISABLED_APP_LIST, "");
        sendCommand(call, intent, "get-disabled-app-list", this::parseDisabledAppList);
    }

    @PluginMethod
    public void getIgnoreDisabledProfiles(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_GET_IGNORE_DISABLED_PROFILES, "");
        sendCommand(call, intent, "get-ignore-disabled-profiles", this::parseIgnoreDisabledProfiles);
    }

    @PluginMethod
    public void getProfilesList(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_GET_PROFILES_LIST, "");
        sendCommand(call, intent, "get-profiles-list", this::parseProfilesList);
    }

    @PluginMethod
    public void getScannerStatus(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_GET_SCANNER_STATUS, "");
        sendCommand(call, intent, "get-scanner-status", this::parseScannerStatus);
    }

    @PluginMethod
    public void getVersionInfo(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_GET_VERSION_INFO, "");
        sendCommand(call, intent, "get-version-info", this::parseVersionInfo);
    }

    @PluginMethod
    public void disableDatawedge(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_ENABLE_DATAWEDGE, false);
        sendCommand(call, intent, "disable-datawedge", this::emptyResponse);
    }

    @PluginMethod
    public void disableScannerInput(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SCANNER_INPUT_PLUGIN, ZebraDataWedge.VALUE_DISABLE_PLUGIN);
        sendCommand(call, intent, "disable-scanner-input", this::emptyResponse);
    }

    @PluginMethod
    public void enableDatawedge(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_ENABLE_DATAWEDGE, true);
        sendCommand(call, intent, "enable-datawedge", this::emptyResponse);
    }

    @PluginMethod
    public void enableScannerInput(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SCANNER_INPUT_PLUGIN, ZebraDataWedge.VALUE_ENABLE_PLUGIN);
        sendCommand(call, intent, "enable-scanner-input", this::emptyResponse);
    }

    @PluginMethod
    public void enumerateTriggers(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_ENUMERATE_TRIGGERS, "");
        sendCommand(call, intent, "enumerate-triggers", this::parseTriggers);
    }

    @PluginMethod
    public void notify(PluginCall call) {
        String deviceIdentifier = requiredString(call, "deviceIdentifier");
        JSONArray notificationSettings = call.getArray("notificationSettings");
        if (deviceIdentifier == null || notificationSettings == null) {
            if (notificationSettings == null) {
                call.reject("notificationSettings is required.");
            }
            return;
        }
        Bundle options = new Bundle();
        options.putString("DEVICE_IDENTIFIER", deviceIdentifier);
        options.putIntArray("NOTIFICATION_SETTINGS", ZebraDataWedge.jsonArrayToIntArray(notificationSettings));
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_NOTIFY, options);
        sendCommand(call, intent, "notify", this::emptyResponse);
    }

    @PluginMethod
    public void resetDefaultProfile(PluginCall call) {
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_RESET_DEFAULT_PROFILE, "");
        sendCommand(call, intent, "reset-default-profile", this::emptyResponse);
    }

    @PluginMethod
    public void setDefaultProfile(PluginCall call) {
        String profileName = requiredString(call, "profileName");
        if (profileName == null) {
            return;
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SET_DEFAULT_PROFILE, profileName);
        sendCommand(call, intent, "set-default-profile", this::emptyResponse);
    }

    @PluginMethod
    public void setReportingOptions(PluginCall call) {
        Boolean reportingEnabled = call.getBoolean("reportingEnabled");
        if (reportingEnabled == null) {
            call.reject("reportingEnabled is required.");
            return;
        }
        Bundle options = new Bundle();
        options.putString("REPORTING_ENABLED", Boolean.toString(reportingEnabled));
        if (call.hasOption("reportingGenerateOption")) {
            options.putString("REPORTING_GENERATE_OPTION", call.getString("reportingGenerateOption"));
        }
        if (call.hasOption("reportingShowForManualImport")) {
            options.putString("REPORTING_SHOW_FOR_MANUAL_IMPORT", Boolean.toString(call.getBoolean("reportingShowForManualImport", false)));
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SET_REPORTING_OPTIONS, options);
        sendCommand(call, intent, "set-reporting-options", this::emptyResponse);
    }

    @PluginMethod
    public void softRfidTrigger(PluginCall call) {
        String action = registeredIntentActions.stream().findFirst().orElse(null);
        if (action == null) {
            call.reject(
                "No scan intentAction is registered. Call softScanTrigger(intentAction) or registerForNotification({ intentAction }) first."
            );
            return;
        }
        startPendingScan(call, action);
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SOFT_RFID_TRIGGER, ZebraDataWedge.VALUE_START_SCANNING);
        sendBroadcast(intent, call);
    }

    @PluginMethod
    public void softScanTrigger(PluginCall call) {
        String intentAction = requiredString(call, "intentAction");
        if (intentAction == null) {
            return;
        }
        ensureIntentAction(intentAction);
        startPendingScan(call, intentAction);
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SOFT_SCAN_TRIGGER, ZebraDataWedge.VALUE_START_SCANNING);
        sendBroadcast(intent, call);
    }

    @PluginMethod
    public void switchScanner(PluginCall call) {
        String scannerIdentifier = call.getString("scannerIdentifier");
        String scannerIndex = call.getString("scannerIndex");
        String value = scannerIdentifier != null ? scannerIdentifier : scannerIndex;
        if (value == null || value.isEmpty()) {
            call.reject("scannerIdentifier or scannerIndex is required.");
            return;
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SWITCH_SCANNER, value);
        sendCommand(call, intent, "switch-scanner", this::emptyResponse);
    }

    @PluginMethod
    public void switchScannerParams(PluginCall call) {
        String scannerIdentifier = requiredString(call, "scannerIdentifier");
        JSObject scannerParams = call.getObject("scannerParams");
        if (scannerIdentifier == null || scannerParams == null) {
            if (scannerParams == null) {
                call.reject("scannerParams is required.");
            }
            return;
        }
        Bundle options = new Bundle();
        options.putString("SCANNER_IDENTIFIER", scannerIdentifier);
        options.putBundle("SCANNER_PARAMS", ZebraDataWedge.jsonObjectToBundle(scannerParams));
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SWITCH_SCANNER_PARAMS, options);
        sendCommand(call, intent, "switch-scanner-params", this::emptyResponse);
    }

    @PluginMethod
    public void switchToProfile(PluginCall call) {
        String profileName = requiredString(call, "profileName");
        if (profileName == null) {
            return;
        }
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SWITCH_TO_PROFILE, profileName);
        sendCommand(call, intent, "switch-to-profile", this::emptyResponse);
    }

    @PluginMethod
    public void getPluginVersion(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("version", PLUGIN_VERSION);
        call.resolve(ret);
    }

    private void ensureReceiver() {
        if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    handleIntent(intent);
                }
            };
        }
        refreshReceiver();
    }

    private void ensureIntentAction(String intentAction) {
        if (registeredIntentActions.add(intentAction)) {
            refreshReceiver();
        }
    }

    private void refreshReceiver() {
        if (receiver == null) {
            return;
        }
        if (receiverRegistered) {
            try {
                getContext().unregisterReceiver(receiver);
            } catch (IllegalArgumentException ignored) {}
            receiverRegistered = false;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ZebraDataWedge.RESULT_ACTION);
        filter.addAction(ZebraDataWedge.NOTIFICATION_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        synchronized (registeredIntentActions) {
            for (String action : registeredIntentActions) {
                filter.addAction(action);
            }
        }
        ContextCompat.registerReceiver(getContext(), receiver, filter, ContextCompat.RECEIVER_EXPORTED);
        receiverRegistered = true;
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (ZebraDataWedge.RESULT_ACTION.equals(action)) {
            String commandId = intent.getStringExtra(ZebraDataWedge.EXTRA_COMMAND_IDENTIFIER);
            if (commandId != null) {
                PendingRequest request = pendingRequests.remove(commandId);
                if (request != null) {
                    handler.removeCallbacks(request.timeoutRunnable);
                    resolvePendingRequest(request, intent);
                    return;
                }
            }
        }

        if (intent.hasExtra(ZebraDataWedge.EXTRA_NOTIFICATION)) {
            notifyListeners("notification", parseNotification(intent));
        }

        if (intent.hasExtra(ZebraDataWedge.EXTRA_SCAN_DATA)) {
            JSObject payload = ZebraDataWedge.parseScanIntent(intent);
            notifyListeners("scan", payload);
            resolvePendingScan(intent.getAction(), payload);
        }
    }

    private void sendCommand(PluginCall call, Intent intent, String prefix, ResponseParser parser) {
        String commandId = ZebraDataWedge.newCommandIdentifier(prefix);
        intent.putExtra(ZebraDataWedge.EXTRA_SEND_RESULT, ZebraDataWedge.VALUE_LAST_RESULT);
        intent.putExtra(ZebraDataWedge.EXTRA_COMMAND_IDENTIFIER, commandId);
        Runnable timeout = () -> {
            PendingRequest removed = pendingRequests.remove(commandId);
            if (removed != null) {
                removed.call.reject("Timed out waiting for DataWedge response.");
            }
        };
        pendingRequests.put(commandId, new PendingRequest(call, parser, timeout));
        handler.postDelayed(timeout, REQUEST_TIMEOUT_MS);
        sendBroadcast(intent, call);
    }

    private void sendBroadcast(Intent intent, PluginCall call) {
        try {
            getContext().sendBroadcast(intent);
        } catch (Exception exception) {
            call.reject("Failed to communicate with Zebra DataWedge.", exception);
        }
    }

    private void resolvePendingRequest(PendingRequest request, Intent intent) {
        try {
            rejectIfFailure(intent);
            request.call.resolve(request.parser.parse(intent));
        } catch (ZebraDataWedgeException exception) {
            request.call.reject(exception.getMessage(), exception.getCode());
        } catch (Exception exception) {
            request.call.reject(exception.getMessage(), exception);
        }
    }

    private void startPendingScan(PluginCall call, String intentAction) {
        Runnable timeout = () -> {
            ScanRequest removed = pendingScans.remove(intentAction);
            if (removed != null) {
                removed.call.reject("Timed out waiting for a Zebra scan result.");
            }
        };
        ScanRequest previous = pendingScans.put(intentAction, new ScanRequest(call, timeout));
        if (previous != null) {
            handler.removeCallbacks(previous.timeoutRunnable);
            previous.call.reject("A scan request was replaced by a newer request.");
        }
        handler.postDelayed(timeout, SCAN_TIMEOUT_MS);
    }

    private void resolvePendingScan(String action, JSObject payload) {
        ScanRequest request = pendingScans.remove(action);
        if (request == null) {
            return;
        }
        handler.removeCallbacks(request.timeoutRunnable);
        request.call.resolve(payload);
    }

    private void rejectIfFailure(Intent intent) throws ZebraDataWedgeException {
        String result = intent.getStringExtra(ZebraDataWedge.EXTRA_RESULT);
        Bundle resultInfo = intent.getBundleExtra(ZebraDataWedge.EXTRA_RESULT_INFO);
        if (ZebraDataWedge.RESULT_FAILURE.equalsIgnoreCase(result)) {
            throw errorFromResult(resultInfo);
        }
        if (resultInfo != null) {
            String resultCode = resultInfo.getString("RESULT_CODE");
            if (resultCode != null && !resultCode.isEmpty() && ZebraDataWedge.RESULT_FAILURE.equalsIgnoreCase(result)) {
                throw errorFromResult(resultInfo);
            }
        }
    }

    private ZebraDataWedgeException errorFromResult(Bundle resultInfo) {
        String code = resultInfo != null ? resultInfo.getString("RESULT_CODE", "UNKNOWN") : "UNKNOWN";
        String message = resultInfo != null ? ZebraDataWedge.bundleToJSObject(resultInfo).toString() : "Unknown DataWedge error.";
        return new ZebraDataWedgeException(code, message);
    }

    private JSObject emptyResponse(Intent intent) {
        return new JSObject();
    }

    private JSObject parseProfilesList(Intent intent) {
        String[] profiles = firstStringArray(intent, ZebraDataWedge.RESULT_GET_PROFILES_LIST);
        JSObject ret = new JSObject();
        ret.put("profiles", ZebraDataWedge.stringArrayToJSArray(profiles));
        return ret;
    }

    private JSObject parseActiveProfile(Intent intent) {
        JSObject ret = new JSObject();
        ret.put("profileName", firstString(intent, ZebraDataWedge.RESULT_GET_ACTIVE_PROFILE, "PROFILE_NAME"));
        return ret;
    }

    private JSObject parseDataWedgeStatus(Intent intent) {
        String rawStatus = firstString(intent, ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "DATAWEDGE_STATUS");
        JSObject ret = new JSObject();
        ret.put("isEnabled", rawStatus != null && ("enabled".equalsIgnoreCase(rawStatus) || Boolean.parseBoolean(rawStatus)));
        return ret;
    }

    private JSObject parseIgnoreDisabledProfiles(Intent intent) {
        String rawValue = firstString(intent, ZebraDataWedge.RESULT_GET_IGNORE_DISABLED_PROFILES, "IGNORE_DISABLED_PROFILES");
        JSObject ret = new JSObject();
        ret.put("enabled", rawValue != null && ("true".equalsIgnoreCase(rawValue) || "enabled".equalsIgnoreCase(rawValue)));
        return ret;
    }

    private JSObject parseScannerStatus(Intent intent) {
        JSObject ret = new JSObject();
        ret.put("status", firstString(intent, ZebraDataWedge.RESULT_GET_SCANNER_STATUS, "SCANNER_STATUS"));
        return ret;
    }

    private JSObject parseTriggers(Intent intent) {
        String[] triggers = firstStringArray(intent, ZebraDataWedge.RESULT_ENUMERATE_TRIGGERS, "ENUMERATE_TRIGGERS");
        JSObject ret = new JSObject();
        ret.put("triggers", ZebraDataWedge.stringArrayToJSArray(triggers));
        return ret;
    }

    private JSObject parseDisabledAppList(Intent intent) {
        JSObject ret = new JSObject();
        ret.put("appList", parseAppListResult(extractBundleList(intent, ZebraDataWedge.RESULT_GET_DISABLED_APP_LIST)));
        return ret;
    }

    private JSObject parseVersionInfo(Intent intent) {
        Bundle versionInfo = firstBundle(intent, ZebraDataWedge.RESULT_GET_VERSION_INFO, "GET_VERSION_INFO_RESULT");
        JSObject ret = new JSObject();
        if (versionInfo == null) {
            return ret;
        }
        ret.put("dataWedgeVersion", versionInfo.getString("DATAWEDGE"));
        ret.put("barcodeVersion", versionInfo.getString("BARCODE_SCANNING"));
        ret.put("decoderVersion", versionInfo.getString("DECODER_LIBRARY"));
        ret.put("simulScanVersion", versionInfo.getString("SIMULSCAN"));
        String[] firmware = versionInfo.getStringArray("SCANNER_FIRMWARE");
        if (firmware != null) {
            ret.put("scannerFirmwareVersions", ZebraDataWedge.stringArrayToJSArray(firmware));
        }
        return ret;
    }

    private JSObject parseConfig(Intent intent) {
        Bundle config = firstBundle(intent, ZebraDataWedge.RESULT_GET_CONFIG, ZebraDataWedge.RESULT_GET_CONFIG_LEGACY);
        JSObject ret = new JSObject();
        if (config == null) {
            return ret;
        }
        ret.put("profileName", config.getString("PROFILE_NAME"));
        ret.put("profileEnabled", "true".equalsIgnoreCase(config.getString("PROFILE_ENABLED")));
        ret.put("pluginConfigList", parsePluginConfigResult(extractPluginConfig(config)));
        ret.put("appList", parseAppListResult(extractAppList(config)));
        return ret;
    }

    private JSObject parseEnumerateScanners(Intent intent) {
        JSArray scanners = new JSArray();
        ArrayList<Bundle> bundles = extractBundleList(intent, ZebraDataWedge.RESULT_ENUMERATE_SCANNERS);
        if (bundles != null && !bundles.isEmpty()) {
            for (Bundle bundle : bundles) {
                scanners.put(parseScannerBundle(bundle));
            }
        } else {
            String[] legacyScanners = firstStringArray(intent, ZebraDataWedge.RESULT_ENUMERATED_SCANNER_LIST);
            if (legacyScanners != null) {
                for (String scanner : legacyScanners) {
                    scanners.put(parseLegacyScanner(scanner));
                }
            }
        }
        JSObject ret = new JSObject();
        ret.put("scanners", scanners);
        return ret;
    }

    private JSArray parsePluginConfigResult(ArrayList<Bundle> pluginConfigs) {
        JSArray result = new JSArray();
        if (pluginConfigs == null) {
            return result;
        }
        for (Bundle config : pluginConfigs) {
            if (config == null) {
                continue;
            }
            JSObject item = new JSObject();
            item.put("pluginName", config.getString("PLUGIN_NAME"));
            item.put("outputPluginName", config.getString("OUTPUT_PLUGIN_NAME"));
            item.put("resetConfig", "true".equalsIgnoreCase(config.getString("RESET_CONFIG")));
            item.put("paramList", ZebraDataWedge.bundleToJSObject(config.getBundle("PARAM_LIST")));
            result.put(item);
        }
        return result;
    }

    private JSArray parseAppListResult(ArrayList<Bundle> appList) {
        JSArray result = new JSArray();
        if (appList == null) {
            return result;
        }
        for (Bundle app : appList) {
            if (app == null) {
                continue;
            }
            JSObject item = new JSObject();
            item.put("packageName", app.getString("PACKAGE_NAME"));
            item.put("activityList", ZebraDataWedge.stringArrayToJSArray(app.getStringArray("ACTIVITY_LIST")));
            result.put(item);
        }
        return result;
    }

    private JSObject parseNotification(Intent intent) {
        Bundle notification = intent.getBundleExtra(ZebraDataWedge.EXTRA_NOTIFICATION);
        JSObject raw = ZebraDataWedge.bundleToJSObject(notification);
        JSObject ret = new JSObject();
        ret.put("notificationType", notification != null ? notification.getString("NOTIFICATION_TYPE") : null);
        ret.put("profileName", notification != null ? notification.getString("PROFILE_NAME") : null);
        ret.put("status", notification != null ? notification.getString("STATUS") : null);
        ret.put("scannerStatus", notification != null ? notification.getString("SCANNER_STATUS") : null);
        ret.put("raw", raw);
        return ret;
    }

    private String requiredString(PluginCall call, String key) {
        String value = call.getString(key);
        if (value == null || value.isEmpty()) {
            call.reject(key + " is required.");
            return null;
        }
        return value;
    }

    private ArrayList<Bundle> parsePluginConfigs(JSONArray pluginConfigs) {
        ArrayList<Bundle> bundles = new ArrayList<>();
        for (int i = 0; i < pluginConfigs.length(); i++) {
            JSONObject json = pluginConfigs.optJSONObject(i);
            if (json == null) {
                continue;
            }
            Bundle config = new Bundle();
            config.putString("PLUGIN_NAME", json.optString("pluginName"));
            config.putString("RESET_CONFIG", Boolean.toString(json.optBoolean("resetConfig", true)));
            if (json.has("outputPluginName")) {
                config.putString("OUTPUT_PLUGIN_NAME", json.optString("outputPluginName"));
            }
            JSONObject paramList = json.optJSONObject("paramList");
            if (paramList != null) {
                config.putBundle("PARAM_LIST", ZebraDataWedge.jsonToBundle(paramList));
            }
            bundles.add(config);
        }
        return bundles;
    }

    private Bundle[] parseAppList(JSONArray appList) {
        List<Bundle> bundles = new ArrayList<>();
        for (int i = 0; i < appList.length(); i++) {
            JSONObject json = appList.optJSONObject(i);
            if (json == null) {
                continue;
            }
            Bundle app = new Bundle();
            app.putString("PACKAGE_NAME", json.optString("packageName"));
            app.putStringArray("ACTIVITY_LIST", ZebraDataWedge.jsonArrayToStringArray(json.optJSONArray("activityList")));
            bundles.add(app);
        }
        return bundles.toArray(new Bundle[0]);
    }

    private ArrayList<Bundle> extractPluginConfig(Bundle bundle) {
        ArrayList<Bundle> result = bundle.getParcelableArrayList("PLUGIN_CONFIG");
        if (result != null) {
            return result;
        }
        Bundle single = bundle.getBundle("PLUGIN_CONFIG");
        if (single == null) {
            return null;
        }
        ArrayList<Bundle> list = new ArrayList<>();
        list.add(single);
        return list;
    }

    private ArrayList<Bundle> extractAppList(Bundle bundle) {
        Object raw = bundle.get("APP_LIST");
        if (raw instanceof Bundle[]) {
            Bundle[] values = (Bundle[]) raw;
            ArrayList<Bundle> result = new ArrayList<>();
            Collections.addAll(result, values);
            return result;
        }
        if (raw instanceof ArrayList<?>) {
            ArrayList<Bundle> result = new ArrayList<>();
            for (Object value : (ArrayList<?>) raw) {
                if (value instanceof Bundle) {
                    result.add((Bundle) value);
                }
            }
            return result;
        }
        return null;
    }

    private ArrayList<Bundle> extractBundleList(Intent intent, String... keys) {
        for (String key : keys) {
            Object raw = intent.getExtras() != null ? intent.getExtras().get(key) : null;
            if (raw instanceof ArrayList<?>) {
                ArrayList<Bundle> result = new ArrayList<>();
                for (Object value : (ArrayList<?>) raw) {
                    if (value instanceof Bundle) {
                        result.add((Bundle) value);
                    }
                }
                return result;
            }
            if (raw instanceof Bundle[]) {
                ArrayList<Bundle> result = new ArrayList<>();
                Collections.addAll(result, (Bundle[]) raw);
                return result;
            }
        }
        return null;
    }

    private Bundle firstBundle(Intent intent, String... keys) {
        for (String key : keys) {
            Bundle value = intent.getBundleExtra(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstString(Intent intent, String... keys) {
        for (String key : keys) {
            String value = intent.getStringExtra(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String[] firstStringArray(Intent intent, String... keys) {
        for (String key : keys) {
            String[] value = intent.getStringArrayExtra(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private JSObject parseScannerBundle(Bundle bundle) {
        JSObject ret = new JSObject();
        ret.put("index", bundle.getInt("SCANNER_INDEX"));
        ret.put("name", bundle.getString("SCANNER_NAME"));
        ret.put("id", bundle.getString("SCANNER_IDENTIFIER"));
        ret.put(
            "connectionState",
            "true".equalsIgnoreCase(bundle.getString("SCANNER_CONNECTION_STATE")) || bundle.getBoolean("SCANNER_CONNECTION_STATE")
        );
        return ret;
    }

    private JSObject parseLegacyScanner(String scanner) {
        JSObject ret = new JSObject();
        ret.put("index", -1);
        ret.put("name", scanner);
        ret.put("id", scanner);
        ret.put("connectionState", false);
        return ret;
    }

    private interface ResponseParser {
        JSObject parse(Intent intent) throws Exception;
    }

    private static class PendingRequest {

        final PluginCall call;
        final ResponseParser parser;
        final Runnable timeoutRunnable;

        PendingRequest(PluginCall call, ResponseParser parser, Runnable timeoutRunnable) {
            this.call = call;
            this.parser = parser;
            this.timeoutRunnable = timeoutRunnable;
        }
    }

    private static class ScanRequest {

        final PluginCall call;
        final Runnable timeoutRunnable;

        ScanRequest(PluginCall call, Runnable timeoutRunnable) {
            this.call = call;
            this.timeoutRunnable = timeoutRunnable;
        }
    }

    private static class ZebraDataWedgeException extends Exception {

        private final String code;

        ZebraDataWedgeException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
