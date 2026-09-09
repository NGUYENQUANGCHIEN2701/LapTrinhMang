import React from 'react';

interface MainMenuProps {
  onStartCampaign: () => void;
  connected: boolean;
}

export const MainMenu: React.FC<MainMenuProps> = ({ onStartCampaign, connected }) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        width: '100%',
        maxWidth: '800px',
        minHeight: '600px',
        backgroundColor: '#1a1e24',
        border: '3px solid #34495e',
        borderRadius: '16px',
        boxShadow: '0 20px 50px rgba(0, 0, 0, 0.7)',
        padding: '40px 30px',
        boxSizing: 'border-box',
        textAlign: 'center',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Background decoration grid */}
      <div
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundImage:
            'radial-gradient(#2c3e50 1px, transparent 1px), radial-gradient(#2c3e50 1px, #1a1e24 1px)',
          backgroundSize: '40px 40px',
          backgroundPosition: '0 0, 20px 20px',
          opacity: 0.15,
          pointerEvents: 'none',
        }}
      />

      {/* Main Title & Badge */}
      <div style={{ zIndex: 1, marginBottom: '32px' }}>
        <div
          style={{
            display: 'inline-block',
            padding: '4px 16px',
            backgroundColor: '#f1c40f22',
            border: '1px solid #f1c40f',
            borderRadius: '20px',
            color: '#f1c40f',
            fontSize: '12px',
            fontWeight: 700,
            letterSpacing: '2px',
            textTransform: 'uppercase',
            marginBottom: '12px',
          }}
        >
          Chiến Trường Xe Tăng 2D
        </div>
        <h1
          style={{
            fontSize: '48px',
            fontWeight: 900,
            margin: '0 0 8px 0',
            letterSpacing: '4px',
            background: 'linear-gradient(135deg, #ffffff 0%, #f1c40f 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            textShadow: '0 4px 20px rgba(241, 196, 15, 0.3)',
          }}
        >
          TANKZORS
        </h1>
        <p style={{ color: '#95a5a6', fontSize: '15px', margin: 0 }}>
          Hệ thống chiến đấu xe tăng thời gian thực với công nghệ WebSocket
        </p>
      </div>

      {/* Menu Options */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '14px',
          width: '100%',
          maxWidth: '380px',
          zIndex: 1,
          marginBottom: '36px',
        }}
      >
        {/* Campaign Mode Button */}
        <button
          id="btn-campaign-mode"
          onClick={onStartCampaign}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '12px',
            padding: '16px 28px',
            fontSize: '18px',
            fontWeight: 800,
            color: '#1a1e24',
            backgroundColor: '#f1c40f',
            border: 'none',
            borderRadius: '10px',
            cursor: 'pointer',
            boxShadow: '0 8px 24px rgba(241, 196, 15, 0.4)',
            transition: 'all 0.2s ease',
            textTransform: 'uppercase',
            letterSpacing: '1px',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.transform = 'translateY(-2px) scale(1.02)';
            e.currentTarget.style.boxShadow = '0 12px 28px rgba(241, 196, 15, 0.6)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.transform = 'none';
            e.currentTarget.style.boxShadow = '0 8px 24px rgba(241, 196, 15, 0.4)';
          }}
        >
          <span style={{ fontSize: '22px' }}>🎖️</span> Chế Độ Chiến Dịch (Campaign)
        </button>

        {/* Online 1v1 Mode Button (Disabled) */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '14px 20px',
            fontSize: '16px',
            fontWeight: 600,
            color: '#7f8c8d',
            backgroundColor: '#242b35',
            border: '1px solid #34495e',
            borderRadius: '10px',
            cursor: 'not-allowed',
            opacity: 0.8,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span>🌐</span> Đấu Mạng Trực Tuyến
          </div>
          <span
            style={{
              fontSize: '11px',
              padding: '2px 8px',
              backgroundColor: '#e67e2222',
              color: '#e67e22',
              borderRadius: '12px',
              border: '1px solid #e67e22',
            }}
          >
            Sắp ra mắt
          </span>
        </div>

        {/* Leaderboard Button (Disabled) */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '14px 20px',
            fontSize: '16px',
            fontWeight: 600,
            color: '#7f8c8d',
            backgroundColor: '#242b35',
            border: '1px solid #34495e',
            borderRadius: '10px',
            cursor: 'not-allowed',
            opacity: 0.8,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span>🏆</span> Bảng Xếp Hạng
          </div>
          <span
            style={{
              fontSize: '11px',
              padding: '2px 8px',
              backgroundColor: '#3498db22',
              color: '#3498db',
              borderRadius: '12px',
              border: '1px solid #3498db',
            }}
          >
            Sắp ra mắt
          </span>
        </div>
      </div>

      {/* Controls & Server Status Bar */}
      <div
        style={{
          zIndex: 1,
          width: '100%',
          maxWidth: '560px',
          backgroundColor: '#12161a',
          border: '1px solid #2c3e50',
          borderRadius: '10px',
          padding: '12px 20px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          fontSize: '13px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#bdc3c7' }}>
          <span>⌨️ Phím:</span>
          <b style={{ color: '#f1c40f' }}>W, A, S, D / Mũi tên</b>
          <span>•</span>
          <span>Bắn:</span>
          <b style={{ color: '#f1c40f' }}>Space / J</b>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span
            style={{
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              backgroundColor: connected ? '#2ecc71' : '#e74c3c',
              boxShadow: connected
                ? '0 0 8px #2ecc71'
                : '0 0 8px #e74c3c',
            }}
          />
          <span
            style={{
              color: connected ? '#2ecc71' : '#e74c3c',
              fontWeight: 600,
            }}
          >
            {connected ? 'Máy chủ sẵn sàng' : 'Chưa kết nối máy chủ'}
          </span>
        </div>
      </div>
    </div>
  );
};
