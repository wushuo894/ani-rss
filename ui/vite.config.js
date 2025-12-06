import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'

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
    plugins: [
        vue(),
        AutoImport({
            imports: ['vue'],
            resolvers: [ElementPlusResolver()]
        }),
        Components({
            resolvers: [ElementPlusResolver({
                importStyle: 'css',
            })]
        })
    ],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src/')
        }
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks: {
                    'vue': ['vue', '@vueuse/core', '@vicons/fa'],
                    'utils': ['crypto-js', 'markdown-it', 'markdown-it-github-alerts'],
                    'element-icon': ['@element-plus/icons-vue'],
                    'artplayer': ['artplayer', 'artplayer-plugin-multiple-subtitles'],
                    'shiki': ['shiki'],
                    'element-plus': ['element-plus']
                },
                chunkFileNames: () => {
                    return `assets/[name]-[hash].js`;
                }
            }
        },
        minify: 'terser',
        terserOptions: {
            compress: {
                drop_console: true,
                drop_debugger: true,
            }
        }
    }
})
