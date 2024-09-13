<template>
  <el-dialog v-model="dialogVisible" title="修改订阅" center>
    <Ani ref="aniRef" v-model:ani="ani" @ok="editAni"/>
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
  'ova': false,
  'totalEpisodeNumber': '',
  'customDownloadPath': false,
  'downloadPath': ''
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

let aniRef = ref()

const showEdit = (item) => {
  ani.value = JSON.parse(JSON.stringify(item))
  setTimeout(() => {
    aniRef.value?.init()
    dialogVisible.value = true
  }, 100)
}

defineExpose({
  showEdit
})

const emit = defineEmits(['load'])
</script>
