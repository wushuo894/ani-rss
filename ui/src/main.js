import {createApp} from 'vue'
import Login from './Login.vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import {zhCn} from "element-plus/es/locale/index";
import 'element-plus/dist/index.css'
import './style.css'

const app = createApp(Login)
app.provide('$ELEMENT', {
    locale: zhCn,
})
// 引入图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
app.mount('#app')
