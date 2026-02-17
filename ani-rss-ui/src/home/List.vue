<template>
  <Edit ref="refEdit"/>
  <PlayList ref="playList"/>
  <Cover ref="refCover"/>
  <Del ref="refDel"/>
  <BgmRate ref="bgmRateRef"/>
  <div class="list-container">
    <el-scrollbar>
      <div class="list-content" v-loading="loading">
        <template v-for="weekItem in weekList">
          <div v-show="searchList(weekItem.i).length">
            <h2 class="list-week-title" v-if="weekItem['label'].length">
              {{ weekItem['label'] }}
            </h2>
            <div class="grid-container">
              <div v-for="item in searchList(weekItem['i'])" v-if="searchList(weekItem['i']).length">
                <el-card shadow="never">
                  <div class="list-card-content">
                    <div class="list-card-image-container">
                      <img :src="`api/file?filename=${item['cover']}&s=${authorization}`" height="130" width="92"
                           :alt="item.title"
                           class="list-card-image"
                           @click="refCover?.show(item)"/>
                    </div>
                    <div class="list-card-info">
                      <div class="list-card-info-inner">
                        <div class="flex">
                          <el-tooltip :content="item.title" placement="top">
                            <el-text :line-clamp="1"
                                     @click="openBgmUrl(item)"
                                     class="list-card-title"
                                     truncated>
                              {{ item.title }}
                            </el-text>
                          </el-tooltip>
                        </div>
                        <div class="list-card-score-container" v-if="scoreShow">
                          <h4 class="list-card-score" @click="bgmRateRef?.show(item)">
                            {{ item['score'].toFixed(1) }}
                          </h4>
                        </div>
                        <el-text v-else
                                 line-clamp="2"
                                 size="small"
                                 class="list-card-url">
                          {{ decodeURLComponentSafe(item.url) }}
                        </el-text>
                        <div class="list-card-tags"
                             :class="isNotMobile ? 'gtc3' : 'gtc2'"
                        >
                          <el-tag>
                            第 {{ item.season }} 季
                          </el-tag>
                          <el-tag type="success" v-if="item.enable">
                            已启用
                          </el-tag>
                          <el-tag type="info" v-else>
                            未启用
                          </el-tag>
                          <el-tag type="info">
                            <el-tooltip :content="item['subgroup']">
                              <el-text line-clamp="1" size="small" class="list-card-subgroup">
                                {{ item['subgroup'] ? item['subgroup'] : '未知字幕组' }}
                              </el-text>
                            </el-tooltip>
                          </el-tag>
                          <el-tag type="warning">
                            {{ item['currentEpisodeNumber'] }} /
                            {{ item['totalEpisodeNumber'] ? item['totalEpisodeNumber'] : '*' }}
                          </el-tag>
                          <el-tag type="danger" v-if="item.ova">
                            ova
                          </el-tag>
                          <el-tag type="danger" v-else>
                            tv
                          </el-tag>
                          <el-tag v-if="item.standbyRssList.length > 0">
                            备用RSS
                          </el-tag>
                        </div>
                        <el-text v-if="item['lastDownloadTime'] && item.lastDownloadFormat" size="small"
                                 type="info">
                          {{ item.lastDownloadFormat }}
                        </el-text>
                      </div>
                      <div class="list-card-actions">
                        <el-button text @click="playList?.show(item)" bg v-if="showPlaylist">
                          <el-icon>
                            <Files/>
                          </el-icon>
                        </el-button>
                        <div class="list-card-spacer" v-if="showPlaylist"></div>
                        <el-button bg text @click="refEdit?.show(item)">
                          <el-icon>
                            <EditIcon/>
                          </el-icon>
                        </el-button>
                        <div class="list-card-spacer"></div>
                        <el-button type="danger" text @click="refDel?.show([item])" bg>
                          <el-icon>
                            <Delete/>
                          </el-icon>
                        </el-button>
                      </div>
                    </div>
                  </div>
                </el-card>
              </div>
            </div>
          </div>
        </template>
        <div class="list-bottom-spacer"></div>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import {onMounted, ref} from "vue";
import {Delete, Edit as EditIcon, Files} from "@element-plus/icons-vue"
import Edit from "./Edit.vue";
import api from "@/js/api.js";
import PlayList from "@/play/PlayList.vue";
import Cover from "./Cover.vue";
import Del from "./Del.vue";
import BgmRate from "./BgmRate.vue";
import formatTime from "@/js/format-time.js";
import {authorization, isNotMobile} from "@/js/global.js";

const defaultWeekList = [
  {
    i: 0,
    label: '星期日'
  },
  {
    i: 6,
    label: '星期六'
  },
  {
    i: 5,
    label: '星期五'
  },
  {
    i: 4,
    label: '星期四'
  },
  {
    i: 3,
    label: '星期三'
  },
  {
    i: 2,
    label: '星期二'
  },
  {
    i: 1,
    label: '星期一'
  },
]
const weekList = ref(defaultWeekList)

