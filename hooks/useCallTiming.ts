import {useEffect, useState} from 'react';
import {NativeModules, NativeEventEmitter} from 'react-native';

const {CallActivityModule} = NativeModules;
const emitter = new NativeEventEmitter(CallActivityModule);

export function useCallTiming() {
  const [recordDisabled, setRecordDisabled] = useState<boolean>(true);

  const [callTime, setCallTime] = useState('Connecting...');

  useEffect(() => {
    const subscription = emitter.addListener('CallTimingUpdate', data => {
      if (data?.callTime) {
        setCallTime(data.callTime);
        if (
          data.callTime !== 'Connecting...' &&
          data.callTime !== 'Calling...'
        ) {
          setRecordDisabled(false);
        }
      }
    });

    return () => {
      subscription.remove();
    };
  }, []);

  return {callTime, recordDisabled};
}
