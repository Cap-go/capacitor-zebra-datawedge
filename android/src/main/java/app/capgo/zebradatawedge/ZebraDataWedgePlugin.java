package app.capgo.zebradatawedge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.Arrays;
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

    // Correlation and parsing share the same keys, including documented alternatives and existing fallbacks.
    private static final String[] ACTIVE_PROFILE_RESULTS = { ZebraDataWedge.RESULT_GET_ACTIVE_PROFILE, "PROFILE_NAME" };
    private static final String[] CONFIG_RESULTS = {
        ZebraDataWedge.RESULT_GET_CONFIG,
        "com.symbol.datawedge.api.GET_CONFIG_RESULT",
        ZebraDataWedge.RESULT_GET_CONFIG_LEGACY
    };
    private static final String[] DATAWEDGE_STATUS_RESULTS = { ZebraDataWedge.RESULT_GET_DATAWEDGE_STATUS, "DATAWEDGE_STATUS" };
    private static final String[] PROFILES_LIST_RESULTS = { ZebraDataWedge.RESULT_GET_PROFILES_LIST };
    private static final String[] SCANNER_STATUS_RESULTS = { ZebraDataWedge.RESULT_GET_SCANNER_STATUS, "SCANNER_STATUS" };
    private static final String[] VERSION_INFO_RESULTS = { ZebraDataWedge.RESULT_GET_VERSION_INFO, "GET_VERSION_INFO_RESULT" };
    private static final String[] DISABLED_APPS_RESULTS = { ZebraDataWedge.RESULT_GET_DISABLED_APP_LIST };
    private static final String[] IGNORE_DISABLED_RESULTS = {
        ZebraDataWedge.RESULT_GET_IGNORE_DISABLED_PROFILES,
        "IGNORE_DISABLED_PROFILES"
    };
    private static final String[] TRIGGER_RESULTS = { ZebraDataWedge.RESULT_ENUMERATE_TRIGGERS, "ENUMERATE_TRIGGERS" };
    private static final String[] SCANNER_ENUMERATION_RESULTS = {
        ZebraDataWedge.RESULT_ENUMERATE_SCANNERS,
        ZebraDataWedge.RESULT_ENUMERATED_SCANNER_LIST
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, ScanRequest> pendingScans = new ConcurrentHashMap<>();
    private final Set<String> manualIntentActions = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Map<String, Set<String>> profileScanIntentActions = new ConcurrentHashMap<>();
    private final Set<String> registeredIntentActions = Collections.synchronizedSet(new LinkedHashSet<>());
    private final DataWedgeDiagnostics diagnostics = new DataWedgeDiagnostics();
    private BroadcastReceiver receiver;
    private boolean receiverRegistered = false;

    @PluginMethod
    public void configureDiagnostics(PluginCall call) {
        Integer capacity = call.getInt("maxEntries", 100);
        if (capacity == null || capacity < 1 || capacity > 1000) {
            call.reject("maxEntries must be between 1 and 1000.", "PARAMETER_INVALID");
            return;
        }
        diagnostics.configure(
            Boolean.TRUE.equals(call.getBoolean("enabled", false)),
            capacity,
            Boolean.TRUE.equals(call.getBoolean("reset", false))
        );
        call.resolve();
    }

    @PluginMethod
    public void getDiagnostics(PluginCall call) {
        call.resolve(diagnostics.snapshot(getContext()));
    }

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
        for (Map.Entry<String, PendingRequest> entry : pendingRequests.entrySet()) {
            PendingRequest request = entry.getValue();
            diagnostics.record(entry.getKey(), "CANCELLED");
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
        final boolean declaresScanIntentRegistration =
            call.getString("scanIntentAction") != null || declaresIntentPluginConfig(pluginConfigs);
        final List<String> profileActions = declaresScanIntentRegistration
            ? ZebraDataWedge.collectScanIntentActions(call.getString("scanIntentAction"), pluginConfigs)
            : null;
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_SET_CONFIG, config);
        sendCommand(call, intent, "set-config", (resultIntent) -> {
            if (profileActions != null) {
                applyProfileScanIntentActions(profileName, profileActions);
            }
            return emptyResponse(resultIntent);
        });
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
            registerManualIntentAction(intentAction);
        }
        Bundle options = new Bundle();
        options.putString(ZebraDataWedge.EXTRA_APPLICATION_NAME, call.getString("appName", getContext().getPackageName()));
        options.putString(ZebraDataWedge.EXTRA_NOTIFICATION_TYPE, notificationType);
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_REGISTER_FOR_NOTIFICATION, options);
        dispatchNotification(intent, call);
    }

    @PluginMethod
    public void registerScanIntentAction(PluginCall call) {
        String intentAction = requiredString(call, "intentAction");
        if (intentAction == null) {
            return;
        }
        registerManualIntentAction(intentAction);
        call.resolve();
    }

    @PluginMethod
    public void unRegisterForNotification(PluginCall call) {
        String notificationType = requiredString(call, "notificationType");
        if (notificationType == null) {
            return;
        }
        Bundle options = new Bundle();
        options.putString(ZebraDataWedge.EXTRA_APPLICATION_NAME, call.getString("appName", getContext().getPackageName()));
        options.putString(ZebraDataWedge.EXTRA_NOTIFICATION_TYPE, notificationType);
        Intent intent = ZebraDataWedge.newIntent();
        intent.putExtra(ZebraDataWedge.CMD_UNREGISTER_FOR_NOTIFICATION, options);
        dispatchNotification(intent, call);
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
        options.putString("APP_LIST", ""); // Associations are returned only when explicitly requested.
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
        options.putString("APP_LIST", ""); // Associations are returned only when explicitly requested.
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
        String reportingGenerateOption = call.getString("reportingGenerateOption");
        if (reportingGenerateOption != null) {
            options.putString("REPORTING_GENERATE_OPTION", reportingGenerateOption);
        }
        Boolean reportingShowForManualImport = call.getBoolean("reportingShowForManualImport");
        if (reportingShowForManualImport != null) {
            options.putString("REPORTING_SHOW_FOR_MANUAL_IMPORT", Boolean.toString(reportingShowForManualImport));
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
                "No scan intentAction is registered. Call registerScanIntentAction(intentAction), softScanTrigger(intentAction), or registerForNotification({ intentAction }) first."
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
        registerManualIntentAction(intentAction);
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
                    int sentFromUid = Process.INVALID_UID;
                    String sentFromPackage = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        sentFromUid = getSentFromUid();
                        sentFromPackage = getSentFromPackage();
                    }
                    handleIntent(context, intent, sentFromUid, sentFromPackage);
                }
            };
        }
        refreshReceiver();
    }

    private boolean declaresIntentPluginConfig(JSONArray pluginConfigs) {
        if (pluginConfigs == null) {
            return false;
        }
        for (int i = 0; i < pluginConfigs.length(); i++) {
            JSONObject json = pluginConfigs.optJSONObject(i);
            if (json == null || !"INTENT".equalsIgnoreCase(json.optString("pluginName"))) {
                continue;
            }
            JSONObject paramList = json.optJSONObject("paramList");
            boolean hasIntentAction =
                paramList != null && !paramList.isNull("intent_action") && !paramList.optString("intent_action").isEmpty();
            if (hasIntentAction) {
                return true;
            }
            if (json.optBoolean("resetConfig", true)) {
                return true;
            }
        }
        return false;
    }

    private void registerManualIntentAction(String intentAction) {
        if (manualIntentActions.add(intentAction)) {
            syncRegisteredIntentActions();
        }
    }

    private void applyProfileScanIntentActions(String profileName, List<String> intentActions) {
        if (intentActions == null || intentActions.isEmpty()) {
            profileScanIntentActions.remove(profileName);
        } else {
            profileScanIntentActions.put(profileName, new LinkedHashSet<>(intentActions));
        }
        syncRegisteredIntentActions();
    }

    private void syncRegisteredIntentActions() {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        synchronized (manualIntentActions) {
            merged.addAll(manualIntentActions);
        }
        for (Set<String> actions : profileScanIntentActions.values()) {
            merged.addAll(actions);
        }
        synchronized (registeredIntentActions) {
            if (registeredIntentActions.equals(merged)) {
                return;
            }
            registeredIntentActions.clear();
            registeredIntentActions.addAll(merged);
        }
        refreshReceiver();
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

    void deliverResultIntentForTest(Context context, Intent intent, int sentFromUid, String sentFromPackage) {
        handleIntent(context, intent, sentFromUid, sentFromPackage);
    }

    private void handleIntent(Context context, Intent intent, int sentFromUid, String sentFromPackage) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (ZebraDataWedge.RESULT_ACTION.equals(action)) {
            String commandId = intent.getStringExtra(ZebraDataWedge.EXTRA_COMMAND_IDENTIFIER);
            PendingRequest request = commandId == null ? null : pendingRequests.get(commandId);
            if (request == null && commandId == null) {
                if (!allowIdentifierFreeQueryCorrelation(context, sentFromUid, sentFromPackage)) {
                    return;
                }
                for (Map.Entry<String, PendingRequest> entry : pendingRequests.entrySet()) {
                    if (entry.getValue().hasQueryPayload(intent)) {
                        if (request != null) return; // Never guess between concurrent query owners.
                        commandId = entry.getKey();
                        request = entry.getValue();
                    }
                }
            }
            if (request != null) {
                boolean failure = ZebraDataWedge.RESULT_FAILURE.equalsIgnoreCase(intent.getStringExtra(ZebraDataWedge.EXTRA_RESULT));
                if (!failure && request.resultExtras.length > 0 && !request.hasQueryPayload(intent)) return;
                if (!pendingRequests.remove(commandId, request)) return;
                diagnostics.record(commandId, failure ? "FAILURE" : "SUCCESS");
                handler.removeCallbacks(request.timeoutRunnable);
                resolvePendingRequest(request, intent);
                return;
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

    /**
     * Identifier-free typed query replies use payload correlation, not sender authentication.
     * When the platform exposes sender identity (API 34+), require the DataWedge package.
     * When identity is unavailable (API 24-33, or API 34+ without shared identity), keep
     * documented identifier-free compatibility and rely on ambiguity guards below. Residual
     * spoofing risk for that mode is tracked in https://github.com/Cap-go/capacitor-zebra-datawedge/issues/25
     */
    private boolean allowIdentifierFreeQueryCorrelation(Context context, int sentFromUid, String sentFromPackage) {
        if (isTrustedDataWedgeSender(context, sentFromUid, sentFromPackage)) {
            return true;
        }
        return isSenderIdentityUnavailable(sentFromUid, sentFromPackage);
    }

    private boolean isSenderIdentityUnavailable(int sentFromUid, String sentFromPackage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true;
        }
        return (sentFromUid == Process.INVALID_UID || sentFromUid == -1) && sentFromPackage == null;
    }

    private boolean isTrustedDataWedgeSender(Context context, int sentFromUid, String sentFromPackage) {
        if (ZebraDataWedge.DATAWEDGE_PACKAGE.equals(sentFromPackage)) {
            return true;
        }
        if (sentFromUid == Process.INVALID_UID || sentFromUid == -1) {
            return false;
        }
        String[] packages = context.getPackageManager().getPackagesForUid(sentFromUid);
        if (packages == null) {
            return false;
        }
        for (String packageName : packages) {
            if (ZebraDataWedge.DATAWEDGE_PACKAGE.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void sendCommand(PluginCall call, Intent intent, String prefix, ResponseParser parser) {
        String commandId = ZebraDataWedge.newCommandIdentifier(prefix);
        diagnostics.record(commandId, "SENT");
        intent.putExtra(ZebraDataWedge.EXTRA_SEND_RESULT, ZebraDataWedge.VALUE_LAST_RESULT);
        intent.putExtra(ZebraDataWedge.EXTRA_COMMAND_IDENTIFIER, commandId);
        Runnable timeout = () -> {
            PendingRequest removed = pendingRequests.remove(commandId);
            if (removed != null) {
                diagnostics.record(commandId, "TIMEOUT");
                removed.call.reject("Timed out waiting for DataWedge response.", "DATAWEDGE_TIMEOUT");
            }
        };
        pendingRequests.put(commandId, new PendingRequest(call, parser, timeout, queryResultExtras(intent)));
        handler.postDelayed(timeout, REQUEST_TIMEOUT_MS);
        try {
            getContext().sendBroadcast(intent);
        } catch (Exception exception) {
            pendingRequests.remove(commandId);
            handler.removeCallbacks(timeout);
            diagnostics.record(commandId, "TRANSPORT_ERROR");
            call.reject("Failed to communicate with Zebra DataWedge.", "DATAWEDGE_TRANSPORT", exception);
        }
    }

    private String[] queryResultExtras(Intent intent) {
        if (intent.hasExtra(ZebraDataWedge.CMD_GET_ACTIVE_PROFILE)) return ACTIVE_PROFILE_RESULTS;
        if (intent.hasExtra(ZebraDataWedge.CMD_GET_CONFIG)) return CONFIG_RESULTS;
        if (intent.hasExtra(ZebraDataWedge.CMD_GET_DATAWEDGE_STATUS)) return DATAWEDGE_STATUS_RESULTS;
        if (intent.hasExtra(ZebraDataWedge.CMD_GET_PROFILES_LIST)) return PROFILES_LIST_RESULTS;
        if (intent.hasExtra(ZebraDataWedge.CMD_GET_SCANNER_STATUS)) return SCANNER_STATUS_RESULTS;
        if (intent.hasExtra(ZebraDataWedge.CMD_GET_VERSION_INFO)) return VERSION_INFO_RESULTS;
        if (intent.hasExtra(ZebraDataWedge.CMD_GET_DISABLED_APP_LIST)) return DISABLED_APPS_RESULTS;
        if (intent.hasExtra(ZebraDataWedge.CMD_GET_IGNORE_DISABLED_PROFILES)) return IGNORE_DISABLED_RESULTS;
        if (intent.hasExtra(ZebraDataWedge.CMD_ENUMERATE_TRIGGERS)) return TRIGGER_RESULTS;
        if (intent.hasExtra(ZebraDataWedge.CMD_ENUMERATE_SCANNERS)) return SCANNER_ENUMERATION_RESULTS;
        return new String[0];
    }

    private void dispatchNotification(Intent intent, PluginCall call) {
        try {
            getContext().sendBroadcast(intent);
        } catch (Exception exception) {
            call.reject("DataWedge notification transport failed.", "DATAWEDGE_TRANSPORT", exception);
            return;
        }
        call.resolve();
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
        String[] profiles = firstStringArray(intent, PROFILES_LIST_RESULTS);
        JSObject ret = new JSObject();
        ret.put("profiles", ZebraDataWedge.stringArrayToJSArray(profiles));
        return ret;
    }

    private JSObject parseActiveProfile(Intent intent) {
        JSObject ret = new JSObject();
        ret.put("profileName", firstString(intent, ACTIVE_PROFILE_RESULTS));
        return ret;
    }

    private JSObject parseDataWedgeStatus(Intent intent) {
        String rawStatus = firstString(intent, DATAWEDGE_STATUS_RESULTS);
        JSObject ret = new JSObject();
        ret.put("isEnabled", rawStatus != null && ("enabled".equalsIgnoreCase(rawStatus) || Boolean.parseBoolean(rawStatus)));
        return ret;
    }

    private JSObject parseIgnoreDisabledProfiles(Intent intent) {
        String rawValue = firstString(intent, IGNORE_DISABLED_RESULTS);
        JSObject ret = new JSObject();
        ret.put("enabled", rawValue != null && ("true".equalsIgnoreCase(rawValue) || "enabled".equalsIgnoreCase(rawValue)));
        return ret;
    }

    private JSObject parseScannerStatus(Intent intent) {
        JSObject ret = new JSObject();
        ret.put("status", firstString(intent, SCANNER_STATUS_RESULTS));
        return ret;
    }

    private JSObject parseTriggers(Intent intent) {
        String[] triggers = firstStringArray(intent, TRIGGER_RESULTS);
        JSObject ret = new JSObject();
        ret.put("triggers", ZebraDataWedge.stringArrayToJSArray(triggers));
        return ret;
    }

    private JSObject parseDisabledAppList(Intent intent) {
        JSObject ret = new JSObject();
        ret.put("appList", parseAppListResult(extractBundleList(intent, DISABLED_APPS_RESULTS)));
        return ret;
    }

    private JSObject parseVersionInfo(Intent intent) {
        Bundle versionInfo = firstBundle(intent, VERSION_INFO_RESULTS);
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
        Bundle config = firstBundle(intent, CONFIG_RESULTS);
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
            Object activities = app.get("ACTIVITY_LIST");
            if (activities instanceof String[]) {
                item.put("activityList", ZebraDataWedge.stringArrayToJSArray((String[]) activities));
            } else if (activities instanceof ArrayList<?>) {
                item.put("activityList", ZebraDataWedge.listToJSArray((ArrayList<?>) activities));
            } else {
                item.put("activityList", new JSArray());
            }
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
        if (raw instanceof android.os.Parcelable[]) {
            ArrayList<Bundle> result = new ArrayList<>();
            for (android.os.Parcelable value : (android.os.Parcelable[]) raw) {
                if (value instanceof Bundle) result.add((Bundle) value);
            }
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
            if (raw instanceof android.os.Parcelable[]) {
                ArrayList<Bundle> result = new ArrayList<>();
                for (android.os.Parcelable value : (android.os.Parcelable[]) raw) {
                    if (value instanceof Bundle) result.add((Bundle) value);
                }
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
            Boolean.TRUE.equals(bundle.get("SCANNER_CONNECTION_STATE")) ||
                "true".equalsIgnoreCase(String.valueOf(bundle.get("SCANNER_CONNECTION_STATE")))
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
        final String[] resultExtras;

        PendingRequest(PluginCall call, ResponseParser parser, Runnable timeoutRunnable, String[] resultExtras) {
            this.call = call;
            this.parser = parser;
            this.resultExtras = resultExtras;
            this.timeoutRunnable = timeoutRunnable;
        }

        boolean hasQueryPayload(Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras == null) return false;
            for (String key : resultExtras) {
                Object value = extras.get(key);
                if (hasExpectedPayloadType(key, value)) return true;
            }
            return false;
        }

        /** Validate the outer wire type; empty containers remain valid query results. */
        private boolean hasExpectedPayloadType(String key, Object value) {
            if (Arrays.equals(resultExtras, VERSION_INFO_RESULTS) || Arrays.equals(resultExtras, CONFIG_RESULTS)) {
                return value instanceof Bundle;
            }
            if (
                Arrays.equals(resultExtras, PROFILES_LIST_RESULTS) ||
                Arrays.equals(resultExtras, TRIGGER_RESULTS) ||
                ZebraDataWedge.RESULT_ENUMERATED_SCANNER_LIST.equals(key)
            ) {
                return value instanceof String[];
            }
            if (Arrays.equals(resultExtras, DISABLED_APPS_RESULTS) || Arrays.equals(resultExtras, SCANNER_ENUMERATION_RESULTS)) {
                return value instanceof ArrayList<?> || value instanceof android.os.Parcelable[];
            }
            return value instanceof String;
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
