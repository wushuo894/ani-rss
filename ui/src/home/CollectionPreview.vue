<template>
  <el-dialog v-model="dialogVisible"
             center
             title="合集预览"
             @close="data.torrent = ''">
    <div v-loading="loading">
      <el-table :data="data" height="500"
                scrollbar-always-on
                stripe>
        <el-table-column label="标题" min-width="400" prop="title"/>
        <el-table-column label="重命名" min-width="280" prop="reName"/>
        <el-table-column label="集数" prop="episode"/>
        <el-table-column label="大小" min-width="100" prop="size"/>
      </el-table>
    </div>
    <div style="margin-top: 12px;display: flex;justify-content: end;">
      <el-button bg icon="Close" text @click="dialogVisible = false">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import api from "../api.js";

let dialogVisible = ref(false)
let loading = ref(false)

let data = ref([])

let show = () => {
  dialogVisible.value = true
  loading.value = true
  api.post('api/collection?type=preview', props.data)
      .then(res => {
        data.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}

defineExpose({show})

let props = defineProps(['data'])
</script>
