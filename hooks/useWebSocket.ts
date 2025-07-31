import {useEffect, useState, useCallback} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface UseWebSocketProps {
  url: string;
  userPhoneNumber: string | null;
}

const useWebSocket = ({ url, userPhoneNumber }: UseWebSocketProps) => {
  const [isFraud, setIsFraud] = useState<{ status: boolean; type: string }>({
    status: false,
    type: '',
  });
  const [ws, setWs] = useState<WebSocket | null>(null);
  const [userId, setUserId] = useState<string | null>(null);
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [reconnectAttempt, setReconnectAttempt] = useState<number>(0);

  // Generate or retrieve user ID
  const getUserId = useCallback(async (): Promise<string> => {
    try {
      let storedUserId = await AsyncStorage.getItem('fraud_detector_user_id');
      if (!storedUserId) {
        // Generate a unique user ID based on phone number and timestamp
        const timestamp = Date.now();
        const phoneHash = userPhoneNumber ? userPhoneNumber.replace(/[^0-9]/g, '') : 'unknown';
        storedUserId = `user_${phoneHash}_${timestamp}`;
        await AsyncStorage.setItem('fraud_detector_user_id', storedUserId);
      }
      return storedUserId;
    } catch (error) {
      console.error('Error managing user ID:', error);
      // Fallback to a session-based ID
      return `user_${userPhoneNumber || 'unknown'}_${Date.now()}`;
    }
  }, [userPhoneNumber]);

  useEffect(() => {
    if (!userPhoneNumber) {
      console.warn('WebSocket: No userPhoneNumber provided, skipping connection.');
      return;
    }

    let websocket: WebSocket | null = null;
    let reconnectTimeout: NodeJS.Timeout | null = null;
    let shouldReconnect = true;

    const initializeConnection = async () => {
      try {
        const currentUserId = await getUserId();
        setUserId(currentUserId);

        // Create WebSocket URL with user identification
        const wsUrl = `${url}?user_id=${encodeURIComponent(currentUserId)}&phone_number=${encodeURIComponent(userPhoneNumber)}`;
        console.log(`[WebSocket] Connecting to: ${wsUrl}`);
        websocket = new WebSocket(wsUrl);

        websocket.onopen = () => {
          setIsConnected(true);
          setReconnectAttempt(0);
          console.log('[WebSocket] Connected');
        };

        websocket.onmessage = event => {
          try {
            const data = JSON.parse(event.data);
            console.log('[WebSocket] Message received:', data);
            if (data.is_fraud) {
              console.log(`[WebSocket] Fraud detected: ${data.fraud_type}`);
              setIsFraud({
                status: data.is_fraud,
                type: data.fraud_type?.toUpperCase() || 'UNKNOWN',
              });
            }
          } catch (parseError) {
            console.error('[WebSocket] Error parsing message:', parseError);
          }
        };

        websocket.onclose = (event) => {
          setIsConnected(false);
          setWs(null);
          console.warn('[WebSocket] Connection closed:', event.reason || event.code);
          if (shouldReconnect) {
            // Exponential backoff: min 2s, max 30s
            const nextAttempt = Math.min(30000, Math.pow(2, reconnectAttempt + 1) * 1000);
            console.log(`[WebSocket] Reconnecting in ${nextAttempt / 1000}s...`);
            reconnectTimeout = setTimeout(() => {
              setReconnectAttempt((prev) => prev + 1);
            }, nextAttempt);
          }
        };

        websocket.onerror = error => {
          setIsConnected(false);
          setWs(null);
          console.error('[WebSocket] Error:', error);
        };

        setWs(websocket);
      } catch (error) {
        console.error('[WebSocket] Initialization error:', error);
      }
    };

    initializeConnection();

    return () => {
      shouldReconnect = false;
      if (reconnectTimeout) clearTimeout(reconnectTimeout);
      if (websocket) {
        websocket.onopen = null;
        websocket.onmessage = null;
        websocket.onclose = null;
        websocket.onerror = null;
        websocket.close();
        setIsConnected(false);
        setWs(null);
        console.log('[WebSocket] Cleaned up connection');
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [url, userPhoneNumber, getUserId, reconnectAttempt]);

  // Utility to send a message (for debugging or feature use)
  const sendMessage = useCallback(
    (msg: any) => {
      if (ws && isConnected) {
        try {
          ws.send(JSON.stringify(msg));
          console.log('[WebSocket] Sent message:', msg);
        } catch (err) {
          console.error('[WebSocket] Send error:', err);
        }
      } else {
        console.warn('[WebSocket] Cannot send, not connected.');
      }
    },
    [ws, isConnected]
  );

  return {
    isFraud,
    setIsFraud,
    ws,
    userId,
    isConnected,
    sendMessage,
    reconnectAttempt,
  };
};

export default useWebSocket;
