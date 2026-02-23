import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'
import compression from 'vite-plugin-compression'

let serverHost = process.env['SERVER_HOST'];

export default defineConfig({
    base: './',
    server: {
        port: 37789,
        proxy: {
            '/api': {
                target: serverHost ? serverHost : 'http://127.0.0.1:7789',
                changeOrigin: false
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
        }),
        compression({
            // 输出压缩日志
            verbose: true,
            // 是否禁用压缩
            disable: false,
            // 对超过10KB的文件进行压缩
            threshold: 10240,
            // 使用gzip压缩
            algorithm: 'gzip',
            // 压缩后文件的扩展名
            ext: '.gz'
        }),
    ],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src/')
        }
    },
    build: {
        rollupOptions: {
            input: {
                main: path.resolve(__dirname, 'index.html'),
                bgmOauthCallback: path.resolve(__dirname, 'bgm-oauth-callback.html')
            },
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
