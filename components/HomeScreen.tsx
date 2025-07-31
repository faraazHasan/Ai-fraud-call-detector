import {
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Alert,
  Image,
  FlatList,
  ActivityIndicator,
  Modal,
  Platform,
} from 'react-native';
import {useState, useEffect, useCallback, useRef} from 'react';
import {make_call} from '../utils/native_modules/dialer_module';
import React from 'react';
import {NativeModules} from 'react-native';
import debounce from 'lodash.debounce';
import useCallHistory from '../hooks/useCallHistory';
const {ContactsModule} = NativeModules;

const MakeCall = require('../assets/images/make-call.png');
const RemoveIcon = require('../assets/images/remove-icon.png');

export default function HomeScreen() {
  const [number, setNumber] = useState('');
  const [contacts, setContacts] = useState<any[]>([]);
  const [filteredContacts, setFilteredContacts] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [showAddContactModal, setShowAddContactModal] =
    useState<boolean>(false);
  const {recordOutgoingCall} = useCallHistory();

  // Fetch all contacts and check default dialer status on component mount
  useEffect(() => {
    fetchContacts();
  }, []);

  // Fetch contacts from the device
  const fetchContacts = () => {
    setIsLoading(true);
    ContactsModule.checkPermissionAndFetchContacts(
      (contactsList: any[]) => {
        setContacts(contactsList);
        setIsLoading(false);
      },
      (error: string) => {
        Alert.alert('Error', error);
        setIsLoading(false);
      },
    );
  };

  // Reference for the debounced function
  const debouncedFilterRef = useRef<any>(null);

  // Memoize the filter contacts function to avoid recreating it on every render
  const filterContactsByNumber = useCallback(
    (text: string) => {
      if (!text || text.length === 0) {
        setFilteredContacts([]);
        return;
      }

      const filtered = contacts.filter(
        contact =>
          contact.phoneNumbers &&
          contact.phoneNumbers.some((phone: string) => phone.includes(text)),
      );

      // Limit to top 3 matches
      setFilteredContacts(filtered.slice(0, 3));
    },
    [contacts],
  );

  // Set up the debounced function
  useEffect(() => {
    debouncedFilterRef.current = debounce((text: string) => {
      filterContactsByNumber(text);
    }, 300);

    // Clean up function
    return () => {
      if (debouncedFilterRef.current && debouncedFilterRef.current.cancel) {
        debouncedFilterRef.current.cancel();
      }
    };
  }, [filterContactsByNumber]);

  // Filter contacts when number changes
  useEffect(() => {
    if (number.length > 0 && debouncedFilterRef.current) {
      debouncedFilterRef.current(number);
    } else {
      setFilteredContacts([]);
    }
  }, [number]);

  // Add contact handler
  const handleAddContact = () => {
    setShowAddContactModal(true);
  };

  // Close add contact modal
  const closeAddContactModal = () => {
    setShowAddContactModal(false);
  };

  // After a contact is added, refresh the contacts list
  const handleContactAdded = () => {
    fetchContacts();
  };

  const dialPhoneNumber = () => {
    if (number.length > 0) {
      // Find if number matches a contact
      const matchingContact = contacts.find(
        contact =>
          contact.phoneNumbers &&
          contact.phoneNumbers.some(
            (num: any) =>
              num.number && 
              typeof num.number === 'string' &&
              num.number.replace(/\D/g, '') === number.replace(/\D/g, ''),
          ),
      );

      // Record the outgoing call in call history
      recordOutgoingCall(
        number,
        matchingContact ? matchingContact.displayName : undefined,
      );

      // Actually make the call
      make_call(number);
    } else {
      Alert.alert('Please enter a phone number');
    }
  };

  // Function to append a digit to the phone number
  const appendNumber = (digit: string) => {
    setNumber(number + digit);
  };

  const deleteLastDigit = () => {
    setNumber(number.slice(0, -1));
  };

  // Render a contact suggestion item
  const renderContactItem = ({item}: {item: any}) => (
    <TouchableOpacity
      style={styles.contactItem}
      onPress={() => {
        if (item.phoneNumbers && item.phoneNumbers.length > 0) {
          setNumber(item.phoneNumbers[0]);
        }
      }}>
      <Text style={styles.contactName}>{item.contactName}</Text>
      {item.phoneNumbers && item.phoneNumbers.length > 0 && (
        <Text style={styles.contactPhone}>{item.phoneNumbers[0]}</Text>
      )}
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <View style={styles.inputComntrolBox}>
        <TextInput
          style={styles.phoneNumberInput}
          value={number}
          onChangeText={setNumber}
          keyboardType="phone-pad"
          placeholder=""
        />
        {number.length > 0 && (
          <TouchableOpacity
            style={styles.touchableArea}
            onPress={deleteLastDigit}>
            <Image
              source={RemoveIcon}
              resizeMode="contain"
              style={styles.removeIcon}
            />
          </TouchableOpacity>
        )}
      </View>

      {/* Contact Suggestions */}
      {number.length > 0 && (
        <View style={styles.suggestionsContainer}>
          {isLoading ? (
            <ActivityIndicator size="small" color="#007BFF" />
          ) : (
            <>
              {filteredContacts.length > 0 ? (
                <FlatList
                  data={filteredContacts}
                  renderItem={renderContactItem}
                  keyExtractor={(item, index) => `contact-${index}`}
                  style={styles.suggestionsList}
                  scrollEnabled={true}
                  nestedScrollEnabled={true}
                />
              ) : (
                <TouchableOpacity
                  style={styles.addContactButton}
                  onPress={handleAddContact}>
                  <Text style={styles.addContactText}>Add to contacts</Text>
                </TouchableOpacity>
              )}
            </>
          )}
        </View>
      )}

      <View style={styles.dialPad}>
        <View style={styles.row}>
          <DialButton digit="1" onPress={() => appendNumber('1')} />
          <DialButton digit="2" onPress={() => appendNumber('2')} />
          <DialButton digit="3" onPress={() => appendNumber('3')} />
        </View>
        <View style={styles.row}>
          <DialButton digit="4" onPress={() => appendNumber('4')} />
          <DialButton digit="5" onPress={() => appendNumber('5')} />
          <DialButton digit="6" onPress={() => appendNumber('6')} />
        </View>
        <View style={styles.row}>
          <DialButton digit="7" onPress={() => appendNumber('7')} />
          <DialButton digit="8" onPress={() => appendNumber('8')} />
          <DialButton digit="9" onPress={() => appendNumber('9')} />
        </View>
        <View style={styles.row}>
          <DialButton digit="*" onPress={() => appendNumber('*')} />
          <DialButton digit="0" onPress={() => appendNumber('0')} />
          <DialButton digit="#" onPress={() => appendNumber('#')} />
        </View>
        <View style={styles.buttonRow}>
          {/* Centered Call button */}
          <TouchableOpacity onPress={dialPhoneNumber} style={styles.callButton}>
            <Image style={styles.makeCallImg} source={MakeCall} />
          </TouchableOpacity>

          {/* Positioned Delete button at the end of the row */}
          {/* {number.length > 0 && (
            <TouchableOpacity
              onPress={deleteLastDigit}
              style={styles.deleteButton}>
              <Text style={styles.deleteButtonText}>DEL</Text>
            </TouchableOpacity>
          )} */}
        </View>
      </View>

      {/* Add Contact Modal */}
      <AddContactModal
        visible={showAddContactModal}
        onClose={closeAddContactModal}
        onContactAdded={handleContactAdded}
        phoneNumber={number}
      />
    </View>
  );
}

