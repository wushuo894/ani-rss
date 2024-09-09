<template>
  <el-dialog v-model="dialogVisible" title="预览" center>
    <div style="width: 100%;" v-loading="loading">
      <el-scrollbar>
        <el-table :data="items" height="500">
          <el-table-column prop="title" label="标题" width="400"/>
          <el-table-column prop="reName" label="重命名" width="300"/>
          <el-table-column prop="episode" label="集数"/>
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

const dialogVisible = ref(false)
const items = ref([])
const loading = ref(true)

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