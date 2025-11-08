<template>
  <el-config-provider :locale="zhCn" :link="linkConfig">
    <Login v-if="!authorization"/>
    <App v-else/>
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import Login from "@/Login.vue";
import App from "@/home/App.vue";
import {useDark} from "@vueuse/core";
import {authorization, color, colorChange, isNotMobile, maxContentWidth} from "@/js/global.js";
import {reactive} from "vue";

let linkConfig = reactive({
  type: 'primary',
  underline: 'never'
})

useDark({
  onChanged: dark => {
    const meta = document.getElementById('themeColorMeta');
    meta.content = dark ? '#000000' : '#ffffff';
  }
})

colorChange(color.value)

let app = document.querySelector('#app');

maxContentWidth.value = Math.max(maxContentWidth.value, 1200)
app
    .style.maxWidth = `${maxContentWidth.value}px`

isNotMobile.value = app.offsetWidth > 800

</script>
