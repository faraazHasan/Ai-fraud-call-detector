import {useState, useEffect} from 'react';
import {NativeModules} from 'react-native';
import useWebSocket from './useWebSocket';
import {usePhoneNumber} from './usePhoneNumber';
const {CallActivityModule} = NativeModules;

const useOngoingCall = () => {
  const {phoneNumber} = usePhoneNumber();
  const [isRecording, setIsRecording] = useState<boolean>(false);
  const [speakerEnabled, setSpeakerEnabled] = useState<boolean>(false);
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [callSid, setCallSid] = useState<string | null>(null);

  const wsUrl = 'wss://ab7bd2f4e77c.ngrok-free.app/api/ws';
  // const wsUrl = 'wss://d2l9rjxlp3sze3.cloudfront.net/api/ws';
  const userPhoneNumber = phoneNumber; // Replace with actual user phone number
  const {ws, isFraud, setIsFraud, userId, isConnected} = useWebSocket({
    url: wsUrl,
    userPhoneNumber: userPhoneNumber,
  });

  console.log('userPhoneNumber>>>>>>>>>>>>>>>>>>>>>>>>', userPhoneNumber);

  // Get call SID when recording starts
  useEffect(() => {
    if (isRecording && !callSid) {
      // Try to get call SID from native module or generate one
      const generateCallSid = async () => {
        try {
          // Try to get actual call SID from native module if available
          const actualCallSid = await CallActivityModule.getCallSid?.();
          if (actualCallSid) {
            setCallSid(actualCallSid);
            return actualCallSid;
          }
        } catch (error) {
          // Could not get actual call SID, will generate one
        }

        // Generate a call SID if we can't get the real one
        const generatedSid = `CA${Date.now()}${Math.random()
          .toString(36)
          .substr(2, 9)}`;
        setCallSid(generatedSid);
        return generatedSid;
      };

      generateCallSid();
    }
  }, [isRecording, callSid, isConnected]);

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
        setCallSid(null); // Reset call SID when recording stops
      }
    } catch (error) {
      console.error('Failed to toggle recording:', error);
    }
  };
  const toggleSpeaker = async () => {
    setSpeakerEnabled(!speakerEnabled);
    try {
      await CallActivityModule.toggleSpeaker(!speakerEnabled);
    } catch (error) {
      console.error('Failed to toggle speaker:', error);
    }
  };

  const toggleMute = async () => {
    setIsMuted(!isMuted);
    try {
      await CallActivityModule.muteCall(!isMuted);
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
    userId,
    isConnected,
    callSid,
    userPhoneNumber,
  };
};

export default useOngoingCall;
