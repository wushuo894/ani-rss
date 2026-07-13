<template>
  <template v-if="notificationConfig['notificationType'] === 'BARK'">
    <el-form-item label="通知模版">
      <el-input v-model:model-value="notificationConfig['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${notification}" type="textarea"/>
    </el-form-item>
    <el-form-item label="ServerUrl">
      <el-input v-model="notificationConfig['barkServerUrl']" placeholder="https://api.day.app"/>
    </el-form-item>
    <el-form-item label="DeviceKeys">
      <el-input-tag v-model="notificationConfig['barkDeviceKeys']"/>
    </el-form-item>
    <el-form-item label="Group">
      <el-input v-model="notificationConfig['barkGroup']"/>
    </el-form-item>
    <el-form-item label="Other">
      <el-checkbox v-model="notificationConfig['barkUseMarkdown']">
        Use Markdown
      </el-checkbox>
    </el-form-item>
  </template>
</template>

<script setup>
let props = defineProps(['notificationConfig', 'config'])
</script>
