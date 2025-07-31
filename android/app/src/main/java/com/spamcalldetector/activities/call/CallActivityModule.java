package com.spamcalldetector.activities.call;

import android.telecom.Call;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.spamcalldetector.helpers.CallManager;
import com.spamcalldetector.helpers.ContactsHelper;
import com.spamcalldetector.activities.dialer.DialerModule;
import com.spamcalldetector.services.CallService;
import android.app.Activity;
import android.net.Uri;
import java.util.List;
import java.util.ArrayList;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class CallActivityModule extends ReactContextBaseJavaModule {

    public static boolean isMuted, isSpeakerOn, isCallOnHold, isRecordingCall;
    private static final String TWILIO_NUMBER = "+18452998019"; // Your Twilio number

    private static boolean conferenceCreated = false; // Flag to prevent multiple conference creations
    DialerModule dialer;

    private static ReactApplicationContext reactContext;

    public CallActivityModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        dialer = new DialerModule(reactContext);
    }

    @Override
    public String getName() {
        return "CallActivityModule";
    }

    // Required for NativeEventEmitter to work correctly
    public void addListener(String eventName) {
        // No-op
    }

    public void removeListeners(double count) {
        // No-op
    }

    // Helper method to get caller ID from a call
    private String getCallerIdFromCall(Call call) {
        if (call == null || call.getDetails() == null)
            return null;
        Uri handle = call.getDetails().getHandle();
        return (handle != null) ? handle.getSchemeSpecificPart() : null;
    }

    @ReactMethod
    public void answerCall(Promise promise) {
        try {
            Log.d("Dialer", "Answer Call triggered");
            Call call = CallManager.getLatestActiveOrRingingCall();
            if (call != null) {
                CallManager.answerCall(call);
                promise.resolve("answered");
            } else {
                Log.e("Dialer", "No active or ringing call found.");
                promise.reject("NO_CALL", "No call available to answer.");
            }
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void toggleRecording(Promise promise) {
        try {
            Call twilioCall = CallManager.findCallByCallerId(TWILIO_NUMBER);
            if (twilioCall != null) {
                boolean hungUp = false;
                // Check if Twilio is in a conference
                for (Call conference : CallManager.getActiveCalls()) {
                    if (!conference.getChildren().isEmpty()) {
                        List<Call> children = conference.getChildren();
                        for (Call child : children) {
                            if (getCallerIdFromCall(child).equals(TWILIO_NUMBER)) {
                                // Twilio is in the conference, remove it
                                Log.d("Dialer", "Twilio is in the conference, disconnecting.");
                                CallManager.hangUpCall(child); // Disconnect the Twilio call
                                hungUp = true;
                                break;
                            }
                        }
                    }
                    if (hungUp) {
                        break; // Exit the outer loop if the child call was hung up
                    }
                }

                if (!hungUp) {
                    // If Twilio is not in the conference, just end the call
                    CallManager.hangUpCall(twilioCall);
                }
                promise.resolve("Recording stopped");
            } else {
                dialer.dialNumber(TWILIO_NUMBER);
                promise.resolve("Recording started");
            }
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void endCall(Promise promise) {
        try {
            boolean isConference = false;
            boolean isTwilioOnly = false;

            // Check if the call is part of a conference
            for (Call call : CallManager.getActiveCalls()) {
                if (!call.getChildren().isEmpty()) {
                    isConference = true;
                    List<Call> remainingCalls = call.getChildren();
                    // Check if only the Twilio call and user are left in the conference
                    List<Call> otherParticipants = new ArrayList<>();
                    for (Call c : remainingCalls) {
                        String id = getCallerIdFromCall(c);
                        if (id != null && !id.equals(TWILIO_NUMBER)) {
                            otherParticipants.add(c);
                        }
                    }
                    // If there are no other participants, Twilio is the only one left
                    isTwilioOnly = (otherParticipants.size() == 0);
                }
            }

            if (isConference && isTwilioOnly) {
                // If it's a conference with only Twilio left, end all calls and finish the
                // outgoing screen
                Log.d("Dialer", "Conference with only Twilio remaining. Ending all calls.");
                for (Call call : CallManager.getActiveCalls()) {
                    CallManager.hangUpCall(call); // Disconnect all calls
                }

                // Finish the OutgoingCallActivity
                CallManager.finishOutgoingCallScreen();
            } else {
                // If the call is not part of a conference or there are more participants, just
                // disconnect it
                for (Call call : CallManager.getActiveCalls()) {
                    CallManager.hangUpCall(call); // Disconnect the call
                    Log.d("Dialer", "Call disconnected: " + call.toString());
                }

                // Finish the OutgoingCallActivity if it exists
                CallManager.finishOutgoingCallScreen();
            }

            promise.resolve("All calls ended");
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void muteCall(boolean mute, Promise promise) {
        try {
            isMuted = mute;
            CallManager.muteCall(mute);
            WritableMap params = Arguments.createMap();
            params.putBoolean("isMuted", isMuted);
            promise.resolve("Mute status updated to: " + mute);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void toggleSpeaker(boolean enable, Promise promise) {
        try {
            isSpeakerOn = enable;
            CallManager.speakerCall(enable);
            WritableMap params = Arguments.createMap();
            params.putBoolean("isSpeakerOn", isSpeakerOn);
            promise.resolve("Speaker status updated to: " + enable);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void holdCall(boolean hold, Promise promise) {
        try {
            isCallOnHold = hold;
            Call call = CallManager.getLatestActiveOrRingingCall();
            if (call != null) {
                if (hold) {
                    CallManager.holdCall(call);
                } else {
                    CallManager.unholdCall(call);
                }
                promise.resolve("Call hold status updated to: " + hold);
            } else {
                promise.reject("NO_CALL", "No call to hold/unhold.");
            }
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void getCallerDetails(Promise promise) {
        try {
            Call call = CallManager.getLatestActiveOrRingingCall();
            WritableMap result = Arguments.createMap();

            if (call != null) {
                List<Call> childCalls = call.getChildren();

                // If this is a conference call with multiple participants
                if (childCalls != null && !childCalls.isEmpty()) {
                    WritableArray numbersArray = Arguments.createArray();

                    for (Call child : childCalls) {
                        Uri handle = child.getDetails().getHandle();
                        if (handle != null) {
                            String number = handle.getSchemeSpecificPart();
                            String name = ContactsHelper.getContactNameByPhoneNumber(number,
                                    getReactApplicationContext());

                            WritableMap participant = Arguments.createMap();
                            participant.putString("phoneNumber", number);
                            participant.putString("callerName", name != null ? name : number);
                            numbersArray.pushMap(participant);
                        }
                    }

                    result.putString("type", "conference");
                    result.putArray("participants", numbersArray);
                } else {
                    // 🟢 Regular single call
                    Uri handle = call.getDetails().getHandle();
                    if (handle != null) {
                        String number = handle.getSchemeSpecificPart();
                        String name = ContactsHelper.getContactNameByPhoneNumber(number,
                                getReactApplicationContext());

                        result.putString("type", "single");
                        result.putString("phoneNumber", number);
                        result.putString("callerName", name != null ? name : number);
                    } else {
                        result.putString("type", "unknown");
                        result.putString("phoneNumber", "unknown");
                        result.putString("callerName", "Unknown Caller");
                    }
                }

                promise.resolve(result);
            } else {
                promise.reject("NO_CALL", "No active call.");
            }
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    public static void disconnectCall(Call call) {
        CallManager.hangUpCall(call);
    }

    public static void emitCallTiming() {
        try {
            Call call = CallManager.getLatestActiveOrRingingCall();

            if (call != null && CallManager.inCallService instanceof CallService) {
                CallService callService = (CallService) CallManager.inCallService;
                String callStatus = callService.getCallStatus();

                WritableMap params = Arguments.createMap();

                if ("Calling...".equals(callStatus) || "Connecting...".equals(callStatus)) {
                    params.putString("callTime", callStatus);
                } else {
                    long seconds = callService.getCallDuration();
                    long min = seconds / 60;
                    long sec = seconds % 60;
                    String formattedTime = String.format("%02d:%02d", min, sec);
                    params.putString("callTime", formattedTime);
                }

                if (reactContext != null) {
                    reactContext
                            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("CallTimingUpdate", params);
                }

            } else {
                // No active call or service, send null or fallback
                WritableMap params = Arguments.createMap();
                params.putString("callTime", null);

                if (reactContext != null) {
                    reactContext
                            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("CallTimingUpdate", params);
                }
            }
        } catch (Exception e) {
            WritableMap params = Arguments.createMap();
            params.putString("callTime", null);

            if (reactContext != null) {
                reactContext
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("CallTimingUpdate", params);
            }
        }
    }

}
