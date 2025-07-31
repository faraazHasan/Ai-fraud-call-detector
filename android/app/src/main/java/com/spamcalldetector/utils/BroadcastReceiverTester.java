package com.spamcalldetector.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.spamcalldetector.helpers.Constants;

/**
 * Utility class to test all broadcast receivers in the application
 */
public class BroadcastReceiverTester {
    private static final String TAG = "BroadcastReceiverTester";

    /**
     * Test all broadcast receivers by sending test broadcasts
     */
    public static void testAllBroadcastReceivers(Context context) {
        Log.d(TAG, "Starting comprehensive broadcast receiver test...");

        // Test 1: Missed Call Notification Service Reset Receiver
        testResetMissedCallCountReceiver(context);

        // Test 2: Call Activity Module Receivers
        testCallActivityModuleReceivers(context);

        // Test 3: Call State Receivers (Incoming/Outgoing Activities)
        testCallStateReceivers(context);

        // Test 4: Missed Call Module Receiver
        testMissedCallModuleReceiver(context);

        Log.d(TAG, "Completed comprehensive broadcast receiver test");
    }

    /**
     * Test the reset missed call count receiver in MissedCallNotificationService
     */
    private static void testResetMissedCallCountReceiver(Context context) {
        Log.d(TAG, "Testing Reset Missed Call Count Receiver...");
        
        Intent resetIntent = new Intent(Constants.ACTION_RESET_MISSED_CALL_COUNT);
        resetIntent.putExtra("test", true);
        resetIntent.putExtra("timestamp", System.currentTimeMillis());
        
        try {
            context.sendBroadcast(resetIntent);
            Log.d(TAG, "✓ Reset missed call count broadcast sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to send reset missed call count broadcast: " + e.getMessage());
        }
    }

    /**
     * Test the receivers in CallActivityModule
     */
    private static void testCallActivityModuleReceivers(Context context) {
        Log.d(TAG, "Testing Call Activity Module Receivers...");

        // Test ACTION_MISSED_CALL receiver
        Intent missedCallIntent = new Intent(Constants.ACTION_MISSED_CALL);
        missedCallIntent.putExtra("phoneNumber", "+1234567890");
        missedCallIntent.putExtra("contactName", "Test Contact");
        missedCallIntent.putExtra("timestamp", System.currentTimeMillis());
        missedCallIntent.putExtra("test", true);

        try {
            context.sendBroadcast(missedCallIntent);
            Log.d(TAG, "✓ ACTION_MISSED_CALL broadcast sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to send ACTION_MISSED_CALL broadcast: " + e.getMessage());
        }

        // Test NOTIFY_JS_MISSED_CALL receiver
        Intent jsNotifyIntent = new Intent(Constants.NOTIFY_JS_MISSED_CALL);
        jsNotifyIntent.putExtra("phoneNumber", "+1234567890");
        jsNotifyIntent.putExtra("contactName", "Test JS Contact");
        jsNotifyIntent.putExtra("timestamp", System.currentTimeMillis());
        jsNotifyIntent.putExtra("test", true);

        try {
            context.sendBroadcast(jsNotifyIntent);
            Log.d(TAG, "✓ NOTIFY_JS_MISSED_CALL broadcast sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to send NOTIFY_JS_MISSED_CALL broadcast: " + e.getMessage());
        }
    }

