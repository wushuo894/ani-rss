<template>
  <el-form label-width="auto"
           style="width: 100%"
           @submit="(event)=>{
                    event.preventDefault()
                   }">
    <el-form-item label="只下载最新集">
      <el-switch v-model:model-value="props.config.downloadNew"/>
    </el-form-item>
    <el-form-item label="标题添加年份">
      <el-switch v-model:model-value="props.config.titleYear"/>
    </el-form-item>
    <el-form-item label="自动剧集偏移">
      <el-switch v-model:model-value="props.config.offset"/>
    </el-form-item>
    <el-form-item label="BGM日文标题">
      <el-switch v-model:model-value="props.config['bgmJpName']"/>
    </el-form-item>
    <el-form-item label="TMDB ID">
      <div>
        <el-switch v-model:model-value="props.config.tmdbId"/>
        <br>
        <el-text class="mx-1" size="small">
          自动获取tmdbId, 如: 女仆冥土小姐。 [tmdbid=242143]
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="TMDB标题">
      <div>
        <el-switch v-model:model-value="props.config.tmdb"/>
        <br>
        <el-text class="mx-1" size="small">
          自动使用TMDB的标题
        </el-text>
        <br>
        <el-checkbox v-model="props.config['tmdbAnime']" label="仅获取动漫"/>
      </div>
    </el-form-item>
    <el-form-item label="TMDB语言">
      <div>
        <div>
          <el-select v-model:model-value="props.config['tmdbLanguage']" style="width: 150px;">
            <el-option v-for="item in tmdb_i18n" :value="item.i18n_tag"
                       :label="`${item.native_name} (${item.i18n_tag})`"
                       :key="item.i18n_tag">
              <span style="float: left">{{ item.native_name }}</span>
              <el-text type="info" style="float: right;" size="small">
                {{ item.i18n_tag }}
              </el-text>
            </el-option>
          </el-select>
        </div>
        <div>
          <el-checkbox v-model="props.config['tmdbRomaji']" label="优先获取罗马音"/>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="tvshow.nfo">
      <div>
        <el-switch v-model:model-value="props.config['tvShowNfo']"/>
        <br>
        <el-text class="mx-1" size="small">
          自动生成 <strong>tvshow.nfo</strong> 文件, 内包含 <strong>tmdbid</strong> 更方便 <strong>emby</strong>
          识别
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="开启全局排除">
      <el-switch v-model:model-value="props.config.enabledExclude" :disabled="props.config.importExclude"/>
    </el-form-item>
    <el-form-item label="导入全局排除">
      <el-switch v-model:model-value="props.config.importExclude" :disabled="props.config.enabledExclude"/>
    </el-form-item>
    <el-form-item label="封面质量">
      <el-select v-model:model-value="props.config['bgmImage']" style="width: 150px;">
        <el-option v-for="item in ['small','grid','large','medium','common']" :key="item"
                   :value="item"></el-option>
      </el-select>
    </el-form-item>
    <el-form-item label="自定义集数规则">
      <div style="width: 100%;">
        <div>
          <el-switch v-model:model-value="props.config.customEpisode"/>
        </div>
        <div style="display: flex;width: 100%;">
          <el-input v-model:model-value="props.config.customEpisodeStr"
                    style="width: 100%"/>
          <div style="width: 4px;"></div>
          <el-input-number v-model:model-value="props.config.customEpisodeGroupIndex"/>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="自动上传">
      <el-switch v-model:model-value="props.config['upload']"/>
    </el-form-item>
  </el-form>
</template>

<script setup>
import {ElText} from "element-plus";
import {tmdb_i18n} from "@/js/tmdb-i18n.js";

let props = defineProps(['config'])
</script>