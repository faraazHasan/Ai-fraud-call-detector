package com.spamcalldetector.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.CallLog;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.spamcalldetector.MainActivity;
import com.spamcalldetector.helpers.ContactsHelper;
import com.spamcalldetector.helpers.Constants;
import java.util.HashSet;
import java.util.Set;

/**
 * Service to monitor call logs and create custom missed call notifications
 * This service overrides the system's default missed call notifications
 */
public class MissedCallNotificationService extends Service {
    private static final String TAG = "MissedCallNotificationService";
    
    private CallLogObserver callLogObserver;
    private NotificationManager notificationManager;
    private Set<String> processedCallIds = new HashSet<>();
    private int missedCallCount = 0;
    private String lastMissedCallNumber = "";
    private String lastMissedCallName = "";
    private Handler mainHandler;
    private BroadcastReceiver resetReceiver;
    private BroadcastReceiver callStateReceiver;
    
    // Track active/ringing calls to prevent missed call notifications during active calls
    private Set<String> activeOrRingingCalls = new HashSet<>();
    private boolean isCallServiceActive = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MissedCallNotificationService created");
        
        mainHandler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        createNotificationChannels();
        startForegroundService();
        startMonitoringCallLog();
        setupResetReceiver();
        setupCallStateReceiver();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MissedCallNotificationService started");
        return START_STICKY; // Restart if killed
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is not a bound service
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MissedCallNotificationService destroyed");
        
        if (callLogObserver != null) {
            getContentResolver().unregisterContentObserver(callLogObserver);
        }
        
        if (resetReceiver != null) {
            try {
                unregisterReceiver(resetReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering reset receiver: " + e.getMessage());
            }
        }
        
        if (callStateReceiver != null) {
            try {
                unregisterReceiver(callStateReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering call state receiver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Create notification channels for missed calls and foreground service
     */
    private void createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Missed call notification channel
            NotificationChannel missedCallChannel = new NotificationChannel(
                Constants.MISSED_CALL_CHANNEL_ID,
                "Missed Calls",
                NotificationManager.IMPORTANCE_HIGH
            );
            missedCallChannel.setDescription("Notifications for missed calls");
            missedCallChannel.enableLights(true);
            missedCallChannel.setLightColor(android.graphics.Color.RED);
            missedCallChannel.enableVibration(true);
            missedCallChannel.setVibrationPattern(Constants.MISSED_CALL_VIBRATION_PATTERN);
            missedCallChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            missedCallChannel.setShowBadge(true);
            // Ensure sound is enabled
            missedCallChannel.setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );
            
            // Foreground service notification channel (hidden)
            NotificationChannel foregroundChannel = new NotificationChannel(
                Constants.FOREGROUND_SERVICE_CHANNEL_ID,
                "Background Service",
                NotificationManager.IMPORTANCE_MIN
            );
            foregroundChannel.setDescription("Background service to monitor call events");
            foregroundChannel.setShowBadge(false);
            foregroundChannel.setSound(null, null); // No sound for background service
            foregroundChannel.enableLights(false);
            foregroundChannel.enableVibration(false);
            foregroundChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(missedCallChannel);
                notificationManager.createNotificationChannel(foregroundChannel);
                Log.d(TAG, "Notification channels created");
            }
        }
    }
    
    /**
     * Start the service as a foreground service
     */
    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = new NotificationCompat.Builder(this, Constants.FOREGROUND_SERVICE_CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build();
        
        startForeground(Constants.FOREGROUND_SERVICE_ID, notification);
        Log.d(TAG, "Started as foreground service");
    }
    
