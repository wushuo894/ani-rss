<template>
  <div v-loading="loading" class="config-page app-page-layout">
    <PageHeaderView title="设置" :subtitle="activeDescription">
      <template #actions>
        <el-button
            class="auto-button"
            icon="Check"
            :disabled="loading"
            :loading="configButtonLoading"
            type="primary"
            @click="saveConfig">
          保存
        </el-button>
      </template>
    </PageHeaderView>
    <div class="config-content app-page-content app-page-padding">
      <el-tabs v-model="activeName" class="segmented-tabs config-tabs">
        <el-tab-pane
            v-for="tab in tabs"
            :key="tab.name"
            :label="tab.label"
            :name="tab.name"
            :lazy="true">
          <el-scrollbar class="config-scrollbar">
            <div class="tab-scroll-content">
              <DownloadView v-if="tab.name === 'download'" v-model:config="config"/>
              <BasicView v-else-if="tab.name === 'basic'" v-model:config="config"/>
              <ExcludeView
                  v-else-if="tab.name === 'exclude'"
                  v-model:exclude="config.exclude"
                  :show-text="true"/>
              <ProxyView v-else-if="tab.name === 'proxy'" v-model:config="config"/>
              <LoginConfigView v-else-if="tab.name === 'login'" :config="config"/>
              <NotificationView v-else-if="tab.name === 'notification'" v-model:config="config"/>
              <AfdianView v-else-if="tab.name === 'afdian'" :config="config"/>
              <AboutView v-else-if="tab.name === 'about'" :config="config"/>
            </div>
          </el-scrollbar>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from "vue";
import {ElMessage} from "element-plus";
import {md5} from "js-md5";
import ExcludeView from "@/view/config/ExcludeView.vue";
import NotificationView from "@/view/config/NotificationView.vue";
import ProxyView from "@/view/config/ProxyView.vue";
import DownloadView from "@/view/config/DownloadView.vue";
import BasicView from "@/view/config/BasicView.vue";
import AboutView from "@/view/config/AboutView.vue";
import LoginConfigView from "@/view/config/LoginConfigView.vue";
import AfdianView from "@/view/config/AfdianView.vue";
import PageHeaderView from "@/view/custom/PageHeaderView.vue";
import {configData} from "@/js/config.js";
import * as http from "@/js/http.js";

const configButtonLoading = ref(false)
const loading = ref(true)

const config = ref(configData)

const activeName = ref('download')
const tabs = [
  {name: 'download', label: '下载设置', description: '下载器、目录与任务行为'},
  {name: 'basic', label: '基本设置', description: '订阅、命名、刮削与备份'},
  {name: 'exclude', label: '全局排除', description: '统一排除不需要的资源'},
  {name: 'proxy', label: '代理设置', description: '网络代理与连接配置'},
  {name: 'login', label: '登录设置', description: '账号与访问安全'},
  {name: 'notification', label: '通知', description: '消息渠道与事件通知'},
  {name: 'afdian', label: '捐赠', description: '支持项目持续维护'},
  {name: 'about', label: '关于', description: '版本信息与项目链接'}
]
const activeDescription = computed(() => tabs.find(tab => tab.name === activeName.value)?.description || '')

const loadConfig = () => {
  loading.value = true
  http.config()
      .then(res => {
        config.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}

const saveConfig = () => {
  configButtonLoading.value = true
  let my_config = JSON.parse(JSON.stringify(config.value))

  let username = my_config.login.username.trim()
  let password = my_config.login.password.trim()

  my_config.login.username = username
  if (password) {
    my_config.login.password = md5(password);
  }

  http.setConfig(my_config)
      .then(res => {
        ElMessage.success(res.message)
        window.$reLoadList?.()
      })
      .finally(() => {
        configButtonLoading.value = false
      })
}

onMounted(() => {
  loadConfig()
})
</script>
<style scoped>
.config-page {
  padding-bottom: 8px;
}

.config-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.config-tabs :deep(.el-tabs__header) {
  flex-shrink: 0;
}

.config-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border-radius: 8px;
  background: var(--el-bg-color);
}

.config-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.config-scrollbar {
  height: 100%;
}

.tab-scroll-content {
  width: min(100%, 920px);
  min-width: 0;
  margin: 0 auto;
  padding: 24px 28px 40px;
}

.config-tabs :deep(.el-alert) {
  border-radius: 6px;
}

@media (max-width: 700px) {
  .tab-scroll-content {
    padding: 18px 4px 30px;
  }

}
</style>
