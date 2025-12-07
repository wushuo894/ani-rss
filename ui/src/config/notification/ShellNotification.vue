<template>
  <template v-if="notificationConfig['notificationType'] === 'SHELL'">
    <el-form-item label="Shell">
      <el-input v-model:model-value="props.notificationConfig['shell']" type="textarea"
                :autosize="{ minRows: 2}"
                placeholder='ping 223.5.5.5'/>
    </el-form-item>
    <el-form-item label="存活限制">
      <el-input-number v-model="props.notificationConfig['aliveLimit']"
                       class="notification-input-width"
                       :min="1">
        <template #suffix>
          <span>秒</span>
        </template>
      </el-input-number>
    </el-form-item>
  </template>
</template>

<script setup>
let props = defineProps(['notificationConfig', 'config'])
</script>
