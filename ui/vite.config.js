import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  server:{
    proxy:{
      '/api':{
        target: 'http://192.168.5.4:7789',
        changeOrigin: true,
      }
    }
  },
  plugins: [vue()],
})
