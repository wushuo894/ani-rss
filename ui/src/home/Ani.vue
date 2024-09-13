<template>
  <Items ref="items"/>
  <el-form label-width="auto"
           @submit="(event)=>{
                event.preventDefault()
             }"
  >
    <el-form-item label="标题">
      <div style="width: 100%;">
        <div>
          <el-input v-model:model-value="props.ani.title"></el-input>
        </div>
        <div style="width: 100%;justify-content: end;display: flex;margin-top: 12px;">
          <el-button @click="props.ani.title = ani.themoviedbName"
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
    <el-form-item label="日期">
      <div style="display: flex;width: 100%;justify-content: end;">
        <el-date-picker
            style="max-width: 150px;"
            v-model="date"
            type="month"
            @change="dateChange"
        >
        </el-date-picker>
      </div>
    </el-form-item>
    <el-form-item label="季">
      <div style="display: flex;justify-content: end;width: 100%;">
        <el-input-number style="max-width: 200px" :min="0" v-model:model-value="props.ani.season"
                         :disabled="props.ani.ova"></el-input-number>
      </div>
    </el-form-item>
    <el-form-item label="集数偏移">
      <div style="display: flex;justify-content: end;width: 100%;">
        <el-input-number v-model:model-value="props.ani.offset" :disabled="props.ani.ova"></el-input-number>
      </div>
    </el-form-item>
    <el-form-item label="总集数">
      <div style="display: flex;justify-content: end;width: 100%;">
        <el-input-number v-model:model-value="props.ani.totalEpisodeNumber"></el-input-number>
      </div>
    </el-form-item>
    <el-form-item label="排除">
      <Exclude ref="exclude" v-model:exclude="props.ani.exclude" :import-exclude="true"/>
    </el-form-item>
    <el-form-item label="全局排除">
      <el-switch v-model:model-value="props.ani['globalExclude']"/>
    </el-form-item>
    <el-form-item label="剧场版">
      <el-switch v-model:model-value="props.ani.ova"></el-switch>
    </el-form-item>
    <el-form-item label="自定义下载">
      <div style="width: 100%;">
        <div>
          <el-switch v-model:model-value="props.ani.customDownloadPath"></el-switch>
        </div>
        <div>
          <el-input type="textarea" style="width: 100%" :disabled="!props.ani.customDownloadPath"
                    v-model:model-value="props.ani.downloadPath"/>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="启用">
      <el-switch v-model:model-value="props.ani.enable"></el-switch>
    </el-form-item>
    <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;">
      <el-button @click="items.show(ani)" bg text>预览</el-button>
      <el-button :loading="okLoading" @click="async ()=>{
        okLoading = true
        emit('ok',()=>okLoading = false)
      }" text bg>确定
      </el-button>
    </div>
  </el-form>
</template>

<script setup>

import Exclude from "../config/Exclude.vue";
import Items from "./Items.vue";
import {onMounted, ref} from "vue";
import api from "../api.js";
import {ElMessage} from "element-plus";

let date = ref()

let items = ref()
let okLoading = ref(false)

let getThemoviedbNameLoading = ref(false)

let getThemoviedbName = () => {
  if (!props.ani.title.length) {
    return
  }

  getThemoviedbNameLoading.value = true
  api.get("/api/tmdb?method=getThemoviedbName&name=" + props.ani.title)
      .then(res => {
        ElMessage.success(res.message)
        props.ani.themoviedbName = res.data
      })
      .finally(() => {
        getThemoviedbNameLoading.value = false
      })
}

let exclude = ref()

onMounted(() => {
  exclude.value?.init()
  init()
})

let init = ()=>{
  date.value = new Date(props.ani.year, props.ani.month - 1, 1);
}

let dateChange = () => {
  props.ani.year = date.value.getFullYear()
  props.ani.month = date.value.getMonth() + 1
  let minYear = 1970
  if (props.ani.year < minYear) {
    props.ani.year = minYear
    init()
    ElMessage.error(`最小年份为 ${minYear}`)
  }
  console.log(`${props.ani.year} / ${props.ani.month}`)
}

defineExpose({
  init
})

let props = defineProps(['ani'])
const emit = defineEmits(['ok'])
</script>
