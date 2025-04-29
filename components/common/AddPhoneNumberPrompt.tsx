import React, {useRef} from 'react';
import {View, Text, Button, Alert, StyleSheet} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import PhoneInput from 'react-native-phone-input';

interface PhoneNumberPromptProps {
  onSave: (phoneNumber: string) => void;
}

const PhoneNumberPrompt: React.FC<PhoneNumberPromptProps> = ({onSave}) => {
  const phoneInputRef = useRef<PhoneInput>(null);

  const handleSave = async () => {
    const phoneNumber = phoneInputRef.current?.getValue();
    if (phoneNumber && phoneNumber.length > 0) {
      await AsyncStorage.setItem('client_id', phoneNumber);
      onSave(phoneNumber);
    } else {
      Alert.alert(
        'Error',
        'Please enter a valid phone number with country code.',
      );
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Enter Your Phone Number</Text>
      <PhoneInput
        ref={phoneInputRef}
        initialCountry="us"
        textProps={{placeholder: '+1234567890'}}
        style={styles.phoneInput}
      />
      <View style={styles.buttonContainer}>
        <Button title="Save" onPress={handleSave} color="#6200EE" />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    color: '#333',
  },
  phoneInput: {
    width: '100%',
    height: 50,
    marginBottom: 20,
    backgroundColor: '#fff',
    borderRadius: 5,
    paddingHorizontal: 10,
  },
  buttonContainer: {
    width: '100%',
    marginTop: 20,
  },
});

export default PhoneNumberPrompt;
