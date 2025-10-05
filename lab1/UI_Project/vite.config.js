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
      target: 'http://localhost:8081',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, ''),
    },
  },
});
