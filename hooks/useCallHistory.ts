import {useState, useEffect} from 'react';
import {NativeModules, NativeEventEmitter} from 'react-native';
import {addCallToHistory} from '../utils/helpers/callHistory';
import {showMissedCallNotification} from '../utils/helpers/notifications';

const {CallActivityModule} = NativeModules;

// Call state constants
export enum CallState {
  IDLE = 'IDLE',
  RINGING = 'RINGING',
  OFFHOOK = 'OFFHOOK',
  DISCONNECTED = 'DISCONNECTED',
}

export interface CallDetails {
  phoneNumber: string;
  contactName?: string;
  state: CallState;
  startTime?: number;
  endTime?: number;
  callType?: 'incoming' | 'outgoing' | 'missed';
}

/**
 * Custom hook to track call states and record call history
 */
const useCallHistory = () => {
  const [currentCall, setCurrentCall] = useState<CallDetails | null>(null);

  useEffect(() => {
    // Only set up native event listeners if the CallActivityModule exists
    if (CallActivityModule) {
      console.log('CallActivityModule exists, setting up event listeners');
      const callEventEmitter = new NativeEventEmitter(CallActivityModule);

      // Subscribe to call state changes
      const callStateSubscription = callEventEmitter.addListener(
        'onCallStateChanged',
        (event: {state: string; phoneNumber: string; contactName?: string}) => {
          console.log('Call state changed:', event);
          const {state, phoneNumber, contactName} = event;

          // Handle incoming call
          if (state === CallState.RINGING) {
            setCurrentCall({
              phoneNumber,
              contactName,
              state: CallState.RINGING,
              startTime: Date.now(),
              callType: 'incoming',
            });
          }
          // Handle call answered
          else if (state === CallState.OFFHOOK) {
            setCurrentCall(prevCall => {
              if (!prevCall) {
                // This is an outgoing call if there was no ringing state
                return {
                  phoneNumber,
                  contactName,
                  state: CallState.OFFHOOK,
                  startTime: Date.now(),
                  callType: 'outgoing',
                };
              } else {
                // This is an incoming call that was answered
                return {
                  ...prevCall,
                  state: CallState.OFFHOOK,
                };
              }
            });
          }
          // Handle call ended
          else if (state === CallState.DISCONNECTED) {
            setCurrentCall(prevCall => {
              if (!prevCall) {
                return null;
              }

              const endTime = Date.now();
              const duration = prevCall.startTime
                ? Math.floor((endTime - prevCall.startTime) / 1000)
                : 0;

              if (
                prevCall.state === CallState.RINGING &&
                prevCall.callType === 'incoming'
              ) {
                // This was a missed call (ringing but never answered)
                showMissedCallNotification(
                  prevCall.phoneNumber,
                  prevCall.contactName,
                );
                addCallToHistory(
                  prevCall.phoneNumber,
                  'missed',
                  0,
                  prevCall.contactName,
                );
              } else {
                // This was a completed call (either incoming or outgoing)
                addCallToHistory(
                  prevCall.phoneNumber,
                  prevCall.callType || 'incoming',
                  duration,
                  prevCall.contactName,
                );
              }

              return null; // Reset current call
            });
          }
        },
      );

      // Clean up on unmount
      return () => {
        callStateSubscription.remove();
      };
    }
  }, []);

  // Manually record an outgoing call (for when using the app's dialer)
  const recordOutgoingCall = (phoneNumber: string, contactName?: string) => {
    addCallToHistory(phoneNumber, 'outgoing', 0, contactName);
  };

  return {currentCall, recordOutgoingCall};
};

export default useCallHistory;
