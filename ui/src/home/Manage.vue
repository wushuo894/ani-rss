<template>
  <el-dialog v-model="dialogVisible" title="管理" center v-if="dialogVisible" class="manage-dialog">
    <div style="min-height: 300px;" v-loading="loading">
      <div style="display: flex;justify-content: space-between;width: 100%;">
        <div style="width: 120px">
          <el-select v-model:model-value="selectFilter" @change="selectChange">
            <el-option v-for="filter in selectFilters"
                       :label="filter.label"
                       :key="filter.label"
                       :value="filter.label"/>
          </el-select>
        </div>
        <div>
          <popconfirm title="删除选中项?" @confirm="del">
            <template #reference>
              <el-button icon="Remove" bg text :disabled="!selectList.length" type="danger">删除
              </el-button>
            </template>
          </popconfirm>
        </div>
      </div>
      <el-scrollbar>
        <el-table
            @selection-change="handleSelectionChange"
            v-model:data="searchList"
            height="400px"
        >
          <el-table-column type="selection" width="55"/>
          <el-table-column label="状态" width="80">
            <template #default="it">
              {{ searchList[it.$index].enable ? '已启用' : '未启用' }}
            </template>
          </el-table-column>
          <el-table-column label="标题" prop="title" width="200"/>
          <el-table-column label="季" prop="season" width="50"/>
          <el-table-column label="URL" prop="url" width="600"/>
        </el-table>
      </el-scrollbar>
    </div>
  </el-dialog>
</template>
<script setup>

import {ref} from "vue";
import api from "../api.js";
import Popconfirm from "../other/Popconfirm.vue";
import {ElMessage} from "element-plus";

let selectFilter = ref('全部')

let selectFilters = ref([
  {
    label: '全部',
    fun: () => true
  },
  {
    label: '已启用',
    fun: it => it.enable
  },
  {
    label: '未启用',
    fun: it => !it.enable
  },
])

let searchList = ref([])

let selectChange = () => {
  searchList.value = list.value.filter(selectFilters.value.filter(item => selectFilter.value === item.label)[0].fun)
}

let dialogVisible = ref(false)
let loading = ref(false)

let show = () => {
  selectFilter.value = '全部'
  dialogVisible.value = true
  getList()
}

const list = ref([])

const getList = () => {
  loading.value = true
  api.get('api/ani')
      .then(res => {
        list.value = res.data
        selectChange()
      })
      .finally(() => {
        loading.value = false
      })
}

let selectList = ref([])

let handleSelectionChange = (v) => {
  selectList.value = v
}

const delLoading = ref(false)

const del = () => {
  delLoading.value = true
  api.del('api/ani', selectList.value.map(it => it['id']))
      .then(res => {
        ElMessage.success(res.message)
        emit('load')
        getList()
      })
      .finally(() => {
        delLoading.value = false
      })
}

defineExpose({show})
const emit = defineEmits(['load'])
</script>

<style>
@media (min-width: 1400px) {
  .manage-dialog {
    width: 1000px;
  }
}
</style>