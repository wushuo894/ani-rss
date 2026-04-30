<template>
  <el-form label-width="auto"
           class="full-width">
    <el-form-item label="Mikan">
      <el-input v-model:model-value="props.config.mikanHost" placeholder="https://mikanime.tv"/>
    </el-form-item>
    <el-form-item label="TmdbApi">
      <el-input v-model:model-value="props.config['tmdbApi']" placeholder="https://api.themoviedb.org"/>
    </el-form-item>
    <el-form-item label="TmdbApiKey">
      <el-input v-model:model-value="props.config['tmdbApiKey']" placeholder="请自备 API 密钥, 留空使用系统默认"/>
    </el-form-item>
    <el-form-item label="GithubToken">
      <div class="full-width">
        <div>
          <el-input v-model="props.config['githubToken']" clearable placeholder="在此处输入GithubToken"/>
        </div>
        <div style="justify-content: end;margin-top: 4px;" class="flex">
          <el-button :icon="Github" bg
                     @click="openUrl('https://github.com/login/oauth/authorize?client_id=Ov23li1dD89l7iGKhYa3&redirect_uri=https://github-app.wushuo.top/&scope=read:user')">
            获取GithubToken
          </el-button>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="最大日志条数">
      <div class="width-150">
        <el-select v-model:model-value="props.config.logsMax">
          <el-option v-for="it in [128,256,512]" :key="it" :label="it" :value="it"/>
        </el-select>
      </div>
    </el-form-item>
    <el-form-item label="自动更新">
      <div class="full-width">
        <div>
          <el-switch v-model:model-value="props.config.autoUpdate"/>
        </div>
        <div>
          <el-text class="mx-1" size="small">
            每天 06:00 自动更新程序
          </el-text>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="DEBUG">
      <el-switch v-model:model-value="props.config.debug"/>
    </el-form-item>
    <el-form-item label="缓存">
      <div class="full-width">
        <div>
          <el-button :loading="clearCacheLoading" bg icon="Delete" @click="clearCache">清理</el-button>
        </div>
        <div>
          <el-text class="mx-1" size="small">
            清理现在不被使用的缓存
          </el-text>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="自动备份配置">
      <div>
        <el-switch v-model="props.config['configBackup']"/>
        <br>
        <el-input-number v-model="props.config['configBackupDay']" :min="1">
          <template #suffix>
            <span>天</span>
          </template>
        </el-input-number>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>
import {ElMessage, ElText} from "element-plus";
import {ref} from "vue";
import * as http from "@/js/http.js";
import {Github} from "@vicons/fa";

let openUrl = (url) => window.open(url)

let clearCacheLoading = ref(false)
let clearCache = () => {
  clearCacheLoading.value = true
  http.clearCache()
      .then(res => {
        ElMessage.success(res.message);
      })
      .finally(() => {
        clearCacheLoading.value = false
      })
}

let props = defineProps(['config'])
</script>

