<template>
  <el-dialog v-model="dialogVisible" center title="下载">
    <div class="torrents-container">
      <div class="torrents-header">
        <el-radio-group v-model="sortType">
          <el-radio-button
              v-for="item in sortTypeList"
              :value="item.value"
              @click="changeSort(item.value)">
            <div class="flex-center">
              {{ item.label }}
              <el-icon
                  v-if="sortType === item.value"
                  class="el-icon--right"
              >
                <Top v-if="sortOrder === 'asc'"/>
                <Bottom v-else/>
              </el-icon>
            </div>
          </el-radio-button>
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
                  {{ torrentsInfo['formatSize'] }}
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
import * as http from "@/js/http.js";
import {Bottom, Top} from "@element-plus/icons-vue";

// 记录排序方式
let sortType = ref('name')
// 记录排序顺序 asc=正序, desc=倒序
let sortOrder = ref('asc')

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
  if (sortType.value === type) {
    // 相同排序方式，切换正序/倒序
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortType.value = type
    sortOrder.value = 'asc'
  }
  torrentsInfos.value = sortInfos(torrentsInfos.value)
}

let sortInfos = (infos) => {
  for (let sortTypeItem of sortTypeList) {
    let {value, fun} = sortTypeItem;
    if (value !== sortType.value) {
      continue
    }
    let sorted = fun(infos)
    return sortOrder.value === 'asc' ? sorted : sorted.reverse()
  }
  return infos;
}

let getTorrentsInfos = async () => {
  while (dialogVisible.value) {
    try {
      let res = await http.torrentsInfos()
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
