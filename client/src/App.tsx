import React, { useState } from 'react';
import { ConnectionStatusUI, globalWebSocketClient } from './core/network';
import { GameCanvas } from './features/game/GameCanvas';
import './App.css';
import { useGameSession, CAMPAIGN_LEVELS } from './core/network/hooks/useGameSession';
import { LevelSelector } from './features/campaign/components/LevelSelector';
import { GameModal } from './features/campaign/components/GameModal';
import { MainMenu } from './features/menu';

export const App: React.FC = () => {
  const [view, setView] = useState<'menu' | 'campaign'>('menu');

  const {
    gameState,
    connected,
    alive,
    currentLevel,
    unlockedLevels,
    selectLevel,
    restartLevel,
    nextLevel,
    resetProgress,
    gameStatus,
    score,
  } = useGameSession();

  const currentLevelConfig =
    CAMPAIGN_LEVELS.find((l) => l.id === currentLevel) ?? CAMPAIGN_LEVELS[0];

  return (
    <div
      style={{
        width: '100%',
        minHeight: '100vh',
        backgroundColor: '#121216',
        color: '#ffffff',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px',
        boxSizing: 'border-box',
        position: 'relative',
        fontFamily: "'Segoe UI', Roboto, Helvetica, Arial, sans-serif",
      }}
    >
      <ConnectionStatusUI />

      {view === 'menu' ? (
        <MainMenu
          onStartCampaign={() => {
            setView('campaign');
            restartLevel();
          }}
          connected={connected}
        />
      ) : (
        <>
          <header style={{ marginBottom: '12px', textAlign: 'center', position: 'relative', width: '100%', maxWidth: '800px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
              <button
                id="btn-back-menu"
                onClick={() => setView('menu')}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '6px 14px',
                  backgroundColor: '#242b35',
                  color: '#bdc3c7',
                  border: '1px solid #34495e',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  fontSize: '13px',
                  fontWeight: 600,
                  transition: 'all 0.2s',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.color = '#f1c40f';
                  e.currentTarget.style.borderColor = '#f1c40f';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.color = '#bdc3c7';
                  e.currentTarget.style.borderColor = '#34495e';
                }}
              >
                ⬅ Menu Chính
              </button>

              <h1
                style={{
                  margin: 0,
                  fontSize: '24px',
                  color: '#f1c40f',
                  letterSpacing: '2px',
                  textTransform: 'uppercase',
                }}
              >
                Tankzors - Chiến Dịch
              </h1>

              <div style={{ width: '100px' }} />
            </div>

            <div style={{ fontSize: '13px', color: '#95a5a6' }}>
              Điều khiển: <b style={{ color: '#ecf0f1' }}>W, A, S, D / Mũi tên</b> | Bắn: <b style={{ color: '#ecf0f1' }}>Space / J</b>
            </div>
          </header>

          {/* Level Selection Bar */}
          <LevelSelector
            currentLevel={currentLevel}
            unlockedLevels={unlockedLevels}
            onSelectLevel={selectLevel}
            onResetProgress={resetProgress}
          />

          <main style={{ position: 'relative' }}>
            {/* Top Info Bar on Canvas */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                backgroundColor: '#1e272e',
                border: '2px solid #34495e',
                borderBottom: 'none',
                borderTopLeftRadius: '10px',
                borderTopRightRadius: '10px',
                padding: '8px 16px',
                fontSize: '14px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ color: '#f1c40f', fontWeight: 'bold' }}>{currentLevelConfig.title}</span>
                <span style={{ color: '#7f8c8d' }}>•</span>
                <span style={{ color: '#bdc3c7' }}>Độ khó: {currentLevelConfig.difficulty}</span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <div>
                  <span style={{ color: '#7f8c8d' }}>Điểm số: </span>
                  <span style={{ color: '#f1c40f', fontWeight: 'bold' }}>{score}</span>
                </div>
                <div>
                  <span style={{ color: '#7f8c8d' }}>Trạng thái: </span>
                  <span
                    style={{
                      fontWeight: 'bold',
                      color:
                        !connected
                          ? '#ff5252'
                          : gameStatus === 'PLAYING'
                          ? '#2ecc71'
                          : gameStatus === 'VICTORY'
                          ? '#f1c40f'
                          : gameStatus === 'DEFEAT'
                          ? '#e74c3c'
                          : '#95a5a6',
                    }}
                  >
                    {!connected
                      ? 'MẤT KẾT NỐI'
                      : gameStatus === 'PLAYING'
                      ? 'ĐANG CHIẾN ĐẤU'
                      : gameStatus === 'VICTORY'
                      ? 'CHIẾN THẮNG'
                      : gameStatus === 'DEFEAT'
                      ? 'THẤT BẠI'
                      : 'CHỜ BẮT ĐẦU'}
                  </span>
                </div>
              </div>
            </div>

            {/* Canvas and Game Overlays */}
            <div style={{ position: 'relative' }}>
              <GameCanvas
                gameState={gameState}
                controlsEnabled={connected && alive && gameStatus === 'PLAYING'}
              />

              {/* Disconnected Overlay */}
              {!connected && (
                <div
                  id="disconnect-overlay"
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    backgroundColor: 'rgba(18, 18, 22, 0.85)',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 20,
                    borderRadius: '12px',
                    backdropFilter: 'blur(5px)',
                  }}
                >
                  <div
                    style={{
                      backgroundColor: '#1e272e',
                      border: '2px solid #e74c3c',
                      borderRadius: '12px',
                      padding: '24px 32px',
                      textAlign: 'center',
                      boxShadow: '0 10px 30px rgba(231, 76, 60, 0.3)',
                    }}
                  >
                    <div style={{ fontSize: '36px', marginBottom: '8px' }}>⚠️</div>
                    <h2
                      style={{
                        margin: '0 0 8px 0',
                        fontSize: '22px',
                        color: '#ff5252',
                        textTransform: 'uppercase',
                        letterSpacing: '1px',
                      }}
                    >
                      Mất kết nối
                    </h2>
                    <p style={{ margin: '0 0 16px 0', color: '#bdc3c7', fontSize: '14px' }}>
                      Mất kết nối tới server. Trò chơi đã tạm dừng.
                    </p>
                    <button
                      id="btn-reconnect"
                      onClick={() => globalWebSocketClient.connect()}
                      style={{
                        padding: '10px 20px',
                        backgroundColor: '#e74c3c',
                        color: '#ffffff',
                        border: 'none',
                        borderRadius: '6px',
                        fontSize: '14px',
                        fontWeight: 'bold',
                        cursor: 'pointer',
                        transition: 'background-color 0.2s',
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#c0392b')}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '#e74c3c')}
                    >
                      🔄 Thử kết nối lại
                    </button>
                  </div>
                </div>
              )}

              {/* Victory / Defeat Modal */}
              <GameModal
                gameStatus={gameStatus}
                currentLevel={currentLevel}
                score={score}
                onRestart={restartLevel}
                onNextLevel={nextLevel}
              />
            </div>
          </main>
        </>
      )}
    </div>
  );
};

export default App;
