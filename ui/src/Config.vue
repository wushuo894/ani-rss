<template>
  <el-dialog v-model="configDialogVisible" title="设置" center>
    <div style="margin: 0 15px;" @keydown.enter="editConfig" v-loading="loading">
      <el-tabs v-model:model-value="activeName">
        <el-tab-pane label="qBittorrent 设置" name="qb">
          <el-form label-width="auto"
                   @submit="(event)=>{
                    event.preventDefault()
                   }">
            <el-form-item label="地址">
              <el-input v-model:model-value="config.host" placeholder="http://192.168.0.x:18080"></el-input>
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model:model-value="config.username" placeholder="username"></el-input>
            </el-form-item>
            <el-form-item label="密码">
              <el-input show-password v-model:model-value="config.password" placeholder="password"></el-input>
            </el-form-item>
            <el-form-item label="保存位置">
              <el-input v-model:model-value="config.downloadPath" placeholder="/downloads/media/anime"></el-input>
            </el-form-item>
            <el-form-item label="剧场版保存位置">
              <el-input v-model:model-value="config.ovaDownloadPath" placeholder="/downloads/media/ova"></el-input>
            </el-form-item>
            <el-form-item label="拼音首字母">
              <div>
                <el-switch v-model:model-value="config.acronym"></el-switch>
                <div>
                  存放为 #,0,A-Z 文件夹下
                </div>
              </div>
            </el-form-item>
            <el-form-item label="同时下载数量限制">
              <div>
                <el-input-number v-model:model-value="config.downloadCount" min="0"></el-input-number>
                <div>
                  设置为时 0 不做限制
                </div>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="基本设置">
          <el-form label-width="auto"
                   @submit="(event)=>{
                      event.preventDefault()
                   }">
            <el-form-item label="RSS间隔(分钟)">
              <el-input-number v-model:model-value="config.sleep" min="1"></el-input-number>
            </el-form-item>
            <el-form-item label="自动重命名">
              <el-switch v-model:model-value="config.rename"></el-switch>
            </el-form-item>
            <el-form-item label="重命名间隔(分钟)">
              <el-input-number v-model:model-value="config.renameSleep" min="1"
                               :disabled="!config.rename"></el-input-number>
            </el-form-item>
            <el-form-item label="自动跳过">
              <div>
                <el-switch v-model:model-value="config.fileExist" :disabled="!config.rename"></el-switch>
                <br>
                <el-text class="mx-1" size="small">
                  文件已下载自动跳过 此选项必须启用 自动重命名。确保 qBittorrent 与本程序 docker 映射挂载路径一致
                </el-text>
              </div>
            </el-form-item>
            <el-form-item label="自动删除">
              <div>
                <el-switch v-model:model-value="config.delete"></el-switch>
                <br>
                <el-text class="mx-1" size="small">
                  自动删除已完成的任务
                </el-text>
              </div>
            </el-form-item>
            <el-form-item label="自动推断剧集偏移">
              <el-switch v-model:model-value="config.offset"></el-switch>
            </el-form-item>
            <el-form-item label="自动禁用订阅">
              <div>
                <el-switch v-model:model-value="config.autoDisabled"></el-switch>
                <br>
                <el-text class="mx-1" size="small">
                  根据 Bangumi 获取总集数 当所有集数都已下载时自动禁用该订阅
                </el-text>
              </div>
            </el-form-item>
            <el-form-item label="DEBUG">
              <el-switch v-model:model-value="config.debug"></el-switch>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="代理设置">
          <el-form label-width="auto"
                   @submit="(event)=>{
                      event.preventDefault()
                   }">
            <el-form-item label="启用">
              <el-switch v-model:model-value="config.proxy"></el-switch>
            </el-form-item>
            <el-form-item label="IP">
              <el-input v-model:model-value="config.proxyHost" :disabled="!config.proxy"
                        placeholder="192.168.0.x"></el-input>
            </el-form-item>
            <el-form-item label="端口">
              <el-input-number v-model:model-value="config.proxyPort" :disabled="!config.proxy" min="1"
                               max="65535"></el-input-number>
            </el-form-item>
          </el-form>
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
        <el-tab-pane label="邮件通知">
          <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
            <el-form-item label="SMTP地址">
              <el-input v-model:model-value="config.mailAccount.host" :disabled="!config.mail"
                        placeholder="smtp.xx.com"/>
            </el-form-item>
            <el-form-item label="SMTP端口">
              <el-input-number v-model:model-value="config.mailAccount.port" min="1" max="65535"
                               :disabled="!config.mail"/>
            </el-form-item>
            <el-form-item label="发件人邮箱">
              <el-input v-model:model-value="config.mailAccount.from" :disabled="!config.mail" placeholder="xx@xx.com"/>
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model:model-value="config.mailAccount.pass" show-password :disabled="!config.mail"/>
            </el-form-item>
            <el-form-item label="SSL">
              <el-switch v-model:model-value="config.mailAccount.sslEnable" :disabled="!config.mail"/>
            </el-form-item>
            <el-form-item label="收件人邮箱">
              <el-input v-model:model-value="config.mailAddressee" :disabled="!config.mail"
                        placeholder="xx@xx.com"></el-input>
            </el-form-item>
            <el-form-item label="总开关">
              <el-switch v-model:model-value="config.mail"></el-switch>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="关于" name="about">
          <el-form style="max-width: 600px" label-width="auto"
                   @submit="(event)=>{
                      event.preventDefault()
                   }">
            <el-form-item label="GitHub">
              <a href="https://github.com/wushuo894/ani-rss" target="_blank">https://github.com/wushuo894/ani-rss</a>
            </el-form-item>
            <el-form-item label="版本号">
              <div>
                <div v-loading="about.version.length < 1" style="min-height: 150px;">
                  v{{ about.version }}
                  <div v-if="about.update">
                    <a href="https://github.com/wushuo894/ani-rss/releases/latest" target="_blank">有更新 v{{
                        about.latest
                      }}</a>
                    <div v-if="about.markdownBody" v-html="about.markdownBody"></div>
                  </div>
                </div>
                <div v-loading="actionLoading">
                  <el-button type="success" v-if="about.update" @click="update" text bg icon="Top">更新</el-button>
                  <el-button type="warning" @click="stop(0)" text bg icon="RefreshRight">重启 ani-rss</el-button>
                  <el-button type="danger" @click="stop(1)" text bg icon="SwitchButton">关闭 ani-rss</el-button>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div style="display: flex;justify-content: end;width: 100%;">
        <el-button :loading="configButtonLoading" @click="editConfig" text bg type="primary">确定</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import CryptoJS from "crypto-js";
