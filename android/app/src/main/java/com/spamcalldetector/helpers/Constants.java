package com.spamcalldetector.helpers;

/**
 * Centralized constants class for the entire application
 * Contains all hardcoded values, IDs, and configuration constants
 */
public class Constants {

    // Call State Constants
    public static final String BIZ4_CALL_STATE_INCOMING = "incoming_call";
    public static final String BIZ4_CALL_STATE_OUTGOING = "outgoing_call";
    public static final String BIZ4_CALL_STATE_INGOING_CALL = "ingoing_call";
    public static final String BIZ4_UNKNOWN_CALLER_NAME = "(Unknown)";

    // Notification IDs
    public static final int MISSED_CALL_NOTIFICATION_ID = 2001;
    public static final int INCOMING_CALL_NOTIFICATION_ID = 2002;
    public static final int FOREGROUND_SERVICE_ID = 3001;
    public static final int FOREGROUND_NOTIFICATION_ID = 3001; // Alias for FOREGROUND_SERVICE_ID

    // Notification Channels
    public static final String MISSED_CALL_CHANNEL_ID = "missed_call_channel";
    public static final String INCOMING_CALL_CHANNEL_ID = "incoming_call_channel";
    public static final String FOREGROUND_SERVICE_CHANNEL_ID = "foreground_service_channel";

    // Request Codes
    public static final int REQUEST_CODE_MANAGE_STORAGE = 101;
    public static final int REQUEST_CODE_CAPTURE_AUDIO_OUTPUT = 102;
    public static final int REQUEST_DEFAULT_DIALER = 1001;

    // Timing Constants (in milliseconds)
    public static final long VOICEMAIL_DETECTION_DELAY = 5000; // 5 seconds
    public static final long CALL_WAITING_DETECTION_DELAY = 20000; // 20 seconds
    public static final long CALL_WAITING_TIMEOUT = 45000; // 45 seconds
    public static final long CALL_LOG_CHECK_DELAY = 1000; // 1 second
    public static final long VOICEMAIL_AUTO_HANGUP_DELAY = 2 * 60 * 1000; // 2 minutes

    // Broadcast Actions
    public static final String ACTION_RESET_MISSED_CALL_COUNT = "ACTION_RESET_MISSED_CALL_COUNT";
    public static final String ACTION_CALL_ANSWERED = "ACTION_CALL_ANSWERED";
    public static final String ACTION_CALL_ENDED = "ACTION_CALL_ENDED";
    public static final String ACTION_CALL_WAITING_DETECTED = "ACTION_CALL_WAITING_DETECTED";
    public static final String ACTION_VOICEMAIL_DETECTED = "ACTION_VOICEMAIL_DETECTED";
    public static final String ACTION_MISSED_CALL = "ACTION_MISSED_CALL";
    public static final String ACTION_MISSED_CALL_DETECTED = "ACTION_MISSED_CALL_DETECTED";
    public static final String NOTIFY_JS_MISSED_CALL = "NOTIFY_JS_MISSED_CALL";

    // Call Actions
    public static final String CALL_ACTION_ANSWER = "ANSWER_CALL";
    public static final String CALL_ACTION_DECLINE = "DECLINE_CALL";
    public static final String CALL_ACTION_END = "END_CALL";

    // Missed Call Vibration Pattern (only)
    public static final long[] MISSED_CALL_VIBRATION_PATTERN = {0, 500, 200, 500};

    // System Notification IDs (for cancellation)
    public static final int[] SYSTEM_MISSED_CALL_NOTIFICATION_IDS = {1, 2, 3, 4, 5, 10, 100, 1000};
    public static final String SYSTEM_MISSED_CALL_TAG = "missed_call";
    public static final String SYSTEM_CALL_TAG = "call";

    // Special numbers
    public static final String TWILIO_NUMBER = "+18452998019";

    // Private constructor to prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
