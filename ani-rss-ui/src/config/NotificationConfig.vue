<template>
  <el-dialog v-model="dialogVisible" center title="修改通知">
    <el-scrollbar :always="true" class="notification-config-scrollbar">
      <el-form label-width="auto">
        <el-form-item label="通知类型">
          <el-select v-model="notificationConfig['notificationType']">
            <el-option v-for="it in notificationTypeList"
                       :key="it.name"
                       :disabled="it.name === 'EMBY_REFRESH' && !props.config['verifyExpirationTime']"
                       :label="it.label"
                       :value="it.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="notificationConfig['comment']" placeholder="无备注"/>
        </el-form-item>
        <el-form-item label="通知状态">
          <el-checkbox-group v-model:model-value="notificationConfig['statusList']">
            <el-checkbox label="开始下载" value="DOWNLOAD_START"/>
            <el-checkbox label="下载完成" value="DOWNLOAD_END"/>
            <el-checkbox label="缺集" value="OMIT"/>
            <el-checkbox label="错误" value="ERROR"/>
            <el-checkbox label="OpenList 上传通知" value="OPEN_LIST_UPLOAD"/>
            <el-checkbox label="订阅完结" value="COMPLETED"/>
            <el-checkbox label="摸鱼检测" value="PROCRASTINATING"/>
          </el-checkbox-group>
        </el-form-item>
        <EmbyRefreshNotification v-model:notification-config="notificationConfig" v-model:config="props.config"/>
        <MailNotification v-model:notification-config="notificationConfig" v-model:config="props.config"/>
        <ServerChanNotification v-model:notification-config="notificationConfig" v-model:config="props.config"/>
        <TelegramNotification v-model:notification-config="notificationConfig" v-model:config="props.config"/>
        <WebhookNotification v-model:notification-config="notificationConfig" v-model:config="props.config"/>
        <SystemNotification v-model:notification-config="notificationConfig" v-model:config="props.config"/>
        <ShellNotification v-model:notification-config="notificationConfig" v-model:config="props.config"/>
        <el-form-item label="重试">
          <el-input-number
              :min="0"
              :max="100"
              v-model="notificationConfig['retry']"
              class="notification-config-retry-input"
          >
            <template #suffix>
              <span>次</span>
            </template>
          </el-input-number>
        </el-form-item>
        <el-form-item label="开启">
          <el-switch v-model="notificationConfig['enable']"/>
        </el-form-item>
      </el-form>
    </el-scrollbar>
    <div class="flex notification-config-footer">
      <el-button bg text @click="messageTest" icon="Odometer" :loading="messageTestLoading">测试
      </el-button>
      <el-button @click="dialogVisible = false" text bg icon="Check" type="primary">确定
      </el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import EmbyRefreshNotification from "./notification/EmbyRefreshNotification.vue";
import MailNotification from "./notification/MailNotification.vue";
import ServerChanNotification from "./notification/ServerChanNotification.vue";
import TelegramNotification from "./notification/TelegramNotification.vue";
import WebhookNotification from "./notification/WebhookNotification.vue";
import ShellNotification from "@/config/notification/ShellNotification.vue";
import SystemNotification from "@/config/notification/SystemNotification.vue";
import {notificationTypeList} from "@/js/notification-type.js";
import {ElMessage} from "element-plus";
import api from "@/js/api.js";


let notificationConfig = ref({
  "comment": "无备注",
  "notificationTemplate": "${notification}",
  "notificationType": "TELEGRAM",
  "mailSMTPHost": "smtp.qq.com",
  "mailSMTPPort": 465,
  "mailFrom": "",
  "mailPassword": "",
  "mailSSLEnable": true,
  "mailTLSEnable": false,
  "mailAddressee": "",
  "mailImage": true,
  "serverChanType": "SERVER_CHAN",
  "serverChanSendKey": "",
  "serverChan3ApiUrl": "",
  "serverChanTitleAction": true,
  "telegramBotToken": "",
  "telegramChatId": "",
  "telegramTopicId": -1,
  "telegramApiHost": "https://api.telegram.org",
  "telegramImage": true,
  "telegramFormat": "",
  "webHookMethod": "POST",
  "webHookUrl": "",
  "webHookBody": "",
  "embyRefresh": false,
  "embyApiKey": "",
  "embyRefreshViewIds": [],
  "embyDelayed": 0,
  "statusList": [
    "DOWNLOAD_START",
    "OMIT",
    "ERROR"
  ]
})

const messageTestLoading = ref(false)

const messageTest = () => {
  messageTestLoading.value = true

  let config = JSON.parse(JSON.stringify(notificationConfig.value))
  config.embyDelayed = 0

  api.post("api/notification?type=test", config)
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        messageTestLoading.value = false
      })
}

let dialogVisible = ref(false)

let props = defineProps(['config'])

let show = (newNotificationConfig) => {
  notificationConfig.value = newNotificationConfig
  dialogVisible.value = true
}

defineExpose({
  show
})

</script>

<style scoped>
.notification-config-scrollbar {
  height: 530px;
  padding: 15px;
}

.notification-config-retry-input {
  width: 160px;
}

.notification-config-footer {
  justify-content: space-between;
  width: 100%;
  margin-top: 8px;
}
</style>
