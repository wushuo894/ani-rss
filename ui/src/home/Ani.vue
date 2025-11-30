<template>
  <Items ref="items" :ani="props.ani"/>
  <StandbyRss ref="standbyRss" :ani="props.ani"/>
  <Mikan ref="mikanRef" @callback="mikanCallback"/>
  <TmdbGroup ref="tmdbGroupRef" :ani="props.ani"/>
  <div style="height: 500px;">
    <el-scrollbar style="padding: 0 12px;" height="500" ref="scrollbar">
      <el-form label-width="auto"
               @submit="(event)=>{
                event.preventDefault()
             }"
      >
        <el-form-item label="标题">
          <div style="width: 100%;">
            <div>
              <el-input v-model:model-value="props.ani.title" style="width: 100%"/>
            </div>
            <div style="width: 100%;justify-content: end;display: flex;margin-top: 12px;">
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
          <div style="display: flex;width: 100%;justify-content: space-between;">
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
            <el-button icon="Refresh" bg text @click="getThemoviedbName" :loading="getThemoviedbNameLoading"/>
          </div>
        </el-form-item>
        <el-form-item v-if="!props.ani.ova && props.ani.tmdb" label="剧集组">
          <div style="display: flex;width: 100%;justify-content: space-between;">
            <el-input v-model="props.ani.tmdb['tmdbGroupId']" placeholder="留空不使用剧集组"/>
            <div style="width: 4px;"/>
            <el-button bg icon="Menu" text @click="tmdbGroupRef?.show"/>
          </div>
        </el-form-item>
        <el-form-item label="BgmUrl">
          <el-input v-model:model-value="props.ani.bgmUrl" placeholder="https://xxx.xxx"/>
        </el-form-item>
        <el-form-item label="主 RSS">
          <div style="width: 100%;display: flex;">
            <el-input v-model:model-value="props.ani.subgroup" style="width: 140px" placeholder="字幕组"/>
            <div style="width: 6px;"></div>
            <el-input v-model:model-value="props.ani.url" placeholder="https://xxx.xxx"/>
            <div style="width: 6px;"></div>
            <el-button bg text
                       @click="mikanRef?.show(props.ani.mikanTitle ? props.ani.mikanTitle : props.ani.title)"
                       icon="VideoCamera"/>
          </div>
        </el-form-item>
        <el-form-item label="备用 RSS">
          <div style="display: flex;justify-content: end;width: 100%;">
            <el-button bg icon="EditPen" text @click="standbyRss?.show">管理</el-button>
          </div>
        </el-form-item>
        <el-form-item label="日期">
          <div style="display: flex;width: 100%;justify-content: end;">
            <el-date-picker
                style="max-width: 150px;"
                v-model="date"
                @change="dateChange"
            />
          </div>
        </el-form-item>
        <el-form-item label="季">
          <div style="display: flex;justify-content: end;width: 100%;">
            <el-input-number style="max-width: 200px" :min="0" v-model:model-value="props.ani.season"
                             :disabled="props.ani.ova"/>
          </div>
        </el-form-item>
        <el-form-item label="集数偏移">
          <div style="display: flex;justify-content: end;width: 100%;">
            <el-input-number v-model:model-value="props.ani.offset" :disabled="props.ani.ova"/>
          </div>
        </el-form-item>
        <el-form-item label="总集数">
          <div style="display: flex;justify-content: end;width: 100%;">
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
        <el-form-item label="自定义集数规则">
          <div style="width: 100%;">
            <div>
              <el-switch v-model:model-value="props.ani.customEpisode"/>
            </div>
            <div style="display: flex;width: 100%;">
              <el-input style="width: 100%"
                        :disabled="!props.ani.customEpisode"
                        v-model:model-value="props.ani.customEpisodeStr"/>
              <div style="width: 4px;"></div>
              <el-input-number v-model:model-value="props.ani.customEpisodeGroupIndex"
                               :disabled="!props.ani.customEpisode"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="自定义路径">
          <div style="width: 100%;">
            <div>
              <el-switch v-model:model-value="props.ani.customDownloadPath"/>
            </div>
            <div>
              <el-input type="textarea" style="width: 100%" :disabled="!props.ani.customDownloadPath"
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
          <div style="width: 100%;">
            <div>
              <el-switch v-model="props.ani.customAlistPath"/>
            </div>
            <div>
              <el-input type="textarea" style="width: 100%" :disabled="!props.ani.customAlistPath"
                        :autosize="{ minRows: 2}"
                        v-model:model-value="props.ani.alistPath"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="自定义完结迁移">
          <div style="width: 100%;">
            <div>
              <el-switch v-model="props.ani.customCompleted"/>
            </div>
            <div>
              <el-input type="textarea" style="width: 100%" :disabled="!props.ani.customCompleted"
                        :autosize="{ minRows: 2}"
                        v-model:model-value="props.ani.customCompletedPathTemplate"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="重命名模版">
          <div style="width: 100%">
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
            <div
                :style="{ opacity: props.ani.customTagsEnable ? 1 : 0.4, 'pointer-events': props.ani.customTagsEnable ? 'auto' : 'none' }">
              <custom-tags :config="props.ani"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="优先保留">
          <div style="width: 100%">
            <el-switch v-model="props.ani['customPriorityKeywordsEnable']"/>
            <br>
            <div
                :style="{ opacity: props.ani['customPriorityKeywordsEnable'] ? 1 : 0.4, 'pointer-events': props.ani['customPriorityKeywordsEnable'] ? 'auto' : 'none' }">
              <PrioKeys
                  v-model:keywords="props.ani['customPriorityKeywords']"
                  :import-global="true"
                  :show-text="true"
              />
            </div>
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
        <el-form-item label="启用">
          <el-switch v-model:model-value="props.ani.enable"/>
        </el-form-item>
      </el-form>
    </el-scrollbar>
  </div>
  <div style="display: flex;justify-content: space-between;width: 100%;margin-top: 10px;">
    <div>
      <el-dropdown trigger="click">
        <el-button bg text icon="MoreFilled">
          其他
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="download">
              刷新
            </el-dropdown-item>
            <el-dropdown-item @click="scrape(false)">
              刮削
            </el-dropdown-item>
            <el-dropdown-item @click="scrape(true)">
              强制刮削
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <div>
      <el-button @click="items.show()" bg text icon="Grid">预览</el-button>
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
import Items from "./Items.vue";
import {onMounted, ref} from "vue";
import api from "@/js/api.js";
import {ElMessage, ElText} from "element-plus";
import StandbyRss from "./StandbyRss.vue";
import Mikan from "./Mikan.vue";
import TmdbGroup from "./TmdbGroup.vue";
import CustomTags from "@/config/CustomTags.vue";

