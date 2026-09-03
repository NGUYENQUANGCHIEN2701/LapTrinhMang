import { useEffect, useRef, useCallback } from 'react';
import { globalWebSocketClient } from '../../network/WebSocketClient';
import { MessageBuilder } from '../../network/utils/MessageBuilder';
import { InputHandler } from '../InputHandler';
import { Direction } from '../../../shared/types/game';

/**
 * Hook kết nối InputHandler với WebSocket.
 *
 * - Bấm phím di chuyển → gửi MOVE_INPUT { direction, moving: true }
 * - Nhả phím di chuyển  → gửi MOVE_INPUT { direction, moving: false }
 * - Bấm Space           → gửi SHOOT_INPUT
 *
 * Chỉ gửi khi THAY ĐỔI trạng thái, không spam mỗi frame.
 */
export const useInput = () => {
  const handlerRef = useRef<InputHandler | null>(null);

  useEffect(() => {
    const handler = new InputHandler({
      onMove: (direction: Direction, moving: boolean) => {
        const message = MessageBuilder.buildMoveInput(direction, moving);
        globalWebSocketClient.sendRaw(message);
      },
      onShoot: () => {
        const message = MessageBuilder.buildShootInput();
        globalWebSocketClient.sendRaw(message);
      },
    });

    handler.attach();
    handlerRef.current = handler;

    return () => {
      handler.detach();
      handlerRef.current = null;
    };
  }, []);

  /** Lấy tập hợp direction đang active (cho debug/UI) */
  const getActiveDirections = useCallback((): Set<Direction> => {
    return handlerRef.current?.getActiveDirections() ?? new Set<Direction>();
  }, []);

  /** Kiểm tra player đang di chuyển không */
  const isMoving = useCallback((): boolean => {
    return handlerRef.current?.isMoving() ?? false;
  }, []);

  return { getActiveDirections, isMoving };
};
