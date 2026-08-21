export class WebSocketClient {
  constructor(url) {
    this.url = url;
  }

  connect() {
    console.log('WebSocket placeholder: connect to', this.url);
  }
}
