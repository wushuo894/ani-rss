<template>
  <el-dialog v-model="dialogVisible" center title="设置">
    <div v-loading="loading">
      <el-tabs v-model:model-value="activeName" style="margin: 0 15px;">
        <el-tab-pane label="下载设置" name="download" :lazy="true">
          <template #label>
            <el-icon>
              <DownloadIcon/>
            </el-icon>
            <span>下载设置</span>
          </template>
          <div style="height: 500px;">
            <el-scrollbar style="padding: 0 12px">
              <Download v-model:config="config"/>
            </el-scrollbar>
          </div>
        </el-tab-pane>
        <el-tab-pane :lazy="true" label="基本设置" name="basic">
          <template #label>
            <el-icon>
              <Operation/>
            </el-icon>
            <span>基本设置</span>
          </template>
          <div style="height: 500px;">
            <el-scrollbar style="padding: 0 12px">
              <Basic v-model:config="config"/>
            </el-scrollbar>
          </div>
        </el-tab-pane>
        <el-tab-pane label="全局排除" :lazy="true">
          <template #label>
            <el-icon>
              <Filter/>
            </el-icon>
            <span>全局排除</span>
          </template>
          <Exclude v-model:exclude="config.exclude" :show-text="true"/>
        </el-tab-pane>
        <el-tab-pane label="代理设置" :lazy="true">
          <template #label>
            <el-icon>
              <Promotion/>
            </el-icon>
            <span>代理设置</span>
          </template>
          <Proxy v-model:config="config"/>
        </el-tab-pane>
        <el-tab-pane label="登录设置" :lazy="true">
          <template #label>
            <el-icon>
              <User/>
            </el-icon>
            <span>登录设置</span>
          </template>
          <LoginConfig :config="config"/>
        </el-tab-pane>
        <el-tab-pane label="通知" :lazy="true">
          <template #label>
            <el-icon>
              <ChatRound/>
            </el-icon>
            <span>通知</span>
          </template>
          <div style="height: 500px;">
            <el-scrollbar style="padding: 0 12px">
              <Notification ref="notificationRef" v-model:config="config"/>
            </el-scrollbar>
          </div>
          <div style="height: 4px;"></div>
        </el-tab-pane>
        <el-tab-pane :lazy="true" label="捐赠" name="afdian">
          <template #label>
            <el-icon>
              <Mug/>
            </el-icon>
            <span>捐赠</span>
          </template>
          <Afdian :config="config"/>
        </el-tab-pane>
        <el-tab-pane label="关于" name="about" :lazy="true">
          <template #label>
            <el-icon>
              <InfoFilled/>
            </el-icon>
            <span>关于</span>
          </template>
          <About :config="config"/>
        </el-tab-pane>
      </el-tabs>
      <div style="display: flex;justify-content: end;width: 100%;margin-top: 8px;">
        <el-button :loading="configButtonLoading" @click="editConfig" text bg icon="Check" type="primary">确定
        </el-button>
        <el-button icon="Close" bg text @click="dialogVisible = false">取消</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import CryptoJS from "crypto-js";
import api from "../js/api.js";
import Exclude from "../config/Exclude.vue";
import Notification from "../config/Notification.vue";
import Proxy from "../config/Proxy.vue";
import Download from "../config/Download.vue";
import Basic from "../config/Basic.vue";
import About from "../config/About.vue";
import LoginConfig from "../config/LoginConfig.vue";
import Afdian from "../config/Afdian.vue";
import {
  ChatRound,
  Download as DownloadIcon,
  Filter,
  InfoFilled,
  Mug,
  Operation,
  Promotion,
  User
} from "@element-plus/icons-vue";

const dialogVisible = ref(false)
const configButtonLoading = ref(false)
const loading = ref(true)

const config = ref({
  'mikanHost': '',
  'downloadToolType': 'qBittorrent',
  'exclude': [],
  'rename': true,
  'rss': false,
  'tmdb': false,
  'downloadToolHost': '',
  'downloadToolUsername': '',
  'downloadToolPassword': '',
  'sleep': 5,
  'watchErrorTorrent': true,
  'delayedDownload': 0,
  'downloadPath': '',
  'ovaDownloadPath': '',
  'fileExist': true,
  'awaitStalledUP': true,
  'delete': false,
  'deleteStandbyRSSOnly': false,
  'deleteFiles': false,
  'offset': false,
  'acronym': false,
  'titleYear': false,
  'autoDisabled': false,
  'skip5': true,
  'logsMax': 2048,
  'debug': false,
  'proxy': false,
  'proxyHost': '',
  'proxyUsername': '',
  'proxyPassword': '',
  'proxyPort': 8080,
  'renameSleep': 1,
  'downloadCount': 0,
  'login': {
    'downloadToolUsername': '',
    'downloadToolPassword': ''
  },
  'qbUseDownloadPath': false,
  'showPlaylist': false,
  'enabledExclude': false,
  'importExclude': false,
  'bgmToken': '',
  'apiKey': '',
  'weekShow': false,
  'scoreShow': false,
  'standbyRss': false,
  'downloadNew': false,
  'innerIP': false,
  'renameTemplate': '',
  'verifyLoginIp': true,
  'loginEffectiveHours': 3,
  'trackersUpdateUrls': '',
  'autoTrackersUpdate': false,
  'renameMinSize': 100,
  'tmdbId': false,
  'renameDelYear': false,
  'renameDelTmdbId': false,
  'ratioLimit': -2,
  'seedingTimeLimit': -2,
  'inactiveSeedingTimeLimit': -2,
  'autoUpdate': false,
  'outTradeNo': ''
})

const activeName = ref('download')

const notificationRef = ref()

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
        emit('load')
        dialogVisible.value = false
      })
      .finally(() => {
        configButtonLoading.value = false
      })
}

defineExpose({
  show
})
const emit = defineEmits(['load'])
</script>

<style scoped>
.el-tabs__item > .el-icon {
  display: none;
}
</style>
