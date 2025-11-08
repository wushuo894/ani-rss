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

/**
 * 链接配置
 */
let linkConfig = reactive({
  type: 'primary',
  underline: 'never'
})

/**
 * 夜间模式
 */
useDark({
  onChanged: dark => {
    // 自动根据夜间模式修改沉浸式状态栏
    const meta = document.getElementById('themeColorMeta');
    meta.content = dark ? '#000000' : '#ffffff';
  }
})

// 修改强调色
colorChange(color.value)

let app = document.querySelector('#app');

// 设置最大布局宽度
maxContentWidth.value = Math.max(maxContentWidth.value, 1200)
app
    .style.maxWidth = `${maxContentWidth.value}px`

// 是否非移动设备
isNotMobile.value = app.offsetWidth > 800

</script>
