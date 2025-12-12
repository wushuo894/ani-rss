<template>
  <el-dialog v-if="dialogVisible" v-model="dialogVisible" :title="playItem.name" center>
    <div
        class="flex-center content">
      <Artplayer :src="src" :subtitles="subtitles"/>
    </div>
  </el-dialog>
</template>
<script setup>

import {ref} from "vue";
import Artplayer from "./Artplayer.vue";
import {authorization} from "@/js/global.js";

let dialogVisible = ref(false)
let src = ref('')
let ani = ref({})
let playItem = ref()
let subtitles = ref([])

let show = (i, pi) => {
  src.value = `api/files?filename=${pi.filename}&s=${authorization.value}`
  for (let subtitle of pi.subtitles) {
    subtitle.url = `api/files?filename=${subtitle.url}&s=${authorization.value}`
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

<style scoped>
.content {
  width: 100%;
  max-height: 500px;
  min-height: 200px;
}
</style>
