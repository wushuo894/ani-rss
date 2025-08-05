<template>
  <div style="width: 100%;">
    <el-tag
        v-for="(tag, index) in props.config.customTags"
        :key="tag"
        closable
        @close="removeCustomTag(index)"
        style="margin-right: 4px;margin-bottom: 4px;"
    >
      {{ tag }}
    </el-tag>
    <el-input
        v-if="inputVisible"
        v-model="inputValue"
        ref="InputRef"
        size="small"
        style="width: 120px;margin-right: 4px;"
        @keyup.enter="handleInputConfirm"
        @blur="handleInputConfirm"
    />
    <el-button
        v-else
        icon="Plus"
        size="small"
        bg text
        style="margin-right: 4px;margin-bottom: 4px;"
        @click="showInput"
    />
  </div>
</template>

<script setup>
import {nextTick, ref} from "vue";

let props = defineProps(['config'])

const inputVisible = ref(false)
const inputValue = ref('')
const InputRef = ref()

const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    InputRef.value && InputRef.value.focus()
  })
}

const handleInputConfirm = () => {
  if (!Array.isArray(props.config.customTags)) {
    props.config.customTags = []
  }
  const value = inputValue.value && inputValue.value.trim()
  if (value && !props.config.customTags.includes(value)) {
    props.config.customTags.push(value)
  }
  inputVisible.value = false
  inputValue.value = ''
}

const removeCustomTag = (index) => {
  props.config.customTags.splice(index, 1)
}
</script>
