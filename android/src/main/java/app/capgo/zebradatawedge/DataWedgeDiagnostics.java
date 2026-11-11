package app.capgo.zebradatawedge;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.util.ArrayDeque;

/** Opt-in metadata only: no barcode values, intent bodies, or arbitrary exception text. */
final class DataWedgeDiagnostics {

    private final ArrayDeque<JSObject> events = new ArrayDeque<>();
    private boolean enabled;
    private int capacity = 100;
    private int dropped;
    private long startedAt;

    synchronized void configure(boolean enabled, Integer capacity, boolean reset) {
        this.enabled = enabled;
        if (capacity != null) this.capacity = capacity;
        if (reset) {
            events.clear();
            dropped = 0;
            startedAt = SystemClock.elapsedRealtime();
        }
        while (events.size() > this.capacity) {
            events.removeFirst();
            dropped++;
        }
        if (enabled && startedAt == 0) startedAt = SystemClock.elapsedRealtime();
    }

    synchronized void record(String identifier, String outcome) {
        if (!enabled) return;
        if (events.size() == capacity) {
            events.removeFirst();
            dropped++;
        }
        JSObject event = new JSObject();
        event.put("commandIdentifier", identifier);
        event.put("outcome", outcome);
        event.put("elapsedMs", SystemClock.elapsedRealtime() - startedAt);
        events.addLast(event);
    }

    synchronized JSObject snapshot(Context context) {
        JSArray commands = new JSArray();
        for (JSObject event : events) {
            // Copy every value so callers cannot mutate the ring through a previous snapshot.
            JSObject copy = new JSObject();
            copy.put("commandIdentifier", event.getString("commandIdentifier"));
            copy.put("outcome", event.getString("outcome"));
            copy.put("elapsedMs", event.optLong("elapsedMs"));
            commands.put(copy);
        }
        JSObject result = new JSObject();
        result.put("diagnosticsVersion", 1);
        result.put("enabled", enabled);
        result.put("maxEntries", capacity);
        result.put("droppedEntries", dropped);
        result.put("manufacturer", Build.MANUFACTURER);
        result.put("model", Build.MODEL);
        result.put("androidVersion", Build.VERSION.RELEASE);
        result.put("dataWedgePackage", describePackage(context));
        result.put("commands", commands);
        return result;
    }

    private JSObject describePackage(Context context) {
        JSObject result = new JSObject();
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("com.symbol.datawedge", 0);
            result.put("status", "installed");
            if (info.versionName != null) result.put("version", info.versionName);
        } catch (PackageManager.NameNotFoundException exception) {
            result.put("status", "notFound");
        } catch (Exception exception) {
            result.put("status", "unavailable");
        }
        return result;
    }
}
