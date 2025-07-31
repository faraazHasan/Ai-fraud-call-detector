import React, {useState, useEffect} from 'react';
import {
  StyleSheet,
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Image,
  Pressable,
  Alert,
  Platform,
} from 'react-native';
import {NativeModules} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {make_call} from '../utils/native_modules/dialer_module';
import CustomTextInput from './common/CommonTextBox';
import {useForm} from 'react-hook-form';
import AddContactModal from './AddContactModal';
import Icon from 'react-native-vector-icons/Ionicons';
import TrashIcon from 'react-native-vector-icons/Feather';

const SearchIcon = require('../assets/images/search-icon.png');

const PlaceholderImg = require('../assets/images/placeholder-img.png');
const BluePlaceholder = require('../assets/images/blue-placeholder.png');

const {ContactsModule} = NativeModules;

const ContactsList: React.FC = () => {
  const insets = useSafeAreaInsets();
  const [contacts, setContacts] = useState<any[]>([]);
  const [filteredContacts, setFilteredContacts] = useState<any[]>([]);
  const {control} = useForm();
  const [searchText, setSearchText] = useState<string>('');
  const [modalVisible, setModalVisible] = useState<boolean>(false);

  const fetchContacts = () => {
    ContactsModule.checkPermissionAndFetchContacts(
      (contactsList: any[]) => {
        // Success callback
        setContacts(contactsList);
        setFilteredContacts(contactsList); // Initially show all contacts
      },
      (error: string) => {
        // Error callback
        Alert.alert('Error', error);
      },
    );
  };

  useEffect(() => {
    fetchContacts();
  }, []);

  /** Handle search input */
  const handleSearch = (text: string) => {
    setSearchText(text);
    const filtered = contacts.filter(
      contact =>
        (contact.contactName &&
          contact.contactName.toLowerCase().includes(text.toLowerCase())) ||
        (contact.phoneNumbers &&
          contact.phoneNumbers.some((phone: string) => phone.includes(text))),
    );
    setFilteredContacts(filtered);
  };

  const openAddContactModal = () => {
    setModalVisible(true);
  };

  const handleContactAdded = () => {
    // Refresh contacts list after adding a new contact
    fetchContacts();
  };

  // Delete contact handler
  const handleDeleteContact = (contact: any) => {
    if (!contact || !contact.contactId) {
      Alert.alert('Error', 'Cannot delete contact: Missing contact ID');
      return;
    }

    Alert.alert(
      'Delete Contact',
      `Are you sure you want to delete ${
        contact.contactName || 'this contact'
      }?`,
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => {
            // Check if the native module method exists
            if (typeof ContactsModule.deleteContact !== 'function') {
              Alert.alert(
                'Feature Not Available',
                'Contact deletion is not supported in this version.',
              );
              return;
            }

            ContactsModule.deleteContact(
              contact.contactId,
              () => {
                Alert.alert('Success', 'Contact deleted successfully');
                fetchContacts();
              },
              (error: string) => {
                Alert.alert('Error', error);
              },
            );
          },
        },
      ],
      {cancelable: true},
    );
  };

  return (
    <View
      style={[
        styles.container,
        {
          paddingTop: insets.top || 10,
          paddingBottom: Platform.OS === 'ios' ? 90 : 75, // Add padding for tab bar
          paddingLeft: insets.left,
          paddingRight: insets.right,
        },
      ]}>
      {/* Search Bar */}
      <View style={styles.searchbar}>
        <View style={styles.relativeContainer}>
          <CustomTextInput
            placeHolder="Search"
            style={styles.inputStyle}
            control={control}
            keyboardType="default"
            name="search"
            value={searchText}
            onChangeText={handleSearch}
          />
          <TouchableOpacity style={styles.searchIconArea}>
            <Image source={SearchIcon} style={styles.searchIcon} />
          </TouchableOpacity>
        </View>
      </View>

      {/* User Info Card */}
      <View style={styles.card}>
        <Image source={PlaceholderImg} style={styles.avatar} />
        <View style={styles.info}>
          <Text style={styles.name}>User Name</Text>
          <Text style={styles.stats}>My Contacts</Text>
        </View>
      </View>

      {/* Contacts List */}
      <ScrollView style={styles.scrollContent}>
        {filteredContacts && filteredContacts.length > 0 ? (
          filteredContacts.map((contact, index) => (
            <View style={styles.contactRow} key={index}>
              <Pressable
                style={styles.nameInline}
                onPress={() => {
                  if (contact.phoneNumbers && contact.phoneNumbers.length > 0) {
                    make_call(contact.phoneNumbers[0]);
                  } else {
                    Alert.alert('No phone number available.');
                  }
                }}>
                <Image source={BluePlaceholder} style={styles.blueAvatar} />
                <Text style={styles.contactItem}>{contact.contactName}</Text>
              </Pressable>
              <TouchableOpacity
                style={styles.deleteIconArea}
                onPress={() => handleDeleteContact(contact)}>
                <TrashIcon name="trash-2" size={20} color="#f44336" />
              </TouchableOpacity>
            </View>
          ))
        ) : (
          <Text style={styles.noContactsText}>No contacts available</Text>
        )}
      </ScrollView>
      {/* Add Contact Button */}
      <TouchableOpacity style={styles.addButton} onPress={openAddContactModal}>
        <Icon name="add" size={24} color="#fff" />
      </TouchableOpacity>

      {/* Add Contact Modal */}
      <AddContactModal
        visible={modalVisible}
        onClose={() => setModalVisible(false)}
        onContactAdded={handleContactAdded}
      />
    </View>
  );
};
/** Styles */
const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 12,
    position: 'relative',
    flex: 1,
    paddingTop: 5, // Reduced padding top
    paddingBottom: 0,
    justifyContent: 'flex-start', // Changed from center to flex-start
    backgroundColor: '#fff',
  },
  relativeContainer: {
    position: 'relative',
  },
  inputStyle: {
    borderWidth: 1,
    borderColor: '#919AA2',
    borderRadius: 10,
    fontSize: 16,
    backgroundColor: 'white',
    height: 'auto',
    fontWeight: '400',
    color: '#000',
    paddingLeft: 45,
    paddingRight: 20,
    paddingVertical: 7,
  },
  searchIcon: {
    width: 18,
    height: 18,
    objectFit: 'contain',
  },
  searchIconArea: {
    position: 'absolute',
    top: 9,
    left: 15,
    width: 18, // Adjust width and height to cover the image area
    height: 18, // Adjust width and height to cover the image area
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 15,
    gap: 10,
  },
  avatar: {
    width: 40,
    height: 40,
    minWidth: 40,
    borderRadius: 20,
  },
  info: {
    justifyContent: 'center',
  },
  name: {
    fontSize: 16,
    fontWeight: '700',
    color: '#414141',
  },
  stats: {
    fontSize: 12,
    color: '#414141',
    fontWeight: '400',
  },
  nameInline: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 10,
  },
  blueAvatar: {
    width: 30,
    height: 30,
    minWidth: 30,
    borderRadius: 24,
  },
  contactItem: {
    fontSize: 14,
    fontWeight: '400',
    color: '#282828',
  },
  searchbar: {
    marginBottom: 30,
  },
  noContactsText: {
    fontSize: 14,
    fontWeight: '400',
    color: '#282828',
    textAlign: 'center',
  },
  scrollContent: {
    backgroundColor: '#fff',
    flex: 1,
    marginBottom: 10,
  },
  contactRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 8,
    paddingRight: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  deleteIconArea: {
    padding: 8,
  },
  addButton: {
    backgroundColor: '#007BFF',
    borderRadius: 25,
    width: 50,
    height: 50,
    justifyContent: 'center',
    alignItems: 'center',
    position: 'absolute',
    bottom: Platform.OS === 'ios' ? 120 : 100, // Higher position to avoid tab bar overlap
    right: 20,
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
});

export default ContactsList;
