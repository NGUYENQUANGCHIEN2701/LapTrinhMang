import React from 'react';
import { CAMPAIGN_LEVELS } from '../../../core/network/hooks/useGameSession';

interface GameModalProps {
  gameStatus: string;
  currentLevel: string;
  score?: number;
  onRestart: () => void;
  onNextLevel: () => void;
}

export const GameModal: React.FC<GameModalProps> = ({
  gameStatus,
  currentLevel,
  score = 0,
  onRestart,
  onNextLevel,
}) => {
  if (gameStatus !== 'VICTORY' && gameStatus !== 'DEFEAT') {
    return null;
  }

  const isVictory = gameStatus === 'VICTORY';
  const curIdx = CAMPAIGN_LEVELS.findIndex((l) => l.id === currentLevel);
  const curLevelConfig = CAMPAIGN_LEVELS[curIdx] ?? CAMPAIGN_LEVELS[0];
  const hasNextLevel = curIdx >= 0 && curIdx < CAMPAIGN_LEVELS.length - 1;
  const nextLevelConfig = hasNextLevel ? CAMPAIGN_LEVELS[curIdx + 1] : null;

  return (
    <div
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 50,
        borderRadius: '12px',
        animation: 'fadeIn 0.3s ease',
      }}
    >
      <div
        style={{
          backgroundColor: '#1e272e',
          border: isVictory ? '3px solid #f1c40f' : '3px solid #e74c3c',
          borderRadius: '16px',
          padding: '30px 40px',
          textAlign: 'center',
          maxWidth: '480px',
          width: '90%',
          boxShadow: isVictory
            ? '0 0 30px rgba(241, 196, 15, 0.4)'
            : '0 0 30px rgba(231, 76, 60, 0.4)',
        }}
      >
        {/* Header Star & Title */}
        {isVictory && (
          <div style={{ fontSize: '32px', marginBottom: '10px' }}>
            {'⭐'.repeat(curLevelConfig.stars)}
          </div>
        )}

        <h2
          style={{
            margin: '0 0 8px 0',
            fontSize: '32px',
            color: isVictory ? '#f1c40f' : '#e74c3c',
            textTransform: 'uppercase',
            letterSpacing: '2px',
            fontWeight: 800,
          }}
        >
          {isVictory ? 'Chiến Thắng!' : 'Thất Trận!'}
        </h2>

        <p style={{ margin: '0 0 16px 0', color: '#bdc3c7', fontSize: '15px', lineHeight: 1.5 }}>
          {isVictory
            ? `Chúc mừng bạn đã hoàn thành ${curLevelConfig.title}!`
            : 'Xe tăng của bạn đã bị tiêu diệt trong làn đạn.'}
        </p>

        {/* Score & Rewards Box */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-around',
            backgroundColor: 'rgba(0, 0, 0, 0.4)',
            padding: '12px',
            borderRadius: '8px',
            marginBottom: '24px',
          }}
        >
          <div>
            <div style={{ fontSize: '12px', color: '#7f8c8d', textTransform: 'uppercase' }}>Điểm số</div>
            <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#f1c40f' }}>{score}</div>
          </div>
          <div>
            <div style={{ fontSize: '12px', color: '#7f8c8d', textTransform: 'uppercase' }}>Màn chơi</div>
            <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#ecf0f1' }}>
              {curLevelConfig.title}
            </div>
          </div>
          {isVictory && (
            <div>
              <div style={{ fontSize: '12px', color: '#7f8c8d', textTransform: 'uppercase' }}>Đánh giá</div>
              <div style={{ fontSize: '16px', color: '#f1c40f' }}>
                {'⭐'.repeat(curLevelConfig.stars)}
              </div>
            </div>
          )}
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {isVictory && hasNextLevel && (
            <button
              onClick={onNextLevel}
              style={{
                backgroundColor: '#2ecc71',
                border: 'none',
                borderRadius: '8px',
                color: '#ffffff',
                fontSize: '16px',
                fontWeight: 'bold',
                padding: '12px 20px',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: '0 4px 12px rgba(46, 204, 113, 0.4)',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = '#27ae60';
                e.currentTarget.style.transform = 'translateY(-2px)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = '#2ecc71';
                e.currentTarget.style.transform = 'translateY(0)';
              }}
            >
              Màn tiếp theo: {nextLevelConfig?.title}
            </button>
          )}

          <button
            onClick={onRestart}
            style={{
              backgroundColor: isVictory ? '#34495e' : '#e74c3c',
              border: 'none',
              borderRadius: '8px',
              color: '#ffffff',
              fontSize: '15px',
              fontWeight: 600,
              padding: '10px 20px',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = isVictory ? '#2c3e50' : '#c0392b';
              e.currentTarget.style.transform = 'translateY(-2px)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = isVictory ? '#34495e' : '#e74c3c';
              e.currentTarget.style.transform = 'translateY(0)';
            }}
          >
            Chơi lại {curLevelConfig.title}
          </button>
        </div>
      </div>
    </div>
  );
};

