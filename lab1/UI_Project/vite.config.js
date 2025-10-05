import { defineConfig } from 'vite';

export default defineConfig({
  server: {
    port: 5173,
  },
  build: {
    outDir: 'dist',
  },
  proxy: {
    '/api': {
      target: 'https://localhost:8082',
      changeOrigin: true,
      secure: false
    },
  },
});
