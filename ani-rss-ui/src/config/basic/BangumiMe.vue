<template>
  <el-dialog v-model="dialogVisible" align-center center class="dialog-max-width" title="查看授权状态">
    <div class="content" v-loading="loading">
      <el-text v-if="errorMessage" type="danger">{{ errorMessage }}</el-text>
      <el-scrollbar v-if="me.username">
        <el-descriptions
            direction="vertical"
            border
        >
          <el-descriptions-item
              :rowspan="2"
              :width="140"
              label="头像"
              align="center"
          >
            <el-avatar :src="me['avatar']['large']" size="large"/>
          </el-descriptions-item>
          <el-descriptions-item label="用户名">
            <el-text v-if="me['username']">
              {{ me['username'] }}
            </el-text>
            <el-text v-else>
              {{ me['id'] }}
            </el-text>
          </el-descriptions-item>
          <el-descriptions-item label="主页">
            <el-link
                type="primary"
                class="text-extra-small"
                :href="me['url']"
                target="_blank">
              {{ me['url'] }}
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">
            {{ me['email'] }}
          </el-descriptions-item>
          <el-descriptions-item label="注册日期">
            <el-text>
              {{ me['regTime'] }}
            </el-text>
          </el-descriptions-item>
          <el-descriptions-item label="授权剩余过期时间">
            <el-tag type="success" v-if="me['expiresDays'] > 3">
              {{ me['expiresDays'] }} 天
            </el-tag>
            <el-tag type="danger" v-else>
              {{ me['expiresDays'] }} 天
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="签名">
            <el-text v-if="me['sign']">
              {{ me['sign'] }}
            </el-text>
            <el-text v-else>
              无
            </el-text>
          </el-descriptions-item>
        </el-descriptions>
      </el-scrollbar>
    </div>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import api from "@/js/api.js";

let me = ref({
  "avatar": {
    "large": "https://lain.bgm.tv/pic/user/l/icon.jpg",
    "medium": "https://lain.bgm.tv/r/200/pic/user/l/icon.jpg",
    "small": "https://lain.bgm.tv/r/100/pic/user/l/icon.jpg"
  },
  "sign": "",
  "url": "https://bgm.tv/user/wushuo",
  "username": "",
  "nickname": "",
  "id": 0,
  "userGroup": 10,
  "regTime": "2022-05-27T15:35:45+08:00",
  "email": "xxxx@xx.com",
  "timeOffset": 8,
  "expiresDays": 6
})

let errorMessage = ref('')

let dialogVisible = ref(false);
let loading = ref(false);

let show = () => {
  errorMessage.value = '';
  dialogVisible.value = true;
  loading.value = true;
  api.post('api/meBgm')
      .then(res => {
        me.value = res.data;
      })
      .catch(err => {
        errorMessage.value = err.message
      })
      .finally(() => {
        loading.value = false;
      })
}

defineExpose({show})
</script>

<style scoped>
.dialog-max-width {
  max-width: 600px;
}

.text-extra-small {
  font-size: var(--el-font-size-extra-small);
}

.content {
  width: 100%;
  min-height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
