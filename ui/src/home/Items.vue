<template>
  <el-dialog v-model="dialogVisible" title="预览" center v-if="dialogVisible">
    <div style="width: 100%;" v-loading="loading">
      <div style="margin: 4px 0;display: flex;">
        <el-select v-model:model-value="select" style="max-width: 120px;">
          <el-option v-for="item in selectItems"
                     :key="item.label"
                     :label="item.label"
                     :value="item.label"/>
        </el-select>
        <div style="width: 4px;"/>
        <el-input v-model:model-value="data.downloadPath" disabled></el-input>
      </div>
      <el-scrollbar style="padding: 0 12px">
        <el-table :data="data.items.filter(selectItems.filter(it => it.label === select)[0].fun)" height="500">
          <el-table-column label="本地是否存在" min-width="100">
            <template #default="it">
              {{ data.items[it.$index].local ? '是' : '否' }}
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="400"/>
          <el-table-column prop="reName" label="重命名" min-width="280"/>
          <el-table-column prop="size" label="大小" width="120"/>
          <el-table-column label="种子" width="90">
            <template #default="it">
              <el-button bg text @click="copy(data.items[it.$index])">复制</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-scrollbar>
      <div style="margin: 4px 0;display: flex;justify-content: end;">
        <el-text class="mx-1" size="small">
          检测 <strong>本地是否存在</strong> 需要开启 <strong>文件已下载自动跳过</strong> 与 <strong>自动重命名</strong>
        </el-text>
      </div>
    </div>
    <div style="margin-top: 12px;display: flex;justify-content: end;">
      <el-button bg text @click="dialogVisible = false" icon="Close">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import api from "../api.js";
import {ElMessage, ElText} from "element-plus";

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
  'items': []
})
const loading = ref(true)

let copy = (it) => {
  const input = document.createElement('input');
  input.value = it['torrent'];
  document.body.appendChild(input);
  input.select();
  document.execCommand('copy');
  document.body.removeChild(input);
  ElMessage.success('已复制')
}

let show = (ani) => {
  data.value.downloadPath = ''
  data.value.items = []
  select.value = '全部'
  dialogVisible.value = true
  loading.value = true
  api.post('/api/items', ani)
      .then(res => {
        data.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}

defineExpose({show})
</script>