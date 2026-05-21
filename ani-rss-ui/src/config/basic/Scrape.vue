<template>
  <el-form @submit.prevent label-width="auto"
           class="full-width">
    <el-form-item label="自动刮削">
      <el-switch v-model="props.config['scrape']"/>
    </el-form-item>
    <el-form-item label="追更天数">
      <div>
        <el-input-number v-model="props.config['followDay']" :min="1">
          <template #suffix>
            天
          </template>
        </el-input-number>
        <br/>
        <el-text class="mx-1" size="small">
          自动强制刮削最近更新集的元数据
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="更多">
      <el-checkbox label="bangumi.ini" v-model="props.config['bangumiIniEnabled']"/>
    </el-form-item>
  </el-form>
</template>

<script setup>

import {ElText} from "element-plus";

let props = defineProps(['config'])
</script>