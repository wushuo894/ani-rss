<template>
  <div class="flex-center" style="width: 100%;flex-flow: column;">
    <div class="flex" style="margin-bottom: 12px;align-items: end;">
      <img alt="icon.svg" height="80" src="../../public/icon.svg" width="80"/>
      <div>
        <h1>ANI-RSS</h1>
        <el-text class="mx-1" size="small">
          &nbsp;v{{ props.config.version }}
        </el-text>
      </div>
    </div>
    <div class="flex" style="margin-bottom: 12px;align-items: center;">
      <div id="button-list">
        <el-button :icon="Github" bg text type="info" @click="openUrl('https://github.com/wushuo894/ani-rss')">GitHub
        </el-button>
        <el-button :icon="Book" bg text type="info" @click="openUrl('https://docs.wushuo.top')">使用文档</el-button>
        <el-button :icon="Telegram" bg text type="info" @click="openUrl('https://t.me/ani_rss')">TG群</el-button>
      </div>
    </div>
    <div v-loading.fullscreen.lock="actionLoading" class="flex" style="margin-bottom: 8px;">
      <popconfirm title="你确定要退出吗?" @confirm="logout">
        <template #reference>
          <el-button type="danger" bg text icon="Back">
            退出
          </el-button>
        </template>
      </popconfirm>
      <div style="margin: 6px;"></div>
      <popconfirm title="你确定重启吗?" @confirm="stop(0)">
        <template #reference>
          <el-button bg icon="RefreshRight" text type="warning">重启</el-button>
        </template>
      </popconfirm>
      <div style="margin: 6px;"></div>
      <popconfirm title="你确定关闭吗?" @confirm="stop(1)">
        <template #reference>
          <el-button bg icon="SwitchButton" text type="danger">关闭</el-button>
        </template>
      </popconfirm>
      <div style="margin: 6px;"></div>
      <el-badge :hidden="!about.update" class="item" value="new">
        <el-button :loading="about.version.length < 1" bg icon="Top" text type="success" @click="dialogVisible = true">
          更新
        </el-button>
      </el-badge>
    </div>
  </div>
  <el-dialog v-if="dialogVisible" v-model="dialogVisible" align-center center title="版本更新"
             style="max-width: 500px;">
    <div v-if="about.update">
      <el-form label-width="auto">
        <el-form-item label="版本号">
          <a :href="`https://github.com/wushuo894/ani-rss/releases/tag/v${about.latest}`"
             target="_blank">{{ about.latest }}</a>
        </el-form-item>
        <el-form-item label="发布时间">
          {{ about.date }}
        </el-form-item>
        <el-form-item label="更新内容">
          <el-scrollbar style="margin-bottom: 16px;max-height: 400px;" :always="true">
            <div class="markdown-body" style="width: 800px;" v-html="md.render(about.markdownBody)"></div>
            <el-alert
                show-icon
                :closable="false"
                style="margin-top: 8px;"
                title="更新依赖于Github, 需要网络环境支持"
                type="info"
            />
          </el-scrollbar>
        </el-form-item>
      </el-form>
    </div>
    <div v-else>
      <el-empty description="无更新"></el-empty>
    </div>
    <div style="width: 100%;justify-content: space-between;" class="flex">
      <el-button bg text icon="Tickets"
                 @click="openUrl('https://docs.wushuo.top/history')"
                 type="primary">
        更新历史
      </el-button>
      <div>
        <el-button :disabled="!about.update" bg text icon="Check"
                   type="success" @click="update">
          开始更新
        </el-button>
        <el-button bg icon="Close" text @click="dialogVisible = false">取消</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {onMounted, ref} from "vue";
import api from "@/js/api.js";
import {ElMessage, ElText} from "element-plus";
import Popconfirm from "@/other/Popconfirm.vue";
import {Book, Github, Telegram} from "@vicons/fa";

import markdownit from 'markdown-it'
import MarkdownItGitHubAlerts from 'markdown-it-github-alerts'
import 'markdown-it-github-alerts/styles/github-colors-light.css'
import 'markdown-it-github-alerts/styles/github-colors-dark-media.css'
import 'markdown-it-github-alerts/styles/github-base.css'

let md = markdownit({
  html: true,
  linkify: true
})

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  token.attrSet('target', '_blank') // 强制添加属性
  return self.renderToken(tokens, idx, options)
}

md.use(MarkdownItGitHubAlerts)

const actionLoading = ref(false)

const stop = (status) => {
  actionLoading.value = true
  api.post("api/stop?status=" + status)
      .then(res => {
        ElMessage.success(res.message)
        setTimeout(() => {
          localStorage.removeItem("authorization")
          location.reload()
        }, 5000)
      })
      .finally(() => {
        actionLoading.value = false
      })
}

const update = () => {
  actionLoading.value = true
  api.post("api/update")
      .then(res => {
        ElMessage.success(res.message)
        setTimeout(() => {
          localStorage.removeItem("authorization")
          location.reload()
        }, 5000)
      })
      .finally(() => {
        actionLoading.value = false
      })
}

const about = ref({
  'version': '',
  'latest': '',
  'update': false,
  'markdownBody': ''
})

onMounted(() => {
  api.get('api/about')
      .then(res => {
        about.value = res.data
      })
})

let logout = () => {
  localStorage.removeItem('authorization')
  location.reload()
}

let openUrl = (url) => window.open(url)

let dialogVisible = ref(false)
let props = defineProps(['config'])

</script>

<style>
#button-list > button {
  margin-top: 12px;
  margin-left: 0;
}

#button-list > button {
  margin-right: 12px;
}

#button-list > button:last-child {
  margin-right: 0;
}

</style>
