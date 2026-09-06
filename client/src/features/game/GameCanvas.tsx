import { useEffect, useRef, useState } from 'react';
import { SpriteManager } from '../../core/SpriteManager';
import { GameLoop } from '../../core/GameLoop';
import type { GameStateData, GameMapData } from '../../core/Renderer';
import { useInput } from '../../core/input';

interface GameCanvasProps {
  gameState?: GameStateData;
  controlsEnabled?: boolean;
}

const PREVIEW_MAP: GameMapData = {
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

// Preserve the original scene while the first authoritative snapshot arrives.
const EMPTY_STATE: GameStateData = {
  map: PREVIEW_MAP,
  tanks: [
    { id: 'player-1', x: 100, y: 100, rotation: 90, hp: 100, maxHp: 100, isPlayer: true },
    { id: 'enemy-1', x: 680, y: 440, rotation: 270, hp: 75, maxHp: 100, isPlayer: false },
  ],
  bullets: [], playerHP: 100, maxHP: 100,
};

export function GameCanvas({ gameState, controlsEnabled = false }: GameCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const latestState = useRef<GameStateData>(EMPTY_STATE);
  const [isAssetsLoaded, setIsAssetsLoaded] = useState(false);
  latestState.current = gameState ?? EMPTY_STATE;
  useInput(controlsEnabled);

  useEffect(() => {
    let disposed = false;
    SpriteManager.getInstance().loadAll().then(() => {
      if (!disposed) setIsAssetsLoaded(true);
    }).catch(() => { if (!disposed) setIsAssetsLoaded(true); });
    const ctx = canvasRef.current?.getContext('2d', { alpha: false });
    if (!ctx) return () => { disposed = true; };
    const loop = new GameLoop(ctx, () => latestState.current);
    loop.start();
    return () => { disposed = true; loop.stop(); };
  }, []);

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
}
