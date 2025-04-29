/* eslint-disable react-native/no-inline-styles */
import {useEffect, useState} from 'react';
import * as React from 'react';
import {
  ScrollView,
  View,
  NativeModules,
  Dimensions,
  Image,
  Text,
  StyleSheet,
  TouchableOpacity,
} from 'react-native';

const CallIcon = require('../assets/images/call-icon.png');
const EndCallImg = require('../assets/images/end-call.png');
const PlaceholderImg = require('../assets/images/placeholder-img.png');

const {height} = Dimensions.get('window');

const {CallActivityModule} = NativeModules;

function IncomingCall(): React.JSX.Element {
  const [, setAnswered] = useState(false);
  const [callleInfo, setCallerInfo] = useState<{
    phoneNumber: string;
    callerName: string;
  }>({phoneNumber: '', callerName: ''});

  const getCallerDetails = async () => {
    CallActivityModule.getCallerDetails()
      .then((response: any) => {
        console.log('Caller details:', response);
        setCallerInfo({
          phoneNumber: response.phoneNumber,
          callerName: response.callerName,
        });
      })
      .catch((error: any) => {
        console.error('Failed to get caller details:', error);
      });
  };

  const answerCall = async () => {
    CallActivityModule.answerCall()
      .then((message: any) => {
        console.log('Call started successfully:', message);
        if (message === 'answered') {
          setAnswered(true);
        }
      })
      .catch((error: any) => {
        console.error('Failed to end call:', error);
      });
  };

  const endCall = async () => {
    CallActivityModule.endCall()
      .then((response: any) => {
        console.log('Call ended successfully:', response);
      })
      .catch((error: any) => {
        console.error('Failed to end call:', error);
      });
  };

  useEffect(() => {
    getCallerDetails();
  }, []);

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
              <View
                style={{
                  position: 'relative',
                  width: 120,
                  height: 120,
                }}>
                <Image source={PlaceholderImg} style={styles.avatar} />
              </View>
            </View>
            <View style={styles.pageHeading}>
              {callleInfo.phoneNumber ? (
                <Text style={styles.h1}>{callleInfo.phoneNumber}</Text>
              ) : null}
              {callleInfo.callerName ? (
                <Text style={styles.name}>{callleInfo.callerName}</Text>
              ) : null}
            </View>
          </View>
          <View>
            <Text
              style={{
                textAlign: 'center',
                color: '#292929',
                fontSize: 14,
                fontWeight: '500',
                marginBottom: 0,
              }}>
              Incoming Call...
            </Text>
            <View
              style={{
                flexDirection: 'row',
                justifyContent: 'space-between',
              }}>
              <TouchableOpacity onPress={endCall}>
                <Image
                  source={EndCallImg}
                  style={{width: 170, height: 170, alignSelf: 'center'}}
                />
                <Text
                  style={{
                    textAlign: 'center',
                    color: '#000000',
                    fontSize: 14,
                    fontWeight: '500',
                    marginTop: -30,
                  }}>
                  Decline
                </Text>
              </TouchableOpacity>
              <TouchableOpacity onPress={answerCall}>
                <Image
                  source={CallIcon}
                  style={{width: 170, height: 170, alignSelf: 'center'}}
                />
                <Text
                  style={{
                    textAlign: 'center',
                    color: '#000000',
                    fontSize: 14,
                    fontWeight: '500',
                    marginTop: -30,
                  }}>
                  Accept
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </ScrollView>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 30,
    position: 'relative',
    flex: 1,
    paddingTop: 100,
    paddingBottom: 60,
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
});

export default IncomingCall;
