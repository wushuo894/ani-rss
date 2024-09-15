<template>
  <el-dialog v-model="dialogVisible" :title="ani.title+' '+playItem.title" center v-if="dialogVisible">
    <div
        style="display: flex;width: 100%;justify-content: center;align-items: center;max-height: 500px;min-height: 200px;">
      <video :src="src" autoplay controls style="max-height: 500px;max-width: 700px">
      </video>
    </div>
  </el-dialog>
</template>
<script setup>

import {ref} from "vue";

let dialogVisible = ref(false)
let src = ref('')
let ani = ref({})
let playItem = ref()

let show = (i, pi) => {
  ani.value = i
  playItem.value = pi
  dialogVisible.value = true
  console.log(pi.filename);
  src.value = `/api/files?filename=${pi.filename}&s=${window.authorization}&config=false`
}

defineExpose({
  show
})
</script>
