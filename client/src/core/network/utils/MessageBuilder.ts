import { Direction, GameMode } from '../../../shared/types/game';
import { BasePacket, PacketType } from '../types';

export class MessageBuilder {
  /**
   * Tạo gói tin Join Game
   */
  public static buildJoinGame(mode: GameMode, playerName: string, levelId?: string): string {
    const packet: BasePacket = {
      type: PacketType.JOIN_GAME,
      payload: { mode, levelId, playerName },
      timestamp: Date.now(),
    };
    return this.stringifyAndLog(packet);
  }

  /**
   * Tạo gói tin di chuyển
   */
  public static buildMoveInput(direction: Direction, moving: boolean): string {
    const packet: BasePacket = {
      type: PacketType.MOVE_INPUT,
      payload: { direction, moving },
      timestamp: Date.now(),
    };
    return this.stringifyAndLog(packet);
  }

  /**
   * Tạo gói tin bắn đạn
   */
  public static buildShootInput(): string {
    const packet: BasePacket = {
      type: PacketType.SHOOT_INPUT,
      timestamp: Date.now(),
    };
    return this.stringifyAndLog(packet);
  }

  /**
   * Tạo gói tin tùy chỉnh theo type và data
   */
  public static buildCustom(type: string, payload?: any): string {
    const packet: BasePacket = {
      type,
      payload,
      timestamp: Date.now(),
    };
    return this.stringifyAndLog(packet);
  }

  private static stringifyAndLog(packet: BasePacket): string {
    const jsonString = JSON.stringify(packet);
    console.log('[WebSocket] Sent:', packet);
    return jsonString;
  }
}
