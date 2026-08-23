import { useCallback, useEffect, useRef, useState } from 'react';
import { GameRenderState } from '../types';
import { Direction } from '../../../shared/types/game';
import { PacketType, globalWebSocketClient } from '../../../core/network';

export const useGameLoop = (mode: string) => {
  const [gameState, setGameState] = useState<GameRenderState>({
    tanks: [
      {
        id: 'player-1',
        position: { x: 400, y: 300 },
        direction: 'UP',
        hp: 100,
        maxHp: 100,
        isAlive: true,
        speed: 4,
        type: 'player',
      },
    ],
    bullets: [],
    score: 0,
    status: 'PLAYING',
  });

  useEffect(() => {
    // Attempt WebSocket connection
    globalWebSocketClient.connect().catch(() => {
      console.warn('[useGameLoop] Running in local simulation mode (server offline)');
    });

    globalWebSocketClient.send({
      type: PacketType.JOIN_GAME,
      mode: mode as any,
      playerName: 'Player1',
    });

    return () => {
      globalWebSocketClient.disconnect();
    };
  }, [mode]);

  const handleMove = useCallback((direction: Direction, moving: boolean) => {
    if (moving) {
      setGameState((prev) => {
        const player = prev.tanks.find((t) => t.id === 'player-1');
        if (!player) return prev;

        let newX = player.position.x;
        let newY = player.position.y;
        const step = 6;

        if (direction === 'UP') newY -= step;
        if (direction === 'DOWN') newY += step;
        if (direction === 'LEFT') newX -= step;
        if (direction === 'RIGHT') newX += step;

        return {
          ...prev,
          tanks: prev.tanks.map((t) =>
            t.id === 'player-1'
              ? { ...t, direction, position: { x: Math.max(20, Math.min(780, newX)), y: Math.max(20, Math.min(580, newY)) } }
              : t
          ),
        };
      });
    }

    globalWebSocketClient.send({
      type: PacketType.MOVE_INPUT,
      direction,
      moving,
    });
  }, []);

  const handleShoot = useCallback(() => {
    setGameState((prev) => {
      const player = prev.tanks.find((t) => t.id === 'player-1');
      if (!player) return prev;

      const newBullet = {
        id: `bullet-${Date.now()}`,
        position: { ...player.position },
        direction: player.direction,
        speed: 10,
        damage: 25,
        ownerId: player.id,
      };

      return {
        ...prev,
        bullets: [...prev.bullets, newBullet],
      };
    });

    globalWebSocketClient.send({
      type: PacketType.SHOOT_INPUT,
    });
  }, []);

  return { gameState, handleMove, handleShoot };
};
