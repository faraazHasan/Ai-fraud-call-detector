package com.spamcalldetector.activities.call;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.spamcalldetector.helpers.CallManager;
import com.spamcalldetector.helpers.Constants;

import java.lang.ref.WeakReference;

public class IncomingCallActivity extends ReactActivity {
    private static final String TAG = "IncomingCallActivity";
    private static WeakReference<Activity> mCurrentActivity = new WeakReference<>(null);
    private BroadcastReceiver callStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentActivity = new WeakReference<>(this);

        getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Register broadcast receiver for call state changes
        registerCallStateBroadcastReceiver();

        // Check if this activity was started from notification
        handleNotificationAction(getIntent());
    }

    private void registerCallStateBroadcastReceiver() {
        try {
            // Unregister any existing receiver first to prevent duplicates
            if (callStateReceiver != null) {
                try {
                    unregisterReceiver(callStateReceiver);
                } catch (Exception e) {
                    // Ignore if not registered
                    Log.d(TAG, "Receiver was not registered: " + e.getMessage());
                }
            }

            callStateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        if (intent == null) {
                            Log.e(TAG, "Received null intent in broadcast receiver");
                            return;
                        }

                        String action = intent.getAction();
                        if (action == null) {
                            Log.e(TAG, "Received intent with null action");
                            return;
                        }

                        Log.d(TAG, "Received broadcast action: " + action);

                        if (Constants.ACTION_CALL_ENDED.equals(action)) {
                            Log.d(TAG, "Call ended, finishing activity");
                            if (!isFinishing()) {
                                finish();
                            }
                        } else if (Constants.ACTION_CALL_ANSWERED.equals(action)) {
                            Log.d(TAG, "Call answered, finishing incoming call activity");
                            if (!isFinishing()) {
                                finish();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling broadcast: " + e.getMessage());
                    }
                }
            };

            // Register receiver for relevant actions
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_CALL_ENDED);
            filter.addAction(Constants.ACTION_CALL_ANSWERED);
            registerReceiver(callStateReceiver, filter);
            Log.d(TAG, "Successfully registered call state broadcast receiver");
        } catch (Exception e) {
            Log.e(TAG, "Error registering broadcast receiver: " + e.getMessage());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationAction(intent);
    }

    private void handleNotificationAction(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "Handling notification action: " + action);

            // Cancel the incoming call notification immediately when any action is taken
            try {
                android.app.NotificationManager notificationManager = 
                    (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(Constants.INCOMING_CALL_NOTIFICATION_ID);
                    Log.d(TAG, "Cancelled incoming call notification with ID: " + Constants.INCOMING_CALL_NOTIFICATION_ID);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling incoming call notification: " + e.getMessage(), e);
            }

            if ("ANSWER_CALL".equals(action)) {
                // Handle answer call action
                Call call = CallManager.getLatestActiveOrRingingCall();
                if (call != null) {
                    Log.d(TAG, "Answering call from notification action");
                    CallManager.answerCall(call);
                } else {
                    Log.w(TAG, "No active or ringing call found to answer");
                }
            } else if ("DECLINE_CALL".equals(action)) {
                // Handle decline call action
                Call call = CallManager.getLatestActiveOrRingingCall();
                if (call != null) {
                    Log.d(TAG, "Declining call from notification action");
                    // Get call details before hanging up
                    String phoneNumber = null;
                    if (call.getDetails() != null && call.getDetails().getHandle() != null) {
                        phoneNumber = call.getDetails().getHandle().getSchemeSpecificPart();
                    }

                    // Now hang up the call
                    CallManager.hangUpCall(call);

                    // Manually broadcast a missed call event since this was user-rejected
                    if (phoneNumber != null) {
                        Log.d(TAG, "User rejected call from: " + phoneNumber + ", marking as missed call");
                        String contactName = null;
                        try {
                            contactName = com.spamcalldetector.helpers.ContactsHelper.getContactNameFromNumber(
                                    getApplicationContext(),
                                    phoneNumber);
                            Log.d(TAG, "Contact name resolved: " + (contactName != null ? contactName : "Unknown"));
                        } catch (Exception e) {
                            Log.e(TAG, "Error getting contact name: " + e.getMessage());
                        }

                        Intent missedCallIntent = new Intent("ACTION_MISSED_CALL");
                        missedCallIntent.putExtra("phoneNumber", phoneNumber);
                        missedCallIntent.putExtra("contactName", contactName);
                        Log.d(TAG, "Broadcasting ACTION_MISSED_CALL intent from IncomingCallActivity");
                        sendBroadcast(missedCallIntent);
                    }
                } else {
                    Log.w(TAG, "No active or ringing call found to decline");
                }
                finish();
            }
        }
    }

    public static Activity getActivity() {
        return mCurrentActivity.get();
    }

    @Override
    protected String getMainComponentName() {
        return "IncomingCall"; // This must match AppRegistry name in JS
    }

    @Override
    protected ReactActivityDelegate createReactActivityDelegate() {
        return new DefaultReactActivityDelegate(
                this,
                getMainComponentName(),
                DefaultNewArchitectureEntryPoint.getFabricEnabled());
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "IncomingCallActivity onDestroy");

        // Unregister broadcast receiver
        if (callStateReceiver != null) {
            try {
                unregisterReceiver(callStateReceiver);
                callStateReceiver = null;
                Log.d(TAG, "Successfully unregistered broadcast receiver");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }
        }

        mCurrentActivity.clear();

        super.onDestroy();
    }

    /**
     * Helper method to send events to React Native
     */
    private void sendEventToJS(String eventName, WritableMap params) {
        try {
            if (getReactInstanceManager() != null &&
                    getReactInstanceManager().getCurrentReactContext() != null) {

                if (params == null) {
                    params = Arguments.createMap();
                }

                getReactInstanceManager()
                        .getCurrentReactContext()
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit(eventName, params);

                Log.d(TAG, "Event sent to JS: " + eventName);
            } else {
                Log.w(TAG, "Could not send event to JS - React context not ready");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending event to JS: " + e.getMessage());
        }
    }
}
