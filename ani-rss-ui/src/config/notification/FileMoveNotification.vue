<template>
  <template v-if="notificationConfig['notificationType'] === 'FILE_MOVE'">
    <el-form-item label="目标位置">
      <el-input
          v-model="notificationConfig['fileMoveTarget']"
          placeholder="/CD2/115/Media/番剧/${title}/Season ${season}"/>
    </el-form-item>
    <el-form-item label="目标位置 (剧场版)">
      <el-input
          v-model="notificationConfig['fileMoveOvaTarget']"
          placeholder="/CD2/115/Media/剧场版/${title}"/>
    </el-form-item>
  </template>
</template>

<script setup>
let props = defineProps(['notificationConfig', 'config'])
</script>
