import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  define: {
    global: "window",
  },
  plugins: [react()],
  server: {
    host: true,         // allow access from Docker / network
    port: 3000,         // match the exposed port
    strictPort: true,   // don’t fall back to a random port
    watch: {
      usePolling: true, 
    },
  },
})
