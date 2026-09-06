import { SpriteManager } from './SpriteManager';

export interface GameMapData {
  width: number;
  height: number;
  tileSize: number;
  grid: number[][];
}

export interface TankData {
  id: string;
  x: number;
  y: number;
  rotation: number; // 0: UP, 90: RIGHT, 180: DOWN, 270: LEFT
  hp: number;
  maxHp: number;
  isPlayer?: boolean;
  alive?: boolean; // false khi tank bị phá hủy (HP <= 0)
}

export interface BulletData {
  id: string;
  x: number;
  y: number;
  direction: number;
  active: boolean;
}

export interface GameStateData {
  map?: GameMapData;
  tanks: TankData[];
  bullets: BulletData[];
  playerHP?: number;
  maxHP?: number;
  score?: number;
  gameStatus?: 'WAITING' | 'PLAYING' | 'VICTORY' | 'DEFEAT' | string;
}

export class Renderer {
  private spriteManager: SpriteManager;
  private readonly CANVAS_WIDTH = 800;
  private readonly CANVAS_HEIGHT = 600;

  // Cache Offscreen Canvas cho Bản Đồ (Pre-rendered Map)
  private mapCanvas: HTMLCanvasElement | null = null;
  private mapCtx: CanvasRenderingContext2D | null = null;
  private cachedMapHash: string = '';
  private wasMapSpriteReady: boolean = false;

  // Hiệu ứng nổ khi tank bị phá hủy: tankId -> { x, y, startTime }
  private destructionEffects: Map<string, { x: number; y: number; startTime: number }> = new Map();
  private static readonly EXPLOSION_DURATION_MS = 600;

  constructor() {
    this.spriteManager = SpriteManager.getInstance();
  }

  /**
   * Xóa sạch màn hình Canvas trước mỗi frame
   */
  public clear(ctx: CanvasRenderingContext2D): void {
    ctx.clearRect(0, 0, this.CANVAS_WIDTH, this.CANVAS_HEIGHT);
    ctx.fillStyle = '#1e272e';
    ctx.fillRect(0, 0, this.CANVAS_WIDTH, this.CANVAS_HEIGHT);
    ctx.imageSmoothingEnabled = false;
  }

  /**
   * Vẽ lưới bản đồ (20x15 ô).
   */
  public drawMap(ctx: CanvasRenderingContext2D, mapData?: GameMapData): void {
    if (!mapData || !mapData.grid) {
      this.drawDefaultMap(ctx);
      return;
    }

    const currentHash = this.generateMapHash(mapData);
    const isMapReady =
      this.spriteManager.isReady('ground') &&
      this.spriteManager.isReady('wall') &&
      this.spriteManager.isReady('bush') &&
      this.spriteManager.isReady('spikes');
    const needsSpriteUpdate = isMapReady && !this.wasMapSpriteReady;

    if (!this.mapCanvas || this.cachedMapHash !== currentHash || needsSpriteUpdate) {
      if (needsSpriteUpdate) {
        this.wasMapSpriteReady = true;
      }
      this.rebuildMapCache(mapData, currentHash, ctx);
    }

    if (this.mapCanvas && this.mapCtx) {
      ctx.drawImage(this.mapCanvas, 0, 0);
    }
  }

  private generateMapHash(mapData: GameMapData): string {
    return `${mapData.width}_${mapData.height}_${mapData.tileSize}_${JSON.stringify(mapData.grid)}`;
  }

