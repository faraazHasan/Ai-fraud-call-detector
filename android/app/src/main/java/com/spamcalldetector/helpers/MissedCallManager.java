package com.spamcalldetector.helpers;

import android.content.Context;
import android.content.Intent;
import android.telecom.Call;
import android.util.Log;
import com.spamcalldetector.services.MissedCallNotificationService;
import com.spamcalldetector.utils.NotificationHelper;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager class to handle missed call detection and notification coordination
 * This class coordinates between the CallService and MissedCallNotificationService
 */
public class MissedCallManager {
    private static final String TAG = "MissedCallManager";
    private static MissedCallManager instance;
    private Context context;
    private NotificationHelper notificationHelper;
    private Map<String, CallInfo> activeCallsMap = new HashMap<>();
    
    private MissedCallManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationHelper = new NotificationHelper(this.context);
    }
    
    public static synchronized MissedCallManager getInstance(Context context) {
        if (instance == null) {
            instance = new MissedCallManager(context);
        }
        return instance;
    }
    
    /**
     * Register a call as active to track its state
     */
    public void registerActiveCall(Call call) {
        try {
            if (call == null || call.getDetails() == null || call.getDetails().getHandle() == null) {
                Log.w(TAG, "Cannot register call - null call or details");
                return;
            }
            
            String phoneNumber = call.getDetails().getHandle().getSchemeSpecificPart();
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Log.w(TAG, "Cannot register call - empty phone number");
                return;
            }
            
            CallInfo callInfo = new CallInfo(phoneNumber, System.currentTimeMillis(), call.getState());
            activeCallsMap.put(phoneNumber, callInfo);
            
            Log.d(TAG, "Registered active call: " + phoneNumber + " with state: " + call.getState());
        } catch (Exception e) {
            Log.e(TAG, "Error registering active call: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update call state for tracking
     */
    public void updateCallState(Call call, int newState) {
        try {
            if (call == null || call.getDetails() == null || call.getDetails().getHandle() == null) {
                Log.w(TAG, "Cannot update call state - null call or details");
                return;
            }
            
            String phoneNumber = call.getDetails().getHandle().getSchemeSpecificPart();
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Log.w(TAG, "Cannot update call state - empty phone number");
                return;
            }
            
            CallInfo callInfo = activeCallsMap.get(phoneNumber);
            
            if (callInfo != null) {
                callInfo.lastState = newState;
                callInfo.lastUpdateTime = System.currentTimeMillis();
                
                Log.d(TAG, "Updated call state for " + phoneNumber + " to: " + newState);
                
                // If call is disconnected and was never answered, it might be missed
                if (newState == Call.STATE_DISCONNECTED) {
                    handleCallDisconnected(phoneNumber, callInfo);
                }
            } else {
                Log.w(TAG, "No call info found for phone number: " + phoneNumber);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating call state: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handle call disconnection and determine if it was missed
     */
    private void handleCallDisconnected(String phoneNumber, CallInfo callInfo) {
        try {
            // Check if the call was ever answered (went to ACTIVE state)
            boolean wasAnswered = callInfo.wasAnswered;
            
            // If call was ringing and then disconnected without being answered, it's missed
            if (!wasAnswered && callInfo.wasRinging) {
                Log.d(TAG, "Detected missed call from: " + phoneNumber);
                
                // Cancel any system notifications immediately
                notificationHelper.cancelSystemMissedCallNotifications();
                
                // Start the missed call notification service if not already running
                startMissedCallNotificationService();
                
                // Send broadcast to notify about missed call
                Intent missedCallIntent = new Intent("ACTION_MISSED_CALL_DETECTED");
                missedCallIntent.putExtra("phoneNumber", phoneNumber);
                missedCallIntent.putExtra("timestamp", callInfo.startTime);
                context.sendBroadcast(missedCallIntent);
            }
            
            // Remove from active calls map
            activeCallsMap.remove(phoneNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling call disconnection: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mark a call as answered
     */
    public void markCallAsAnswered(Call call) {
        try {
            if (call == null || call.getDetails() == null || call.getDetails().getHandle() == null) {
                Log.w(TAG, "Cannot mark call as answered - null call or details");
                return;
            }
            
            String phoneNumber = call.getDetails().getHandle().getSchemeSpecificPart();
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Log.w(TAG, "Cannot mark call as answered - empty phone number");
                return;
            }
            
            CallInfo callInfo = activeCallsMap.get(phoneNumber);
            
            if (callInfo != null) {
                callInfo.wasAnswered = true;
                Log.d(TAG, "Marked call as answered: " + phoneNumber);
            } else {
                Log.w(TAG, "No call info found to mark as answered: " + phoneNumber);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error marking call as answered: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mark a call as ringing
     */
    public void markCallAsRinging(Call call) {
        try {
            if (call == null || call.getDetails() == null || call.getDetails().getHandle() == null) {
                Log.w(TAG, "Cannot mark call as ringing - null call or details");
                return;
            }
            
            String phoneNumber = call.getDetails().getHandle().getSchemeSpecificPart();
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Log.w(TAG, "Cannot mark call as ringing - empty phone number");
                return;
            }
            
            CallInfo callInfo = activeCallsMap.get(phoneNumber);
            
            if (callInfo != null) {
                callInfo.wasRinging = true;
                Log.d(TAG, "Marked call as ringing: " + phoneNumber);
            } else {
                Log.w(TAG, "No call info found to mark as ringing: " + phoneNumber);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error marking call as ringing: " + e.getMessage(), e);
        }
    }
    
    /**
     * Start the missed call notification service
     */
    private void startMissedCallNotificationService() {
        try {
            Intent serviceIntent = new Intent(context, MissedCallNotificationService.class);
            context.startService(serviceIntent);
            Log.d(TAG, "Started MissedCallNotificationService");
        } catch (Exception e) {
            Log.e(TAG, "Error starting MissedCallNotificationService: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancel system missed call notifications
     */
    public void cancelSystemNotifications() {
        if (notificationHelper != null) {
            notificationHelper.cancelSystemMissedCallNotifications();
        }
    }
    
    /**
     * Inner class to track call information
     */
    private static class CallInfo {
        String phoneNumber;
        long startTime;
        long lastUpdateTime;
        int lastState;
        boolean wasAnswered = false;
        boolean wasRinging = false;
        
        CallInfo(String phoneNumber, long startTime, int initialState) {
            this.phoneNumber = phoneNumber;
            this.startTime = startTime;
            this.lastUpdateTime = startTime;
            this.lastState = initialState;
            
            // Check if it started as ringing
            if (initialState == Call.STATE_RINGING) {
                this.wasRinging = true;
            }
            
            // Check if it started as active (unlikely but possible)
            if (initialState == Call.STATE_ACTIVE) {
                this.wasAnswered = true;
            }
        }
    }
}
