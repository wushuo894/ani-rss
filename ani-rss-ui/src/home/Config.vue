<template>
  <el-dialog v-model="dialogVisible" center title="设置">
    <div v-loading="loading" class="loading">
      <el-tabs v-model:model-value="activeName" style="margin: 0 15px;">
        <el-tab-pane label="下载设置" name="download" :lazy="true">
          <div style="height: 500px;">
            <el-scrollbar style="padding: 0 12px">
              <Download v-model:config="config"/>
            </el-scrollbar>
          </div>
        </el-tab-pane>
        <el-tab-pane :lazy="true" label="基本设置" name="basic">
          <div style="height: 500px;">
            <el-scrollbar style="padding: 0 12px;">
              <Basic v-model:config="config"/>
            </el-scrollbar>
          </div>
        </el-tab-pane>
        <el-tab-pane label="全局排除" :lazy="true">
          <Exclude v-model:exclude="config.exclude" :show-text="true"/>
        </el-tab-pane>
        <el-tab-pane label="代理设置" :lazy="true">
          <Proxy v-model:config="config"/>
        </el-tab-pane>
        <el-tab-pane label="登录设置" :lazy="true">
          <LoginConfig :config="config"/>
        </el-tab-pane>
        <el-tab-pane label="通知" :lazy="true">
          <div style="height: 500px;">
            <el-scrollbar style="padding: 0 12px;">
              <Notification v-model:config="config"/>
            </el-scrollbar>
          </div>
        </el-tab-pane>
        <el-tab-pane :lazy="true" label="捐赠" name="afdian">
          <Afdian :config="config"/>
        </el-tab-pane>
        <el-tab-pane label="关于" name="about" :lazy="true">
          <About :config="config"/>
        </el-tab-pane>
      </el-tabs>
      <div class="action">
        <el-button :loading="configButtonLoading" bg icon="Check" text type="primary" @click="saveConfig">确定
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
import api from "@/js/api.js";
import Exclude from "@/config/Exclude.vue";
import Notification from "@/config/Notification.vue";
import Proxy from "@/config/Proxy.vue";
import Download from "@/config/Download.vue";
import Basic from "@/config/Basic.vue";
import About from "@/config/About.vue";
import LoginConfig from "@/config/LoginConfig.vue";
import Afdian from "@/config/Afdian.vue";
import {configData} from "@/js/config.js";
import * as http from "@/js/http.js";

const dialogVisible = ref(false)
const configButtonLoading = ref(false)
const loading = ref(true)

const config = ref(configData)

const activeName = ref('download')

const show = (update) => {
  activeName.value = update ? 'about' : 'download'
  dialogVisible.value = true
  loading.value = true
  http.config()
      .then(res => {
        config.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}

const saveConfig = () => {
  configButtonLoading.value = true
  let my_config = JSON.parse(JSON.stringify(config.value))

  let username = my_config.login.username.trim()
  let password = my_config.login.password.trim()

  my_config.login.username = username
  if (password) {
    my_config.login.password = CryptoJS['MD5'](password).toString();
  }

  api.post('api/setConfig', my_config)
      .then(res => {
        ElMessage.success(res.message)
        window.$reLoadList()
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
<style scoped>
.action {
  display: flex;
  justify-content: end;
  width: 100%;
  margin-top: 8px;
}
</style>
