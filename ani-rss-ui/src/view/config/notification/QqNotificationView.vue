<template>
  <template v-if="notificationConfig['notificationType'] === 'QQ'">
    <SettingsItem label="通知模版">
      <el-input v-model="notificationConfig['notificationTemplate']" :autosize="{ minRows: 2 }"
                placeholder="${notification}" type="textarea"/>
    </SettingsItem>
    <SettingsItem label="AppID">
      <el-input v-model="notificationConfig['qqAppId']" placeholder="QQ 开放平台 AppID"/>
    </SettingsItem>
    <SettingsItem label="AppSecret">
      <el-input v-model="notificationConfig['qqClientSecret']" show-password
                placeholder="QQ 开放平台 AppSecret"/>
    </SettingsItem>
    <SettingsItem label="用户 OpenID">
      <div class="qq-bind-row">
        <el-input v-model="notificationConfig['qqOpenId']" class="qq-bind-input" placeholder="私聊 OpenID"/>
        <el-button v-if="!bindState.loading" type="primary" plain @click="startBind">
          绑定
        </el-button>
        <el-button v-else type="danger" plain @click="cancelBind">
          取消
        </el-button>
      </div>
    </SettingsItem>
    <SettingsItem v-if="bindState.message" label="绑定状态">
      <el-text type="primary">{{ bindState.message }}</el-text>
    </SettingsItem>
  </template>
</template>

<script setup>
import SettingsItem from "@/view/custom/SettingsItem.vue";
import {ElMessage} from "element-plus";
import {onUnmounted, reactive, watch} from "vue";
import * as http from "@/js/http.js";

let props = defineProps(['notificationConfig', 'config'])

let bindState = reactive({loading: false, taskId: '', timer: null, message: '', attempt: 0})

let startBind = () => {
  if (!props.notificationConfig.qqAppId) {
    ElMessage.error('请先填写 AppID')
    return
  }
  if (!props.notificationConfig.qqClientSecret) {
    ElMessage.error('请先填写 AppSecret')
    return
  }

  let attempt = ++bindState.attempt
  bindState.loading = true
  bindState.message = '正在连接 QQ 网关…'

  http.qqBind({
    appId: props.notificationConfig.qqAppId,
    clientSecret: props.notificationConfig.qqClientSecret
  }).then(res => {
    if (bindState.attempt !== attempt) {
      http.qqBindCancel({taskId: res.data.taskId}).catch(() => {
      })
      return
    }
    bindState.taskId = res.data.taskId
    bindState.message = res.data.message
    bindState.timer = setInterval(pollBind, 1200)
  }).catch(() => {
    if (bindState.attempt !== attempt) {
      return
    }
    bindState.loading = false
    bindState.message = ''
  })
}

let pollBind = () => {
  let taskId = bindState.taskId
  let attempt = bindState.attempt
  if (!taskId) {
    stopPolling()
    return
  }

  http.qqBindStatus({taskId})
      .then(res => {
        if (bindState.attempt !== attempt || bindState.taskId !== taskId) {
          return
        }
        let data = res.data
        bindState.message = data.message
        if (data.state === 'SUCCESS') {
          props.notificationConfig.qqOpenId = data.openId
          ElMessage.success(data.message)
          stopPolling()
        } else if (data.state === 'ERROR') {
          ElMessage.error(data.message)
          stopPolling()
        } else if (data.state === 'CANCELLED') {
          stopPolling()
        }
      })
      .catch(() => {
        if (bindState.attempt !== attempt) {
          return
        }
        bindState.message = ''
        stopPolling()
      })
}

let stopPolling = () => {
  bindState.loading = false
  bindState.taskId = ''
  if (bindState.timer) {
    clearInterval(bindState.timer)
    bindState.timer = null
  }
}

let cancelBind = () => {
  bindState.attempt += 1
  if (bindState.taskId) {
    http.qqBindCancel({taskId: bindState.taskId}).catch(() => {
    })
  }
  bindState.message = '已取消绑定'
  stopPolling()
}

let cancelIfBinding = () => {
  if (!bindState.loading) {
    return
  }
  bindState.attempt += 1
  if (bindState.taskId) {
    http.qqBindCancel({taskId: bindState.taskId}).catch(() => {
    })
  }
  bindState.message = ''
  stopPolling()
}

watch(() => props.notificationConfig, () => {
  cancelIfBinding()
})

watch(() => props.notificationConfig.notificationType, (value) => {
  if (value !== 'QQ') {
    cancelIfBinding()
  }
})

onUnmounted(() => {
  cancelIfBinding()
})
</script>

<style scoped>
.qq-bind-row {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 8px;
}

.qq-bind-input {
  flex: 1;
}
</style>
