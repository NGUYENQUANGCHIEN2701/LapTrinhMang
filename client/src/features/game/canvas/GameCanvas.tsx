import React, { useEffect, useRef } from 'react';
import { GameCanvasProps } from '../types';
import { useGameLoop } from '../hooks/useGameLoop';
import { useInputHandler } from '../hooks/useInputHandler';
import { Renderer } from './Renderer';
import { GameHUDOverlay } from '../components/GameHUDOverlay';
import { CANVAS_HEIGHT, CANVAS_WIDTH } from '../../../shared/constants/game';

export const GameCanvas: React.FC<GameCanvasProps> = ({ mode }) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const { gameState, handleMove, handleShoot } = useGameLoop(mode);

  useInputHandler({
    onMove: handleMove,
    onShoot: handleShoot,
  });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const renderer = new Renderer(ctx);
    renderer.render(gameState);
  }, [gameState]);

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '100vh',
      backgroundColor: '#121216',
      gap: '16px',
      padding: '20px'
    }}>
      <GameHUDOverlay mode={mode} gameState={gameState} />

      <div style={{
        position: 'relative',
        borderRadius: '12px',
        overflow: 'hidden',
        boxShadow: '0 12px 40px rgba(0, 0, 0, 0.6)',
        border: '2px solid #22222a'
      }}>
        <canvas
          ref={canvasRef}
          width={CANVAS_WIDTH}
          height={CANVAS_HEIGHT}
          style={{ display: 'block', background: '#000000' }}
        />
      </div>

      <div style={{ color: '#888899', fontSize: '0.85rem' }}>
        Dùng phím <strong style={{ color: '#ffffff' }}>W A S D</strong> hoặc <strong style={{ color: '#ffffff' }}>Mũi Tên</strong> để di chuyển, <strong style={{ color: '#ffffff' }}>Space</strong> để bắn.
      </div>
    </div>
  );
};

export default GameCanvas;
