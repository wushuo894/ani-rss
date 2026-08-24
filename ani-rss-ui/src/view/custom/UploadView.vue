<template>
  <input ref="inputRef" :accept="accept" hidden="hidden" type="file" @change="changeFile">
  <div
      @click="selectAndUpload"
      @dragenter.prevent="onDragEnter"
      @dragover.prevent="onDragOver"
      @dragleave.prevent="onDragLeave"
      @drop.prevent="onDrop"
      :class="{ 'upload-dragover': isDragOver }"
      style="display: inline-block;"
  >
    <slot/>
  </div>
</template>

<style scoped>
.upload-dragover {
  outline: 2px dashed var(--el-color-primary);
  outline-offset: 2px;
  background-color: var(--el-color-primary-light-9);
  border-radius: 4px;
}
</style>

<script setup>

import {computed, ref} from "vue";
import {authorization} from "@/js/global.js";

const props = defineProps({
  url: String,
  extensions: {
    type: Array,
    default: () => []
  },
  callback: Function
})

let inputRef = ref()
let isDragOver = ref(false)
let dragCounter = ref(0)
const acceptedExtensions = computed(() => props.extensions
    .map(extension => extension.replace(/^\./, '').toLowerCase()))
const accept = computed(() => acceptedExtensions.value
    .map(extension => `.${extension}`)
    .join(','))

let selectAndUpload = () => {
  inputRef.value.value = ''
  inputRef.value.click()
}

let uploadFile = async (file) => {
  if (!file) {
    return Promise.reject(new Error('文件为空'))
  }

  const extension = file.name.match(/\.([^.]+)$/)?.[1]?.toLowerCase()
  if (acceptedExtensions.value.length && !acceptedExtensions.value.includes(extension)) {
    return Promise.reject(new Error('文件格式错误'))
  }

  const formData = new FormData();
  formData.append("file", file)

  const res = await fetch(props.url, {
    method: 'POST',
    body: formData,
    headers: {
      'Authorization': authorization.value
    }
  });
  return await res.json();
}

let changeFile = () => {
  let file = inputRef.value.files[0];
  uploadFile(file)
      .then(res => props.callback?.(res, file))
      .catch(err => props.callback?.({message: err.message}, file))
}

let onDragEnter = () => {
  dragCounter.value++
  isDragOver.value = true
}

let onDragOver = () => {
  // 必须 preventDefault 否则 drop 不会触发，已在模板用 .prevent
}

let onDragLeave = () => {
  dragCounter.value--
  if (dragCounter.value <= 0) {
    isDragOver.value = false
    dragCounter.value = 0
  }
}

let onDrop = (e) => {
  isDragOver.value = false
  dragCounter.value = 0

  const files = e.dataTransfer?.files
  if (!files || files.length === 0) {
    return
  }
  // 与点击上传保持一致：只上传第一个文件
  uploadFile(files[0])
      .then(res => props.callback?.(res, files[0]))
      .catch(err => props.callback?.({message: err.message}, files[0]))
}

defineExpose({
  selectAndUpload
})
</script>
