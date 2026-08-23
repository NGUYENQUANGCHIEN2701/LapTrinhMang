import React from 'react';
import { Link } from 'react-router-dom';
import { useMenuState } from '../hooks/useMenuState';

export const MainMenuScreen: React.FC = () => {
  const { menuItems } = useMenuState();

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '100vh',
      backgroundColor: '#121216',
      color: '#ffffff',
      fontFamily: 'Inter, system-ui, sans-serif',
      padding: '20px'
    }}>
      <div style={{
        background: 'rgba(30, 30, 36, 0.85)',
        backdropFilter: 'blur(10px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '16px',
        padding: '40px',
        maxWidth: '500px',
        width: '100%',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)',
        textAlign: 'center'
      }}>
        <h1 style={{
          fontSize: '2.5rem',
          margin: '0 0 10px 0',
          background: 'linear-gradient(135deg, #00e676, #00b0ff)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
          fontWeight: 800,
          letterSpacing: '1px'
        }}>
          TANKZORS PRO
        </h1>
        <p style={{ color: '#a0a0b0', marginBottom: '30px', fontSize: '0.95rem' }}>
          Game Chiến Đấu Xe Tăng PvE - Lập Trình Mạng
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {menuItems.map((item) => (
            <Link
              key={item.id}
              to={item.path}
              style={{
                display: 'block',
                padding: '16px 20px',
                background: '#22222a',
                color: '#ffffff',
                textDecoration: 'none',
                borderRadius: '10px',
                border: '1px solid #33333f',
                transition: 'all 0.2s ease',
                textAlign: 'left'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = '#00e676';
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 4px 16px rgba(0, 230, 118, 0.2)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = '#33333f';
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = 'none';
              }}
            >
              <div style={{ fontWeight: 600, fontSize: '1.05rem', color: '#00e676' }}>
                {item.label}
              </div>
              <div style={{ fontSize: '0.85rem', color: '#888899', marginTop: '4px' }}>
                {item.description}
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
};

export default MainMenuScreen;