    /**
     * Test call state receivers in Incoming/Outgoing call activities
     */
    private static void testCallStateReceivers(Context context) {
        Log.d(TAG, "Testing Call State Receivers...");

        // Test ACTION_CALL_ENDED
        Intent callEndedIntent = new Intent(Constants.ACTION_CALL_ENDED);
        callEndedIntent.putExtra("test", true);
        callEndedIntent.putExtra("timestamp", System.currentTimeMillis());

        try {
            context.sendBroadcast(callEndedIntent);
            Log.d(TAG, "✓ ACTION_CALL_ENDED broadcast sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to send ACTION_CALL_ENDED broadcast: " + e.getMessage());
        }

        // Test ACTION_CALL_ANSWERED
        Intent callAnsweredIntent = new Intent(Constants.ACTION_CALL_ANSWERED);
        callAnsweredIntent.putExtra("test", true);
        callAnsweredIntent.putExtra("timestamp", System.currentTimeMillis());

        try {
            context.sendBroadcast(callAnsweredIntent);
            Log.d(TAG, "✓ ACTION_CALL_ANSWERED broadcast sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to send ACTION_CALL_ANSWERED broadcast: " + e.getMessage());
        }

        // Test ACTION_CALL_WAITING_DETECTED (for OutgoingCallActivity)
        Intent callWaitingIntent = new Intent(Constants.ACTION_CALL_WAITING_DETECTED);
        callWaitingIntent.putExtra("test", true);
        callWaitingIntent.putExtra("timestamp", System.currentTimeMillis());

        try {
            context.sendBroadcast(callWaitingIntent);
            Log.d(TAG, "✓ ACTION_CALL_WAITING_DETECTED broadcast sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to send ACTION_CALL_WAITING_DETECTED broadcast: " + e.getMessage());
        }

        // Test ACTION_VOICEMAIL_DETECTED (for OutgoingCallActivity)
        Intent voicemailIntent = new Intent(Constants.ACTION_VOICEMAIL_DETECTED);
        voicemailIntent.putExtra("test", true);
        voicemailIntent.putExtra("timestamp", System.currentTimeMillis());

        try {
            context.sendBroadcast(voicemailIntent);
            Log.d(TAG, "✓ ACTION_VOICEMAIL_DETECTED broadcast sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to send ACTION_VOICEMAIL_DETECTED broadcast: " + e.getMessage());
        }
    }

    /**
     * Test the receiver in MissedCallModule
     */
    private static void testMissedCallModuleReceiver(Context context) {
        Log.d(TAG, "Testing Missed Call Module Receiver...");

        // Test ACTION_MISSED_CALL_DETECTED
        Intent missedCallDetectedIntent = new Intent(Constants.ACTION_MISSED_CALL_DETECTED);
        missedCallDetectedIntent.putExtra("phoneNumber", "+1987654321");
        missedCallDetectedIntent.putExtra("contactName", "Module Test Contact");
        missedCallDetectedIntent.putExtra("timestamp", System.currentTimeMillis());
        missedCallDetectedIntent.putExtra("test", true);

        try {
            context.sendBroadcast(missedCallDetectedIntent);
            Log.d(TAG, "✓ ACTION_MISSED_CALL_DETECTED broadcast sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to send ACTION_MISSED_CALL_DETECTED broadcast: " + e.getMessage());
        }
    }

    /**
     * Test individual broadcast receiver with custom action and data
     */
    public static void testCustomBroadcast(Context context, String action, String phoneNumber, String contactName) {
        Log.d(TAG, "Testing custom broadcast with action: " + action);

        Intent customIntent = new Intent(action);
        if (phoneNumber != null) {
            customIntent.putExtra("phoneNumber", phoneNumber);
        }
        if (contactName != null) {
            customIntent.putExtra("contactName", contactName);
        }
        customIntent.putExtra("timestamp", System.currentTimeMillis());
        customIntent.putExtra("test", true);

        try {
            context.sendBroadcast(customIntent);
            Log.d(TAG, "✓ Custom broadcast (" + action + ") sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to send custom broadcast (" + action + "): " + e.getMessage());
        }
    }

    /**
     * Check if broadcast receivers are properly registered by examining logs
     */
    public static void checkReceiverRegistrationStatus() {
        Log.d(TAG, "=== BROADCAST RECEIVER REGISTRATION STATUS ===");
        Log.d(TAG, "Check the following in your logs:");
        Log.d(TAG, "1. MissedCallNotificationService: 'Reset receiver setup completed successfully'");
        Log.d(TAG, "2. CallActivityModule: 'Successfully registered both receivers'");
        Log.d(TAG, "3. IncomingCallActivity: 'Successfully registered call state broadcast receiver'");
        Log.d(TAG, "4. OutgoingCallActivity: 'Successfully registered call state broadcast receiver'");
        Log.d(TAG, "5. MissedCallModule: 'Registered missed call broadcast receiver'");
        Log.d(TAG, "=== END REGISTRATION STATUS ===");
    }

    /**
     * Comprehensive diagnostic method to check all receivers
     */
    public static void runDiagnostics(Context context) {
        Log.d(TAG, "=== STARTING BROADCAST RECEIVER DIAGNOSTICS ===");
        
        // Check registration status
        checkReceiverRegistrationStatus();
        
        // Wait a moment for any pending registrations
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test all receivers
        testAllBroadcastReceivers(context);
        
        Log.d(TAG, "=== BROADCAST RECEIVER DIAGNOSTICS COMPLETED ===");
        Log.d(TAG, "Check logs above for ✓ (success) or ✗ (failure) indicators");
        Log.d(TAG, "Also check for receiver onReceive() method calls in logs");
    }
}
