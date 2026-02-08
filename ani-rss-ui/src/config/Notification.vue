<template>
  <el-collapse>
    <el-collapse-item title="通知模板">
      <el-input v-model="props.config['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${text}" type="textarea"/>
      <div class="flex notification-template-link">
        <el-link type="primary" href="https://docs.wushuo.top/config/notification" target="_blank">
          通知模版示例
        </el-link>
      </div>
    </el-collapse-item>
  </el-collapse>
  <div class="notification-container">
    <div>
      <el-space wrap class="flex flex-wrap gap-4" size="small">
        <el-card v-for="it in props.config['notificationConfigList']" shadow="never" class="notification-card">
          <div class="flex notification-card-content">
            <div>
              <p style="width: 110px;">
                {{ getLabel(it['notificationType']) }}
              </p>
              <el-text line-clamp="1" size="small" class="notification-card-text" truncated>
                {{ it['comment'] ? it['comment'] : '无备注' }}
              </el-text>
            </div>
            <div>
              <el-dropdown trigger="click">
                <el-button circle icon="MoreFilled" size="large" text type="primary"/>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="notificationConfigRef?.show(it)">
                      <el-text type="primary">
                        <el-icon>
                          <Edit/>
                        </el-icon>
                        编辑
                      </el-text>
                    </el-dropdown-item>
                    <el-dropdown-item @click="del(it)">
                      <el-text type="danger">
                        <el-icon>
                          <Delete/>
                        </el-icon>
                        删除
                      </el-text>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-card>
      </el-space>
    </div>
    <el-button
        icon="FolderAdd"
        bg text
        type="primary"
        @click="add"
        class="notification-add-button"
        :loading="addLoading"
    >
      添加通知
    </el-button>
  </div>
  <NotificationConfig ref="notificationConfigRef" v-model:config="props.config"/>
</template>

<script setup>
import NotificationConfig from "./NotificationConfig.vue";
import {ref} from "vue";

import {getLabel} from "@/js/notification-type.js";
import api from "@/js/api.js";
import {Delete, Edit} from "@element-plus/icons-vue";

let addLoading = ref(false)

let add = () => {
  addLoading.value = true
  api.post('api/notification?type=add')
      .then((res) => {
        props.config['notificationConfigList'].push(res.data)
        notificationConfigRef.value?.show(res.data)
      })
      .finally(() => {
        addLoading.value = false
      })
}

let del = (it) => {
  props.config['notificationConfigList'] = props.config['notificationConfigList'].filter(item => item !== it)
}

let notificationConfigRef = ref()

let props = defineProps(['config'])
</script>

<style scoped>
.notification-template-link {
  width: 100%;
  justify-content: end;
}

.notification-container {
  margin-top: 8px;
}

.notification-card {
  min-width: 180px;
}

.notification-card-content {
  align-items: center;
  justify-content: space-between;
}

.notification-card-text {
  max-width: 120px;
}

.notification-add-button {
  margin-top: 12px;
}
</style>

<style>
/* 通知组件通用样式 - 非scoped以便子组件使用 */
.notification-input-width {
  width: 160px !important;
}

.notification-flex-between {
  justify-content: space-between;
  width: 100%;
}

.notification-margin-top-right {
  margin-top: 4px;
  margin-right: 4px;
}

.notification-margin-top-center {
  margin-top: 4px;
  align-items: center;
}

.notification-margin-left {
  margin-left: 4px;
}

.notification-flex-end {
  display: flex;
  justify-content: end;
}
</style>
