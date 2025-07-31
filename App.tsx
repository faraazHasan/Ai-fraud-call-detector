import React, {useEffect, useRef} from 'react';
import {NavigationContainer} from '@react-navigation/native';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import {Platform, DeviceEventEmitter, NativeModules, Alert} from 'react-native';
import {
  SafeAreaProvider,
  useSafeAreaInsets,
} from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/Ionicons';
import HomeScreen from './components/HomeScreen';
import ContactsList from './components/ContactsList';
import RecentCalls from './components/RecentCalls';
import {initializeNotifications} from './utils/helpers/notifications';
import useMissedCallListener from './hooks/useMissedCallListener';
import SettingsScreen from './components/SettingsScreen';
import PhoneNumberSettingsScreen from './components/settings/SettingsScreen';

const Tab = createBottomTabNavigator();

const DialerIcon = ({color, size}: {color: string; size: number}) => (
  <Icon name="call" color={color} size={size} />
);

const ContactsIcon = ({color, size}: {color: string; size: number}) => (
  <Icon name="person" color={color} size={size} />
);

const RecentCallsIcon = ({color, size}: {color: string; size: number}) => (
  <Icon name="time" color={color} size={size} />
);

const SettingsIcon = ({color, size}: {color: string; size: number}) => (
  <Icon name="settings" color={color} size={size} />
);

export default function App() {
  // Initialize notifications when app loads
  useEffect(() => {
    initializeNotifications();
  }, []);

  // Use the missed call listener hook to track missed calls
  useMissedCallListener();

  return (
    <SafeAreaProvider>
      <AppContent />
    </SafeAreaProvider>
  );
}

function AppContent() {
  const insets = useSafeAreaInsets();
  const navigationRef = useRef<any>(null);

  useEffect(() => {
    // Handle navigation from notification
    const handleNotificationNavigation = () => {
      console.log('App: Received NAVIGATE_TO_RECENT_CALLS event');
      if (navigationRef.current) {
        console.log('App: Navigating to RecentCalls screen');
        navigationRef.current.navigate('RecentCalls');
      } else {
        console.warn('App: Navigation ref not available');
      }
    };

    // Listen for navigation events from native side
    const subscription1 = DeviceEventEmitter.addListener(
      'NAVIGATE_TO_RECENT_CALLS',
      handleNotificationNavigation
    );
    
    // Also listen for navigation events from MissedCallModule
    const subscription2 = DeviceEventEmitter.addListener(
      'onNavigateToScreen',
      (data: any) => {
        console.log('App: Received onNavigateToScreen event:', data);
        if (data && data.screen === 'RecentCalls') {
          handleNotificationNavigation();
        }
      }
    );

    // Check if app was opened from notification
    const checkInitialNavigation = async () => {
      try {
        if (NativeModules.MissedCallModule) {
          // You can add a method to check if app was opened from notification
          // For now, we'll handle it through the event listener
        }
      } catch (error) {
        console.log('Error checking initial navigation:', error);
      }
    };

    checkInitialNavigation();

    return () => {
      subscription1.remove();
      subscription2.remove();
    };
  }, []);

  return (
    <NavigationContainer ref={navigationRef}>
      <Tab.Navigator
        id={undefined}
        initialRouteName="Dialer"
        screenOptions={{
          headerShown: false,
          tabBarStyle: {
            height: Platform.OS === 'ios' ? 90 + insets.bottom : 80,
            paddingTop: 8,
            paddingBottom: Platform.OS === 'ios' ? 35 + insets.bottom : 25,
            borderTopWidth: 2,
            borderTopColor: '#e0e0e0',
            backgroundColor: 'white',
            // Enhanced shadow for better separation
            shadowColor: '#000',
            shadowOffset: {width: 0, height: -2},
            shadowOpacity: 0.15,
            shadowRadius: 6,
            elevation: 10,
          },
          tabBarIconStyle: {
            width: 40,
            height: 26,
            borderRadius: 8,
            marginBottom: 3,
            marginTop: Platform.OS === 'ios' ? 12 : 8,
          },
          tabBarLabelStyle: {
            fontSize: 13,
            fontWeight: '600',
            marginBottom: Platform.OS === 'ios' ? 12 : 8,
          },
          tabBarActiveTintColor: '#007BFF',
          tabBarInactiveTintColor: '#888',
        }}>
        <Tab.Screen
          name="RecentCalls"
          component={RecentCalls}
          options={{
            tabBarIcon: RecentCallsIcon,
            tabBarLabel: 'Recents',
          }}
        />
        <Tab.Screen
          name="Dialer"
          component={HomeScreen}
          options={{
            tabBarIcon: DialerIcon,
            tabBarLabel: 'Dial',
          }}
        />
        <Tab.Screen
          name="Contacts"
          component={ContactsList}
          options={{
            tabBarIcon: ContactsIcon,
            tabBarLabel: 'Contacts',
          }}
        />
        <Tab.Screen
          name="Settings"
          component={SettingsScreen}
          options={{
            tabBarIcon: ({color, size}) => (
              <SettingsIcon color={color} size={size} />
            ),
            headerShown: false,
          }}
        />
        <Tab.Screen
          name="Phone Number Settings"
          component={PhoneNumberSettingsScreen}
          options={{
            tabBarIcon: ({color, size}) => (
              <SettingsIcon color={color} size={size} />
            ),
            headerShown: false,
          }}
        />
      </Tab.Navigator>
    </NavigationContainer>
  );
}
