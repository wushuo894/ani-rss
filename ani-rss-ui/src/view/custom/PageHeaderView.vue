<template>
  <header class="page-header app-page-padding">
    <div class="page-header-heading">
      <h2 class="page-header-title">{{ title }}</h2>
      <el-text v-if="subtitle" class="page-header-subtitle" size="small" type="info">
        {{ subtitle }}
      </el-text>
    </div>
    <div v-if="$slots.actions" class="page-header-actions">
      <slot name="actions"/>
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: [String, Number],
    default: ''
  }
})
</script>

<style scoped>
.page-header {
  min-width: 0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-header-heading {
  min-width: 0;
  flex: 1;
}

.page-header-title {
  line-height: 1.4;
}

.page-header-subtitle {
  display: block;
  max-width: 100%;
  overflow-wrap: anywhere;
}

.page-header-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  margin-left: auto;
}

@media (max-width: 560px) {
  .page-header {
    align-items: flex-start;
  }
}
</style>
