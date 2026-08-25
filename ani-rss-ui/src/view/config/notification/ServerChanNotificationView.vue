<template>
  <template v-if="notificationConfig['notificationType'] === 'SERVER_CHAN'">
    <SettingsItem label="通知模版">
      <el-input v-model:model-value="notificationConfig['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${notification}" type="textarea"/>
    </SettingsItem>
    <SettingsItem label="Type">
      <el-select v-model="notificationConfig.serverChanType">
        <el-option
            v-for="it in [{
              label: 'ServerChan',
              name: 'SERVER_CHAN'
            }, {
              label: 'ServerChan3',
              name: 'SERVER_CHAN_3'
            }]"
            :key="it.name"
            :label="it.label"
            :value="it.name"
        />
      </el-select>
    </SettingsItem>
    <SettingsItem label="sendKey" v-if="notificationConfig.serverChanType === 'SERVER_CHAN'">
      <el-input v-model="notificationConfig.serverChanSendKey" placeholder="1234567890"/>
    </SettingsItem>
    <SettingsItem label="apiUrl" v-if="notificationConfig.serverChanType === 'SERVER_CHAN_3'">
      <el-input v-model="notificationConfig.serverChan3ApiUrl"
                placeholder="https://<uid>.push.ft07.com/send/<sendKey>.send"/>
    </SettingsItem>
    <SettingsItem label="事件标题">
      <el-switch v-model="notificationConfig['serverChanTitleAction']"/>
    </SettingsItem>
  </template>
</template>

<script setup>
import SettingsItem from "@/view/custom/SettingsItem.vue";

let props = defineProps(['notificationConfig', 'config'])
</script>
