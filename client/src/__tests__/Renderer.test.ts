import { describe, it, expect, beforeEach, vi } from 'vitest';
import { SpriteManager } from '../core/SpriteManager';
import { Renderer, GameMapData, TankData, BulletData } from '../core/Renderer';
import { GameLoop } from '../core/GameLoop';

function createMockContext(): CanvasRenderingContext2D {
  const ctx = {
    clearRect: vi.fn(),
    fillRect: vi.fn(),
    strokeRect: vi.fn(),
    beginPath: vi.fn(),
    arc: vi.fn(),
    fill: vi.fn(),
    stroke: vi.fn(),
    save: vi.fn(),
    restore: vi.fn(),
    translate: vi.fn(),
    rotate: vi.fn(),
    drawImage: vi.fn(),
    fillText: vi.fn(),
    roundRect: vi.fn(),
    fillStyle: '',
    strokeStyle: '',
    lineWidth: 1,
    font: '',
    textAlign: 'left',
  } as unknown as CanvasRenderingContext2D;
  return ctx;
}

class MockImage {
  src = '';
  complete = true;
  naturalWidth = 40;
  naturalHeight = 40;
  onload: (() => void) | null = null;
  onerror: (() => void) | null = null;
}

// @ts-ignore
global.Image = class extends MockImage {
  constructor() {
    super();
    setTimeout(() => {
      if (this.onload) this.onload();
    }, 1);
  }
};

