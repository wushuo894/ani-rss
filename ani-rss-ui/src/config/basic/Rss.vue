<template>
  <el-form label-width="auto"
           class="form-full-width"
           @submit="(event)=>{
                    event.preventDefault()
                   }">
    <el-form-item label="RSS开关">
      <el-switch v-model:model-value="props.config.rss"/>
    </el-form-item>
    <el-form-item label="RSS间隔">
      <el-input-number v-model:model-value="props.config.sleep" :disabled="!props.config.rss" :min="5">
        <template #suffix>
          <span>分钟</span>
        </template>
      </el-input-number>
    </el-form-item>
    <el-form-item label="RSS超时">
      <el-input-number v-model:model-value="props.config['rssTimeout']"
                       :max="60" :min="6">
        <template #suffix>
          <span>秒</span>
        </template>
      </el-input-number>
    </el-form-item>
    <el-form-item label="自动跳过">
      <div class="full-width">
        <el-switch v-model:model-value="props.config.fileExist" :disabled="!config.rename"/>
        <br>
        <el-text class="mx-1" size="small">
          文件已下载自动跳过 此选项必须启用 自动重命名。确保 下载工具 与本程序 docker 映射挂载路径一致
          &nbsp;
          <el-link
              class="text-extra-small"
              type="primary"
              href="https://docs.wushuo.top/config/basic/rss#auto-skip"
              target="_blank">
            详细说明
          </el-link>
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="自动禁用订阅">
      <div class="full-width">
        <el-switch v-model:model-value="props.config.autoDisabled"/>
        <br>
        <el-text class="mx-1" size="small">
          根据 Bangumi 获取总集数 当所有集数都已下载时自动禁用该订阅
        </el-text>
        <div>
          <el-checkbox v-model="props.config['completed']"
                       :disabled="!props.config['verifyExpirationTime'] || !props.config.autoDisabled"
                       label="订阅完结迁移"/>
        </div>
        <div>
          <el-input v-model="props.config['completedPathTemplate']"
                    :disabled="!props.config.autoDisabled || !props.config['completed']"/>
        </div>
        <AfdianPrompt :config="props.config" name="订阅完结迁移"/>
      </div>
    </el-form-item>
    <el-form-item label="自动更新总集数">
      <div class="full-width">
        <el-switch v-model="props.config.updateTotalEpisodeNumber"/>
        <div>
          <el-checkbox v-model="props.config.forceUpdateTotalEpisodeNumber"
                       :disabled="!props.config.updateTotalEpisodeNumber"
                       class="el-checkbox-danger"
                       label="强制更新"/>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="自动跳过X.5集">
      <el-switch v-model:model-value="props.config.skip5"/>
    </el-form-item>
    <el-form-item label="遗漏检测">
      <div>
        <div>
          <el-switch v-model:model-value="props.config.omit"/>
        </div>
        <el-text class="mx-1" size="small">
          总开关 若检测到RSS中集数出现遗漏会发送通知
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="摸鱼检测">
      <div>
        <div>
          <el-switch v-model="props.config['procrastinating']"/>
        </div>
        <div>
          <el-input-number v-model="props.config['procrastinatingDay']"
                           :disabled="!props.config['procrastinating']" :max="365"
                           :min="7">
            <template #suffix>
              <span>天</span>
            </template>
          </el-input-number>
        </div>
        <el-checkbox
            :disabled="!props.config['procrastinating']"
            v-model="props.config.procrastinatingMasterOnly"
            label="仅启用主RSS摸鱼检测"/>
        <br>
        <el-text class="mx-1" size="small">
          检测到主RSS更新摸鱼会发送通知<br>
          建议配合 <strong>自动禁用订阅</strong> 食用
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="备用RSS">
      <div style="width: 100%">
        <div>
          <el-switch v-model:model-value="props.config.standbyRss"/>
        </div>
        <div>
          <el-checkbox v-model="props.config['coexist']" :disabled="!props.config.standbyRss"
                       label="多字幕组共存模式"/>
          <el-checkbox v-model="props.config['copyMasterToStandby']" :disabled="!props.config.standbyRss"
                       label="添加订阅时自动复制主rss至备用rss"/>
        </div>
        <div class="flex full-width justify-end">
          <el-link
              type="primary"
              href="https://docs.wushuo.top/config/basic/rss#back-rss"
              target="_blank">
            详细说明
          </el-link>
        </div>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>
import {ElText} from "element-plus";
import AfdianPrompt from "@/other/AfdianPrompt.vue";

let props = defineProps(['config'])
</script>

<style scoped>
.form-full-width {
  width: 100%;
}

.full-width {
  width: 100%;
}

.text-extra-small {
  font-size: var(--el-font-size-extra-small);
}

.justify-end {
  justify-content: end;
}
</style>
