import { useState, useEffect } from 'react';
import { Alert } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

const PHONE_NUMBER_KEY = '@user_phone_number';

export const usePhoneNumber = () => {
  const [phoneNumber, setPhoneNumber] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Load phone number from storage on mount
  useEffect(() => {
    loadPhoneNumber();
  }, []);

  const loadPhoneNumber = async () => {
    try {
      const storedNumber = await AsyncStorage.getItem(PHONE_NUMBER_KEY);
      if (storedNumber) {
        setPhoneNumber(storedNumber);
      }
    } catch (error) {
      console.error('Failed to load phone number', error);
      Alert.alert('Error', 'Failed to load your phone number');
    } finally {
      setIsLoading(false);
    }
  };

  const savePhoneNumber = async (number: string): Promise<boolean> => {
    try {
      // Basic phone number validation
      const cleanedNumber = number.replace(/\D/g, '');
      if (cleanedNumber.length < 10) {
        Alert.alert('Invalid Number', 'Please enter a valid phone number');
        return false;
      }
      
      await AsyncStorage.setItem(PHONE_NUMBER_KEY, cleanedNumber);
      setPhoneNumber(cleanedNumber);
      return true;
    } catch (error) {
      console.error('Failed to save phone number', error);
      Alert.alert('Error', 'Failed to save your phone number');
      return false;
    }
  };

  const clearPhoneNumber = async () => {
    try {
      await AsyncStorage.removeItem(PHONE_NUMBER_KEY);
      setPhoneNumber(null);
    } catch (error) {
      console.error('Failed to clear phone number', error);
    }
  };

  const checkPhoneNumber = async (): Promise<boolean> => {
    try {
      const storedNumber = await AsyncStorage.getItem(PHONE_NUMBER_KEY);
      return !!storedNumber;
    } catch (error) {
      console.error('Error checking phone number:', error);
      return false;
    }
  };

  return {
    phoneNumber,
    isLoading,
    savePhoneNumber,
    clearPhoneNumber,
    checkPhoneNumber,
    hasPhoneNumber: !!phoneNumber,
  };
};
