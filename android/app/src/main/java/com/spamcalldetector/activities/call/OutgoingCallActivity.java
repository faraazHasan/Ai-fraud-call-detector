package com.spamcalldetector.activities.call;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.WindowManager;
import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.spamcalldetector.utils.CallStateManager;
import com.spamcalldetector.utils.ActivityStates;
import com.spamcalldetector.MainActivity;
import com.spamcalldetector.helpers.CallManager;
import com.spamcalldetector.helpers.Constants;

import java.lang.ref.WeakReference;

public class OutgoingCallActivity extends ReactActivity {
    private static final String TAG = "OutgoingCallActivity";
    private static WeakReference<Activity> mCurrentActivity = new WeakReference<>(null);
    private BroadcastReceiver callStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentActivity = new WeakReference<>(this);

        // Prevent screen from sleeping and show over lock screen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(null, null, 0));
        }

        setFinishOnTouchOutside(false); // Just in case user tries to dismiss like a dialog

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
                        } else if (Constants.ACTION_CALL_WAITING_DETECTED.equals(action)) {
                            Log.d(TAG, "Call waiting detected");
                            // Send event to React Native
                            sendEventToJS("callWaitingDetected", null);
                        } else if (Constants.ACTION_VOICEMAIL_DETECTED.equals(action)) {
                            Log.d(TAG, "Voicemail detected");
                            // Send event to React Native
                            sendEventToJS("voicemailDetected", null);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling broadcast: " + e.getMessage());
                    }
                }
            };

            // Register receiver for multiple actions
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_CALL_ENDED);
            filter.addAction(Constants.ACTION_CALL_WAITING_DETECTED);
            filter.addAction(Constants.ACTION_VOICEMAIL_DETECTED);
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

            if ("END_CALL".equals(action)) {
                // Handle end call action from notification
                Call call = CallManager.getLatestActiveOrRingingCall();
                if (call != null) {
                    CallManager.hangUpCall(call);
                }
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Disable back button to prevent manual close
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "OutgoingCallActivity onDestroy");

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

        try {
            // Relaunch the screen if the call is still ongoing
            if (CallStateManager.isCallOngoing()) {
                Log.d(TAG, "Call is still ongoing, relaunching activity");
                Intent intent = new Intent(this, OutgoingCallActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error relaunching activity: " + e.getMessage());
        }

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

    public static Activity getActivity() {
        return mCurrentActivity.get();
    }

    @Override
    protected String getMainComponentName() {
        return "OutgoingCall";
    }

    @Override
    protected ReactActivityDelegate createReactActivityDelegate() {
        return new DefaultReactActivityDelegate(this, getMainComponentName(),
                DefaultNewArchitectureEntryPoint.getFabricEnabled());
    }
}
