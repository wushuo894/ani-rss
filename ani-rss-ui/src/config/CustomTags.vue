<template>
  <div style="width: 100%;">
    <el-tag
        v-if="!props.config.customTags.length"
        type="info"
        class="margin-4px">
      无
    </el-tag>
    <el-tag
        v-for="(tag, index) in props.config.customTags"
        :key="tag"
        closable
        @close="removeCustomTag(index)"
        class="margin-4px"
    >
      {{ tag }}
    </el-tag>
    <el-input
        v-if="inputVisible"
        v-model="inputValue"
        ref="inputRef"
        size="small"
        class="margin-4px custom-tags-input"
        @keyup.enter="handleInputConfirm"
        @blur="handleInputConfirm"
    />
    <el-button
        v-else
        icon="Plus"
        size="small"
        bg text
        class="margin-4px"
        @click="showInput"
    />
  </div>
</template>

<script setup>
import {nextTick, ref} from "vue";

let props = defineProps(['config'])

const inputVisible = ref(false)
const inputValue = ref('')
const inputRef = ref()

const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    inputRef.value && inputRef.value.focus()
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

<style scoped>
.margin-4px {
  margin: 0 0 4px 4px;
}

.custom-tags-input {
  width: 120px;
}
</style>
