import { Alert, NativeModules } from 'react-native';

const { DialerRoleManager } = NativeModules;

export const requestDialerRole = () => {
  DialerRoleManager.requestDialerRole(
    // Pass the current activity context (this) and a callback
    (message: any) => {
      console.log(message); // Log the callback message
      Alert.alert(message); // Optionally show an alert to the user
    }
  );
};
