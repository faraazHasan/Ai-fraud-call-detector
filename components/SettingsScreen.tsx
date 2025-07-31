import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  Switch,
} from 'react-native';
import DialerSettings from './DialerSettings';
import PermissionManagerComponent from './PermissionManager';

interface SettingsScreenProps {
  style?: any;
}

const SettingsScreen: React.FC<SettingsScreenProps> = ({ style }) => {
  const [activeTab, setActiveTab] = useState<'permissions' | 'dialer'>('permissions');
  const [permissionsGranted, setPermissionsGranted] = useState<boolean>(false);

  const handlePermissionsChanged = (permissions: any) => {
    setPermissionsGranted(permissions.hasAllCriticalPermissions);
  };

  const showInfo = () => {
    Alert.alert(
      'App Settings',
      'Manage your app permissions and default dialer settings here.\n\n' +
      '• Permissions: Grant required permissions for app functionality\n' +
      '• Dialer Settings: Set this app as your default dialer\n\n' +
      'Both are important for the app to work properly.',
      [{ text: 'OK' }]
    );
  };

  return (
    <View style={[styles.container, style]}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>Settings</Text>
        <TouchableOpacity onPress={showInfo} style={styles.infoButton}>
          <Text style={styles.infoButtonText}>ℹ️</Text>
        </TouchableOpacity>
      </View>

      {/* Tab Navigation */}
      <View style={styles.tabContainer}>
        <TouchableOpacity
          style={[
            styles.tab,
            activeTab === 'permissions' && styles.activeTab
          ]}
          onPress={() => setActiveTab('permissions')}
        >
          <Text style={[
            styles.tabText,
            activeTab === 'permissions' && styles.activeTabText
          ]}>
            Permissions {!permissionsGranted && '⚠️'}
          </Text>
        </TouchableOpacity>
        
        <TouchableOpacity
          style={[
            styles.tab,
            activeTab === 'dialer' && styles.activeTab
          ]}
          onPress={() => setActiveTab('dialer')}
        >
          <Text style={[
            styles.tabText,
            activeTab === 'dialer' && styles.activeTabText
          ]}>
            Dialer Settings
          </Text>
        </TouchableOpacity>
      </View>

      {/* Content */}
      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        {activeTab === 'permissions' ? (
          <PermissionManagerComponent 
            onPermissionsChanged={handlePermissionsChanged}
          />
        ) : (
          <DialerSettings />
        )}
      </ScrollView>

      {/* Important Notice */}
      {!permissionsGranted && (
        <View style={styles.notice}>
          <Text style={styles.noticeText}>
            ⚠️ Some permissions are missing. Grant all permissions for full functionality.
          </Text>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333333',
  },
  infoButton: {
    padding: 4,
  },
  infoButtonText: {
    fontSize: 18,
  },
  tabContainer: {
    flexDirection: 'row',
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  tab: {
    flex: 1,
    paddingVertical: 16,
    paddingHorizontal: 20,
    alignItems: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  activeTab: {
    borderBottomColor: '#007AFF',
  },
  tabText: {
    fontSize: 16,
    fontWeight: '500',
    color: '#666666',
  },
  activeTabText: {
    color: '#007AFF',
    fontWeight: '600',
  },
  content: {
    flex: 1,
  },
  notice: {
    backgroundColor: '#FFF3E0',
    borderTopWidth: 1,
    borderTopColor: '#FFB74D',
    padding: 16,
  },
  noticeText: {
    fontSize: 14,
    color: '#E65100',
    textAlign: 'center',
    fontWeight: '500',
  },
});

export default SettingsScreen;
