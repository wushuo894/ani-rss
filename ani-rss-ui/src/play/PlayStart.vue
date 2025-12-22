<template>
  <el-dialog v-model="dialogVisible" :title="playItem.name" center @close="onClose">
    <div
        class="flex-center content" v-loading="loading">
      <Artplayer :playItem="playItem" v-if="dialogVisible && !loading"/>
    </div>
  </el-dialog>
</template>
<script setup>

import {ref} from "vue";
import Artplayer from "./Artplayer.vue";
import {authorization} from "@/js/global.js";
import api from "@/js/api.js";

let loading = ref(false);
let dialogVisible = ref(false)
let playItem = ref({})

let show = (pi) => {
  playItem.value = {...pi};
  playItem.value.src = `${location.href}api/files?filename=${playItem.value.filename}&s=${authorization.value}`
  for (let subtitle of playItem.value.subtitles) {
    subtitle.url = `${location.href}api/files?filename=${subtitle.url}&s=${authorization.value}`
  }

  loading.value = true;
  // 获取内封字幕
  api.post('api/playitem', {
    'type': 'getSubtitles',
    'file': playItem.value.filename
  })
      .then(res => {
        for (let sub of res.data) {
          const blob = new Blob([sub.content], {type: "text/plain"});
          sub.url = URL.createObjectURL(blob);
          playItem.value.subtitles.push(sub)
        }
      })
      .finally(() => {
        loading.value = false
      });
  dialogVisible.value = true
}

defineExpose({
  show
})

let onClose = () => {
  dialogVisible.value = false
  playItem.value = {}
}
</script>

<style scoped>
.content {
  width: 100%;
  max-height: 500px;
  min-height: 200px;
}
</style>
