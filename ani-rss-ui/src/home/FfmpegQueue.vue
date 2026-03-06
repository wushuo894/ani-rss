<template>
  <el-dialog v-model="dialogVisible" center title="转码队列" width="520px" @close="onClose">
    <div v-loading="loading" class="ffmpeg-container">

      <!-- 正在转码的任务（可能多个） -->
      <template v-if="info.active && info.active.length">
        <el-text size="small" type="info">正在转码 ({{ info.active.length }})</el-text>
        <div v-for="task in info.active" :key="task.torrent" class="ffmpeg-active-item">
          <p class="ffmpeg-name">{{ task.torrent }}</p>
          <el-text v-if="task.file" size="small" type="info" class="ffmpeg-filename">
            {{ task.file }}
          </el-text>
          <el-progress
              :percentage="task.progress"
              :striped="true"
              :striped-flow="true"
              :duration="5"
              class="ffmpeg-progress"
          />
        </div>
      </template>
      <el-empty v-else description="暂无转码任务" class="ffmpeg-empty"/>

      <!-- 等待队列 -->
      <template v-if="info.queue && info.queue.length">
        <el-divider content-position="left">
          <el-text size="small">等待队列 ({{ info.queue.length }})</el-text>
        </el-divider>
        <el-scrollbar max-height="240px">
          <div v-for="(name, idx) in info.queue" :key="idx" class="ffmpeg-queue-item">
            <el-icon size="12" color="var(--el-color-info)">
              <Clock/>
            </el-icon>
            <el-text size="small" class="ffmpeg-queue-name">{{ name }}</el-text>
          </div>
        </el-scrollbar>
      </template>

    </div>
  </el-dialog>
</template>

<script setup>
import {onUnmounted, ref} from "vue";
import {Clock} from "@element-plus/icons-vue";
import * as http from "@/js/http.js";

const dialogVisible = ref(false)
const loading = ref(false)
const info = ref({active: [], queue: []})

let timer = null

const load = () => {
  http.ffmpegQueue()
      .then(res => {
        info.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}

const show = () => {
  if (timer) {
    clearInterval(timer)
  }
  dialogVisible.value = true
  loading.value = true
  load()
  timer = setInterval(load, 2000)
}

const onClose = () => {
  clearInterval(timer)
  timer = null
}

onUnmounted(() => {
  clearInterval(timer)
})

defineExpose({show})
</script>

<style scoped>
.ffmpeg-container {
  min-height: 150px;
}

.ffmpeg-active-item {
  margin: 8px 0 14px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}

.ffmpeg-name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 500;
  word-break: break-all;
}

.ffmpeg-filename {
  display: block;
  margin-bottom: 8px;
  word-break: break-all;
}

.ffmpeg-progress {
  margin-top: 4px;
}

.ffmpeg-empty {
  padding: 20px 0;
}

.ffmpeg-queue-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 6px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.ffmpeg-queue-item:last-child {
  border-bottom: none;
}

.ffmpeg-queue-name {
  word-break: break-all;
  line-height: 1.4;
}
</style>
