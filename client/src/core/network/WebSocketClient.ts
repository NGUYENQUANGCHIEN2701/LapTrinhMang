import { DEFAULT_SERVER_URL } from '../../shared/constants/game';
import { IncomingPacket, OutgoingPacket } from './types';

type MessageHandler = (packet: IncomingPacket) => void;

export class WebSocketClient {
  private socket: WebSocket | null = null;
  private url: string;
  private messageListeners: Set<MessageHandler> = new Set();
  private isConnected: boolean = false;

  constructor(url: string = DEFAULT_SERVER_URL) {
    this.url = url;
  }

  public connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.socket = new WebSocket(this.url);

        this.socket.onopen = () => {
          this.isConnected = true;
          console.log('[WebSocketClient] Connected to server:', this.url);
          resolve();
        };

        this.socket.onmessage = (event) => {
          try {
            const data: IncomingPacket = JSON.parse(event.data);
            this.notifyListeners(data);
          } catch (err) {
            console.error('[WebSocketClient] Failed to parse packet:', err);
          }
        };

        this.socket.onerror = (error) => {
          console.error('[WebSocketClient] Socket error:', error);
          reject(error);
        };

        this.socket.onclose = () => {
          this.isConnected = false;
          console.log('[WebSocketClient] Connection closed');
        };
      } catch (err) {
        reject(err);
      }
    });
  }

  public send(packet: OutgoingPacket): void {
    if (this.socket && this.isConnected && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(packet));
    } else {
      console.warn('[WebSocketClient] Cannot send packet: socket not connected');
    }
  }

  public addMessageListener(listener: MessageHandler): void {
    this.messageListeners.add(listener);
  }

  public removeMessageListener(listener: MessageHandler): void {
    this.messageListeners.delete(listener);
  }

  private notifyListeners(packet: IncomingPacket): void {
    this.messageListeners.forEach((listener) => listener(packet));
  }

  public disconnect(): void {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
      this.isConnected = false;
    }
  }

  public getIsConnected(): boolean {
    return this.isConnected;
  }
}

export const globalWebSocketClient = new WebSocketClient();
