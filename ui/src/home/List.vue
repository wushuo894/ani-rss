<template>
  <Edit ref="edit" @load="getList"/>
  <PlayList ref="playList"/>
  <div style="height: 100%;overflow: hidden;">
    <el-scrollbar>
      <div style="margin: 0 10px;min-height: 500px" v-loading="loading">
        <template v-for="weekItem in weekList">
          <div v-show="searchList(weekItem.i).length">
            <h2 style="margin: 16px 0 8px 4px;" v-if="weekItem.label.length">
              {{ weekItem.label }}
            </h2>
            <div class="grid-container">
              <div v-for="item in searchList(weekItem.i)" v-if="searchList(weekItem.i).length">
                <el-card shadow="never">
                  <div style="display: flex;width: 100%;align-items: center;">
                    <div style="height: 100%;">
                      <img :src="`/api/file?filename=${item['cover']}&s=${authorization()}`" height="130" width="92"
                           :alt="item.title"
                           @click="openBgmUrl(item)"
                           style="border-radius: 4px;cursor: pointer;">
                    </div>
                    <div style="flex-grow: 1;position: relative;">
                      <div style="margin-left: 10px;">
                        <div style="
                          column-count: 1;
                          overflow: hidden;
                          white-space: nowrap;
                          text-overflow: ellipsis;
                          width: 200px;
                          font-size: 0.97em;
                          line-height: 1.6;
                          font-weight: 500;
                          hyphens: auto;
                          letter-spacing: .0125em;">
                          {{ item.title }}
                        </div>
                        <div style="
                                    color: #9e9e9e !important;
                                    font-size: .75rem !important;
                                    font-weight: 300;
                                    line-height: 1.667;
                                    -webkit-line-clamp: 2;
                                    max-width: 220px;
                                    overflow: hidden;
                                    text-overflow: ellipsis;
                                    letter-spacing: .0333333333em !important;
                                    font-family: Roboto, sans-serif;
                                    text-transform: none !important;">
                          {{ item.url }}
                        </div>
                        <div style="
                        width: 180px;
                        display: grid;
                        grid-gap: 4px;
                        "
                             :class="itemsPerRow > 1 ? 'gtc3' : 'gtc2'"
                        >
                          <el-tag>
                            第 {{ item.season }} 季
                          </el-tag>
                          <el-tag type="success" v-if="item.enable">
                            已启用
                          </el-tag>
                          <el-tag type="success" v-else>
                            未启用
                          </el-tag>
                          <el-tag type="info" v-if="itemsPerRow > 1">
                            {{ item['subgroup'] ? item['subgroup'] : '未知' }}
                          </el-tag>
                          <el-tag type="info" v-else>
                            {{ (item['subgroup'] ? item['subgroup'] : '未知').substr(0, 6) }}
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
                        </div>
                      </div>
                      <div
                          style="display: flex;align-items: flex-end;justify-content:flex-end; flex-direction: column;position: absolute;right: 0;bottom: 0;">
                        <el-button text @click="playList?.show(item)" bg v-if="showPlaylist">
                          <el-icon>
                            <Files/>
                          </el-icon>
                        </el-button>
                        <div style="height: 5px;" v-if="showPlaylist"></div>
                        <el-button text @click="edit?.show(item)" bg>
                          <el-icon>
                            <EditIcon/>
                          </el-icon>
                        </el-button>
                        <div style="height: 5px;"></div>
                        <popconfirm title="你确定要删除吗?" @confirm="delAni(item)">
                          <template #reference>
                            <el-button type="danger" text :loading="item['deleteLoading']" bg>
                              <el-icon>
                                <Delete/>
                              </el-icon>
                            </el-button>
                          </template>
                        </popconfirm>
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
    <el-affix position="bottom">
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
                  <template v-if="itemsPerRow > 1">
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
import {ElMessage} from 'element-plus'
import {Back, Delete, Edit as EditIcon, Files} from "@element-plus/icons-vue"
import Edit from "./Edit.vue";
import api from "../api.js";
import Popconfirm from "../other/Popconfirm.vue";
import PlayList from "../play/PlayList.vue";


const weekList = ref([
  {
    i: 1,
    label: '星期日'
  },
  {
    i: 2,
    label: '星期一'
  },
  {
    i: 3,
    label: '星期二'
  },
  {
    i: 4,
    label: '星期三'
  },
  {
    i: 5,
    label: '星期四'
  },
  {
    i: 6,
    label: '星期五'
  },
  {
    i: 7,
    label: '星期六'
  }]
)

const pagerCount = ref(10)
const edit = ref()
const pageSize = ref(40)
const loading = ref(true)
const playList = ref()
const showPlaylist = ref(false)

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

const delAni = (ani) => {
  ani['deleteLoading'] = true
  api.del('/api/ani', [ani.id])
      .then(res => {
        ElMessage.success(res.message)
        getList()
      })
      .finally(() => {
        ani['deleteLoading'] = false
      })
}

const list = ref([])
const weekShow = ref(false)

const getList = () => {
  api.get('/api/config')
      .then(res => {
        showPlaylist.value = res.data.showPlaylist
        weekShow.value = res.data.weekShow
        if (!weekShow.value) {
          weekList.value = [{
            i: 1,
            label: ''
          }]
        }
        api.get('/api/ani')
            .then(res => {
              list.value = res.data
            })
            .finally(() => {
              loading.value = false
              updateGridLayout()
            })
      })
}

let authorization = () => {
  return window.authorization;
}

const itemsPerRow = ref(1)

let updateGridLayout = () => {
  const gridContainer = document.querySelectorAll('.grid-container');
  if (!gridContainer.length) {
    return
  }
  const windowWidth = window.innerWidth;
  if (windowWidth) {
    document.querySelector('.el-affix').style['width'] = windowWidth + 'px'
  }
  itemsPerRow.value = Math.max(1, Math.floor(windowWidth / 400));

  for (let gridContainerElement of gridContainer) {
    gridContainerElement.style.gridTemplateColumns = `repeat(${itemsPerRow.value}, 1fr)`;
  }

  if (itemsPerRow.value === 1) {
    pagerCount.value = 4
  }
}

onMounted(() => {
  let size = window.localStorage.getItem('pageSize')
  if (size) {
    pageSize.value = Number.parseInt(size)
  }
  window.addEventListener('resize', updateGridLayout);
  getList()

  let day = new Date().getDay()
  weekList.value = weekList.value.slice(day, weekList.value.length).concat(weekList.value.slice(0, day))
})

let logout = () => {
  localStorage.removeItem('authorization')
  location.reload()
}

let elIconClass = () => {
  return itemsPerRow.value > 1 ? 'el-icon--left' : '';
}

let yearMonth = () => {
  return new Set(list.value.map(it => `${it.year}-${it.month < 10 ? '0' + it.month : it.month}`).sort((a, b) => a > b ? -1 : 1));
}

let openBgmUrl = (it) => {
  if (it.bgmUrl.length) {
    window.open(it.bgmUrl)
    return
  }
  if (it.title.length) {
    let title = it.title.replace(/\(\d{4}\)$/g, "").trim()
    window.open(`https://bgm.tv/subject_search/${title}?cat=2`)
  }
}

defineExpose({
  getList, yearMonth
})

let props = defineProps(['title', 'filter'])

</script>

<style>
.gtc3 {
  grid-template-columns: repeat(3, 1fr);
}

.gtc2 {
  grid-template-columns: repeat(2, 1fr);
}
</style>


