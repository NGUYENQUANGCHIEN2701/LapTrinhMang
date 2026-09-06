import React from 'react';
import { CAMPAIGN_LEVELS, CampaignLevelConfig } from '../../../core/network/hooks/useGameSession';

interface LevelSelectorProps {
  currentLevel: string;
  unlockedLevels: string[];
  onSelectLevel: (levelId: string) => void;
  onResetProgress?: () => void;
}

export const LevelSelector: React.FC<LevelSelectorProps> = ({
  currentLevel,
  unlockedLevels,
  onSelectLevel,
  onResetProgress,
}) => {
  const getDifficultyColor = (difficulty: CampaignLevelConfig['difficulty']) => {
    switch (difficulty) {
      case 'Dễ':
        return '#2ecc71';
      case 'Trung bình':
        return '#e67e22';
      case 'Khó':
        return '#e74c3c';
      default:
        return '#f1c40f';
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '10px',
        marginBottom: '16px',
        width: '100%',
        maxWidth: '800px',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          width: '100%',
          padding: '0 4px',
        }}
      >
        <div>
          <span style={{ fontSize: '15px', fontWeight: 600, color: '#ecf0f1', textTransform: 'uppercase', letterSpacing: '1px' }}>
            Chọn Màn Chơi Campaign
          </span>
        </div>

        {onResetProgress && (
          <button
            onClick={() => {
              if (window.confirm('Bạn có chắc chắn muốn đặt lại tiến trình chiến dịch về Màn 1?')) {
                onResetProgress();
              }
            }}
            title="Đặt lại tiến trình về Màn 1"
            style={{
              background: 'transparent',
              border: '1px solid #7f8c8d',
              borderRadius: '4px',
              color: '#bdc3c7',
              fontSize: '12px',
              padding: '3px 8px',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.borderColor = '#e74c3c';
              e.currentTarget.style.color = '#e74c3c';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.borderColor = '#7f8c8d';
              e.currentTarget.style.color = '#bdc3c7';
            }}
          >
            Reset Tiến Trình
          </button>
        )}
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '12px',
          width: '100%',
        }}
      >
        {CAMPAIGN_LEVELS.map((level) => {
          const isUnlocked = unlockedLevels.includes(level.id);
          const isActive = currentLevel === level.id;
          const starsString = '⭐'.repeat(level.stars);

          return (
            <button
              key={level.id}
              disabled={!isUnlocked}
              onClick={() => isUnlocked && onSelectLevel(level.id)}
              style={{
                position: 'relative',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'flex-start',
                padding: '12px 14px',
                borderRadius: '8px',
                backgroundColor: isActive ? '#1e272e' : isUnlocked ? '#2c3e50' : '#1a1e24',
                border: isActive
                  ? '2px solid #f1c40f'
                  : isUnlocked
                  ? '1px solid #34495e'
                  : '1px dashed #3a3f47',
                color: isUnlocked ? '#ffffff' : '#7f8c8d',
                cursor: isUnlocked ? 'pointer' : 'not-allowed',
                opacity: isUnlocked ? 1 : 0.55,
                boxShadow: isActive ? '0 0 12px rgba(241, 196, 15, 0.4)' : 'none',
                transition: 'all 0.2s ease',
                textAlign: 'left',
              }}
              onMouseEnter={(e) => {
                if (isUnlocked && !isActive) {
                  e.currentTarget.style.borderColor = '#f1c40f';
                  e.currentTarget.style.transform = 'translateY(-2px)';
                }
              }}
              onMouseLeave={(e) => {
                if (isUnlocked && !isActive) {
                  e.currentTarget.style.borderColor = '#34495e';
                  e.currentTarget.style.transform = 'translateY(0)';
                }
              }}
            >
              {/* Top Row: Title and Lock/Active indicator */}
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  width: '100%',
                  marginBottom: '6px',
                }}
              >
                <span style={{ fontSize: '14px', fontWeight: 'bold', color: isActive ? '#f1c40f' : '#ecf0f1' }}>
                  {level.title}
                </span>

                {isActive ? (
                  <span
                    style={{
                      fontSize: '11px',
                      backgroundColor: '#f1c40f',
                      color: '#000000',
                      padding: '2px 6px',
                      borderRadius: '4px',
                      fontWeight: 'bold',
                    }}
                  >
                    Đang chơi
                  </span>
                ) : !isUnlocked ? (
                  <span style={{ fontSize: '11px', color: '#7f8c8d', backgroundColor: 'rgba(255,255,255,0.06)', padding: '2px 6px', borderRadius: '4px' }} title="Cần hoàn thành màn trước để mở khóa">
                    Khóa
                  </span>
                ) : (
                  <span style={{ fontSize: '12px', color: '#2ecc71', fontWeight: 600 }}>
                    Sẵn sàng
                  </span>
                )}
              </div>

              {/* Middle Row: Difficulty & Stars */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                <span
                  style={{
                    fontSize: '11px',
                    fontWeight: 600,
                    color: getDifficultyColor(level.difficulty),
                    backgroundColor: 'rgba(0,0,0,0.3)',
                    padding: '2px 6px',
                    borderRadius: '4px',
                  }}
                >
                  {level.difficulty} ({level.enemiesCount} AI)
                </span>
                <span style={{ fontSize: '11px' }}>{starsString}</span>
              </div>

              {/* Description */}
              <span
                style={{
                  fontSize: '11px',
                  color: isUnlocked ? '#bdc3c7' : '#57606f',
                  lineHeight: '1.3',
                }}
              >
                {isUnlocked ? level.desc : 'Hoàn thành màn trước để mở khóa'}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
};

