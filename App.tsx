import {NavigationContainer} from '@react-navigation/native';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import ContactsList from './components/ContactsList';
import Icon from 'react-native-vector-icons/Ionicons'; // Import icons
import React from 'react';
import HomeScreen from './components/HomeScreen';

const Tab = createBottomTabNavigator();

const DialerIcon = ({color, size}: {color: string; size: number}) => (
  <Icon name="call" color={color} size={size} />
);

const ContactsIcon = ({color, size}: {color: string; size: number}) => (
  <Icon name="person" color={color} size={size} />
);

export default function App() {
  return (
    <NavigationContainer>
      <Tab.Navigator
        id={undefined}
        initialRouteName="Dialer"
        screenOptions={{
          headerShown: false,
          tabBarStyle: {
            height: 65,
          },
          tabBarIconStyle: {
            width: 50,
            height: 30,
            // backgroundColor: '#e0ecfd',
            borderRadius: 8,
            marginBottom: 4,
          },
          tabBarLabelStyle: {
            fontSize: 12,
            fontWeight: '500',
          },
        }}>
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
            tabBarLabel: 'Contacts', // Remove the title from the tab
          }}
        />
      </Tab.Navigator>
    </NavigationContainer>
  );
}
