package com.spamcalldetector.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.telecom.Call;
import android.util.Log;

import com.spamcalldetector.helpers.CallManager;
import com.spamcalldetector.helpers.Constants;

/**
 * Helper class to manage call state detection logic
 */
public class CallStateHelper {
    private static final String TAG = "CallStateHelper";

    // Detection timeouts (moved to Constants class)

    // State flags
    private boolean isPotentialVoicemail = false;
    private boolean isOutgoingCall = false;

    // Handlers
    private final Handler voicemailTimeoutHandler;
    private final Handler callWaitingHandler;

    // Context
    private final Context context;
    private String callerName;

    public CallStateHelper(Context context) {
        this.context = context;
        voicemailTimeoutHandler = new Handler();
        callWaitingHandler = new Handler();
    }

    /**
     * Set caller name for notifications
     */
    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    /**
     * Track if this is an outgoing call
     */
    public void setIsOutgoingCall(boolean isOutgoingCall) {
        this.isOutgoingCall = isOutgoingCall;
    }

    /**
     * Start call waiting detection for outgoing calls
     */
    public void startCallWaitingDetection(Call call) {
        if (call == null) {
            Log.e(TAG, "Cannot start call waiting detection - call is null");
            return;
        }

        try {
            int initialState = call.getState();
            Log.d(TAG, "Starting call waiting detection for " + callerName + " - Initial state: " + initialState);

            // For outgoing calls, set a timer to detect call waiting
            callWaitingHandler.postDelayed(() -> {
                // Make sure call is still valid
                if (call == null || !CallManager.getActiveCalls().contains(call)) {
                    Log.d(TAG, "Call no longer valid during waiting detection, aborting");
                    return;
                }

                try {
                    int currentState = call.getState();
                    Log.d(TAG, "Call waiting check - current state: " + currentState +
                            " for " + callerName + " after " + (Constants.CALL_WAITING_DETECTION_DELAY / 1000) + " seconds");

                    if (currentState == Call.STATE_DIALING || currentState == Call.STATE_CONNECTING) {
                        Log.d(TAG, "Call waiting detected - call still in dialing/connecting state after " +
                                (Constants.CALL_WAITING_DETECTION_DELAY / 1000) + " seconds");

                        // Broadcast that we detected call waiting
                        Intent callWaitingIntent = new Intent("ACTION_CALL_WAITING_DETECTED");
                        context.sendBroadcast(callWaitingIntent);
                        Log.d(TAG, "Call waiting broadcast sent");

                        // Schedule auto-disconnect if still in call waiting state
                        callWaitingHandler.postDelayed(() -> {
                            if (call == null || !CallManager.getActiveCalls().contains(call)) {
                                Log.d(TAG, "Call no longer valid during waiting timeout, aborting");
                                return;
                            }

                            try {
                                int finalState = call.getState();
                                Log.d(TAG, "Call waiting timeout check - current state: " + finalState +
                                        " for " + callerName + " after additional " + (Constants.CALL_WAITING_TIMEOUT / 1000)
                                        + " seconds");

                                if ((finalState == Call.STATE_DIALING || finalState == Call.STATE_CONNECTING) &&
                                        CallManager.getActiveCalls().contains(call)) {
                                    Log.d(TAG, "Auto hanging up after call waiting timeout of " +
                                            ((Constants.CALL_WAITING_DETECTION_DELAY + Constants.CALL_WAITING_TIMEOUT) / 1000)
                                            + " total seconds");
                                    call.disconnect();

                                    // Broadcast call ended
                                    Intent callEndedIntent = new Intent(Constants.ACTION_CALL_ENDED);
                                    callEndedIntent.putExtra("REASON", "CALL_WAITING_TIMEOUT");
                                    context.sendBroadcast(callEndedIntent);
                                } else {
                                    Log.d(TAG, "Call state changed during waiting period, not auto-hanging up");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error in call waiting timeout handler: " + e.getMessage());
                            }
                        }, Constants.CALL_WAITING_TIMEOUT - Constants.CALL_WAITING_DETECTION_DELAY);
                    } else {
                        Log.d(TAG, "Call is no longer in dialing/connecting state, not considered waiting");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking call state in waiting detection: " + e.getMessage());
                }
            }, Constants.CALL_WAITING_DETECTION_DELAY);
        } catch (Exception e) {
            Log.e(TAG, "Error starting call waiting detection: " + e.getMessage());
        }
    }

    /**
     * Start voicemail detection for answered calls
     */
    public void startVoicemailDetection(Call call) {
        if (call == null) {
            Log.e(TAG, "Cannot start voicemail detection - call is null");
            return;
        }

        // When a call is "answered", it could be a human or voicemail
        // Set a flag that this might be a voicemail, particularly for outgoing calls
        isPotentialVoicemail = true;

        Log.d(TAG, "Starting voicemail detection for " + callerName +
                " - Will check after " + (Constants.VOICEMAIL_DETECTION_DELAY / 1000) + " seconds");

        // Set a timer to check if this is likely a voicemail
        // We'll reset this flag if we detect user interaction or audio from the other
        // party
        try {
            voicemailTimeoutHandler.postDelayed(() -> {
                Log.d(TAG, "Voicemail detection check for " + callerName +
                        " - isPotentialVoicemail: " + isPotentialVoicemail);

                if (isPotentialVoicemail) {
                    Log.d(TAG, "Call appears to be voicemail - starting auto-hangup timer of " +
                            (Constants.VOICEMAIL_AUTO_HANGUP_DELAY / 1000) + " seconds");

                    // Broadcast that we detected a likely voicemail
                    Intent voicemailIntent = new Intent("ACTION_VOICEMAIL_DETECTED");
                    context.sendBroadcast(voicemailIntent);
                    Log.d(TAG, "Voicemail detected broadcast sent");

                    // Set another timer to hang up after a reasonable voicemail recording time
                    voicemailTimeoutHandler.postDelayed(() -> {
                        try {
                            Log.d(TAG, "Voicemail timeout check for " + callerName +
                                    " - isPotentialVoicemail: " + isPotentialVoicemail +
                                    ", call still active: "
                                    + (call != null && CallManager.getActiveCalls().contains(call)));

                            if (isPotentialVoicemail && call != null && CallManager.getActiveCalls().contains(call)) {
                                Log.d(TAG, "Auto hanging up after voicemail recording timeout of " +
                                        (Constants.VOICEMAIL_AUTO_HANGUP_DELAY / 1000) + " seconds");
                                call.disconnect();

                                // Broadcast call ended with reason
                                Intent callEndedIntent = new Intent(Constants.ACTION_CALL_ENDED);
                                callEndedIntent.putExtra("REASON", "VOICEMAIL_TIMEOUT");
                                context.sendBroadcast(callEndedIntent);
                            } else {
                                Log.d(TAG,
                                        "Not auto-hanging up - either human interaction detected or call already ended");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in voicemail timeout handler: " + e.getMessage());
                        }
                    }, Constants.VOICEMAIL_AUTO_HANGUP_DELAY);
                } else {
                    Log.d(TAG, "Human interaction detected, not treating as voicemail");
                }
            }, Constants.VOICEMAIL_DETECTION_DELAY);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up voicemail detection: " + e.getMessage());
        }
    }

    /**
     * Call this method when human interaction is detected on the call
     * (e.g., user pressed mute, speaker, or we detected audio from other side)
     */
    public void onHumanInteractionDetected(String interactionType) {
        boolean wasVoicemail = isPotentialVoicemail;

        // This is not a voicemail if we detect human interaction
        isPotentialVoicemail = false;

        Log.d(TAG, "Human interaction detected: " + (interactionType != null ? interactionType : "unknown") +
                " for " + callerName + ", was potential voicemail: " + wasVoicemail +
                ", disabling voicemail auto-hangup");

        // Cancel any pending voicemail detection tasks since we confirmed human
        // interaction
        voicemailTimeoutHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Clean up handlers
     */
    public void cleanup() {
        if (voicemailTimeoutHandler != null) {
            voicemailTimeoutHandler.removeCallbacksAndMessages(null);
        }

        if (callWaitingHandler != null) {
            callWaitingHandler.removeCallbacksAndMessages(null);
        }

        isPotentialVoicemail = false;
        isOutgoingCall = false;
    }

    /**
     * Is this call potentially a voicemail
     */
    public boolean isPotentialVoicemail() {
        return isPotentialVoicemail;
    }

    /**
     * Cancel call waiting detection
     */
    public void cancelCallWaitingDetection() {
        if (callWaitingHandler != null) {
            callWaitingHandler.removeCallbacksAndMessages(null);
        }
    }
}