  /**
   * Render trước toàn bộ 20x15 ô bản đồ lên Offscreen Canvas
   */
  private rebuildMapCache(mapData: GameMapData, hash: string, targetCtx: CanvasRenderingContext2D): void {
    if (typeof document !== 'undefined' && document.createElement) {
      if (!this.mapCanvas) {
        this.mapCanvas = document.createElement('canvas');
        this.mapCanvas.width = this.CANVAS_WIDTH;
        this.mapCanvas.height = this.CANVAS_HEIGHT;
        this.mapCtx = this.mapCanvas.getContext('2d');
      }
    }

    const drawTarget = this.mapCtx || targetCtx;
    drawTarget.clearRect(0, 0, this.CANVAS_WIDTH, this.CANVAS_HEIGHT);
    drawTarget.imageSmoothingEnabled = false;

    const tileSize = mapData.tileSize || 40;
    const rows = mapData.height || 15;
    const cols = mapData.width || 20;

    const groundSprite = this.spriteManager.getSprite('ground');
    const wallSprite = this.spriteManager.getSprite('wall');
    const bushSprite = this.spriteManager.getSprite('bush');
    const spikesSprite = this.spriteManager.getSprite('spikes');
    const isGroundReady = this.spriteManager.isReady('ground');
    const isWallReady = this.spriteManager.isReady('wall');
    const isBushReady = this.spriteManager.isReady('bush');
    const isSpikesReady = this.spriteManager.isReady('spikes');

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const tileType = mapData.grid[r][c];
        const px = c * tileSize;
        const py = r * tileSize;

        // Ô đất nền / cỏ thường (TileType = 0, 3, 4)
        if (tileType === 0 || tileType === 3 || tileType === 4) {
          if (isGroundReady && groundSprite) {
            const tileVariant = (r * 3 + c * 7) % 5;
            drawTarget.drawImage(groundSprite, tileVariant * 20, 0, 20, 20, px, py, tileSize, tileSize);
          } else {
            drawTarget.fillStyle = '#f5f5dc';
            drawTarget.fillRect(px, py, tileSize, tileSize);
            drawTarget.strokeStyle = '#e6e6cc';
            drawTarget.strokeRect(px, py, tileSize, tileSize);
          }
        }
        // Ô tường đá / gạch (TileType = 1)
        else if (tileType === 1) {
          if (isWallReady && wallSprite) {
            const tileVariant = (r + c) % 6;
            drawTarget.drawImage(wallSprite, tileVariant * 20, 0, 20, 20, px, py, tileSize, tileSize);
          } else {
            drawTarget.fillStyle = '#7f8c8d';
            drawTarget.fillRect(px, py, tileSize, tileSize);
            drawTarget.strokeStyle = '#2c3e50';
            drawTarget.lineWidth = 2;
            drawTarget.strokeRect(px, py, tileSize, tileSize);
          }
        }
        // Ô chông gai sắt chống tăng (TileType = 2: Chặn xe nhưng cho đạn bay xuyên qua)
        else if (tileType === 2) {
          // Vẽ nền đất cỏ bên dưới trước (vì spikes.png là ảnh có nền trong suốt)
          if (isGroundReady && groundSprite) {
            const tileVariant = (r * 3 + c * 7) % 5;
            drawTarget.drawImage(groundSprite, tileVariant * 20, 0, 20, 20, px, py, tileSize, tileSize);
          } else {
            drawTarget.fillStyle = '#556b2f';
            drawTarget.fillRect(px, py, tileSize, tileSize);
          }

          // Vẽ chông gai sắt lên trên
          if (isSpikesReady && spikesSprite) {
            const spikeFrame = (r + c) % 3;
            drawTarget.drawImage(spikesSprite, spikeFrame * 20, 0, 20, 20, px, py, tileSize, tileSize);
          } else {
            // Fallback nếu ảnh spikes chưa sẵn sàng
            drawTarget.fillStyle = '#718093';
            drawTarget.fillRect(px + 4, py + 4, tileSize - 8, tileSize - 8);
            drawTarget.strokeStyle = '#2f3640';
            drawTarget.lineWidth = 2;
            drawTarget.strokeRect(px + 4, py + 4, tileSize - 8, tileSize - 8);
          }
        }
        // Ô bụi cây / cây cối (TileType = 5) từ palms.png
        else if (tileType === 5) {
          if (isGroundReady && groundSprite) {
            const tileVariant = (r * 3 + c * 7) % 5;
            drawTarget.drawImage(groundSprite, tileVariant * 20, 0, 20, 20, px, py, tileSize, tileSize);
          }
          if (isBushReady && bushSprite) {
            const bushFrame = (r * 2 + c * 3) % 5;
            drawTarget.drawImage(bushSprite, bushFrame * 88, 0, 88, 88, px, py, tileSize, tileSize);
          } else {
            drawTarget.fillStyle = '#27ae60';
            drawTarget.fillRect(px, py, tileSize, tileSize);
          }
        }
      }
    }

    this.cachedMapHash = hash;
  }

  private drawDefaultMap(ctx: CanvasRenderingContext2D): void {
    const tileSize = 40;
    const cols = 20;
    const rows = 15;

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const px = c * tileSize;
        const py = r * tileSize;
        const isBorder = r === 0 || r === rows - 1 || c === 0 || c === cols - 1;

        if (isBorder) {
          ctx.fillStyle = '#7f8c8d';
          ctx.fillRect(px, py, tileSize, tileSize);
        } else {
          ctx.fillStyle = '#f5f5dc';
          ctx.fillRect(px, py, tileSize, tileSize);
          ctx.strokeStyle = '#e6e6cc';
          ctx.strokeRect(px, py, tileSize, tileSize);
        }
      }
    }
  }

  public drawTanks(ctx: CanvasRenderingContext2D, tanks: TankData[]): void {
    if (!tanks) return;

    const now = performance.now();

    for (const tank of tanks) {
      // Tank bị phá hủy (alive=false hoặc hp<=0) -> vẽ hiệu ứng nổ thay vì tank
      if (tank.alive === false || tank.hp <= 0) {
        if (!this.destructionEffects.has(tank.id)) {
          this.destructionEffects.set(tank.id, { x: tank.x, y: tank.y, startTime: now });
        }
        continue;
      }
      this.drawTank(ctx, tank);
    }

    // Vẽ hiệu ứng nổ cho các tank vừa bị phá hủy
    this.drawDestructionEffects(ctx, now);
  }

  public drawTank(ctx: CanvasRenderingContext2D, tank: TankData): void {
    const isPlayer = tank.isPlayer || tank.id.startsWith('player');
    const spriteKey = isPlayer ? 'player_tank' : 'enemy_tank';
    const tankSprite = this.spriteManager.getSprite(spriteKey);

    ctx.save();
    ctx.translate(tank.x, tank.y);

    const size = 40;
    const halfSize = size / 2;

    if (this.spriteManager.isReady(spriteKey) && tankSprite) {
      // Spritesheet của Tankzors gồm 16 frame góc (mỗi frame 22.5 độ theo chiều kim đồng hồ):
      // Frame 0: DOWN (180°), Frame 4: LEFT (270°), Frame 8: UP (0°), Frame 12: RIGHT (90°)
      const normalizedRotation = ((tank.rotation % 360) + 360) % 360;
      const frameIndex = (Math.floor((normalizedRotation + 11.25) / 22.5) + 8) % 16;
      const sx = frameIndex * 24;

      ctx.drawImage(tankSprite, sx, 0, 24, 24, -halfSize, -halfSize, size, size);
    } else {
      const rad = (tank.rotation * Math.PI) / 180;
      ctx.rotate(rad);

      ctx.fillStyle = isPlayer ? '#2ecc71' : '#e74c3c';
      ctx.fillRect(-halfSize, -halfSize, size, size);

      ctx.strokeStyle = '#2c3e50';
      ctx.lineWidth = 2;
      ctx.strokeRect(-halfSize, -halfSize, size, size);

      ctx.fillStyle = '#34495e';
      ctx.fillRect(-4, -halfSize - 10, 8, 14);
    }

    ctx.restore();

    this.drawMiniHealthBar(ctx, tank.x, tank.y, tank.hp, tank.maxHp || 100);
  }

  private drawMiniHealthBar(
    ctx: CanvasRenderingContext2D,
    x: number,
    y: number,
    hp: number,
    maxHp: number
  ): void {
    const barWidth = 36;
    const barHeight = 5;
    const bx = x - barWidth / 2;
    const by = y - 28;

    // Nền đen
    ctx.fillStyle = 'rgba(0, 0, 0, 0.6)';
    ctx.fillRect(bx - 1, by - 1, barWidth + 2, barHeight + 2);

    // Thanh máu: xanh > 60%, vàng 30-60%, đỏ < 30%
    const pct = Math.max(0, Math.min(1, hp / maxHp));
    ctx.fillStyle = pct > 0.6 ? '#2ecc71' : pct > 0.3 ? '#f1c40f' : '#e74c3c';
    ctx.fillRect(bx, by, barWidth * pct, barHeight);

    // Hiển thị số HP phía trên thanh máu (vd: "75/100")
    ctx.save();
    ctx.fillStyle = '#ffffff';
    ctx.font = '8px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(`${hp}/${maxHp}`, x, by - 2);
    ctx.restore();
  }

  /**
   * Vẽ hiệu ứng nổ tại vị trí các tank vừa bị phá hủy.
   * Hiệu ứng kéo dài EXPLOSION_DURATION_MS rồi tự xóa.
   */
  private drawDestructionEffects(ctx: CanvasRenderingContext2D, now: number): void {
    const expiredIds: string[] = [];

    this.destructionEffects.forEach((effect, id) => {
      const elapsed = now - effect.startTime;

      if (elapsed > Renderer.EXPLOSION_DURATION_MS) {
        expiredIds.push(id);
        return;
      }

      // Tỷ lệ animation: 0 -> 1 theo thời gian
      const progress = elapsed / Renderer.EXPLOSION_DURATION_MS;
      // Fade out dần
      const alpha = 1 - progress;
      // Scale từ 1x lên 1.5x
      const scale = 1 + progress * 0.5;
      const size = 48 * scale;

      ctx.save();
      ctx.globalAlpha = alpha;
      ctx.translate(effect.x, effect.y);

      const explosionSprite = this.spriteManager.getSprite('explosion_big');
      if (this.spriteManager.isReady('explosion_big') && explosionSprite) {
        ctx.drawImage(explosionSprite, -size / 2, -size / 2, size, size);
      } else {
        // Fallback: vẽ vòng tròn nổ màu cam/đỏ
        const gradient = ctx.createRadialGradient(0, 0, 0, 0, 0, size / 2);
        gradient.addColorStop(0, '#ff6600');
        gradient.addColorStop(0.5, '#ff3300');
        gradient.addColorStop(1, 'rgba(255, 0, 0, 0)');
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(0, 0, size / 2, 0, Math.PI * 2);
        ctx.fill();
      }

      ctx.restore();
    });

    // Dọn dẹp hiệu ứng hết hạn
    for (const id of expiredIds) {
      this.destructionEffects.delete(id);
    }
  }

  public drawBullets(ctx: CanvasRenderingContext2D, bullets: BulletData[]): void {
    if (!bullets) return;

    for (const bullet of bullets) {
      if (bullet.active !== false) {
        this.drawBullet(ctx, bullet);
      }
    }
  }

  public drawBullet(ctx: CanvasRenderingContext2D, bullet: BulletData): void {
    const bulletSprite = this.spriteManager.getSprite('bullet');
    const angle = bullet.direction ?? 0;
    const rad = (angle * Math.PI) / 180;

    ctx.save();
    ctx.translate(bullet.x, bullet.y);
    ctx.rotate(rad);

    if (this.spriteManager.isReady('bullet') && bulletSprite) {
      // Frame 0 trong bullet.png chuẩn (hướng UP 0 độ) và xoay qua rad
      ctx.drawImage(bulletSprite, 0, 0, 24, 24, -8, -8, 16, 16);
    } else if (bulletSprite) {
      ctx.drawImage(bulletSprite, 0, 0, 24, 24, -8, -8, 16, 16);
    }
    ctx.restore();
  }

  public drawHUD(
    ctx: CanvasRenderingContext2D,
    playerHP: number = 100,
    maxHP: number = 100
  ): void {
    ctx.save();

    ctx.fillStyle = 'rgba(0, 0, 0, 0.65)';
    ctx.roundRect ? ctx.roundRect(10, 10, 240, 45, 8) : ctx.fillRect(10, 10, 240, 45);
    ctx.fill();

    ctx.strokeStyle = '#f1c40f';
    ctx.lineWidth = 1.5;
    ctx.stroke();

    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 13px sans-serif';
    ctx.fillText('PLAYER HP', 22, 28);

    const barX = 22;
    const barY = 34;
    const barW = 216;
    const barH = 14;

    ctx.fillStyle = '#2c3e50';
    ctx.fillRect(barX, barY, barW, barH);

    const pct = Math.max(0, Math.min(1, playerHP / maxHP));
    ctx.fillStyle = pct > 0.6 ? '#2ecc71' : pct > 0.3 ? '#f1c40f' : '#e74c3c';
    ctx.fillRect(barX, barY, barW * pct, barH);

    ctx.strokeStyle = '#ecf0f1';
    ctx.lineWidth = 1;
    ctx.strokeRect(barX, barY, barW, barH);

    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 11px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(`${playerHP} / ${maxHP}`, barX + barW / 2, barY + 11);

    ctx.restore();
  }
}
