<template>
  <el-dialog v-model="visible" title="批量添加订阅" width="860" align-center
             :close-on-click-modal="false" :close-on-press-escape="false">
    <el-tabs v-model="activeName" type="card" @tab-remove="closeItemById">
      <el-tab-pane v-for="item in entries" :key="item.id" :name="item.id" closable>
        <template #label>
          <span>{{ item.label }}</span>
          <el-tag v-if="item.status === 'loading'" size="small" type="info">加载中</el-tag>
          <el-tag v-else-if="item.status === 'success'" size="small" type="success">已加载</el-tag>
          <el-tag v-else-if="item.status === 'added'" size="small" type="success">已添加</el-tag>
          <el-tag v-else size="small" type="danger">失败</el-tag>
        </template>
        <div v-if="item.status === 'loading'" v-loading="true" class="batch-loading">正在加载订阅信息…</div>
        <el-alert v-else-if="item.status === 'error'" :title="item.error" type="error" show-icon :closable="false"/>
        <div v-else-if="item.ani" class="batch-editor">
          <AniView v-model:ani="item.ani" @callback="confirmItem(item, $event)"/>
        </div>
        <div v-if="item.status === 'error'" class="batch-actions">
          <el-button type="primary" :loading="item.status === 'loading'" @click="retry(item)">重试</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import {computed, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import * as http from '@/js/http.js'
import AniView from './AniView.vue'
import {aniData} from '@/js/ani.js'

const props = defineProps({
  modelValue: Boolean,
  items: {type: Array, default: () => []},
  type: {type: String, required: true}
})
const emit = defineEmits(['update:modelValue', 'done'])
const entries = ref([])
const activeName = ref('')
const visible = computed({get: () => props.modelValue, set: value => emit('update:modelValue', value)})

const makeAni = item => {
  const source = typeof item.source === 'string' ? JSON.parse(item.source) : item.source
  const ani = {...aniData, url: source.rss, type: props.type, season: 1, offset: 0, match: []}
  if (props.type === 'anime-garden') {
    ani.bgmUrl = `https://bgm.tv/subject/${source.bgmId}`
    ani.subgroup = source.name
  } else {
    ani.bgmUrl = source.bgmUrl || `https://bgm.tv/subject/${source.bgmId}`
    ani.subgroup = source.label || source.name
  }
  // 将同番剧的后续字幕组作为 DTO 的备用 RSS 一并提交给解析接口。
  if (item.sources.length > 1) {
    ani.standbyRssList = item.sources.slice(1).map(value => {
      const standby = typeof value === 'string' ? JSON.parse(value) : value
      return {label: standby.label || standby.name, url: standby.rss, offset: 0}
    })
  }
  return ani
}

// 加载完成后补全匹配规则，避免 rssToAni 的响应覆盖批量选择结果。
const appendMatchRules = (ani, item) => {
  ani.match = item.sources.flatMap(value => {
    const group = typeof value === 'string' ? JSON.parse(value) : value
    const subgroup = group.label || group.name
    return (group.match || []).map(rule => `{{${subgroup}}}:${rule}`)
  })
  return ani
}

// 单条串行解析，失败只影响当前 tab，后续任务继续执行。
const load = async item => {
  item.status = 'loading'
  item.error = ''
  try {
    const request = item.request || makeAni(item)
    const result = (await http.rssToAni(request)).data
    if (item.sources?.length) {
      item.ani = appendMatchRules(result, item)
    } else {
      // 手动添加沿用 AddView 中尚未参与解析的本地匹配配置。
      result.match = request.match || result.match || []
      result.showDownlaod = false
      item.ani = result
    }
    item.status = 'success'
  } catch (error) {
    item.status = 'error'
    item.error = error?.message || '订阅加载失败'
  }
}

const open = async (items = props.items) => {
  const grouped = new Map()
  items.forEach(value => {
    const source = typeof value === 'string' ? JSON.parse(value) : value
    const key = props.type === 'mikan' ? new URL(source.rss).searchParams.get('bangumiId') : source.bgmId
    if (!grouped.has(key)) grouped.set(key, [])
    grouped.get(key).push(value)
  })
  entries.value = [...grouped.values()].map((sources, index) => ({
    id: `${Date.now()}-${index}`,
    label: `订阅 ${index + 1}`,
    source: sources[0],
    sources,
    status: 'loading',
    ani: null,
    error: ''
  }))
  activeName.value = entries.value[0]?.id || ''
  for (const item of entries.value) await load(item)
}

// 手动或单条添加直接传入 RssToAniDTO，并复用相同的加载与确认流程。
const openRequests = async requests => {
  entries.value = requests.map((request, index) => ({
    id: `${Date.now()}-${index}`,
    label: request.title || request.subgroup || `订阅 ${index + 1}`,
    request,
    sources: [],
    status: 'loading',
    ani: null,
    error: ''
  }))
  activeName.value = entries.value[0]?.id || ''
  for (const item of entries.value) await load(item)
}

const retry = item => load(item)

const confirmItem = async (item, done) => {
  try {
    const added = await addSubscription(item)
    if (!added) return
    item.status = 'added'
    ElMessage.success('订阅添加成功')
    closeItem(item)
  } catch (error) {
    ElMessage.error(error?.message || '订阅添加失败')
  } finally {
    done?.()
  }
}

// 首次添加禁止覆盖；检测到重复时由用户明确确认后再携带 override 重试。
const addSubscription = async item => {
  try {
    await http.addAni(item.ani)
  } catch (error) {
    const duplicate = ['此订阅已存在', '订阅标题重复'].includes(error?.message)
    if (!duplicate) throw error
    try {
      await ElMessageBox.confirm(
          `订阅“${item.ani.title}”已存在，是否覆盖原订阅？`,
          '订阅重复',
          {confirmButtonText: '覆盖', cancelButtonText: '取消', type: 'warning'}
      )
    } catch {
      return false
    }
    await http.addAni(item.ani, true)
  }
  return true
}

const closeItem = item => {
  entries.value = entries.value.filter(value => value.id !== item.id)
  if (activeName.value === item.id) activeName.value = entries.value[0]?.id || ''
  if (!entries.value.length) {
    emit('update:modelValue', false)
    emit('done')
  }
}

const closeItemById = id => {
  const item = entries.value.find(value => value.id === id)
  if (item) closeItem(item)
}

defineExpose({open, openRequests})
</script>

<style scoped>
.batch-loading {
  min-height: 180px;
  display: grid;
  place-items: center;
}

.batch-editor {
  min-height: 0;
}

.batch-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
