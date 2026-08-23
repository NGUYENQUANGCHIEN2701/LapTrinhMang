export type GameMode = 'campaign' | 'online';

export type Direction = 'UP' | 'DOWN' | 'LEFT' | 'RIGHT';

export enum TileType {
  GROUND = 0,
  WALL = 1,
  WATER = 2,
  SPAWN_PLAYER = 3,
  SPAWN_ENEMY = 4,
}

export interface Position {
  x: number;
  y: number;
}

export interface Rectangle {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface TankState {
  id: string;
  position: Position;
  direction: Direction;
  hp: number;
  maxHp: number;
  isAlive: boolean;
  speed: number;
  type: 'player' | 'ai';
  color?: string;
}

export interface BulletState {
  id: string;
  position: Position;
  direction: Direction;
  speed: number;
  damage: number;
  ownerId: string;
}

export interface MapData {
  id: string;
  name: string;
  width: number;
  height: number;
  tiles: number[][];
}
