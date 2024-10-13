<template>
  <el-form label-width="auto"
           style="width: 100%"
           @submit="(event)=>{
                    event.preventDefault()
                   }">
    <el-form-item label="下载工具">
      <el-select v-model:model-value="props.config.download">
        <el-option v-for="item in downloadSelect"
                   :key="item"
                   :label="item"
                   :value="item"/>
      </el-select>
    </el-form-item>
    <el-form-item label="地址">
      <el-input v-model:model-value="props.config.host" placeholder="http://192.168.1.x:8080"></el-input>
    </el-form-item>
    <template v-if="props.config.download !== 'Aria2'">
      <el-form-item label="用户名">
        <el-input v-model:model-value="props.config.username" placeholder="username"></el-input>
      </el-form-item>
      <el-form-item label="密码">
        <el-input show-password v-model:model-value="props.config.password" placeholder="password"></el-input>
      </el-form-item>
    </template>
    <el-form-item label="RPC 密钥" v-else>
      <el-input show-password v-model:model-value="props.config.password" placeholder=""></el-input>
    </el-form-item>
    <el-form-item>
      <div style="display:flex;width: 100%;justify-content: end;">
        <el-button @click="downloadLoginTest" bg text :loading="downloadLoginTestLoading" icon="Odometer">测试
        </el-button>
      </div>
    </el-form-item>
    <el-form-item label="保存位置">
      <el-input v-model:model-value="props.config.downloadPath" placeholder="/Media/anime"></el-input>
    </el-form-item>
    <el-form-item label="剧场版保存位置">
      <el-input v-model:model-value="props.config.ovaDownloadPath" placeholder="/Media/ova"></el-input>
    </el-form-item>
    <el-form-item label="自动删除">
      <div>
        <el-switch v-model:model-value="props.config.delete"></el-switch>
        <br>
        <el-text class="mx-1" size="small">
          自动删除已完成的任务, 不会删除本地文件
          <br>
          如果同时开启了 <strong>备用RSS</strong> 将在 <strong>主RSS</strong> 删除 <strong>备用RSS</strong> 所下载的视频
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="拼音首字母">
      <div>
        <el-switch v-model:model-value="props.config.acronym" :disabled="props.config.quarter"></el-switch>
        <br>
        <el-text class="mx-1" size="small">
          存放到 #,0,A-Z 文件夹下
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="季度">
      <div>
        <el-switch v-model:model-value="props.config.quarter" :disabled="props.config.acronym"></el-switch>
        <br>
        <el-text class="mx-1" size="small">
          按季度存放, 如 2024-07
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="同时下载数量限制">
      <div>
        <el-input-number v-model:model-value="props.config.downloadCount" min="0"></el-input-number>
        <div>
          设置为时 0 不做限制, 在 做种 完成后添加新任务
        </div>
      </div>
    </el-form-item>
    <el-form-item label="检测是否死种">
      <el-switch v-model:model-value="props.config.watchErrorTorrent"/>
    </el-form-item>
    <el-form-item label="修改任务标题">
      <el-switch v-model:model-value="props.config.qbRenameTitle" :disabled="config.download !== 'qBittorrent'"/>
    </el-form-item>
    <el-form-item label="qb保存路径">
      <div>
        <el-switch v-model:model-value="props.config.qbUseDownloadPath" :disabled="config.download !== 'qBittorrent'"/>
        <br>
        <el-text class="mx-1" size="small">
          开启后将使用qBittorrent的临时下载位置 (最终下载位置不受影响)
        </el-text>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>


import {ref} from "vue";
import api from "../api.js";
import {ElMessage} from "element-plus";

const downloadSelect = ref([
  'qBittorrent',
  'Transmission',
  'Aria2'
])

const downloadLoginTestLoading = ref(false)
const downloadLoginTest = () => {
  downloadLoginTestLoading.value = true
  api.post("api/downloadLoginTestLoading", props.config)
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        downloadLoginTestLoading.value = false
      })
}

let props = defineProps(['config'])
</script>
