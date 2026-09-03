import { Direction } from '../../shared/types/game';

/**
 * Bảng ánh xạ phím vật lý → Direction logic.
 * Hỗ trợ cả WASD và phím mũi tên.
 */
const KEY_TO_DIRECTION: Record<string, Direction> = {
  w: 'UP',
  W: 'UP',
  ArrowUp: 'UP',
  s: 'DOWN',
  S: 'DOWN',
  ArrowDown: 'DOWN',
  a: 'LEFT',
  A: 'LEFT',
  ArrowLeft: 'LEFT',
  d: 'RIGHT',
  D: 'RIGHT',
  ArrowRight: 'RIGHT',
};

const SHOOT_KEY = ' '; // Space

/**
 * Callback khi trạng thái input thay đổi.
 */
export interface InputCallbacks {
  /** Gọi khi bấm/nhả phím di chuyển. moving=true → bấm, moving=false → nhả */
  onMove: (direction: Direction, moving: boolean) => void;
  /** Gọi khi bấm phím bắn (Space) */
  onShoot: () => void;
}

/**
 * InputHandler - lắng nghe keydown/keyup, chỉ gửi callback khi THAY ĐỔI trạng thái.
 *
 * - Giữ phím liên tục (e.repeat) → bỏ qua, không gọi callback
 * - Bấm W + ArrowUp cùng lúc → chỉ gửi 1 lần MOVE UP (cùng direction)
 * - Nhả W nhưng vẫn giữ ArrowUp → không gửi STOP cho UP
 * - Bấm W + D → gửi MOVE UP + MOVE RIGHT (2 direction đồng thời)
 */
export class InputHandler {
  /** Set chứa các phím vật lý đang được giữ (e.key) */
  private pressedKeys = new Set<string>();

  /** Set chứa các direction logic đang active */
  private activeDirections = new Set<Direction>();

  private callbacks: InputCallbacks;

  private readonly boundKeyDown: (e: KeyboardEvent) => void;
  private readonly boundKeyUp: (e: KeyboardEvent) => void;

  constructor(callbacks: InputCallbacks) {
    this.callbacks = callbacks;
    this.boundKeyDown = this.handleKeyDown.bind(this);
    this.boundKeyUp = this.handleKeyUp.bind(this);
  }

  /**
   * Gắn event listener vào window. Gọi 1 lần khi mount.
   */
  public attach(): void {
    window.addEventListener('keydown', this.boundKeyDown);
    window.addEventListener('keyup', this.boundKeyUp);
  }

  /**
   * Gỡ event listener và reset state. Gọi khi unmount.
   */
  public detach(): void {
    window.removeEventListener('keydown', this.boundKeyDown);
    window.removeEventListener('keyup', this.boundKeyUp);
    this.pressedKeys.clear();
    this.activeDirections.clear();
  }

  private handleKeyDown(e: KeyboardEvent): void {
    const direction = KEY_TO_DIRECTION[e.key];
    const isShoot = e.key === SHOOT_KEY;

    // Chặn hành vi mặc định cho phím game (tránh scroll trang)
    if (direction || isShoot) {
      e.preventDefault();
    }

    // Bỏ qua sự kiện repeat (giữ phím liên tục) → không spam
    if (e.repeat) return;

    // Xử lý phím di chuyển
    if (direction) {
      this.pressedKeys.add(e.key);

      // Chỉ gửi nếu direction này chưa active
      // (vd: đã bấm W rồi, giờ bấm thêm ArrowUp → không gửi lại UP)
      if (!this.activeDirections.has(direction)) {
        this.activeDirections.add(direction);
        this.callbacks.onMove(direction, true);
      }
      return;
    }

    // Xử lý phím bắn
    if (isShoot) {
      this.pressedKeys.add(e.key);
      this.callbacks.onShoot();
    }
  }

  private handleKeyUp(e: KeyboardEvent): void {
    const direction = KEY_TO_DIRECTION[e.key];

    // Xử lý nhả phím di chuyển
    if (direction) {
      this.pressedKeys.delete(e.key);

      // Kiểm tra xem còn phím nào khác đang map tới cùng direction không
      // (vd: nhả W nhưng ArrowUp vẫn đang giữ → UP vẫn active)
      const stillActive = Array.from(this.pressedKeys).some(
        (key) => KEY_TO_DIRECTION[key] === direction,
      );

      if (!stillActive && this.activeDirections.has(direction)) {
        this.activeDirections.delete(direction);
        this.callbacks.onMove(direction, false);
      }
      return;
    }

    // Xử lý nhả phím bắn
    if (e.key === SHOOT_KEY) {
      this.pressedKeys.delete(e.key);
    }
  }

  /** Trả về tập hợp các direction đang active (copy) */
  public getActiveDirections(): Set<Direction> {
    return new Set(this.activeDirections);
  }

  /** Kiểm tra có đang di chuyển không */
  public isMoving(): boolean {
    return this.activeDirections.size > 0;
  }
}
