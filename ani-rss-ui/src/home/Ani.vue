<template>
  <Preview ref="previewRef" :ani="props.ani"/>
  <StandbyRss ref="standbyRssRef" :ani="props.ani"/>
  <AniBT ref="aniBTRef" @callback="mikanCallback"/>
  <Mikan ref="mikanRef" @callback="mikanCallback"/>
  <AnimeGarden ref="animeGardenRef" @callback="mikanCallback"/>
  <TmdbGroup ref="tmdbGroupRef" :ani="props.ani"/>
  <div style="padding: 0 12px;">
    <el-tabs v-model="activeName" class="tabs-center">
      <el-tab-pane label="基本" name="base" :lazy="true">
        <el-scrollbar height="500" ref="scrollbarRef">
          <el-form @submit.prevent label-width="auto">
            <el-form-item label="标题">
              <div class="full-width">
                <div>
                  <el-input v-model:model-value="props.ani.title" class="full-width"/>
                </div>
                <div class="change-title-button">
                  <el-button :loading="getBgmNameLoading"
                             bg
                             icon="DocumentAdd" text @click="getBgmName">
                    使用Bangumi
                  </el-button>
                  <el-button @click="props.ani.title = ani.themoviedbName"
                             icon="DocumentAdd"
                             :disabled="props.ani.title === ani.themoviedbName || !ani.themoviedbName.length" bg text>
                    使用TMDB
                  </el-button>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="TMDB">
              <div class="flex full-width" style="justify-content: space-between;">
                <div class="el-input is-disabled">
                  <div class="el-input__wrapper" tabindex="-1"
                       style="pointer-events: auto;cursor: auto;justify-content: left;padding: 0 11px;">
                    <el-link v-if="props.ani?.tmdb?.id"
                             type="primary"
                             :href="`https://www.themoviedb.org/${props.ani.ova ? 'movie' : 'tv'}/${props.ani.tmdb.id}`"
                             target="_blank">
                      {{ props.ani.themoviedbName }}
                    </el-link>
                    <span v-else>{{ props.ani.themoviedbName }}</span>
                  </div>
                </div>
                <div style="width: 4px;"></div>
                <el-button icon="Search" bg text @click="searchThemoviedb"/>
                <div style="width: 4px;"></div>
                <el-button icon="Refresh" bg text @click="getThemoviedbName" :loading="getThemoviedbNameLoading"/>
              </div>
            </el-form-item>
            <el-form-item v-if="!props.ani.ova && props.ani.tmdb" label="剧集组">
              <div class="tmdb-group">
                <el-input v-model="props.ani.tmdb['tmdbGroupId']" placeholder="留空不使用剧集组"/>
                <div style="width: 4px;"/>
                <el-button bg icon="Menu" text @click="tmdbGroupRef?.show"/>
              </div>
            </el-form-item>
            <el-form-item label="BgmUrl">
              <el-input v-model:model-value="props.ani.bgmUrl" placeholder="https://xxx.xxx"/>
            </el-form-item>
            <el-form-item label="主 RSS">
              <div class="full-width">
                <div class="flex full-width">
                  <el-input v-model:model-value="props.ani.subgroup" style="width: 140px" placeholder="字幕组"/>
                  <div style="width: 6px;"></div>
                  <el-input v-model:model-value="props.ani.url" placeholder="https://xxx.xxx"/>
                </div>
                <div style="justify-content: end;margin-top: 4px;" class="flex full-width">
                  <el-button bg text
                             @click="mikanShow">
                    <template #icon>
                      <img src="@/icon/icon-Mikan.png" alt="mikan" class="icon"/>
                    </template>
                  </el-button>
                  <el-button bg text
                             @click="aniBTShow">
                    <template #icon>
                      <img src="@/icon/icon-AniBT.png" alt="ani-bt" class="icon"/>
                    </template>
                  </el-button>
                  <el-button bg text
                             @click="animeGardenShow">
                    <template #icon>
                      <img src="@/icon/icon-AnimeGarden.png" alt="anime-garden" class="icon"/>
                    </template>
                  </el-button>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="备用 RSS">
              <div class="form-item-flex">
                <el-button bg icon="EditPen" text @click="standbyRssRef?.show">管理</el-button>
              </div>
            </el-form-item>
            <el-form-item label="日期">
              <div class="form-item-flex">
                <el-date-picker
                    style="max-width: 150px;"
                    v-model="props.ani.releaseDate"
                />
              </div>
            </el-form-item>
            <el-form-item label="季">
              <div class="form-item-flex">
                <el-input-number style="max-width: 200px"
                                 :min="0"
                                 v-model="props.ani.season"
                                 :disabled="props.ani.ova"/>
              </div>
            </el-form-item>
            <el-form-item label="集数偏移">
              <div class="form-item-flex">
                <el-input-number v-model:model-value="props.ani.offset" :disabled="props.ani.ova"/>
              </div>
            </el-form-item>
            <el-form-item label="总集数">
              <div class="form-item-flex">
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
            <el-form-item label="启用">
              <el-switch v-model:model-value="props.ani.enable"/>
            </el-form-item>
          </el-form>
        </el-scrollbar>
      </el-tab-pane>
      <el-tab-pane label="自定义" name="custom" :lazy="true">
        <el-scrollbar height="500">
          <el-form @submit.prevent label-width="auto">
            <el-form-item label="自定义集数规则">
              <div class="full-width">
                <div>
                  <el-switch v-model:model-value="props.ani.customEpisode"/>
                </div>
                <div class="flex full-width">
                  <el-input class="full-width"
                            :disabled="!props.ani.customEpisode"
                            v-model:model-value="props.ani.customEpisodeStr"/>
                  <div style="width: 4px;"></div>
                  <el-input-number v-model:model-value="props.ani.customEpisodeGroupIndex"
                                   :disabled="!props.ani.customEpisode"/>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="自定义路径">
              <div class="full-width">
                <div>
                  <el-switch v-model:model-value="props.ani.customDownloadPath"/>
                </div>
                <div>
                  <el-input type="textarea" class="full-width" :disabled="!props.ani.customDownloadPath"
                            :autosize="{ minRows: 2}"
                            v-model:model-value="props.ani.downloadPath"/>
                </div>
                <div style="display: flex;justify-content: space-between;margin-top: 6px;">
                  <el-button :disabled="!props.ani.customDownloadPath" :loading="downloadPathLoading" bg icon="Refresh"
                             text
                             @click="downloadPath"/>
                  <el-text class="mx-1" size="small">
                    最终下载位置以 <strong>预览</strong> 为准
                  </el-text>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="自定义上传">
              <div class="full-width">
                <div>
                  <el-switch v-model="props.ani.customUploadEnable"/>
                </div>
                <div>
                  <el-input
                      v-model="props.ani.customUploadPathTarget"
                      :autosize="{ minRows: 2}"
                      :disabled="!props.ani.customUploadEnable"
                      class="full-width"
                      type="textarea"/>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="自定义完结迁移">
              <div class="full-width">
                <div>
                  <el-switch v-model="props.ani.customCompleted"/>
                </div>
                <div>
                  <el-input type="textarea" class="full-width" :disabled="!props.ani.customCompleted"
                            :autosize="{ minRows: 2}"
                            v-model:model-value="props.ani.customCompletedPathTemplate"/>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="重命名模版">
              <div class="full-width">
                <el-switch v-model="props.ani['customRenameTemplateEnable']"/>
                <br>
                <el-input v-model:model-value="props.ani['customRenameTemplate']"
                          :disabled="!props.ani['customRenameTemplateEnable']"
                          placeholder="${title} S${seasonFormat}E${episodeFormat}"/>
                <br>
                <el-link
                    style="font-size: var(--el-font-size-extra-small)"
                    type="primary"
                    href="https://docs.wushuo.top/config/basic/rename#rename-template"
                    target="_blank">
                  详细说明
                </el-link>
              </div>
            </el-form-item>
            <el-form-item label="自定义标签">
              <div>
                <el-switch v-model="props.ani.customTagsEnable"/>
                <Disable v-model="props.ani.customTagsEnable">
                  <custom-tags :config="props.ani"/>
                </Disable>
              </div>
            </el-form-item>
            <el-form-item label="优先保留">
              <div class="full-width">
                <el-switch v-model="props.ani.customPriorityKeywordsEnable"/>
                <br>
                <Disable v-model="props.ani.customPriorityKeywordsEnable">
                  <PrioKeys
                      v-model:keywords="props.ani.customPriorityKeywords"
                      :import-global="true"
                      :show-text="true"
                  />
                </Disable>
              </div>
            </el-form-item>
            <el-form-item label="其它">
              <el-checkbox v-model="props.ani.omit" label="遗漏检测"/>
              <el-checkbox v-model="props.ani.upload" label="自动上传"/>
              <el-checkbox v-model="props.ani.downloadNew" label="只下载最新集"/>
              <el-checkbox v-model="props.ani['procrastinating']" label="摸鱼检测"/>
              <el-checkbox v-model="props.ani['message']" label="通知"/>
              <el-checkbox v-model="props.ani['completed']" label="完结迁移"/>
            </el-form-item>
          </el-form>
        </el-scrollbar>
      </el-tab-pane>
    </el-tabs>
  </div>
  <div class="flex full-width" style="justify-content: space-between;margin-top: 10px;">
    <div>
      <el-dropdown trigger="click">
        <el-button bg text icon="MoreFilled">
          其他
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="refreshAni">
              <el-text>
                <el-icon>
                  <RefreshRight/>
                </el-icon>
                刷新
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item @click="scrape(false)">
              <el-text>
                <el-icon>
                  <RefreshRight/>
                </el-icon>
                刮削
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item @click="scrape(true)">
              <el-text type="warning">
                <el-icon>
                  <Refresh/>
                </el-icon>
                刮削 [F]
              </el-text>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <div>
      <el-button @click="previewRef.show()" bg text icon="Grid">预览</el-button>
      <el-button icon="Check" type="primary" :loading="okLoading" @click="async ()=>{
        okLoading = true
        emit('callback',()=>okLoading = false)
      }" text bg>确定
      </el-button>
    </div>
  </div>
