<template>
  <el-dialog v-model="dialogVisible" title="设置" center v-if="dialogVisible">
    <div style="margin: 0 15px;" v-loading="loading">
      <el-tabs v-model:model-value="activeName">
        <el-tab-pane label="下载设置" name="download" :lazy="true">
          <div style="height: 500px;">
            <el-scrollbar style="padding: 0 12px">
              <Download v-model:config="config"/>
            </el-scrollbar>
          </div>
        </el-tab-pane>
        <el-tab-pane label="基本设置" :lazy="true">
          <div style="height: 500px;">
            <el-scrollbar style="padding: 0 12px">
              <Basic v-model:config="config"/>
            </el-scrollbar>
          </div>
        </el-tab-pane>
        <el-tab-pane label="全局排除" :lazy="true">
          <Exclude ref="exclude" v-model:exclude="config.exclude" :show-text="true"/>
        </el-tab-pane>
        <el-tab-pane label="代理设置" :lazy="true">
          <Proxy v-model:config="config"/>
        </el-tab-pane>
        <el-tab-pane label="登录设置" :lazy="true">
          <LoginConfig :config="config"/>
        </el-tab-pane>
        <el-tab-pane label="通知" :lazy="true">
          <div style="margin: 4px;">
            <Message ref="messageRef" v-model:config="config" v-model:message-active-name="messageActiveName"/>
          </div>
          <div style="height: 4px;"></div>
        </el-tab-pane>
        <el-tab-pane label="关于" name="about" :lazy="true">
          <About/>
        </el-tab-pane>
      </el-tabs>
      <div style="display: flex;justify-content: end;width: 100%;">
        <el-button :loading="configButtonLoading" @click="editConfig" text bg icon="Check">确定</el-button>
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
import LoginConfig from "../config/LoginConfig.vue";

const dialogVisible = ref(false)
const configButtonLoading = ref(false)
const loading = ref(true)

const config = ref({
  'mikanHost': '',
  'download': 'qBittorrent',
  'exclude': [],
  'rename': true,
  'rss': false,
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
  'skip5': true,
  'debug': false,
  'proxy': false,
  'proxyHost': '',
  'proxyUsername': '',
  'proxyPassword': '',
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
  'webHook': false,
  'qbRenameTitle': true,
  'qbUseDownloadPath': false,
  'seasonName': 'Season 1',
  'showPlaylist': false,
  'enabledExclude': false,
  'importExclude': false,
  'bgmToken': '',
  'apiKey': '',
  'weekShow': false,
  'scoreShow': false,
  'backRss': false
})

const activeName = ref('download')

const exclude = ref()

const messageActiveName = ref('')
const messageRef = ref()

const show = (update) => {
  activeName.value = update ? 'about' : 'download'
  dialogVisible.value = true
  loading.value = true
  api.get('api/config')
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
  api.post('api/config', my_config)
      .then(res => {
        ElMessage.success(res.message)
        dialogVisible.value = false
      })
      .finally(() => {
        configButtonLoading.value = false
      })
}

defineExpose({
  show
})

</script>


<style>
@media (min-width: 900px) {
  #menu > div {
    display: inline-block;
  }
}
</style>
