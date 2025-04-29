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
} from 'react-native';
import {NativeModules} from 'react-native';
import {make_call} from '../utils/native_modules/dialer_module';
import CustomTextInput from './common/CommonTextBox';
import {useForm} from 'react-hook-form';

const SearchIcon = require('../assets/images/search-icon.png');

const PlaceholderImg = require('../assets/images/placeholder-img.png');
const BluePlaceholder = require('../assets/images/blue-placeholder.png');

const {ContactsModule} = NativeModules;

const ContactsList: React.FC = () => {
  const [contacts, setContacts] = useState<any[]>([]);
  const [filteredContacts, setFilteredContacts] = useState<any[]>([]);
  const {control} = useForm();
  const [searchText, setSearchText] = useState<string>('');

  useEffect(() => {
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

  return (
    <View style={styles.container}>
      {/* Search Bar */}
      <View style={styles.searchbar}>
        <View style={{position: 'relative'}}>
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
            <Image
              source={SearchIcon}
              style={{width: 18, height: 18, objectFit: 'contain'}}
            />
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
      <ScrollView style={{backgroundColor: '#fff'}}>
        {filteredContacts && filteredContacts.length > 0 ? (
          filteredContacts.map((contact, index) => (
            <Pressable
              style={styles.nameInline}
              key={index}
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
          ))
        ) : (
          <Text style={styles.noContactsText}>No contacts available</Text>
        )}
      </ScrollView>
    </View>
  );
};
/** Styles */
const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 15,
    position: 'relative',
    flex: 1,
    paddingTop: 30,
    paddingBottom: 0,
    justifyContent: 'center',
    backgroundColor: '#fff',
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
    marginBottom: 20,
    gap: 10,
  },
  avatar: {
    width: 50,
    height: 50,
    minWidth: 50,
    borderRadius: 24,
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
});

export default ContactsList;
