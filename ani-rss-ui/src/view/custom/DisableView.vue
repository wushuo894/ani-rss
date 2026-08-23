<template>
  <div :style="{
    'opacity': props.url ? 1 : 0.4,
    'pointer-events': props.url ? 'auto' : 'none'
  }">
    <slot/>
  </div>
</template>

<script setup>
let props = defineProps({
  url: Boolean
})
</script>