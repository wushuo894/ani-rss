<template>
  <template v-if="notificationConfig['notificationType'] === 'TELEGRAM'">
    <el-form-item label="通知模版">
      <el-input v-model="notificationConfig['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${notification}" type="textarea"/>
    </el-form-item>
    <el-form-item label="Api Host">
      <el-input v-model="notificationConfig['telegramApiHost']"
                placeholder="https://api.telegram.org"/>
    </el-form-item>
    <el-form-item label="Token">
      <el-input v-model="notificationConfig['telegramBotToken']"
                placeholder="123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"/>
    </el-form-item>
    <el-form-item label="ChatId">
      <div>
        <div class="auto-flex notification-flex-between">
          <div class="notification-margin-top-right">
            <el-input v-model="notificationConfig['telegramChatId']"
                      placeholder="123456789"/>
          </div>
          <div class="flex notification-margin-top-center">
            <div>
              <el-select v-model="chatId"
                         class="notification-input-width"
                         @change="chatIdChange">
                <el-option v-for="chat in chatList"
                           :key="chat.id"
                           :label="`${chat.type}: ${chat.username}`"
                           :value="chat.id"/>
              </el-select>
            </div>
            <div class="notification-margin-left">
              <el-button icon="Refresh" bg text @click="getUpdates" :loading="getUpdatesLoading"/>
            </div>
          </div>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="话题ID">
      <el-input-number v-model="notificationConfig['telegramTopicId']"
                       :min="-1" class="notification-input-width"/>
    </el-form-item>
    <el-form-item label="图片">
      <el-switch v-model="notificationConfig['telegramImage']"/>
    </el-form-item>
    <el-form-item label="格式">
      <div class="notification-input-width">
        <el-select v-model="notificationConfig['telegramFormat']" placeholder="None">
          <el-option label="None" value=""/>
          <el-option label="Markdown" value="Markdown"/>
          <el-option label="HTML" value="HTML"/>
        </el-select>
      </div>
    </el-form-item>
  </template>
</template>

<script setup>
import {ElMessage} from "element-plus";
import {ref} from "vue";
import * as http from "@/js/http.js";

let chatList = ref([])
let chatId = ref('')
let getUpdatesLoading = ref(false)

let getUpdates = () => {
  if (!props.notificationConfig.telegramBotToken.length) {
    ElMessage.error('Token 不能为空')
    return
  }

  getUpdatesLoading.value = true
  http.getTgUpdates(props.notificationConfig)
      .then(res => {
        chatList.value = res.data
        if (chatList.value.length) {
          chatId.value = chatList.value[0].id
          chatIdChange(chatId.value)
        }
      })
      .finally(() => {
        getUpdatesLoading.value = false
      })
}

let chatIdChange = (v) => {
  props.notificationConfig.telegramChatId = v
}

let props = defineProps(['notificationConfig', 'config'])

</script>
