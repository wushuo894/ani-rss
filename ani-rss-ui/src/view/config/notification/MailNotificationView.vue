<template>
  <template v-if="notificationConfig['notificationType'] === 'MAIL'">
    <SettingsItem label="通知模版">
      <el-input v-model:model-value="notificationConfig['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${notification}" type="textarea"/>
    </SettingsItem>
    <SettingsItem label="SMTP地址">
      <el-input v-model:model-value="notificationConfig['mailSMTPHost']" placeholder="smtp.qq.com"/>
    </SettingsItem>
    <SettingsItem label="SMTP端口">
      <el-input-number
          v-model="notificationConfig['mailSMTPPort']"
          class="notification-input-width"
          :min="1" :max="65535"/>
    </SettingsItem>
    <SettingsItem label="发件人邮箱">
      <el-input v-model:model-value="notificationConfig['mailFrom']"
                placeholder="xx@qq.com"/>
    </SettingsItem>
    <SettingsItem label="密码">
      <el-input v-model:model-value="notificationConfig['mailPassword']" show-password/>
    </SettingsItem>
    <SettingsItem label="SSL">
      <el-switch v-model:model-value="notificationConfig['mailSSLEnable']"/>
    </SettingsItem>
    <SettingsItem label="STARTTLS">
      <el-switch v-model:model-value="notificationConfig['mailTLSEnable']"/>
    </SettingsItem>
    <SettingsItem label="收件人邮箱">
      <el-input v-model:model-value="notificationConfig['mailAddressee']"
                placeholder="xx@xx.com"></el-input>
    </SettingsItem>
    <SettingsItem label="图片">
      <el-switch v-model:model-value="notificationConfig['mailImage']"/>
    </SettingsItem>
  </template>
</template>

<script setup>
import SettingsItem from "@/view/custom/SettingsItem.vue";

let props = defineProps(['notificationConfig', 'config'])
</script>
