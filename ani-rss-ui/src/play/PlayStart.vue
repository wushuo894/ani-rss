<template>
  <el-dialog v-model="dialogVisible" :title="playItem.name" center @close="onClose">
    <div
        class="flex-center content">
      <Artplayer :playItem="playItem" v-if="dialogVisible"/>
    </div>
  </el-dialog>
</template>
<script setup>

import {ref} from "vue";
import Artplayer from "./Artplayer.vue";
import {authorization} from "@/js/global.js";

let dialogVisible = ref(false)
let playItem = ref({})

let show = (pi) => {
  playItem.value = {...pi};
  playItem.value.src = `${location.href}api/files?filename=${playItem.value.filename}&s=${authorization.value}`
  for (let subtitle of playItem.value.subtitles) {
    subtitle.url = `${location.href}api/files?filename=${subtitle.url}&s=${authorization.value}`
  }
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
