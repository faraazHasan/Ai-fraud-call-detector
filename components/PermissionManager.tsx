import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
  ScrollView,
  Switch,
} from 'react-native';
import { NativeModules, Linking, Platform } from 'react-native';

const { PermissionManager } = NativeModules;

interface PermissionStatus {
  hasPhonePermissions: boolean;
  hasContactsPermissions: boolean;
  hasStoragePermissions: boolean;
  hasAudioPermissions: boolean;
  hasNotificationPermissions: boolean;
  hasManageExternalStorage: boolean;
  hasSystemAlertWindow: boolean;
  hasAllCriticalPermissions: boolean;
}

interface PermissionManagerProps {
  style?: any;
  onPermissionsChanged?: (permissions: PermissionStatus) => void;
}

const PermissionManagerComponent: React.FC<PermissionManagerProps> = ({ 
  style, 
  onPermissionsChanged 
}) => {
  const [permissions, setPermissions] = useState<PermissionStatus>({
    hasPhonePermissions: false,
    hasContactsPermissions: false,
    hasStoragePermissions: false,
    hasAudioPermissions: false,
    hasNotificationPermissions: false,
    hasManageExternalStorage: false,
    hasSystemAlertWindow: false,
    hasAllCriticalPermissions: false,
  });
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRequesting, setIsRequesting] = useState<boolean>(false);

  useEffect(() => {
    checkAllPermissions();
  }, []);

  useEffect(() => {
    if (onPermissionsChanged) {
      onPermissionsChanged(permissions);
    }
  }, [permissions, onPermissionsChanged]);

  const checkAllPermissions = async () => {
    try {
      setIsLoading(true);
      if (PermissionManager && PermissionManager.checkAllPermissions) {
        const result = await PermissionManager.checkAllPermissions();
        setPermissions(result);
      }
    } catch (error) {
      console.error('Error checking permissions:', error);
      Alert.alert('Error', 'Failed to check permissions');
    } finally {
      setIsLoading(false);
    }
  };

  const requestAllPermissions = async () => {
    try {
      setIsRequesting(true);
      
      if (!PermissionManager || !PermissionManager.requestAllPermissions) {
        Alert.alert('Error', 'Permission manager is not available');
        return;
      }

      await PermissionManager.requestAllPermissions();
      
      // Wait a bit for permissions to be processed
      setTimeout(() => {
        checkAllPermissions();
      }, 1000);
      
    } catch (error) {
      console.error('Error requesting permissions:', error);
      Alert.alert('Error', 'Failed to request permissions');
    } finally {
      setIsRequesting(false);
    }
  };

  const requestSpecificPermission = async (permissionType: string) => {
    try {
      setIsRequesting(true);
      
      switch (permissionType) {
        case 'phone':
          await PermissionManager.requestPhonePermissions();
          break;
        case 'contacts':
          await PermissionManager.requestContactsPermissions();
          break;
        default:
          await PermissionManager.requestAllPermissions();
      }
      
      setTimeout(() => {
        checkAllPermissions();
      }, 1000);
      
    } catch (error) {
      console.error(`Error requesting ${permissionType} permissions:`, error);
      Alert.alert('Error', `Failed to request ${permissionType} permissions`);
    } finally {
      setIsRequesting(false);
    }
  };

  const openAppSettings = () => {
    Alert.alert(
      'Open Settings',
      'Some permissions need to be granted manually in app settings. Would you like to open the settings now?',
      [
        { text: 'Cancel', style: 'cancel' },
        { 
          text: 'Open Settings', 
          onPress: () => {
            if (Platform.OS === 'android') {
              Linking.openSettings();
            }
          }
        }
      ]
    );
  };

  const showPermissionInfo = (permissionType: string) => {
    const permissionInfos: { [key: string]: { title: string; description: string } } = {
      phone: {
        title: 'Phone Permissions',
        description: 'Required to make calls, read phone state, and access call logs. Essential for the dialer functionality.'
      },
      contacts: {
        title: 'Contacts Permissions',
        description: 'Required to read and write contacts. Allows you to see contact names during calls and add new contacts.'
      },
      storage: {
        title: 'Storage Permissions',
        description: 'Required to save call recordings and access external storage for app data.'
      },
      audio: {
        title: 'Audio Permissions',
        description: 'Required to record calls and modify audio settings during calls.'
      },
      notification: {
        title: 'Notification Permissions',
        description: 'Required to show missed call notifications and other important alerts.'
      },
      special: {
        title: 'Special Permissions',
        description: 'System overlay and external storage management permissions for advanced features.'
      }
    };

    const info = permissionInfos[permissionType];
    if (info) {
      Alert.alert(info.title, info.description, [{ text: 'OK' }]);
    }
  };

  const PermissionItem = ({ 
    title, 
    granted, 
    onRequest, 
    onInfo, 
    essential = false 
  }: {
    title: string;
    granted: boolean;
    onRequest: () => void;
    onInfo: () => void;
    essential?: boolean;
  }) => (
    <View style={styles.permissionItem}>
      <View style={styles.permissionHeader}>
        <Text style={[styles.permissionTitle, essential && styles.essentialPermission]}>
          {title} {essential && '*'}
        </Text>
        <TouchableOpacity onPress={onInfo} style={styles.infoButton}>
          <Text style={styles.infoButtonText}>ℹ️</Text>
        </TouchableOpacity>
      </View>
      
      <View style={styles.permissionStatus}>
        <View style={[
          styles.statusIndicator,
          granted ? styles.statusGranted : styles.statusDenied
        ]}>
          <Text style={[
            styles.statusText,
            granted ? styles.statusGrantedText : styles.statusDeniedText
          ]}>
            {granted ? '✓ Granted' : '✗ Not Granted'}
          </Text>
        </View>
        
        {!granted && (
          <TouchableOpacity
            style={[styles.requestButton, isRequesting && styles.requestButtonDisabled]}
            onPress={onRequest}
            disabled={isRequesting}
          >
            <Text style={styles.requestButtonText}>Grant</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );

  if (isLoading) {
    return (
      <View style={[styles.container, styles.centered, style]}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.loadingText}>Checking permissions...</Text>
      </View>
    );
  }

  return (
    <ScrollView style={[styles.container, style]} showsVerticalScrollIndicator={false}>
      <View style={styles.header}>
        <Text style={styles.title}>App Permissions</Text>
        <Text style={styles.subtitle}>
          Grant the required permissions to use all app features
        </Text>
      </View>

      {/* Overall Status */}
      <View style={[
        styles.overallStatus,
        permissions.hasAllCriticalPermissions ? styles.statusComplete : styles.statusIncomplete
      ]}>
        <Text style={styles.overallStatusText}>
          {permissions.hasAllCriticalPermissions 
            ? '✅ All permissions granted!' 
            : '⚠️ Some permissions are missing'
          }
        </Text>
      </View>

      {/* Essential Permissions */}
      <Text style={styles.sectionTitle}>Essential Permissions *</Text>
      
      <PermissionItem
        title="Phone & Calling"
        granted={permissions.hasPhonePermissions}
        onRequest={() => requestSpecificPermission('phone')}
        onInfo={() => showPermissionInfo('phone')}
        essential={true}
      />

      <PermissionItem
        title="Contacts"
        granted={permissions.hasContactsPermissions}
        onRequest={() => requestSpecificPermission('contacts')}
        onInfo={() => showPermissionInfo('contacts')}
        essential={true}
      />

      {/* Optional Permissions */}
      <Text style={styles.sectionTitle}>Optional Permissions</Text>
      
      <PermissionItem
        title="Storage"
        granted={permissions.hasStoragePermissions}
        onRequest={() => requestSpecificPermission('storage')}
        onInfo={() => showPermissionInfo('storage')}
      />

      <PermissionItem
        title="Audio Recording"
        granted={permissions.hasAudioPermissions}
        onRequest={() => requestSpecificPermission('audio')}
        onInfo={() => showPermissionInfo('audio')}
      />

      <PermissionItem
        title="Notifications"
        granted={permissions.hasNotificationPermissions}
        onRequest={() => requestSpecificPermission('notification')}
        onInfo={() => showPermissionInfo('notification')}
      />

      {/* Action Buttons */}
      <View style={styles.actionButtons}>
        {!permissions.hasAllCriticalPermissions && (
          <TouchableOpacity
            style={[styles.grantAllButton, isRequesting && styles.grantAllButtonDisabled]}
            onPress={requestAllPermissions}
            disabled={isRequesting}
          >
            {isRequesting ? (
              <ActivityIndicator size="small" color="#FFFFFF" />
            ) : (
              <Text style={styles.grantAllButtonText}>Grant All Permissions</Text>
            )}
          </TouchableOpacity>
        )}

        <TouchableOpacity
          style={styles.refreshButton}
          onPress={checkAllPermissions}
        >
          <Text style={styles.refreshButtonText}>Refresh Status</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.settingsButton}
          onPress={openAppSettings}
        >
          <Text style={styles.settingsButtonText}>Open App Settings</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.footer}>
        <Text style={styles.footerText}>
          * Essential permissions are required for basic app functionality
        </Text>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  centered: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    padding: 20,
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#666666',
    lineHeight: 22,
  },
  overallStatus: {
    margin: 16,
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
  },
  statusComplete: {
    backgroundColor: '#E8F5E8',
    borderColor: '#4CAF50',
    borderWidth: 1,
  },
  statusIncomplete: {
    backgroundColor: '#FFF3E0',
    borderColor: '#FF9800',
    borderWidth: 1,
  },
  overallStatusText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333333',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333333',
    marginHorizontal: 16,
    marginTop: 24,
    marginBottom: 12,
  },
  permissionItem: {
    backgroundColor: '#FFFFFF',
    marginHorizontal: 16,
    marginVertical: 4,
    padding: 16,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  permissionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  permissionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333333',
    flex: 1,
  },
  essentialPermission: {
    color: '#D32F2F',
  },
  infoButton: {
    padding: 4,
  },
  infoButtonText: {
    fontSize: 16,
  },
  permissionStatus: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  statusIndicator: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    flex: 1,
    marginRight: 12,
  },
  statusGranted: {
    backgroundColor: '#E8F5E8',
  },
  statusDenied: {
    backgroundColor: '#FFEBEE',
  },
  statusText: {
    fontSize: 14,
    fontWeight: '500',
    textAlign: 'center',
  },
  statusGrantedText: {
    color: '#2E7D32',
  },
  statusDeniedText: {
    color: '#D32F2F',
  },
  requestButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
  },
  requestButtonDisabled: {
    backgroundColor: '#CCCCCC',
  },
  requestButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
  },
  actionButtons: {
    padding: 16,
    gap: 12,
  },
  grantAllButton: {
    backgroundColor: '#4CAF50',
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
  },
  grantAllButtonDisabled: {
    backgroundColor: '#CCCCCC',
  },
  grantAllButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
  refreshButton: {
    backgroundColor: '#2196F3',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  refreshButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
  },
  settingsButton: {
    backgroundColor: '#FF9800',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  settingsButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
  },
  footer: {
    padding: 16,
    paddingTop: 8,
  },
  footerText: {
    fontSize: 12,
    color: '#999999',
    textAlign: 'center',
    fontStyle: 'italic',
  },
  loadingText: {
    fontSize: 16,
    color: '#666666',
    marginTop: 12,
  },
});

export default PermissionManagerComponent;
