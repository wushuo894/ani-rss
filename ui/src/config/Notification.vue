<template>
  <div style="margin-bottom: 12px;margin-top: 4px;">
    <el-input v-model:model-value="props.config['notificationTemplate']" type="textarea"
              placeholder="${text}" :autosize="{ minRows: 2}"/>
    <div class="flex" style="width: 100%;justify-content: end;">
      <a target="_blank" href="https://docs.wushuo.top/config/message">通知模版示例</a>
    </div>
  </div>
  <div>
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
                <el-button bg icon="MoreFilled" text type="primary"/>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item icon="Edit" @click="notificationConfigRef?.show(it)">
                      编辑
                    </el-dropdown-item>
                    <el-dropdown-item icon="Delete" @click="del(it)">
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
