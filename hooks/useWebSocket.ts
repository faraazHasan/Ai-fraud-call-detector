import {useEffect, useState} from 'react';

const useWebSocket = (url: string) => {
  const [isFraud, setIsFraud] = useState<{status: boolean; type: string}>({
    status: false,
    type: '',
  });
  const [ws, setWs] = useState<WebSocket | null>(null);

  useEffect(() => {
    let websocket: WebSocket | null = null;

    try {
      websocket = new WebSocket(url);
      websocket.onopen = () => {
        console.log('Connected to WebSocket');
      };

      websocket.onmessage = event => {
        const data = JSON.parse(event.data);
        if (data.is_fraud) {
          console.log(data);
          setIsFraud({
            status: data.is_fraud,
            type: data.fraud_type.toUpperCase(),
          });
          if (websocket) {
            websocket.close();
          }
        }
      };

      websocket.onclose = () => {
        console.log('WebSocket Disconnected');
      };

      websocket.onerror = error => {
        console.error('WebSocket Error:', error);
      };

      setWs(websocket);
    } catch (error) {
      console.error('WebSocket Error:', error);
      return;
    }

    return () => {
      if (websocket) {
        websocket.close();
      }
    };
  }, [url]);

  return {isFraud, setIsFraud, ws};
};

export default useWebSocket;
