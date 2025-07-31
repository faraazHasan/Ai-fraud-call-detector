package com.spamcalldetector.activities.call;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.spamcalldetector.helpers.CallLogHelper;

import java.util.List;
import java.util.Map;

public class CallHistoryModule extends ReactContextBaseJavaModule {
    private static final String TAG = "CallHistoryModule";
    private final ReactApplicationContext reactContext;

    public CallHistoryModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        Log.d(TAG, "CallHistoryModule initialized");
    }

    @NonNull
    @Override
    public String getName() {
        return "CallHistoryModule";
    }

    @ReactMethod
    public void getRecentCalls(int limit, Promise promise) {
        try {
            Context context = getReactApplicationContext();
            Log.d(TAG, "Getting recent calls with limit: " + limit);

            List<Map<String, Object>> callLogs = CallLogHelper.getRecentCalls(context, limit);
            Log.d(TAG, "Retrieved " + callLogs.size() + " call logs");

            // Convert to WritableArray for React Native
            WritableArray result = Arguments.createArray();
            for (Map<String, Object> callData : callLogs) {
                WritableMap call = Arguments.createMap();
                try {
                    call.putString("id", (String) callData.get("id"));
                    call.putString("phoneNumber", (String) callData.get("phoneNumber"));
                    call.putString("contactName", (String) callData.get("contactName"));
                    call.putDouble("timestamp", (Long) callData.get("timestamp"));
                    call.putInt("duration", (Integer) callData.get("duration"));
                    call.putString("type", (String) callData.get("type"));
                    call.putBoolean("isNew", (Boolean) callData.get("isNew"));

                    result.pushMap(call);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing call data: " + e.getMessage());
                    // Continue with next record
                }
            }

            Log.d(TAG, "Sending " + result.size() + " call records to React Native");
            promise.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error getting recent calls: " + e.getMessage());
            promise.reject("GET_CALLS_ERROR", "Error retrieving call history: " + e.getMessage());
        }
    }

    @ReactMethod
    public void markCallAsRead(String callId, Promise promise) {
        try {
            Context context = getReactApplicationContext();
            boolean success = CallLogHelper.markCallAsRead(context, callId);
            promise.resolve(success);
        } catch (Exception e) {
            Log.e(TAG, "Error marking call as read: " + e.getMessage());
            promise.reject("MARK_CALL_ERROR", "Error marking call as read: " + e.getMessage());
        }
    }

    @ReactMethod
    public void syncCallHistory(Promise promise) {
        try {
            Context context = getReactApplicationContext();

            // Get the 100 most recent calls
            List<Map<String, Object>> callLogs = CallLogHelper.getRecentCalls(context, 100);

            // Convert to WritableArray
            WritableArray result = Arguments.createArray();
            for (Map<String, Object> callData : callLogs) {
                WritableMap call = Arguments.createMap();
                call.putString("id", (String) callData.get("id"));
                call.putString("phoneNumber", (String) callData.get("phoneNumber"));
                call.putString("contactName", (String) callData.get("contactName"));
                call.putDouble("timestamp", (Long) callData.get("timestamp"));
                call.putInt("duration", (Integer) callData.get("duration"));
                call.putString("type", (String) callData.get("type"));
                call.putBoolean("isNew", (Boolean) callData.get("isNew"));

                result.pushMap(call);
            }

            promise.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error syncing call history: " + e.getMessage());
            promise.reject("SYNC_HISTORY_ERROR", "Error syncing call history: " + e.getMessage());
        }
    }
}
