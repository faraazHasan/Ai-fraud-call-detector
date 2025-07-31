import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import Icon from 'react-native-vector-icons/Ionicons';
import PhoneNumberSettings from './PhoneNumberSettings';
import { usePhoneNumber } from '../../hooks/usePhoneNumber';

const PhoneNumberSettingsScreen = () => {
  const navigation = useNavigation();
  const { phoneNumber, clearPhoneNumber } = usePhoneNumber();
  const [showPhoneSettings, setShowPhoneSettings] = useState(false);

  const handleClearPhoneNumber = async () => {
    await clearPhoneNumber();
    setShowPhoneSettings(true);
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Icon name="arrow-back" size={24} color="#007AFF" />
        </TouchableOpacity>
        <Text style={styles.title}>Settings</Text>
        <View style={styles.headerRight} />
      </View>

      <ScrollView style={styles.scrollView}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Account</Text>
          
          <View style={styles.settingItem}>
            <View style={styles.settingInfo}>
              <Text style={styles.settingTitle}>Phone Number</Text>
              <Text style={styles.settingSubtitle}>
                {phoneNumber || 'Not set'}
              </Text>
            </View>
            <TouchableOpacity 
              style={styles.settingAction}
              onPress={() => setShowPhoneSettings(!showPhoneSettings)}
            >
              <Icon 
                name={showPhoneSettings ? 'chevron-up' : 'chevron-forward'} 
                size={20} 
                color="#8E8E93" 
              />
            </TouchableOpacity>
          </View>

          {showPhoneSettings && (
            <View style={styles.phoneSettingsContainer}>
              <PhoneNumberSettings 
                onSuccess={() => setShowPhoneSettings(false)} 
              />
            </View>
          )}
          
          {phoneNumber && (
            <TouchableOpacity 
              style={styles.clearButton}
              onPress={handleClearPhoneNumber}
            >
              <Text style={styles.clearButtonText}>Clear Phone Number</Text>
            </TouchableOpacity>
          )}
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f8f8',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
    backgroundColor: '#fff',
  },
  backButton: {
    padding: 8,
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
    color: '#000',
  },
  headerRight: {
    width: 40,
  },
  scrollView: {
    flex: 1,
  },
  section: {
    marginBottom: 24,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: '#e0e0e0',
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '500',
    color: '#8E8E93',
    padding: 16,
    paddingBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  settingItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  settingInfo: {
    flex: 1,
    marginRight: 16,
  },
  settingTitle: {
    fontSize: 16,
    color: '#000',
    marginBottom: 4,
  },
  settingSubtitle: {
    fontSize: 14,
    color: '#8E8E93',
  },
  settingAction: {
    padding: 8,
  },
  phoneSettingsContainer: {
    padding: 16,
    backgroundColor: '#f8f8f8',
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
  },
  clearButton: {
    padding: 16,
    alignItems: 'center',
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
  },
  clearButtonText: {
    color: '#FF3B30',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default PhoneNumberSettingsScreen;
