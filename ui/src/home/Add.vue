<template>
  <Items ref="items"/>
  <Mikan ref="mikan" @add="args => ani.url = args"/>
  <el-dialog v-model="addDialogVisible" title="添加订阅" center>
    <div v-if="showRss" @keydown.enter="getRss">
      <el-tabs tab-position="left" v-model="activeName">
        <el-tab-pane label="Mikan" name="1">
          <el-form label-width="auto"
                   style="height: 200px"
                   v-if="showRss" @keydown.enter="getRss('mikan')"
                   @submit="(event)=>{
                event.preventDefault()
             }">
            <el-form-item label="RSS 地址">
              <div style="width: 100%">
                <el-input
                    v-model:model-value="ani.url"
                    placeholder="https://mikanani.me/RSS/Bangumi?bangumiId=xxx&subgroupid=xxx"
                />
                <br>
                <div style="width: 100%;display: flex;justify-content: end;margin-top: 8px;">
                  <el-button @click="mikan?.show" text bg>Mikan</el-button>
                </div>
                <div>
                  <el-text class="mx-1" size="small">
                    不支持聚合订阅，原因是如果一次过多更新会出现遗漏
                    <br>
                    不必在 mikan 网站添加订阅, 你可以通过上方👆 [Mikan] 按钮浏览字幕组订阅
                  </el-text>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="Nyaa" name="2">
          <el-form label-width="auto"
                   style="height: 200px"
                   v-if="showRss" @keydown.enter="getRss('nyaa')"
                   @submit="(event)=>{
                event.preventDefault()
             }">
            <el-form-item label="番剧名称">
              <el-input
                  v-model:model-value="ani.title"
                  placeholder="可以为空 如果获取失败建议补全"
              />
            </el-form-item>
            <el-form-item label="RSS 地址">
              <el-input
                  v-model:model-value="ani.url"
                  placeholder="https://nyaa.si/?page=rss&q=xx"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;">
        <el-button :loading="rssButtonLoading" @click="getRss('nyaa')" text bg>确定</el-button>
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
              <el-button @click="ani.title = ani.themoviedbName" bg text>使用TMDB</el-button>
            </div>
            <div v-if="!ani.themoviedbName.length"
                 style="width: 100%;justify-content: end;display: flex;margin-top: 12px;">
              <el-text class="mx-1" size="small">
                无法获取到其在 TMDB 中的名称!!! 刮削可能会出现问题
              </el-text>
              <div style="width: 4px;"></div>
              <a href="https://tmdb.org" target="_blank">https://tmdb.org</a>
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
import api from "../api.js";
import Mikan from "./Mikan.vue";
import Items from "./Items.vue";
import Exclude from "../config/Exclude.vue";

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

const getRss = (type) => {
  rssButtonLoading.value = true
  ani.value.type = type
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

const activeName = ref('1')

const showAdd = () => {
  ani.value = {
    'url': '',
    'season': 1,
    'offset': 0,
    'title': '',
    'exclude': []
  }
  activeName.value = '1'
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
        ElMessage.success(res.message)
        ani.value.themoviedbName = res.data
      })
      .finally(() => {
        getThemoviedbNameLoading.value = false
      })
}

defineExpose({showAdd})
const emit = defineEmits(['load'])

</script>

