<template>
  <el-collapse v-model:model-value="messageActiveName" accordion>
    <el-collapse-item title="Telegram通知" name="1">
      <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
        <el-form-item label="Api Host">
          <el-input v-model:model-value="props.config.telegramApiHost"
                    :disabled="!config.telegram"
                    placeholder="https://api.telegram.org"/>
        </el-form-item>
        <el-form-item label="Token">
          <el-input v-model:model-value="props.config.telegramBotToken" :disabled="!config.telegram"
                    placeholder="123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"/>
        </el-form-item>
        <el-form-item label="ChatId">
          <div>
            <div style="justify-content: space-between;width: 100%;" class="auto">
              <div style="margin-top: 4px;margin-right: 4px;">
                <el-input v-model:model-value="props.config.telegramChatId" :disabled="!config.telegram"
                          placeholder="123456789"/>
              </div>
              <div style="display: flex;margin-top: 4px;align-items: center;">
                <div>
                  <el-select v-model:model-value="chatId" @change="chatIdChange" style="width: 160px"
                             :disabled="!config.telegram">
                    <el-option v-for="item in Object.keys(chatIdMap)"
                               :key="item"
                               :label="item"
                               :value="item"/>
                  </el-select>
                </div>
                <div style="margin-left: 4px;">
                  <el-button icon="Refresh" bg text @click="getUpdates" :loading="getUpdatesLoading"
                             :disabled="!config.telegram"/>
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="图片">
          <el-switch v-model:model-value="props.config.telegramImage" :disabled="!config.telegram"/>
        </el-form-item>
        <el-form-item label="开关">
          <div style="width: 100%;display: flex;justify-content: space-between;">
            <el-switch v-model:model-value="props.config.telegram"/>
            <el-button bg text @click="messageTest('Telegram')"
                       :loading="messageTestLoading && messageTestType === 'Telegram'"
                       :disabled="!config.telegram" icon="Odometer">测试
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-collapse-item>
    <el-collapse-item title="邮箱通知" name="2">
      <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
        <el-form-item label="SMTP地址">
          <el-input v-model:model-value="props.config.mailAccount.host" :disabled="!config.mail"
                    placeholder="smtp.xx.com"/>
        </el-form-item>
        <el-form-item label="SMTP端口">
          <el-input-number v-model:model-value="props.config.mailAccount.port" min="1" max="65535"
                           :disabled="!config.mail"/>
        </el-form-item>
        <el-form-item label="发件人邮箱">
          <el-input v-model:model-value="props.config.mailAccount.from" :disabled="!config.mail"
                    placeholder="xx@xx.com"/>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model:model-value="props.config.mailAccount.pass" show-password :disabled="!config.mail"/>
        </el-form-item>
        <el-form-item label="SSL">
          <el-switch v-model:model-value="props.config.mailAccount.sslEnable" :disabled="!config.mail"/>
        </el-form-item>
        <el-form-item label="收件人邮箱">
          <el-input v-model:model-value="props.config.mailAddressee" :disabled="!config.mail"
                    placeholder="xx@xx.com"></el-input>
        </el-form-item>
        <el-form-item label="开关">
          <div style="width: 100%;display: flex;justify-content: space-between;">
            <el-switch v-model:model-value="props.config.mail"></el-switch>
            <el-button bg text @click="messageTest('Mail')" :loading="messageTestLoading && messageTestType === 'Mail'"
                       :disabled="!config.mail" icon="Odometer">测试
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-collapse-item>
    <el-collapse-item title="WebHook" name="3">
      <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
        <el-form-item label="Method">
          <el-select v-model:model-value="props.config.webHookMethod">
            <el-option v-for="item in ['POST','GET','PUT','DELETE']"
                       :key="item"
                       :label="item"
                       :value="item"/>
          </el-select>
        </el-form-item>
        <el-form-item label="URL">
          <el-input v-model:model-value="props.config.webHookUrl" type="textarea"
                    placeholder="http://www.xxx.com?text=test_${message}"></el-input>
        </el-form-item>
        <el-form-item label="Body">
          <el-input v-model:model-value="props.config.webHookBody" type="textarea"
                    placeholder='{"text":"test_${message}"}'></el-input>
        </el-form-item>
        <el-form-item label="开关">
          <div style="display: flex;width: 100%;justify-content: space-between;">
            <el-switch v-model:model-value="props.config.webHook"/>
            <el-button bg text @click="messageTest('WebHook')"
                       :loading="messageTestLoading && messageTestType === 'WebHook'"
                       :disabled="!config.webHook" icon="Odometer">测试
            </el-button>
          </div>
        </el-form-item>
        <div style="display: flex;justify-content: end;">
          <el-text class="mx-1" size="small">
            ${message} 会自动替换为信息
            <br>
            ${image} 会自动替换为图片链接
          </el-text>
        </div>
      </el-form>
    </el-collapse-item>
  </el-collapse>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import api from "../api.js";

const chatIdMap = ref({})
const chatId = ref('')
const getUpdatesLoading = ref(false)

const getUpdates = () => {
  if (!props.config.telegramBotToken.length) {
    ElMessage.error('Token 不能为空')
    return
  }

  getUpdatesLoading.value = true
  api.post("api/telegram?method=getUpdates", props.config)
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

const chatIdChange = (k) => {
  props.config.telegramChatId = chatIdMap.value[k]
}

const messageTestLoading = ref(false)
const messageTestType = ref('')

const messageTest = (type) => {
  messageTestType.value = type
  messageTestLoading.value = true
  api.post("api/message?type=" + type, props.config)
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        messageTestLoading.value = false
      })
}

const messageActiveName = ref('')

let props = defineProps(['config'])

</script>

<style>
@media (min-width: 1000px) {
  .auto {
    display: flex;
  }
}
</style>
