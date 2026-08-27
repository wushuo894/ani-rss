<template>
  <ImportAniView ref="importAniRef" @callback="getList"/>
  <DelAniView ref="delAniRef" @callback="reloadAllLists"/>
  <el-dialog v-model="dialogVisible" center title="管理">
    <div class="manage-content" v-loading="loading">
      <div class="manage-header">
        <div class="manage-toolbar">
          <div class="manage-search">
            <el-input
                v-model="text"
                clearable
                placeholder="搜索"
                prefix-icon="Search"
            />
          </div>
          <div class="select-width">
            <el-select
                v-model="releaseDate"
                clearable>
              <el-option v-for="it in releaseDateList"
                         :key="it" :label="it" :value="it"
              />
            </el-select>
          </div>
          <div class="select-width">
            <el-select v-model="selectedFilter">
              <el-option v-for="filter in selectFilters"
                         :key="filter.label"
                         :label="filter.label"
                         :value="filter.label"/>
            </el-select>
          </div>
        </div>
        <div class="manage-header-actions">
          <el-dropdown trigger="click">
            <el-button aria-label="批量操作" icon="MoreFilled" title="批量操作"/>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :disabled="!hasSelection" @click="updateTotalEpisodeNumber(false)">
                  <el-text>
                    <el-icon>
                      <RefreshRight/>
                    </el-icon>
                    更新总集数
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item :disabled="!hasSelection" @click="updateTotalEpisodeNumber(true)">
                  <el-text type="warning">
                    <el-icon>
                      <Refresh/>
                    </el-icon>
                    更新总集数 [F]
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item :disabled="!hasSelection" divided @click="batchScrape(false)">
                  <el-text>
                    <el-icon>
                      <RefreshRight/>
                    </el-icon>
                    刮削
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item :disabled="!hasSelection" @click="batchScrape(true)">
                  <el-text type="warning">
                    <el-icon>
                      <Refresh/>
                    </el-icon>
                    刮削 [F]
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item :disabled="!hasSelection" divided @click="batchEnable(true)">
                  <el-text type="primary">
                    <el-icon>
                      <CircleCheck/>
                    </el-icon>
                    启用
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item :disabled="!hasSelection" @click="batchEnable(false)">
                  <el-text type="warning">
                    <el-icon>
                      <CircleClose/>
                    </el-icon>
                    禁用
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item divided @click="importAniRef?.show">
                  <el-text>
                    <el-icon>
                      <Download/>
                    </el-icon>
                    导入
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item :disabled="!hasSelection" @click="exportData">
                  <el-text>
                    <el-icon>
                      <Upload/>
                    </el-icon>
                    导出
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item :disabled="!hasSelection" divided @click="showDeleteDialog">
                  <el-text type="danger">
                    <el-icon>
                      <Remove/>
                    </el-icon>
                    删除
                  </el-text>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <el-table
          ref="tableRef"
          :data="searchList"
          height="400px"
          row-key="id"
          size="small"
          stripe
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" fixed/>
        <el-table-column label="标题" width="200" fixed>
          <template #default="{row}">
            <el-text :line-clamp="2" size="small">
              {{ row.title }}
            </el-text>
          </template>
        </el-table-column>
        <el-table-column label="季" prop="season" width="50"/>
        <el-table-column label="字幕组" width="100">
          <template #default="{row}">
            <el-text size="small" truncated>
              {{ row.subgroup }}
            </el-text>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{row}">
            <el-tag v-if="row.enable">
              已启用
            </el-tag>
            <el-tag v-else type="info">
              未启用
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="100">
          <template #default="{row}">
            <el-tag type="warning">
              {{ row.currentEpisodeNumber }} /
              {{ row.totalEpisodeNumber || '*' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{row}">
            <el-tag type="danger">{{ row.ova ? 'ova' : 'tv' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="URL" width="300">
          <template #default="{row}">
            <el-text :line-clamp="2" size="small">
              {{ row.url }}
            </el-text>
          </template>
        </el-table-column>
      </el-table>
      <div>
        <p class="manage-count">共 {{ searchList.length }} 项</p>
      </div>
    </div>
  </el-dialog>
</template>
<script setup>
import {computed, nextTick, ref, watch} from "vue";
import {ElMessage, ElText} from "element-plus";
import DelAniView from "./DelAniView.vue";
import ImportAniView from "@/view/home/ImportAniView.vue";
import {CircleCheck, CircleClose, Download, Refresh, RefreshRight, Remove, Upload} from "@element-plus/icons-vue";
import * as http from "@/js/http.js";

const selectFilters = [
  {
    label: '全部',
    predicate: () => true
  },
  {
    label: '已启用',
    predicate: item => item.enable
  },
  {
    label: '未启用',
    predicate: item => !item.enable
  }
]

const dialogVisible = ref(false)
const loading = ref(false)
const releaseDateList = ref([])
const releaseDate = ref('')
const selectedFilter = ref(selectFilters[0].label)
const selectedItems = ref([])
const list = ref([])
const text = ref('')
const delAniRef = ref()
const importAniRef = ref()
const tableRef = ref()

const activeFilter = computed(() =>
    selectFilters.find(filter => filter.label === selectedFilter.value) ?? selectFilters[0]
)
const normalizedSearchText = computed(() => text.value.trim().toLowerCase())
const searchList = computed(() => list.value.filter(item => {
  const query = normalizedSearchText.value
  if (query) {
    const matchesText = [item.title, item.pinyin, item.pinyinInitials]
        .some(value => String(value ?? '').toLowerCase().includes(query))
    if (!matchesText) {
      return false
    }
  }

  if (releaseDate.value && releaseDate.value !== String(item.releaseDate ?? '').replace(/-\d{2}$/, '')) {
    return false
  }

  return activeFilter.value.predicate(item)
}))
const selectedIds = computed(() => selectedItems.value.map(item => item.id))
const hasSelection = computed(() => selectedIds.value.length > 0)

const clearSelection = () => {
  selectedItems.value = []
  tableRef.value?.clearSelection()
}

watch([text, releaseDate, selectedFilter], clearSelection)

const fetchList = async () => {
  const res = await http.listAni()
  const data = res.data ?? {}
  releaseDateList.value = data.releaseDateList ?? []
  list.value = (data.weekList ?? []).flatMap(week => week.items ?? [])
  await nextTick()
  clearSelection()
}

const getList = async () => {
  loading.value = true
  try {
    await fetchList()
  } finally {
    loading.value = false
  }
}

const reloadAllLists = async () => {
  await getList()
  window.$reLoadList?.()
}

const show = () => {
  releaseDate.value = ''
  selectedFilter.value = selectFilters[0].label
  text.value = ''
  clearSelection()
  dialogVisible.value = true
  getList()
}

const handleSelectionChange = value => {
  selectedItems.value = value
}

const requireSelection = () => {
  if (!hasSelection.value) {
    ElMessage.error('未选择订阅')
    return false
  }
  return true
}

const runSelectedAction = async (request, {reload = false} = {}) => {
  if (!requireSelection()) {
    return
  }

  loading.value = true
  try {
    const res = await request([...selectedIds.value])
    ElMessage.success(res.message)
    if (reload) {
      await fetchList()
      window.$reLoadList?.()
    }
  } finally {
    loading.value = false
  }
}

const exportData = () => {
  if (!requireSelection()) {
    return
  }

  const textContent = JSON.stringify(selectedItems.value)
  const blob = new Blob([textContent], {type: 'application/json'})
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a')
  anchor.style.display = 'none'
  anchor.href = url
  anchor.download = 'ani.v2.json'
  document.body.appendChild(anchor)
  try {
    anchor.click()
  } finally {
    document.body.removeChild(anchor)
    URL.revokeObjectURL(url)
  }
}

const showDeleteDialog = () => {
  if (requireSelection()) {
    delAniRef.value?.show(selectedItems.value)
  }
}

const batchEnable = value => {
  return runSelectedAction(ids => http.batchEnable(value, ids), {reload: true})
}

const updateTotalEpisodeNumber = force => {
  return runSelectedAction(ids => http.updateTotalEpisodeNumber(force, ids), {reload: true})
}

const batchScrape = force => {
  return runSelectedAction(ids => http.batchScrape(force, ids))
}

defineExpose({show})
</script>

<style scoped>
.manage-content {
  min-height: 300px;
}

.manage-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  margin-bottom: 8px;
}

.manage-toolbar {
  min-width: 0;
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px;
}

.manage-search {
  width: 180px;
}

.manage-header-actions {
  flex-shrink: 0;
}

.select-width {
  width: 120px;
}

.manage-count {
  margin: 6px;
  text-align: end;
}

@media (max-width: 560px) {
  .manage-search {
    flex: 1 1 180px;
  }

  .select-width {
    flex: 1 1 120px;
  }
}

</style>
