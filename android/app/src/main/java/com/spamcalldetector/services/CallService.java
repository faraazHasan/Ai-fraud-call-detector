package com.spamcalldetector.services;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;
import com.spamcalldetector.utils.CallStateHelper;
import com.spamcalldetector.MainActivity;
import com.spamcalldetector.activities.call.IncomingCallActivity;
import com.spamcalldetector.activities.call.OutgoingCallActivity;
import com.spamcalldetector.helpers.CallManager;
import com.spamcalldetector.utils.CallStateManager;
import com.spamcalldetector.activities.call.CallActivityModule;
import com.spamcalldetector.services.MissedCallNotificationService;
import com.spamcalldetector.helpers.MissedCallManager;
import com.spamcalldetector.helpers.Constants;
import com.spamcalldetector.utils.NotificationHelper;

public class CallService extends InCallService {

    private static final String TAG = "CallService";

    // Ringtone removed - handled by notification channel
    private Handler handler;
    private Runnable updateCallTimeRunnable;
    private long startTime;
    private long elapsedTime;
    private String callStatus;

    // Helper classes
    private CallStateHelper callStateHelper;
    private MissedCallManager missedCallManager;
    private NotificationHelper notificationHelper;

    // Call metadata
    private String callerNumber = "Unknown";
    private String callerName = "Unknown Caller";

