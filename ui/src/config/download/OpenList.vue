<template>
  <el-form-item label="Host">
    <el-input v-model:model-value="props.config['alistHost']" placeholder="http://127.0.0.1:5244"/>
  </el-form-item>
  <el-form-item label="Token">
    <el-input v-model:model-value="props.config['alistToken']" placeholder="openlist-xxxxxx"/>
  </el-form-item>
  <el-form-item label="上传位置">
    <el-input v-model:model-value="props.config['alistPath']" placeholder="/"/>
  </el-form-item>
  <el-form-item label="剧场版上传位置">
    <el-input v-model:model-value="props.config['alistOvaPath']" placeholder="/"/>
  </el-form-item>
  <el-form-item label="失败重试次数">
    <el-input-number v-model:model-value="props.config['alistRetry']" :max="100" :min="1"/>
  </el-form-item>
  <el-form-item label="上传开关">
    <div style="width: 100%">
      <div>
        <el-switch v-model="props.config['alist']"/>
      </div>
      <div>
        <el-checkbox v-model="props.config['alistTask']" :disabled="!props.config['alist']" label="添加为任务"/>
      </div>
      <div>
        <el-text class="mx-1" size="small">
          自动将下载完成的文件上传至 OpenList
        </el-text>
      </div>
    </div>
  </el-form-item>
  <el-form-item label="刷新开关">
    <div style="width: 100%">
      <div>
        <el-switch v-model:model-value="props.config['alistRefresh']"/>
      </div>
      <div>
        <el-text class="mx-1" size="small">
          刷新 OpenList 上传路径的文件列表
        </el-text>
      </div>
    </div>
  </el-form-item>
  <el-form-item label="刷新延迟">
    <el-input-number v-model="props.config['alistRefreshDelayed']" :min="0">
      <template #suffix>
        <span>秒</span>
      </template>
    </el-input-number>
  </el-form-item>
</template>

<script setup>
let props = defineProps(['config'])
</script>
