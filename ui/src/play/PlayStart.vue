<template>
  <el-dialog v-model="dialogVisible" :title="ani.title+' '+playItem.title" center v-if="dialogVisible">
    <div
        style="display: flex;width: 100%;justify-content: center;align-items: center;max-height: 500px;min-height: 200px;">
      <Artplayer :src="src" :subtitles="subtitles"/>
    </div>
  </el-dialog>
</template>
<script setup>

import {ref} from "vue";
import Artplayer from "./Artplayer.vue";

let dialogVisible = ref(false)
let src = ref('')
let ani = ref({})
let playItem = ref()
let subtitles = ref([])

let show = (i, pi) => {
  src.value = `/api/files?filename=${pi.filename}&s=${window.authorization}&config=false`
  for (let subtitle of pi.subtitles) {
    subtitle.url = `/api/files?filename=${subtitle.url}&s=${window.authorization}&config=false`
  }
  subtitles = pi.subtitles
  ani.value = i
  playItem.value = pi
  dialogVisible.value = true
}

defineExpose({
  show
})
</script>
