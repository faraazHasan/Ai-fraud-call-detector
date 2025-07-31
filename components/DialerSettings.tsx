import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { NativeModules } from 'react-native';

const { DialerRoleManager } = NativeModules;

interface DialerSettingsProps {
  style?: any;
}

const DialerSettings: React.FC<DialerSettingsProps> = ({ style }) => {
  const [isDefaultDialer, setIsDefaultDialer] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isChecking, setIsChecking] = useState<boolean>(true);

  useEffect(() => {
    checkDialerStatus();
  }, []);

  const checkDialerStatus = async () => {
    try {
      setIsChecking(true);
      if (DialerRoleManager && DialerRoleManager.checkDialerRole) {
        const isDefault = await DialerRoleManager.checkDialerRole();
        setIsDefaultDialer(isDefault);
      }
    } catch (error) {
      console.error('Error checking dialer status:', error);
    } finally {
      setIsChecking(false);
    }
  };

  const requestDialerRole = async () => {
    try {
      setIsLoading(true);
      
      if (!DialerRoleManager || !DialerRoleManager.requestDialerRole) {
        Alert.alert(
          'Error',
          'Dialer role manager is not available on this device.'
        );
        return;
      }

      const result = await DialerRoleManager.requestDialerRole();
      
      if (result) {
        setIsDefaultDialer(true);
        Alert.alert(
          'Success',
          'App is now set as the default dialer!',
          [{ text: 'OK' }]
        );
      } else {
        Alert.alert(
          'Permission Denied',
          'You need to set this app as the default dialer to use all features.',
          [
            { text: 'Cancel', style: 'cancel' },
            { text: 'Try Again', onPress: requestDialerRole }
          ]
        );
      }
    } catch (error) {
      console.error('Error requesting dialer role:', error);
      Alert.alert(
        'Error',
        'Failed to request default dialer permission. Please try again.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  const showDialerInfo = () => {
    Alert.alert(
      'Default Dialer App',
      'Setting this app as your default dialer allows you to:\n\n' +
      '• Make calls directly from the app\n' +
      '• Handle incoming calls with custom interface\n' +
      '• Access advanced call features\n' +
      '• Override system missed call notifications\n\n' +
      'You can change this setting anytime in your device settings.',
      [{ text: 'OK' }]
    );
  };

  if (isChecking) {
    return (
      <View style={[styles.container, style]}>
        <ActivityIndicator size="small" color="#007AFF" />
        <Text style={styles.checkingText}>Checking dialer status...</Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, style]}>
      <View style={styles.header}>
        <Text style={styles.title}>Default Dialer Settings</Text>
        <TouchableOpacity onPress={showDialerInfo} style={styles.infoButton}>
          <Text style={styles.infoButtonText}>ℹ️</Text>
        </TouchableOpacity>
      </View>
      
      <View style={styles.statusContainer}>
        <Text style={styles.statusLabel}>Current Status:</Text>
        <View style={[
          styles.statusBadge,
          isDefaultDialer ? styles.statusActive : styles.statusInactive
        ]}>
          <Text style={[
            styles.statusText,
            isDefaultDialer ? styles.statusActiveText : styles.statusInactiveText
          ]}>
            {isDefaultDialer ? '✓ Default Dialer' : '✗ Not Default Dialer'}
          </Text>
        </View>
      </View>

      {!isDefaultDialer && (
        <View style={styles.actionContainer}>
          <Text style={styles.description}>
            To use all features of this app, please set it as your default dialer.
          </Text>
          
          <TouchableOpacity
            style={[styles.button, isLoading && styles.buttonDisabled]}
            onPress={requestDialerRole}
            disabled={isLoading}
          >
            {isLoading ? (
              <ActivityIndicator size="small" color="#FFFFFF" />
            ) : (
              <Text style={styles.buttonText}>Set as Default Dialer</Text>
            )}
          </TouchableOpacity>
        </View>
      )}

      {isDefaultDialer && (
        <View style={styles.successContainer}>
          <Text style={styles.successText}>
            ✅ This app is your default dialer. All features are available!
          </Text>
          
          <TouchableOpacity
            style={styles.refreshButton}
            onPress={checkDialerStatus}
          >
            <Text style={styles.refreshButtonText}>Refresh Status</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    margin: 16,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333333',
  },
  infoButton: {
    padding: 4,
  },
  infoButtonText: {
    fontSize: 16,
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  statusLabel: {
    fontSize: 14,
    color: '#666666',
    marginRight: 8,
  },
  statusBadge: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  statusActive: {
    backgroundColor: '#E8F5E8',
  },
  statusInactive: {
    backgroundColor: '#FFF2F2',
  },
  statusText: {
    fontSize: 12,
    fontWeight: '600',
  },
  statusActiveText: {
    color: '#2E7D32',
  },
  statusInactiveText: {
    color: '#D32F2F',
  },
  actionContainer: {
    marginTop: 8,
  },
  description: {
    fontSize: 14,
    color: '#666666',
    lineHeight: 20,
    marginBottom: 16,
  },
  button: {
    backgroundColor: '#007AFF',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonDisabled: {
    backgroundColor: '#CCCCCC',
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  successContainer: {
    marginTop: 8,
  },
  successText: {
    fontSize: 14,
    color: '#2E7D32',
    lineHeight: 20,
    marginBottom: 12,
  },
  refreshButton: {
    backgroundColor: '#F0F0F0',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 6,
    alignItems: 'center',
  },
  refreshButtonText: {
    color: '#007AFF',
    fontSize: 14,
    fontWeight: '500',
  },
  checkingText: {
    fontSize: 14,
    color: '#666666',
    marginLeft: 8,
  },
});

export default DialerSettings;
