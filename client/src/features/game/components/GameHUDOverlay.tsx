import React from 'react';
import { Link } from 'react-router-dom';
import { GameRenderState } from '../types';

interface Props {
  mode: string;
  gameState: GameRenderState;
}

export const GameHUDOverlay: React.FC<Props> = ({ mode, gameState }) => {
  const player = gameState.tanks.find((t) => t.type === 'player');

  return (
    <div style={{
      width: '800px',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '12px 20px',
      background: '#1e1e24',
      borderRadius: '8px',
      border: '1px solid rgba(255, 255, 255, 0.1)',
      boxSizing: 'border-box'
    }}>
      <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
        <div style={{ color: '#00e676', fontWeight: 'bold', fontSize: '1.1rem' }}>
          MODE: {mode.toUpperCase()}
        </div>
        <div style={{ color: '#ffffff', fontSize: '0.95rem' }}>
          HP: <span style={{ color: '#00e676', fontWeight: 600 }}>{player ? player.hp : 0}</span> / {player ? player.maxHp : 100}
        </div>
        <div style={{ color: '#ffffff', fontSize: '0.95rem' }}>
          SCORE: <span style={{ color: '#ffeb3b', fontWeight: 600 }}>{gameState.score}</span>
        </div>
      </div>

      <Link
        to="/"
        style={{
          padding: '6px 14px',
          background: '#33333f',
          color: '#ffffff',
          borderRadius: '6px',
          textDecoration: 'none',
          fontSize: '0.85rem',
          transition: 'background 0.2s'
        }}
      >
        Thoát Trận
      </Link>
    </div>
  );
};
