<template>
  <el-dialog v-model="dialogVisible" title="管理" center v-if="dialogVisible" class="manage-dialog">
    <div style="min-height: 300px;" v-loading="loading">
      <div style="display: flex;justify-content: space-between;width: 100%;">
        <dev>
          <el-checkbox label="选中已启用" v-model="checkEnableAll"
                       @change="checkEnableAllChange"
                       :indeterminate="checkEnableAllIn"/>
          <el-checkbox label="选中未启用" v-model="checkDisabledAll"
                       @change="checkDisabledAllChange"
                       :indeterminate="checkDisabledAllIn"/>
        </dev>
        <div>
          <popconfirm title="删除选中项?" @confirm="del">
            <template #reference>
              <el-button icon="Remove" bg text :disabled="!list.filter(it => it.ok).length" type="danger">删除</el-button>
            </template>
          </popconfirm>
        </div>
      </div>
      <el-scrollbar>
        <el-table v-model:data="list" height="400px">
          <el-table-column width="50">
            <template #default="it">
              <el-checkbox v-model="list[it.$index].ok" @change="checkboxChange"/>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="it">
              {{ list[it.$index].enable ? '已启用' : '未启用' }}
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

let dialogVisible = ref(false)
let loading = ref(false)

const checkEnableAll = ref(false)
const checkDisabledAll = ref(false)

const checkEnableAllChange = (val) => {
  for (let filterElement of list.value.filter(it => it.enable)) {
    filterElement.ok = val
  }
}

const checkDisabledAllChange =(val)=>{
  for (let filterElement of list.value.filter(it => !it.enable)) {
    filterElement.ok = val
  }
}

let show = () => {
  dialogVisible.value = true
  getList()
}

const list = ref([])

const getList = () => {
  loading.value = true
  api.get('api/ani')
      .then(res => {
        for (let datum of res.data) {
          datum.ok = false
        }
        list.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}

const checkEnableAllIn = ref(false)
const checkDisabledAllIn = ref(false)

const checkboxChange = () => {
  checkEnableAllIn.value = list.value.filter(it => it.ok && it.enable).length > 0 && list.value.filter(it => it.ok && it.enable).length < list.value.filter(it => it.enable).length
  checkDisabledAllIn.value = list.value.filter(it => it.ok && !it.enable).length > 0 && list.value.filter(it => it.ok && !it.enable).length < list.value.filter(it => !it.enable).length
  if (list.value.filter(it => it.enable).length) {
    checkEnableAll.value = list.value.filter(it => it.enable).length === list.value.filter(it => it.ok && it.enable).length
  }
  if (list.value.filter(it => !it.enable).length) {
    checkDisabledAll.value = list.value.filter(it => !it.enable).length === list.value.filter(it => it.ok && !it.enable).length
  }
}

const delLoading = ref(false)

const del = () => {
  delLoading.value = true
  api.del('api/ani', list.value.filter(it => it.ok).map(it => it.id))
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