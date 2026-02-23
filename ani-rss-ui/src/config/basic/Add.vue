<template>
  <el-form label-width="auto"
           class="form-full-width"
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
    <el-form-item label="BGM日语标题">
      <el-switch v-model:model-value="props.config['bgmJpName']"/>
    </el-form-item>
    <el-form-item label="TMDB ID">
      <div>
        <el-switch v-model="props.config.tmdbId"/>
        <br>
        <el-checkbox
            :disabled="!props.config.tmdbId"
            v-model="props.config.tmdbIdPlexMode"
            label="Plex Mode"
        />
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
          <el-select v-model:model-value="props.config['tmdbLanguage']" class="select-width-150">
            <el-option v-for="item in tmdb_i18n" :value="item.i18n_tag"
                       :label="`${item.native_name} (${item.i18n_tag})`"
                       :key="item.i18n_tag">
              <span class="float-left">{{ item.native_name }}</span>
              <el-text type="info" class="float-right" size="small">
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
    <el-form-item label="自动刮削">
      <el-switch v-model:model-value="props.config['scrape']"/>
    </el-form-item>
    <el-form-item label="开启全局排除">
      <el-switch v-model:model-value="props.config.enabledExclude" :disabled="props.config.importExclude"/>
    </el-form-item>
    <el-form-item label="导入全局排除">
      <el-switch v-model:model-value="props.config.importExclude" :disabled="props.config.enabledExclude"/>
    </el-form-item>
    <el-form-item label="封面质量">
      <el-select v-model="props.config['bgmImage']" class="select-width-150">
        <el-option v-for="item in ['small','grid','large','medium','common']" :key="item"
                   :value="item"></el-option>
      </el-select>
    </el-form-item>
    <el-form-item label="自定义集数规则">
      <div class="full-width">
        <div>
          <el-switch v-model="props.config.customEpisode"/>
        </div>
        <div class="flex-row-full">
          <el-input v-model="props.config.customEpisodeStr"
                    class="full-width"/>
          <div class="spacer-4"></div>
          <el-input-number v-model="props.config.customEpisodeGroupIndex"/>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="自动上传">
      <el-switch v-model="props.config['upload']"/>
    </el-form-item>
    <el-form-item label="自动替换">
      <div>
        <div>
          <el-switch v-model="props.config['replace']"/>
        </div>
        <el-text size="small">
          重名的订阅将允许被替换
        </el-text>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>
import {ElText} from "element-plus";
import {tmdb_i18n} from "@/js/tmdb-i18n.js";

let props = defineProps(['config'])
</script>

<style scoped>
.form-full-width {
  width: 100%;
}

.select-width-150 {
  width: 150px;
}

.float-left {
  float: left;
}

.float-right {
  float: right;
}

.full-width {
  width: 100%;
}

.flex-row-full {
  display: flex;
  width: 100%;
}

.spacer-4 {
  width: 4px;
}
</style>
