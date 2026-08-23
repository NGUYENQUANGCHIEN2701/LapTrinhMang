import { BulletState, GameMode, TankState } from '../../../shared/types/game';

export interface GameCanvasProps {
  mode: GameMode;
  levelId?: string;
}

export interface GameRenderState {
  tanks: TankState[];
  bullets: BulletState[];
  score: number;
  status: 'PLAYING' | 'VICTORY' | 'DEFEAT' | 'CONNECTING';
}

export interface InputState {
  up: boolean;
  down: boolean;
  left: boolean;
  right: boolean;
  shoot: boolean;
}
