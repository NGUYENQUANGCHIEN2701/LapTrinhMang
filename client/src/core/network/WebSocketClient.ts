import { DEFAULT_SERVER_URL } from '../../shared/constants/game';
import { MessageParser } from './utils/MessageParser';
import {
  ConnectionStatus,
  OnCloseListener,
  OnErrorListener,
  OnMessageListener,
  OnOpenListener,
} from './types';

export class WebSocketClient {
  private socket: WebSocket | null = null;
  private url: string;
  private status: ConnectionStatus = 'disconnected';

  // Retry Auto-Reconnect config
  private reconnectAttempts: number = 0;
  private maxRetries: number = 3;
  private retryDelayMs: number = 2000;
  private isIntentionallyClosed: boolean = false;

  // Event Listeners Sets
  private openListeners: Set<OnOpenListener> = new Set();
  private messageListeners: Set<OnMessageListener> = new Set();
  private closeListeners: Set<OnCloseListener> = new Set();
  private errorListeners: Set<OnErrorListener> = new Set();

  constructor(url: string = DEFAULT_SERVER_URL) {
    this.url = url;
  }

  /**
   * Khởi tạo kết nối đến WebSocket Server
   */
  public connect(): void {
    if (this.socket && (this.socket.readyState === WebSocket.CONNECTING || this.socket.readyState === WebSocket.OPEN)) {
      return;
    }

    this.isIntentionallyClosed = false;
    this.setStatus('connecting');
    console.log(`🔌 [WebSocketClient] Connecting to ${this.url}... (Lần thử ${this.reconnectAttempts + 1}/${this.maxRetries})`);

    try {
      this.socket = new WebSocket(this.url);

      this.socket.onopen = () => {
        this.reconnectAttempts = 0;
        this.setStatus('connected');
        console.log('✅ [WebSocketClient] Connected successfully to', this.url);
        this.openListeners.forEach((listener) => listener());
      };

      this.socket.onmessage = (event: MessageEvent) => {
        const parseResult = MessageParser.parse(event.data);
        if (parseResult.success && parseResult.data) {
          this.messageListeners.forEach((listener) => listener(parseResult.data));
        }
      };

      this.socket.onerror = (event: Event) => {
        console.error('❌ [WebSocketClient] Socket error:', event);
        this.setStatus('error');
        this.errorListeners.forEach((listener) => listener(event));
      };

      this.socket.onclose = (event: CloseEvent) => {
        console.warn('⚠️ [WebSocketClient] Connection closed code:', event.code, 'reason:', event.reason);
        this.setStatus('disconnected');
        this.closeListeners.forEach((listener) => listener(event));

        if (!this.isIntentionallyClosed) {
          this.handleAutoReconnect();
        } else {
          this.setStatus('disconnected');
        }
      };
    } catch (err) {
      console.error('❌ [WebSocketClient] Exception on connect:', err);
      this.setStatus('error');
      this.handleAutoReconnect();
    }
  }

  /**
   * Tự động kết nối lại khi mất kết nối (retry tối đa 3 lần)
   */
  private handleAutoReconnect(): void {
    if (this.reconnectAttempts < this.maxRetries) {
      this.reconnectAttempts++;
      this.setStatus('connecting');
      console.log(
        `🔄 [WebSocketClient] Retrying connection in ${this.retryDelayMs / 1000}s... (Lần ${this.reconnectAttempts}/${this.maxRetries})`
      );

      setTimeout(() => {
        this.connect();
      }, this.retryDelayMs);
    } else {
      console.error(`[WebSocketClient] Exceeded maximum retry attempts (${this.maxRetries}). Connection failed.`);
      this.setStatus('disconnected');
    }
  }

  /**
   * Gửi dữ liệu chuỗi JSON thô lên Server
   */
  public sendRaw(rawMessage: string): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(rawMessage);
    } else {
      console.warn('⚠️ [WebSocketClient] Cannot send message: Socket is not OPEN (Status:', this.status, ')');
    }
  }

  /**
   * Ngắt kết nối chủ động
   */
  public disconnect(): void {
    this.isIntentionallyClosed = true;
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.setStatus('disconnected');
  }

  /**
   * Đăng ký Event Listeners (onOpen, onMessage, onClose, onError)
   */
  public onOpen(listener: OnOpenListener): () => void {
    this.openListeners.add(listener);
    return () => this.openListeners.delete(listener);
  }

  public onMessage(listener: OnMessageListener): () => void {
    this.messageListeners.add(listener);
    return () => this.messageListeners.delete(listener);
  }

  public onClose(listener: OnCloseListener): () => void {
    this.closeListeners.add(listener);
    return () => this.closeListeners.delete(listener);
  }

  public onError(listener: OnErrorListener): () => void {
    this.errorListeners.add(listener);
    return () => this.errorListeners.delete(listener);
  }

  private setStatus(newStatus: ConnectionStatus): void {
    this.status = newStatus;
  }

  public getStatus(): ConnectionStatus {
    return this.status;
  }

  public getReconnectAttempts(): number {
    return this.reconnectAttempts;
  }

  public getMaxRetries(): number {
    return this.maxRetries;
  }
}

export const globalWebSocketClient = new WebSocketClient();
