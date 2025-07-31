import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, Button, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import { usePhoneNumber } from '../../hooks/usePhoneNumber';

interface PhoneNumberSettingsProps {
  onSuccess?: () => void;
}

const PhoneNumberSettings = ({ onSuccess }: PhoneNumberSettingsProps) => {
  const [inputNumber, setInputNumber] = useState('');
  const { phoneNumber, isLoading, savePhoneNumber, clearPhoneNumber } = usePhoneNumber();

  const handleSave = async () => {
    const success = await savePhoneNumber(inputNumber);
    if (success) {
      setInputNumber('');
      Alert.alert('Success', 'Phone number saved successfully');
      if (onSuccess) {
        onSuccess();
      }
    }
  };

  // Auto-focus input when mounted if no phone number is set
  useEffect(() => {
    if (!phoneNumber) {
      // Focus logic can be added here if using a ref to the input
    }
  }, []);

  const handleClear = async () => {
    await clearPhoneNumber();
    Alert.alert('Success', 'Phone number cleared');
  };

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="Enter your phone number"
        value={inputNumber}
        onChangeText={setInputNumber}
        keyboardType="phone-pad"
        autoCapitalize="none"
        autoCorrect={false}
        placeholderTextColor="#999"
      />
      
      <View style={styles.buttonContainer}>
        <Button
          title="Save Phone Number"
          onPress={handleSave}
          color="#007AFF"
          disabled={!inputNumber.trim()}
        />
      </View>

      {phoneNumber && (
        <View style={styles.currentNumberContainer}>
          <View style={styles.currentNumberText}>
            <Text style={styles.label}>Current Number:</Text>
            <Text style={styles.phoneNumber}>{phoneNumber}</Text>
          </View>
          <Button
            title="Clear"
            onPress={handleClear}
            color="#FF3B30"
          />
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 20,
    backgroundColor: '#fff',
    borderRadius: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    margin: 10,
  },
  input: {
    height: 50,
    borderColor: '#ddd',
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 15,
    fontSize: 16,
    marginBottom: 15,
    backgroundColor: '#f9f9f9',
  },
  buttonContainer: {
    marginBottom: 15,
    borderRadius: 8,
    overflow: 'hidden',
  },
  currentNumberContainer: {
    marginTop: 15,
    paddingTop: 15,
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
  currentNumberText: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },
  label: {
    fontSize: 16,
    color: '#333',
    marginRight: 10,
  },
  phoneNumber: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#007AFF',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
});

export default PhoneNumberSettings;
