<template>
  <div :style="{
    'opacity': props.modelValue ? 1 : 0.4,
    'pointer-events': props.modelValue ? 'auto' : 'none'
  }">
    <slot/>
  </div>
</template>

<script setup>
let props = defineProps({
  modelValue: Boolean
})
</script>