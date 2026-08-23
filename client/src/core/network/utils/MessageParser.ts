import { BasePacket } from '../types';

export interface ParseResult<T = BasePacket> {
  success: boolean;
  data?: T;
  raw: string;
  error?: string;
}

export class MessageParser {
  /**
   * Parse chuỗi tin nhắn JSON từ server thành object TypeScript
   */
  public static parse<T = BasePacket>(rawMessage: string): ParseResult<T> {
    try {
      if (!rawMessage || typeof rawMessage !== 'string') {
        return {
          success: false,
          raw: rawMessage,
          error: 'Tin nhắn không hợp lệ',
        };
      }

      const parsed = JSON.parse(rawMessage) as T;
      console.log('[WebSocket] Received:', parsed);

      return {
        success: true,
        data: parsed,
        raw: rawMessage,
      };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Lỗi parse JSON';
      console.error('[MessageParser] Parse error:', errorMessage, 'Raw:', rawMessage);
      return {
        success: false,
        raw: rawMessage,
        error: errorMessage,
      };
    }
  }
}
