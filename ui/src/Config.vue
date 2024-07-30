<template>
  <el-dialog v-model="configDialogVisible" title="设置" center>
    <div style="margin: 0 15px;" @keydown.enter="editConfig" v-loading="loading">
      <el-tabs>
        <el-tab-pane label="qBittorrent 设置">
          <el-form label-width="auto"
                   @submit="(event)=>{
                    event.preventDefault()
                   }">
            <el-form-item label="地址">
              <el-input v-model:model-value="config.host"></el-input>
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model:model-value="config.username"></el-input>
            </el-form-item>
            <el-form-item label="密码">
              <el-input show-password v-model:model-value="config.password"></el-input>
            </el-form-item>
            <el-form-item label="下载地址">
              <el-input v-model:model-value="config.downloadPath"></el-input>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="基本设置">
          <el-form style="max-width: 600px" label-width="auto"
                   @submit="(event)=>{
                      event.preventDefault()
                   }">
            <el-form-item label="间隔(分钟)">
              <el-input-number v-model:model-value="config.sleep" min="1"></el-input-number>
            </el-form-item>
            <el-form-item label="自动重命名">
              <el-switch v-model:model-value="config.rename"></el-switch>
            </el-form-item>
            <el-form-item label="文件已下载自动跳过">
              <div>
                <el-switch v-model:model-value="config.fileExist" :disabled="!config.rename"></el-switch>
                <br>
                <el-text class="mx-1" size="small">此选项必须启用 自动重命名。确保 qBittorrent 与本程序 docker 映射挂载路径一致
                </el-text>
              </div>
            </el-form-item>
            <el-form-item label="自动删除已完成任务">
              <el-switch v-model:model-value="config.delete"></el-switch>
            </el-form-item>
            <el-form-item label="DEBUG">
              <el-switch v-model:model-value="config.debug"></el-switch>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="代理设置">
          <el-form style="max-width: 600px" label-width="auto"
                   @submit="(event)=>{
                      event.preventDefault()
                   }">
            <el-form-item label="启用">
              <el-switch v-model:model-value="config.proxy"></el-switch>
            </el-form-item>
            <el-form-item label="IP">
              <el-input v-model:model-value="config.proxyHost" :disabled="!config.proxy"></el-input>
            </el-form-item>
            <el-form-item label="端口">
              <el-input-number v-model:model-value="config.proxyPort" :disabled="!config.proxy" min="1"
                               max="65535"></el-input-number>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="关于">
          <el-form style="max-width: 600px" label-width="auto"
                   @submit="(event)=>{
                      event.preventDefault()
                   }">
            <el-form-item label="GitHub">
              <a href="https://github.com/wushuo894/ani-rss" target="_blank">https://github.com/wushuo894/ani-rss</a>
            </el-form-item>
            <el-form-item label="版本号" v-loading="about.version.length < 1">
              <div>
                v{{ about.version }}
                <div v-if="about.update">
                  <a href="https://github.com/wushuo894/ani-rss/releases/latest" target="_blank">有更新 v{{
                      about.latest
                    }}</a>
                  <div v-if="about.markdownBody" v-html="about.markdownBody"></div>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div style="display: flex;justify-content: end;width: 100%;">
        <el-button :loading="configButtonLoading" @click="editConfig">确定</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";

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
  'fileExist': true,
  'delete': false,
  'debug': false,
  'proxy': false,
  'proxyHost': '',
  'proxyPort': 8080
})

const about = ref({
  'version': '',
  'latest': '',
  'update': false,
  'markdownBody': ''
})

const showConfig = () => {
  configDialogVisible.value = true
  fetch('/api/config', {
    'method': 'GET'
  })
      .then(res => res.json())
      .then(res => {
        if (res.code !== 200) {
          ElMessage.error(res.message)
          return
        }
        config.value = res.data
        loading.value = false
      })
  fetch('/api/about', {
    'method': 'GET'
  }).then(res => res.json())
      .then(res => {
        if (res.code !== 200) {
          ElMessage.error(res.message)
          return
        }
        about.value = res.data
      })
}
const editConfig = () => {
  configButtonLoading.value = true
  fetch('/api/config', {
    'method': 'POST',
    'body': JSON.stringify(config.value)
  })
      .then(res => res.json())
      .then(res => {
        configButtonLoading.value = false
        if (res.code !== 200) {
          ElMessage.error(res.message)
          return
        }
        ElMessage.success(res.message)
        configDialogVisible.value = false
      })
}
defineExpose({
  showConfig
})
</script>
