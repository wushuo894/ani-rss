<template>
  <el-dialog v-model="dialogVisible" title="修改订阅" center @close="aniShow = false">
    <Ani ref="aniRef" v-model:ani="ani" @ok="editAni" v-if="aniShow"/>
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

const aniShow = ref(false)

const showEdit = (item) => {
  ani.value = JSON.parse(JSON.stringify(item))
  ani.value.showDownlaod = true
  aniShow.value = true
  dialogVisible.value = true
}

defineExpose({
  showEdit
})

const emit = defineEmits(['load'])
</script>
