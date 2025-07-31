import {useEffect} from 'react';
import {NativeModules, NativeEventEmitter, AppState} from 'react-native';
import {addCallToHistory} from '../utils/helpers/callHistory';
import {showMissedCallNotification} from '../utils/helpers/notifications';

const {CallActivityModule} = NativeModules;

/**
 * Custom hook for subscribing to missed calls
 */
const useMissedCallListener = () => {
  // Only set up if we're on a device that supports CallActivityModule
  useEffect(() => {
    if (!CallActivityModule) {
      console.log(
        'CallActivityModule is not available for missed call detection',
      );
      return;
    }

    console.log('Setting up missed call listener with CallActivityModule');

    // Set up event emitter
    const eventEmitter = new NativeEventEmitter(CallActivityModule);

    // Subscribe to missed call events
    const subscription = eventEmitter.addListener('onMissedCall', event => {
      console.log('Received missed call event:', event);
      const {phoneNumber, contactName, timestamp} = event;

      // Log the missed call
      console.log(
        `Missed call from ${contactName || phoneNumber} at ${new Date(
          timestamp,
        ).toLocaleTimeString()}`,
      );

      // Use a debounced version of addCallToHistory to prevent duplicates
      // if we get multiple events for the same missed call
      debounceAddCallToHistory(phoneNumber, contactName, timestamp);

      // Show notification only if app is not in foreground
      if (AppState.currentState !== 'active') {
        showMissedCallNotification(phoneNumber, contactName);
      }
    });

    // Keep track of processed calls to avoid duplicates
    const processedCalls = new Map<string, number>();

    // Debounce function to prevent duplicate call history entries
    const debounceAddCallToHistory = (
      phoneNumber: string,
      contactName: string | undefined,
      timestamp: number,
    ) => {
      // Create a unique key for this call
      const callKey = `${phoneNumber}-${timestamp}`;

      // If we've already processed this call within the last 5 seconds, ignore it
      const now = Date.now();
      if (processedCalls.has(callKey)) {
        const lastProcessed = processedCalls.get(callKey);
        if (lastProcessed && now - lastProcessed < 5000) {
          // 5 seconds
          console.log('Ignoring duplicate missed call event');
          return;
        }
      }

      // Mark this call as processed
      processedCalls.set(callKey, now);

      // Clean up old entries
      for (const [key, time] of processedCalls.entries()) {
        if (now - time > 60000) {
          // Remove entries older than 1 minute
          processedCalls.delete(key);
        }
      }

      // Add to call history
      addCallToHistory(phoneNumber, 'missed', 0, contactName);
    };

    // Clean up the subscription on unmount
    return () => {
      subscription.remove();
    };
  }, []);
};

export default useMissedCallListener;
