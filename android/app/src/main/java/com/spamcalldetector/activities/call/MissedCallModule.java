package com.spamcalldetector.activities.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.spamcalldetector.helpers.Constants;
import com.spamcalldetector.helpers.MissedCallManager;
import com.spamcalldetector.services.MissedCallNotificationService;

/**
 * React Native module for handling missed call notifications
 */
public class MissedCallModule extends ReactContextBaseJavaModule {
    private static final String TAG = "MissedCallModule";
    private ReactApplicationContext reactContext;
    private BroadcastReceiver missedCallReceiver;
    private MissedCallManager missedCallManager;

    public MissedCallModule(ReactApplicationContext context) {
        super(context);
        this.reactContext = context;
        this.missedCallManager = MissedCallManager.getInstance(context);
        
        // Register broadcast receiver for missed calls
        registerMissedCallReceiver();
    }

    @Override
    public String getName() {
        return "MissedCallModule";
    }

    /**
     * Start the missed call notification service
     */
    @ReactMethod
    public void startMissedCallService(Promise promise) {
        try {
            Intent serviceIntent = new Intent(reactContext, MissedCallNotificationService.class);
            reactContext.startService(serviceIntent);
            Log.d(TAG, "Started MissedCallNotificationService from React Native");
            promise.resolve("Missed call service started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting missed call service: " + e.getMessage(), e);
            promise.reject("ERROR", "Failed to start missed call service: " + e.getMessage());
        }
    }

    /**
     * Stop the missed call notification service
     */
    @ReactMethod
    public void stopMissedCallService(Promise promise) {
        try {
            Intent serviceIntent = new Intent(reactContext, MissedCallNotificationService.class);
            boolean stopped = reactContext.stopService(serviceIntent);
            
            Log.d(TAG, "Missed call service stop result: " + stopped);
            promise.resolve(stopped);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping missed call service: " + e.getMessage(), e);
            promise.reject("ERROR", "Failed to stop missed call service: " + e.getMessage());
        }
    }
    
    /**
     * Reset missed call count and clear notification
     */
    @ReactMethod
    public void resetMissedCallCount(Promise promise) {
        try {
            Log.d(TAG, "resetMissedCallCount called from React Native");
            
            // Send broadcast to service to reset count
            Intent resetIntent = new Intent("ACTION_RESET_MISSED_CALL_COUNT");
            // Make it explicit that this is for our service
            resetIntent.setPackage(reactContext.getPackageName());
            
            // Send the broadcast
            reactContext.sendBroadcast(resetIntent);
            
            Log.d(TAG, "Reset missed call count broadcast sent with package: " + reactContext.getPackageName());
            
            // The broadcast should handle the reset through the service
            
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error resetting missed call count: " + e.getMessage(), e);
            promise.reject("ERROR", "Failed to reset missed call count: " + e.getMessage());
        }
    }
    
    /**
     * Navigate to Recent Calls screen
     */
    @ReactMethod
    public void navigateToRecentCalls(Promise promise) {
        try {
            Log.d(TAG, "Sending navigation event to React Native");
            
            // Send event to React Native
            WritableMap params = Arguments.createMap();
            params.putString("screen", "RecentCalls");
            sendEvent("onNavigateToScreen", params);
            
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to recent calls: " + e.getMessage(), e);
            promise.reject("ERROR", "Failed to navigate to recent calls: " + e.getMessage());
        }
    }

    /**
     * Cancel system missed call notifications
     */
    @ReactMethod
    public void cancelSystemNotifications(Promise promise) {
        try {
            if (missedCallManager != null) {
                missedCallManager.cancelSystemNotifications();
                promise.resolve("System notifications cancelled");
            } else {
                promise.reject("ERROR", "MissedCallManager not initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling system notifications: " + e.getMessage(), e);
            promise.reject("ERROR", "Failed to cancel system notifications: " + e.getMessage());
        }
    }

    /**
     * Test missed call functionality
     */
    @ReactMethod
    public void testMissedCall(String phoneNumber, String contactName, Promise promise) {
        try {
            // Create test missed call broadcast
            Intent missedCallIntent = new Intent("ACTION_MISSED_CALL");
            missedCallIntent.putExtra("phoneNumber", phoneNumber);
            missedCallIntent.putExtra("contactName", contactName);
            missedCallIntent.putExtra("timestamp", System.currentTimeMillis());
            
            reactContext.sendBroadcast(missedCallIntent);
            
            Log.d(TAG, "Test missed call broadcast sent for: " + phoneNumber);
            promise.resolve("Test missed call sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error sending test missed call: " + e.getMessage(), e);
            promise.reject("ERROR", "Failed to send test missed call: " + e.getMessage());
        }
    }

    /**
     * Register broadcast receiver for missed calls
     */
    private void registerMissedCallReceiver() {
        try {
            if (missedCallReceiver != null) {
                // Unregister existing receiver first
                reactContext.unregisterReceiver(missedCallReceiver);
            }

            missedCallReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.d(TAG, "Received broadcast: " + action);

                    if (Constants.ACTION_MISSED_CALL.equals(action) || Constants.ACTION_MISSED_CALL_DETECTED.equals(action)) {
                        String phoneNumber = intent.getStringExtra("phoneNumber");
                        String contactName = intent.getStringExtra("contactName");
                        long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());

                        Log.d(TAG, "Processing missed call from: " + phoneNumber);

                        // Send event to React Native
                        WritableMap params = Arguments.createMap();
                        params.putString("phoneNumber", phoneNumber);
                        params.putString("contactName", contactName != null ? contactName : "");
                        params.putDouble("timestamp", timestamp);
                        params.putString("type", "missed");

                        sendEvent("onMissedCall", params);
                    }
                }
            };

            // Register receiver for missed call broadcasts
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_MISSED_CALL);
            filter.addAction(Constants.ACTION_MISSED_CALL_DETECTED);
            
            // Use RECEIVER_NOT_EXPORTED for Android 14+ compatibility
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                reactContext.registerReceiver(missedCallReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                reactContext.registerReceiver(missedCallReceiver, filter);
            }
            Log.d(TAG, "Registered missed call broadcast receiver");

        } catch (Exception e) {
            Log.e(TAG, "Error registering missed call receiver: " + e.getMessage(), e);
        }
    }

    /**
     * Send event to React Native
     */
    private void sendEvent(String eventName, WritableMap params) {
        try {
            if (reactContext != null && reactContext.hasActiveCatalystInstance()) {
                reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
                Log.d(TAG, "Sent event to React Native: " + eventName);
            } else {
                Log.w(TAG, "Cannot send event - React context not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending event to React Native: " + e.getMessage(), e);
        }
    }

    /**
     * Clean up resources when module is destroyed
     */
    @Override
    public void invalidate() {
        super.invalidate();
        
        try {
            if (missedCallReceiver != null && reactContext != null) {
                reactContext.unregisterReceiver(missedCallReceiver);
                missedCallReceiver = null;
                Log.d(TAG, "Unregistered missed call receiver");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver: " + e.getMessage(), e);
        }
    }

    // Required for NativeEventEmitter
    @ReactMethod
    public void addListener(String eventName) {
        // No-op
    }

    @ReactMethod
    public void removeListeners(double count) {
        // No-op
    }
}
