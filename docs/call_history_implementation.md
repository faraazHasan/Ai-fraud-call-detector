# Call History and Missed Call Notifications

This document explains the implementation details of the recent calls screen and missed call notifications in the app.

## Components and Files

1. **RecentCalls.tsx**: A new component that displays call history, including incoming, outgoing, and missed calls.
2. **utils/helpers/callHistory.ts**: Utility functions for managing call history in AsyncStorage.
3. **utils/helpers/notifications.ts**: Utility functions for creating and showing missed call notifications.
4. **hooks/useCallHistory.ts**: A custom hook that tracks call states and records calls in history.

## Features Implemented

### 1. Recent Calls Screen

- Displays a chronological list of all calls (incoming, outgoing, missed)
- Shows call type with appropriate icons
- Shows call duration for completed calls
- Tapping on an entry allows calling back
- Automatically refreshes when screen comes into focus

### 2. Call History Recording

- Records all calls automatically
- Stores call history in AsyncStorage
- Captures:
  - Phone number
  - Contact name (if available)
  - Call type (incoming, outgoing, missed)
  - Call duration
  - Timestamp

### 3. Missed Call Notifications

- Shows notifications for missed calls
- Includes caller name if available
- Provides a "Call Back" action button
- Uses the @notifee/react-native library

## How to Use

### Viewing Recent Calls

Navigate to the "Recents" tab in the bottom navigation bar to view your recent calls history.

### Making Calls

When making calls from the dialer or contacts, calls are automatically logged in your call history.

### Missed Calls

If you miss a call, you'll receive a notification with the option to call back. The call will also appear in your recent calls list.

## Technical Implementation

### Call Detection

The app uses a native module to detect call states:

- RINGING: Incoming call is ringing
- OFFHOOK: Call is in progress
- DISCONNECTED: Call has ended

### Data Storage

Call history is stored in the device's AsyncStorage under the key 'call_history'.

### Notifications

Notifications are created using the @notifee/react-native library:

- Custom notification channel for Android
- Supports direct callback from notification
- High priority for timely alerts

## Future Improvements

- Group calls by day in the UI
- Filter options (missed calls only, etc.)
- Delete individual or all call history entries
- Call blocking integration with spam detection
