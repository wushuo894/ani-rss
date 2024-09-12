import {createApp} from 'vue'
import 'element-plus/dist/index.css'
import './style.css'
import Login from './Login.vue'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/theme-chalk/dark/css-vars.css'


const app = createApp(Login)
app.use(ElementPlus)
// 引入图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
app.mount('#app')
