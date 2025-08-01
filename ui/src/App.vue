<template>
  <el-config-provider :locale="locale">
    <Login/>
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn.mjs'
import Login from "@/Login.vue";

const locale = zhCn
</script>
