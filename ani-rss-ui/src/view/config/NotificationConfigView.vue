<template>
  <el-dialog v-model="dialogVisible" center title="修改通知">
    <el-scrollbar :always="true" class="notification-config-scrollbar">
      <SettingsItem label="通知类型">
        <el-select v-model="notificationConfig['notificationType']">
          <el-option v-for="it in notificationTypeList"
                     :key="it.name"
                     :label="it.label"
                     :value="it.name"
          />
        </el-select>
      </SettingsItem>
      <SettingsItem label="备注">
        <el-input v-model="notificationConfig['comment']" placeholder="无备注"/>
      </SettingsItem>
      <SettingsItem label="通知状态">
        <el-checkbox-group v-model:model-value="notificationConfig['statusList']">
          <el-checkbox label="开始下载" value="DOWNLOAD_START"/>
          <el-checkbox label="下载完成" value="DOWNLOAD_END"/>
          <el-checkbox label="缺集" value="OMIT"/>
          <el-checkbox label="错误" value="ERROR"/>
          <el-checkbox label="订阅完结" value="COMPLETED"/>
          <el-checkbox label="摸鱼检测" value="PROCRASTINATING"/>
        </el-checkbox-group>
      </SettingsItem>
      <EmbyRefreshNotificationView v-model:notification-config="notificationConfig" v-model:config="props.config"/>
      <MailNotificationView v-model:notification-config="notificationConfig" v-model:config="props.config"/>
      <ServerChanNotificationView v-model:notification-config="notificationConfig" v-model:config="props.config"/>
      <BarkNotificationView v-model:config="props.config" v-model:notification-config="notificationConfig"/>
      <TelegramNotificationView v-model:notification-config="notificationConfig" v-model:config="props.config"/>
      <WebhookNotificationView v-model:notification-config="notificationConfig" v-model:config="props.config"/>
      <SystemNotificationView v-model:notification-config="notificationConfig" v-model:config="props.config"/>
      <ShellNotificationView v-model:notification-config="notificationConfig" v-model:config="props.config"/>
      <FileMoveNotificationView v-model:notification-config="notificationConfig" v-model:config="props.config"/>
      <OpenListUploadNotificationView v-model:config="props.config" v-model:notification-config="notificationConfig"/>
      <SettingsItem label="顺序">
        <div>
          <el-input-number
              v-model="notificationConfig['sort']"
              class="notification-input-width"
          />
        </div>
      </SettingsItem>
      <SettingsItem label="重试">
        <el-input-number
            :min="0"
            :max="100"
            v-model="notificationConfig['retry']"
            class="notification-input-width"
        />
      </SettingsItem>
      <SettingsItem label="开启">
        <el-switch v-model="notificationConfig['enable']"/>
      </SettingsItem>
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
import SettingsItem from "@/view/custom/SettingsItem.vue";
import {ref} from "vue";
import EmbyRefreshNotificationView from "./notification/EmbyRefreshNotificationView.vue";
import MailNotificationView from "./notification/MailNotificationView.vue";
import ServerChanNotificationView from "./notification/ServerChanNotificationView.vue";
import TelegramNotificationView from "./notification/TelegramNotificationView.vue";
import WebhookNotificationView from "./notification/WebhookNotificationView.vue";
import ShellNotificationView from "./notification/ShellNotificationView.vue";
import SystemNotificationView from "./notification/SystemNotificationView.vue";
import {notificationTypeList} from "@/js/notification-type.js";
import {ElMessage} from "element-plus";
import FileMoveNotificationView from "./notification/FileMoveNotificationView.vue";
import OpenListUploadNotificationView from "./notification/OpenListUploadNotificationView.vue";
import {testNotification} from "@/js/http.js";
import BarkNotificationView from "./notification/BarkNotificationView.vue";


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

  testNotification(config)
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

.notification-config-footer {
  justify-content: space-between;
  width: 100%;
  margin-top: 8px;
}
</style>
