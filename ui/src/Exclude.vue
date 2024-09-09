<template>
  <div>
    <div class="flex gap-2">
      <el-tag
          v-for="tag in props.exclude"
          :key="tag"
          closable
          :disable-transitions="false"
          @close="handleClose(tag)"
          style="margin-right: 4px;margin-bottom: 4px;"
      >
        {{ tag }}
      </el-tag>
      <el-input
          style="max-width: 120px;margin-right: 4px;margin-bottom: 4px;"
          v-if="excludeVisible"
          ref="InputRef"
          v-model="excludeValue"
          class="w-20"
          size="small"
          @keyup.enter="handleInputConfirm"
          @blur="handleInputConfirm"
      />
      <el-button v-else class="button-new-tag" size="small" @click="showInput" bg text
                 style="margin-right: 4px;margin-bottom: 4px;">
        +
      </el-button>
    </div>
    <div style="margin-top: 4px;">
      <el-text class="mx-1" size="small">
        支持 <a href="https://www.runoob.com/regexp/regexp-syntax.html" target="_blank">正则表达式</a>
      </el-text>
    </div>
  </div>
</template>

<script setup>

import {ref} from "vue";

const excludeVisible = ref(false)
const excludeValue = ref('')

const handleClose = (tag) => {
  props.exclude.splice(props.exclude.indexOf(tag), 1)
}

const InputRef = ref()

const showInput = () => {
  excludeVisible.value = true
  InputRef.value?.input?.focus()
}

const handleInputConfirm = () => {
  if (excludeValue.value) {
    props.exclude.push(excludeValue.value)
  }
  excludeVisible.value = false
  excludeValue.value = ''
}

let init = () => {
  excludeVisible.value = false
  excludeValue.value = ''
}

defineExpose({
  init
})

let props = defineProps(['exclude'])
</script>