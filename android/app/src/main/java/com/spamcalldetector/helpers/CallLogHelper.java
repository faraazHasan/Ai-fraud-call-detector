package com.spamcalldetector.helpers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.util.Log;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to access and process call logs
 */
public class CallLogHelper {
    private static final String TAG = "CallLogHelper";

    /**
     * Get recent call logs from the device
     * 
     * @param context The application context
     * @param limit   Maximum number of entries to return
     * @return List of maps containing call data
     */
    public static List<Map<String, Object>> getRecentCalls(Context context, int limit) {
        List<Map<String, Object>> callLogs = new ArrayList<>();

        try {
            Log.d(TAG, "Starting call log retrieval with limit: " + limit);

            // Check for READ_CALL_LOG permission
            int permissionCheck = android.content.pm.PackageManager.PERMISSION_DENIED;
            try {
                permissionCheck = context.checkCallingOrSelfPermission(android.Manifest.permission.READ_CALL_LOG);
            } catch (Exception e) {
                Log.e(TAG, "Error checking READ_CALL_LOG permission: " + e.getMessage());
            }

            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "READ_CALL_LOG permission not granted");
                return callLogs; // Return empty list
            }

            Log.d(TAG, "Permission check passed, accessing call logs");
            String[] projection = new String[] {
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.NEW // Add this to check if call was seen
            };

            Uri callLogUri = CallLog.Calls.CONTENT_URI;
            Log.d(TAG, "Querying content resolver for call logs");
            Cursor cursor = context.getContentResolver().query(
                    callLogUri,
                    projection,
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC LIMIT " + limit);

            if (cursor != null) {
                int idColumn = cursor.getColumnIndex(CallLog.Calls._ID);
                int numberColumn = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int nameColumn = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                int dateColumn = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationColumn = cursor.getColumnIndex(CallLog.Calls.DURATION);
                int typeColumn = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int newColumn = cursor.getColumnIndex(CallLog.Calls.NEW);

                while (cursor.moveToNext()) {
                    String id = cursor.getString(idColumn);
                    String phoneNumber = cursor.getString(numberColumn);
                    String contactName = cursor.getString(nameColumn);
                    long timestamp = cursor.getLong(dateColumn);
                    int duration = cursor.getInt(durationColumn);
                    int type = cursor.getInt(typeColumn);
                    int isNew = cursor.getInt(newColumn); // 1 = new/unseen call

                    // Convert call type to string
                    String callType;
                    switch (type) {
                        case CallLog.Calls.INCOMING_TYPE:
                            callType = "incoming";
                            break;
                        case CallLog.Calls.OUTGOING_TYPE:
                            callType = "outgoing";
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            callType = "missed";
                            break;
                        case CallLog.Calls.REJECTED_TYPE:
                            callType = "rejected";
                            break;
                        default:
                            callType = "unknown";
                            break;
                    }

                    // Create a map for each call record
                    Map<String, Object> callData = new HashMap<>();
                    callData.put("id", id);
                    callData.put("phoneNumber", phoneNumber);
                    callData.put("contactName", contactName == null ? "" : contactName);
                    callData.put("timestamp", timestamp);
                    callData.put("duration", duration);
                    callData.put("type", callType);
                    callData.put("isNew", isNew == 1);

                    callLogs.add(callData);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting call logs: " + e.getMessage());
        }

        return callLogs;
    }

    /**
     * Mark a missed call as read in the call log
     */
    public static boolean markCallAsRead(Context context, String callId) {
        try {
            // Values to update
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(CallLog.Calls.NEW, 0); // 0 = not new/seen

            // Where clause
            String whereClause = CallLog.Calls._ID + " = ?";
            String[] whereArgs = new String[] { callId };

            // Update the call log
            int rowsAffected = context.getContentResolver().update(
                    CallLog.Calls.CONTENT_URI,
                    values,
                    whereClause,
                    whereArgs);

            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error marking call as read: " + e.getMessage());
            return false;
        }
    }
}
