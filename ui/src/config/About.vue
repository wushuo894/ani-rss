<template>
  <div style="display: flex;width: 100%;justify-content: center;align-items: center;flex-flow: column;">
    <div style="margin-bottom: 12px;display: flex;align-items: end;">
      <img src="../../public/icon.svg" height="80" width="80" alt="icon.svg"/>
      <div>
        <h1>ANI-RSS</h1>
        <el-text class="mx-1" size="small">
          &nbsp;v{{ props.config.version }}
        </el-text>
      </div>
    </div>
    <div style="margin-bottom: 12px;align-items: center;display: flex;">
      <div id="button-list">
        <el-button bg text type="info" @click="openUrl('https://github.com/wushuo894/ani-rss')" :icon="Github">GitHub
        </el-button>
        <el-button bg text type="info" @click="openUrl('https://docs.wushuo.top')" :icon="Book">使用文档</el-button>
        <el-button bg text type="info" @click="openUrl('https://afdian.com/a/wushuo894')" :icon="Bone">投喂作者
        </el-button>
        <el-button bg text type="info" @click="openUrl('https://t.me/ani_rss')" :icon="Telegram">TG群</el-button>
        <el-button bg text type="info"
                   :icon="Qq"
                   @click="openUrl('http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&amp;k=_EKAkxs6Ld4fWcMNAbUQzcp4tv20vjVH&amp;authKey=KG3GAsZfKQosbAWkks%2FbEj0LCGwxoeLJ3DTU0loHkGdHLqHYgJNv3%2BmSERmYt47b&amp;noverify=0&amp;group_code=171563627')">
          QQ群
        </el-button>
      </div>
    </div>
    <div v-loading.fullscreen.lock="actionLoading" style="display: flex;">
      <popconfirm title="你确定重启吗?" @confirm="stop(0)">
        <template #reference>
          <el-button type="warning" text bg icon="RefreshRight">重启</el-button>
        </template>
      </popconfirm>
      <div style="margin: 6px;"></div>
      <popconfirm title="你确定关闭吗?" @confirm="stop(1)">
        <template #reference>
          <el-button type="danger" text bg icon="SwitchButton">关闭</el-button>
        </template>
      </popconfirm>
      <div style="margin: 6px;"></div>
      <el-badge class="item" value="new" :hidden="!about.update">
        <el-button type="success" @click="dialogVisible = true" text bg icon="Top" :loading="about.version.length < 1">
          更新
        </el-button>
      </el-badge>
    </div>
  </div>
  <el-dialog title="版本更新" v-model="dialogVisible" v-if="dialogVisible" width="400" align-center center>
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
          <div v-html="about.markdownBody" style="margin-bottom: 16px;"></div>
          <el-alert
              title="更新依赖于Github, 需要网络环境支持"
              type="info"
              :closable="false"
          />
        </el-form-item>
      </el-form>
      <div style="width: 100%;justify-content: end;display: flex;">
        <el-button @click="update" text bg icon="Check" type="primary" :disabled="!about.update">确定
        </el-button>
        <el-button icon="Close" bg text @click="dialogVisible = false">取消</el-button>
      </div>
    </div>
    <div v-else>
      <el-empty description="无更新"></el-empty>
    </div>
  </el-dialog>
</template>

<script setup>
import {onMounted, ref} from "vue";
import api from "../api.js";
import {ElMessage, ElText} from "element-plus";
import Popconfirm from "../other/Popconfirm.vue";
import {Bone, Book, Github, Qq, Telegram} from "@vicons/fa";

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

let openUrl = (url) => window.open(url)

let dialogVisible = ref(false)
let props = defineProps(['config'])

</script>

<style>
#button-list > button {
  margin-top: 16px;
  margin-left: 0;
}

#button-list > button {
  margin-right: 16px;
}

#button-list > button:last-child {
  margin-right: 0;
}

</style>
