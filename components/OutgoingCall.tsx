/* eslint-disable react-native/no-inline-styles */
import {
  StyleSheet,
  View,
  Image,
  Text,
  Dimensions,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import CallDuration from './CountdownTimer';
import React from 'react';
import CustomModal from './Modal';
import {useCallTiming} from '../hooks/useCallTiming';
import {useCallerDetails} from '../hooks/useCallerDetails';
import useOngoingCall from '../hooks/useOngoingCall';
const EndCallImg = require('../assets/images/end-call.png');
const PlaceholderImg = require('../assets/images/placeholder-img.png');

const {height} = Dimensions.get('window');

const SpeakerIcon = require('../assets/images/speaker-icon.png');
const MuteIcon = require('../assets/images/mic-icon.png');
const RecodingIcon = require('../assets/images/recoding-icon.png');

const SpeakerIconActive = require('../assets/images/speaker-icon-active.png');
const MuteIconActive = require('../assets/images/mic-icon-active.png');
const RecodingIconActive = require('../assets/images/recoding-icon-active.png');

function OutgoingCall(): React.JSX.Element {
  const {callerInfo, loading, error} = useCallerDetails();
  const {callTime, recordDisabled} = useCallTiming();
  const {
    isRecording,
    speakerEnabled,
    isMuted,
    isFraud,
    endCall,
    toggleRecordCall,
    toggleSpeaker,
    toggleMute,
    setIsFraud,
  } = useOngoingCall();

  if (loading) {
    return <Text>Loading caller info...</Text>;
  }
  if (error) {
    return <Text>Error: {error}</Text>;
  }

  return (
    <>
      <ScrollView style={{backgroundColor: '#fafaff'}}>
        <View style={styles.container}>
          <View>
            <View
              style={{
                marginBottom: 20,
                flexDirection: 'row',
                justifyContent: 'center',
              }}>
              <View style={{position: 'relative', width: 120, height: 120}}>
                <Image source={PlaceholderImg} style={styles.avatar} />
              </View>
            </View>

            {callerInfo?.type === 'single' && (
              <>
                <View style={styles.pageHeading}>
                  <Text style={styles.h1}>{callerInfo.phoneNumber}</Text>
                </View>
                <Text style={styles.name}>{callerInfo.callerName}</Text>
              </>
            )}
            {callerInfo?.type === 'conference' &&
              callerInfo.participants.map((p, index) => (
                <View style={styles.pageHeading} key={index}>
                  <Text style={styles.h1}>
                    {p.phoneNumber === '+18452998019'
                      ? 'Fraud Detector'
                      : p.phoneNumber}
                  </Text>
                  <Text style={styles.name}>{p.callerName}</Text>
                </View>
              ))}
          </View>

          <View>
            <CallDuration callDuration={callTime} />
            <View
              style={{flexDirection: 'row', justifyContent: 'center', gap: 35}}>
              <TouchableOpacity onPress={toggleSpeaker}>
                <Image
                  source={!speakerEnabled ? SpeakerIcon : SpeakerIconActive}
                  style={{width: 70, height: 70, alignSelf: 'center'}}
                />
              </TouchableOpacity>
              <TouchableOpacity onPress={toggleMute}>
                <Image
                  source={!isMuted ? MuteIcon : MuteIconActive}
                  style={{width: 70, height: 70, alignSelf: 'center'}}
                />
              </TouchableOpacity>
              <TouchableOpacity
                disabled={recordDisabled}
                onPress={toggleRecordCall}>
                <Image
                  source={!isRecording ? RecodingIcon : RecodingIconActive}
                  style={{width: 70, height: 70, alignSelf: 'center'}}
                />
              </TouchableOpacity>
            </View>

            <View
              style={{
                flexDirection: 'row',
                justifyContent: 'center',
                marginBottom: 0,
              }}>
              <TouchableOpacity onPress={endCall}>
                <Image
                  source={EndCallImg}
                  style={{width: 170, height: 170, alignSelf: 'center'}}
                />
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </ScrollView>
      <CustomModal
        isVisible={isFraud.status}
        content={'Scam Alert'}
        contentBrief={isFraud.type}
        yesText="Hang Up"
        noText="Continue Call"
        onnoPress={() => {
          setIsFraud({status: false, type: '' });
          toggleRecordCall();
        }}
        onClose={() => setIsFraud({status: false, type: '' })}
        onyesPress={() => {
          setIsFraud({status: false, type: '' });
          endCall();
        }}
      />
    </>
  );
}

export default OutgoingCall;

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 30,
    position: 'relative',
    flex: 1,
    paddingTop: 100,
    paddingBottom: 10,
    justifyContent: 'space-between',
    minHeight: height,
  },
  pageHeading: {
    marginBottom: 20,
    textAlign: 'center',
  },
  h1: {
    color: '#000000',
    fontWeight: '500',
    fontSize: 20,
    lineHeight: 28,
    marginBottom: 5,
    textAlign: 'center',
  },
  avatar: {
    width: 120,
    height: 120,
    minHeight: 120,
    objectFit: 'cover',
    borderRadius: 100,
  },
  name: {
    fontSize: 14,
    color: '#000000',
    textAlign: 'center',
    marginBottom: 20,
    fontWeight: '500',
  },
  timer: {
    textAlign: 'center',
    color: '#000',
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 20,
  },
});
