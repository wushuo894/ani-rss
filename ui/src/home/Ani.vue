<template>
  <Items ref="items"/>
  <BackRss ref="backRss" :ani="props.ani"/>
  <Mikan ref="mikanRef" @add="args => {
    ani.subgroup = args.group
    ani.url = args.url
  }"/>
  <div style="height: 500px;">
    <el-scrollbar style="padding: 0 12px;" height="500px;" ref="scrollbar">
      <el-form label-width="auto"
               @submit="(event)=>{
                event.preventDefault()
             }"
      >
        <el-form-item label="标题">
          <div style="width: 100%;">
            <div>
              <el-input v-model:model-value="props.ani.title"/>
            </div>
            <div style="width: 100%;justify-content: end;display: flex;margin-top: 12px;">
              <el-button @click="props.ani.title = ani.themoviedbName"
                         icon="DocumentAdd"
                         :disabled="props.ani.title === ani.themoviedbName || !ani.themoviedbName.length" bg text>
                使用TMDB
              </el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="TMDB">
          <div style="display: flex;width: 100%;justify-content: space-between;">
            <el-input v-model:model-value="props.ani.themoviedbName" disabled/>
            <div style="width: 4px;"></div>
            <el-button icon="Refresh" bg text @click="getThemoviedbName" :loading="getThemoviedbNameLoading"/>
          </div>
        </el-form-item>
        <el-form-item label="bgmUrl">
          <el-input v-model:model-value="ani.bgmUrl" placeholder="https://xxx.xxx"/>
        </el-form-item>
        <el-form-item label="主 RSS">
          <div style="width: 100%;display: flex;">
            <el-input v-model:model-value="ani.subgroup" style="width: 140px" placeholder="字幕组"/>
            <div style="width: 6px;"></div>
            <el-input v-model:model-value="ani.url" placeholder="https://xxx.xxx"/>
            <div style="width: 6px;"></div>
            <el-button bg text @click="mikanRef?.show" icon="VideoCamera"/>
          </div>
        </el-form-item>
        <el-form-item label="备用 RSS">
          <div style="display: flex;justify-content: end;width: 100%;">
            <el-button text bg @click="backRss?.show" icon="EditPen">管理</el-button>
          </div>
        </el-form-item>
        <el-form-item label="日期">
          <div style="display: flex;width: 100%;justify-content: end;">
            <el-date-picker
                style="max-width: 150px;"
                v-model="date"
                @change="dateChange"
            />
          </div>
        </el-form-item>
        <el-form-item label="季">
          <div style="display: flex;justify-content: end;width: 100%;">
            <el-input-number style="max-width: 200px" :min="0" v-model:model-value="props.ani.season"
                             :disabled="props.ani.ova"/>
          </div>
        </el-form-item>
        <el-form-item label="集数偏移">
          <div style="display: flex;justify-content: end;width: 100%;">
            <el-input-number v-model:model-value="props.ani.offset" :disabled="props.ani.ova"/>
          </div>
        </el-form-item>
        <el-form-item label="总集数">
          <div style="display: flex;justify-content: end;width: 100%;">
            <el-input-number v-model:model-value="props.ani.totalEpisodeNumber"/>
          </div>
        </el-form-item>
        <el-form-item label="匹配">
          <Exclude ref="match" v-model:exclude="props.ani.match" :import-exclude="false"/>
        </el-form-item>
        <el-form-item label="排除">
          <Exclude ref="exclude" v-model:exclude="props.ani.exclude" :import-exclude="true"/>
        </el-form-item>
        <el-form-item label="全局排除">
          <el-switch v-model:model-value="props.ani['globalExclude']"/>
        </el-form-item>
        <el-form-item label="剧场版">
          <el-switch v-model:model-value="props.ani.ova"/>
        </el-form-item>
        <el-form-item label="自定义集数规则">
          <div style="width: 100%;">
            <div>
              <el-switch v-model:model-value="props.ani.customEpisode"/>
            </div>
            <div style="display: flex;width: 100%;">
              <el-input style="width: 100%"
                        :disabled="!props.ani.customEpisode"
                        v-model:model-value="props.ani.customEpisodeStr"/>
              <div style="width: 4px;"></div>
              <el-input-number v-model:model-value="props.ani.customEpisodeGroupIndex"
                               :disabled="!props.ani.customEpisode"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="自定义下载">
          <div style="width: 100%;">
            <div>
              <el-switch v-model:model-value="props.ani.customDownloadPath"/>
            </div>
            <div>
              <el-input type="textarea" style="width: 100%" :disabled="!props.ani.customDownloadPath"
                        v-model:model-value="props.ani.downloadPath"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="遗漏检测">
          <el-switch v-model:model-value="props.ani.omit"/>
        </el-form-item>
        <el-form-item label="只下载最新集">
          <el-switch v-model:model-value="props.ani.downloadNew"/>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model:model-value="props.ani.enable"/>
        </el-form-item>
      </el-form>
    </el-scrollbar>
  </div>
  <div style="display: flex;justify-content: space-between;width: 100%;margin-top: 10px;">
    <div>
      <popconfirm title="立即刷新订阅?" @confirm="download" v-if="props.ani.showDownlaod">
        <template #reference>
          <el-button bg text :loading="downloadLoading" icon="Refresh">刷新
          </el-button>
        </template>
      </popconfirm>
    </div>
    <div>
      <el-button @click="items.show(ani)" bg text icon="Grid">预览</el-button>
      <el-button icon="Check" :loading="okLoading" @click="async ()=>{
        okLoading = true
        emit('ok',()=>okLoading = false)
      }" text bg>确定
      </el-button>
    </div>
  </div>
</template>

<script setup>

import Exclude from "../config/Exclude.vue";
import Items from "./Items.vue";
import {onMounted, ref} from "vue";
import api from "../api.js";
import {ElMessage} from "element-plus";
import Popconfirm from "../other/Popconfirm.vue";
import BackRss from "./BackRss.vue";
import Mikan from "./Mikan.vue";

const mikanRef = ref()

let backRss = ref()
let date = ref()

let items = ref()
let okLoading = ref(false)

let getThemoviedbNameLoading = ref(false)

let getThemoviedbName = () => {
  if (!props.ani.title.length) {
    return
  }

  getThemoviedbNameLoading.value = true
  api.get("api/tmdb?method=getThemoviedbName&name=" + props.ani.title)
      .then(res => {
        ElMessage.success(res.message)
        props.ani.themoviedbName = res.data
      })
      .finally(() => {
        getThemoviedbNameLoading.value = false
      })
}

let exclude = ref()
let match = ref()

onMounted(() => {
  init()
})

let scrollbar = ref()

let init = () => {
  date.value = new Date(props.ani.year, props.ani.month - 1, props.ani.date);
  scrollbar.value?.setScrollTop(0)
}

let dateChange = () => {
  if (!date.value) {
    return
  }
  props.ani.year = date.value.getFullYear()
  props.ani.month = date.value.getMonth() + 1
  props.ani.date = date.value.getDate()
  let minYear = 1970
  if (props.ani.year < minYear) {
    props.ani.year = minYear
    init()
    ElMessage.error(`最小年份为 ${minYear}`)
  }
}


let downloadLoading = ref(false)
let download = () => {
  downloadLoading.value = true
  api.post('api/ani?download=true', props.ani)
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        downloadLoading.value = false
      })
}

let props = defineProps(['ani'])
const emit = defineEmits(['ok'])
</script>
