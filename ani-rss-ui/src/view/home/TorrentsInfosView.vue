<template>
  <PageHeaderView title="下载" :subtitle="`共 ${torrentsInfos.length} 个任务`"/>
  <div class="torrents-page app-page-padding">
    <div class="torrents-container">
      <div class="torrents-toolbar">
        <el-tabs v-model="activeTab" class="torrents-tabs">
          <el-tab-pane name="downloading">
            <template #label>
              <span class="tab-label">下载中</span>
              <el-tag size="small" type="primary">{{ downloadingInfos.length }}</el-tag>
            </template>
          </el-tab-pane>
          <el-tab-pane name="completed">
            <template #label>
              <span class="tab-label">已完成</span>
              <el-tag size="small" type="success">{{ completedInfos.length }}</el-tag>
            </template>
          </el-tab-pane>
        </el-tabs>
        <div class="sort-actions">
          <el-dropdown trigger="click" @command="changeSortType">
            <el-button class="sort-field-button" bg text>
              <el-icon>
                <Sort/>
              </el-icon>
              <span>{{ currentSortLabel }}</span>
              <el-icon class="sort-field-arrow">
                <ArrowDown/>
              </el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                    v-for="item in sortTypeList"
                    :key="item.value"
                    :command="item.value">
                  <span class="sort-option-label">{{ item.label }}</span>
                  <el-icon v-if="sortType === item.value" class="el-icon--right">
                    <Check/>
                  </el-icon>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-tooltip :content="sortOrder === 'asc' ? '正序' : '倒序'" placement="top">
            <el-button
                :aria-label="sortOrder === 'asc' ? '正序' : '倒序'"
                class="sort-order-button"
                bg
                text
                @click="toggleSortOrder">
              <el-icon>
                <SortUp v-if="sortOrder === 'asc'"/>
                <SortDown v-else/>
              </el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>
      <el-empty v-if="!activeTorrentsInfos.length" :description="emptyDescription" class="torrents-empty"/>
      <el-scrollbar v-else class="torrents-scrollbar">
        <el-card v-for="torrentsInfo in activeTorrentsInfos"
                 :key="torrentsInfo.hash || torrentsInfo.id || torrentsInfo.name"
                 shadow="never"
                 class="torrents-card">
          <p>{{ torrentsInfo.name }}</p>
          <el-progress :percentage="torrentsInfo['progress']"/>
          <div class="torrents-size-info">
            <span>
              <span class="torrents-size-value">{{ formatTorrentSize(torrentsInfo['completed']) }}</span>
              /
              <span class="torrents-size-value">{{ formatTorrentSize(torrentsInfo['size']) }}</span>
            </span>
          </div>
          <template #footer>
            <div class="flex torrents-footer">
              <div>
                <el-tag v-for="tag in torrentsInfo['tagList']" class="torrents-tag-spacer" type="info">
                  {{ tag }}
                </el-tag>
              </div>
              <div>
                <el-tag class="torrents-tag-spacer" type="primary">
                  {{ torrentsInfo['state'] }}
                </el-tag>
              </div>
            </div>
          </template>
        </el-card>
      </el-scrollbar>
    </div>
  </div>
</template>

<script setup>
import {computed, onActivated, onDeactivated, onUnmounted, ref} from "vue";
import * as http from "@/js/http.js";
import {ArrowDown, Check, Sort, SortDown, SortUp} from "@element-plus/icons-vue";
import {formatSize} from "@/js/format.js";
import PageHeaderView from "@/view/custom/PageHeaderView.vue";

const activeTab = ref('downloading')
// 记录排序方式
let sortType = ref('name')
// 记录排序顺序 asc=正序, desc=倒序
let sortOrder = ref('asc')

let sortTypeList = [
  {
    label: "名称",
    value: "name",
    fun: (value) => {
      return value.sort((a, b) => a.name.localeCompare(b.name));
    }
  },
  {
    label: "进度",
    value: "progress",
    fun: (value) => {
      return value.sort((a, b) => b.progress - a.progress);
    }
  }
]

let polling = false
let stopped = false

let torrentsInfos = ref([])

const completedInfos = computed(() => torrentsInfos.value.filter(isCompleted))
const downloadingInfos = computed(() => torrentsInfos.value.filter(item => !isCompleted(item)))
const activeTorrentsInfos = computed(() => activeTab.value === 'completed' ? completedInfos.value : downloadingInfos.value)
const emptyDescription = computed(() => activeTab.value === 'completed' ? '当前无已完成任务' : '当前无下载中任务')
const currentSortLabel = computed(() => sortTypeList.find(item => item.value === sortType.value)?.label || '名称')

const isCompleted = item => {
  return item.state === 'stoppedUP'
}

const formatTorrentSize = bytes => {
  const size = Number(bytes)
  if (!Number.isFinite(size) || size < 0) {
    return '-'
  }
  return size === 0 ? '0 B' : formatSize(size)
}

const resort = () => {
  torrentsInfos.value = sortInfos(torrentsInfos.value)
}

let changeSortType = type => {
  if (sortType.value === type) {
    return
  }
  sortType.value = type
  sortOrder.value = 'asc'
  resort()
}

let toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  resort()
}

let sortInfos = (infos) => {
  for (let sortTypeItem of sortTypeList) {
    let {value, fun} = sortTypeItem;
    if (value !== sortType.value) {
      continue
    }
    let sorted = fun([...infos])
    return sortOrder.value === 'asc' ? sorted : sorted.reverse()
  }
  return infos;
}

let startPolling = async () => {
  if (polling) {
    return
  }
  polling = true
  while (!stopped) {
    try {
      let res = await http.torrentsInfos()
      let infos = await res.data
      torrentsInfos.value = sortInfos(infos)
    } catch (_) {
    }
    await sleep(3000)
  }
  polling = false
}

let sleep = ms => {
  return new Promise(resolve => setTimeout(resolve, ms));
}

const resumePolling = () => {
  stopped = false
  startPolling()
}

const pausePolling = () => {
  stopped = true
}

onActivated(resumePolling)
onDeactivated(pausePolling)
onUnmounted(pausePolling)
</script>

<style scoped>
.torrents-page {
  height: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.torrents-container {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.torrents-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px 10px;
  flex-shrink: 0;
}

.torrents-tabs {
  flex: 1;
  min-width: 0;
}

.torrents-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.torrents-tabs :deep(.el-tabs__nav-wrap:after) {
  height: 0;
}

.torrents-tabs :deep(.el-tabs__item) {
  height: 36px;
  font-weight: 600;
}

.tab-label {
  margin-right: 6px;
  flex-shrink: 0;
}

.sort-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.sort-actions .el-button {
  margin-left: 0;
}

.sort-field-button {
  min-width: 82px;
  padding: 0 9px;
}

.sort-field-arrow {
  margin-left: 6px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.sort-order-button {
  width: 30px;
  height: 30px;
  padding: 0;
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

.torrents-size-info {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin-top: 6px;
  font-size: 13px;
  line-height: 20px;
  font-variant-numeric: tabular-nums;
}

.torrents-size-label {
  margin-right: 4px;
  color: var(--el-text-color-placeholder);
}

.torrents-size-value {
  color: var(--el-text-color-regular);
}

.torrents-footer {
  width: 100%;
  justify-content: space-between;
}

.torrents-tag-spacer {
  margin-top: 4px;
  margin-left: 4px;
}

@media (max-width: 700px) {
  .torrents-toolbar {
    align-items: flex-end;
    gap: 6px;
    padding: 0 0 8px;
  }

  .torrents-tabs :deep(.el-tabs__item) {
    padding: 0 10px;
  }
}
</style>
