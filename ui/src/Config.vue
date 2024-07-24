<template>
  <el-dialog v-model="configDialogVisible" title="设置" center>
    <div style="margin: 0 15px;" @keydown.enter="editConfig">
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
              <el-input-number v-model:model-value="config.sleep"></el-input-number>
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

const config = ref({
  'rename': true,
  'host': '',
  'username': '',
  'password': '',
  'sleep': 5,
  'downloadPath': '',
  'fileExist': true,
  'delete': false,
  'debug': false
})

const showConfig = () => {
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
        configDialogVisible.value = true
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
