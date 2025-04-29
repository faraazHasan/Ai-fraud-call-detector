import { useState, useEffect } from 'react';
import { NativeModules } from 'react-native';

const { CallActivityModule } = NativeModules;

type Participant = {
  phoneNumber: string;
  callerName: string;
};

type CallerInfo =
  | {
      type: 'single';
      phoneNumber: string;
      callerName: string;
    }
  | {
      type: 'conference';
      participants: Participant[];
    }
  | null;

export function useCallerDetails() {
  const [callerInfo, setCallerInfo] = useState<CallerInfo>(null);
  const [error, setError] = useState<null | string>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const getCallerDetails = async () => {
    setLoading(true);
    try {
      const response = await CallActivityModule.getCallerDetails();

      if (response.type === 'conference' && Array.isArray(response.participants)) {
        setCallerInfo({
          type: 'conference',
          participants: response.participants,
        });
      } else {
        setCallerInfo({
          type: 'single',
          phoneNumber: response.phoneNumber,
          callerName: response.callerName,
        });
      }

      setError(null);
    } catch (err: any) {
      console.error('getCallerDetails error:', err);
      setCallerInfo(null);
      setError(err?.message || 'Unknown error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    getCallerDetails();
  }, []);

  return { callerInfo, loading, error, refresh: getCallerDetails };
}
