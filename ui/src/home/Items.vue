<template class="items">
  <el-dialog v-model="dialogVisible" center class="items-dialog" title="预览">
    <div style="width: 100%;" v-loading="loading">
      <div style="margin: 4px 0;display: flex;">
        <el-select v-model:model-value="select" style="max-width: 120px;" @change="selectChange">
          <el-option v-for="item in selectItems"
                     :key="item.label"
                     :label="item.label"
                     :value="item.label"/>
        </el-select>
        <div style="width: 4px;"/>
        <el-input v-model:model-value="data.downloadPath" disabled></el-input>
      </div>
      <div style="width: 100%;display: flex;justify-content: end;margin-top: 8px;">
        <el-button bg text :disabled="!selectViews.length" @click="allowDownload" icon="Check" type="primary">允许下载
        </el-button>
        <el-button bg text :disabled="!selectViews.length" @click="notDownload" icon="Close">禁止下载</el-button>
        <popconfirm @confirm="delTorrent" :title="`删除${selectViews.filter(it => it.local).length}个种子缓存?`">
          <template #reference>
            <el-button icon="Remove" bg text type="danger"
                       :disabled="!selectViews.filter(it => it.local).length">
              删除种子
            </el-button>
          </template>
        </popconfirm>
      </div>
      <div style="padding: 0 12px">
        <el-table :data="showItems" height="500"
                  @selection-change="handleSelectionChange"
                  scrollbar-always-on
                  stripe>
          <el-table-column type="selection" width="55" fixed/>
          <el-table-column label="是否下载" min-width="100">
            <template #default="it">
              <el-tag v-if="props.ani['notDownload'].includes(showItems[it.$index]['episode'])" type="info">否</el-tag>
              <el-tag v-else>是</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="本地存在" min-width="100">
            <template #default="it">
              <el-tag v-if="!showItems[it.$index].local" type="info">否</el-tag>
              <el-tag v-else>是</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="主RSS" min-width="80">
            <template #default="it">
              <el-tag v-if="!showItems[it.$index]['master']" type="info">否</el-tag>
              <el-tag v-else>是</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="subgroup" label="字幕组" min-width="120"/>
          <el-table-column label="标题" min-width="400">
            <template #default="it">
              <el-text size="small">
                {{ showItems[it.$index].title }}
              </el-text>
            </template>
          </el-table-column>
          <el-table-column label="重命名" min-width="280">
            <template #default="it">
              <el-text size="small">
                {{ showItems[it.$index]['reName'] }}
              </el-text>
            </template>
          </el-table-column>
          <el-table-column prop="pubDate" label="发布时间" min-width="140"/>
          <el-table-column prop="infoHash" label="InfoHash" min-width="360"/>
          <el-table-column prop="size" label="大小" width="120"/>
          <el-table-column label="种子" width="90">
            <template #default="it">
              <el-button bg text @click="copy(showItems[it.$index]['torrent'])">复制</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="data['omitList'].length">
          <el-alert :title="`缺少集数: ${data['omitList'].slice(0,10).join('、')}`" type="warning" show-icon
                    :closable="false"/>
        </div>
      </div>
    </div>
    <div class="flex" style="margin-top: 12px;justify-content: space-between;">
      <span>共 {{ showItems.length }} 项</span>
      <el-button bg text @click="dialogVisible = false" icon="Close">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import api from "@/js/api.js";
import {ElMessage} from "element-plus";
import Popconfirm from "@/other/Popconfirm.vue";

let selectViews = ref([])
let handleSelectionChange = (selectViewsValue) => {
  selectViews.value = selectViewsValue
}

const select = ref('全部')
const selectItems = ref([
  {
    label: '全部',
    fun: () => true
  },
  {
    label: '本地已存在',
    fun: it => it.local
  },
  {
    label: '本地不存在',
    fun: it => !it.local
  }
])
const dialogVisible = ref(false)
const data = ref({
  'downloadPath': '',
  'items': [],
  'omitList': []
})
const loading = ref(true)

let copy = (v) => {
  const input = document.createElement('input');
  input.value = v
  document.body.appendChild(input);
  input.select();
  document.execCommand('copy');
  document.body.removeChild(input);
  ElMessage.success('已复制')
}

let show = () => {
  data.value.downloadPath = ''
  data.value.items = []
  select.value = '全部'
  dialogVisible.value = true
  load()
}

let selectChange = () => {
  showItems.value = data.value.items.filter(selectItems.value.filter(it => it.label === select.value)[0].fun)
}

let showItems = ref([])

let load = () => {
  loading.value = true
  api.post('api/items', props.ani)
      .then(res => {
        data.value = res.data
        selectChange()
      })
      .finally(() => {
        loading.value = false
      })
}

let delTorrent = () => {
  let infoHash = selectViews.value.filter(it => it['local']).map(it => it['infoHash']).join(",")
  api.del(`api/torrent?id=${props.ani.id}&infoHash=${infoHash}`)
      .then(res => {
        ElMessage.success(res.message)
        load()
      })
}

let notDownload = () => {
  props.ani['notDownload'].push(...selectViews.value.map(it => it['episode']))
  props.ani['notDownload'] = Array.from(new Set(props.ani['notDownload']))
}

let allowDownload = () => {
  props.ani['notDownload'] = props.ani['notDownload'].filter(episode => !selectViews.value.map(it => it['episode']).includes(episode))
}

defineExpose({show})
let props = defineProps(['ani'])
</script>

<style>
@media (min-width: 1400px) {
  .items-dialog {
    width: 1000px;
  }
}
</style>
