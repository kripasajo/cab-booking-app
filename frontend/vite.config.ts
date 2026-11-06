import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the Vite server forwards API and WebSocket calls to the services
// running on your machine (same routing that nginx does in Docker).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/auth': 'http://localhost:8081',
      '/api/users': 'http://localhost:8081',
      '/api/bookings': 'http://localhost:8082',
      '/api/drivers': 'http://localhost:8083',
      '/api/pricing': 'http://localhost:8084',
      '/ws': { target: 'ws://localhost:8085', ws: true },
    },
  },
});
