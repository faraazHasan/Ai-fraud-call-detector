package com.spamcalldetector.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.spamcalldetector.R;
import com.spamcalldetector.activities.call.IncomingCallActivity;
import com.spamcalldetector.helpers.Constants;

/**
 * Helper class to handle incoming call notifications and system notification cancellation
 * Implements Android's official incoming call notification requirements
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createIncomingCallNotificationChannel();
    }
    
    /**
     * Create the incoming call notification channel as per Android documentation
     * This channel will handle ringtone and audio attributes for incoming calls
     */
    private void createIncomingCallNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    Constants.INCOMING_CALL_CHANNEL_ID,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_MAX
                );
                
                // Set channel description
                channel.setDescription("Notifications for incoming phone calls");
                
                // Use the default system ringtone for incoming call notifications
                Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                channel.setSound(ringtoneUri, new AudioAttributes.Builder()
                    // Setting the AudioAttributes is important as it identifies the purpose of your notification sound
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
                
                // Enable vibration and lights
                channel.enableVibration(true);
                channel.enableLights(true);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                
                // Create the channel
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Created incoming call notification channel");
            } catch (Exception e) {
                Log.e(TAG, "Error creating incoming call notification channel: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Show incoming call notification as per Android documentation
     * Creates a heads-up notification with full-screen intent for incoming calls
     */
    public void showIncomingCallNotification(String callerName, String callerNumber) {
        try {
            Log.d(TAG, "Creating incoming call notification for: " + callerName + " (" + callerNumber + ")");
            
            // Create an intent which triggers the fullscreen incoming call user interface
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setClass(context, IncomingCallActivity.class);
            
            Log.d(TAG, "Intent created with flags: " + intent.getFlags());
            
            // Add caller information to the intent
            intent.putExtra("caller_name", callerName);
            intent.putExtra("caller_number", callerNumber);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                Constants.INCOMING_CALL_NOTIFICATION_ID, 
                intent, 
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            
            // Build the notification as an ongoing high priority item
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(context, Constants.INCOMING_CALL_CHANNEL_ID);
            } else {
                builder = new Notification.Builder(context);
                builder.setPriority(Notification.PRIORITY_HIGH);
            }
            
            builder.setOngoing(true)
                .setCategory(Notification.CATEGORY_CALL)
                .setSmallIcon(R.drawable.ic_notification) // Using existing phone icon
                .setContentTitle("Incoming call")
                .setContentText(callerName != null && !callerName.isEmpty() ? callerName : callerNumber)
                .setLargeIcon((android.graphics.Bitmap) null) // You can add a contact photo here if available
                // Set notification content intent to take user to the fullscreen UI if user taps on the notification body
                .setContentIntent(pendingIntent)
                // Set full screen intent to trigger display of the fullscreen UI when the notification manager deems it appropriate
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
            
            // Add action buttons for answer and decline
            addIncomingCallActions(builder);
            
            // Show the notification
            Log.d(TAG, "Attempting to show notification with ID: " + Constants.INCOMING_CALL_NOTIFICATION_ID);
            notificationManager.notify(
                Constants.INCOMING_CALL_NOTIFICATION_ID, 
                builder.build()
            );
            
            Log.d(TAG, "Incoming call notification successfully shown for: " + callerName);
            Log.d(TAG, "Full-screen intent should trigger IncomingCallActivity");
        } catch (Exception e) {
            Log.e(TAG, "Error showing incoming call notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * Add action buttons to the incoming call notification
     */
    private void addIncomingCallActions(Notification.Builder builder) {
        try {
            // Answer call action
            Intent answerIntent = new Intent(context, IncomingCallActivity.class);
            answerIntent.setAction(Constants.CALL_ACTION_ANSWER);
            answerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            PendingIntent answerPendingIntent = PendingIntent.getActivity(
                context,
                1,
                answerIntent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            
            // Decline call action
            Intent declineIntent = new Intent(context, IncomingCallActivity.class);
            declineIntent.setAction(Constants.CALL_ACTION_DECLINE);
            declineIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            PendingIntent declinePendingIntent = PendingIntent.getActivity(
                context,
                2,
                declineIntent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            
            // Add the actions to the notification
            builder.addAction(R.drawable.ic_call, "Answer", answerPendingIntent);
            builder.addAction(R.drawable.ic_call_end, "Decline", declinePendingIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding incoming call actions: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancel the incoming call notification
     */
    public void cancelIncomingCallNotification() {
        try {
            // Cancel the notification using just the notification ID (not channel ID as tag)
            notificationManager.cancel(Constants.INCOMING_CALL_NOTIFICATION_ID);
            Log.d(TAG, "Incoming call notification cancelled with ID: " + Constants.INCOMING_CALL_NOTIFICATION_ID);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling incoming call notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancel system missed call notifications
     * This helps prevent duplicate notifications from the system
     */
    public void cancelSystemMissedCallNotifications() {
        try {
            if (notificationManager != null) {
                // Cancel common system missed call notification IDs
                for (int id : Constants.SYSTEM_MISSED_CALL_NOTIFICATION_IDS) {
                    notificationManager.cancel(id);
                }
                
                // Also try to cancel by tag if system uses tags
                notificationManager.cancel(Constants.SYSTEM_MISSED_CALL_TAG, 1);
                notificationManager.cancel(Constants.SYSTEM_CALL_TAG, 1);
                
                Log.d(TAG, "Attempted to cancel system missed call notifications");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error canceling system notifications: " + e.getMessage(), e);
        }
    }
}
