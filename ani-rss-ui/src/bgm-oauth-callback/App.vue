<template>
  <div class="page">
    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-header">
          <span>授权结果</span>
        </div>
      </template>
      <div>
        <div v-if="me">
          <el-descriptions direction="vertical" border>
            <el-descriptions-item :rowspan="2" :width="140" label="头像" align="center">
              <el-avatar :src="me?.avatar?.large"/>
            </el-descriptions-item>
            <el-descriptions-item label="用户名">
              <el-text v-if="me.username">
                {{ me.username }}
              </el-text>
              <el-text v-else>
                {{ me.id }}
              </el-text>
            </el-descriptions-item>
            <el-descriptions-item label="主页">
              {{ me.url }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ me.email }}
            </el-descriptions-item>
            <el-descriptions-item label="注册日期">
              <el-text>
                {{ me.regTime }}
              </el-text>
            </el-descriptions-item>
            <el-descriptions-item label="授权剩余过期时间">
              <el-tag type="success" v-if="me.expiresDays > 3">
                {{ me.expiresDays }} 天
              </el-tag>
              <el-tag type="danger" v-else>
                {{ me.expiresDays }} 天
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="签名">
              <el-text v-if="me.sign">
                {{ me.sign }}
              </el-text>
              <el-text v-else>
                无
              </el-text>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <el-alert
            class="result"
            :title="text"
            :type="type"
            v-loading.fullscreen.lock="loading"
            :closable="false"
        />
      </div>
      <template #footer>
        <div class="footer">
          <el-button bg text @click="close">关闭</el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {init} from "@/js/global.js";
import api from "@/js/api.js";
import * as http from "@/js/http.js";

const type = ref('success')
const text = ref('')
const loading = ref(false)
const me = ref(null)

const close = () => {
  window.close()
}

const loadMe = async () => {
  return http.meBgm()
      .then(res => {
        me.value = res.data
      });
}

const load = async (code) => {
  loading.value = true
  api.post(`api/bgm/oauth/callback?code=${code}`)
      .then(async res => {
        let {code, message} = res
        type.value = code === 200 ? 'success' : 'error'

        if (code === 200) {
          await loadMe()
        }

        text.value = message
      })
      .finally(() => {
        loading.value = false
      })
}

onMounted(() => {
  const url = new URL(location.href)
  const code = url.searchParams.get('code')
  if (!code) {
    type.value = 'error'
    text.value = 'code 为空'
    return
  }
  load(code)
})

init()
</script>

<style scoped>
:global(body) {
  margin: 0;
  padding: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}

.page {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
}

.card {
  min-width: 480px;
}

.result {
  margin-top: 8px;
}

.footer {
  display: flex;
  justify-content: flex-end;
}
</style>
