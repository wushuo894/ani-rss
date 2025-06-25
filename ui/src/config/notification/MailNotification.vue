<template>
  <template v-if="props.notificationConfig['notificationType'] === 'MAIL'">
    <el-form-item label="通知模版">
      <el-input v-model:model-value="props.notificationConfig['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${notification}" type="textarea"/>
    </el-form-item>
    <el-form-item label="SMTP地址">
      <el-input v-model:model-value="props.notificationConfig['mailSMTPHost']" placeholder="smtp.qq.com"/>
    </el-form-item>
    <el-form-item label="SMTP端口">
      <el-input-number v-model:model-value="props.notificationConfig['mailSMTPPort']" :min="1" :max="65535"/>
    </el-form-item>
    <el-form-item label="发件人邮箱">
      <el-input v-model:model-value="props.notificationConfig['mailFrom']"
                placeholder="xx@qq.com"/>
    </el-form-item>
    <el-form-item label="密码">
      <el-input v-model:model-value="props.notificationConfig['mailPassword']" show-password/>
    </el-form-item>
    <el-form-item label="SSL">
      <el-switch v-model:model-value="props.notificationConfig['mailSSLEnable']"/>
    </el-form-item>
    <el-form-item label="STARTTLS">
      <el-switch v-model:model-value="props.notificationConfig['mailTLSEnable']"/>
    </el-form-item>
    <el-form-item label="收件人邮箱">
      <el-input v-model:model-value="props.notificationConfig['mailAddressee']"
                placeholder="xx@xx.com"></el-input>
    </el-form-item>
    <el-form-item label="图片">
      <el-switch v-model:model-value="props.notificationConfig['mailImage']"/>
    </el-form-item>
  </template>
</template>

<script setup>
let props = defineProps(['notificationConfig', 'config'])
</script>