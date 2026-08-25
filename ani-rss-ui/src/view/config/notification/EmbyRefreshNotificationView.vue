<template>
  <template v-if="notificationConfig['notificationType'] === 'EMBY_REFRESH'">
    <SettingsItem label="EmbyHost">
      <el-input v-model="notificationConfig['embyHost']" placeholder="http://x.x.x.x:8096"/>
    </SettingsItem>
    <SettingsItem label="Emby密钥">
      <el-input v-model="notificationConfig['embyApiKey']"/>
    </SettingsItem>
    <SettingsItem label="媒体库">
      <div>
        <el-checkbox-group v-model="notificationConfig['embyRefreshViewIds']">
          <el-checkbox
              v-for="view in views"
              :key="view.id"
              :label="view.name"
              :value="view.id"/>
        </el-checkbox-group>
        <div>
          <el-button :loading="getEmbyViewsLoading" bg icon="Refresh" text @click="getEmbyViews"/>
        </div>
      </div>
    </SettingsItem>
    <SettingsItem label="延迟">
      <el-input-number v-model="notificationConfig['embyDelayed']"
                       class="notification-input-width"
                       :min="0">
        <template #suffix>
          <span>秒</span>
        </template>
      </el-input-number>
    </SettingsItem>
  </template>
</template>

<script setup>
import SettingsItem from "@/view/custom/SettingsItem.vue";
import {onMounted, ref} from "vue";
import * as http from "@/js/http.js";

const views = ref([])

const getEmbyViewsLoading = ref(false)

const getEmbyViews = () => {
  getEmbyViewsLoading.value = true
  http.getEmbyViews(props.notificationConfig)
      .then(res => {
        views.value = res.data
      })
      .finally(() => {
        getEmbyViewsLoading.value = false
      })
}

onMounted(() => {
  if (props.notificationConfig['embyHost'] && props.notificationConfig['embyApiKey']) {
    getEmbyViews()
  }
})

let props = defineProps(['notificationConfig', 'config'])
</script>
