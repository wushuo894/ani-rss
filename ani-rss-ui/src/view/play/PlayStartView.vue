<template>
  <el-dialog v-model="dialogVisible" :title="playItem.name" center @close="onClose">
    <div
        class="flex-center content" v-loading="loading">
      <MoviPlayerView :playItem="playItem" v-if="dialogVisible && !loading"/>
    </div>
  </el-dialog>
</template>
<script setup>

import {ref} from "vue";
import MoviPlayerView from "./MoviPlayerView.vue";
import {toApiFile} from "@/js/global.js";

let loading = ref(false);
let dialogVisible = ref(false)
let playItem = ref({})

let show = (pi) => {
  playItem.value = {...pi};
  playItem.value.src = toApiFile(playItem.value.filename)
  loading.value = false
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