const refEdit = ref()
const refDel = ref()
const loading = ref(true)
const playList = ref()
const scoreShow = ref(false)
const showPlaylist = ref(false)
const refCover = ref()
const bgmRateRef = ref()

const searchList = (week) => {
  const text = props.title.trim()
  if (text.length < 1) {
    return list.value.filter(props.filter).filter(it => !weekShow.value || week === it.week)
  }
  return list.value
      .filter(props.filter)
      .filter(it => !weekShow.value || week === it.week)
      .filter(it => {
        let {title, pinyin, pinyinInitials} = it
        return title.indexOf(text) > -1 ||
            pinyin.indexOf(text) > -1 ||
            pinyinInitials.indexOf(text) > -1;
      });
}

const list = ref([])
const weekShow = ref(false)

const getList = () => {
  loading.value = true

  list.value = []
  api.get('api/config')
      .then(res => {
        showPlaylist.value = res.data.showPlaylist
        weekShow.value = res.data.weekShow
        scoreShow.value = res.data.scoreShow
        if (weekShow.value) {
          weekList.value = defaultWeekList;

          // 0表示周日，1表示周一
          let day = new Date().getDay()

          let currentDay = weekList.value.find(it => it.i === day)
          let currentDayIndex = weekList.value.indexOf(currentDay)

          weekList.value = weekList.value
              .slice(currentDayIndex, weekList.value.length)
              .concat(weekList.value.slice(0, currentDayIndex))
        } else {
          weekList.value = [{i: 1, label: ''}];
        }
        let showLastDownloadTime = res.data['showLastDownloadTime']
        api.get('api/ani')
            .then(res => {
              if (showLastDownloadTime) {
                list.value = res.data.map(it => {
                  return {...it, lastDownloadFormat: formatTime(it['lastDownloadTime'])}
                })
              } else {
                list.value = res.data
              }
              updateGridLayout()
            })
            .finally(() => {
              loading.value = false
            })
      })
}

let updateGridLayout = () => {
  const app = document.querySelector('#app');
  let gridColumns = Math.max(1, Math.floor(app.offsetWidth / 400));

  const el = document.documentElement
  el.style.setProperty('--grid-columns', gridColumns)
}

onMounted(() => {
  window.addEventListener('resize', updateGridLayout);
  window.$reLoadList = getList
  getList()
})

let yearMonth = () => {
  return new Set(list.value
      .map(it => `${it['year']}-${it['month'] < 10 ? '0' + it['month'] : it['month']}`)
      .sort((a, b) => a > b ? -1 : 1)
  );
}

let openBgmUrl = (it) => {
  if (it.bgmUrl.length) {
    window.open(it.bgmUrl)
    return
  }
  if (it.title.length) {
    let title = it.title.replace(/ ?\((19|20)\d{2}\)/g, "").trim()
    title = title.replace(/ ?\[tmdbid=(\d+)]/g, "").trim()
    window.open(`https://bgm.tv/subject_search/${title}?cat=2`)
  }
}

let decodeURLComponentSafe = (str) => {
  return decodeURIComponent(str.replace('+', ' '));
}

defineExpose({
  yearMonth
})

let props = defineProps({
  currentPage: {
    type: Number,
    default: 1
  },
  title: String,
  filter: Function
})

</script>

<style scoped>
.grid-container {
  display: grid;
  grid-gap: 8px;
  width: 100%;
  grid-template-columns: repeat(var(--grid-columns), 1fr);
}

.list-container {
  height: 100%;
  overflow: hidden;
}

.list-content {
  margin: 0 10px;
  min-height: 500px;
}

.list-week-title {
  margin: 16px 0 8px 4px;
}

.list-card-content {
  display: flex;
  width: 100%;
  align-items: center;
}

.list-card-image-container {
  height: 100%;
}

.list-card-image {
  border-radius: var(--el-border-radius-base);
  cursor: pointer;
}

.list-card-info {
  flex-grow: 1;
  position: relative;
}

.list-card-info-inner {
  margin-left: 8px;
}

.list-card-title {
  width: 200px;
  line-height: 1.6;
  letter-spacing: 0.0125em;
  font-weight: 500;
  font-size: 0.97em;
  cursor: pointer;
  color: var(--el-text-color-primary);
}

.list-card-score-container {
  margin-bottom: 8px;
}

.list-card-score {
  color: #E800A4;
  cursor: pointer;
}

.list-card-url {
  max-width: 300px;
}

.list-card-tags {
  width: 180px;
  display: grid;
  grid-gap: 4px;
}

.list-card-subgroup {
  max-width: 60px;
  color: var(--el-color-info);
}

.list-card-actions {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  flex-direction: column;
  position: absolute;
  right: 0;
  bottom: 0;
}

.list-card-spacer {
  height: 5px;
}

.list-bottom-spacer {
  height: 80px;
}

.gtc3 {
  grid-template-columns: repeat(3, 1fr);
}

.gtc2 {
  grid-template-columns: repeat(2, 1fr);
}
</style>


