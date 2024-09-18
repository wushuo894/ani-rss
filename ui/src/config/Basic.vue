<template>
  <div>
    <el-form label-width="auto"
             @submit="(event)=>{
                      event.preventDefault()
                   }">
      <el-form-item label="Mikan">
        <el-input v-model:model-value="props.config.mikanHost" placeholder="https://mikanime.tv"/>
      </el-form-item>
      <el-form-item label="季命名">
        <el-select v-model:model-value="props.config.seasonName" style="width: 150px">
          <el-option :value="it" :key="it" :label="it" v-for="it in ['Season 1','S01','None']"/>
        </el-select>
      </el-form-item>
      <el-form-item label="RSS开关">
        <el-switch v-model:model-value="props.config.rss"/>
      </el-form-item>
      <el-form-item label="RSS间隔(分钟)">
        <el-input-number v-model:model-value="props.config.sleep" :min="1" :disabled="!props.config.rss"/>
      </el-form-item>
      <el-form-item label="自动重命名">
        <el-switch v-model:model-value="props.config.rename"/>
      </el-form-item>
      <el-form-item label="重命名间隔(分钟)">
        <el-input-number v-model:model-value="props.config.renameSleep" min="1"
                         :disabled="!config.rename"/>
      </el-form-item>
      <el-form-item label="自动跳过">
        <div style="width: 100%">
          <el-switch v-model:model-value="props.config.fileExist" :disabled="!config.rename"/>
          <br>
          <el-text class="mx-1" size="small">
            文件已下载自动跳过 此选项必须启用 自动重命名。确保 下载工具 与本程序 docker 映射挂载路径一致
            <a href="https://docs.wushuo.top/docs#%E8%87%AA%E5%8A%A8%E8%B7%B3%E8%BF%87"
               target="_blank">详细说明</a>
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="自动推断剧集偏移">
        <el-switch v-model:model-value="props.config.offset"/>
      </el-form-item>
      <el-form-item label="TMDB">
        <div>
          <el-switch v-model:model-value="props.config.tmdb"/>
          <br>
          <el-text class="mx-1" size="small">
            自动使用TMDB的标题
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="自动禁用订阅">
        <div>
          <el-switch v-model:model-value="props.config.autoDisabled"/>
          <br>
          <el-text class="mx-1" size="small">
            根据 Bangumi 获取总集数 当所有集数都已下载时自动禁用该订阅
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="标题添加年份">
        <el-switch v-model:model-value="props.config.titleYear"/>
      </el-form-item>
      <el-form-item label="自动跳过X.5集">
        <el-switch v-model:model-value="props.config.skip5"/>
      </el-form-item>
      <el-form-item label="备用RSS">
        <div>
          <el-switch v-model:model-value="props.config.backRss"/>
          <div style="display: flex;width: 100%;justify-content: end;padding-top: 6px;">
            <el-text class="mx-1" size="small">
              请勿将动漫花园的rss用作 <strong>其他</strong> rss的备用或主RSS
              <br>
              使用备用rss请同时开启qb的 <strong>修改任务标题</strong>, 对tr兼容性 <strong>不太稳定</strong>, 且此功能
              <strong>不支持
                aria2</strong>
              <br>
              若开启了 <strong>自动删除</strong> 将会 <strong>自动替换</strong> 备用rss 为 主rss 版本
              (需要映射路径与下载器一致，否则若旧视频为mp4新视频为mkv时无法完成自动删除旧视频)
            </el-text>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="IP白名单">
        <div style="width: 100%;">
          <div>
            <el-switch v-model:model-value="config['ipWhitelist']"/>
          </div>
          <div style="width: 100%;">
            <el-input style="width: 100%" type="textarea"
                      :disabled="!config['ipWhitelist']"
                      :placeholder="'127.0.0.1\n192.168.1.0/24'" v-model:model-value="config['ipWhitelistStr']"/>
            <br>
            <el-text class="mx-1" size="small">
              对IP白名单跳过身份验证, 换行可填写多个
            </el-text>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="DEBUG">
        <el-switch v-model:model-value="props.config.debug"/>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import {ElText} from "element-plus";

let props = defineProps(['config'])
</script>