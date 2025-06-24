<template>
  <Mikan ref="mikan" @add="args => {
    ani.url = args.url
    ani.match = JSON.parse(args.match).map(s => `{{${args.group}}}:${s}`)
    getRss()
  }"/>
  <Bgm ref="bgmRef" @add="it => {
    ani.title = it['name_cn'] ? it['name_cn'] : it['name']
    ani.bgmUrl = it.url
  }"/>
  <el-dialog v-model="dialogVisible" center title="添加订阅"
             :close-on-click-modal="!rssButtonLoading"
             :close-on-press-escape="!rssButtonLoading"
             :show-close="!rssButtonLoading"
  >
    <div v-show="showRss">
      <el-tabs tab-position="left" v-model="activeName">
        <el-tab-pane label="Mikan" name="mikan">
          <el-form label-width="auto"
                   style="height: 260px"
                   @submit="(event)=>{
                event.preventDefault()
             }">
            <el-form-item label="RSS 地址">
              <div style="width: 100%">
                <el-input
                    :disabled="rssButtonLoading"
                    type="textarea"
                    :autosize="{ minRows: 2}"
                    v-model:model-value="ani.url"
                    placeholder="https://mikanani.me/RSS/Bangumi?bangumiId=xxx&subgroupid=xxx"
                />
                <br>
                <div style="width: 100%;display: flex;justify-content: end;margin-top: 8px;">
                  <el-button @click="mikan?.show()" text bg icon="VideoCamera" type="primary"
                             :disabled="rssButtonLoading">Mikan
                  </el-button>
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
        <el-tab-pane label="Other" name="other">
          <el-form label-width="auto"
                   style="height: 200px"
                   @submit="(event)=>{
                event.preventDefault()
             }">
            <el-form-item label="番剧名称">
              <div class="flex" style="width: 100%;">
                <el-input
                    v-model:model-value="ani.title"
                    :disabled="rssButtonLoading"
                    placeholder="请勿留空"
                />
                <div style="width: 4px;"></div>
                <el-button :disabled="rssButtonLoading" bg icon="Search" text type="primary"
                           @click="bgmRef?.show(ani.title)"/>
              </div>
            </el-form-item>
            <el-form-item label="BgmUrl">
              <el-input
                  v-model:model-value="ani.bgmUrl"
                  placeholder="https://bgm.tv/subject/123456"
                  :disabled="rssButtonLoading"
              />
            </el-form-item>
            <el-form-item label="RSS 地址">
              <el-input
                  :disabled="rssButtonLoading"
                  :autosize="{ minRows: 2}"
                  type="textarea"
                  v-model:model-value="ani.url"
                  placeholder="https://xxxx.com/a.xml"
              />
            </el-form-item>
          </el-form>
          <el-text class="mx-1" size="small">
            dmhy等含有磁力链接的RSS不支持Aria2
          </el-text>
        </el-tab-pane>
      </el-tabs>
      <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;">
        <el-button :loading="rssButtonLoading" @click="getRss" text bg icon="Check">确定</el-button>
      </div>
    </div>
    <div v-if="!showRss">
      <Ani v-model:ani="ani" @ok="addAni"/>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import api from "@/js/api.js";
import Mikan from "./Mikan.vue";
import Ani from "./Ani.vue";
import Bgm from "./Bgm.vue";
import {aniData} from "@/js/ani.js";

const showRss = ref(true)
const mikan = ref()
const bgmRef = ref()

const dialogVisible = ref(false)

const ani = ref(aniData)

const rssButtonLoading = ref(false)

const getRss = () => {
  if (activeName.value === 'other') {
    if (!ani.value.bgmUrl) {
      ElMessage.error('请选择在 Bangumi 中所对应的番剧')
      return
    }
  }
  rssButtonLoading.value = true
  ani.value.type = activeName.value
  api.post('api/rss', ani.value)
      .then(res => {
        let match = ani.value['match'];
        ani.value = res['data']
        ani.value['match'] = match
        ani.value.showDownlaod = false
        showRss.value = false
      })
      .finally(() => {
        rssButtonLoading.value = false
      })
}

const addAni = (fun) => {
  api.post('api/ani', ani.value)
      .then(res => {
        ElMessage.success(res.message)
        emit('load')
        dialogVisible.value = false
      }).finally(fun)
}

const activeName = ref('mikan')

const show = () => {
  ani.value = aniData
  activeName.value = 'mikan'
  showRss.value = true
  dialogVisible.value = true
  rssButtonLoading.value = false
}

defineExpose({show})
const emit = defineEmits(['load'])

</script>

