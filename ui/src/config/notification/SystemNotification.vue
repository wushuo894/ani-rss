<template>
  <template v-if="props.notificationConfig['notificationType'] === 'SYSTEM'">
    <el-form-item label="通知模版">
      <el-input v-model:model-value="props.notificationConfig['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${notification}" type="textarea"/>
    </el-form-item>
  </template>
</template>

<script setup>
let props = defineProps(['notificationConfig', 'config'])
</script>
