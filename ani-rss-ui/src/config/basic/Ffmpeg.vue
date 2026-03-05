<template>
  <el-form label-width="auto"
           class="form-full-width"
           @submit="(event)=>{
                    event.preventDefault()
                   }">
    <el-form-item label="归一化开关">
      <div class="full-width">
        <div>
          <el-switch v-model="props.config.ffmpegEnable"/>
        </div>
        <div>
          <el-text class="mx-1" size="small">
            开启后将使用 FFmpeg 对下载的视频进行编码归一化处理
          </el-text>
        </div>
      </div>
    </el-form-item>

    <template v-if="props.config.ffmpegEnable">
      <el-form-item label="FFmpeg 路径">
        <div class="full-width">
          <el-input v-model="props.config.ffmpegPath" placeholder="ffmpeg"/>
          <el-text class="mx-1" size="small">
            Docker 环境请确保已安装 FFmpeg, 或填写完整路径
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="FFprobe 路径">
        <el-input v-model="props.config.ffprobePath" placeholder="ffprobe"/>
      </el-form-item>
      <el-form-item label="测试">
        <el-button :loading="testLoading" bg icon="VideoCamera" @click="testFfmpeg">测试可用性</el-button>
      </el-form-item>
      <el-form-item label="缓存路径">
        <div class="full-width">
          <el-input v-model="props.config.ffmpegCachePath" placeholder="/Media/cache"/>
          <el-text class="mx-1" size="small">
            BT 下载文件的临时存放位置, 转码后输出到保存路径。请注意！如果使用docker容器，请确保容器挂载正确！
          </el-text>
        </div>
      </el-form-item>

      <el-divider content-position="left">视频编码</el-divider>
      <el-form-item label="可接受视频编码">
        <div class="full-width">
          <el-select
              v-model="props.config.ffmpegAcceptVideoCodecs"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="留空表示不检测"
              style="width: 100%">
            <el-option v-for="it in videoCodecOptions" :key="it" :label="it" :value="it"/>
          </el-select>
          <el-text class="mx-1" size="small">
            白名单: 源编码在列表中则跳过转码, 不在则转码。留空 = 不检测
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="目标视频编码">
        <div class="full-width">
          <div class="width-200">
            <el-select v-model="props.config.ffmpegVideoCodec"
                       filterable
                       allow-create
                       clearable
                       placeholder="留空保持原编码">
              <el-option v-for="it in targetVideoCodecOptions" :key="it" :label="it" :value="it"/>
            </el-select>
          </div>
          <el-text class="mx-1" size="small">
            需要转码时使用的编码。留空 = 保持原编码 (copy)
          </el-text>
        </div>
      </el-form-item>

      <el-divider content-position="left">音频编码</el-divider>
      <el-form-item label="可接受音频编码">
        <div class="full-width">
          <el-select
              v-model="props.config.ffmpegAcceptAudioCodecs"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="留空表示不检测"
              style="width: 100%">
            <el-option v-for="it in audioCodecOptions" :key="it" :label="it" :value="it"/>
          </el-select>
          <el-text class="mx-1" size="small">
            白名单: 源编码在列表中则跳过, 不在则转码。留空 = 不检测
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="目标音频编码">
        <div class="full-width">
          <div class="width-200">
            <el-select v-model="props.config.ffmpegAudioCodec"
                       filterable
                       allow-create
                       clearable
                       placeholder="留空保持原编码">
              <el-option v-for="it in targetAudioCodecOptions" :key="it" :label="it" :value="it"/>
            </el-select>
          </div>
          <el-text class="mx-1" size="small">
            需要转码时使用的编码。留空 = 保持原编码 (copy)
          </el-text>
        </div>
      </el-form-item>

      <el-divider content-position="left">容器格式</el-divider>
      <el-form-item label="可接受容器格式">
        <div class="full-width">
          <el-select
              v-model="props.config.ffmpegAcceptFormats"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="留空表示不检测"
              style="width: 100%">
            <el-option v-for="it in formatOptions" :key="it" :label="it" :value="it"/>
          </el-select>
          <el-text class="mx-1" size="small">
            白名单: 源格式在列表中则跳过, 不在则转封装。留空 = 不检测
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="目标容器格式">
        <div class="full-width">
          <div class="width-200">
            <el-select v-model="props.config.ffmpegFormat"
                       filterable
                       allow-create
                       clearable
                       placeholder="留空保持原格式">
              <el-option v-for="it in formatOptions" :key="it" :label="it" :value="it"/>
            </el-select>
          </div>
          <el-text class="mx-1" size="small">
            需要转封装时使用的格式。留空 = 保持原格式
          </el-text>
        </div>
      </el-form-item>

      <el-divider content-position="left">转码参数</el-divider>
      <el-form-item label="CRF (视频质量)">
        <div class="full-width">
          <div class="width-200">
            <el-input-number v-model="props.config.ffmpegCrf" :min="0" :max="51"/>
          </div>
          <el-text class="mx-1" size="small">
            0-51, 数值越小质量越高文件越大。建议: 18(高质量) 23(平衡) 28(小文件)
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="编码预设">
        <div class="width-200">
          <el-select v-model="props.config.ffmpegPreset">
            <el-option v-for="it in presetOptions" :key="it" :label="it" :value="it"/>
          </el-select>
        </div>
      </el-form-item>
      <el-form-item label="字幕处理">
        <div class="full-width">
          <div class="width-200">
            <el-select v-model="props.config.ffmpegSubtitleMode">
              <el-option label="保留 (copy)" value="copy"/>
              <el-option label="移除 (remove)" value="remove"/>
            </el-select>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="额外参数">
        <div class="full-width">
          <el-input v-model="props.config.ffmpegExtraArgs" placeholder="如: -map 0 -threads 4"/>
          <el-text class="mx-1" size="small">
            自定义 FFmpeg 命令行参数, 多个参数用空格分隔
          </el-text>
        </div>
      </el-form-item>

      <el-divider content-position="left">做种与任务</el-divider>
      <el-form-item label="转码后做种">
        <div class="full-width">
          <div>
            <el-switch v-model="props.config.ffmpegSeeding"/>
          </div>
          <el-text class="mx-1" size="small">
            开启: 转码后保留缓存文件继续做种。关闭: 转码完成后删除种子和缓存文件
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="轮询间隔">
        <div class="full-width">
          <el-input-number v-model="props.config.ffmpegSleepSeconds" :min="5">
            <template #suffix>
              <span>秒</span>
            </template>
          </el-input-number>
          <el-text class="mx-1" size="small">
            检测新下载完成文件的间隔时间
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="最大并发数">
        <div class="full-width">
          <el-input-number v-model="props.config.ffmpegMaxConcurrent" :min="1" :max="8"/>
          <el-text class="mx-1" size="small">
            同时进行转码的最大任务数，请根据机器性能设置。
          </el-text>
        </div>
      </el-form-item>
    </template>
  </el-form>
