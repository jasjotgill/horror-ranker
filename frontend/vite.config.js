import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Listen on the local network too, so a phone on the same Wi-Fi can open the dev server.
    host: true,
    // The browser only ever talks to the dev server; it forwards /api to Spring Boot.
    // Same origin from the browser's point of view, so no CORS setup is needed.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
