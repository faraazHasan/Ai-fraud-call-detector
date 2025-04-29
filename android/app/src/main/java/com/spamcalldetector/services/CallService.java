package com.spamcalldetector.services;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import com.spamcalldetector.MainActivity;
import com.spamcalldetector.activities.call.IncomingCallActivity;
import com.spamcalldetector.activities.call.OutgoingCallActivity;
import com.spamcalldetector.helpers.CallManager;
import com.spamcalldetector.utils.CallStateManager;
import com.spamcalldetector.activities.call.CallActivityModule;

public class CallService extends InCallService {

    private static final String TAG = "CallService";
    private static final String TWILIO_NUMBER = "+18452998019";

    private Ringtone ringtone;
    private Handler handler;
    private Runnable updateCallTimeRunnable;
    private long startTime;
    private long elapsedTime;
    private String callStatus;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "📞 InCallService Started");
        CallManager.inCallService = this;
        handler = new Handler();
        CallStateManager.setCallOngoing(true);
        callStatus = "Connecting...";
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        CallManager.inCallService = this;
        CallManager.registerCall(call);

        int state = call.getState();
        Log.d(TAG, "Call Added. State: " + state);

        if (state == Call.STATE_RINGING) {
            handleIncomingCall(call);
        } else if (state == Call.STATE_DIALING || state == Call.STATE_CONNECTING) {
            handleOutgoingCall(call);
        }
    }

    private void handleIncomingCall(Call call) {
        Log.d(TAG, "Incoming Call");

        // Play ringtone when the incoming call is received
        playRingtone();

        // Create an intent for IncomingCallActivity
        Intent incomingCallIntent = new Intent(this, IncomingCallActivity.class);
        incomingCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add flags to ensure activity appears on top of the lock screen
        incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // Ensure the activity is single instance

        // Start IncomingCallActivity
        startActivity(incomingCallIntent);

        // Update call status (if needed)
        callStatus = "Incoming...";
    }

    private void handleOutgoingCall(Call call) {
        Log.d(TAG, "Outgoing Call");

        String number = call.getDetails().getHandle().getSchemeSpecificPart();
        if (!TWILIO_NUMBER.equals(number)) {
            Intent intent = new Intent(this, OutgoingCallActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        callStatus = "Calling...";
    }

    public void onCallAnswered(Call call) {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }

        if (CallManager.getActiveCalls().size() == 1) {
            startCallTimer();
        }

        callStatus = formatElapsedTime(elapsedTime);
    }

    private void playRingtone() {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing ringtone: " + e.getMessage());
        }
    }

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
        Log.d(TAG, "Call Removed");

        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }

        CallManager.unregisterCall(call);

        if (CallManager.getActiveCalls().isEmpty()) {
            stopCallTimer();
            CallStateManager.setCallOngoing(false);
        }
    }
}
