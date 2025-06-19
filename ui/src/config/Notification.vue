<template>
  <el-collapse>
    <el-collapse-item title="通知模板">
      <el-input v-model="props.config['notificationTemplate']" :autosize="{ minRows: 2}"
                placeholder="${text}" type="textarea"/>
      <div class="flex" style="width: 100%;justify-content: end;">
        <a href="https://docs.wushuo.top/config/message" target="_blank">通知模版示例</a>
      </div>
    </el-collapse-item>
  </el-collapse>
  <div style="margin-top: 8px;">
    <div>
      <el-space wrap class="flex flex-wrap gap-4" size="small">
        <el-card v-for="it in props.config['notificationConfigList']" shadow="never" style="min-width: 180px">
          <div style="align-items: center;justify-content: space-between;" class="flex">
            <div>
              <p>
                {{ getLabel(it['notificationType']) }}
              </p>
              <el-text size="small">
                {{ it['comment'] ? it['comment'] : '无备注' }}
              </el-text>
            </div>
            <div>
              <el-dropdown trigger="click">
                <el-button circle icon="MoreFilled" size="large" text type="primary"/>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="notificationConfigRef?.show(it)">
                      编辑
                    </el-dropdown-item>
                    <el-dropdown-item @click="del(it)">
                      删除
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
        style="margin-top: 12px;"
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

import {getLabel} from "../js/notification-type.js";
import api from "../js/api.js";

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

<style>
@media (min-width: 1000px) {
  .auto {
    display: flex;
  }
}
</style>
