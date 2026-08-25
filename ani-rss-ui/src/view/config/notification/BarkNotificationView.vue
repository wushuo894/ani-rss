<template>
  <template v-if="notificationConfig['notificationType'] === 'BARK'">
    <SettingsItem label="通知模版">
      <el-input v-model:model-value="notificationConfig['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${notification}" type="textarea"/>
    </SettingsItem>
    <SettingsItem label="ServerUrl">
      <el-input v-model="notificationConfig['barkServerUrl']" placeholder="https://api.day.app"/>
    </SettingsItem>
    <SettingsItem label="DeviceKeys">
      <el-input-tag v-model="notificationConfig['barkDeviceKeys']"/>
    </SettingsItem>
    <SettingsItem label="Group">
      <el-input v-model="notificationConfig['barkGroup']"/>
    </SettingsItem>
    <SettingsItem label="Level">
      <el-select v-model="notificationConfig['barkLevel']" placeholder="Select Level">
        <el-option label="Critical" value="critical"/>
        <el-option label="Active" value="active"/>
        <el-option label="TimeSensitive" value="timeSensitive"/>
        <el-option label="Passive" value="passive"/>
      </el-select>
    </SettingsItem>
    <SettingsItem label="Volume">
      <el-input-number v-model="notificationConfig['barkVolume']" :min="1" :max="10"/>
    </SettingsItem>
    <SettingsItem label="Other">
      <el-checkbox v-model="notificationConfig['barkUseMarkdown']">
        Use Markdown
      </el-checkbox>
    </SettingsItem>
  </template>
</template>

<script setup>
import SettingsItem from "@/view/custom/SettingsItem.vue";

let props = defineProps(['notificationConfig', 'config'])
</script>
