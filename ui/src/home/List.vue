<template>
  <Edit ref="refEdit" @load="getList"/>
  <PlayList ref="playList"/>
  <Cover ref="refCover" @load="getList"/>
  <Del ref="refDel" @load="getList"/>
  <BgmRate ref="bgmRateRef"/>
  <div style="height: 100%;overflow: hidden;">
    <el-scrollbar>
      <div style="margin: 0 10px;min-height: 500px" v-loading="loading">
        <template v-for="weekItem in weekList">
          <div v-show="searchList(weekItem.i).length">
            <h2 style="margin: 16px 0 8px 4px;" v-if="weekItem['label'].length">
              {{ weekItem['label'] }}
            </h2>
            <div class="grid-container">
              <div v-for="item in searchList(weekItem['i'])" v-if="searchList(weekItem['i']).length">
                <el-card shadow="never">
                  <div style="display: flex;width: 100%;align-items: center;">
                    <div style="height: 100%;">
                      <img :src="`api/file?filename=${item['cover']}&s=${authorization()}`" height="130" width="92"
                           :alt="item.title"
                           style="border-radius: 4px;cursor: pointer;"
                           @click="refCover?.show(item)"/>
                    </div>
                    <div style="flex-grow: 1;position: relative;">
                      <div style="margin-left: 8px;">
                        <div class="flex">
                          <el-tooltip :content="item.title" placement="top">
                            <el-text line-clamp="1"
                                     @click="openBgmUrl(item)"
                                     style="max-width: 200px;
                                       line-height: 1.6;
                                       letter-spacing: 0.0125em;
                                       font-weight: 500;
                                       font-size: 0.97em;
                                       cursor: pointer;
                                       color: var(--el-text-color-primary);"
                                     truncated>
                              {{ item.title }}
                            </el-text>
                          </el-tooltip>
                        </div>
                        <div style="margin-bottom: 8px;" v-if="scoreShow">
                          <h4 style="color: #E800A4;cursor: pointer;" @click="bgmRateRef?.show(item)">
                            {{ item['score'].toFixed(1) }}
                          </h4>
                        </div>
                        <el-text v-else line-clamp="2" size="small">
                          {{ item.url }}
                        </el-text>
                        <div style="
                        width: 180px;
                        display: grid;
                        grid-gap: 4px;
                        "
                             :class="isNotMobile() ? 'gtc3' : 'gtc2'"
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
                              <el-text line-clamp="1" size="small" style="max-width: 60px;color: var(--el-color-info);">
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
                          <el-tag v-if="item.backRssList.length > 0">
                            备用RSS
                          </el-tag>
                        </div>
                        <el-text v-if="item['lastDownloadTime'] && item.lastDownloadFormat" size="small" style="margin-left: 4px;"
                                 type="info">
                          {{ item.lastDownloadFormat }}
                        </el-text>
                      </div>
                      <div
                          style="display: flex;align-items: flex-end;justify-content:flex-end; flex-direction: column;position: absolute;right: 0;bottom: 0;">
                        <el-button text @click="playList?.show(item)" bg v-if="showPlaylist">
                          <el-icon>
                            <Files/>
                          </el-icon>
                        </el-button>
                        <div style="height: 5px;" v-if="showPlaylist"></div>
                        <el-button bg text @click="refEdit?.show(item)">
                          <el-icon>
                            <EditIcon/>
                          </el-icon>
                        </el-button>
                        <div style="height: 5px;"></div>
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
        <div style="height: 80px;"></div>
      </div>
    </el-scrollbar>
    <el-affix position="bottom" :style="`width: ${width}px`">
      <div style="width: 100%;
                                                      background: linear-gradient(to bottom,rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.01) );
                                                      backdrop-filter: blur(2px);padding-top: 10px;z-index: 99999"
           id="page">
        <div style="display: flex;justify-content: end;width: 100%;">
          <div style="margin-right: 10px;margin-bottom: 10px;">
            <popconfirm title="你确定要退出吗?" @confirm="logout">
              <template #reference>
                <el-button type="danger" bg text>
                  <el-icon :class="elIconClass()">
                    <Back/>
                  </el-icon>
                  <template v-if="isNotMobile()">
                    退出登录
                  </template>
                </el-button>
              </template>
            </popconfirm>
          </div>
        </div>
      </div>
    </el-affix>
  </div>
</template>

<script setup>
import {onMounted, ref} from "vue";
import {Back, Delete, Edit as EditIcon, Files} from "@element-plus/icons-vue"
import Edit from "./Edit.vue";
import api from "../api.js";
import Popconfirm from "../other/Popconfirm.vue";
import PlayList from "../play/PlayList.vue";
import Cover from "./Cover.vue";
import Del from "./Del.vue";
import {useWindowSize} from "@vueuse/core";
import BgmRate from "./BgmRate.vue";
import formatTime from "../date-format.js";

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
const pageSize = ref(40)
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
        if (it['title'].toString().indexOf(text) > -1) {
          return true
        }
        let pinyin = it['pinyin']
        if (pinyin.indexOf(text) > -1) {
          return true
        }
        if (pinyin.replaceAll(' ', '').indexOf(text.replaceAll(' ', '')) > -1) {
          return true
        }
        return pinyin.split(' ').map(s => s.substring(0, 1)).join('').indexOf(text) > -1;
      });
}

const list = ref([])
const weekShow = ref(false)

const getList = () => {
  loading.value = true

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

let authorization = () => {
  return window.authorization;
}


let updateGridLayout = () => {
  const gridContainer = document.querySelectorAll('.grid-container');
  if (!gridContainer.length) {
    return
  }
  let itemsPerRow = Math.max(1, Math.floor(width.value / 400));

  for (let gridContainerElement of gridContainer) {
    gridContainerElement.style.gridTemplateColumns = `repeat(${itemsPerRow}, 1fr)`;
  }
}

onMounted(() => {
  let size = window.localStorage.getItem('pageSize')
  if (size) {
    pageSize.value = Number.parseInt(size)
  }
  window.addEventListener('resize', updateGridLayout);
  getList()
})

let logout = () => {
  localStorage.removeItem('authorization')
  location.reload()
}

let elIconClass = () => {
  return isNotMobile() ? 'el-icon--left' : '';
}

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

let isNotMobile = () => {
  return width.value > 800;
}

const {width} = useWindowSize()

defineExpose({
  getList, yearMonth
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

<style>

.gtc3 {
  grid-template-columns: repeat(3, 1fr);
}

.gtc2 {
  grid-template-columns: repeat(2, 1fr);
}
</style>


