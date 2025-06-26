<template>
  <el-form-item label="外观">
    <el-radio-group v-model="store">
      <el-radio-button label="自动" value="auto"/>
      <el-radio-button label="浅色" value="light"/>
      <el-radio-button label="深色" value="dark"/>
    </el-radio-group>
  </el-form-item>
  <el-form-item label="主题色">
    <el-color-picker v-model="color" :predefine="predefineColors"
                     @blur="colorChange(color)"
                     @change="colorChange(color)"
                     @active-change="colorChange"/>
  </el-form-item>
  <el-form-item label="按星期展示">
    <el-switch v-model:model-value="props.config.weekShow"/>
  </el-form-item>
  <el-form-item label="显示评分">
    <el-switch v-model:model-value="props.config.scoreShow"/>
  </el-form-item>
  <el-form-item label="显示视频列表">
    <el-switch v-model:model-value="props.config.showPlaylist"/>
  </el-form-item>
  <el-form-item label="显示更新时间">
    <el-switch v-model:model-value="props.config['showLastDownloadTime']"/>
  </el-form-item>
  <el-form-item label="自定义CSS">
    <div style="width: 100%;">
      <el-input v-model:model-value="props.config['customCss']" :autosize="{ minRows: 2, maxRows: 4 }"
                placeholder="" type="textarea"/>
      <br>
      <div class="flex" style="justify-content:end; width: 100%;">
        <a href="https://github.com/wushuo894/ani-rss-css"
           target="_blank">更多CSS</a>
      </div>
    </div>
  </el-form-item>
  <el-form-item label="自定义JS">
    <el-input v-model:model-value="props.config['customJs']" :autosize="{ minRows: 2, maxRows: 4 }"
              placeholder="" type="textarea"/>
  </el-form-item>
</template>

<script setup>

import {useColorMode, useLocalStorage} from "@vueuse/core";
import {ref} from "vue";

const {store} = useColorMode()

let predefineColors = ref([
  '#409eff', '#109D58', '#BF3545', '#CB7574',
  '#9AAEC7', '#2EC5B6', '#1C1C1C', '#F7B1A9',
  '#B18874', '#E9BA86', '#F68F6C', '#F0458B',
  '#C35653', '#40494E', '#6F0000', '#8D3647',
  '#E6C5D0', '#2377B3', '#49312D', '#7C9AB6',
  '#A5B18D', '#E8662A', '#AB5D50'
])

let color = useLocalStorage('--el-color-primary', '#409eff')

let colorChange = (v) => {
  // document.documentElement 是全局变量时
  const el = document.documentElement
  // const el = document.getElementById('xxx')

  // 获取 css 变量
  getComputedStyle(el).getPropertyValue(`--el-color-primary`)

  // 设置 css 变量
  el.style.setProperty('--el-color-primary', v)
}

let props = defineProps(['config'])
</script>