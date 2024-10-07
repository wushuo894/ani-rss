<template>
  <el-dialog v-model="dialogVisible" title="Bangumi" center v-if="dialogVisible">
    <div style="min-height: 300px;">
      <div>
        <div style="display: flex;width: 100%;">
          <div style="flex: 1;">
            <el-input v-model:model-value="name" @keyup.enter="search" placeholder="请输入搜索标题" clearable/>
          </div>
          <div style="width: 4px;"></div>
          <el-button @click="search" :loading="searchLoading" text bg icon="Search">搜索</el-button>
        </div>
      </div>
      <el-table v-model:data="list" height="500px">
        <el-table-column prop="id" label="id" width="80"/>
        <el-table-column label="封面" width="120">
          <template #default="it">
            <img :src="img(list[it.$index]['images']['small'])" :alt="list[it.$index]['name']" height="100px" width="78px">
          </template>
        </el-table-column>
        <el-table-column prop="name_cn" label="名称" width="200"/>
        <el-table-column prop="url" label="url" width="200"/>
        <el-table-column>
          <template #default="it">
            <el-button bg text @click="ok(list[it.$index])" >选择</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-dialog>
</template>
<script setup>

import {ref} from "vue";
import api from "../api.js";

let dialogVisible = ref(false)

let name = ref('')

let searchLoading = ref(false)
let list = ref([])

let search = () => {
  searchLoading.value = true
  api.get('/api/bgm?name=' + name.value)
      .then(res => {
        list.value = res.data
      })
      .finally(() => {
        searchLoading.value = false
      })
}

let show = (s) => {
  name.value = ''
  if (s) {
    name.value = s
    search()
  }
  list.value = []
  dialogVisible.value = true
}

let img = (url) => {
  return `api/file?img=${btoa(url)}&s=${window.authorization}`;
}

let ok = (it) => {
  emit('add', it)
  dialogVisible.value = false
}

defineExpose({show})

const emit = defineEmits(['add'])
</script>
