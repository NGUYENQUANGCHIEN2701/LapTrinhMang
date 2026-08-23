import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCampaignProgress } from '../hooks/useCampaignProgress';

export const CampaignScreen: React.FC = () => {
  const { levels } = useCampaignProgress();
  const navigate = useNavigate();

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      minHeight: '100vh',
      backgroundColor: '#121216',
      color: '#ffffff',
      fontFamily: 'Inter, system-ui, sans-serif',
      padding: '40px 20px'
    }}>
      <div style={{ maxWidth: '700px', width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px' }}>
          <h1 style={{
            margin: 0,
            fontSize: '2rem',
            background: 'linear-gradient(135deg, #00e676, #00b0ff)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}>
            Chế độ Chiến dịch (Campaign)
          </h1>
          <Link to="/" style={{ color: '#00e676', textDecoration: 'none', fontSize: '0.95rem' }}>
            ← Quay lại Menu
          </Link>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {levels.map((lvl) => (
            <div
              key={lvl.id}
              style={{
                background: '#1e1e24',
                borderRadius: '12px',
                padding: '24px',
                border: lvl.isUnlocked ? '1px solid #00e676' : '1px solid #33333f',
                opacity: lvl.isUnlocked ? 1 : 0.6,
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}
            >
              <div>
                <h3 style={{ margin: '0 0 8px 0', fontSize: '1.2rem', color: lvl.isUnlocked ? '#ffffff' : '#888899' }}>
                  {lvl.name}
                </h3>
                <p style={{ margin: '0 0 12px 0', fontSize: '0.9rem', color: '#aaaabb' }}>
                  {lvl.description}
                </p>
                <div style={{ display: 'flex', gap: '16px', fontSize: '0.85rem' }}>
                  <span style={{ color: '#ffeb3b' }}>⭐ Sao: {lvl.starsEarned}/3</span>
                  <span style={{ color: '#00e676' }}>💰 Thưởng: +${lvl.rewardMoney}</span>
                </div>
              </div>

              <button
                disabled={!lvl.isUnlocked}
                onClick={() => navigate(`/game/campaign?level=${lvl.id}`)}
                style={{
                  padding: '12px 24px',
                  borderRadius: '8px',
                  border: 'none',
                  background: lvl.isUnlocked ? 'linear-gradient(135deg, #00e676, #00b0ff)' : '#33333f',
                  color: lvl.isUnlocked ? '#000000' : '#888899',
                  fontWeight: 'bold',
                  cursor: lvl.isUnlocked ? 'pointer' : 'not-allowed',
                  fontSize: '0.95rem'
                }}
              >
                {lvl.isUnlocked ? 'Vào Trận' : 'Khóa'}
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default CampaignScreen;
