import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
    base: './',
    server: {
        port: 30000,
        proxy: {
            '/api': {
                target: 'http://127.0.0.1:7789',
                changeOrigin: true,
            }
        }
    },
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src/')
        }
    }
})
