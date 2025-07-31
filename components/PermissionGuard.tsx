import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
  Modal,
} from 'react-native';
import { NativeModules } from 'react-native';

const { PermissionManager } = NativeModules;

interface PermissionGuardProps {
  requiredPermissions: ('phone' | 'contacts' | 'storage' | 'audio')[];
  children: React.ReactNode;
  fallbackComponent?: React.ReactNode;
  onPermissionGranted?: () => void;
  onPermissionDenied?: () => void;
}

const PermissionGuard: React.FC<PermissionGuardProps> = ({
  requiredPermissions,
  children,
  fallbackComponent,
  onPermissionGranted,
  onPermissionDenied,
}) => {
  const [hasPermissions, setHasPermissions] = useState<boolean>(false);
  const [isChecking, setIsChecking] = useState<boolean>(true);
  const [isRequesting, setIsRequesting] = useState<boolean>(false);
  const [showPermissionModal, setShowPermissionModal] = useState<boolean>(false);

  useEffect(() => {
    checkPermissions();
  }, []);

  const checkPermissions = async () => {
    try {
      setIsChecking(true);
      
      if (!PermissionManager) {
        console.warn('PermissionManager not available');
        setHasPermissions(false);
        setIsChecking(false);
        return;
      }

      const permissions = await PermissionManager.checkAllPermissions();
      
      let allRequired = true;
      
      for (const permission of requiredPermissions) {
        switch (permission) {
          case 'phone':
            if (!permissions.hasPhonePermissions) allRequired = false;
            break;
          case 'contacts':
            if (!permissions.hasContactsPermissions) allRequired = false;
            break;
          case 'storage':
            if (!permissions.hasStoragePermissions) allRequired = false;
            break;
          case 'audio':
            if (!permissions.hasAudioPermissions) allRequired = false;
            break;
        }
      }
      
      setHasPermissions(allRequired);
      
      if (allRequired && onPermissionGranted) {
        onPermissionGranted();
      } else if (!allRequired && onPermissionDenied) {
        onPermissionDenied();
      }
      
    } catch (error) {
      console.error('Error checking permissions:', error);
      setHasPermissions(false);
    } finally {
      setIsChecking(false);
    }
  };

  const requestPermissions = async () => {
    try {
      setIsRequesting(true);
      
      // Request specific permissions based on requirements
      for (const permission of requiredPermissions) {
        switch (permission) {
          case 'phone':
            await PermissionManager.requestPhonePermissions();
            break;
          case 'contacts':
            await PermissionManager.requestContactsPermissions();
            break;
          case 'storage':
            // Storage permissions need special handling
            await PermissionManager.requestAllPermissions();
            break;
          case 'audio':
            await PermissionManager.requestAllPermissions();
            break;
        }
      }
      
      // Wait a bit for permissions to be processed
      setTimeout(() => {
        checkPermissions();
        setShowPermissionModal(false);
      }, 1000);
      
    } catch (error) {
      console.error('Error requesting permissions:', error);
      Alert.alert('Error', 'Failed to request permissions');
    } finally {
      setIsRequesting(false);
    }
  };

  const getPermissionDescription = (permission: string): string => {
    switch (permission) {
      case 'phone':
        return 'Phone permissions are required to make calls and access call logs.';
      case 'contacts':
        return 'Contacts permissions are required to display contact names and add new contacts.';
      case 'storage':
        return 'Storage permissions are required to save call recordings and app data.';
      case 'audio':
        return 'Audio permissions are required to record calls and manage audio settings.';
      default:
        return 'This permission is required for the app to function properly.';
    }
  };

  const showPermissionExplanation = () => {
    const descriptions = requiredPermissions.map(getPermissionDescription).join('\n\n');
    
    Alert.alert(
      'Permissions Required',
      `This feature requires the following permissions:\n\n${descriptions}\n\nWould you like to grant these permissions now?`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Grant Permissions', onPress: () => setShowPermissionModal(true) }
      ]
    );
  };

  if (isChecking) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.checkingText}>Checking permissions...</Text>
      </View>
    );
  }

  if (!hasPermissions) {
    return (
      <>
        {fallbackComponent || (
          <View style={styles.permissionContainer}>
            <Text style={styles.permissionTitle}>Permissions Required</Text>
            <Text style={styles.permissionMessage}>
              This feature requires additional permissions to work properly.
            </Text>
            
            <TouchableOpacity
              style={styles.requestButton}
              onPress={showPermissionExplanation}
            >
              <Text style={styles.requestButtonText}>Grant Permissions</Text>
            </TouchableOpacity>
            
            <TouchableOpacity
              style={styles.refreshButton}
              onPress={checkPermissions}
            >
              <Text style={styles.refreshButtonText}>Refresh</Text>
            </TouchableOpacity>
          </View>
        )}
        
        {/* Permission Request Modal */}
        <Modal
          visible={showPermissionModal}
          transparent={true}
          animationType="slide"
          onRequestClose={() => setShowPermissionModal(false)}
        >
          <View style={styles.modalOverlay}>
            <View style={styles.modalContent}>
              <Text style={styles.modalTitle}>Grant Permissions</Text>
              <Text style={styles.modalMessage}>
                The app needs the following permissions:
              </Text>
              
              {requiredPermissions.map((permission, index) => (
                <View key={index} style={styles.permissionItem}>
                  <Text style={styles.permissionName}>
                    {permission.charAt(0).toUpperCase() + permission.slice(1)}
                  </Text>
                  <Text style={styles.permissionDesc}>
                    {getPermissionDescription(permission)}
                  </Text>
                </View>
              ))}
              
              <View style={styles.modalButtons}>
                <TouchableOpacity
                  style={[styles.modalButton, styles.cancelButton]}
                  onPress={() => setShowPermissionModal(false)}
                >
                  <Text style={styles.cancelButtonText}>Cancel</Text>
                </TouchableOpacity>
                
                <TouchableOpacity
                  style={[styles.modalButton, styles.grantButton]}
                  onPress={requestPermissions}
                  disabled={isRequesting}
                >
                  {isRequesting ? (
                    <ActivityIndicator size="small" color="#FFFFFF" />
                  ) : (
                    <Text style={styles.grantButtonText}>Grant</Text>
                  )}
                </TouchableOpacity>
              </View>
            </View>
          </View>
        </Modal>
      </>
    );
  }

  return <>{children}</>;
};

