<template>
  <AnimeGarden ref="animeGardenRef" @callback="aniBTCallback"/>
  <AniBT ref="aniBTRef" @callback="aniBTCallback"/>
  <Mikan ref="mikanRef" @callback="mikanCallback"/>
  <Bgm ref="bgmRef" @callback="bgmCallback"/>
  <el-dialog v-model="dialogVisible" center title="添加订阅"
             :close-on-click-modal="!rssButtonLoading"
             :close-on-press-escape="!rssButtonLoading"
             :show-close="!rssButtonLoading"
  >
    <div v-show="showRss">
      <el-tabs tab-position="left" v-model="activeName">
        <el-tab-pane label="Mikan" name="mikan">
          <el-form label-width="auto"
                   style="height: 260px">
            <el-form-item label="RSS 地址">
              <div class="full-width">
                <el-input
                    :disabled="rssButtonLoading"
                    type="textarea"
                    :autosize="{ minRows: 2}"
                    v-model:model-value="ani.url"
                    placeholder="https://mikanani.me/RSS/Bangumi?bangumiId=xxx&subgroupid=xxx"
                />
                <br>
                <div class="mikan-button">
                  <el-button @click="mikanRef?.show()" text bg type="primary"
                             :disabled="rssButtonLoading">
                    <template #icon>
                      <img src="@/icon/icon-mikan.png" alt="mikan" class="icon el-icon--left"/>
                    </template>
                    Mikan
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
        <el-tab-pane label="AniBT" name="ani-bt">
          <el-form label-width="auto"
                   style="height: 260px">
            <el-form-item label="RSS 地址">
              <div class="full-width">
                <el-input
                    :disabled="rssButtonLoading"
                    type="textarea"
                    :autosize="{ minRows: 2}"
                    v-model:model-value="ani.url"
                    placeholder="https://anibt.net/rss/anime.xml?bgmId=xxx&groupSlug=xxx"
                />
                <br>
                <div class="mikan-button">
                  <el-button @click="aniBTRef?.show()" text bg type="primary"
                             :disabled="rssButtonLoading">
                    <template #icon>
                      <img src="@/icon/icon-AniBT.png" alt="ani-bt" class="icon el-icon--left"/>
                    </template>
                    AniBT
                  </el-button>
                </div>
                <div>
                  <el-text class="mx-1" size="small">
                    不支持聚合订阅，原因是如果一次过多更新会出现遗漏
                    <br>
                    不必在 AniBT 网站添加订阅, 你可以通过上方👆 [AniBT] 按钮浏览字幕组订阅
                  </el-text>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="AG" name="anime-garden">
          <el-form label-width="auto"
                   style="height: 260px">
            <el-form-item label="RSS 地址">
              <div class="full-width">
                <el-input
                    :disabled="rssButtonLoading"
                    type="textarea"
                    :autosize="{ minRows: 2}"
                    v-model:model-value="ani.url"
                    placeholder="https://api.animes.garden/feed.xml?subject=xxx&fansub=xxx"
                />
                <br>
                <div class="mikan-button">
                  <el-button @click="animeGardenRef?.show()" text bg type="primary"
                             :disabled="rssButtonLoading">
                    <template #icon>
                      <img src="@/icon/icon-AnimeGarden.png" alt="AnimeGarden" class="icon el-icon--left"/>
                    </template>
                    AnimeGarden
                  </el-button>
                </div>
                <div>
                  <el-text class="mx-1" size="small">
                    不支持聚合订阅，原因是如果一次过多更新会出现遗漏
                    <br>
                    不必在 AnimeGarden 网站添加订阅, 你可以通过上方👆 [AnimeGarden] 按钮浏览字幕组订阅
                  </el-text>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="Other" name="other">
          <el-form label-width="auto"
                   style="height: 200px">
            <el-form-item label="番剧名称">
              <div class="flex full-width">
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
      <div class="action">
        <el-button :loading="rssButtonLoading" @click="getRss" text bg icon="Check">确定</el-button>
      </div>
    </div>
    <div v-if="!showRss">
      <Ani v-model:ani="ani" @callback="addAni"/>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import Mikan from "./Mikan.vue";
import Ani from "./Ani.vue";
import Bgm from "./Bgm.vue";
import {aniData} from "@/js/ani.js";
import * as http from "@/js/http.js";
import AniBT from "@/home/AniBT.vue";
import {useLocalStorage} from "@vueuse/core";
import AnimeGarden from "@/home/AnimeGarden.vue";

const showRss = ref(true)
const aniBTRef = ref()
const mikanRef = ref()
const animeGardenRef = ref()
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
  http.rssToAni(ani.value)
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
  http.addAni(ani.value)
      .then(res => {
        ElMessage.success(res.message)
        window.$reLoadList()
        dialogVisible.value = false
      }).finally(fun)
}

const activeName = useLocalStorage('add-active-name', 'mikan')

const show = () => {
  ani.value = JSON.parse(JSON.stringify(aniData))
  showRss.value = true
  dialogVisible.value = true
  rssButtonLoading.value = false
}

let bgmCallback = it => {
  ani.value.title = it['name_cn'] ? it['name_cn'] : it['name']
  ani.value.bgmUrl = it.url
}

let aniBTCallback = v => {
  let {group, match, url, bgmUrl} = v
  ani.value.bgmUrl = bgmUrl
  ani.value.subgroup = group
  ani.value.url = url
  ani.value.match = JSON.parse(match)
      .map(s => `{{${group}}}:${s}`)
  getRss()
}

let mikanCallback = v => {
  let {group, match, url} = v
  ani.value.url = url
  ani.value.match = JSON.parse(match)
      .map(s => `{{${group}}}:${s}`)
  getRss()
}

defineExpose({show})
</script>

<style scoped>
.mikan-button {
  width: 100%;
  display: flex;
  justify-content: end;
  margin-top: 8px;
}

.action {
  width: 100%;
  display: flex;
  justify-content: end;
  margin-top: 10px;
}

.icon {
  width: 24px;
  height: 24px;
  border-radius: 8px;
}
</style>
