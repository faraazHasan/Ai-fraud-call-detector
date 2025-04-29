package com.spamcalldetector.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.util.Log;
import android.widget.Toast;

import com.spamcalldetector.activities.call.IncomingCallActivity;
import com.spamcalldetector.activities.call.OutgoingCallActivity;
import com.spamcalldetector.services.CallService;

import java.util.ArrayList;
import java.util.List;

public class CallManager {

    @SuppressLint("StaticFieldLeak")
    public static InCallService inCallService;

    private static final String TWILIO_NUMBER = "+18452998019"; // Your Twilio number

    private static final List<Call> activeCalls = new ArrayList<>();

    public static int BIZ4_CALL_STATE = 0;

    // Register a call into active calls list
    public static synchronized void registerCall(Call call) {
        if (!activeCalls.contains(call)) {
            activeCalls.add(call);
            call.registerCallback(callback);
        }
    }

    // Unregister a call when it's removed
    public static synchronized void unregisterCall(Call call) {
        call.unregisterCallback(callback);
        activeCalls.remove(call);
    }

    // Get the list of active calls
    public static synchronized List<Call> getActiveCalls() {
        return new ArrayList<>(activeCalls);
    }

    // Find a call by caller ID
    public static synchronized Call findCallByCallerId(String callerId) {
        for (Call call : activeCalls) {
            String id = getCallerIdFromCall(call);
            if (id != null && id.equals(callerId))
                return call;
        }
        return null;
    }

