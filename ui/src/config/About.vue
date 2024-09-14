<template>
  <el-form label-width="auto"
           @submit="(event)=>{
                      event.preventDefault()
                   }">
    <el-form-item label="GitHub">
      <a href="https://github.com/wushuo894/ani-rss" target="_blank">https://github.com/wushuo894/ani-rss</a>
    </el-form-item>
    <el-form-item label="使用文档">
      <a href="https://docs.wushuo.top" target="_blank">https://docs.wushuo.top</a>
    </el-form-item>
    <el-form-item label="投喂作者">
      <a href="https://afdian.com/a/wushuo894" target="_blank">https://afdian.com/a/wushuo894</a>
    </el-form-item>
    <el-form-item label="版本号">
      <div>
        <div v-loading="about.version.length < 1" style="min-height: 100px;margin-bottom: 16px;">
          v{{ about.version }}
          <div v-if="about.update">
            <a href="https://github.com/wushuo894/ani-rss/releases/latest" target="_blank">有更新 v{{
                about.latest
              }}</a>
            <div v-if="about.markdownBody" v-html="about.markdownBody"></div>
          </div>
        </div>
        <div v-loading.fullscreen.lock="actionLoading" id="menu">
          <el-badge class="item" v-if="about.update" value="new">
            <el-button type="success" @click="update" text bg icon="Top">更新</el-button>
          </el-badge>
          <div style="margin: 6px;" v-if="about.update"></div>
          <el-popconfirm title="你确定重启吗?" @confirm="stop(0)">
            <template #reference>
              <el-button type="warning" text bg icon="RefreshRight">重启 ani-rss</el-button>
            </template>
            <template #actions="{ confirm, cancel }">
              <el-button size="small" @click="cancel" bg text icon="Close">取消</el-button>
              <div style="margin: 4px;"></div>
              <el-button
                  type="danger"
                  size="small"
                  @click="confirm"
                  bg text
                  icon="Check"
              >
                确定
              </el-button>
            </template>
          </el-popconfirm>
          <div style="margin: 6px;"></div>
          <el-popconfirm title="你确定关闭吗?" @confirm="stop(1)">
            <template #reference>
              <el-button type="danger" text bg icon="SwitchButton">关闭 ani-rss</el-button>
            </template>
            <template #actions="{ confirm, cancel }">
              <el-button size="small" @click="cancel" bg text icon="Close">取消</el-button>
              <div style="margin: 4px;"></div>
              <el-button
                  type="danger"
                  size="small"
                  @click="confirm"
                  bg text
                  icon="Check"
              >
                确定
              </el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>


import {onMounted, ref} from "vue";
import api from "../api.js";
import {ElMessage} from "element-plus";

const actionLoading = ref(false)

const stop = (status) => {
  actionLoading.value = true
  api.post("/api/stop?status=" + status)
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
  api.post("/api/update")
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

onMounted(()=>{
  api.get('/api/about')
      .then(res => {
        about.value = res.data
      })
})

</script>