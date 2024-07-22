import {createApp} from 'vue'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import ElementPlus from 'element-plus'


const app = createApp(App)
app.use(ElementPlus)
app.mount('#app')
