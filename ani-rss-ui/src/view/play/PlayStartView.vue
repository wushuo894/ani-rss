<template>
  <el-dialog v-model="dialogVisible" :title="playItem.name" center @close="onClose">
    <div
        class="flex-center content"
        v-loading="loading"
        element-loading-text="正在加载播放器…">
      <component :is="playerComponent"
                 :playItem="playItem"
                 v-if="dialogVisible && playerComponent"/>
    </div>
  </el-dialog>
</template>
<script setup>

import {ref, shallowRef} from "vue";
import {toApiFile} from "@/js/global.js";
import {ElMessage} from "element-plus";

let loading = ref(false);
let dialogVisible = ref(false)
let playItem = ref({})

// 缓存已加载的组件，避免 Vue 对组件定义进行深层响应式代理。
const playerComponent = shallowRef()

let show = async (pi) => {
  playItem.value = {...pi};
  playItem.value.src = toApiFile(playItem.value.filename)
  loading.value = true
  dialogVisible.value = true

  try {
    if (playerComponent.value) {
      return
    }

    const {default: component} = await import("./MoviPlayerView.vue")
    playerComponent.value = component
  } catch {
    ElMessage.error('播放器加载失败，请刷新页面后重试')
    dialogVisible.value = false
  } finally {
    loading.value = false
  }
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
