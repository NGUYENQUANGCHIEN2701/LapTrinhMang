import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Phase 4.5: build relative paths để nhúng vào Spring Boot static folder
  // không cần biết context path của server-spring (có thể chạy / hoặc /app).
  base: './',
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/__tests__/**/*.test.ts'],
  },
});
