<template>
  <Mikan ref="mikan" @add="args => ani.url = args"/>
  <el-dialog v-model="dialogVisible" title="添加订阅" center>
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
      <Ani v-model:ani="ani" @ok="addAni"/>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import api from "../api.js";
import Mikan from "./Mikan.vue";
import Ani from "./Ani.vue";

const showRss = ref(true)
const mikan = ref()

const dialogVisible = ref(false)

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

const addAni = (fun) => {
  api.post('/api/ani', ani.value)
      .then(res => {
        ElMessage.success(res.message)
        emit('load')
        dialogVisible.value = false
      }).finally(fun)
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
  dialogVisible.value = true
  rssButtonLoading.value = false
}

defineExpose({showAdd})
const emit = defineEmits(['load'])

</script>

