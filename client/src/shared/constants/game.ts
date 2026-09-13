export const CANVAS_WIDTH = 800;
export const CANVAS_HEIGHT = 600;
export const TILE_SIZE = 32;

// Tự động build WebSocket URL dựa trên current origin (khi serve trong server-spring
// qua Thymeleaf) hoặc fallback tới ws://localhost:8080/game khi Vite dev.
// Override được qua VITE_WS_URL env nếu cần custom deploy.
function detectServerUrl(): string {
  const ENV_WS_URL = (typeof import.meta !== 'undefined'
    && (import.meta as any).env
    && (import.meta as any).env.VITE_WS_URL)
    ? (import.meta as any).env.VITE_WS_URL as string
    : null;
  if (ENV_WS_URL) return ENV_WS_URL;

  if (typeof window !== 'undefined' && window.location) {
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${proto}//${window.location.host}/game`;
  }
  return 'ws://localhost:8080/game';
}

export const DEFAULT_SERVER_URL = detectServerUrl();

export const COLORS = {
  BACKGROUND: '#121216',
  WALL: '#4a4e69',
  WATER: '#2a9d8f',
  GROUND: '#22222a',
  PLAYER_TANK: '#00e676',
  ENEMY_TANK: '#ff5252',
  BULLET: '#ffeb3b',
  UI_TEXT: '#ffffff',
  UI_ACCENT: '#00e676',
  UI_PANEL: '#1e1e24',
} as const;