    /**
     * Start monitoring the call log for changes
     */
    private void startMonitoringCallLog() {
        try {
            callLogObserver = new CallLogObserver(mainHandler);
            getContentResolver().registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                callLogObserver
            );
            Log.d(TAG, "Started monitoring call log");
            
            // Also check for existing missed calls on startup
            checkForMissedCalls();
        } catch (Exception e) {
            Log.e(TAG, "Error starting call log monitoring: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check for missed calls in the call log
     */
    private void checkForMissedCalls() {
        try {
            // Check for READ_CALL_LOG permission
            if (checkCallingOrSelfPermission(android.Manifest.permission.READ_CALL_LOG) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "READ_CALL_LOG permission not granted");
                return;
            }
            
            String[] projection = new String[] {
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE,
                CallLog.Calls.NEW
            };
            
            // Query for new missed calls only
            String selection = CallLog.Calls.TYPE + " = ? AND " + CallLog.Calls.NEW + " = ?";
            String[] selectionArgs = new String[] {
                String.valueOf(CallLog.Calls.MISSED_TYPE),
                "1" // NEW = 1 means unread/new
            };
            
            Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                CallLog.Calls.DATE + " DESC LIMIT 5" // Check last 5 missed calls
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String callId = cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID));
                    String phoneNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    String contactName = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                    long timestamp = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                    
                    // Only process if we haven't already processed this call
                    if (!processedCallIds.contains(callId)) {
                        Log.d(TAG, "Found new missed call: " + phoneNumber + " (ID: " + callId + ") at " + timestamp);
                        
                        // Check if this call is currently active/ringing or if CallService is active
                        if (isCallServiceActive || activeOrRingingCalls.contains(phoneNumber)) {
                            Log.d(TAG, "Skipping missed call notification for " + phoneNumber + " - call is currently active/ringing or CallService is active");
                            processedCallIds.add(callId); // Mark as processed to avoid future checks
                            continue;
                        }
                        
                        // Process the missed call immediately
                        Log.d(TAG, "Processing missed call: " + phoneNumber);
                        handleMissedCall(callId, phoneNumber, contactName, timestamp);
                        processedCallIds.add(callId);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for missed calls: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handle a missed call by creating a custom notification
     */
    private void handleMissedCall(String callId, String phoneNumber, String contactName, long timestamp) {
        try {
            Log.d(TAG, "Handling missed call from: " + phoneNumber);
            
            // Get contact name if not available
            if (contactName == null || contactName.isEmpty()) {
                contactName = ContactsHelper.getContactNameFromNumber(this, phoneNumber);
                if (contactName == null || contactName.isEmpty()) {
                    contactName = phoneNumber;
                }
            }
            
            // Update missed call tracking
            missedCallCount++;
            lastMissedCallNumber = phoneNumber;
            lastMissedCallName = contactName;
            
            // AGGRESSIVE system notification suppression
            // Step 1: Cancel system notifications BEFORE marking as read
            cancelSystemNotifications();
            
            // Step 2: Mark the call as read IMMEDIATELY
            markCallAsRead(callId);
            
            // Step 3: Cancel again after marking as read
            mainHandler.postDelayed(() -> cancelSystemNotifications(), 200);
            
            // Create custom missed call notification with count
            createMissedCallNotification(phoneNumber, contactName, timestamp);
            
            // Send broadcast to notify the app
            Intent missedCallIntent = new Intent("ACTION_MISSED_CALL");
            missedCallIntent.putExtra("phoneNumber", phoneNumber);
            missedCallIntent.putExtra("contactName", contactName);
            missedCallIntent.putExtra("timestamp", timestamp);
            sendBroadcast(missedCallIntent);
            
            Log.d(TAG, "Processed missed call notification for: " + contactName);
        } catch (Exception e) {
            Log.e(TAG, "Error handling missed call: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a custom missed call notification
     */
    private void createMissedCallNotification(String phoneNumber, String contactName, long timestamp) {
        try {
            // Create intent to open the app and navigate to Recent Calls screen
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mainIntent.putExtra("navigateTo", "RecentCalls");
            mainIntent.putExtra("openMissedCalls", true);
            mainIntent.putExtra("phoneNumber", phoneNumber);
            mainIntent.putExtra("fromNotification", true);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this, Constants.MISSED_CALL_NOTIFICATION_ID, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Create call back intent
            Intent callBackIntent = new Intent(Intent.ACTION_CALL);
            callBackIntent.setData(Uri.parse("tel:" + phoneNumber));
            PendingIntent callBackPendingIntent = PendingIntent.getActivity(
                this, 1, callBackIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Build the notification with count
            String title = missedCallCount == 1 ? "Missed Call" : missedCallCount + " Missed Calls";
            String content = missedCallCount == 1 ? 
                "Missed call from " + contactName :
                "Latest: " + contactName + " (" + missedCallCount + " missed calls)";
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.MISSED_CALL_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentTitle(title)
                .setContentText(content)
                .setSubText(phoneNumber)
                .setWhen(timestamp)
                .setNumber(missedCallCount)
                .setShowWhen(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .addAction(
                    android.R.drawable.ic_menu_call,
                    "Call Back",
                    callBackPendingIntent
                );
            
            // Show the notification
            if (notificationManager != null) {
                notificationManager.notify(Constants.MISSED_CALL_NOTIFICATION_ID, builder.build());
                Log.d(TAG, "Missed call notification created for: " + contactName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating missed call notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mark a call as read in the call log to prevent system notification
     */
    private void markCallAsRead(String callId) {
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(CallLog.Calls.NEW, 0); // Mark as read
            
            String whereClause = CallLog.Calls._ID + " = ?";
            String[] whereArgs = new String[] { callId };
            
            int rowsUpdated = getContentResolver().update(
                CallLog.Calls.CONTENT_URI,
                values,
                whereClause,
                whereArgs
            );
            
            if (rowsUpdated > 0) {
                Log.d(TAG, "Marked call as read: " + callId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error marking call as read: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancel system missed call notifications
     */
    private void cancelSystemNotifications() {
        try {
            if (notificationManager != null) {
                // Cancel common system missed call notification IDs
                for (int id : Constants.SYSTEM_MISSED_CALL_NOTIFICATION_IDS) {
                    notificationManager.cancel(id);
                }
                
                // Also try to cancel by tag if system uses tags
                notificationManager.cancel(Constants.SYSTEM_MISSED_CALL_TAG, 1);
                notificationManager.cancel(Constants.SYSTEM_CALL_TAG, 1);
                
                // Try common tags used by system
                String[] possibleTags = {"missed_call", "call", "phone", "dialer", "telecom", "android"};
                for (String tag : possibleTags) {
                    for (int id : Constants.SYSTEM_MISSED_CALL_NOTIFICATION_IDS) {
                        notificationManager.cancel(tag, id);
                    }
                }
                
                // Also try to cancel all notifications from system packages
                // This is more aggressive but necessary for some OEMs
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        // Cancel all active notifications and let our service recreate the custom one
                        android.service.notification.StatusBarNotification[] activeNotifications = 
                            notificationManager.getActiveNotifications();
                        
                        for (android.service.notification.StatusBarNotification notification : activeNotifications) {
                            // Only cancel if it's not our notification
                            if (notification.getId() != Constants.MISSED_CALL_NOTIFICATION_ID && 
                                notification.getId() != Constants.FOREGROUND_NOTIFICATION_ID) {
                                notificationManager.cancel(notification.getId());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not cancel active notifications: " + e.getMessage());
                }
                
                Log.d(TAG, "Attempted to cancel system missed call notifications");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling system notifications: " + e.getMessage(), e);
        }
    }
    
    /**
     * Setup broadcast receiver for call state changes
     */
    private void setupCallStateReceiver() {
        try {
            callStateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || intent.getAction() == null) {
                        return;
                    }
                    
                    String action = intent.getAction();
                    Log.d(TAG, "Received call state broadcast: " + action);
                    
                    switch (action) {
                        case "ACTION_CALL_SERVICE_STARTED":
                            isCallServiceActive = true;
                            Log.d(TAG, "CallService is now active - suppressing missed call notifications");
                            break;
                            
                        case "ACTION_CALL_SERVICE_STOPPED":
                            isCallServiceActive = false;
                            activeOrRingingCalls.clear();
                            Log.d(TAG, "CallService stopped - missed call notifications enabled");
                            break;
                            
                        case "ACTION_CALL_RINGING":
                            String ringingNumber = intent.getStringExtra("phoneNumber");
                            if (ringingNumber != null) {
                                activeOrRingingCalls.add(ringingNumber);
                                Log.d(TAG, "Call ringing from: " + ringingNumber + " - suppressing missed call notifications");
                            }
                            break;
                            
                        case "ACTION_CALL_ANSWERED":
                        case "ACTION_CALL_ENDED":
                            String endedNumber = intent.getStringExtra("phoneNumber");
                            if (endedNumber != null) {
                                activeOrRingingCalls.remove(endedNumber);
                                Log.d(TAG, "Call ended/answered for: " + endedNumber + " - removed from active calls");
                            }
                            break;
                    }
                }
            };
            
            IntentFilter filter = new IntentFilter();
            filter.addAction("ACTION_CALL_SERVICE_STARTED");
            filter.addAction("ACTION_CALL_SERVICE_STOPPED");
            filter.addAction("ACTION_CALL_RINGING");
            filter.addAction(Constants.ACTION_CALL_ANSWERED);
            filter.addAction(Constants.ACTION_CALL_ENDED);
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            
            // Register with proper export flag for Android 14+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(callStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                Log.d(TAG, "Call state receiver registered with RECEIVER_NOT_EXPORTED for Android 14+");
            } else {
                registerReceiver(callStateReceiver, filter);
                Log.d(TAG, "Call state receiver registered for Android < 14");
            }
            
            Log.d(TAG, "Call state receiver setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up call state receiver: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reset missed call count (call when user views missed calls)
     */
    public void resetMissedCallCount() {
        Log.d(TAG, "Resetting missed call count from " + missedCallCount + " to 0");
        missedCallCount = 0;
        if (notificationManager != null) {
            notificationManager.cancel(Constants.MISSED_CALL_NOTIFICATION_ID);
            Log.d(TAG, "Cancelled missed call notification with ID: " + Constants.MISSED_CALL_NOTIFICATION_ID);
        } else {
            Log.w(TAG, "NotificationManager is null, cannot cancel notification");
        }
    }
    
    /**
     * Setup broadcast receiver for reset missed call count
     */
    private void setupResetReceiver() {
        try {
            resetReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "BroadcastReceiver onReceive called with action: " + intent.getAction());
                    if (Constants.ACTION_RESET_MISSED_CALL_COUNT.equals(intent.getAction())) {
                        Log.d(TAG, "Received reset missed call count broadcast - processing reset");
                        resetMissedCallCount();
                    } else {
                        Log.w(TAG, "Received broadcast with unexpected action: " + intent.getAction());
                    }
                }
            };
            
            IntentFilter filter = new IntentFilter(Constants.ACTION_RESET_MISSED_CALL_COUNT);
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            
            // Register with proper export flag for Android 14+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(resetReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                Log.d(TAG, "Reset receiver registered with RECEIVER_NOT_EXPORTED for Android 14+");
            } else {
                registerReceiver(resetReceiver, filter);
                Log.d(TAG, "Reset receiver registered for Android < 14");
            }
            
            Log.d(TAG, "Reset receiver setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up reset receiver: " + e.getMessage(), e);
        }
    }
    
    /**
     * Content observer to monitor call log changes
     */
    private class CallLogObserver extends ContentObserver {
        public CallLogObserver(Handler handler) {
            super(handler);
        }
        
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(TAG, "Call log changed, checking for missed calls");
            
            // Delay the check slightly to ensure the call log is fully updated
            mainHandler.postDelayed(() -> checkForMissedCalls(), Constants.CALL_LOG_CHECK_DELAY);
        }
        
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            Log.d(TAG, "Call log changed at URI: " + uri);
            
            // Delay the check slightly to ensure the call log is fully updated
            mainHandler.postDelayed(() -> checkForMissedCalls(), Constants.CALL_LOG_CHECK_DELAY);
        }
    }
    

}
