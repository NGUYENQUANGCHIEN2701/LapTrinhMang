import { useCallback, useEffect, useState } from 'react';
import { globalWebSocketClient } from '../WebSocketClient';
import { MessageBuilder } from '../utils/MessageBuilder';
import { BasePacket, ConnectionStatus } from '../types';

export const useWebSocket = () => {
  const [status, setStatus] = useState<ConnectionStatus>(globalWebSocketClient.getStatus());
  const [lastMessage, setLastMessage] = useState<BasePacket | null>(null);
  const [reconnectCount, setReconnectCount] = useState<number>(0);

  useEffect(() => {
    // Auto connect if disconnected
    if (globalWebSocketClient.getStatus() === 'disconnected') {
      globalWebSocketClient.connect();
    }

    const unsubOpen = globalWebSocketClient.onOpen(() => {
      setStatus('connected');
      setReconnectCount(0);
    });

    const unsubMessage = globalWebSocketClient.onMessage((data) => {
      setLastMessage(data);
    });

    const unsubClose = globalWebSocketClient.onClose(() => {
      setStatus(globalWebSocketClient.getStatus());
      setReconnectCount(globalWebSocketClient.getReconnectAttempts());
    });

    const unsubError = globalWebSocketClient.onError(() => {
      setStatus('error');
      setReconnectCount(globalWebSocketClient.getReconnectAttempts());
    });

    const interval = setInterval(() => {
      setStatus(globalWebSocketClient.getStatus());
      setReconnectCount(globalWebSocketClient.getReconnectAttempts());
    }, 500);

    return () => {
      unsubOpen();
      unsubMessage();
      unsubClose();
      unsubError();
      clearInterval(interval);
    };
  }, []);

  const sendMessage = useCallback((type: string, payload?: any) => {
    const rawPayload = MessageBuilder.buildCustom(type, payload);
    globalWebSocketClient.sendRaw(rawPayload);
  }, []);

  return {
    status,
    sendMessage,
    lastMessage,
    reconnectCount,
    maxRetries: globalWebSocketClient.getMaxRetries(),
  };
};