import api from "./api.js";

const configDialogVisible = ref(false)
const configButtonLoading = ref(false)
const loading = ref(true)

const config = ref({
  'rename': true,
  'host': '',
  'username': '',
  'password': '',
  'sleep': 5,
  'downloadPath': '',
  'ovaDownloadPath': '',
  'fileExist': true,
  'delete': false,
  'offset': false,
  'acronym': false,
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
  }
})

const about = ref({
  'version': '',
  'latest': '',
  'update': false,
  'markdownBody': ''
})

const activeName = ref('qb')
const showConfig = (ab) => {
  if (!ab.update) {
    api.get('/api/about')
        .then(res => {
          about.value = res.data
        })
  } else {
    activeName.value = 'about'
  }

  configDialogVisible.value = true
  about.value = ab
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

const actionLoading = ref(false)

const stop = (status) => {
  actionLoading.value = true
  api.post("/api/stop?status=" + status)
      .then(res => {
        ElMessage.success(res.message)
        setTimeout(() => {
          location.reload()
        }, 5000)
      })
      .finally(() => {
        actionLoading.value = false
      })
}

const update = () => {
  actionLoading.value = true
  api.post("/api/update")
      .then(res => {
        ElMessage.success(res.message)
        setTimeout(() => {
          location.reload()
        }, 5000)
      })
      .finally(() => {
        actionLoading.value = false
      })
}

defineExpose({
  showConfig
})
</script>
