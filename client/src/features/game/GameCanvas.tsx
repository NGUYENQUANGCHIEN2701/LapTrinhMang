import React, { useEffect, useRef, useState } from 'react';
import { SpriteManager } from '../../core/SpriteManager';
import { GameLoop } from '../../core/GameLoop';
import { GameStateData, GameMapData } from '../../core/Renderer';
import { useInput } from '../../core/input';

interface GameCanvasProps {
  gameState?: GameStateData;
}

// Map mẫu Campaign 1 (20x15 ô)
const MOCK_MAP: GameMapData = {
  width: 20,
  height: 15,
  tileSize: 40,
  grid: [
    [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
    [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    [1, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 0, 0, 0, 5, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 1],
    [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
  ],
};

export const GameCanvas: React.FC<GameCanvasProps> = ({ gameState }) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const gameLoopRef = useRef<GameLoop | null>(null);
  const [isAssetsLoaded, setIsAssetsLoaded] = useState(false);

  // Task 1.12: Bắt phím điều khiển + gửi input qua WebSocket
  useInput();

  // Demo State trong khi chờ WebSocket Server đẩy state thật
  const mockStateRef = useRef<GameStateData>({
    map: MOCK_MAP,
    tanks: [
      { id: 'player-1', x: 100, y: 100, rotation: 90, hp: 100, maxHp: 100, isPlayer: true },
      { id: 'enemy-1', x: 680, y: 440, rotation: 270, hp: 75, maxHp: 100, isPlayer: false },
    ],
    bullets: [
      { id: 'bullet-1', x: 200, y: 100, direction: 90, active: true },
      { id: 'bullet-2', x: 580, y: 440, direction: 270, active: true },
    ],
    playerHP: 100,
    maxHP: 100,
  });

  useEffect(() => {
    // 1. Preload hình ảnh assets
    SpriteManager.getInstance()
      .loadAll()
      .then(() => setIsAssetsLoaded(true));

    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d', { alpha: false }); // Tắt alpha channel trên Canvas chính để tăng tốc GPU!
    if (!ctx) return;

    // 2. Khởi tạo Singleton GameLoop một lần duy nhất
    if (!gameLoopRef.current) {
      gameLoopRef.current = new GameLoop(ctx, () => gameState || mockStateRef.current);
    }
    
    if (!gameLoopRef.current.isRunning()) {
      gameLoopRef.current.start();
    }

    // 3. Demo animation di chuyển đạn để thử nghiệm
    const interval = setInterval(() => {
      if (gameState) return;

      const state = mockStateRef.current;
      state.bullets = state.bullets.map((b) => {
        let newX = b.x + (b.direction === 90 ? 4 : -4);
        if (newX > 740) newX = 140;
        if (newX < 60) newX = 660;
        return { ...b, x: newX };
      });
    }, 16);

    return () => {
      if (gameLoopRef.current) {
        gameLoopRef.current.stop();
        gameLoopRef.current = null;
      }
      clearInterval(interval);
    };
  }, [gameState]);

  return (
    <div className="game-canvas-container" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
      <div style={{ position: 'relative', border: '4px solid #34495e', borderRadius: '12px', overflow: 'hidden', boxShadow: '0 10px 30px rgba(0,0,0,0.5)' }}>
        <canvas
          ref={canvasRef}
          width={800}
          height={600}
          style={{ display: 'block', backgroundColor: '#1e272e' }}
        />
        {!isAssetsLoaded && (
          <div style={{ position: 'absolute', top: 10, right: 10, backgroundColor: 'rgba(0,0,0,0.7)', color: '#f1c40f', padding: '4px 8px', borderRadius: '4px', fontSize: '12px' }}>
            ⏳ Đang nạp Assets...
          </div>
        )}
      </div>
    </div>
  );
};
