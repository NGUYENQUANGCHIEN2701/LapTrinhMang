import { BulletState, Direction, GameMode, TankState } from '../../shared/types/game';

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

export type OnOpenListener = () => void;
export type OnMessageListener = (data: any) => void;
export type OnCloseListener = (event: CloseEvent) => void;
export type OnErrorListener = (error: Event) => void;

export enum PacketType {
  CONNECT = 'CONNECT',
  JOIN_GAME = 'JOIN_GAME',
  MOVE_INPUT = 'MOVE_INPUT',
  SHOOT_INPUT = 'SHOOT_INPUT',
  GAME_STATE_UPDATE = 'GAME_STATE_UPDATE',
  PLAYER_DEATH = 'PLAYER_DEATH',
  LEVEL_COMPLETE = 'LEVEL_COMPLETE',
  ERROR = 'ERROR',
}

export interface BasePacket {
  type: PacketType | string;
  payload?: any;
  timestamp?: number;
}

export interface JoinGamePacket extends BasePacket {
  type: PacketType.JOIN_GAME;
  payload: {
    mode: GameMode;
    levelId?: string;
    playerName: string;
  };
}

export interface MoveInputPacket extends BasePacket {
  type: PacketType.MOVE_INPUT;
  payload: {
    direction: Direction;
    moving: boolean;
  };
}

export interface ShootInputPacket extends BasePacket {
  type: PacketType.SHOOT_INPUT;
}

export interface GameStatePayload {
  tanks: TankState[];
  bullets: BulletState[];
  score: number;
  gameStatus: 'PLAYING' | 'VICTORY' | 'DEFEAT';
}

export interface GameStateUpdatePacket extends BasePacket {
  type: PacketType.GAME_STATE_UPDATE;
  payload: GameStatePayload;
}

export type OutgoingPacket = JoinGamePacket | MoveInputPacket | ShootInputPacket | BasePacket;
export type IncomingPacket = GameStateUpdatePacket | BasePacket;
