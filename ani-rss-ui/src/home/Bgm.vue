<template>
  <el-dialog v-model="dialogVisible" center title="Bangumi">
    <div class="bgm-dialog-content">
      <div>
        <div class="bgm-search-container">
          <div class="bgm-search-input-wrapper">
            <el-input v-model:model-value="name" @keyup.enter="search" placeholder="请输入搜索标题" clearable/>
          </div>
          <div class="bgm-search-spacer"></div>
          <el-button @click="search" :loading="searchLoading" text bg icon="Search" :disabled="!name">搜索</el-button>
        </div>
      </div>
      <el-table size="small" v-model:data="list" height="500px">
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
            <div class="flex flex-center bgm-table-button-wrapper">
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
import {authorization} from "@/js/global.js";
import * as http from "@/js/http.js";

let dialogVisible = ref(false)

let name = ref('')

let searchLoading = ref(false)
let list = ref([])

let search = () => {
  searchLoading.value = true
  http.searchBgm(name.value)
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
  return `api/file?img=${btoa(url)}&s=${authorization.value}`;
}

let ok = (it) => {
  emit('callback', it)
  dialogVisible.value = false
}

defineExpose({show})

const emit = defineEmits(['callback'])
</script>

<style scoped>
.bgm-dialog-content {
  min-height: 300px;
}

.bgm-search-container {
  display: flex;
  width: 100%;
}

.bgm-search-input-wrapper {
  flex: 1;
}

.bgm-search-spacer {
  width: 4px;
}

.bgm-table-button-wrapper {
  width: 100%;
}
</style>
