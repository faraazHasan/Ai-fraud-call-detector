# Missed Call Detection Implementation

This document describes how missed calls are detected and handled in the Ai-fraud-call-detector app.

## Overview

The app detects missed calls through native Android integration and makes this information available to the React Native layer. This enables:

1. Real-time missed call notifications
2. Automatic call history recording
3. Integration with the Recent Calls screen

## Technical Implementation

### Android Implementation

1. **CallManager.java**

   - Detects call state changes through the Android Telecom API
   - Identifies missed calls by analyzing disconnect causes
   - Broadcasts missed call events with caller information

2. **IncomingCallActivity.java**

   - Handles user-rejected calls
   - Marks calls as missed when explicitly declined
   - Ensures consistency in call history records

3. **CallActivityModule.java**
   - Provides a bridge between native Android and React Native
   - Exposes methods to register missed calls from JS
   - Creates event emitters for real-time notifications

### React Native Implementation

1. **useMissedCallListener.ts**

   - Hook that listens for missed call events from native layer
   - Records missed calls in call history
   - Triggers notifications when calls are missed

2. **App.tsx**
   - Initializes the missed call listener at app startup
   - Ensures the listener runs throughout the app lifecycle

## Data Flow

1. A call is received on the device
2. If the call is:
   - Not answered and caller hangs up
   - Explicitly rejected by the user
   - The system detects it as a missed call
3. CallManager broadcasts a missed call event
4. useMissedCallListener captures the event in React Native
5. Call is added to call history with type 'missed'
6. A notification is shown to the user
7. The call appears in the Recent Calls screen

## Testing Missed Call Detection

### Real Call Testing

1. Make an incoming call to the device
2. Either:
   - Wait for the call to stop ringing without answering
   - Decline the call using the reject button
3. Observe that:
   - A notification appears for the missed call
   - The call is logged in Recent Calls as missed
   - The call back functionality works from both notification and recent call list

### Using Test Mode

For development testing without needing real phone calls:

1. Go to the "Test" tab in the app
2. Press the "Test Missed Call" button
3. This will simulate a missed call event
4. Verify that:
   - A notification appears for the test missed call
   - The call appears in the Recent Calls screen
   - You can interact with the call entry normally
