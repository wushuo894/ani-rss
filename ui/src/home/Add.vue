<template>
  <Mikan ref="mikan" @add="args => ani.url = args.url"/>
  <el-dialog v-model="dialogVisible" title="添加订阅" center v-if="dialogVisible">
    <div v-if="showRss" @keydown.enter="getRss">
      <el-tabs tab-position="left" v-model="activeName">
        <el-tab-pane label="Mikan" name="mikan">
          <el-form label-width="auto"
                   style="height: 200px"
                   v-if="showRss" @keydown.enter="getRss"
                   @submit="(event)=>{
                event.preventDefault()
             }">
            <el-form-item label="RSS 地址">
              <div style="width: 100%">
                <el-input
                    type="textarea"
                    v-model:model-value="ani.url"
                    placeholder="https://mikanani.me/RSS/Bangumi?bangumiId=xxx&subgroupid=xxx"
                />
                <br>
                <div style="width: 100%;display: flex;justify-content: end;margin-top: 8px;">
                  <el-button @click="mikan?.show" text bg icon="VideoCamera">Mikan</el-button>
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
        <el-tab-pane label="Nyaa" name="nyaa">
          <el-form label-width="auto"
                   style="height: 200px"
                   v-if="showRss" @keydown.enter="getRss"
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
                  type="textarea"
                  v-model:model-value="ani.url"
                  placeholder="https://nyaa.si/?page=rss&q=xxx"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="Dmhy" name="dmhy">
          <el-form label-width="auto"
                   style="height: 200px"
                   v-if="showRss" @keydown.enter="getRss"
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
              <div style="width: 100%">
                <el-input
                    v-model:model-value="ani.url"
                    type="textarea"
                    placeholder="https://share.dmhy.org/topics/rss/rss.xml?keyword=xxx"
                />
                <div>
                  <el-text class="mx-1" size="small">
                    Dmhy 仅支持qb开启修改任务标题的情况下自动重命名与坏种检测
                  </el-text>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;">
        <el-button :loading="rssButtonLoading" @click="getRss" text bg icon="Check">确定</el-button>
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
  'ova': false,
  'totalEpisodeNumber': '',
  'customDownloadPath': false,
  'downloadPath': '',
  'year': 1970,
  'month': 1,
  'subgroup': '',
  'backRssList': [],
  'bgmUrl': ''
})

const rssButtonLoading = ref(false)

const getRss = () => {
  rssButtonLoading.value = true
  ani.value.type = activeName.value
  api.post('/api/rss', ani.value)
      .then(res => {
        ani.value = res['data']
        ani.value.showDownlaod = false
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

const activeName = ref('mikan')

const show = () => {
  ani.value = {
    'url': '',
    'season': 1,
    'offset': 0,
    'title': '',
    'exclude': [],
    'totalEpisodeNumber': 0
  }
  activeName.value = 'mikan'
  showRss.value = true
  dialogVisible.value = true
  rssButtonLoading.value = false
}

defineExpose({show})
const emit = defineEmits(['load'])

</script>