const mikanRef = ref()
const tmdbGroupRef = ref()

let standbyRss = ref()
let date = ref()

let items = ref()
let okLoading = ref(false)

let getThemoviedbNameLoading = ref(false)

let getThemoviedbName = () => {
  if (!props.ani.title.length) {
    return
  }

  getThemoviedbNameLoading.value = true
  api.post('api/tmdb?method=getThemoviedbName', props.ani)
      .then(res => {
        ElMessage.success(res.message)
        props.ani['themoviedbName'] = res.data['themoviedbName']
        props.ani['tmdb'] = res.data['tmdb']
      })
      .finally(() => {
        getThemoviedbNameLoading.value = false
      })
}

let exclude = ref()
let match = ref()

onMounted(() => {
  init()
})

let scrollbar = ref()

let init = () => {
  date.value = new Date(props.ani.year, props.ani.month - 1, props.ani.date);
  scrollbar.value?.setScrollTop(0)
}

let dateChange = () => {
  if (!date.value) {
    return
  }
  props.ani.year = date.value.getFullYear()
  props.ani.month = date.value.getMonth() + 1
  props.ani.date = date.value.getDate()
  let minYear = 1970
  if (props.ani.year < minYear) {
    props.ani.year = minYear
    init()
    ElMessage.error(`最小年份为 ${minYear}`)
  }
}


let downloadLoading = ref(false)
let download = () => {
  downloadLoading.value = true
  api.post('api/ani?type=download', props.ani)
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        downloadLoading.value = false
      })
}

let downloadPathLoading = ref(false)
let downloadPath = () => {
  downloadPathLoading.value = true
  let newAni = JSON.parse(JSON.stringify(props.ani))
  newAni.customDownloadPath = false
  api.post('api/downloadPath', newAni)
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
  api.post('api/bgm?type=getTitle', props.ani)
      .then(res => {
        props.ani.title = res.data
      })
      .finally(() => {
        getBgmNameLoading.value = false
      })
}

let mikanCallback = v => {
  let {group, match, url} = v
  props.ani.url = url
  props.ani.subgroup = group

  let newMatch = JSON.parse(match).map(s => `{{${group}}}:${s}`)

  // 剔除旧的同字幕组规则
  props.ani.match = props.ani.match.filter(it => it.indexOf(`{{${group}}}:`) !== 0)

  props.ani.match.push(...newMatch)
}

let scrape = (force) => {
  api.post('api/scrape?force=' + force, props.ani)
      .then(res => {
        ElMessage.success(res.message)
      })
}

let props = defineProps(['ani'])
const emit = defineEmits(['callback'])
</script>
