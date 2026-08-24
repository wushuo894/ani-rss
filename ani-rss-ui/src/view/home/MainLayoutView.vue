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
  </div>
</template>

<script setup>
import {onMounted} from "vue";
import {RouterView, useRoute} from "vue-router";
import {Collection, Download, House, Setting, Tickets} from "@element-plus/icons-vue";
import {initLayout} from "@/js/global.js";

const route = useRoute()

onMounted(() => {
  initLayout()
})
</script>

<style scoped>
.app-shell {
  width: 100%;
  height: 100%;
  display: flex;
  background: var(--el-bg-color);
}

.app-nav {
  width: 132px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
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
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.app-logo {
  width: 34px;
  height: 34px;
}

.app-menu {
  flex: 1;
  border: 0;
}

.app-menu :deep(.el-menu-item) {
  height: 46px;
  margin: 4px 8px;
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
  padding: 12px;
  background: var(--el-bg-color);
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
    padding: 8px;
  }
}
</style>
