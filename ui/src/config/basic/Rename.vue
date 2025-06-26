<template>
  <el-form-item label="自动重命名">
    <el-switch v-model:model-value="props.config.rename"/>
  </el-form-item>
  <el-form-item label="重命名间隔">
    <el-input-number v-model:model-value="props.config['renameSleepSeconds']"
                     :disabled="!config.rename"
                     :min="5">
      <template #suffix>
        <span>秒</span>
      </template>
    </el-input-number>
  </el-form-item>
  <el-form-item label="重命名模版">
    <div style="width: 100%">
      <el-input v-model:model-value="props.config.renameTemplate"
                placeholder="${title} S${seasonFormat}E${episodeFormat}"/>
      <br>
      <el-text class="mx-1" size="small">
        <a href="https://docs.wushuo.top/config/basic/rename#rename-template"
           target="_blank">详细说明</a>
      </el-text>
    </div>
  </el-form-item>
  <el-form-item label="剔除年份">
    <div>
      <el-switch v-model:model-value="props.config.renameDelYear"/>
      <br>
      <el-text class="mx-1" size="small">
        重命名时 ${title} 剔除 年份, 如 (2024)
      </el-text>
    </div>
  </el-form-item>
  <el-form-item label="剔除TMDB ID">
    <div>
      <el-switch v-model:model-value="props.config.renameDelTmdbId"/>
      <br>
      <el-text class="mx-1" size="small">
        重命名时 ${title} 剔除 tmdbid, 如 [tmdbid=242143]
      </el-text>
    </div>
  </el-form-item>
</template>

<script setup>
import {ElText} from "element-plus";

let props = defineProps(['config'])
</script>