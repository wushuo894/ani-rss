<template>
  <el-dialog v-model="dialogVisible" center title="下载">
    <div class="torrents-container">
      <div class="torrents-header">
        <el-radio-group v-model="sortType" @change="changeSort">
          <el-radio-button v-for="sortType in sortTypeList" :value="sortType.value" :label="sortType.label"/>
        </el-radio-group>
      </div>
      <el-empty v-if="!torrentsInfos.length" description="当前无下载任务" class="torrents-empty"/>
      <el-scrollbar v-else class="torrents-scrollbar">
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

// 记录排序方式
let sortType = ref('name')

let sortTypeList = [
  {
    label: "按名称排序",
    value: "name",
    fun: (value) => {
      return value.sort((a, b) => a.name.localeCompare(b.name));
    }
  },
  {
    label: "按进度排序",
    value: "progress",
    fun: (value) => {
      return value.sort((a, b) => b.progress - a.progress);
    }
  }
]

let dialogVisible = ref(false)

let show = () => {
  dialogVisible.value = true
  getTorrentsInfos()
}

let torrentsInfos = ref([])

let changeSort = (type) => {
  sortType.value = type
  torrentsInfos.value = sortInfos(torrentsInfos.value)
}

let sortInfos = (infos) => {
  for (let sortTypeItem of sortTypeList) {
    let {value, fun} = sortTypeItem;
    if (value !== sortType.value) {
      continue
    }
    return fun(infos)
  }
  return infos;
}

let getTorrentsInfos = async () => {
  while (dialogVisible.value) {
    try {
      let res = await api.get('api/torrentsInfos')
      let infos = await res.data
      torrentsInfos.value = sortInfos(infos)
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
  display: flex;
  flex-direction: column;
}

.torrents-header {
  text-align: center;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.torrents-scrollbar {
  flex: 1;
  overflow: hidden;
}

.torrents-empty {
  flex: 1;
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
