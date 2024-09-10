<template>
  <el-dialog v-model="configDialogVisible" title="设置" center>
    <div style="margin: 0 15px;" v-loading="loading">
      <el-tabs v-model:model-value="activeName">
        <el-tab-pane label="下载设置" name="qb">
          <el-form label-width="auto"
                   @submit="(event)=>{
                    event.preventDefault()
                   }">
            <el-form-item label="下载工具">
              <el-select v-model:model-value="config.download">
                <el-option v-for="item in downloadSelect"
                           :key="item"
                           :label="item"
                           :value="item"/>
              </el-select>
            </el-form-item>
            <el-form-item label="地址">
              <el-input v-model:model-value="config.host" placeholder="http://192.168.1.66:8080"></el-input>
            </el-form-item>
            <template v-if="config.download !== 'Aria2'">
              <el-form-item label="用户名">
                <el-input v-model:model-value="config.username" placeholder="username"></el-input>
              </el-form-item>
              <el-form-item label="密码">
                <el-input show-password v-model:model-value="config.password" placeholder="password"></el-input>
              </el-form-item>
            </template>
            <el-form-item label="RPC 密钥" v-else>
              <el-input show-password v-model:model-value="config.password" placeholder=""></el-input>
            </el-form-item>
            <el-form-item>
              <div style="display:flex;width: 100%;justify-content: end;">
                <el-button @click="downloadLoginTest" bg text :loading="downloadLoginTestLoading">登录测试</el-button>
              </div>
            </el-form-item>
            <el-form-item label="保存位置">
              <el-input v-model:model-value="config.downloadPath" placeholder="/downloads/media/anime"></el-input>
            </el-form-item>
            <el-form-item label="剧场版保存位置">
              <el-input v-model:model-value="config.ovaDownloadPath" placeholder="/downloads/media/ova"></el-input>
            </el-form-item>
            <el-form-item label="自动删除">
              <div>
                <el-switch v-model:model-value="config.delete"></el-switch>
                <br>
                <el-text class="mx-1" size="small">
                  自动删除已完成的任务, 不会删除本地文件
                </el-text>
              </div>
            </el-form-item>
            <el-form-item label="拼音首字母">
              <div>
                <el-switch v-model:model-value="config.acronym"></el-switch>
                <br>
                <el-text class="mx-1" size="small">
                  存放到 #,0,A-Z 文件夹下
                </el-text>
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
                  文件已下载自动跳过 此选项必须启用 自动重命名。确保 下载工具 与本程序 docker 映射挂载路径一致
                  <a href="https://docs.wushuo.top/docs#%E8%87%AA%E5%8A%A8%E8%B7%B3%E8%BF%87"
                     target="_blank">详细说明</a>
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
        <el-tab-pane label="全局排除">
          <Exclude ref="exclude" v-model:exclude="config.exclude"/>
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
            <el-form-item label="使用文档">
              <a href="https://docs.wushuo.top" target="_blank">https://docs.wushuo.top</a>
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
                <div v-loading="actionLoading" id="menu">
                  <el-badge class="item" v-if="about.update" value="new">
                    <el-button type="success" @click="update" text bg icon="Top">更新</el-button>
                  </el-badge>
                  <div style="margin: 6px;" v-if="about.update"></div>
                  <el-popconfirm title="你确定重启吗?" @confirm="stop(0)">
                    <template #reference>
                      <el-button type="warning" text bg icon="RefreshRight">重启 ani-rss</el-button>
                    </template>
                    <template #actions="{ confirm, cancel }">
                      <el-button size="small" @click="cancel" bg text icon="Close">取消</el-button>
                      <div style="margin: 4px;"></div>
                      <el-button
                          type="danger"
                          size="small"
                          @click="confirm"
                          bg text
                          icon="Check"
                      >
                        确定
                      </el-button>
                    </template>
                  </el-popconfirm>
                  <div style="margin: 6px;"></div>
                  <el-popconfirm title="你确定关闭吗?" @confirm="stop(1)">
                    <template #reference>
                      <el-button type="danger" text bg icon="SwitchButton">关闭 ani-rss</el-button>
                    </template>
                    <template #actions="{ confirm, cancel }">
                      <el-button size="small" @click="cancel" bg text icon="Close">取消</el-button>
                      <div style="margin: 4px;"></div>
                      <el-button
                          type="danger"
                          size="small"
                          @click="confirm"
                          bg text
                          icon="Check"
                      >
                        确定
                      </el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </el-form-item>
          </el-form>
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
import api from "./api.js";
import Exclude from "./Exclude.vue";

const configDialogVisible = ref(false)
const configButtonLoading = ref(false)
const loading = ref(true)

const downloadSelect = ref([
  'qBittorrent',
  'Transmission',
  'Aria2'
])

const config = ref({
  'download': 'qBittorrent',
  'exclude': [],
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

const exclude = ref()

const showConfig = (ab) => {
  exclude.value?.init()
  about.value = ab
  if (!ab.update) {
    activeName.value = 'qb'
    api.get('/api/about')
        .then(res => {
          about.value = res.data
        })
  } else {
    activeName.value = 'about'
  }

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

const actionLoading = ref(false)

const stop = (status) => {
  actionLoading.value = true
  api.post("/api/stop?status=" + status)
      .then(res => {
        ElMessage.success(res.message)
        setTimeout(() => {
          localStorage.removeItem("authorization")
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
          localStorage.removeItem("authorization")
          location.reload()
        }, 5000)
      })
      .finally(() => {
        actionLoading.value = false
      })
}

const downloadLoginTestLoading = ref(false)
const downloadLoginTest = () => {
  downloadLoginTestLoading.value = true
  api.post("/api/downloadLoginTestLoading", config.value)
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        downloadLoginTestLoading.value = false
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