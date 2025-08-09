<template>
  <el-form label-width="auto"
           style="width: 100%"
           @submit="(event)=>{
                    event.preventDefault()
                   }">
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
        <div>
          <el-input v-model:model-value="props.config.renameTemplate"
                    placeholder="${title} S${seasonFormat}E${episodeFormat}"/>
        </div>
        <div>
          <el-alert
              v-if="!testRenameTemplate(props.config.renameTemplate)"
              style="margin-top: 8px;"
              type="warning"
              show-icon
              :closable="false"
          >
            <template #title>
              模板内至少需要保留 S${seasonFormat}E${episodeFormat} or S${season}E${episode} 否则会导致无法正常重命名
            </template>
          </el-alert>
        </div>
        <el-text class="mx-1" size="small">
          <el-link
              style="font-size: var(--el-font-size-extra-small)"
              type="primary"
              href="https://docs.wushuo.top/config/basic/rename#rename-template"
              target="_blank">详细说明
          </el-link>
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="剔除年份">
      <div>
        <el-switch v-model:model-value="props.config.renameDelYear"/>
        <br>
        <el-text class="mx-1" size="small">
          重命名时剔除 年份, 如 (2024)
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="剔除TMDB ID">
      <div>
        <el-switch v-model:model-value="props.config.renameDelTmdbId"/>
        <br>
        <el-text class="mx-1" size="small">
          重命名时剔除 tmdbid, 如 [tmdbid=242143]
        </el-text>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>
import {ElText} from "element-plus";

let testRenameTemplate = renameTemplate => {
  let test = [
    'S${season}E${episode}',
    'S${seasonFormat}E${episodeFormat}'
  ]
  for (let s of test) {
    if (renameTemplate.indexOf(s) > -1) {
      return true;
    }
  }
  return false;
}

let props = defineProps(['config'])
</script>