    public static Call.Callback callback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int newState) {
            Log.d("Dialer", "onStateChanged: " + newState);
            BIZ4_CALL_STATE = newState;

            // Handle call state changes
            if (newState == Call.STATE_ACTIVE) {
                closeCallScreens();
                // Handle outgoing call screen and call disconnection logic
                List<Call> activeCalls = CallManager.getActiveCalls(); // Get active calls

                StringBuilder phoneNumbers = new StringBuilder("Phone Numbers: ");
                for (Call c : activeCalls) {
                    // Assuming 'getPhoneNumber()' method exists in the Call class
                    phoneNumbers.append(getCallerIdFromCall(c)).append(", ");
                }

                // Trim the last comma and space if any
                if (phoneNumbers.length() > 0) {
                    phoneNumbers.setLength(phoneNumbers.length() - 2);
                }
                if (inCallService instanceof CallService) {
                    ((CallService) inCallService).onCallAnswered(call);
                }
                Log.d("Dialer", "all calls after active: " + activeCalls.size() + "\n" + phoneNumbers.toString());

                Call twilioCall = findCallByCallerId(TWILIO_NUMBER);
                if (twilioCall != null) {
                    twilioCall.playDtmfTone('1');
                    twilioCall.stopDtmfTone();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        createConferenceCall(inCallService);
                    }, 2000);
                }

                inCallService.sendBroadcast(new Intent("call_answered"));
            }

            if (newState == Call.STATE_DISCONNECTED) {
                // Check if the disconnected call is part of a conference and handle it
                if (call.getChildren() != null && !call.getChildren().isEmpty()) {
                    Log.d("Dialer", "This call is part of a conference, handling participant left.");
                    handleConferenceParticipantLeft(call);
                } else {
                    Log.d("Dialer", "This call is NOT part of a conference.");
                }
                unregisterCall(call);

                // Handle outgoing call screen logic
                Activity incoming = IncomingCallActivity.getActivity();
                Activity outgoing = OutgoingCallActivity.getActivity();
                List<Call> activeCalls = CallManager.getActiveCalls(); // Get active calls

                // Finish incoming call screen if it's still active
                if (incoming != null) {
                    incoming.finish();
                }

                // Handle outgoing call screen and call disconnection logic
                StringBuilder phoneNumbers = new StringBuilder("Phone Numbers: ");
                for (Call c : activeCalls) {
                    // Assuming 'getPhoneNumber()' method exists in the Call class
                    phoneNumbers.append(getCallerIdFromCall(c)).append(", ");
                }

                // Trim the last comma and space if any
                if (phoneNumbers.length() > 0) {
                    phoneNumbers.setLength(phoneNumbers.length() - 2);
                }

                // Finish outgoing call screen if there are no calls left
                if (outgoing != null && activeCalls.size() <= 0) {
                    outgoing.finish();
                } else if (outgoing != null && activeCalls.size() == 1) {
                    // If there is only one call left, handle it (for example, hanging up Twilio
                    // call)
                    Call twilioCall = findCallByCallerId(TWILIO_NUMBER);
                    if (twilioCall != null) {
                        hangUpCall(twilioCall);
                    }
                }

                // Send broadcast that the call has ended
                Intent intent = new Intent("call_ended");
                inCallService.sendBroadcast(intent);

                Log.d("Dialer", "all calls after: " + activeCalls.size() + "\n" + phoneNumbers.toString());
                // Log the disconnection event
                Log.d("Dialer", "Call disconnect event.");
            }
        }

        @Override
        public void onChildrenChanged(Call conference, List<Call> children) {
            Log.d("Dialer", "Conference children updated: " + children.size());
        }
    };

    // Handle when a conference participant leaves
    private static void handleConferenceParticipantLeft(Call disconnectedCall) {
        Log.d("Dialer", "Called handleConferenceParticipantLeft");

        for (Call conference : activeCalls) {
            if (!conference.getChildren().isEmpty()) {
                List<Call> remaining = conference.getChildren();
                List<Call> others = new ArrayList<>();

                for (Call c : remaining) {
                    String id = getCallerIdFromCall(c);
                    if (id != null && !id.equals(TWILIO_NUMBER)) {
                        others.add(c);
                    }
                }
                Log.d("Dialer", "others.size(): " + others.size());

                // If only Twilio is left in the conference, disconnect all calls and finish the
                // outgoing screen
                if (others.size() == 0) {
                    Log.d("Dialer", "Only Twilio and self left — ending call");
                    conference.disconnect();
                    finishOutgoingCallScreen();
                } else {
                    // If there's another participant, just remove the one who left
                    Log.d("Dialer", "Other participant left, conference continues");
                    // Find the call that disconnected and remove it from the conference
                    Call disconnected = findCallByCallerId(getCallerIdFromCall(disconnectedCall));
                    if (disconnected != null) {
                        disconnected.disconnect();
                    }
                }
                break;
            }
        }
    }

    // Finish the OutgoingCallActivity screen if it's still open
    public static void finishOutgoingCallScreen() {
        Activity outgoing = OutgoingCallActivity.getActivity();
        if (outgoing != null) {
            outgoing.finish();
        }
    }

    // Close the incoming and outgoing call screens
    private static void closeCallScreens() {
        Activity incoming = IncomingCallActivity.getActivity();
        Activity outgoing = OutgoingCallActivity.getActivity();
        if (incoming != null)
            incoming.finish();

        if (outgoing == null) {
            Intent intent = new Intent(inCallService, OutgoingCallActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            inCallService.startActivity(intent);
        }
    }

    // Create a conference call if there are at least two active calls
    public static void createConferenceCall(Context context) {
        if (activeCalls.size() < 2) {
            Log.d("Dialer", "Need at least 2 calls for conference.");
            return;
        }

        Call first = activeCalls.get(0);
        Call second = activeCalls.get(1);

        if (first.getState() != Call.STATE_HOLDING) {
            first.hold();
            Log.d("Dialer", "First call put on hold.");
        }

        if (second.getState() != Call.STATE_ACTIVE) {
            Log.d("Dialer", "Answering the second call.");
            answerCall(second);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                first.conference(second);
                Log.d("Dialer", "Conference created successfully.");
                // Optionally, enable speakerphone
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                    audioManager.setSpeakerphoneOn(true);
                }
            } catch (Exception e) {
                Log.e("Dialer", "Error creating conference: " + e.getMessage());
            }
        }, 1500); // Delay to ensure the second call is properly answered before the conference
    }

    // Answer the call
    public static void answerCall(Call call) {
        call.answer(VideoProfile.STATE_AUDIO_ONLY);
    }

    // Hang up a call
    public static void hangUpCall(Call call) {
        call.disconnect();
    }

    // Put a call on hold
    public static void holdCall(Call call) {
        call.hold();
        Toast.makeText(inCallService, "Call on hold", Toast.LENGTH_SHORT).show();
    }

    // Unhold a call
    public static void unholdCall(Call call) {
        call.unhold();
        Toast.makeText(inCallService, "Call unheld", Toast.LENGTH_SHORT).show();
    }

    // Mute or unmute the call
    public static void muteCall(boolean isMuted) {
        inCallService.setMuted(isMuted);
        Toast.makeText(inCallService, isMuted ? "Call muted" : "Call unmuted", Toast.LENGTH_SHORT).show();
    }

    // Toggle the speakerphone on or off
    public static void speakerCall(boolean isSpeakerOn) {
        int route = isSpeakerOn ? CallAudioState.ROUTE_SPEAKER : CallAudioState.ROUTE_EARPIECE;
        inCallService.setAudioRoute(route);
        Toast.makeText(inCallService, isSpeakerOn ? "Speaker on" : "Speaker off", Toast.LENGTH_SHORT).show();
    }

    // Helper method to get caller ID from a call
    private static String getCallerIdFromCall(Call call) {
        if (call == null || call.getDetails() == null)
            return null;
        Uri handle = call.getDetails().getHandle();
        return (handle != null) ? handle.getSchemeSpecificPart() : null;
    }

    // Get the latest active or ringing call
    public static Call getLatestActiveOrRingingCall() {
        for (int i = activeCalls.size() - 1; i >= 0; i--) {
            Call call = activeCalls.get(i);
            int state = call.getState();
            if (state == Call.STATE_RINGING || state == Call.STATE_DIALING ||
                    state == Call.STATE_CONNECTING || state == Call.STATE_ACTIVE) {
                return call;
            }
        }
        return null;
    }
}
