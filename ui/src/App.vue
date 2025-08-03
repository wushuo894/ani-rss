<template>
  <el-config-provider :locale="zhCn">
    <Login/>
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import Login from "@/Login.vue";
</script>
