<template>
  <Items ref="items"/>
  <el-dialog v-model="editDialogVisible" title="修改订阅" center>
    <el-form label-width="auto"
             @submit="(event)=>{
                event.preventDefault()
             }"
    >
      <el-form-item label="标题">
        <div style="width: 100%;">
          <div>
            <el-input v-model:model-value="ani.title"></el-input>
          </div>
          <div style="width: 100%;justify-content: end;display: flex;margin-top: 8px;"
               v-if="ani.title !== ani.themoviedbName && ani.themoviedbName.length">
            <el-button @click="ani.title = ani.themoviedbName" bg text>使用TMDB</el-button>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="TMDB">
        <div style="display: flex;width: 100%;justify-content: space-between;">
          <el-input v-model:model-value="ani.themoviedbName" disabled/>
          <div style="width: 4px;"></div>
          <el-button icon="Refresh" bg text @click="getThemoviedbName" :loading="getThemoviedbNameLoading"/>
        </div>
      </el-form-item>
      <el-form-item label="季">
        <div style="display: flex;justify-content: end;width: 100%;">
          <el-input-number style="max-width: 200px" :min="0" v-model:model-value="ani.season"
                           :disabled="ani.ova"></el-input-number>
        </div>
      </el-form-item>
      <el-form-item label="集数偏移">
        <div style="display: flex;justify-content: end;width: 100%;">
          <el-input-number v-model:model-value="ani.offset" :disabled="ani.ova"></el-input-number>
        </div>
      </el-form-item>
      <el-form-item label="排除">
        <Exclude ref="exclude" v-model:exclude="ani.exclude"/>
      </el-form-item>
      <el-form-item label="全局排除">
        <el-switch v-model:model-value="ani['globalExclude']"/>
      </el-form-item>
      <el-form-item label="剧场版">
        <el-switch v-model:model-value="ani.ova"></el-switch>
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model:model-value="ani.enable"></el-switch>
      </el-form-item>
      <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;">
        <el-button @click="items.show(ani)" bg text>预览</el-button>
        <el-button :loading="editAniButtonLoading" @click="editAni" text bg>确定</el-button>
      </div>
    </el-form>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import {ElMessage} from "element-plus";
import api from "../api.js";
import Exclude from "../config/Exclude.vue";
import Items from "./Items.vue";

let exclude = ref()
const items = ref()

const editDialogVisible = ref(false)
const ani = ref({
  'url': '',
  'season': 1,
  'offset': 0,
  'title': '',
  'themoviedbName': '',
  'exclude': [],
  'enable': true,
  'ova': false
})

const editAniButtonLoading = ref(false)

const editAni = () => {
  editAniButtonLoading.value = true
  api.put('/api/ani', ani.value)
      .then(res => {
        ElMessage.success(res.message)
        emit('load')
        editDialogVisible.value = false
      })
      .finally(() => {
        editAniButtonLoading.value = false
      })
}


const showEdit = (item) => {
  editDialogVisible.value = true
  ani.value = JSON.parse(JSON.stringify(item))
  exclude.value?.init()
}

let getThemoviedbNameLoading = ref(false)

let getThemoviedbName = () => {
  if (!ani.value.title.length) {
    return
  }

  getThemoviedbNameLoading.value = true
  api.get("/api/tmdb?method=getThemoviedbName&name=" + ani.value.title)
      .then(res => {
        ElMessage.success(res.message)
        ani.value.themoviedbName = res.data
      })
      .finally(() => {
        getThemoviedbNameLoading.value = false
      })
}

defineExpose({
  showEdit
})

const emit = defineEmits(['load'])

</script>
