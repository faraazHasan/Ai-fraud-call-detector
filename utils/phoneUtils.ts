import AsyncStorage from '@react-native-async-storage/async-storage';

const PHONE_NUMBER_KEY = '@user_phone_number';

export const getStoredPhoneNumber = async (): Promise<string | null> => {
  try {
    return await AsyncStorage.getItem(PHONE_NUMBER_KEY);
  } catch (error) {
    console.error('Error getting phone number:', error);
    return null;
  }
};

export const validatePhoneNumber = (phoneNumber: string): boolean => {
  if (!phoneNumber) return false;
  // Basic validation - at least 10 digits, can include country code
  const cleaned = phoneNumber.replace(/\D/g, '');
  return cleaned.length >= 10;
};

export const formatPhoneNumber = (phoneNumber: string): string => {
  // Format as (XXX) XXX-XXXX
  const cleaned = phoneNumber.replace(/\D/g, '');
  const match = cleaned.match(/^(\d{0,3})(\d{0,3})(\d{0,4})$/);
  if (!match) return phoneNumber;
  
  return `(${match[1]}) ${match[2]}${match[3] ? `-${match[3]}` : ''}`.trim();
};