</template>

<script setup>

import Exclude from "@/config/Exclude.vue";
import PrioKeys from "@/config/PrioKeys.vue";
import Preview from "./Preview.vue";
import {onMounted, ref} from "vue";
import {ElMessage, ElMessageBox, ElText} from "element-plus";
import StandbyRss from "./StandbyRss.vue";
import Mikan from "./Mikan.vue";
import TmdbGroup from "./TmdbGroup.vue";
import CustomTags from "@/config/CustomTags.vue";
import {Refresh, RefreshRight} from "@element-plus/icons-vue";
import * as http from "@/js/http.js";
import {getBgmTitle} from "@/js/http.js";
import AniBT from "@/home/AniBT.vue";
import AnimeGarden from "@/home/AnimeGarden.vue";
import Disable from "@/other/Disable.vue";

const activeName = ref('base')

const aniBTRef = ref()
const mikanRef = ref()
const animeGardenRef = ref()
const tmdbGroupRef = ref()

let standbyRssRef = ref()

let previewRef = ref()
let okLoading = ref(false)

let getThemoviedbNameLoading = ref(false)

let getThemoviedbName = () => {
  if (!props.ani.title.length) {
    return
  }

  getThemoviedbNameLoading.value = true
  http.getThemoviedbName({
    title: props.ani.title,
    ova: props.ani.ova
  })
      .then(res => {
        ElMessage.success(res.message)
        props.ani['themoviedbName'] = res.data['themoviedbName']
        props.ani['tmdb'] = res.data['tmdb']
      })
      .finally(() => {
        getThemoviedbNameLoading.value = false
      })
}

