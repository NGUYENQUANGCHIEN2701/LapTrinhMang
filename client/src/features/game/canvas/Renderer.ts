import { COLORS, TILE_SIZE } from '../../../shared/constants/game';
import { GameRenderState } from '../types';

export class Renderer {
  private ctx: CanvasRenderingContext2D;

  constructor(ctx: CanvasRenderingContext2D) {
    this.ctx = ctx;
  }

  public render(state: GameRenderState): void {
    const { width, height } = this.ctx.canvas;

    // Clear background
    this.ctx.fillStyle = COLORS.BACKGROUND;
    this.ctx.fillRect(0, 0, width, height);

    // Draw Grid Lines (Subtle)
    this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
    this.ctx.lineWidth = 1;
    for (let x = 0; x < width; x += TILE_SIZE) {
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x, height);
      this.ctx.stroke();
    }
    for (let y = 0; y < height; y += TILE_SIZE) {
      this.ctx.beginPath();
      this.ctx.moveTo(0, y);
      this.ctx.lineTo(width, y);
      this.ctx.stroke();
    }

    // Render Tanks
    state.tanks.forEach((tank) => {
      if (!tank.isAlive) return;

      this.ctx.save();
      this.ctx.translate(tank.position.x, tank.position.y);

      // Tank Body
      this.ctx.fillStyle = tank.type === 'player' ? COLORS.PLAYER_TANK : COLORS.ENEMY_TANK;
      this.ctx.fillRect(-16, -16, 32, 32);

      // Tank Cannon / Turret
      this.ctx.fillStyle = '#ffffff';
      switch (tank.direction) {
        case 'UP':
          this.ctx.fillRect(-3, -22, 6, 14);
          break;
        case 'DOWN':
          this.ctx.fillRect(-3, 8, 6, 14);
          break;
        case 'LEFT':
          this.ctx.fillRect(-22, -3, 14, 6);
          break;
        case 'RIGHT':
          this.ctx.fillRect(8, -3, 14, 6);
          break;
      }

      // HP Bar Overlay
      const hpPercentage = Math.max(0, tank.hp / tank.maxHp);
      this.ctx.fillStyle = 'rgba(0, 0, 0, 0.6)';
      this.ctx.fillRect(-18, -26, 36, 5);

      this.ctx.fillStyle = tank.type === 'player' ? '#00e676' : '#ff5252';
      this.ctx.fillRect(-18, -26, 36 * hpPercentage, 5);

      this.ctx.restore();
    });

    // Render Bullets
    state.bullets.forEach((bullet) => {
      this.ctx.fillStyle = COLORS.BULLET;
      this.ctx.beginPath();
      this.ctx.arc(bullet.position.x, bullet.position.y, 4, 0, Math.PI * 2);
      this.ctx.fill();
    });
  }
}
