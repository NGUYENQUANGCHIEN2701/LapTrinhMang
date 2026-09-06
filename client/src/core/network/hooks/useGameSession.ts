import { useCallback, useEffect, useRef, useState } from 'react';
import type { GameStateData } from '../../Renderer';
import { globalWebSocketClient } from '../WebSocketClient';
import { MessageBuilder } from '../utils/MessageBuilder';

export interface CampaignLevelConfig {
  id: string;
  title: string;
  difficulty: 'Dễ' | 'Trung bình' | 'Khó';
  stars: number;
  enemiesCount: number;
  desc: string;
}

export const CAMPAIGN_LEVELS: CampaignLevelConfig[] = [
  {
    id: 'campaign_1',
    title: 'Màn 1: Huấn luyện',
    difficulty: 'Dễ',
    stars: 1,
    enemiesCount: 1,
    desc: 'Bản đồ mở rộng, làm quen điều khiển và ngắm bắn.',
  },
  {
    id: 'campaign_2',
    title: 'Màn 2: Chiến hào',
    difficulty: 'Trung bình',
    stars: 2,
    enemiesCount: 2,
    desc: 'Chiến hào quanh co và chướng ngại vật chông sắt chống tăng.',
  },
  {
    id: 'campaign_3',
    title: 'Màn 3: Pháo đài',
    difficulty: 'Khó',
    stars: 3,
    enemiesCount: 3,
    desc: 'Pháo đài kiên cố, kẻ địch tinh nhuệ bao vây 3 hướng.',
  },
];

const STORAGE_KEY = 'tankzors_unlocked_levels';

function getInitialUnlockedLevels(): string[] {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      const parsed = JSON.parse(saved);
      if (Array.isArray(parsed) && parsed.includes('campaign_1')) {
        return parsed;
      }
    }
  } catch (e) {
    // Ignore storage errors
  }
  return ['campaign_1'];
}

/** The server is the only source of positions and bullets. */
export function useGameSession() {
  const [gameState, setGameState] = useState<GameStateData>();
  const [connected, setConnected] = useState(false);
  const [alive, setAlive] = useState(false);
  const [currentLevel, setCurrentLevel] = useState<string>('campaign_1');
  const [unlockedLevels, setUnlockedLevels] = useState<string[]>(getInitialUnlockedLevels);

  const sessionId = useRef<string | undefined>(undefined);
  const currentLevelRef = useRef(currentLevel);
  currentLevelRef.current = currentLevel;

  const joinLevel = useCallback((levelId: string) => {
    const client = globalWebSocketClient;
    sessionId.current = undefined;
    setAlive(false);
    client.sendRaw(MessageBuilder.buildJoinGame('campaign', 'Player', levelId));
  }, []);

  const selectLevel = useCallback(
    (levelId: string) => {
      if (!unlockedLevels.includes(levelId)) return;
      setCurrentLevel(levelId);
      joinLevel(levelId);
    },
    [unlockedLevels, joinLevel]
  );

  const restartLevel = useCallback(() => {
    joinLevel(currentLevelRef.current);
  }, [joinLevel]);

  const nextLevel = useCallback(() => {
    const currentIndex = CAMPAIGN_LEVELS.findIndex((l) => l.id === currentLevelRef.current);
    if (currentIndex >= 0 && currentIndex < CAMPAIGN_LEVELS.length - 1) {
      const nextId = CAMPAIGN_LEVELS[currentIndex + 1].id;
      setCurrentLevel(nextId);
      joinLevel(nextId);
    }
  }, [joinLevel]);

  const resetProgress = useCallback(() => {
    const defaultUnlocked = ['campaign_1'];
    setUnlockedLevels(defaultUnlocked);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(defaultUnlocked));
    } catch (e) {}
    setCurrentLevel('campaign_1');
    joinLevel('campaign_1');
  }, [joinLevel]);

  useEffect(() => {
    const client = globalWebSocketClient;
    const join = () => {
      sessionId.current = undefined;
      setConnected(true);
      setAlive(false);
      setGameState(undefined);
      client.sendRaw(MessageBuilder.buildJoinGame('campaign', 'Player', currentLevelRef.current));
    };
    const lost = () => {
      setConnected(false);
      setAlive(false);
    };
    const unsubscribeOpen = client.onOpen(join);
    const unsubscribeClose = client.onClose(lost);
    const unsubscribeError = client.onError(lost);
    const unsubscribeMessage = client.onMessage((packet) => {
      if (packet.type === 'CONNECT') sessionId.current = packet.sessionId;
      if (packet.type !== 'STATE') return;
      const state = packet as GameStateData;
      if (!Array.isArray(state.tanks) || !Array.isArray(state.bullets) || !state.map) return;
      const player = state.tanks.find((tank) =>
        sessionId.current ? tank.id === sessionId.current : tank.isPlayer
      );
      setAlive(Boolean(player && player.alive !== false && player.hp > 0));
      setGameState({ ...state, playerHP: player?.hp ?? 0, maxHP: player?.maxHp ?? 100 });

      // Khi người chơi chiến thắng (VICTORY), tự động mở khóa màn kế tiếp
      if (state.gameStatus === 'VICTORY') {
        const curLevel = currentLevelRef.current;
        const curIdx = CAMPAIGN_LEVELS.findIndex((l) => l.id === curLevel);
        if (curIdx >= 0 && curIdx < CAMPAIGN_LEVELS.length - 1) {
          const nextLevelId = CAMPAIGN_LEVELS[curIdx + 1].id;
          setUnlockedLevels((prev) => {
            if (!prev.includes(nextLevelId)) {
              const updated = [...prev, nextLevelId];
              try {
                localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
              } catch (e) {}
              return updated;
            }
            return prev;
          });
        }
      }
    });

    if (client.getStatus() === 'connected') join();
    else client.connect();

    return () => {
      unsubscribeOpen();
      unsubscribeClose();
      unsubscribeError();
      unsubscribeMessage();
    };
  }, []);

  const gameStatus = gameState?.gameStatus ?? 'WAITING';
  const score = gameState?.score ?? 0;

  return {
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
  };
}
