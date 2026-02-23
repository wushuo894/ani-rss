<template>
  <template v-if="props.notificationConfig['notificationType'] === 'EMBY_REFRESH'">
    <el-form-item label="EmbyHost">
      <el-input v-model="props.notificationConfig['embyHost']" placeholder="http://x.x.x.x:8096"/>
    </el-form-item>
    <el-form-item label="Emby密钥">
      <el-input v-model="props.notificationConfig['embyApiKey']"/>
    </el-form-item>
    <el-form-item label="媒体库">
      <div>
        <el-checkbox-group v-model="props.notificationConfig['embyRefreshViewIds']">
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
    </el-form-item>
    <el-form-item label="延迟">
      <el-input-number v-model="props.notificationConfig['embyDelayed']"
                       class="notification-input-width"
                       :min="0">
        <template #suffix>
          <span>秒</span>
        </template>
      </el-input-number>
    </el-form-item>
  </template>
</template>

<script setup>
import {onMounted, ref} from "vue";
import * as http from "@/js/http.js";

const views = ref([])

const getEmbyViewsLoading = ref(false)

const getEmbyViews = () => {
  getEmbyViewsLoading.value = true
  http.getViews(props.notificationConfig)
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