</template>

<script setup>
import {ElMessage, ElText} from "element-plus";
import {ref} from "vue";
import * as http from "@/js/http.js";

let props = defineProps(['config'])

let testLoading = ref(false)

let videoCodecOptions = ['h264', 'hevc', 'av1', 'vp9', 'mpeg4', 'mpeg2video']
let targetVideoCodecOptions = ['libx264', 'libx265', 'libsvtav1', 'libvpx-vp9']
let audioCodecOptions = ['aac', 'opus', 'flac', 'mp3', 'ac3', 'eac3', 'dts', 'vorbis']
let targetAudioCodecOptions = ['aac', 'libopus', 'libmp3lame', 'ac3', 'eac3', 'flac']
let formatOptions = ['mkv', 'mp4', 'avi', 'ts']
let presetOptions = ['ultrafast', 'superfast', 'veryfast', 'faster', 'fast', 'medium', 'slow', 'slower', 'veryslow']

let testFfmpeg = () => {
  testLoading.value = true
  http.ffmpegTest(props.config)
      .then(res => {
        ElMessage.success(res.message)
      })
      .catch(() => {
      })
      .finally(() => {
        testLoading.value = false
      })
}
</script>

<style scoped>
.form-full-width {
  width: 100%;
}

.full-width {
  width: 100%;
}

.width-200 {
  width: 200px;
}
</style>
