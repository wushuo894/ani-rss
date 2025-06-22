<template>
  <el-dialog v-model="dialogVisible" align-center center style="max-width: 600px;" title="试用">
    <el-text class="mx-1" size="small">
      给本项目点一个 <strong>Star</strong> 并获取 <strong>GithubToken</strong>, 检测通过后可进行试用
    </el-text>
    <div style="margin: 8px 0 8px 0">
      <el-input v-model="props.config['githubToken']" clearable placeholder="在此处输入GithubToken"/>
    </div>
    <div class="flex" style="justify-content: end;width: 100%;">
      <el-button bg icon="Star" text type="info" @click="openUrl('https://github.com/wushuo894/ani-rss')">去点个Star
      </el-button>
      <el-button :icon="Github" bg text type="info"
                 @click="openUrl('https://github.com/login/oauth/authorize?client_id=Ov23li1dD89l7iGKhYa3&redirect_uri=https://github-app.wushuo.top/&scope=read:user')">
        获取GithubToken
      </el-button>
      <el-button :loading="tryOutLoading" bg icon="Select" text type="primary" @click="tryOut">开始试用</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {Github} from "@vicons/fa";
import api from "@/js/api.js";
import {ElMessage} from "element-plus";

let dialogVisible = ref(false)

let show = () => {
  dialogVisible.value = true
}

let openUrl = (url) => window.open(url)


let tryOutLoading = ref(false)

let tryOut = () => {
  tryOutLoading.value = true
  api.post('api/afdian?type=tryOut', props.config)
      .then(res => {
        ElMessage.success(res.message)
        props.config['expirationTime'] = res.data
        props.config['verifyExpirationTime'] = true
        props.config['tryOut'] = true
        dialogVisible.value = false
      })
      .finally(() => {
        tryOutLoading.value = false
      })
}

defineExpose({show})
let props = defineProps(['config'])
</script>
