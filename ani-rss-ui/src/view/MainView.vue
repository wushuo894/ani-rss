<template>
  <el-config-provider
      :locale="zhCn"
      :link="linkConfig"
      :dialog="dialogConfig">
    <LoginView v-if="!authorization"/>
    <AppView v-else/>
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import LoginView from "@/view/LoginView.vue";
import AppView from "@/view/home/AppView.vue";
import {authorization, init} from "@/js/global.js";
import {reactive} from "vue";

/**
 * 链接配置
 */
let linkConfig = reactive({
  type: 'primary',
  underline: 'never'
})

let dialogConfig = reactive({
  alignCenter: true
})

init()

</script>
