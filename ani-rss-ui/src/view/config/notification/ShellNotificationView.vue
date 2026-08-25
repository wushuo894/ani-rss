<template>
  <template v-if="notificationConfig['notificationType'] === 'SHELL'">
    <SettingsItem label="Shell">
      <el-input v-model:model-value="notificationConfig['shell']" type="textarea"
                :autosize="{ minRows: 2}"
                placeholder='ping 223.5.5.5'/>
    </SettingsItem>
    <SettingsItem label="存活限制">
      <el-input-number v-model="notificationConfig['aliveLimit']"
                       class="notification-input-width"
                       :min="1">
        <template #suffix>
          <span>秒</span>
        </template>
      </el-input-number>
    </SettingsItem>
  </template>
</template>

<script setup>
import SettingsItem from "@/view/custom/SettingsItem.vue";

let props = defineProps(['notificationConfig', 'config'])
</script>
