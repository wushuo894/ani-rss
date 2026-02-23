<template>
  <el-popconfirm :title="props.title" @confirm="emit('confirm')" :width="160">
    <template #reference>
      <slot name="reference"/>
    </template>
    <template #actions="{ confirm, cancel }">
      <div class="flex action">
        <div>
          <el-button size="small" @click="cancel" bg text icon="Close">取消</el-button>
        </div>
        <div>
          <el-button
              type="danger"
              size="small"
              @click="confirm"
              bg text
              icon="Check"
          >
            确定
          </el-button>
        </div>
      </div>
    </template>
  </el-popconfirm>
</template>

<script setup>
let props = defineProps(['title'])
const emit = defineEmits(['confirm'])
</script>

<style scoped>
.action {
  justify-content: space-between;
}
</style>
