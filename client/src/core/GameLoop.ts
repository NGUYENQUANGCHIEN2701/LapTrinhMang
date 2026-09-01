import { Renderer, GameStateData } from './Renderer';

export class GameLoop {
  private renderer: Renderer;
  private ctx: CanvasRenderingContext2D;
  private animationFrameId: number | null = null;
  private running: boolean = false;
  private getStateCallback: () => GameStateData;

  // Đo đạc FPS
  private lastTime: number = 0;
  private fps: number = 60;
  private frameCount: number = 0;
  private lastFpsUpdate: number = 0;

  constructor(ctx: CanvasRenderingContext2D, getStateCallback: () => GameStateData) {
    this.ctx = ctx;
    this.renderer = new Renderer();
    this.getStateCallback = getStateCallback;
  }

  /**
   * Bắt đầu vòng lặp Render 60fps
   */
  public start(): void {
    if (this.running) return;

    this.running = true;
    this.lastTime = performance.now();
    this.lastFpsUpdate = performance.now();

    const loop = (currentTime: number) => {
      if (!this.running) return;

      // Tính deltaTime và FPS
      const deltaTime = (currentTime - this.lastTime) / 1000;
      this.lastTime = currentTime;

      this.frameCount++;
      if (currentTime - this.lastFpsUpdate >= 1000) {
        this.fps = this.frameCount;
        this.frameCount = 0;
        this.lastFpsUpdate = currentTime;
      }

      // Lấy trạng thái game mới nhất và vẽ
      const state = this.getStateCallback();
      this.renderFrame(state);

      this.animationFrameId = requestAnimationFrame(loop);
    };

    this.animationFrameId = requestAnimationFrame(loop);
    console.log('🚀 [GameLoop] Đã khởi chạy vòng lặp Canvas Render 60fps.');
  }

  /**
   * Dừng vòng lặp Render
   */
  public stop(): void {
    this.running = false;
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }
    console.log('🛑 [GameLoop] Đã dừng vòng lặp Canvas Render.');
  }

  /**
   * Thực hiện vẽ 1 khung hình (frame)
   */
  private renderFrame(state: GameStateData): void {
    // 1. Xóa màn hình
    this.renderer.clear(this.ctx);

    // 2. Vẽ bản đồ (20x15 ô)
    this.renderer.drawMap(this.ctx, state?.map);

    // 3. Vẽ xe tăng (Player & AI)
    this.renderer.drawTanks(this.ctx, state?.tanks);

    // 4. Vẽ danh sách đạn
    this.renderer.drawBullets(this.ctx, state?.bullets);

    // 5. Vẽ thanh máu HP & HUD
    this.renderer.drawHUD(this.ctx, state?.playerHP ?? 100, state?.maxHP ?? 100);

    // 6. Vẽ thông số FPS nhỏ ở góc phải
    this.drawFPS(this.ctx);
  }

  private drawFPS(ctx: CanvasRenderingContext2D): void {
    ctx.save();
    ctx.fillStyle = '#00ff00';
    ctx.font = '12px monospace';
    ctx.fillText(`FPS: ${this.fps}`, 730, 25);
    ctx.restore();
  }

  public isRunning(): boolean {
    return this.running;
  }

  public getFPS(): number {
    return this.fps;
  }
}