    // For call state detection
    private boolean isOutgoingCall = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "📞 InCallService Started");

        // Initialize handlers and call state tracking
        CallManager.inCallService = this;
        handler = new Handler();
        CallStateManager.setCallOngoing(true);
        callStatus = "Connecting...";

        // Initialize helper classes
        try {
            callStateHelper = new CallStateHelper(this);
            missedCallManager = MissedCallManager.getInstance(this);
            notificationHelper = new NotificationHelper(this);
            
            // Start missed call notification service
            startMissedCallNotificationService();
            
            // Notify MissedCallNotificationService that CallService is active
            Intent callServiceStartedIntent = new Intent("ACTION_CALL_SERVICE_STARTED");
            sendBroadcast(callServiceStartedIntent);
            Log.d(TAG, "Broadcast sent: CallService started");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing helper classes: " + e.getMessage(), e);
        }
    }

    // Notification handling removed - only missed call notifications remain

    // Foreground service with notifications removed - service runs without notifications

    @SuppressLint("MissingPermission")
    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        
        try {
            if (call == null) {
                Log.e(TAG, "Call is null in onCallAdded");
                return;
            }

            CallManager.inCallService = this;
            CallManager.registerCall(call);

        int state = call.getState();
        Log.d(TAG, "Call Added. State: " + state);
        
        // Register call with missed call manager
        if (missedCallManager != null) {
            missedCallManager.registerActiveCall(call);
        }

        // Extract caller information
        try {
            if (call.getDetails() != null && call.getDetails().getHandle() != null) {
                callerNumber = call.getDetails().getHandle().getSchemeSpecificPart();
                if (callerNumber != null && !callerNumber.isEmpty()) {
                    // Try to get contact name from phone number
                    callerName = getContactName(callerNumber);
                    if (callerName == null || callerName.isEmpty()) {
                        callerName = callerNumber; // Fallback to number if no contact name
                    }
                } else {
                    callerNumber = "Unknown";
                    callerName = "Unknown Caller";
                }
            }
            Log.d(TAG, "Caller info - Name: " + callerName + ", Number: " + callerNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error extracting caller info: " + e.getMessage(), e);
            callerNumber = "Unknown";
            callerName = "Unknown Caller";
        }

        // Track if this is an outgoing call
        isOutgoingCall = (state == Call.STATE_DIALING || state == Call.STATE_CONNECTING);
        callStateHelper.setIsOutgoingCall(isOutgoingCall);

        // Foreground service notifications removed

        // Register for call state changes to update notification
        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int newState) {
                Log.d(TAG, "Call state changed to: " + newState);

                // Update missed call manager with state change
                if (missedCallManager != null) {
                    missedCallManager.updateCallState(call, newState);
                    
                    // Mark specific states for missed call tracking
                    if (newState == Call.STATE_RINGING) {
                        missedCallManager.markCallAsRinging(call);
                    } else if (newState == Call.STATE_ACTIVE) {
                        missedCallManager.markCallAsAnswered(call);
                    }
                }

                // Notification updates removed

                if (newState == Call.STATE_ACTIVE) {
                    // Ringtone stop is handled by notification system
                    onCallAnswered(call);
                } else if (newState == Call.STATE_DIALING || newState == Call.STATE_CONNECTING) {
                    // If call state changes to dialing/connecting again, update call waiting
                    // detection
                    if (isOutgoingCall) {
                        callStateHelper.startCallWaitingDetection(call);
                    }
                } else if (newState == Call.STATE_DISCONNECTED) {
                    // Handle call disconnection
                    if (CallManager.getActiveCalls().isEmpty()) {
                        stopCallTimer();
                        CallStateManager.setCallOngoing(false);

                        // Foreground service and notifications removed

                        // Broadcast to close any open call activities
                        Intent closeActivityIntent = new Intent("ACTION_CALL_ENDED");
                        sendBroadcast(closeActivityIntent);
                        Log.d(TAG, "Call ended broadcast sent");

                        // Stop service after a delay
                        new Handler().postDelayed(() -> {
                            stopSelf();
                        }, 3000);
                    }
                }
            }
        });

            if (state == Call.STATE_RINGING) {
                handleIncomingCall(call);
            } else if (state == Call.STATE_DIALING || state == Call.STATE_CONNECTING) {
                handleOutgoingCall(call);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCallAdded: " + e.getMessage(), e);
        }
    }

    private void handleIncomingCall(Call call) {
        Log.d(TAG, "=== INCOMING CALL DEBUG START ===");
        Log.d(TAG, "Incoming Call from: " + callerName + " (" + callerNumber + ")");
        Log.d(TAG, "Call state: " + call.getState());
        Log.d(TAG, "NotificationHelper initialized: " + (notificationHelper != null));

        // Update CallStateHelper with caller information
        callStateHelper.setCallerName(callerName);
        callStateHelper.setIsOutgoingCall(false);
        
        // Notify MissedCallNotificationService that a call is ringing
        Intent callRingingIntent = new Intent("ACTION_CALL_RINGING");
        callRingingIntent.putExtra("phoneNumber", callerNumber);
        sendBroadcast(callRingingIntent);
        Log.d(TAG, "Broadcast sent: Call ringing from " + callerNumber);

        // Smart incoming call handling based on phone state
        boolean shouldLaunchActivity = isPhoneLockedOrIdle();
        
        if (shouldLaunchActivity) {
            // Phone is locked or idle - launch full-screen incoming call activity
            Log.d(TAG, "Phone is locked/idle - launching IncomingCallActivity");
            try {
                launchIncomingCallActivityDirectly();
                Log.d(TAG, "IncomingCallActivity launched successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch IncomingCallActivity: " + e.getMessage(), e);
                // Fallback to notification if activity launch fails
                if (notificationHelper != null) {
                    Log.d(TAG, "Fallback: showing notification instead");
                    notificationHelper.showIncomingCallNotification(callerName, callerNumber);
                }
            }
        } else {
            // Phone is in use - show heads-up notification only
            Log.d(TAG, "Phone is in use - showing heads-up notification only");
            if (notificationHelper != null) {
                try {
                    notificationHelper.showIncomingCallNotification(callerName, callerNumber);
                    Log.d(TAG, "Heads-up notification shown for: " + callerName);
                } catch (Exception e) {
                    Log.e(TAG, "Error showing notification: " + e.getMessage(), e);
                    // Last resort: launch activity anyway
                    Log.d(TAG, "Notification failed, launching activity as last resort");
                    launchIncomingCallActivityDirectly();
                }
            } else {
                Log.w(TAG, "NotificationHelper is null, launching activity directly");
                launchIncomingCallActivityDirectly();
            }
        }
        
        // Update call status
        callStatus = "Incoming...";
        Log.d(TAG, "=== INCOMING CALL DEBUG END ===");
    }

    /**
     * Fallback method to launch IncomingCallActivity directly
     * Used when notification system fails or is not available
     */
    private void launchIncomingCallActivityDirectly() {
        try {
            Log.d(TAG, "=== DIRECT ACTIVITY LAUNCH DEBUG START ===");
            Log.d(TAG, "Launching IncomingCallActivity directly with caller: " + callerName);
            Log.d(TAG, "Caller number: " + callerNumber);
            Log.d(TAG, "Service context: " + this.getClass().getSimpleName());
            
            Intent incomingCallIntent = new Intent(this, IncomingCallActivity.class);
            incomingCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            
            // Pass caller information to the activity
            incomingCallIntent.putExtra("caller_name", callerName);
            incomingCallIntent.putExtra("caller_number", callerNumber);
            
            Log.d(TAG, "Intent created with flags: " + incomingCallIntent.getFlags());
            Log.d(TAG, "Intent extras - caller_name: " + incomingCallIntent.getStringExtra("caller_name"));
            Log.d(TAG, "Intent extras - caller_number: " + incomingCallIntent.getStringExtra("caller_number"));
            
            startActivity(incomingCallIntent);
            Log.d(TAG, "IncomingCallActivity launched successfully");
            Log.d(TAG, "=== DIRECT ACTIVITY LAUNCH DEBUG END ===");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch IncomingCallActivity: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void handleOutgoingCall(Call call) {
        Log.d(TAG, "Outgoing Call");

        // Update CallStateHelper with caller information
        callStateHelper.setCallerName(callerName);
        callStateHelper.setIsOutgoingCall(true);

        // Start call waiting detection for outgoing calls
        callStateHelper.startCallWaitingDetection(call);

        String number = call.getDetails().getHandle().getSchemeSpecificPart();
        if (!Constants.TWILIO_NUMBER.equals(number)) {
            Intent intent = new Intent(this, OutgoingCallActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            // Add call information to the intent
            intent.putExtra("CALL_STATE", call.getState());
            intent.putExtra("CALLER_NAME", callerName);
            intent.putExtra("CALLER_NUMBER", callerNumber);

            startActivity(intent);
        }

        callStatus = "Calling...";
    }

    public void onCallAnswered(Call call) {
        Log.d(TAG, "Call Answered");
        
        // Cancel incoming call notification since call is now answered
        if (notificationHelper != null) {
            notificationHelper.cancelIncomingCallNotification();
        }
        
        // Cancel any call waiting detection since the call was answered
        callStateHelper.cancelCallWaitingDetection();

        if (CallManager.getActiveCalls().size() == 1) {
            startCallTimer();
        }

        callStatus = formatElapsedTime(elapsedTime);

        // Broadcast to notify incoming call activity to close and transition to active
        // call view
        Intent callAnsweredIntent = new Intent(Constants.ACTION_CALL_ANSWERED);
        callAnsweredIntent.putExtra("phoneNumber", callerNumber);
        sendBroadcast(callAnsweredIntent);
        Log.d(TAG, "Call answered broadcast sent for: " + callerNumber);
    }

    // Ringtone is now handled by notification channel as per Android documentation
    // Manual ringtone playing removed to prevent conflicts

    private void startCallTimer() {
        startTime = System.currentTimeMillis();
        elapsedTime = 0;

        updateCallTimeRunnable = new Runnable() {
            @Override
            public void run() {
                elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                callStatus = formatElapsedTime(elapsedTime);
                Log.d(TAG, "Call Duration: " + callStatus);
                CallActivityModule.emitCallTiming();

                handler.postDelayed(this, 1000);
            }
        };

        handler.post(updateCallTimeRunnable);
    }

    private void stopCallTimer() {
        if (updateCallTimeRunnable != null) {
            handler.removeCallbacks(updateCallTimeRunnable);
        }
    }

    private String formatElapsedTime(long seconds) {
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public long getCallDuration() {
        return elapsedTime;
    }

    public String getCallStatus() {
        return callStatus;
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        Log.d(TAG, "Call Removed from: " + callerNumber);

        // Cancel incoming call notification if it's still showing
        if (notificationHelper != null) {
            notificationHelper.cancelIncomingCallNotification();
        }

        // Clean up call state helper resources
        callStateHelper.cleanup();

        CallManager.unregisterCall(call);

        if (CallManager.getActiveCalls().isEmpty()) {
            stopCallTimer();
            CallStateManager.setCallOngoing(false);

            // Foreground service and notifications removed

            // Broadcast to close any open call activities
            Intent closeActivityIntent = new Intent(Constants.ACTION_CALL_ENDED);
            closeActivityIntent.putExtra("phoneNumber", callerNumber);
            sendBroadcast(closeActivityIntent);
            Log.d(TAG, "Call removed broadcast sent for: " + callerNumber);

            // Stop service after a delay
            new Handler().postDelayed(() -> {
                stopSelf();
            }, 3000);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Clean up call state helper resources
        if (callStateHelper != null) {
            callStateHelper.cleanup();
        }
        
        // Notify MissedCallNotificationService that CallService is stopping
        Intent callServiceStoppedIntent = new Intent("ACTION_CALL_SERVICE_STOPPED");
        sendBroadcast(callServiceStoppedIntent);
        Log.d(TAG, "Broadcast sent: CallService stopped");

        // Check if we have active calls when service is being destroyed
        if (!CallManager.getActiveCalls().isEmpty()) {
            Log.w(TAG, "CallService being destroyed while calls are active, attempting to restart");

            // If we have active calls, restart the service to prevent termination
            Intent restartServiceIntent = new Intent(getApplicationContext(), CallService.class);
            restartServiceIntent.setPackage(getPackageName());

            // Start the service again if there are active calls
            startService(restartServiceIntent);
        } else {
            Log.d(TAG, "CallService destroyed - no active calls");
        }
    }

    // processCallAction method removed - notification buttons no longer used

    /**
     * Method for activities to notify about user interactions with the call UI
     */
    public void notifyUserInteraction(String interactionType) {
        if (callStateHelper != null) {
            callStateHelper.onHumanInteractionDetected(interactionType);
        }
    }

    /**
     * Get contact name from phone number
     */
    private String getContactName(String phoneNumber) {
        try {
            // Use ContactsHelper if available, otherwise return the phone number
            return com.spamcalldetector.helpers.ContactsHelper.getContactNameByPhoneNumber(phoneNumber, this);
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact name: " + e.getMessage(), e);
            return phoneNumber; // Fallback to phone number
        }
    }

    /**
     * Check if phone is locked or idle (should launch full-screen activity)
     * Returns true if phone is locked or idle, false if phone is in use
     */
    private boolean isPhoneLockedOrIdle() {
        try {
            // Check if screen is locked
            android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            boolean isLocked = keyguardManager != null && keyguardManager.isKeyguardLocked();
            
            // Check if screen is on
            android.os.PowerManager powerManager = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            boolean isScreenOn = powerManager != null && powerManager.isInteractive();
            
            // Check if phone app is in foreground (indicating phone is in use)
            android.app.ActivityManager activityManager = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            boolean isPhoneAppInForeground = false;
            
            if (activityManager != null) {
                java.util.List<android.app.ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
                if (runningProcesses != null) {
                    for (android.app.ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                        if (processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            // Check if it's a phone/dialer app
                            String packageName = processInfo.processName;
                            if (packageName.contains("dialer") || packageName.contains("phone") || 
                                packageName.contains("telecom") || packageName.contains("call")) {
                                isPhoneAppInForeground = true;
                                break;
                            }
                        }
                    }
                }
            }
            
            // Launch activity if:
            // 1. Phone is locked, OR
            // 2. Screen is off, OR  
            // 3. No phone app is currently in foreground
            boolean shouldLaunchActivity = isLocked || !isScreenOn || !isPhoneAppInForeground;
            
            Log.d(TAG, "Phone state check - Locked: " + isLocked + ", ScreenOn: " + isScreenOn + 
                      ", PhoneAppInForeground: " + isPhoneAppInForeground + ", ShouldLaunchActivity: " + shouldLaunchActivity);
            
            return shouldLaunchActivity;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking phone state: " + e.getMessage(), e);
            // Default to launching activity if we can't determine state
            return true;
        }
    }
    
    /**
     * Start the missed call notification service
     */
    private void startMissedCallNotificationService() {
        try {
            Intent serviceIntent = new Intent(this, MissedCallNotificationService.class);
            startService(serviceIntent);
            Log.d(TAG, "Started MissedCallNotificationService");
        } catch (Exception e) {
            Log.e(TAG, "Error starting MissedCallNotificationService: " + e.getMessage(), e);
        }
    }

    // This method ensures the service keeps running by returning START_STICKY
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CallService onStartCommand");

        // Check for user interaction notifications
        if (intent != null && intent.hasExtra("INTERACTION_TYPE")) {
            String interactionType = intent.getStringExtra("INTERACTION_TYPE");
            notifyUserInteraction(interactionType);
        }

        return START_STICKY;
    }
}
