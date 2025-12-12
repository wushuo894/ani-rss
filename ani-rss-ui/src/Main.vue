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
import {authorization, init} from "@/js/global.js";
import {reactive} from "vue";

/**
 * 链接配置
 */
let linkConfig = reactive({
  type: 'primary',
  underline: 'never'
})

init()

</script>
