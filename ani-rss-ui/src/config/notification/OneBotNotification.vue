<template>
  <template v-if="notificationConfig['notificationType'] === 'ONEBOT'">
    <el-form-item label="通知模版">
      <el-input v-model="notificationConfig['notificationTemplate']" :autosize="{ minRows: 2 }"
                placeholder="${notification}" type="textarea"/>
    </el-form-item>
    <el-form-item label="OneBot 地址">
      <el-input v-model="notificationConfig['oneBotUrl']"
                placeholder="http://127.0.0.1:5700 或 ws://127.0.0.1:3001"/>
    </el-form-item>
    <el-form-item label="Access Token">
      <el-input v-model="notificationConfig['oneBotAccessToken']"
                placeholder="可选，留空则不验证"/>
    </el-form-item>
    <el-form-item label="消息类型">
      <div class="notification-input-width">
        <el-select v-model="notificationConfig['oneBotMessageType']">
          <el-option label="私聊" value="PRIVATE"/>
          <el-option label="群聊" value="GROUP"/>
        </el-select>
      </div>
    </el-form-item>
    <el-form-item v-if="notificationConfig['oneBotMessageType'] === 'PRIVATE'" label="用户ID">
      <el-input v-model="notificationConfig['oneBotUserId']"
                placeholder="QQ号"/>
    </el-form-item>
    <el-form-item v-if="notificationConfig['oneBotMessageType'] === 'GROUP'" label="群号">
      <el-input v-model="notificationConfig['oneBotGroupId']"
                placeholder="群号"/>
    </el-form-item>
    <el-form-item label="图片">
      <el-switch v-model="notificationConfig['oneBotImage']"/>
    </el-form-item>
  </template>
</template>

<script setup>
let props = defineProps(['notificationConfig', 'config'])
</script>
