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
                // Check if it was a missed call (ringing to disconnected without answering)
                boolean wasMissedCall = false;
                if (call.getDetails() != null && call.getDetails().getDisconnectCause() != null) {
                    int disconnectReason = call.getDetails().getDisconnectCause().getCode();
                    // Get previous state and log more details
                    int previousState = -1;
                    if (call.getDetails() != null) {
                        previousState = call.getDetails().getState();
                    }
                    Log.d("Dialer", "Call disconnected. Previous state: " + previousState +
                            ", Disconnect reason: " + disconnectReason);

                    // Enhanced missed call detection with more conditions
                    wasMissedCall = (previousState == Call.STATE_RINGING ||
                            disconnectReason == android.telecom.DisconnectCause.REJECTED ||
                            disconnectReason == android.telecom.DisconnectCause.MISSED ||
                            disconnectReason == android.telecom.DisconnectCause.LOCAL ||
                            disconnectReason == android.telecom.DisconnectCause.REMOTE);

                    // Broadcast missed call event to React Native
                    if (wasMissedCall) {
                        String phoneNumber = getCallerIdFromCall(call);
                        Log.d("Dialer", "Detected missed call from: " + phoneNumber);

                        String contactName = ContactsHelper
                                .getContactNameFromNumber(inCallService.getApplicationContext(), phoneNumber);
                        Log.d("Dialer", "Contact name: " + (contactName != null ? contactName : "Unknown"));

                        // Create the intent
                        Intent missedCallIntent = new Intent("ACTION_MISSED_CALL");
                        missedCallIntent.putExtra("phoneNumber", phoneNumber);
                        missedCallIntent.putExtra("contactName", contactName);

                        // Send broadcast using application context for more reliability
                        Log.d("Dialer", "Broadcasting ACTION_MISSED_CALL intent");
                        Context appContext = inCallService.getApplicationContext();
                        appContext.sendBroadcast(missedCallIntent);

                        // For extra reliability, also try to directly call the JS layer if possible
                        try {
                            // This will only work if we can access the ReactContext
                            if (inCallService != null) {
                                Log.d("Dialer", "Attempting additional notification method");
                                Intent jsNotifyIntent = new Intent("NOTIFY_JS_MISSED_CALL");
                                jsNotifyIntent.putExtra("phoneNumber", phoneNumber);
                                jsNotifyIntent.putExtra("contactName", contactName != null ? contactName : "");
                                jsNotifyIntent.putExtra("timestamp", System.currentTimeMillis());
                                appContext.sendBroadcast(jsNotifyIntent);
                            }
                        } catch (Exception e) {
                            Log.e("Dialer", "Error in additional notification: " + e.getMessage());
                        }
                    }
                }

                // Enhanced debugging for call disconnection
                String disconnectedCallId = getCallerIdFromCall(call);
                Log.d("Dialer", "Call disconnected: " + disconnectedCallId);
                Log.d("Dialer", "Call has children: " + (call.getChildren() != null && !call.getChildren().isEmpty()));
                
                // Check all active calls to see if any are conferences
                for (Call activeCall : activeCalls) {
                    if (activeCall.getChildren() != null && !activeCall.getChildren().isEmpty()) {
                        Log.d("Dialer", "Found conference call with " + activeCall.getChildren().size() + " children");
                        for (Call child : activeCall.getChildren()) {
                            String childId = getCallerIdFromCall(child);
                            Log.d("Dialer", "Conference child: " + childId);
                        }
                    }
                }
                
                // Check if the disconnected call is part of a conference and handle it
                if (call.getChildren() != null && !call.getChildren().isEmpty()) {
                    Log.d("Dialer", "This call is part of a conference, handling participant left.");
                    handleConferenceParticipantLeft(call);
                } else {
                    Log.d("Dialer", "This call is NOT part of a conference.");
                    
                    // Additional check: see if this was a child of a conference
                    for (Call activeCall : activeCalls) {
                        if (activeCall.getChildren() != null && activeCall.getChildren().contains(call)) {
                            Log.d("Dialer", "Disconnected call was a child of a conference");
                            // The onChildrenChanged callback should handle this automatically
                            break;
                        }
                    }
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
            
            // Handle when conference participants change
            if (children != null && children.size() > 0) {
                List<Call> others = new ArrayList<>();
                Call twilioCall = null;
                
                // Log all participants for debugging
                StringBuilder participantLog = new StringBuilder("Conference participants: ");
                for (Call child : children) {
                    String id = getCallerIdFromCall(child);
                    participantLog.append(id).append(", ");
                    
                    if (id != null && id.equals(TWILIO_NUMBER)) {
                        twilioCall = child;
                    } else if (id != null && !id.equals(TWILIO_NUMBER)) {
                        others.add(child);
                    }
                }
                Log.d("Dialer", participantLog.toString());
                Log.d("Dialer", "Others count: " + others.size() + ", Twilio present: " + (twilioCall != null));
                
                // If only Twilio is left in the conference (no other participants except user)
                if (others.size() == 0 && twilioCall != null) {
                    Log.d("Dialer", "Only Twilio and user left in conference - ending call");
                    
                    // First hang up the Twilio call specifically
                    try {
                        hangUpCall(twilioCall);
                        Log.d("Dialer", "Twilio call disconnected successfully via onChildrenChanged");
                    } catch (Exception e) {
                        Log.e("Dialer", "Error disconnecting Twilio call via onChildrenChanged: " + e.getMessage());
                    }
                    
                    // Then disconnect the conference
                    try {
                        conference.disconnect();
                        Log.d("Dialer", "Conference disconnected successfully via onChildrenChanged");
                    } catch (Exception e) {
                        Log.e("Dialer", "Error disconnecting conference via onChildrenChanged: " + e.getMessage());
                    }
                    
                    // Finish the outgoing call screen
                    finishOutgoingCallScreen();
                }
            } else if (children == null || children.size() == 0) {
                Log.d("Dialer", "Conference has no children - ending conference");
                try {
                    conference.disconnect();
                    finishOutgoingCallScreen();
                } catch (Exception e) {
                    Log.e("Dialer", "Error disconnecting empty conference: " + e.getMessage());
                }
            }
        }
    };

    // Handle when a conference participant leaves
    private static void handleConferenceParticipantLeft(Call disconnectedCall) {
        Log.d("Dialer", "Called handleConferenceParticipantLeft");

        for (Call conference : activeCalls) {
            if (!conference.getChildren().isEmpty()) {
                List<Call> remaining = conference.getChildren();
                List<Call> others = new ArrayList<>();
                Call twilioCall = null;

                for (Call c : remaining) {
                    String id = getCallerIdFromCall(c);
                    if (id != null && id.equals(TWILIO_NUMBER)) {
                        twilioCall = c;
                    } else if (id != null && !id.equals(TWILIO_NUMBER)) {
                        others.add(c);
                    }
                }
                Log.d("Dialer", "others.size(): " + others.size() + ", twilioCall present: " + (twilioCall != null));

                // If only Twilio is left in the conference (no other participants except user)
                if (others.size() == 0 && twilioCall != null) {
                    Log.d("Dialer", "Only Twilio and self left — ending Twilio call and conference");

                    // First hang up the Twilio call specifically
                    try {
                        hangUpCall(twilioCall);
                        Log.d("Dialer", "Twilio call disconnected successfully");
                    } catch (Exception e) {
                        Log.e("Dialer", "Error disconnecting Twilio call: " + e.getMessage());
                    }

                    // Then disconnect the conference
                    try {
                        conference.disconnect();
                        Log.d("Dialer", "Conference disconnected successfully");
                    } catch (Exception e) {
                        Log.e("Dialer", "Error disconnecting conference: " + e.getMessage());
                    }

                    // Finish the outgoing call screen
                    finishOutgoingCallScreen();
                } else if (others.size() > 0) {
                    // If there are still other participants, conference continues
                    Log.d("Dialer", "Other participants still in conference, continuing");
                } else {
                    // If there's no Twilio and no others, something went wrong
                    Log.d("Dialer", "No participants left in conference, ending call");
                    conference.disconnect();
                    finishOutgoingCallScreen();
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
