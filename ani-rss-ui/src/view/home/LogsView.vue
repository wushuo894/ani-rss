<template>
  <PageHeaderView title="日志" :subtitle="`共 ${logs.length} 条 · 显示 ${filteredLogs.length} 条`">
    <template #actions>
      <div class="log-actions">
        <el-tooltip content="下载日志" placement="bottom">
          <el-button @click="downloadLogs" bg text>
            <el-icon>
              <DownloadIcon/>
            </el-icon>
            <span class="action-label">下载</span>
          </el-button>
        </el-tooltip>
        <el-tooltip content="刷新日志" placement="bottom">
          <el-button :loading="getLogsLoading" @click="getLogs" bg text>
            <el-icon>
              <Refresh/>
            </el-icon>
            <span class="action-label">刷新</span>
          </el-button>
        </el-tooltip>
        <PopconfirmView title="清空当前日志?" @confirm="clearLogs">
          <template #reference>
            <el-button :loading="clearLoading" type="danger" bg text>
              <el-icon>
                <Delete/>
              </el-icon>
              <span class="action-label">清空</span>
            </el-button>
          </template>
        </PopconfirmView>
      </div>
    </template>
  </PageHeaderView>
  <div v-loading="loading" class="logs-page app-page-padding">
    <section class="logs-toolbar" aria-label="日志筛选">
      <el-input
          v-model="searchText"
          :prefix-icon="Search"
          class="log-search"
          clearable
          placeholder="搜索日志"/>
      <el-select
          v-model="selectLevels"
          class="level-select"
          clearable
          collapse-tags
          collapse-tags-tooltip
          :max-collapse-tags="1"
          multiple
          placeholder="日志等级">
        <el-option v-for="level in levels" :key="level" :label="level" :value="level">
          <div class="level-option">
            <span class="level-option-name">
              <span class="level-dot" :class="`is-${level.toLowerCase()}`"></span>
              {{ level }}
            </span>
            <span class="level-count">{{ levelCounts[level] }}</span>
          </div>
        </el-option>
      </el-select>
      <el-select
          v-model="selectLoggerNames"
          class="logger-select"
          clearable
          collapse-tags
          collapse-tags-tooltip
          multiple
          placeholder="来源">
        <el-option
            v-for="loggerName in loggerNames"
            :key="loggerName"
            :label="loggerName"
            :value="loggerName"/>
      </el-select>
    </section>

    <div class="log-viewer">
      <div class="log-columns" aria-hidden="true">
        <span>时间</span>
        <span>级别</span>
        <span>来源</span>
        <span>内容</span>
      </div>
      <el-scrollbar ref="scrollbarRef" class="logs-scrollbar" always>
        <div ref="innerRef" class="log-list">
          <el-empty v-if="!filteredLogs.length"
                    :description="logs.length ? '没有匹配的日志' : '暂无日志'"
                    :image-size="72"/>
          <div v-for="(entry, index) in filteredLogs"
               v-else
               :key="`${entry.timestamp}-${index}`"
               class="log-entry"
               :class="`level-${entry.level.toLowerCase()}`">
            <el-tooltip :content="entry.timestamp || '未知时间'" placement="top">
              <time class="log-time">{{ entry.time || '--:--:--' }}</time>
            </el-tooltip>
            <span class="log-level">{{ entry.level }}</span>
            <el-tooltip :content="entry.loggerName || '未知来源'" placement="top">
              <div class="log-source">
                <span class="logger-name">{{ entry.shortLoggerName }}</span>
                <span class="thread-name">{{ entry.threadName || '未知线程' }}</span>
              </div>
            </el-tooltip>
            <pre class="log-message">{{ entry.content }}</pre>
          </div>
        </div>
      </el-scrollbar>
    </div>
  </div>
</template>

<script setup>
import {computed, nextTick, onActivated, ref} from "vue";
import {Delete, Download as DownloadIcon, Refresh, Search} from "@element-plus/icons-vue";
import {authorization} from "@/js/global.js";
import PopconfirmView from "@/view/custom/PopconfirmView.vue";
import PageHeaderView from "@/view/custom/PageHeaderView.vue";
import * as http from "@/js/http.js";

const levels = ['DEBUG', 'INFO', 'WARN', 'ERROR']
const loading = ref(true)
const getLogsLoading = ref(false)
const clearLoading = ref(false)
const logs = ref([])
const searchText = ref('')
const selectLevels = ref([...levels])
const selectLoggerNames = ref([])
const scrollbarRef = ref()
const innerRef = ref()

const loggerNames = computed(() => Array.from(new Set(
    logs.value.map(item => item.loggerName).filter(Boolean)
)).sort())

const levelCounts = computed(() => Object.fromEntries(
    levels.map(level => [level, logs.value.filter(item => item.level === level).length])
))

const normalizedLogs = computed(() => logs.value.map(item => {
  const message = item.message || ''
  const timestamp = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/.test(message)
      ? message.slice(0, 19)
      : ''
  const delimiter = item.loggerName ? ` ${item.loggerName} - ` : ''
  const contentStart = delimiter ? message.indexOf(delimiter) : -1
  const content = contentStart >= 0
      ? message.slice(contentStart + delimiter.length)
      : message

  return {
    ...item,
    timestamp,
    time: timestamp.slice(11),
    content,
    shortLoggerName: item.loggerName?.split('.').pop() || '未知来源'
  }
}))

