<template>
  <template v-if="props.notificationConfig['notificationType'] === 'SERVER_CHAN'">
    <el-form-item label="通知模版">
      <el-input v-model:model-value="props.notificationConfig['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${notification}" type="textarea"/>
    </el-form-item>
    <el-form-item label="Type">
      <el-select v-model="props.notificationConfig.serverChanType">
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
    </el-form-item>
    <el-form-item label="sendKey" v-if="props.notificationConfig.serverChanType === 'SERVER_CHAN'">
      <el-input v-model="props.notificationConfig.serverChanSendKey" placeholder="1234567890"/>
    </el-form-item>
    <el-form-item label="apiUrl" v-if="props.notificationConfig.serverChanType === 'SERVER_CHAN_3'">
      <el-input v-model="props.notificationConfig.serverChan3ApiUrl"
                placeholder="https://<uid>.push.ft07.com/send/<sendKey>.send"/>
    </el-form-item>
    <el-form-item label="事件标题">
      <el-switch v-model="props.notificationConfig['serverChanTitleAction']"/>
    </el-form-item>
  </template>
</template>

<script setup>
let props = defineProps(['notificationConfig', 'config'])
</script>
