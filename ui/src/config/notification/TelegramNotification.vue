<template>
  <template v-if="notificationConfig['notificationType'] === 'TELEGRAM'">
    <el-form-item label="Api Host">
      <el-input v-model:model-value="props.notificationConfig['telegramApiHost']"
                placeholder="https://api.telegram.org"/>
    </el-form-item>
    <el-form-item label="Token">
      <el-input v-model:model-value="props.notificationConfig['telegramBotToken']"
                placeholder="123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"/>
    </el-form-item>
    <el-form-item label="ChatId">
      <div>
        <div style="justify-content: space-between;width: 100%;" class="auto">
          <div style="margin-top: 4px;margin-right: 4px;">
            <el-input v-model:model-value="props.notificationConfig['telegramChatId']"
                      placeholder="123456789"/>
          </div>
          <div class="flex" style="margin-top: 4px;align-items: center;">
            <div>
              <el-select v-model:model-value="chatId" @change="chatIdChange" style="width: 160px">
                <el-option v-for="item in Object.keys(chatIdMap)"
                           :key="item"
                           :label="item"
                           :value="item"/>
              </el-select>
            </div>
            <div style="margin-left: 4px;">
              <el-button icon="Refresh" bg text @click="getUpdates" :loading="getUpdatesLoading"/>
            </div>
          </div>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="话题ID">
      <el-input-number v-model="props.notificationConfig['telegramTopicId']"
                       :min="-1" style="width: 160px;"/>
    </el-form-item>
    <el-form-item label="图片">
      <el-switch v-model:model-value="props.notificationConfig['telegramImage']"/>
    </el-form-item>
    <el-form-item label="格式">
      <div style="width: 160px;">
        <el-select v-model:model-value="props.notificationConfig['telegramFormat']" placeholder="None">
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
import api from "@/js/api.js";
import {ref} from "vue";

let chatIdMap = ref({})
let chatId = ref('')
let getUpdatesLoading = ref(false)

let getUpdates = () => {
  if (!props.notificationConfig.telegramBotToken.length) {
    ElMessage.error('Token 不能为空')
    return
  }

  getUpdatesLoading.value = true
  api.post("api/telegram?method=getUpdates", props.notificationConfig)
      .then(res => {
        chatIdMap.value = res.data
        if (Object.keys(chatIdMap.value).length) {
          chatId.value = Object.keys(chatIdMap.value)[0]
          chatIdChange(chatId.value)
        }
      })
      .finally(() => {
        getUpdatesLoading.value = false
      })
}

let chatIdChange = (k) => {
  props.notificationConfig.telegramChatId = chatIdMap.value[k]
}

let props = defineProps(['notificationConfig', 'config'])

</script>
