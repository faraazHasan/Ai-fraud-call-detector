import React from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  Alert,
  Modal,
} from 'react-native';
import {NativeModules} from 'react-native';
import CustomTextInput from './common/CommonTextBox';
import {useForm} from 'react-hook-form';

const {ContactsModule} = NativeModules;

interface AddContactModalProps {
  visible: boolean;
  onClose: () => void;
  onContactAdded: () => void;
}

const AddContactModal: React.FC<AddContactModalProps> = ({
  visible,
  onClose,
  onContactAdded,
}) => {
  const {
    control,
    handleSubmit,
    formState: {errors},
    reset,
  } = useForm({
    defaultValues: {
      name: '',
      phoneNumber: '',
    },
  });

  const onSubmit = (data: any) => {
    console.log('Form submitted with data:', data);

    // Validate data
    if (!data.name || !data.phoneNumber) {
      Alert.alert('Error', 'Name and phone number are required');
      return;
    }

    try {
      // Add contact using native module
      ContactsModule.addContact(
        data.name,
        data.phoneNumber,
        (_: string) => {
          // Success callback
          Alert.alert('Success', 'Contact added successfully');
          reset(); // Reset form
          onContactAdded(); // Refresh contacts list
          onClose(); // Close modal
        },
        (error: string) => {
          // Error callback
          console.error('Error adding contact:', error);
          Alert.alert('Error', error);
        },
      );
    } catch (e) {
      console.error('Exception when adding contact:', e);
      Alert.alert('Error', String(e));
    }
  };

  return (
    <Modal
      animationType="slide"
      transparent={true}
      visible={visible}
      onRequestClose={onClose}>
      <View style={styles.centeredView}>
        <View style={styles.modalView}>
          <Text style={styles.modalTitle}>Add New Contact</Text>

          <CustomTextInput
            placeHolder="Name"
            style={styles.input}
            keyboardType="default"
            name="name"
            control={control}
            error={errors.name}
          />
          {errors.name && (
            <Text style={styles.errorText}>Name is required</Text>
          )}

          <CustomTextInput
            placeHolder="Phone Number"
            style={styles.input}
            keyboardType="phone-pad"
            name="phoneNumber"
            control={control}
            error={errors.phoneNumber}
          />
          {errors.phoneNumber && (
            <Text style={styles.errorText}>Phone number is required</Text>
          )}

          <View style={styles.buttonContainer}>
            <TouchableOpacity
              style={[styles.button, styles.cancelButton]}
              onPress={onClose}>
              <Text style={[styles.buttonText, {color: '#333'}]}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.button, styles.addButton]}
              onPress={handleSubmit(onSubmit)}>
              <Text style={styles.buttonText}>Add Contact</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  centeredView: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  modalView: {
    margin: 20,
    backgroundColor: 'white',
    borderRadius: 20,
    padding: 25,
    width: '85%',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 20,
    textAlign: 'center',
    color: '#414141',
  },
  input: {
    borderWidth: 1,
    borderColor: '#919AA2',
    borderRadius: 10,
    fontSize: 16,
    backgroundColor: 'white',
    height: 'auto',
    fontWeight: '400',
    color: '#000',
    paddingLeft: 15,
    paddingRight: 15,
    paddingVertical: 10,
    marginBottom: 15,
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 20,
  },
  button: {
    borderRadius: 10,
    padding: 15,
    flex: 1,
    marginHorizontal: 5,
    alignItems: 'center',
  },
  cancelButton: {
    backgroundColor: '#e0e0e0',
  },
  addButton: {
    backgroundColor: '#007BFF',
  },
  buttonText: {
    color: '#fff',
    fontWeight: '600',
    fontSize: 16,
  },
  errorText: {
    color: 'red',
    fontSize: 12,
    marginTop: -10,
    marginBottom: 10,
    marginLeft: 5,
  },
});

export default AddContactModal;
