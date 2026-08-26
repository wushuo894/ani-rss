<template>
  <AnimeGardenView ref="animeGardenRef" @callback="rssCallback"/>
  <AniBTView ref="aniBTRef" @callback="rssCallback"/>
  <MikanView ref="mikanRef" @callback="rssCallback"/>
  <BgmView ref="bgmRef" @callback="bgmCallback"/>
  <el-dialog v-model="dialogVisible" class="add-subscription-dialog" center title="添加订阅"
             :close-on-click-modal="!rssButtonLoading"
             :close-on-press-escape="!rssButtonLoading"
             :show-close="!rssButtonLoading"
  >
    <div v-show="showRss" class="add-source-step">
      <el-tabs v-model="activeName" class="add-source-tabs tabs-center" tab-position="top">
        <el-tab-pane
            v-for="source in rssSources"
            :key="source.name"
            :label="source.label"
            :name="source.name">
          <div class="source-panel">
            <div class="source-heading">
              <div class="source-identity">
                <img :src="source.icon" :alt="source.label" class="source-icon"/>
                <div>
                  <h3>{{ source.label }}</h3>
                  <el-text size="small" type="info">RSS 订阅</el-text>
                </div>
              </div>
              <el-button
                  bg
                  text
                  type="primary"
                  :disabled="rssButtonLoading"
                  @click="source.open">
                <el-icon class="el-icon--left">
                  <Search/>
                </el-icon>
                浏览 {{ source.label }}
              </el-button>
            </div>
            <el-form class="source-form" label-position="top" @submit.prevent>
              <el-form-item label="RSS 地址">
                <el-input
                    v-model="ani.url"
                    :autosize="{ minRows: 3, maxRows: 5 }"
                    :disabled="rssButtonLoading"
                    :placeholder="source.placeholder"
                    type="textarea"/>
              </el-form-item>
            </el-form>
            <el-alert
                :closable="false"
                show-icon
                title="请使用单个番剧与字幕组的 RSS，聚合订阅集中更新时可能出现遗漏。"
                type="info"/>
          </div>
        </el-tab-pane>
        <el-tab-pane label="其他" name="other">
          <div class="source-panel other-source-panel">
            <el-form class="source-form" label-position="top" @submit.prevent>
              <el-form-item label="番剧名称">
                <div class="title-search-row">
                  <el-input
                      v-model="ani.title"
                      :disabled="rssButtonLoading"
                      placeholder="请勿留空"/>
                  <el-button
                      aria-label="搜索 Bangumi"
                      bg
                      text
                      type="primary"
                      :disabled="rssButtonLoading"
                      title="搜索 Bangumi"
                      @click="bgmRef?.show(ani.title)">
                    <el-icon class="el-icon--left">
                      <Search/>
                    </el-icon>
                  </el-button>
                </div>
              </el-form-item>
              <el-form-item label="Bangumi 地址">
                <el-input
                    v-model="ani.bgmUrl"
                    :disabled="rssButtonLoading"
                    placeholder="https://bgm.tv/subject/123456"/>
              </el-form-item>
              <el-form-item label="RSS 地址">
                <el-input
                    v-model="ani.url"
                    :autosize="{ minRows: 3, maxRows: 5 }"
                    :disabled="rssButtonLoading"
                    placeholder="https://example.com/anime.xml"
                    type="textarea"/>
              </el-form-item>
            </el-form>
            <el-alert
                :closable="false"
                show-icon
                title="含有磁力链接的 RSS 不支持 Aria2。"
                type="warning"/>
          </div>
        </el-tab-pane>
      </el-tabs>
      <div class="action">
        <el-button :loading="rssButtonLoading" bg text type="primary" @click="getRss">
          下一步
          <el-icon class="el-icon--right">
            <ArrowRight/>
          </el-icon>
        </el-button>
      </div>
    </div>
    <div v-if="!showRss" class="ani-step">
      <el-button
          class="ani-step-back"
          bg text icon="ArrowLeft"
          @click="showRss = true">
        返回 RSS
      </el-button>
      <AniView v-model:ani="ani" @callback="addAni"/>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import {ArrowRight, Search} from "@element-plus/icons-vue";
import MikanView from "./MikanView.vue";
import AniView from "./AniView.vue";
import BgmView from "./BgmView.vue";
import {aniData} from "@/js/ani.js";
import * as http from "@/js/http.js";
import AniBTView from "@/view/home/AniBTView.vue";
import {useLocalStorage} from "@vueuse/core";
import AnimeGardenView from "@/view/home/AnimeGardenView.vue";
import mikanIcon from "@/icon/icon-Mikan.png";
import aniBTIcon from "@/icon/icon-AniBT.png";
import animeGardenIcon from "@/icon/icon-AnimeGarden.png";

const showRss = ref(true)
const aniBTRef = ref()
const mikanRef = ref()
const animeGardenRef = ref()
const bgmRef = ref()

const rssSources = [
  {
    name: 'mikan',
    label: 'Mikan',
    icon: mikanIcon,
    placeholder: 'https://mikanani.me/RSS/Bangumi?bangumiId=xxx&subgroupid=xxx',
    open: () => mikanRef.value?.show()
  },
  {
    name: 'ani-bt',
    label: 'AniBT',
    icon: aniBTIcon,
    placeholder: 'https://anibt.net/rss/anime.xml?bgmId=xxx&groupSlug=xxx',
    open: () => aniBTRef.value?.show()
  },
  {
    name: 'anime-garden',
    label: 'AnimeGarden',
    icon: animeGardenIcon,
    placeholder: 'https://api.animes.garden/feed.xml?subject=xxx&fansub=xxx',
    open: () => animeGardenRef.value?.show()
  }
]

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

let rssCallback = v => {
  let {subgroup, match, url, bgmUrl} = v
  ani.value.url = url
  ani.value.bgmUrl = bgmUrl
  ani.value.subgroup = subgroup
  ani.value.match = JSON.parse(match)
      .map(s => `{{${subgroup}}}:${s}`)
  getRss()
}

defineExpose({show})
</script>

<style scoped>
.add-source-tabs {
  min-width: 0;
}

.source-panel {
  min-height: 272px;
  padding: 14px 4px 4px;
}

.source-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.source-identity {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.source-identity h3 {
  margin: 0;
  font-size: 16px;
  line-height: 1.35;
}

.source-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 8px;
  object-fit: cover;
}

.source-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.source-form :deep(.el-form-item__label) {
  height: auto;
  padding-bottom: 7px;
  color: var(--el-text-color-regular);
  font-weight: 600;
  line-height: 1.4;
}

.title-search-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px;
  gap: 8px;
}

.title-search-row .el-button {
  width: 40px;
  margin: 0;
}

.other-source-panel {
  min-height: 366px;
}

.action {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-extra-light);
}

.ani-step-back {
  margin: 0 12px 10px;
}

@media (max-width: 600px) {
  .source-panel {
    min-height: 250px;
    padding-top: 10px;
  }

  .source-heading {
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 16px;
  }

  .source-heading .el-button {
    max-width: 172px;
    margin: 0;
  }

  .source-icon {
    width: 36px;
    height: 36px;
  }

  .other-source-panel {
    min-height: 350px;
  }
}
</style>
