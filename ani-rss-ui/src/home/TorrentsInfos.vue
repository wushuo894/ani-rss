<template>
  <el-dialog v-model="dialogVisible" center title="下载">
    <div class="torrents-container">
      <el-empty v-if="!torrentsInfos.length" description="当前无下载任务" class="torrents-empty"/>
      <el-scrollbar v-else>
        <el-card v-for="torrentsInfo in torrentsInfos"
                 shadow="never"
                 class="torrents-card">
          <p>{{ torrentsInfo.name }}</p>
          <el-progress :percentage="torrentsInfo['progress']"/>
          <template #footer>
            <div class="flex torrents-footer">
              <div>
                <el-tag v-for="tag in torrentsInfo['tags']" class="torrents-tag-spacer" type="info">
                  {{ tag }}
                </el-tag>
              </div>
              <div>
                <el-tag class="torrents-tag-right" type="success">
                  {{ torrentsInfo['sizeStr'] }}&nbsp;MB
                </el-tag>
                <el-tag type="primary">
                  {{ torrentsInfo['state'] }}
                </el-tag>
              </div>
            </div>
          </template>
        </el-card>
      </el-scrollbar>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import api from "@/js/api.js";

let dialogVisible = ref(false)

let show = () => {
  dialogVisible.value = true
  getTorrentsInfos()
}

let torrentsInfos = ref([])

let getTorrentsInfos = async () => {
  while (dialogVisible.value) {
    try {
      let res = await api.get('api/torrentsInfos')
      torrentsInfos.value = await res.data
    } catch (_) {
    }
    await sleep(3000)
  }
}

let sleep = ms => {
  return new Promise(resolve => setTimeout(resolve, ms));
}

defineExpose({show})
</script>

<style scoped>
.torrents-container {
  height: 500px;
}

.torrents-empty {
  height: 100%;
}

.torrents-card {
  margin-bottom: 4px;
}

.torrents-footer {
  width: 100%;
  justify-content: space-between;
}

.torrents-tag-spacer {
  margin-left: 4px;
}

.torrents-tag-right {
  margin-right: 4px;
}
</style>
