<template>
  <template v-if="notificationConfig['notificationType'] === 'MAIL'">
    <el-form-item label="通知模版">
      <el-input v-model:model-value="notificationConfig['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${notification}" type="textarea"/>
    </el-form-item>
    <el-form-item label="SMTP地址">
      <el-input v-model:model-value="notificationConfig['mailSMTPHost']" placeholder="smtp.qq.com"/>
    </el-form-item>
    <el-form-item label="SMTP端口">
      <el-input-number
          v-model="notificationConfig['mailSMTPPort']"
          class="notification-input-width"
          :min="1" :max="65535"/>
    </el-form-item>
    <el-form-item label="发件人邮箱">
      <el-input v-model:model-value="notificationConfig['mailFrom']"
                placeholder="xx@qq.com"/>
    </el-form-item>
    <el-form-item label="密码">
      <el-input v-model:model-value="notificationConfig['mailPassword']" show-password/>
    </el-form-item>
    <el-form-item label="SSL">
      <el-switch v-model:model-value="notificationConfig['mailSSLEnable']"/>
    </el-form-item>
    <el-form-item label="STARTTLS">
      <el-switch v-model:model-value="notificationConfig['mailTLSEnable']"/>
    </el-form-item>
    <el-form-item label="收件人邮箱">
      <el-input v-model:model-value="notificationConfig['mailAddressee']"
                placeholder="xx@xx.com"></el-input>
    </el-form-item>
    <el-form-item label="图片">
      <el-switch v-model:model-value="notificationConfig['mailImage']"/>
    </el-form-item>
  </template>
</template>

<script setup>
let props = defineProps(['notificationConfig', 'config'])
</script>
