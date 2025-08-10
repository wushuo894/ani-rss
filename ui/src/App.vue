<template>
  <el-config-provider :locale="zhCn" :link="linkConfig">
    <Login/>
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import Login from "@/Login.vue";

let linkConfig = reactive({
  type: 'primary',
  underline: 'never'
})
</script>
