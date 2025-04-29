import {useState} from 'react';
import {NativeModules} from 'react-native';
import useWebSocket from './useWebSocket';
const {CallActivityModule} = NativeModules;

const useOngoingCall = () => {
  const [isRecording, setIsRecording] = useState<boolean>(false);
  const [speakerEnabled, setSpeakerEnabled] = useState<boolean>(false);
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const wsUrl = 'wss://d2l9rjxlp3sze3.cloudfront.net/api/ws';
  const {ws, isFraud, setIsFraud} = useWebSocket(wsUrl);

  const endCall = async () => {
    CallActivityModule.endCall()
      .then((response: any) => {
        console.log('Call ended successfully:', response);
        ws?.close();
      })
      .catch((error: any) => {
        console.error('Failed to end call:', error);
      });
  };
  const toggleRecordCall = async () => {
    try {
      const response = await CallActivityModule.toggleRecording();
      if (response === 'Recording started') {
        setIsRecording(true);
      } else if (response === 'Recording stopped') {
        setIsRecording(false);
      }
      console.log('Recording toggled successfully:', response);
    } catch (error) {
      console.error('Failed to toggle recording:', error);
    }
  };
  const toggleSpeaker = async () => {
    setSpeakerEnabled(!speakerEnabled);
    try {
      const response = await CallActivityModule.toggleSpeaker(!speakerEnabled);
      console.log('Speaker toggled successfully:', response);
    } catch (error) {
      console.error('Failed to toggle speaker:', error);
    }
  };

  const toggleMute = async () => {
    setIsMuted(!isMuted);
    try {
      const response = await CallActivityModule.muteCall(!isMuted);
      console.log('Mute toggled successfully:', response);
    } catch (error) {
      console.error('Failed to toggle mute:', error);
    }
  };

  return {
    isRecording,
    speakerEnabled,
    isMuted,
    isFraud,
    endCall,
    toggleRecordCall,
    toggleSpeaker,
    toggleMute,
    setIsFraud,
  };
};

export default useOngoingCall;