const styles = StyleSheet.create({
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  checkingText: {
    marginTop: 12,
    fontSize: 16,
    color: '#666666',
  },
  permissionContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#F8F9FA',
  },
  permissionTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 16,
    textAlign: 'center',
  },
  permissionMessage: {
    fontSize: 16,
    color: '#666666',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 32,
  },
  requestButton: {
    backgroundColor: '#007AFF',
    paddingVertical: 16,
    paddingHorizontal: 32,
    borderRadius: 12,
    marginBottom: 16,
  },
  requestButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  refreshButton: {
    backgroundColor: '#F0F0F0',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  refreshButtonText: {
    color: '#007AFF',
    fontSize: 14,
    fontWeight: '500',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalContent: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 24,
    width: '100%',
    maxWidth: 400,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 16,
    textAlign: 'center',
  },
  modalMessage: {
    fontSize: 16,
    color: '#666666',
    marginBottom: 20,
    textAlign: 'center',
  },
  permissionItem: {
    marginBottom: 16,
    padding: 12,
    backgroundColor: '#F8F9FA',
    borderRadius: 8,
  },
  permissionName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333333',
    marginBottom: 4,
  },
  permissionDesc: {
    fontSize: 14,
    color: '#666666',
    lineHeight: 20,
  },
  modalButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 24,
  },
  modalButton: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  cancelButton: {
    backgroundColor: '#F0F0F0',
    marginRight: 8,
  },
  grantButton: {
    backgroundColor: '#007AFF',
    marginLeft: 8,
  },
  cancelButtonText: {
    color: '#666666',
    fontSize: 16,
    fontWeight: '500',
  },
  grantButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default PermissionGuard;
