import { useEffect, useRef, useState } from 'react';
import type { GameStateData } from '../../Renderer';
import { globalWebSocketClient } from '../WebSocketClient';
import { MessageBuilder } from '../utils/MessageBuilder';

/** The server is the only source of positions and bullets. */
export function useGameSession() {
  const [gameState, setGameState] = useState<GameStateData>();
  const [connected, setConnected] = useState(false);
  const [alive, setAlive] = useState(false);
  const sessionId = useRef<string | undefined>(undefined);

  useEffect(() => {
    const client = globalWebSocketClient;
    const join = () => {
      sessionId.current = undefined;
      setConnected(true);
      setAlive(false);
      setGameState(undefined);
      client.sendRaw(MessageBuilder.buildJoinGame('campaign', 'Player', 'campaign_1'));
    };
    const lost = () => { setConnected(false); setAlive(false); };
    const unsubscribeOpen = client.onOpen(join);
    const unsubscribeClose = client.onClose(lost);
    const unsubscribeError = client.onError(lost);
    const unsubscribeMessage = client.onMessage((packet) => {
      if (packet.type === 'CONNECT') sessionId.current = packet.sessionId;
      if (packet.type !== 'STATE') return;
      const state = packet as GameStateData;
      if (!Array.isArray(state.tanks) || !Array.isArray(state.bullets) || !state.map) return;
      const player = state.tanks.find(tank => sessionId.current ? tank.id === sessionId.current : tank.isPlayer);
      setAlive(Boolean(player && player.alive !== false && player.hp > 0));
      setGameState({ ...state, playerHP: player?.hp ?? 0, maxHP: player?.maxHp ?? 100 });
    });
    if (client.getStatus() === 'connected') join();
    else client.connect();
    return () => {
      unsubscribeOpen(); unsubscribeClose(); unsubscribeError(); unsubscribeMessage();
    };
  }, []);

  return { gameState, connected, alive };
}
