<template>
  <div class="settings-item" :class="{'is-label-empty': !label}">
    <div v-if="label" class="settings-item-label">
      {{ label }}
    </div>
    <div class="settings-item-content">
      <slot/>
    </div>
  </div>
</template>
<script setup>
defineProps({
  label: {
    type: String,
    default: ''
  }
})
</script>
<style scoped>
.settings-item {
  display: grid;
  grid-template-columns: 130px minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  min-width: 0;
  margin-bottom: 16px;
  align-items: start;
  box-sizing: border-box;
}

.settings-item.is-label-empty {
  grid-template-columns: minmax(0, 1fr);
}

.settings-item-label {
  color: var(--el-text-color-regular);
  font-weight: 500;
  font-size: 13px !important;
  line-height: 32px;
  overflow-wrap: anywhere;
}

.settings-item-content {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
}

@media (max-width: 700px) {
  .settings-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .settings-item-label {
    line-height: 1.4;
  }
}
</style>
