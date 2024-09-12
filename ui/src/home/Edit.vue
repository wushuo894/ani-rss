<template>
  <el-dialog v-model="dialogVisible" title="修改订阅" center>
    <Ani v-model:ani="ani" @ok="editAni"/>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import {ElMessage} from "element-plus";
import api from "../api.js";
import Ani from "./Ani.vue";


const dialogVisible = ref(false)

const ani = ref({
  'url': '',
  'season': 1,
  'offset': 0,
  'title': '',
  'themoviedbName': '',
  'exclude': [],
  'enable': true,
  'ova': false
})

const editAni = (fun) => {
  api.put('/api/ani', ani.value)
      .then(res => {
        ElMessage.success(res.message)
        emit('load')
        dialogVisible.value = false
      })
      .finally(fun);
}


const showEdit = (item) => {
  dialogVisible.value = true
  ani.value = JSON.parse(JSON.stringify(item))
}

defineExpose({
  showEdit
})

const emit = defineEmits(['load'])
</script>
