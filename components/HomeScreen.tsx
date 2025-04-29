import {
  StyleSheet,
  View,
  Dimensions,
  Text,
  ScrollView,
  TextInput,
  TouchableOpacity,
  Alert,
  Image,
} from 'react-native';
import {useState} from 'react';
import {make_call} from '../utils/native_modules/dialer_module';
import React from 'react';

const MakeCall = require('../assets/images/make-call.png');
const RemoveIcon = require('../assets/images/remove-icon.png');

const {height} = Dimensions.get('window');

export default function HomeScreen() {
  const [number, setNumber] = useState('');

  const dialPhoneNumber = () => {
    if (number.length > 0) {
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
  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.inputComntrolBox}>
        <TextInput
          style={styles.phoneNumberInput}
          value={number}
          editable={false}
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
    </ScrollView>
  );
}

const DialButton = ({digit, onPress}: any) => (
  <TouchableOpacity style={styles.dialButton} onPress={onPress}>
    <Text style={styles.dialButtonText}>{digit}</Text>
  </TouchableOpacity>
);

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 30,
    position: 'relative',
    flex: 1,
    paddingBottom: 30,
    justifyContent: 'flex-end',
    height: height,
    backgroundColor: 'white',
  },

  title: {
    fontSize: 30,
    marginBottom: 20,
    fontWeight: 'bold',
  },
  phoneNumberInput: {
    fontSize: 35,
    textAlign: 'center',
    width: '100%',
    borderRadius: 10,
    paddingVertical: 15,
    paddingLeft: 15,
    paddingRight: 50,
    color: '#414141',
  },
  dialPad: {
    flexDirection: 'column',
    alignItems: 'center',
    marginBottom: 20,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginBottom: 10,
  },
  dialButton: {
    backgroundColor: '#dfdfdf',
    padding: 10,
    margin: 10,
    borderRadius: 50,
    width: 80,
    height: 80,
    justifyContent: 'center',
    alignItems: 'center',
  },
  dialButtonText: {
    fontSize: 30,
    fontWeight: 'bold',
    color: '#414141',
    lineHeight: 40,
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
    marginBottom: 30,
    paddingBottom: 0,
  },
  removeIcon: {
    width: 25,
    height: 25,
    objectFit: 'contain',
  },
  makeCallImg: {
    width: 80,
    height: 80,
    alignSelf: 'center',
  },
});
