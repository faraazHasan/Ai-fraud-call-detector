import {NativeModules, Platform} from 'react-native';
// Add console log to see what modules are available
console.log('Available Native Modules:', Object.keys(NativeModules));
const {CallHistoryModule} = NativeModules;
console.log('CallHistoryModule loaded:', CallHistoryModule ? 'Yes' : 'No');

export interface CallLogEntry {
  id: string;
  phoneNumber: string;
  contactName: string;
  timestamp: number;
  duration: number;
  type: 'incoming' | 'outgoing' | 'missed' | 'rejected' | 'unknown';
  isNew: boolean;
}

/**
 * Get recent calls from the device's call log
 * @param limit Maximum number of entries to return
 * @returns Promise with array of call log entries
 */
export const getRecentCalls = async (limit = 50): Promise<CallLogEntry[]> => {
  if (Platform.OS !== 'android') {
    console.log('Call history is only available on Android');
    return [];
  }

  try {
    if (!CallHistoryModule) {
      console.error('CallHistoryModule is not available');
      // Print all available modules to help with debugging
      console.log('Available modules:', Object.keys(NativeModules));
      return [];
    }

    console.log('Calling native getRecentCalls with limit:', limit);
    const calls = await CallHistoryModule.getRecentCalls(limit);
    console.log('Call history retrieved:', calls ? 'Success' : 'Failed');
    return calls || [];
  } catch (error) {
    console.error('Error getting recent calls:', error);
    console.error(
      'Error details:',
      error instanceof Error ? error.message : String(error),
    );
    return [];
  }
};

/**
 * Mark a call as read in the device's call log
 * @param callId The ID of the call to mark as read
 * @returns Promise with success status
 */
export const markCallAsRead = async (callId: string): Promise<boolean> => {
  if (Platform.OS !== 'android') {
    console.log('Marking calls as read is only available on Android');
    return false;
  }

  try {
    if (!CallHistoryModule) {
      console.error('CallHistoryModule is not available');
      return false;
    }

    console.log('Marking call as read:', callId);
    const result = await CallHistoryModule.markCallAsRead(callId);
    console.log('Mark call as read result:', result);
    return result;
  } catch (error) {
    console.error('Error marking call as read:', error);
    console.error(
      'Error details:',
      error instanceof Error ? error.message : String(error),
    );
    return false;
  }
};

/**
 * Synchronize the in-app call history with the device's call log
 * @returns Promise with array of call log entries
 */
export const syncCallHistory = async (): Promise<CallLogEntry[]> => {
  if (Platform.OS !== 'android') {
    console.log('Call history sync is only available on Android');
    return [];
  }

  try {
    if (!CallHistoryModule) {
      console.error('CallHistoryModule is not available');
      return [];
    }

    const calls = await CallHistoryModule.syncCallHistory();
    return calls || [];
  } catch (error) {
    console.error('Error syncing call history:', error);
    return [];
  }
};
