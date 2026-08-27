<template>
  <el-dialog
      v-model="dialogVisible"
      center
      class="el-dialog-auto-width"
      title="预览"
      @closed="resetPreview">
    <div class="items-content" v-loading="loading">
      <div class="items-select-container">
        <el-select v-model="selectedFilter" class="items-select" @change="clearSelection">
          <el-option
              v-for="filter in filters"
              :key="filter.label"
              :label="filter.label"
              :value="filter.label"/>
        </el-select>
        <el-input v-model="previewData.downloadPath" readonly/>
      </div>
      <div class="items-button-container">
        <el-button :disabled="!selectedItems.length" icon="Check" type="primary" @click="allowDownload">允许下载
        </el-button>
        <el-button :disabled="!selectedItems.length" icon="Close" @click="forbidDownload">禁止下载</el-button>
        <PopconfirmView :title="deleteConfirmTitle" @confirm="deleteTorrentCache">
          <template #reference>
            <el-button
                :disabled="!selectedDownloadedItems.length"
                :loading="deleteLoading"
                icon="Remove"
                type="danger">
              删除种子
            </el-button>
          </template>
        </PopconfirmView>
      </div>
      <div class="items-table-container">
        <el-table
            ref="tableRef"
            :data="visibleItems"
            height="500"
            scrollbar-always-on
            size="small"
            stripe
            @selection-change="selectedItems = $event">
          <el-table-column type="selection" width="55" fixed/>
          <el-table-column label="是否下载" min-width="100">
            <template #default="{row}">
              <el-tag v-if="notDownloadEpisodes.has(row.episode)" type="info">否</el-tag>
              <el-tag v-else>是</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="已下载" min-width="100">
            <template #default="{row}">
              <el-tag v-if="!row.hasDownloaded" type="info">否</el-tag>
              <el-tag v-else>是</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="主RSS" min-width="80">
            <template #default="{row}">
              <el-tag v-if="!row.master" type="info">否</el-tag>
              <el-tag v-else>是</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="字幕组" min-width="100">
            <template #default="{row}">
              <el-text size="small" truncated>
                {{ row.subgroup }}
              </el-text>
            </template>
          </el-table-column>
          <el-table-column label="标题" min-width="400">
            <template #default="{row}">
              <el-text size="small">
                {{ row.title }}
              </el-text>
            </template>
          </el-table-column>
          <el-table-column label="重命名" min-width="280">
            <template #default="{row}">
              <el-text size="small">
                {{ row.reName }}
              </el-text>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" min-width="120">
            <template #default="{row}">
              <el-text size="small">
                {{ row.pubDate }}
              </el-text>
            </template>
          </el-table-column>
          <el-table-column label="InfoHash" min-width="200">
            <template #default="{row}">
              <el-text size="small">
                {{ row.infoHash }}
              </el-text>
            </template>
          </el-table-column>
          <el-table-column prop="formatSize" label="大小" width="120"/>
          <el-table-column label="种子" width="90">
            <template #default="{row}">
              <el-button bg size="small" text @click="copyTorrent(row.torrent)">复制</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-alert
            v-if="omitAlertTitle"
            :closable="false"
            :title="omitAlertTitle"
            show-icon
            type="warning"/>
      </div>
    </div>
    <div class="flex items-footer">
      <span>共 {{ visibleItems.length }} 项</span>
      <el-button bg icon="Close" text @click="dialogVisible = false">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import {computed, ref} from "vue";
import {ElMessage} from "element-plus";
import PopconfirmView from "@/view/custom/PopconfirmView.vue";
import * as http from "@/js/http.js";

const props = defineProps({
  ani: {
    type: Object,
    required: true
  }
})

const filters = [
  {
    label: '全部',
    predicate: () => true
  },
  {
    label: '已下载',
    predicate: item => item.hasDownloaded
  },
  {
    label: '未下载',
    predicate: item => !item.hasDownloaded
  }
]

const createEmptyPreview = () => ({
  downloadPath: '',
  items: [],
  omitList: []
})