const filteredLogs = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()
  return normalizedLogs.value.filter(item => {
    if (selectLevels.value.length && !selectLevels.value.includes(item.level)) {
      return false
    }
    if (selectLoggerNames.value.length && !selectLoggerNames.value.includes(item.loggerName)) {
      return false
    }
    if (!keyword) {
      return true
    }
    return [item.message, item.loggerName, item.threadName, item.level]
        .filter(Boolean)
        .some(value => value.toLowerCase().includes(keyword))
  })
})

const scrollToBottom = async () => {
  await nextTick()
  scrollbarRef.value?.setScrollTop(innerRef.value?.scrollHeight || 0)
}

const getLogs = () => {
  getLogsLoading.value = true
  if (!logs.value.length) {
    loading.value = true
  }
  return http.logs()
      .then(async res => {
        logs.value = res.data || []
        selectLoggerNames.value = selectLoggerNames.value
            .filter(loggerName => loggerNames.value.includes(loggerName))
        await scrollToBottom()
      })
      .finally(() => {
        loading.value = false
        getLogsLoading.value = false
      })
}

const clearLogs = () => {
  clearLoading.value = true
  http.clearLogs()
      .then(() => {
        selectLoggerNames.value = []
        return getLogs()
      })
      .finally(() => {
        clearLoading.value = false
      })
}

const downloadLogs = () => {
  window.open(`api/downloadLogs?s=${authorization.value}`)
}

onActivated(getLogs)
</script>

<style scoped>
.logs-page {
  height: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.log-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-actions .el-button {
  margin-left: 0;
}

.logs-toolbar {
  flex-shrink: 0;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background-color: var(--el-bg-color);
  margin-bottom: 10px;
}

.log-search {
  flex: 1 1 220px;
  min-width: 160px;
}

.level-select {
  flex: 0 1 240px;
  min-width: 180px;
}

.level-option,
.level-option-name {
  display: flex;
  align-items: center;
}

.level-option {
  justify-content: space-between;
  gap: 16px;
}

.level-option-name {
  gap: 7px;
}

.level-dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  border-radius: 50%;
  background-color: var(--el-text-color-secondary);
}

.level-dot.is-info {
  background-color: var(--el-color-primary);
}

.level-dot.is-warn {
  background-color: var(--el-color-warning);
}

.level-dot.is-error {
  background-color: var(--el-color-danger);
}

.level-count {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.logger-select {
  flex: 0 1 280px;
  min-width: 180px;
}

.log-viewer {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background-color: var(--el-bg-color);
}

.log-columns,
.log-entry {
  display: grid;
  grid-template-columns: 82px 62px minmax(140px, 220px) minmax(0, 1fr);
  column-gap: 10px;
}

.log-columns {
  flex-shrink: 0;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-light);
  color: var(--el-text-color-secondary);
  background-color: var(--el-fill-color-extra-light);
  font-size: 12px;
}

.logs-scrollbar {
  flex: 1;
  min-height: 0;
}

.log-list {
  min-height: 100%;
}

.log-entry {
  align-items: start;
  padding: 8px 9px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  border-left: 3px solid transparent;
  transition: background-color 0.12s ease;
}

.log-entry:hover {
  background-color: var(--el-fill-color-light);
}

.log-entry.level-info {
  border-left-color: var(--el-color-primary-light-5);
}

.log-entry.level-warn {
  border-left-color: var(--el-color-warning);
}

.log-entry.level-error {
  border-left-color: var(--el-color-danger);
}

.log-time,
.log-level,
.logger-name,
.thread-name,
.log-message {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
}

.log-time {
  padding-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.log-level {
  width: fit-content;
  min-width: 46px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  background-color: var(--el-fill-color);
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
}

.level-info .log-level {
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.level-warn .log-level {
  color: var(--el-color-warning);
  background-color: var(--el-color-warning-light-9);
}

.level-error .log-level {
  color: var(--el-color-danger);
  background-color: var(--el-color-danger-light-9);
}

.log-source {
  min-width: 0;
  display: grid;
  gap: 2px;
  cursor: default;
}

.logger-name,
.thread-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logger-name {
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.thread-name {
  color: var(--el-text-color-placeholder);
  font-size: 10px;
}

.log-message {
  min-width: 0;
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 12px;
  line-height: 1.55;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

@media (max-width: 900px) {
  .logs-toolbar {
    flex-wrap: wrap;
  }

  .log-search {
    flex: 1 1 calc(50% - 4px);
  }

  .logger-select {
    flex: 1 1 calc(50% - 4px);
  }

}

@media (max-width: 700px) {
  .action-label {
    display: none;
  }

  .logs-toolbar {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }

  .log-search,
  .logger-select,
  .level-select {
    width: 100%;
    min-width: 0;
  }

  .log-columns {
    display: none;
  }

  .log-entry {
    grid-template-columns: 70px 52px minmax(0, 1fr);
    row-gap: 6px;
  }

  .log-message {
    grid-column: 1 / -1;
  }

  .thread-name {
    display: none;
  }
}
</style>
