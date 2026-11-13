<template>
  <template v-if="notificationConfig['notificationType'] === 'ASTRBOT'">
    <!-- 复用现有模板变量，消息以文本形式发送到指定的 AstrBot 会话。 -->
    <SettingsItem label="通知模版">
      <el-input v-model="notificationConfig['notificationTemplate']" :autosize="{ minRows: 2 }"
                placeholder="${notification}" type="textarea"/>
    </SettingsItem>
    <SettingsItem label="服务地址">
      <el-input v-model.trim="notificationConfig['astrBotHost']" placeholder="http://astrbot:6185"/>
    </SettingsItem>
    <SettingsItem label="API Key">
      <el-input v-model.trim="notificationConfig['astrBotApiKey']" placeholder="abk_xxx"
                type="password" show-password/>
    </SettingsItem>
    <SettingsItem label="目标 UMO">
      <el-input v-model.trim="notificationConfig['astrBotUmo']" placeholder="在目标会话发送 /sid 获取"/>
    </SettingsItem>
    <SettingsItem label="使用说明">
      <div>
        <div>需要 AstrBot v4.18.0 及以上版本，在「设置 → OpenAPI」创建具有 im 权限的 API Key。</div>
        <div>服务地址填写 ani-rss 可访问的 AstrBot 地址；Docker 同网络部署可使用 http://astrbot:6185。</div>
        <el-link href="https://docs.astrbot.app/dev/openapi.html" target="_blank" type="primary">
          AstrBot HTTP API 文档
        </el-link>
      </div>
    </SettingsItem>
  </template>
</template>

<script setup>
import SettingsItem from "@/view/custom/SettingsItem.vue";

// 通知配置由父级统一保存，切换类型时保留已填写的参数。
defineProps(['notificationConfig'])
</script>
