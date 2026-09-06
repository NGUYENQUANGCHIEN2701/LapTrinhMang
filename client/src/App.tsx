import React from 'react';
import { ConnectionStatusUI } from './core/network';
import { GameCanvas } from './features/game/GameCanvas';
import './App.css';
import { useGameSession, CAMPAIGN_LEVELS } from './core/network/hooks/useGameSession';
import { LevelSelector } from './features/campaign/components/LevelSelector';
import { GameModal } from './features/campaign/components/GameModal';

export const App: React.FC = () => {
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

      <header style={{ marginBottom: '12px', textAlign: 'center' }}>
        <h1
          style={{
            margin: '0 0 4px 0',
            fontSize: '28px',
            color: '#f1c40f',
            letterSpacing: '2px',
            textTransform: 'uppercase',
          }}
        >
          Tankzors - Chế Độ Chiến Dịch
        </h1>
        <div style={{ fontSize: '14px', color: '#95a5a6' }}>
          Điều khiển: <b style={{ color: '#ecf0f1' }}>W, A, S, D</b> hoặc <b style={{ color: '#ecf0f1' }}>Phím mũi tên</b> | Bắn: <b style={{ color: '#ecf0f1' }}>Space / J</b>
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
                    gameStatus === 'PLAYING'
                      ? '#2ecc71'
                      : gameStatus === 'VICTORY'
                      ? '#f1c40f'
                      : gameStatus === 'DEFEAT'
                      ? '#e74c3c'
                      : '#95a5a6',
                }}
              >
                {gameStatus === 'PLAYING'
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
    </div>
  );
};

export default App;
