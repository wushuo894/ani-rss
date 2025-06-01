<template>
  <el-collapse v-model="messageActiveName" accordion>
    <el-collapse-item title="通知设置" name="0">
      <div style="margin-bottom: 12px;">
        <div style="margin-bottom: 12px;margin-top: 4px;">
          <el-input v-model:model-value="props.config.messageTemplate" type="textarea"
                    placeholder="${text}" :autosize="{ minRows: 2}"/>
          <div class="flex" style="width: 100%;justify-content: end;">
            <a target="_blank" href="https://docs.wushuo.top/config/message">通知模版示例</a>
          </div>
        </div>
        <div>
          <el-checkbox-group v-model:model-value="config.messageList">
            <el-checkbox label="开始下载" value="DOWNLOAD_START"/>
            <el-checkbox label="下载完成" value="DOWNLOAD_END"/>
            <el-checkbox label="缺集" value="OMIT"/>
            <el-checkbox label="错误" value="ERROR"/>
            <el-checkbox :disabled="!props.config['verifyExpirationTime']" label="Alist上传通知" value="ALIST_UPLOAD"/>
            <el-checkbox :disabled="!props.config['verifyExpirationTime']" label="订阅完结" value="COMPLETED"/>
            <el-checkbox :disabled="!props.config['verifyExpirationTime']" label="摸鱼检测" value="PROCRASTINATING"/>
          </el-checkbox-group>
        </div>
        <div>
          <el-text class="mx-1" size="small">
            下载完成通知暂不支持 Aria2、Alist
          </el-text>
          <br>
          <AfdianPrompt :config="props.config" name="Alist上传通知、订阅完结、摸鱼检测"/>
        </div>
      </div>
    </el-collapse-item>
    <el-collapse-item title="TG通知" name="1">
      <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
        <el-form-item label="Api Host">
          <el-input v-model:model-value="props.config.telegramApiHost"
                    :disabled="config.telegram"
                    placeholder="https://api.telegram.org"/>
        </el-form-item>
        <el-form-item label="Token">
          <el-input v-model:model-value="props.config.telegramBotToken" :disabled="config.telegram"
                    placeholder="123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"/>
        </el-form-item>
        <el-form-item label="ChatId">
          <div>
            <div style="justify-content: space-between;width: 100%;" class="auto">
              <div style="margin-top: 4px;margin-right: 4px;">
                <el-input v-model:model-value="props.config.telegramChatId" :disabled="config.telegram"
                          placeholder="123456789"/>
              </div>
              <div class="flex" style="margin-top: 4px;align-items: center;">
                <div>
                  <el-select v-model:model-value="chatId" @change="chatIdChange" style="width: 160px"
                             :disabled="config.telegram">
                    <el-option v-for="item in Object.keys(chatIdMap)"
                               :key="item"
                               :label="item"
                               :value="item"/>
                  </el-select>
                </div>
                <div style="margin-left: 4px;">
                  <el-button icon="Refresh" bg text @click="getUpdates" :loading="getUpdatesLoading"
                             :disabled="config.telegram"/>
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="话题ID">
          <el-input-number v-model="props.config.telegramTopicId" :disabled="config.telegram"
                           :min="-1" style="width: 160px;"/>
        </el-form-item>
        <el-form-item label="图片">
          <el-switch v-model:model-value="props.config.telegramImage" :disabled="config.telegram"/>
        </el-form-item>
        <el-form-item label="格式">
          <div style="width: 160px;">
            <el-select v-model:model-value="props.config.telegramFormat" :disabled="config.telegram" placeholder="None">
              <el-option label="None" value=""/>
              <el-option label="Markdown" value="Markdown"/>
              <el-option label="HTML" value="HTML"/>
            </el-select>
          </div>
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
          <el-input v-model:model-value="props.config.mailAccount.host" :disabled="config.mail"
                    placeholder="smtp.xx.com"/>
        </el-form-item>
        <el-form-item label="SMTP端口">
          <el-input-number v-model:model-value="props.config.mailAccount.port" :min="1" :max="65535"
                           :disabled="config.mail"/>
        </el-form-item>
        <el-form-item label="发件人邮箱">
          <el-input v-model:model-value="props.config.mailAccount.from" :disabled="config.mail"
                    placeholder="xx@xx.com"/>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model:model-value="props.config.mailAccount.pass" show-password :disabled="config.mail"/>
        </el-form-item>
        <el-form-item label="SSL">
          <el-switch v-model:model-value="props.config.mailAccount.sslEnable" :disabled="config.mail"/>
        </el-form-item>
        <el-form-item label="STARTTLS">
          <el-switch v-model:model-value="props.config.mailAccount['starttlsEnable']" :disabled="config.mail"/>
        </el-form-item>
        <el-form-item label="收件人邮箱">
          <el-input v-model:model-value="props.config.mailAddressee" :disabled="config.mail"
                    placeholder="xx@xx.com"></el-input>
        </el-form-item>
        <el-form-item label="图片">
          <el-switch v-model:model-value="props.config.mailImage" :disabled="config.mail"/>
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
    <el-collapse-item title="Server酱" name="3">
      <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
        <el-form-item label="Type">
          <el-select v-model="props.config.serverChanType" :disabled="config.serverChan">
            <el-option
                v-for="(value, label) in { 'server酱': 'serverChan', 'server酱³': 'serverChan3' }"
                :key="label"
                :label="label"
                :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="sendKey" v-if="props.config.serverChanType === 'serverChan'">
          <el-input v-model="props.config.serverChanSendKey" placeholder="1234567890"
                    :disabled="config.serverChan"></el-input>
        </el-form-item>
        <el-form-item label="apiUrl" v-else-if="props.config.serverChanType === 'serverChan3'">
          <el-input v-model="props.config.serverChan3ApiUrl"
                    :disabled="config.serverChan"
                    placeholder="https://<uid>.push.ft07.com/send/<sendKey>.send"></el-input>
        </el-form-item>
        <el-form-item label="事件标题">
          <el-switch v-model="props.config['serverChanTitleAction']"
                     :disabled="!config.serverChan"/>
        </el-form-item>
        <el-form-item label="开关">
          <div style="display: flex;width: 100%;justify-content: space-between;">
            <el-switch v-model:model-value="props.config.serverChan"/>
            <el-button bg text @click="messageTest('ServerChan')"
                       :loading="messageTestLoading && messageTestType === 'ServerChan'"
                       :disabled="!config.serverChan" icon="Odometer">测试
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-collapse-item>
    <el-collapse-item name="4" title="系统通知">
      <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
        <el-form-item label="开关">
          <div style="display: flex;width: 100%;justify-content: space-between;">
            <el-switch v-model:model-value="props.config.systemMsg"/>
            <el-button :disabled="!config.systemMsg" :loading="messageTestLoading" bg
                       icon="Odometer"
                       text @click="messageTest('SystemMsg')">测试
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-collapse-item>
    <el-collapse-item title="WebHook" name="5">
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
                    autosize
                    placeholder="http://www.xxx.com?text=test_${message}"></el-input>
        </el-form-item>
        <el-form-item label="Body">
          <el-input v-model:model-value="props.config.webHookBody" type="textarea"
                    :autosize="{ minRows: 2}"
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
          <a target="_blank" href="https://docs.wushuo.top/config/message">通知模版示例</a>
        </div>
      </el-form>
    </el-collapse-item>
    <el-collapse-item name="6" title="Emby媒体库刷新">
      <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
        <el-form-item label="EmbyHost">
          <el-input v-model="props.config['embyHost']" placeholder="http://x.x.x.x:8096"/>
        </el-form-item>
        <el-form-item label="Emby密钥">
          <el-input v-model="props.config['embyApiKey']"/>
        </el-form-item>
        <el-form-item label="媒体库">
          <el-checkbox-group v-model="props.config['embyRefreshViewIds']">
            <el-checkbox
                v-for="view in views"
                :key="view.id"
                :label="view.name"
                :value="view.id"/>
          </el-checkbox-group>
          <el-button :loading="getEmbyViewsLoading" bg icon="Refresh" text @click="getEmbyViews"/>
        </el-form-item>
        <el-form-item label="延迟">
          <el-input-number v-model="props.config['embyDelayed']" :min="0">
            <template #suffix>
              <span>秒</span>
            </template>
          </el-input-number>
        </el-form-item>
        <el-form-item label="开启">
          <el-switch v-model="props.config['embyRefresh']" :disabled="!props.config['verifyExpirationTime']"/>
        </el-form-item>
        <div class="flex" style="justify-content: space-between;width: 100%;">
          <AfdianPrompt :config="props.config" name="Emby媒体库刷新"/>
          <el-button :loading="messageTestLoading" bg
                     icon="Odometer"
                     text @click="messageTest('EmbyRefresh')">测试
          </el-button>
        </div>
      </el-form>
    </el-collapse-item>
  </el-collapse>
</template>

<script setup>
import {onMounted, ref} from "vue";
import {ElMessage} from "element-plus";
import api from "../api.js";
import AfdianPrompt from "../other/AfdianPrompt.vue";

const chatIdMap = ref({})
const chatId = ref('')
const getUpdatesLoading = ref(false)

const views = ref([])

const getEmbyViewsLoading = ref(false)

const getEmbyViews = () => {
  getEmbyViewsLoading.value = true
  api.post('api/emby?type=getViews', props.config)
      .then(res => {
        views.value = res.data
      })
      .finally(() => {
        getEmbyViewsLoading.value = false
      })
}

onMounted(() => {
  if (props.config['embyHost'] && props.config['embyApiKey']) {
    getEmbyViews()
  }
})

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

  let config = JSON.parse(JSON.stringify(props.config))
  config.embyDelayed = 0

  api.post("api/message?type=" + type, config)
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        messageTestLoading.value = false
      })
}

const messageActiveName = ref('0')

let props = defineProps(['config'])

</script>

<style>
@media (min-width: 1000px) {
  .auto {
    display: flex;
  }
}
</style>