const dialogVisible = ref(false)
const loading = ref(false)
const deleteLoading = ref(false)
const selectedFilter = ref(filters[0].label)
const selectedItems = ref([])
const previewData = ref(createEmptyPreview())
const tableRef = ref()
let loadVersion = 0

const activeFilter = computed(() =>
    filters.find(filter => filter.label === selectedFilter.value) ?? filters[0]
)
const visibleItems = computed(() => previewData.value.items.filter(activeFilter.value.predicate))
const selectedDownloadedItems = computed(() => selectedItems.value.filter(item => item.hasDownloaded))
const notDownloadEpisodes = computed(() => new Set(props.ani.notDownload ?? []))
const deleteConfirmTitle = computed(() => `删除${selectedDownloadedItems.value.length}个种子缓存?`)
const omitAlertTitle = computed(() => {
  const omitList = previewData.value.omitList
  return omitList.length ? `缺少集数: ${omitList.slice(0, 10).join('、')}` : ''
})

const fallbackCopy = value => {
  const input = document.createElement('input');
  input.value = value
  document.body.appendChild(input);
  input.select();
  const copied = document.execCommand('copy');
  document.body.removeChild(input);
  return copied
}

const copyTorrent = async value => {
  let copied = false
  try {
    if (!navigator.clipboard?.writeText) {
      throw new Error('Clipboard API is unavailable')
    }
    await navigator.clipboard.writeText(value)
    copied = true
  } catch {
    copied = fallbackCopy(value)
  }

  if (copied) {
    ElMessage.success('已复制')
  } else {
    ElMessage.error('复制失败')
  }
}

const clearSelection = () => {
  selectedItems.value = []
  tableRef.value?.clearSelection()
}

const resetPreview = () => {
  loadVersion++
  selectedFilter.value = filters[0].label
  previewData.value = createEmptyPreview()
  loading.value = false
  deleteLoading.value = false
  clearSelection()
}

const loadPreview = async () => {
  const currentVersion = ++loadVersion
  loading.value = true
  clearSelection()
  try {
    const res = await http.previewAni(props.ani)
    if (currentVersion !== loadVersion) {
      return
    }
    const data = res.data ?? {}
    previewData.value = {
      ...createEmptyPreview(),
      ...data,
      items: data.items ?? [],
      omitList: data.omitList ?? []
    }
  } finally {
    if (currentVersion === loadVersion) {
      loading.value = false
    }
  }
}

const show = () => {
  resetPreview()
  dialogVisible.value = true
  loadPreview()
}

const deleteTorrentCache = async () => {
  const infoHash = selectedDownloadedItems.value
      .map(item => item.infoHash)
      .filter(Boolean)
      .join(',')
  if (!infoHash) {
    return
  }

  deleteLoading.value = true
  try {
    const res = await http.deleteTorrent(props.ani.id, infoHash)
    ElMessage.success(res.message)
    if (dialogVisible.value) {
      await loadPreview()
    }
  } finally {
    deleteLoading.value = false
  }
}

const forbidDownload = () => {
  const episodes = new Set(props.ani.notDownload ?? [])
  selectedItems.value.forEach(item => episodes.add(item.episode))
  props.ani.notDownload = Array.from(episodes)
}

const allowDownload = () => {
  const selectedEpisodes = new Set(selectedItems.value.map(item => item.episode))
  props.ani.notDownload = (props.ani.notDownload ?? [])
      .filter(episode => !selectedEpisodes.has(episode))
}

defineExpose({show})
</script>

<style scoped>
.items-content {
  width: 100%;
}

.items-select-container {
  margin: 4px 0;
  display: flex;
  gap: 4px;
}

.items-select {
  flex: 0 0 120px;
}

.items-select-container :deep(.el-input) {
  min-width: 0;
}

.items-button-container {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
  margin-bottom: 8px;
}

.items-button-container :deep(.el-button + .el-button) {
  margin-left: 0;
}

.items-table-container {
  padding: 0 12px;
}

.items-footer {
  margin-top: 12px;
  justify-content: space-between;
  align-items: center;
}
</style>
