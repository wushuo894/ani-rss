import {createApp} from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import AppView from './AppView.vue'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

const app = createApp(AppView)
// 引入图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
app.mount('#app')
