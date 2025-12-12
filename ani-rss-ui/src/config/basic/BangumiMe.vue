<template>
  <el-dialog v-model="dialogVisible" align-center center class="dialog-max-width" title="查看授权状态">
    <el-descriptions
        v-loading="loading"
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
          {{ me['reg_time'] }}
        </el-text>
      </el-descriptions-item>
      <el-descriptions-item label="授权剩余过期时间">
        <el-tag type="success" v-if="me['expires_days'] > 3">
          {{ me['expires_days'] }} 天
        </el-tag>
        <el-tag type="danger" v-else>
          {{ me['expires_days'] }} 天
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
  "username": "wushuo",
  "nickname": "wushuo",
  "id": 123456,
  "user_group": 10,
  "reg_time": "2022-05-27T15:35:45+08:00",
  "email": "xxxx@xx.com",
  "time_offset": 8,
  "expires_days": 6
})

let dialogVisible = ref(false);
let loading = ref(false);

let show = () => {
  dialogVisible.value = true;
  loading.value = true;
  api.get('api/bgm?type=me')
      .then(res => {
        me.value = res.data;
        const formatter = new Intl.DateTimeFormat('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
          hour12: false
        })
        me.value['reg_time'] = formatter.format(new Date(me.value['reg_time']))
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
</style>
