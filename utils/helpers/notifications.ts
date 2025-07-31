import notifee, {
  AndroidImportance,
  AndroidStyle,
  EventType,
  AndroidCategory,
  NotificationSettings,
} from '@notifee/react-native';
import {Platform} from 'react-native';
import {addCallToHistory} from './callHistory';

// Create notification channel for missed calls (Android only)
export const createNotificationChannel = async (): Promise<void> => {
  if (Platform.OS === 'android') {
    await notifee.createChannel({
      id: 'missed-calls',
      name: 'Missed Calls',
      importance: AndroidImportance.HIGH,
      vibration: true,
      sound: 'default',
    });
  }
};

// Check and request notification permissions
export const checkNotificationPermissions = async (): Promise<boolean> => {
  const settings: NotificationSettings = await notifee.requestPermission();
  return settings.authorizationStatus >= 1; // AUTHORIZED or PROVISIONAL
};

/**
 * Display a missed call notification
 * @param phoneNumber The phone number that called
 * @param contactName The name of the contact if available
 */
export const showMissedCallNotification = async (
  phoneNumber: string,
  contactName?: string,
): Promise<void> => {
  try {
    // Ensure permissions and channel are set up
    await checkNotificationPermissions();
    await createNotificationChannel();

    const displayName = contactName || phoneNumber;
    const timestamp = Date.now();
    const formattedTime = new Date().toLocaleTimeString();

    // Record this missed call in our call history
    await addCallToHistory(phoneNumber, 'missed', 0, contactName);

    // Create the notification with a unique ID to prevent overrides
    const notificationId = `missed-${phoneNumber}-${timestamp}`;

    await notifee.displayNotification({
      id: notificationId,
      title: 'Missed Call',
      body: `You missed a call from ${displayName} at ${formattedTime}`,
      android: {
        channelId: 'missed-calls',
        smallIcon: 'ic_notification', // Make sure this icon exists in the Android project
        importance: AndroidImportance.HIGH,
        style: {
          type: AndroidStyle.BIGTEXT,
          text: `You missed a call from ${displayName} at ${formattedTime}`,
        },
        category: AndroidCategory.CALL,
        pressAction: {
          id: 'call-back',
          launchActivity: 'default',
        },
        actions: [
          {
            title: 'Call Back',
            pressAction: {
              id: 'call-back',
            },
          },
          {
            title: 'Dismiss',
            pressAction: {
              id: 'dismiss',
            },
          },
        ],
        // Auto-cancel system notifications for missed calls
        autoCancel: true,
        // Prevent system notifications from showing
        fullScreenAction: {
          id: 'full-screen',
          launchActivity: 'default',
        },
      },
      ios: {
        categoryId: 'missed-call',
        sound: 'default',
        critical: true,
        interruptionLevel: 'timeSensitive',
      },
      data: {
        phoneNumber,
        contactName: contactName || '',
        timestamp: timestamp.toString(),
      },
    });
  } catch (error) {
    console.error('Error showing missed call notification:', error);
  }
};

// Set up notification event listeners
export const setupNotificationListeners = (): (() => void) => {
  const unsubscribe = notifee.onForegroundEvent(({type, detail}) => {
    switch (type) {
      case EventType.PRESS:
        // User pressed the notification
        if (detail.notification?.data?.phoneNumber) {
          // Import the make_call function
          const {make_call} = require('../native_modules/dialer_module');
          make_call(detail.notification.data.phoneNumber);
        }
        break;

      case EventType.ACTION_PRESS:
        // User pressed an action button
        if (
          detail.pressAction?.id === 'call-back' &&
          detail.notification?.data?.phoneNumber
        ) {
          // Import the make_call function
          const {make_call} = require('../native_modules/dialer_module');
          make_call(detail.notification.data.phoneNumber);
        }
        break;
    }
  });

  return unsubscribe;
};

// Initialize notification system
export const initializeNotifications = async (): Promise<void> => {
  await createNotificationChannel();
  setupNotificationListeners();
};
