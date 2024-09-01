import {createApp} from 'vue'
import 'element-plus/dist/index.css'
import './style.css'
import Login from './Login.vue'
import ElementPlus from 'element-plus'


const app = createApp(Login)
app.use(ElementPlus)
app.mount('#app')
