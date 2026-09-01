/**
 * SpriteManager: Quản lý nạp trước (preload) và lưu trữ cache hình ảnh cho Renderer.
 * Tự động phân loại assets thành tanks, map, effects, ui.
 * Có cơ chế fallback mượt mà nếu tài nguyên hình ảnh chưa sẵn sàng.
 */

export interface SpriteMap {
  [key: string]: HTMLImageElement;
}

export class SpriteManager {
  private static instance: SpriteManager;
  private sprites: SpriteMap = {};
  private loadedCount: number = 0;
  private totalCount: number = 0;
  private isAllLoaded: boolean = false;

  // Cấu hình các tài nguyên ảnh được phân loại gọn gàng
  private readonly ASSET_PATHS: Record<string, string> = {
    // Tanks
    player_tank: '/assets/tanks/player_tank.png',
    enemy_tank: '/assets/tanks/enemy_tank.png',
    kamikaze_tank: '/assets/tanks/kamikaze_tank.png',
    turret_tank: '/assets/tanks/turret_tank.png',

    // Map
    wall: '/assets/map/wall.png',
    ground: '/assets/map/ground.png',
    bush: '/assets/map/palms.png',

    // Effects
    bullet: '/assets/effects/bullet.png',
    explosion_small: '/assets/effects/explosion_small.png',
    explosion_big: '/assets/effects/explosion_big.png',

    // UI
    hp_bar: '/assets/ui/hp_bar.png',
    star: '/assets/ui/star.png',
    coin: '/assets/ui/coin.png',
  };

  private constructor() {}

  public static getInstance(): SpriteManager {
    if (!SpriteManager.instance) {
      SpriteManager.instance = new SpriteManager();
    }
    return SpriteManager.instance;
  }

  /**
   * Nạp trước toàn bộ các hình ảnh game bất đồng bộ.
   */
  public async loadAll(): Promise<void> {
    const keys = Object.keys(this.ASSET_PATHS);
    this.totalCount = keys.length;
    this.loadedCount = 0;

    const promises = keys.map((key) => this.loadImage(key, this.ASSET_PATHS[key]));

    try {
      await Promise.all(promises);
      this.isAllLoaded = true;
      console.log(`[SpriteManager] Nạp thành công ${this.loadedCount}/${this.totalCount} ảnh assets.`);
    } catch (error) {
      console.warn('[SpriteManager] Một số tài nguyên ảnh chưa sẵn sàng. Chuyển sang chế độ vẽ Fallback.', error);
    }
  }

  private loadImage(key: string, src: string): Promise<HTMLImageElement> {
    return new Promise((resolve) => {
      const img = new Image();
      img.src = src;

      img.onload = () => {
        this.sprites[key] = img;
        this.loadedCount++;
        resolve(img);
      };

      img.onerror = () => {
        console.warn(`[SpriteManager] Không thể tải ảnh: ${src}. Sẽ vẽ hình học thay thế.`);
        resolve(img); // Resolve để không ném lỗi gián đoạn Promise.all
      };
    });
  }

  /**
   * Lấy ảnh sprite theo key
   */
  public getSprite(key: string): HTMLImageElement | null {
    return this.sprites[key] || null;
  }

  /**
   * Kiểm tra ảnh đã tải xong chưa
   */
  public isReady(key: string): boolean {
    const img = this.sprites[key];
    return !!img && img.complete && img.naturalWidth !== 0;
  }

  public getProgress(): number {
    return this.totalCount === 0 ? 1 : this.loadedCount / this.totalCount;
  }
}