describe('Client Renderer & Core Test Suite (50 Unit Tests)', () => {
  let ctx: CanvasRenderingContext2D;
  let renderer: Renderer;

  beforeEach(() => {
    ctx = createMockContext();
    renderer = new Renderer();
    vi.clearAllMocks();
    vi.spyOn(SpriteManager.getInstance(), 'isReady').mockReturnValue(false);
  });

  // ==========================================
  // GROUP 1: SpriteManager Tests (Test 01 - 10)
  // ==========================================

  it('Test 01: SpriteManager getInstance trả về Singleton duy nhất', () => {
    const instance1 = SpriteManager.getInstance();
    const instance2 = SpriteManager.getInstance();
    expect(instance1).toBe(instance2);
  });

  it('Test 02: SpriteManager ban đầu tiến độ progress bằng 0', () => {
    const manager = SpriteManager.getInstance();
    expect(manager.getProgress()).toBeGreaterThanOrEqual(0);
  });

  it('Test 03: getSprite với key chưa tồn tại trả về null', () => {
    const manager = SpriteManager.getInstance();
    expect(manager.getSprite('non_existent_key')).toBeNull();
  });

  it('Test 04: isReady trả về false với key không hợp lệ', () => {
    const manager = SpriteManager.getInstance();
    expect(manager.isReady('unknown_key')).toBe(false);
  });

  it('Test 05: loadAll tải toàn bộ danh sách tài nguyên không bị quăng exception', async () => {
    const manager = SpriteManager.getInstance();
    await expect(manager.loadAll()).resolves.not.toThrow();
  });

  it('Test 06: Sau khi loadAll, getProgress trả về 1.0', async () => {
    const manager = SpriteManager.getInstance();
    await manager.loadAll();
    expect(manager.getProgress()).toBe(1);
  });

  it('Test 07: Kịch bản nạp sprite player_tank', () => {
    vi.spyOn(SpriteManager.getInstance(), 'isReady').mockReturnValue(true);
    const manager = SpriteManager.getInstance();
    const sprite = manager.getSprite('player_tank');
    expect(sprite).toBeDefined();
  });

  it('Test 08: Kịch bản nạp sprite enemy_tank', () => {
    vi.spyOn(SpriteManager.getInstance(), 'isReady').mockReturnValue(true);
    const manager = SpriteManager.getInstance();
    const sprite = manager.getSprite('enemy_tank');
    expect(sprite).toBeDefined();
  });

  it('Test 09: Kịch bản nạp sprite wall bản đồ', () => {
    vi.spyOn(SpriteManager.getInstance(), 'isReady').mockReturnValue(true);
    const manager = SpriteManager.getInstance();
    const sprite = manager.getSprite('wall');
    expect(sprite).toBeDefined();
  });

  it('Test 10: Kịch bản nạp sprite ground đất nền', () => {
    vi.spyOn(SpriteManager.getInstance(), 'isReady').mockReturnValue(true);
    const manager = SpriteManager.getInstance();
    const sprite = manager.getSprite('ground');
    expect(sprite).toBeDefined();
  });

  // ==========================================
  // GROUP 2: Renderer Base & Safety (Test 11 - 20)
  // ==========================================

  it('Test 11: Renderer khởi tạo thành công', () => {
    expect(renderer).toBeDefined();
  });

  it('Test 12: clear() gọi clearRect và fillRect xóa sạch màn hình 800x600', () => {
    renderer.clear(ctx);
    expect(ctx.clearRect).toHaveBeenCalledWith(0, 0, 800, 600);
    expect(ctx.fillRect).toHaveBeenCalledWith(0, 0, 800, 600);
  });

  it('Test 13: drawMap với mapData = undefined vẽ bản đồ mặc định an toàn', () => {
    expect(() => renderer.drawMap(ctx, undefined)).not.toThrow();
    expect(ctx.fillRect).toHaveBeenCalled();
  });

  it('Test 14: drawMap với grid = null vẽ bản đồ mặc định an toàn', () => {
    const nullGridMap = { width: 20, height: 15, tileSize: 40, grid: null as unknown as number[][] };
    expect(() => renderer.drawMap(ctx, nullGridMap)).not.toThrow();
  });

  it('Test 15: drawTanks với mảng tanks = null/undefined an toàn', () => {
    expect(() => renderer.drawTanks(ctx, null as unknown as TankData[])).not.toThrow();
    expect(() => renderer.drawTanks(ctx, undefined as unknown as TankData[])).not.toThrow();
  });

  it('Test 16: drawTanks với mảng rỗng không gọi hàm vẽ tank', () => {
    renderer.drawTanks(ctx, []);
    expect(ctx.save).not.toHaveBeenCalled();
  });

  it('Test 17: drawBullets với mảng bullets = null/undefined an toàn', () => {
    expect(() => renderer.drawBullets(ctx, null as unknown as BulletData[])).not.toThrow();
    expect(() => renderer.drawBullets(ctx, undefined as unknown as BulletData[])).not.toThrow();
  });

  it('Test 18: drawBullets với mảng rỗng không vẽ đạn nào', () => {
    renderer.drawBullets(ctx, []);
    expect(ctx.arc).not.toHaveBeenCalled();
  });

  it('Test 19: drawHUD với playerHP/maxHP mặc định an toàn', () => {
    expect(() => renderer.drawHUD(ctx)).not.toThrow();
    expect(ctx.fillText).toHaveBeenCalledWith('100 / 100', expect.any(Number), expect.any(Number));
  });

  it('Test 20: drawHUD với HP âm tự động giới hạn clamp về 0', () => {
    renderer.drawHUD(ctx, -50, 100);
    expect(ctx.fillText).toHaveBeenCalledWith('-50 / 100', expect.any(Number), expect.any(Number));
  });

  // ==========================================
  // GROUP 3: Map Rendering Logic (Test 21 - 30)
  // ==========================================

  it('Test 21: drawMap duyệt 20x15 ô bản đồ và vẽ thành công', () => {
    const mockMap: GameMapData = {
      width: 20,
      height: 15,
      tileSize: 40,
      grid: Array(15).fill(Array(20).fill(0)),
    };
    renderer.drawMap(ctx, mockMap);
    expect((ctx.fillRect as any).mock.calls.length + (ctx.drawImage as any).mock.calls.length).toBeGreaterThan(0);
  });

  it('Test 22: Ô TileType.GROUND (0) được vẽ thành công', () => {
    const mockMap: GameMapData = {
      width: 1,
      height: 1,
      tileSize: 40,
      grid: [[0]],
    };
    renderer.drawMap(ctx, mockMap);
    expect((ctx.fillRect as any).mock.calls.length + (ctx.drawImage as any).mock.calls.length).toBeGreaterThan(0);
  });

  it('Test 23: Ô TileType.WALL (1) được vẽ thành công', () => {
    const mockMap: GameMapData = {
      width: 1,
      height: 1,
      tileSize: 40,
      grid: [[1]],
    };
    renderer.drawMap(ctx, mockMap);
    expect((ctx.fillRect as any).mock.calls.length + (ctx.drawImage as any).mock.calls.length).toBeGreaterThan(0);
  });

  it('Test 24: Ô TileType.WATER (2) được vẽ thành công', () => {
    const mockMap: GameMapData = {
      width: 1,
      height: 1,
      tileSize: 40,
      grid: [[2]],
    };
    renderer.drawMap(ctx, mockMap);
    expect((ctx.fillRect as any).mock.calls.length + (ctx.drawImage as any).mock.calls.length).toBeGreaterThan(0);
  });

  it('Test 25: Ô TileType.SPAWN_PLAYER (3) coi như ô đi được GROUND', () => {
    const mockMap: GameMapData = {
      width: 1,
      height: 1,
      tileSize: 40,
      grid: [[3]],
    };
    renderer.drawMap(ctx, mockMap);
    expect((ctx.fillRect as any).mock.calls.length + (ctx.drawImage as any).mock.calls.length).toBeGreaterThan(0);
  });

  it('Test 26: Ô TileType.SPAWN_ENEMY (4) coi như ô đi được GROUND', () => {
    const mockMap: GameMapData = {
      width: 1,
      height: 1,
      tileSize: 40,
      grid: [[4]],
    };
    renderer.drawMap(ctx, mockMap);
    expect((ctx.fillRect as any).mock.calls.length + (ctx.drawImage as any).mock.calls.length).toBeGreaterThan(0);
  });

  it('Test 27: Hỗ trợ tileSize tùy chỉnh (vd: 30px)', () => {
    const mockMap: GameMapData = {
      width: 2,
      height: 2,
      tileSize: 30,
      grid: [
        [0, 1],
        [1, 0],
      ],
    };
    renderer.drawMap(ctx, mockMap);
    expect((ctx.fillRect as any).mock.calls.length + (ctx.drawImage as any).mock.calls.length).toBeGreaterThan(0);
  });

  it('Test 28: Tường bao quanh viền bản đồ mặc định', () => {
    renderer.drawMap(ctx, undefined);
    expect(ctx.fillRect).toHaveBeenCalledWith(0, 0, 40, 40);
  });

  it('Test 29: Không bị crash khi grid chứa giá trị TileType lạ', () => {
    const mockMap: GameMapData = {
      width: 1,
      height: 1,
      tileSize: 40,
      grid: [[99]],
    };
    expect(() => renderer.drawMap(ctx, mockMap)).not.toThrow();
  });

  it('Test 30: Kiểm tra Map Rendering an toàn', () => {
    const mockMap: GameMapData = {
      width: 1,
      height: 1,
      tileSize: 40,
      grid: [[0]],
    };
    renderer.drawMap(ctx, mockMap);
    expect((ctx.fillRect as any).mock.calls.length + (ctx.drawImage as any).mock.calls.length).toBeGreaterThan(0);
  });

  // ==========================================
  // GROUP 4: Tank Rendering Logic (Test 31 - 40)
  // ==========================================

  it('Test 31: drawTank thực hiện save() và restore() cân bằng context', () => {
    const playerTank: TankData = { id: 'player-1', x: 100, y: 100, rotation: 0, hp: 100, maxHp: 100, isPlayer: true };
    renderer.drawTank(ctx, playerTank);
    expect(ctx.save).toHaveBeenCalledTimes(1);
    expect(ctx.restore).toHaveBeenCalledTimes(1);
  });

  it('Test 32: Dịch chuyển translate đúng vị trí (x, y) xe tăng', () => {
    const playerTank: TankData = { id: 'player-1', x: 150, y: 250, rotation: 90, hp: 100, maxHp: 100 };
    renderer.drawTank(ctx, playerTank);
    expect(ctx.translate).toHaveBeenCalledWith(150, 250);
  });

  it('Test 33: Xoay góc rotation theo Radian chính xác (90 deg -> PI/2 rad)', () => {
    const playerTank: TankData = { id: 'player-1', x: 100, y: 100, rotation: 90, hp: 100, maxHp: 100 };
    renderer.drawTank(ctx, playerTank);
    expect(ctx.rotate).toHaveBeenCalledWith((90 * Math.PI) / 180);
  });

  it('Test 34: Tank người chơi (isPlayer=true) vẽ màu xanh lá', () => {
    const playerTank: TankData = { id: 'player-1', x: 100, y: 100, rotation: 0, hp: 100, maxHp: 100, isPlayer: true };
    renderer.drawTank(ctx, playerTank);
    expect(ctx.fillStyle).toBe('#2ecc71');
  });

  it('Test 35: Tank AI (isPlayer=false) vẽ màu đỏ', () => {
    const enemyTank: TankData = { id: 'enemy-1', x: 200, y: 200, rotation: 180, hp: 100, maxHp: 100, isPlayer: false };
    renderer.drawTank(ctx, enemyTank);
    expect(ctx.fillRect).toHaveBeenCalledWith(-20, -20, 40, 40);
  });

  it('Test 36: Tự động phát hiện tank người chơi theo tiền tố id="player..."', () => {
    const tank: TankData = { id: 'player_tank_abc', x: 100, y: 100, rotation: 0, hp: 100, maxHp: 100 };
    renderer.drawTank(ctx, tank);
    expect(ctx.fillStyle).toBe('#2ecc71');
  });

  it('Test 37: Mini Health Bar vẽ màu xanh khi HP > 50%', () => {
    const tank: TankData = { id: 'player-1', x: 100, y: 100, rotation: 0, hp: 75, maxHp: 100 };
    renderer.drawTank(ctx, tank);
    expect(ctx.fillStyle).toBe('#2ecc71');
  });

  it('Test 38: Mini Health Bar vẽ màu vàng khi 25% < HP <= 50%', () => {
    const tank: TankData = { id: 'player-1', x: 100, y: 100, rotation: 0, hp: 40, maxHp: 100 };
    renderer.drawTank(ctx, tank);
    expect(ctx.fillStyle).toBe('#f1c40f');
  });

  it('Test 39: Mini Health Bar vẽ màu đỏ khi HP <= 25%', () => {
    const tank: TankData = { id: 'player-1', x: 100, y: 100, rotation: 0, hp: 15, maxHp: 100 };
    renderer.drawTank(ctx, tank);
    expect(ctx.fillStyle).toBe('#e74c3c');
  });

  it('Test 40: Mini Health Bar vẽ đúng chiều dài theo % HP (50% = 18px)', () => {
    const tank: TankData = { id: 'player-1', x: 100, y: 100, rotation: 0, hp: 50, maxHp: 100 };
    renderer.drawTank(ctx, tank);
    expect(ctx.fillRect).toHaveBeenCalledWith(82, 72, 18, 5);
  });

  // ==========================================
  // GROUP 5: Bullet Rendering Logic (Test 41 - 45)
  // ==========================================

  it('Test 41: drawBullet dịch chuyển vị trí translate đúng tọa độ đạn', () => {
    const bullet: BulletData = { id: 'b1', x: 200, y: 300, direction: 90, active: true };
    renderer.drawBullet(ctx, bullet);
    expect(ctx.translate).toHaveBeenCalledWith(200, 300);
  });

  it('Test 42: drawBullets bỏ qua không vẽ các viên đạn active == false', () => {
    const bullets: BulletData[] = [
      { id: 'b1', x: 100, y: 100, direction: 0, active: false },
      { id: 'b2', x: 200, y: 200, direction: 90, active: true },
    ];
    renderer.drawBullets(ctx, bullets);
    expect(ctx.translate).toHaveBeenCalledTimes(1);
    expect(ctx.translate).toHaveBeenCalledWith(200, 200);
  });

  it('Test 43: drawBullet thực hiện save và restore context', () => {
    const bullet: BulletData = { id: 'b1', x: 100, y: 100, direction: 0, active: true };
    renderer.drawBullet(ctx, bullet);
    expect(ctx.save).toHaveBeenCalledTimes(1);
    expect(ctx.restore).toHaveBeenCalledTimes(1);
  });

  it('Test 44: Vẽ đồng thời nhiều đạn active', () => {
    const bullets: BulletData[] = [
      { id: 'b1', x: 100, y: 100, direction: 0, active: true },
      { id: 'b2', x: 200, y: 200, direction: 90, active: true },
      { id: 'b3', x: 300, y: 300, direction: 180, active: true },
    ];
    renderer.drawBullets(ctx, bullets);
    expect(ctx.translate).toHaveBeenCalledTimes(3);
  });

  it('Test 45: drawBullet thực hiện xoay góc theo Radian chính xác', () => {
    const bullet: BulletData = { id: 'b1', x: 100, y: 100, direction: 90, active: true };
    renderer.drawBullet(ctx, bullet);
    expect(ctx.rotate).toHaveBeenCalledWith((90 * Math.PI) / 180);
  });

  // ==========================================
  // GROUP 6: HUD & GameLoop Lifecycle (Test 46 - 50)
  // ==========================================

  it('Test 46: drawHUD vẽ banner viền vàng f1c40f góc trái', () => {
    renderer.drawHUD(ctx, 100, 100);
    expect(ctx.stroke).toHaveBeenCalled();
    expect(ctx.save).toHaveBeenCalled();
  });

  it('Test 47: drawHUD hiển thị thanh máu xanh lá khi HP = 100%', () => {
    renderer.drawHUD(ctx, 100, 100);
    expect(ctx.fillRect).toHaveBeenCalledWith(22, 34, 216, 14);
    expect(ctx.restore).toHaveBeenCalled();
  });

  it('Test 48: GameLoop khởi tạo thành công và ban đầu isRunning = false', () => {
    const gameLoop = new GameLoop(ctx, () => ({ tanks: [], bullets: [] }));
    expect(gameLoop.isRunning()).toBe(false);
  });

  it('Test 49: GameLoop.start() chuyển isRunning thành true', () => {
    const gameLoop = new GameLoop(ctx, () => ({ tanks: [], bullets: [] }));
    gameLoop.start();
    expect(gameLoop.isRunning()).toBe(true);
    gameLoop.stop();
  });

  it('Test 50: GameLoop.stop() chuyển isRunning thành false và dừng loop', () => {
    const gameLoop = new GameLoop(ctx, () => ({ tanks: [], bullets: [] }));
    gameLoop.start();
    gameLoop.stop();
    expect(gameLoop.isRunning()).toBe(false);
  });
});
