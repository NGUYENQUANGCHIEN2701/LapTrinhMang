import React from 'react';
import { ConnectionStatusUI } from './core/network';
import { GameCanvas } from './features/game/GameCanvas';
import './App.css';
import { useGameSession } from './core/network/hooks/useGameSession';

export const App: React.FC = () => {
  const { gameState, connected, alive } = useGameSession();
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

      <header style={{ marginBottom: '16px', textAlign: 'center' }}>
        <h1 style={{ margin: 0, fontSize: '28px', color: '#f1c40f', letterSpacing: '2px', textTransform: 'uppercase' }}>
          🛡️ Tankzors Game
        </h1>
      </header>

      <main>
        <GameCanvas gameState={gameState} controlsEnabled={connected && alive} />
      </main>
    </div>
  );
};

export default App;
