<template>
  <el-dialog v-model="dialogVisible" title="预览" center>
    <div style="width: 100%;" v-loading="loading">
      <el-scrollbar>
        <el-table :data="items" height="500">
          <el-table-column prop="title" label="标题" min-width="400"/>
          <el-table-column prop="reName" label="重命名" min-width="280"/>
          <el-table-column prop="size" label="大小" width="120"/>
          <el-table-column label="种子" width="90">
            <template #default="it">
              <el-button bg text @click="copy(items[it.$index])">复制</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-scrollbar>
    </div>
    <div style="margin-top: 12px;display: flex;justify-content: end;">
      <el-button bg text @click="dialogVisible = false">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import api from "./api.js";
import {ElMessage} from "element-plus";

const dialogVisible = ref(false)
const items = ref([])
const loading = ref(true)

let copy = (it) => {
  navigator.clipboard.writeText(it['torrent'])
  ElMessage.success('已复制')
}

let show = (ani) => {
  items.value = []
  dialogVisible.value = true
  loading.value = true
  api.post('/api/items', ani)
      .then(res => {
        items.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}

defineExpose({show})
</script>