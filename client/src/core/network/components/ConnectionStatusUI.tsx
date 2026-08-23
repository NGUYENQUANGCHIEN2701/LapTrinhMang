import React from 'react';
import { useWebSocket } from '../hooks/useWebSocket';

export const ConnectionStatusUI: React.FC = () => {
  const { status, reconnectCount, maxRetries } = useWebSocket();

  const getStatusBadge = () => {
    switch (status) {
      case 'connecting':
        return {
          text: reconnectCount > 0
            ? `Đang thử kết nối lại (${reconnectCount}/${maxRetries})...`
            : 'Đang kết nối...',
          bgColor: '#ff980022',
          color: '#ff9800',
          borderColor: '#ff9800',
        };
      case 'connected':
        return {
          text: 'Đã kết nối',
          bgColor: '#00e67622',
          color: '#00e676',
          borderColor: '#00e676',
        };
      case 'disconnected':
        return {
          text: reconnectCount >= maxRetries
            ? `Mất kết nối (Thử lại ${reconnectCount}/${maxRetries} thất bại)`
            : 'Mất kết nối',
          bgColor: '#ff525222',
          color: '#ff5252',
          borderColor: '#ff5252',
        };
      case 'error':
        return {
          text: 'Lỗi kết nối server',
          bgColor: '#ff174422',
          color: '#ff1744',
          borderColor: '#ff1744',
        };
    }
  };

  const badge = getStatusBadge();

  return (
    <div style={{
      position: 'fixed',
      top: '16px',
      left: '16px',
      zIndex: 9999,
      display: 'flex',
      alignItems: 'center',
      gap: '8px',
      padding: '8px 14px',
      borderRadius: '20px',
      backgroundColor: badge.bgColor,
      color: badge.color,
      border: `1px solid ${badge.borderColor}`,
      fontFamily: 'Inter, system-ui, sans-serif',
      fontSize: '0.85rem',
      fontWeight: 600,
      backdropFilter: 'blur(8px)',
      boxShadow: '0 4px 16px rgba(0, 0, 0, 0.4)',
      pointerEvents: 'none',
      transition: 'all 0.3s ease'
    }}>
      {badge.text}
    </div>
  );
};
