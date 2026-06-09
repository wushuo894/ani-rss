<template>
  <el-form @submit.prevent label-width="auto"
           class="full-width">
    <el-alert v-if="!props.config['bgmEnabled'] && !props.config['tmdbEnabled']"
              type="error"
              show-icon
              :closable="false"
              class="scrape-alert">
      所有数据源已关闭，将不会执行刮削，仅按规则进行重命名和下载
    </el-alert>
    <el-form-item label="自动刮削">
      <el-switch v-model="props.config['scrape']"
                 :disabled="!props.config['bgmEnabled'] && !props.config['tmdbEnabled']"/>
    </el-form-item>
    <el-form-item label="Bangumi">
      <el-switch v-model="props.config['bgmEnabled']" @change="onDataSourceChange"/>
    </el-form-item>
    <el-form-item label="TMDB">
      <el-switch v-model="props.config['tmdbEnabled']" @change="onDataSourceChange"/>
    </el-form-item>
    <el-form-item label="Bangumi">
      <el-switch v-model="props.config['bgmEnabled']"/>
    </el-form-item>
    <el-form-item label="TMDB">
      <el-switch v-model="props.config['tmdbEnabled']"/>
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
    <el-form-item v-if="props.config['bgmEnabled']" label="更多">
      <el-checkbox label="bangumi.ini" v-model="props.config['bangumiIniEnabled']"/>
    </el-form-item>
    <template v-if="props.config['tmdbEnabled']">
      <el-form-item label="TmdbApi">
        <el-input v-model:model-value="props.config['tmdbApi']" placeholder="https://api.themoviedb.org"/>
      </el-form-item>
      <el-form-item label="TmdbApiKey">
        <el-input v-model:model-value="props.config['tmdbApiKey']" placeholder="请自备 API 密钥, 留空使用系统默认"/>
      </el-form-item>
      <el-form-item label="TmdbImage">
        <el-input v-model:model-value="props.config['tmdbImage']" placeholder="https://image.tmdb.org"/>
      </el-form-item>
    </template>
  </el-form>
</template>

<script setup>

import {ElText} from "element-plus";

let props = defineProps(['config'])

let onDataSourceChange = () => {
  if (!props.config['bgmEnabled'] && !props.config['tmdbEnabled']) {
    props.config['scrape'] = false
  } else {
    props.config['scrape'] = true
  }
}
</script>

<style scoped>
.scrape-alert {
  margin-bottom: 16px;
}
</style>
