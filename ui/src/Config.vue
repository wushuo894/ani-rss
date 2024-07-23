<template>
  <el-dialog v-model="configDialogVisible" title="设置" center>
    <el-form style="max-width: 600px" label-width="auto">
      <el-form-item label="qBittorrent 地址">
        <el-input v-model:model-value="config.host"></el-input>
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model:model-value="config.username"></el-input>
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model:model-value="config.password"></el-input>
      </el-form-item>
      <el-form-item label="间隔">
        <el-input-number v-model:model-value="config.sleep"></el-input-number>
      </el-form-item>
      <el-form-item label="重命名">
        <el-switch v-model:model-value="config.rename"></el-switch>
      </el-form-item>
      <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;">
        <el-button :loading="configButtonLoading" @click="editConfig">确定</el-button>
      </div>
    </el-form>
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
  'sleep': 5
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
