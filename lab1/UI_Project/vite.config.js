import { defineConfig } from 'vite';
import fs from 'fs';

export default defineConfig({
  server: {
    port: 5173,
    https: {
      key: fs.readFileSync('key.pem'),
      cert: fs.readFileSync('cert.pem')
    },
    proxy: {
      '/api': {
        target: 'https://localhost:8082',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist'
  }
});