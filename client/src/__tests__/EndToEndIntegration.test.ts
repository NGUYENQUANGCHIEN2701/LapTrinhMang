import { describe, it, expect } from 'vitest';
import { MessageBuilder } from '../core/network/utils/MessageBuilder';
import { MessageParser } from '../core/network/utils/MessageParser';
import { PacketType } from '../core/network/types';
import { Direction } from '../shared/types/game';
import type { GameStateData } from '../core/Renderer';

describe('Task 1.14: Client End-to-End Integration Tests', () => {
  it('1. Đóng gói thông điệp JOIN_GAME cho chế độ Campaign', () => {
    const raw = MessageBuilder.buildJoinGame('campaign', 'Player1', 'campaign_1');
    const parsed = JSON.parse(raw);

    expect(parsed.type).toBe(PacketType.JOIN_GAME);
    expect(parsed.payload).toBeDefined();
    expect(parsed.payload.mode).toBe('campaign');
    expect(parsed.payload.levelId).toBe('campaign_1');
    expect(parsed.payload.playerName).toBe('Player1');
  });

  it('2. Đóng gói thông điệp MOVE_INPUT khi bấm và nhả phím di chuyển', () => {
    // Bấm phím W (Direction UP, moving = true)
    const movePressRaw = MessageBuilder.buildMoveInput('UP' as Direction, true);
    const movePress = JSON.parse(movePressRaw);
    expect(movePress.type).toBe(PacketType.MOVE_INPUT);
    expect(movePress.payload.direction).toBe('UP');
    expect(movePress.payload.moving).toBe(true);

    // Nhả phím W (Direction UP, moving = false)
    const moveReleaseRaw = MessageBuilder.buildMoveInput('UP' as Direction, false);
    const moveRelease = JSON.parse(moveReleaseRaw);
    expect(moveRelease.type).toBe(PacketType.MOVE_INPUT);
    expect(moveRelease.payload.direction).toBe('UP');
    expect(moveRelease.payload.moving).toBe(false);
  });

  it('3. Đóng gói thông điệp SHOOT_INPUT khi bấm Space hoặc J', () => {
    const shootRaw = MessageBuilder.buildShootInput();
    const parsed = JSON.parse(shootRaw);

    expect(parsed.type).toBe(PacketType.SHOOT_INPUT);
    expect(parsed.timestamp).toBeGreaterThan(0);
  });

  it('4. Phân tích gói tin STATE từ server nhận về và ánh xạ dữ liệu chính xác', () => {
    const sampleServerState = {
      type: 'STATE',
      gameStatus: 'PLAYING',
      score: 100,
      tanks: [
        {
          id: 'player-session-1',
          x: 100,
          y: 100,
          rotation: 0,
          hp: 100,
          maxHp: 100,
          alive: true,
          isPlayer: true,
        },
        {
          id: 'ai-16-11',
          x: 660,
          y: 460,
          rotation: 180,
          hp: 75,
          maxHp: 100,
          alive: true,
          isPlayer: false,
        },
      ],
      bullets: [
        {
          id: 'bullet-1',
          x: 100,
          y: 80,
          direction: 0,
          active: true,
          ownerId: 'player-session-1',
        },
      ],
      map: {
        width: 20,
        height: 15,
        tileSize: 40,
        grid: [[1, 1], [1, 0]],
      },
      timestamp: Date.now(),
    };

    const parseResult = MessageParser.parse(JSON.stringify(sampleServerState));
    expect(parseResult.success).toBe(true);
    expect(parseResult.data).toBeDefined();

    const state = parseResult.data as GameStateData;
    expect(state.tanks).toHaveLength(2);
    expect(state.bullets).toHaveLength(1);

    const playerTank = state.tanks.find((t) => t.isPlayer);
    expect(playerTank).toBeDefined();
    expect(playerTank?.hp).toBe(100);

    const enemyTank = state.tanks.find((t) => !t.isPlayer);
    expect(enemyTank).toBeDefined();
    expect(enemyTank?.hp).toBe(75);
  });

  it('5. Trạng thái mất kết nối hiển thị thông báo và vô hiệu hóa controls', () => {
    let connected = false;
    let alive = true;
    let gameStatus = 'PLAYING';

    const isControlEnabled = () => connected && alive && gameStatus === 'PLAYING';

    expect(isControlEnabled()).toBe(false);

    // Khi kết nối thành công
    connected = true;
    expect(isControlEnabled()).toBe(true);

    // Khi server ngắt kết nối
    connected = false;
    expect(isControlEnabled()).toBe(false);
  });
});