const DialButton = ({digit, onPress}: any) => (
  <TouchableOpacity style={styles.dialButton} onPress={onPress}>
    <Text style={styles.dialButtonText}>{digit}</Text>
  </TouchableOpacity>
);

// Add Contact Modal component
const AddContactModal = ({
  visible,
  onClose,
  onContactAdded,
  phoneNumber,
}: {
  visible: boolean;
  onClose: () => void;
  onContactAdded: () => void;
  phoneNumber: string;
}) => {
  const [name, setName] = useState('');

  const handleAddContact = () => {
    if (!name.trim()) {
      Alert.alert('Error', 'Please enter a contact name');
      return;
    }

    ContactsModule.addContact(
      name,
      phoneNumber,
      () => {
        Alert.alert('Success', 'Contact added successfully');
        setName('');
        onContactAdded();
        onClose();
      },
      (error: string) => {
        Alert.alert('Error', error);
      },
    );
  };

  return (
    <Modal
      animationType="slide"
      transparent={true}
      visible={visible}
      onRequestClose={onClose}>
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={styles.modalTitle}>Add New Contact</Text>

          <Text style={styles.modalLabel}>Phone Number</Text>
          <TextInput
            value={phoneNumber}
            editable={false}
            style={styles.modalInput}
          />

          <Text style={styles.modalLabel}>Name</Text>
          <TextInput
            value={name}
            onChangeText={setName}
            style={styles.modalInput}
            placeholder="Enter contact name"
          />

          <View style={styles.modalButtons}>
            <TouchableOpacity
              style={[styles.modalButton, styles.cancelButton]}
              onPress={onClose}>
              <Text style={styles.cancelButtonText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.modalButton, styles.saveButton]}
              onPress={handleAddContact}>
              <Text style={styles.saveButtonText}>Save</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 15,
    position: 'relative',
    flex: 1,
    paddingBottom: Platform.OS === 'ios' ? 100 : 80, // Increased padding to avoid tab bar overlap
    justifyContent: 'flex-end',
    backgroundColor: 'white',
  },

  title: {
    fontSize: 24,
    marginBottom: 15,
    fontWeight: 'bold',
  },
  phoneNumberInput: {
    fontSize: 28, // Smaller font size
    textAlign: 'center',
    width: '100%',
    borderRadius: 10,
    paddingVertical: 8,
    paddingLeft: 15,
    paddingRight: 40,
    color: '#414141',
  },
  dialPad: {
    flexDirection: 'column',
    alignItems: 'center',
    marginBottom: 10,
    justifyContent: 'center',
    width: '100%',
    maxWidth: 320,
    alignSelf: 'center',
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
    width: '100%',
  },
  dialButton: {
    backgroundColor: '#dfdfdf',
    padding: 0,
    margin: 4,
    borderRadius: 50,
    width: '30%',
    aspectRatio: 1,
    justifyContent: 'center',
    alignItems: 'center',
    maxWidth: 65,
  },
  dialButtonText: {
    fontSize: 26,
    fontWeight: 'bold',
    color: '#414141',
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'center', // This centers the Call button
    alignItems: 'center',
    marginTop: 20,
    position: 'relative', // This ensures the delete button can be absolutely positioned
    width: '100%',
  },
  callButton: {
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1, // Ensures that Call button stays on top of the Delete button
  },
  deleteButton: {
    backgroundColor: 'grey',
    opacity: 0.6,
    padding: 10,
    borderRadius: 50,
    width: 50,
    height: 50,
    justifyContent: 'center',
    alignItems: 'center',
    position: 'absolute', // Position Delete button at the end of the row
    right: 50, // Position it to the far right
  },
  deleteButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  touchableArea: {
    position: 'absolute',
    top: 29,
    right: 15,
    width: 25, // Adjust width and height to cover the image area
    height: 25, // Adjust width and height to cover the image area
    zIndex: 1,
  },
  inputComntrolBox: {
    position: 'relative',
    borderBottomWidth: 1,
    borderBottomColor: '#D8D8D8',
    marginBottom: 15,
    paddingBottom: 0,
  },
  removeIcon: {
    width: 25,
    height: 25,
    objectFit: 'contain',
  },
  makeCallImg: {
    width: 60,
    height: 60,
    alignSelf: 'center',
  },
  // Contact suggestion styles
  suggestionsContainer: {
    marginBottom: 10,
    backgroundColor: '#f5f5f5',
    borderRadius: 10,
    padding: 8,
    maxHeight: 120, // Limit maximum height
  },
  suggestionsList: {
    maxHeight: 150,
  },
  contactItem: {
    padding: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  contactName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  contactPhone: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
  },
  addContactButton: {
    padding: 12,
    backgroundColor: '#e1f5fe',
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  addContactText: {
    color: '#0277bd',
    fontWeight: '600',
    fontSize: 14,
  },
  // Modal styles
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    width: '80%',
    backgroundColor: 'white',
    borderRadius: 10,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 15,
    textAlign: 'center',
    color: '#333',
  },
  modalLabel: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 5,
    color: '#555',
  },
  modalInput: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 5,
    padding: 10,
    marginBottom: 15,
    fontSize: 16,
  },
  modalButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 10,
  },
  modalButton: {
    flex: 1,
    padding: 12,
    borderRadius: 5,
    alignItems: 'center',
    marginHorizontal: 5,
  },
  saveButton: {
    backgroundColor: '#007BFF',
  },
  cancelButton: {
    backgroundColor: '#f0f0f0',
  },
  saveButtonText: {
    color: 'white',
    fontWeight: '600',
    fontSize: 16,
  },
  cancelButtonText: {
    color: '#333',
    fontWeight: '600',
    fontSize: 16,
  },
});
