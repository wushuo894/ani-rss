<template>
  <template v-if="props.notificationConfig['notificationType'] === 'SERVER_CHAN'">
    <el-form-item label="Type">
      <el-select v-model="props.notificationConfig.serverChanType">
        <el-option
            v-for="it in ['serverChan', 'serverChan3']"
            :key="it"
            :label="it"
            :value="it"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="sendKey" v-if="props.notificationConfig.serverChanType === 'serverChan'">
      <el-input v-model="props.notificationConfig.serverChanSendKey" placeholder="1234567890"/>
    </el-form-item>
    <el-form-item label="apiUrl" v-if="props.notificationConfig.serverChanType === 'serverChan3'">
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
