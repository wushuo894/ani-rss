<template>
  <Items ref="items"/>
  <Mikan ref="mikan" @add="args => ani.url = args"/>
  <el-dialog v-model="addDialogVisible" title="添加订阅" center>
    <div v-if="showRss" @keydown.enter="getRss">
      <el-form label-width="auto"
               v-if="showRss" @keydown.enter="getRss"
               @submit="(event)=>{
                event.preventDefault()
             }">
        <el-form-item label="RSS 地址">
          <el-input
              v-model:model-value="ani.url"
              placeholder="https://mikanani.me/RSS/Bangumi?bangumiId=xxx&subgroupid=xxx"
          />
        </el-form-item>
      </el-form>
      <div style="display: flex;justify-content: space-between;width: 100%;margin-top: 10px;">
        <el-button @click="mikan?.show" text bg>Mikan</el-button>
        <el-button :loading="rssButtonLoading" @click="getRss" text bg>确定</el-button>
      </div>
    </div>
    <div v-else>
      <el-form label-width="auto"
               @submit="(event)=>{
                event.preventDefault()
             }">
        <el-form-item label="标题">
          <div style="width: 100%;">
            <div>
              <el-input v-model:model-value="ani.title"></el-input>
            </div>
            <div style="width: 100%;justify-content: end;display: flex;margin-top: 12px;"
                 v-if="ani.title !== ani.themoviedbName && ani.themoviedbName.length">
              <el-text class="mx-1" size="small">
                标题与 TMDB 不一致!!! 刮削可能会出现问题
              </el-text>
              <div style="width: 8px;"></div>
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
          <el-button :loading="addAniButtonLoading" @click="addAni" bg text>确定</el-button>
        </div>
      </el-form>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import api from "./api.js";
import Mikan from "./Mikan.vue";
import Items from "./Items.vue";
import Exclude from "./Exclude.vue";

const showRss = ref(true)
const mikan = ref()
const items = ref()
const exclude = ref()

const addDialogVisible = ref(false)

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

const rssButtonLoading = ref(false)

const getRss = () => {
  rssButtonLoading.value = true
  api.post('/api/rss', ani.value)
      .then(res => {
        ani.value = res['data']
        showRss.value = false
      })
      .finally(() => {
        rssButtonLoading.value = false
      })
}


const addAniButtonLoading = ref(false)

const addAni = () => {
  addAniButtonLoading.value = true
  api.post('/api/ani', ani.value)
      .then(res => {
        ElMessage.success(res.message)
        emit('load')
        addDialogVisible.value = false
      })
      .finally(() => {
        addAniButtonLoading.value = false
      })
}

const showAdd = () => {
  ani.value = {
    'url': '',
    'season': 1,
    'offset': 0,
    'title': '',
    'exclude': []
  }
  showRss.value = true
  addDialogVisible.value = true
  addAniButtonLoading.value = false
  rssButtonLoading.value = false
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
        ani.value.themoviedbName = res.data
      })
      .finally(() => {
        getThemoviedbNameLoading.value = false
      })
}

defineExpose({showAdd})
const emit = defineEmits(['load'])

</script>

