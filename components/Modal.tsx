/* eslint-disable react-native/no-inline-styles */
import React, {useEffect} from 'react';
import {View, Text, StyleSheet, TouchableOpacity, Image} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import Modal from 'react-native-modal';
import Sound from 'react-native-sound';
const ScamIcon = require('../assets/images/scam-icon.png');

// Define the types for the props
interface CustomModalProps {
  isVisible: boolean;
  content: string;
  contentBrief: string;
  yesText: string;
  noText: string;
  onClose: () => void;
  onyesPress: () => void;
  onnoPress: () => void;
}

const CustomModal: React.FC<CustomModalProps> = ({
  isVisible,
  content,
  contentBrief,
  yesText,
  noText,
  onClose,
  onyesPress,
  onnoPress,
}) => {
  useEffect(() => {
    let beep: Sound;
    if (isVisible) {
      // Play the beep sound when the modal is opened
      beep = new Sound('beep.mp3', Sound.MAIN_BUNDLE, error => {
        if (error) {
          console.log('Failed to load the sound', error);
          return;
        }
        beep.play(success => {
          if (success) {
            console.log('Beep sound played');
          } else {
            console.log('Beep sound playback failed');
          }
        });
      });
      console.log('Beep sound:', beep);
    }
    return () => {
      if (beep) {
        beep.release();
      }
    };
  }, [isVisible]);

  return (
    <Modal
      isVisible={isVisible} // Controls visibility of the modal
      // onBackdropPress={onClose} // Close modal on backdrop press
      onBackButtonPress={onClose} // Close modal on back button press
      style={styles.modal} // Apply custom modal styling
    >
      <View style={styles.modalContent}>
        <LinearGradient
          colors={['#f03134', '#F42D2D', '#000000']} // More white at the top
          locations={[0, 0.3, 1]} // Adjust the transition
          start={{x: 0.5, y: 0}}
          end={{x: 0.5, y: 1}}
          style={{borderRadius: 10}}>
          <View
            style={{
              padding: 20,
            }}>
            <View style={styles.contentInline}>
              <Image
                source={ScamIcon}
                resizeMode="contain"
                style={styles.scamIcon}
              />
              <View>
                <Text style={styles.modalText}>{content}</Text>
                <Text style={styles.modalBrief}>{contentBrief}</Text>
              </View>
            </View>

            {/* Close button */}
            <View
              style={{flexDirection: 'row', justifyContent: 'space-between'}}>
              <TouchableOpacity
                onPress={onyesPress}
                style={{...styles.noButton, ...styles.yesButton}}>
                <Text style={styles.closeButtonText}>{yesText}</Text>
              </TouchableOpacity>
              <TouchableOpacity onPress={onnoPress} style={styles.noButton}>
                <Text style={styles.closeButtonText}>{noText}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </LinearGradient>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modal: {
    justifyContent: 'flex-start', // Align the modal at the top
    margin: 0, // Remove default margin/padding around modal
    top: 0, // Position at the top of the screen
  },
  modalContent: {
    backgroundColor: 'transparent', // Transparent background
    padding: 20,
    borderRadius: 10,
    width: '100%', // Make the modal span the entire width
  },

  modalText: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#ffffff',
    marginBottom: 5,
  },
  modalBrief: {
    fontSize: 16,
    color: '#ffffff',
    marginBottom: 0,
  },
  yesButton: {
    backgroundColor: '#F5413D',
  },
  noButton: {
    backgroundColor: '#2AB930',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 5,
    marginTop: 15,
    width: '45%',
  },
  closeButtonText: {
    color: 'white',
    fontSize: 16,
    textAlign: 'center',
  },
  scamIcon: {
    width: 80,
    minWidth: 80,
    height: 80,
    objectFit: 'contain',
  },
  contentInline: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 20,
    marginBottom: 20,
  },
});

export default CustomModal;
