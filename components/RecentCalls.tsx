import React, {useState, useEffect} from 'react';
import {
  StyleSheet,
  View,
  Text,
  FlatList,
  TouchableOpacity,
  Image,
  ActivityIndicator,
  Platform,
} from 'react-native';
import {useFocusEffect} from '@react-navigation/native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {CallRecord, CallType} from '../utils/helpers/callHistory';
import {
  getRecentCalls,
  markCallAsRead,
} from '../utils/native_modules/call_history';
import {NativeModules} from 'react-native';

const {MissedCallModule} = NativeModules;

const RecentCallsScreen: React.FC = () => {
  const [callHistory, setCallHistory] = useState<CallRecord[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // Load call history when screen comes into focus
  useFocusEffect(
    React.useCallback(() => {
      loadCallHistory();
      // Reset missed call count when user views recent calls
      resetMissedCallCount();
    }, []),
  );

  // Initial load
  useEffect(() => {
    loadCallHistory();
  }, []);
  
  // Reset missed call count
  const resetMissedCallCount = async () => {
    try {
      console.log('RecentCalls: Attempting to reset missed call count...');
      
      // First, mark any missed calls as read in the call log
      if (callHistory && callHistory.length > 0) {
        const missedCalls = callHistory.filter(call => 
          call.type === 'missed'
        );
        
        console.log(`RecentCalls: Found ${missedCalls.length} missed calls to mark as read`);
        
        for (const call of missedCalls) {
          try {
            await markCallAsRead(call.id);
            console.log(`RecentCalls: Marked call ${call.id} as read`);
          } catch (error) {
            console.warn(`RecentCalls: Failed to mark call ${call.id} as read:`, error);
          }
        }
      }
      
      // Then reset the missed call count in the service
      if (MissedCallModule && MissedCallModule.resetMissedCallCount) {
        const result = await MissedCallModule.resetMissedCallCount();
        console.log('RecentCalls: Missed call count reset successfully, result:', result);
      } else {
        console.warn('RecentCalls: MissedCallModule or resetMissedCallCount method not available');
      }
    } catch (error) {
      console.error('RecentCalls: Error resetting missed call count:', error);
    }
  };

  const loadCallHistory = async () => {
    try {
      setLoading(true);

      if (Platform.OS === 'android') {
        try {
          console.log('Attempting to get call history from native module');
          // On Android, get call history from the native module
          const nativeCalls = await getRecentCalls(100);
          console.log('Native call history entries:', nativeCalls?.length || 0);

          if (nativeCalls && nativeCalls.length > 0) {
            // Convert native call entries to our app's format
            const convertedCalls: CallRecord[] = nativeCalls.map(call => ({
              id: call.id,
              phoneNumber: call.phoneNumber,
              contactName: call.contactName || undefined,
              timestamp: call.timestamp,
              duration: call.duration,
              type: call.type as CallType,
            }));

            // Mark any new missed calls as read
            for (const call of nativeCalls) {
              if (
                call.isNew &&
                (call.type === 'missed' || call.type === 'rejected')
              ) {
                await markCallAsRead(call.id);
              }
            }

            // Update the state with native call history
            setCallHistory(convertedCalls);

            // Also save to AsyncStorage for consistency
            await AsyncStorage.setItem(
              'call_history',
              JSON.stringify(convertedCalls),
            );

            setLoading(false);
            return;
          }
        } catch (nativeError) {
          // If there's an error with the native module, log it and continue to fallback
          console.error('Error with native call history:', nativeError);
        }
      }

      // Fall back to AsyncStorage if native call history isn't available
      const historyJson = await AsyncStorage.getItem('call_history');
      if (historyJson) {
        const history = JSON.parse(historyJson);
        // Sort by timestamp, most recent first
        setCallHistory(
          history.sort(
            (a: CallRecord, b: CallRecord) => b.timestamp - a.timestamp,
          ),
        );
      }
    } catch (error) {
      console.error('Error loading call history:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (timestamp: number): string => {
    const date = new Date(timestamp);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    // Check if call was today
    if (date.toDateString() === today.toDateString()) {
      return (
        'Today ' +
        date.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})
      );
    }
    // Check if call was yesterday
    else if (date.toDateString() === yesterday.toDateString()) {
      return (
        'Yesterday ' +
        date.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})
      );
    }
    // Otherwise show date
    else {
      return (
        date.toLocaleDateString() +
        ' ' +
        date.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})
      );
    }
  };

  const formatDuration = (seconds: number): string => {
    if (seconds === 0) {
      return '';
    }

    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;

    if (minutes === 0) {
      return `${remainingSeconds}s`;
    } else {
      return `${minutes}m ${remainingSeconds}s`;
    }
  };

  const getCallTypeIcon = (type: CallType) => {
    switch (type) {
      case 'incoming':
        return <Icon name="call-received" color="#4CAF50" size={18} />;
      case 'outgoing':
        return <Icon name="call-made" color="#2196F3" size={18} />;
      case 'missed':
        return <Icon name="call-missed" color="#F44336" size={18} />;
      default:
        return null;
    }
  };

  const handleCallPress = (item: CallRecord) => {
    // Import the make_call function at the top of the file
    const {make_call} = require('../utils/native_modules/dialer_module');
    make_call(item.phoneNumber);
  };

  const renderCallItem = ({item}: {item: CallRecord}) => {
    return (
      <TouchableOpacity
        style={styles.callItem}
        onPress={() => handleCallPress(item)}>
        <View style={styles.callIconContainer}>
          {getCallTypeIcon(item.type)}
        </View>
        <View style={styles.callInfoContainer}>
          <Text style={styles.contactName}>
            {item.contactName || item.phoneNumber}
          </Text>
          <Text style={styles.callDetails}>
            {formatDate(item.timestamp)}
            {item.duration > 0 && ` • ${formatDuration(item.duration)}`}
          </Text>
        </View>
        <TouchableOpacity
          style={styles.callButton}
          onPress={() => handleCallPress(item)}>
          <Image
            source={require('../assets/images/call-icon.png')}
            style={styles.callButtonImage}
          />
        </TouchableOpacity>
      </TouchableOpacity>
    );
  };

  // We can use the grouped calls approach in a future update
  // if we want to switch to a SectionList

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#007BFF" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.headerText}>Recent Calls</Text>
      {callHistory.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Icon name="call-outline" size={60} color="#CCCCCC" />
          <Text style={styles.emptyText}>No recent calls</Text>
        </View>
      ) : (
        <FlatList
          data={callHistory}
          renderItem={renderCallItem}
          keyExtractor={item => item.id}
          contentContainerStyle={styles.listContainer}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 16,
    paddingTop: 16,
  },
  headerText: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 16,
    marginTop: 40,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    marginTop: 16,
    fontSize: 18,
    color: '#888888',
  },
  listContainer: {
    paddingBottom: 20,
  },
  callItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  callIconContainer: {
    width: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  callInfoContainer: {
    flex: 1,
    marginLeft: 10,
  },
  contactName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#212121',
  },
  callDetails: {
    fontSize: 14,
    color: '#757575',
    marginTop: 2,
  },
  callButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#E8F5E9',
  },
  callButtonImage: {
    width: 24,
    height: 24,
    resizeMode: 'contain',
  },
});

export default RecentCallsScreen;
