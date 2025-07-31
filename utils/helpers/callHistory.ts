import AsyncStorage from '@react-native-async-storage/async-storage';
// Call types
export type CallType = 'incoming' | 'outgoing' | 'missed';

export interface CallRecord {
  id: string;
  phoneNumber: string;
  contactName?: string;
  timestamp: number; // Unix timestamp
  duration: number; // Call duration in seconds
  type: CallType;
}

// Maximum number of call history entries to keep
const MAX_CALL_HISTORY = 100;

/**
 * Add a new call to the call history
 * @param phoneNumber The phone number of the call
 * @param contactName Optional contact name if available
 * @param type The type of call (incoming, outgoing, missed)
 * @param duration Call duration in seconds
 */
export const addCallToHistory = async (
  phoneNumber: string,
  type: CallType,
  duration: number = 0,
  contactName?: string,
): Promise<void> => {
  try {
    // Generate a unique ID for the call
    const id = `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    // Create the call record
    const newCall: CallRecord = {
      id,
      phoneNumber,
      contactName,
      timestamp: Date.now(),
      duration,
      type,
    };

    // Get existing call history
    const historyJson = await AsyncStorage.getItem('call_history');
    let history: CallRecord[] = historyJson ? JSON.parse(historyJson) : [];

    // Add the new call to the beginning of the array
    history.unshift(newCall);

    // Limit the history size
    if (history.length > MAX_CALL_HISTORY) {
      history = history.slice(0, MAX_CALL_HISTORY);
    }

    // Save back to AsyncStorage
    await AsyncStorage.setItem('call_history', JSON.stringify(history));
  } catch (error) {
    console.error('Error adding call to history:', error);
  }
};

/**
 * Get the complete call history
 * @returns Array of call records
 */
export const getCallHistory = async (): Promise<CallRecord[]> => {
  try {
    const historyJson = await AsyncStorage.getItem('call_history');
    return historyJson ? JSON.parse(historyJson) : [];
  } catch (error) {
    console.error('Error getting call history:', error);
    return [];
  }
};

/**
 * Clear all call history
 */
export const clearCallHistory = async (): Promise<void> => {
  try {
    await AsyncStorage.removeItem('call_history');
  } catch (error) {
    console.error('Error clearing call history:', error);
  }
};

/**
 * Delete a specific call from history by ID
 * @param callId The ID of the call to delete
 */
export const deleteCallFromHistory = async (callId: string): Promise<void> => {
  try {
    const historyJson = await AsyncStorage.getItem('call_history');
    if (historyJson) {
      let history: CallRecord[] = JSON.parse(historyJson);
      history = history.filter(call => call.id !== callId);
      await AsyncStorage.setItem('call_history', JSON.stringify(history));
    }
  } catch (error) {
    console.error('Error deleting call from history:', error);
  }
};
