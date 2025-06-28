<template>
  <el-dialog v-model="jsDialogVisible" align-center center title="自定义JS" width="800">
    <el-input v-model:model-value="props.config['customJs']" :rows="6"
              type="textarea"/>
    <div class="flex" style="justify-content:end; width: 100%;margin-top: 8px;">
      <a href="https://github.com/wushuo894/ani-rss-css"
         target="_blank">更多CSS</a>
    </div>
    <div class="flex" style="justify-content:end; width: 100%;margin-top: 8px;">
      <el-button bg icon="Close" text @click="jsDialogVisible = false">关闭</el-button>
    </div>
  </el-dialog>
  <el-dialog v-model="cssDialogVisible" align-center center title="自定义CSS" width="800">
    <el-input v-model:model-value="props.config['customCss']" :rows="6"
              type="textarea"/>
    <div class="flex" style="justify-content:end; width: 100%;margin-top: 8px;">
      <el-button bg icon="Close" text @click="cssDialogVisible = false">关闭</el-button>
    </div>
  </el-dialog>
  <el-form-item label="外观">
    <el-radio-group v-model="store">
      <el-radio-button label="自动" value="auto">
        <template #default>
          <el-icon>
            <Adjust/>
          </el-icon>
        </template>
      </el-radio-button>
      <el-radio-button label="浅色" value="light">
        <el-icon>
          <Sun/>
        </el-icon>
      </el-radio-button>
      <el-radio-button label="深色" value="dark">
        <el-icon>
          <Moon/>
        </el-icon>
      </el-radio-button>
    </el-radio-group>
  </el-form-item>
  <el-form-item label="主题色">
    <el-color-picker v-model="color" :predefine="predefineColors"
                     @blur="colorChange(color)"
                     @change="colorChange(color)"
                     @active-change="colorChange"/>
  </el-form-item>
  <el-form-item label="其他">
    <el-checkbox v-model="props.config.scoreShow" label="显示评分"/>
    <el-checkbox v-model="props.config.weekShow" label="按星期展示"/>
    <el-checkbox v-model="props.config.showPlaylist" label="显示视频列表"/>
    <el-checkbox v-model="props.config.showLastDownloadTime" label="显示更新时间"/>
  </el-form-item>
  <el-form-item label="自定义">
    <el-button bg text @click="jsDialogVisible = true">
      <template #icon>
        <Js/>
      </template>
      JavaScript
    </el-button>
    <el-button bg text @click="cssDialogVisible = true">
      <template #icon>
        <Css3Alt/>
      </template>
      CSS
    </el-button>
  </el-form-item>
</template>

<script setup>

import {useColorMode, useLocalStorage} from "@vueuse/core";
import {ref} from "vue";
import {Adjust, Css3Alt, Js, Moon, Sun} from "@vicons/fa";

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

let jsDialogVisible = ref(false)
let cssDialogVisible = ref(false)

let props = defineProps(['config'])
</script>