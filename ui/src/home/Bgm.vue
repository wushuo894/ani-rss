<template>
  <el-dialog v-model="dialogVisible" center title="Bangumi">
    <div style="min-height: 300px;">
      <div>
        <div style="display: flex;width: 100%;">
          <div style="flex: 1;">
            <el-input v-model:model-value="name" @keyup.enter="search" placeholder="请输入搜索标题" clearable/>
          </div>
          <div style="width: 4px;"></div>
          <el-button @click="search" :loading="searchLoading" text bg icon="Search" :disabled="!name">搜索</el-button>
        </div>
      </div>
      <el-table v-model:data="list" height="500px">
        <el-table-column prop="id" label="id" width="80"/>
        <el-table-column label="封面" width="120">
          <template #default="it">
            <img :alt="list[it.$index]['name']" :src="img(list[it.$index]['images']['large'])" height="100px"
                 width="78px">
          </template>
        </el-table-column>
        <el-table-column label="名称" width="200">
          <template #default="it">
            <span>{{ list[it.$index]['name_cn'] ? list[it.$index]['name_cn'] : list[it.$index]['name'] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="url" prop="url" width="240"/>
        <el-table-column>
          <template #default="it">
            <div class="flex flex-center" style="width: 100%;">
              <el-button bg text @click="ok(list[it.$index])">选择</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-dialog>
</template>
<script setup>

import {ref} from "vue";
import api from "@/js/api.js";

let dialogVisible = ref(false)

let name = ref('')

let searchLoading = ref(false)
let list = ref([])

let search = () => {
  searchLoading.value = true
  api.get('api/bgm?type=search&name=' + name.value)
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
