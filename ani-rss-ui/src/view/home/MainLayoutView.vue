<template>
  <div class="app-shell">
    <aside class="app-nav">
      <div class="app-brand">
        <img class="app-logo" src="/icon.svg" alt="ANI-RSS">
        <span>ANI-RSS</span>
      </div>
      <el-menu
          :default-active="route.path"
          :ellipsis="false"
          class="app-menu"
          router>
        <el-menu-item index="/home">
          <el-icon>
            <House/>
          </el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/subscriptions">
          <el-icon>
            <Collection/>
          </el-icon>
          <span>订阅</span>
        </el-menu-item>
        <el-menu-item index="/downloads">
          <el-icon>
            <Download/>
          </el-icon>
          <span>下载</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon>
            <Tickets/>
          </el-icon>
          <span>日志</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon>
            <Setting/>
          </el-icon>
          <span>设置</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <main class="app-main">
      <RouterView v-slot="{ Component }">
        <KeepAlive>
          <component :is="Component"/>
        </KeepAlive>
      </RouterView>
    </main>
    <!-- 登录后提示默认凭据，允许稍后处理，也可直接进入安全设置。 -->
    <el-dialog v-model="defaultLoginDialogVisible" title="请修改默认登录信息"
               width="min(440px, calc(100vw - 32px))" :close-on-click-modal="false">
      <p class="default-login-message">
        当前仍在使用默认用户名和密码，请及时前往「设置 → 安全」修改，并保存设置。
      </p>
      <el-checkbox v-model="hideDefaultLoginReminder" class="default-login-preference">
        不再提醒
      </el-checkbox>
      <template #footer>
        <el-button @click="defaultLoginDialogVisible = false">稍后再说</el-button>
        <el-button type="primary" @click="openSecuritySettings">立即修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {onMounted, ref} from "vue";
import {useLocalStorage} from "@vueuse/core";
import {RouterView, useRoute, useRouter} from "vue-router";
import {Collection, Download, House, Setting, Tickets} from "@element-plus/icons-vue";
import {initLayout} from "@/js/global.js";
import * as http from "@/js/http.js";

const route = useRoute()
const router = useRouter()
const defaultLoginDialogVisible = ref(false)
// 在当前浏览器持久化提醒偏好，刷新页面或重新登录后仍然生效。
const hideDefaultLoginReminder = useLocalStorage('hide-default-login-reminder', false)

/**
 * 未选择“不再提醒”时读取后端判断，页面内切换路由时不重复弹窗。
 */
const checkDefaultLogin = async () => {
  if (hideDefaultLoginReminder.value) {
    return
  }
  try {
    const res = await http.config()
    // 请求期间其他标签页也可能更新提醒偏好，展示前再次判断。
    defaultLoginDialogVisible.value = !hideDefaultLoginReminder.value && res.data?.login?.isDefault === true
  } catch {
    // 请求错误由接口层统一提示，避免影响主页面的正常展示。
  }
}

/**
 * 关闭提醒并定位到安全页签，复用现有账号密码修改及保存流程。
 */
const openSecuritySettings = () => {
  defaultLoginDialogVisible.value = false
  router.push({path: '/settings', query: {tab: 'security'}})
}

onMounted(() => {
  initLayout()
  checkDefaultLogin()
})
</script>

<style scoped>
.default-login-message {
  margin: 0;
  line-height: 1.8;
  color: var(--el-text-color-regular);
}

.default-login-preference {
  margin-top: 12px;
}

.app-shell {
  width: 100%;
  height: 100%;
  display: flex;
}

.app-nav {
  width: 132px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.app-brand {
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.app-logo {
  width: 34px;
  height: 34px;
}

.app-menu {
  flex: 1;
  border: 0;
  background: none;
}

.app-menu :deep(.el-menu-item) {
  height: 46px;
  margin: 8px 12px;
  border-radius: 8px;
  border: none !important;
}

.app-menu :deep(.el-menu-item.is-active) {
  background-color: var(--el-menu-hover-bg-color);
}

.app-main {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

@media (max-width: 800px) {
  .app-shell {
    display: block;
    padding-bottom: calc(58px + env(safe-area-inset-bottom, 0px));
  }

  .app-nav {
    position: fixed;
    left: 0;
    bottom: 0;
    z-index: 10;
    width: 100%;
    padding-bottom: env(safe-area-inset-bottom, 0px);
    border-top: 1px solid var(--el-border-color-light);
  }

  .app-brand {
    display: none;
  }

  .app-menu {
    flex: none;
    height: 58px;
    display: flex;
    justify-content: space-around;
    gap: 4px;
    padding: 4px;
    box-sizing: border-box;
  }

  .app-menu :deep(.el-menu-item) {
    flex: 1;
    height: 50px;
    line-height: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
    margin: 0;
    padding: 0 4px !important;
    transition: color var(--el-transition-duration), background-color var(--el-transition-duration);
  }

  .app-menu :deep(.el-menu-item .el-icon) {
    margin: 0;
  }

  .app-menu :deep(.el-menu-item span) {
    font-size: 12px;
  }

  .app-main {
    height: 100%;
  }
}
</style>
