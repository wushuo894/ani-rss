import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  server:{
    proxy:{
      '/api':{
        target: 'http://127.0.0.1:7789',
        changeOrigin: true,
      }
    }
  },
  plugins: [vue()],
})
