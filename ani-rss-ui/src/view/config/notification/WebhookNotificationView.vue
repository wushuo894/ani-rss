<template>
  <template v-if="notificationConfig['notificationType'] === 'WEB_HOOK'">
    <SettingsItem label="Method">
      <el-select v-model:model-value="notificationConfig['webHookMethod']">
        <el-option v-for="item in ['POST','GET','PUT','DELETE']"
                   :key="item"
                   :label="item"
                   :value="item"/>
      </el-select>
    </SettingsItem>
    <SettingsItem label="URL">
      <el-input v-model:model-value="notificationConfig['webHookUrl']" type="textarea"
                autosize
                placeholder="https://www.xxx.com?text=test_${notification}"/>
    </SettingsItem>
    <SettingsItem label="Header">
      <el-input
          type="textarea"
          :autosize="{ minRows: 2}"
          v-model="notificationConfig['webHookHeader']"
          :placeholder="'Authorization: Bearer xxx\nCookie: 123456'"/>
    </SettingsItem>
    <SettingsItem label="Body">
      <el-input v-model="notificationConfig['webHookBody']"
                type="textarea"
                :autosize="{ minRows: 2}"
                placeholder='{"text":"test_${notification}"}'/>
    </SettingsItem>
    <div class="notification-flex-end">
      <el-link type="primary" href="https://docs.wushuo.top/config/notification" target="_blank">通知模版示例</el-link>
    </div>
  </template>
</template>

<script setup>
import SettingsItem from "@/view/custom/SettingsItem.vue";

let props = defineProps(['notificationConfig', 'config'])
</script>
