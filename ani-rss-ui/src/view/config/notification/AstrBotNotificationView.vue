<template>
  <template v-if="notificationConfig['notificationType'] === 'ASTRBOT'">
    <!-- 复用现有模板变量，根据图片开关附带本地缓存的番剧封面。 -->
    <SettingsItem label="通知模版">
      <el-input v-model="notificationConfig['notificationTemplate']" :autosize="{ minRows: 2 }"
                placeholder="${notification}" type="textarea"/>
    </SettingsItem>
    <SettingsItem label="服务地址">
      <el-input v-model.trim="notificationConfig['astrBotHost']" placeholder="http://astrbot:6185"/>
    </SettingsItem>
    <SettingsItem label="API Key">
      <el-input v-model.trim="notificationConfig['astrBotApiKey']" placeholder="abk_xxx"
                type="password" show-password/>
    </SettingsItem>
    <SettingsItem label="目标 UMO">
      <el-input v-model.trim="notificationConfig['astrBotUmo']" placeholder="在目标会话发送 /sid 获取"/>
    </SettingsItem>
    <SettingsItem label="发送图片">
      <el-switch v-model="notificationConfig['astrBotImage']"/>
    </SettingsItem>
  </template>
</template>

<script setup>
import SettingsItem from "@/view/custom/SettingsItem.vue";

// 通知配置由父级统一保存，切换类型时保留已填写的参数。
defineProps(['notificationConfig'])
</script>
