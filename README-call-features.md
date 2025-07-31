# Ai-fraud-call-detector

## Call History & Missed Call Features

This app includes a complete call management system that tracks outgoing, incoming, and missed calls.

### Features

1. **Recent Calls Screen**

   - View all call history in chronological order
   - See call type (incoming, outgoing, or missed) with distinctive icons
   - Display call duration for completed calls
   - Call back directly from the call history

2. **Missed Call Detection**

   - Automatic detection of missed calls
   - Push notifications for missed calls
   - "Call Back" action directly from notifications

3. **Call Tracking**
   - Records outgoing calls made from the app's dialer
   - Tracks incoming calls whether answered or missed
   - Stores call metadata (number, contact name, timestamp, duration)

### How to Test the Features

#### Recent Calls Screen

1. Launch the app
2. Navigate to the "Recents" tab at the bottom of the screen
3. View your call history (if no calls are recorded yet, make some calls first)
4. Tap on any entry to call back that number

#### Outgoing Calls

1. Go to the "Dial" tab
2. Enter a phone number
3. Press the call button
4. Once the call is complete, check the Recents tab to see the outgoing call record

#### Missed Call Detection

1. Have someone call your device
2. Either:
   - Don't answer until the call stops ringing
   - Decline the call using the reject button
3. You should receive a missed call notification
4. The call should appear in the Recents tab with a missed call icon

#### Call Backs

1. From a missed call notification:
   - Tap on the notification
   - Choose "Call Back" from the notification actions
2. From the Recent Calls screen:
   - Tap on any call history entry to call back that number

### Technical Implementation

The app implements:

- React Native UI components for call display and interaction
- Native Android components for call detection and management
- AsyncStorage for persistent call history
- Notifee for push notifications
- Custom hooks for call state management

For detailed technical documentation, see:

- `docs/call_history_implementation.md`
- `docs/missed_call_implementation.md`