let searchThemoviedb = () => {
  ElMessageBox.prompt('输入 TmdbId', {
    confirmButtonText: 'OK',
    confirmButtonClass: 'is-text is-has-bg el-button--primary',
    cancelButtonText: 'Cancel',
    cancelButtonClass: 'is-text is-has-bg',
  })
      .then(value => {
        http.getThemoviedbName({
          tmdbId: value.value,
          ova: props.ani.ova
        })
            .then(res => {
              ElMessage.success(res.message)
              props.ani['themoviedbName'] = res.data['themoviedbName']
              props.ani['tmdb'] = res.data['tmdb']
            })
      })
}

let exclude = ref()
let match = ref()

onMounted(() => {
  init()
})

let scrollbarRef = ref()

let init = () => {
  scrollbarRef.value.setScrollTop(0)
  activeName.value = 'base'
}

let refreshAni = () => {
  http.refreshAni(props.ani)
      .then(res => {
        ElMessage.success(res.message)
      })
}

let downloadPathLoading = ref(false)
let downloadPath = () => {
  downloadPathLoading.value = true
  let newAni = JSON.parse(JSON.stringify(props.ani))
  newAni.customDownloadPath = false
  http.downloadPath(newAni)
      .then(res => {
        props.ani.downloadPath = res.data.downloadPath
      })
      .finally(() => {
        downloadPathLoading.value = false
      })
}

let getBgmNameLoading = ref(false)

let getBgmName = () => {
  getBgmNameLoading.value = true
  getBgmTitle(props.ani)
      .then(res => {
        props.ani.title = res.data
      })
      .finally(() => {
        getBgmNameLoading.value = false
      })
}

let mikanCallback = v => {
  let {subgroup, match, url} = v
  props.ani.url = url
  props.ani.subgroup = subgroup

  let newMatch = JSON.parse(match).map(s => `{{${subgroup}}}:${s}`)

  // 剔除旧的同字幕组规则
  props.ani.match = props.ani.match.filter(it => it.indexOf(`{{${subgroup}}}:`) !== 0)

  props.ani.match.push(...newMatch)
}

let scrape = (force) => {
  http.scrape(force, props.ani)
      .then(res => {
        ElMessage.success(res.message)
      })
}

let mikanShow = () => {
  let query = props.ani.mikanTitle ? props.ani.mikanTitle : props.ani.title;

  if (props.ani.url) {
    let url = new URL(props.ani.url);
    let searchParams = url.searchParams;
    let mikanId = searchParams.get("bangumiId");
    if (mikanId) {
      query = `id: ${mikanId}`
    }
  }

  mikanRef.value?.show(query)
}

let animeGardenShow = () => {
  let bgmUrl = props.ani.bgmUrl;
  animeGardenRef.value?.show(bgmUrl)
}

let aniBTShow = () => {
  let bgmUrl = props.ani.bgmUrl;
  aniBTRef.value?.show(bgmUrl)
}

let props = defineProps(['ani'])
const emit = defineEmits(['callback'])
</script>

<style scoped>
.change-title-button {
  width: 100%;
  justify-content: end;
  display: flex;
  margin-top: 12px;
}

.tmdb-group {
  display: flex;
  width: 100%;
  justify-content: space-between;
}

.form-item-flex {
  width: 100%;
  display: flex;
  justify-content: end;
}

.icon {
  width: 24px;
  height: 24px;
  border-radius: 8px;
}
</style>
