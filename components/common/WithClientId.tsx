import React, {useState, useEffect} from 'react';
import {ActivityIndicator} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import PhoneNumberPrompt from './AddPhoneNumberPrompt';

const withClientId = (WrappedComponent: React.ComponentType<any>) => {
  return (props: any) => {
    const [clientId, setClientId] = useState<string | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [showPrompt, setShowPrompt] = useState<boolean>(false);

    useEffect(() => {
      const fetchClientId = async () => {
        const savedClientId = await AsyncStorage.getItem('client_id');
        if (savedClientId) {
          setClientId(savedClientId);
        }
        setLoading(false);
      };

      fetchClientId();
    }, []);

    const handleSave = (id: string) => {
      setClientId(id);
      setShowPrompt(false);
    };

    const checkClientId = () => {
      if (!clientId) {
        setShowPrompt(true);
        return false;
      }
      return true;
    };

    if (loading) {
      return <ActivityIndicator size="large" />;
    }

    if (showPrompt || !clientId) {
      return <PhoneNumberPrompt onSave={handleSave} />;
    }

    return (
      <WrappedComponent
        {...props}
        clientId={clientId}
        checkClientId={checkClientId}
      />
    );
  };
};

export default withClientId;
