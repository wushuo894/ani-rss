<template>
  <el-dialog v-model="configDialogVisible" title="设置" center>
    <div style="margin: 0 15px;" v-loading="loading">
      <el-tabs v-model:model-value="activeName">
        <el-tab-pane label="下载设置" name="download">
          <Download v-model:config="config"/>
        </el-tab-pane>
        <el-tab-pane label="基本设置">
          <Basic v-model:config="config"/>
        </el-tab-pane>
        <el-tab-pane label="全局排除">
          <Exclude ref="exclude" v-model:exclude="config.exclude"/>
        </el-tab-pane>
        <el-tab-pane label="代理设置">
          <Proxy v-model:config="config"/>
        </el-tab-pane>
        <el-tab-pane label="登录设置">
          <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
            <el-form-item label="用户名">
              <el-input v-model:model-value="config.login.username"/>
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model:model-value="config.login.password"/>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="通知">
          <div style="margin: 4px;">
            <Message ref="messageRef" v-model:config="config" v-model:message-active-name="messageActiveName"/>
          </div>
          <div style="height: 4px;"></div>
        </el-tab-pane>
        <el-tab-pane label="关于" name="about">
          <About/>
        </el-tab-pane>
      </el-tabs>
      <div style="display: flex;justify-content: end;width: 100%;">
        <el-button :loading="configButtonLoading" @click="editConfig" text bg>确定</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import CryptoJS from "crypto-js";
import api from "../api.js";
import Exclude from "../config/Exclude.vue";
import Message from "../config/Message.vue";
import Proxy from "../config/Proxy.vue";
import Download from "../config/Download.vue";
import Basic from "../config/Basic.vue";
import About from "../config/About.vue";

const configDialogVisible = ref(false)
const configButtonLoading = ref(false)
const loading = ref(true)

const config = ref({
  'download': 'qBittorrent',
  'exclude': [],
  'rename': true,
  'tmdb': false,
  'host': '',
  'username': '',
  'password': '',
  'sleep': 5,
  'watchErrorTorrent': true,
  'downloadPath': '',
  'ovaDownloadPath': '',
  'fileExist': true,
  'delete': false,
  'offset': false,
  'acronym': false,
  'titleYear': false,
  'autoDisabled': false,
  'debug': false,
  'proxy': false,
  'proxyHost': '',
  'proxyPort': 8080,
  'renameSleep': 1,
  'downloadCount': 0,
  'mail': false,
  'mailAddressee': '',
  'mailAccount': {
    'host': '',
    'port': 25,
    'from': '',
    'pass': '',
    'sslEnable': false
  },
  'login': {
    'username': '',
    'password': ''
  },
  'telegram': false,
  'telegramBotToken': '',
  'telegramChatId': '',
  'telegramApiHost': 'https://api.telegram.org',
  'webHookUrl': '',
  'webHookMethod': '',
  'webHookBody': '',
  'webHook': false
})

const activeName = ref('download')

const exclude = ref()

const messageActiveName = ref('')
const messageRef = ref()

const showConfig = (update) => {
  exclude.value?.init()
  messageRef.value?.init()
  activeName.value = update ? 'about' : 'download'
  configDialogVisible.value = true
  loading.value = true
  api.get('/api/config')
      .then(res => {
        config.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}

const editConfig = () => {
  configButtonLoading.value = true
  let my_config = JSON.parse(JSON.stringify(config.value))
  if (my_config.login.password) {
    my_config.login.password = CryptoJS.MD5(my_config.login.password).toString();
  }
  api.post('/api/config', my_config)
      .then(res => {
        ElMessage.success(res.message)
        configDialogVisible.value = false
      })
      .finally(() => {
        configButtonLoading.value = false
      })
}

defineExpose({
  showConfig
})

</script>


<style>
@media (min-width: 900px) {
  #menu > div {
    display: inline-block;
  }
}
</style>