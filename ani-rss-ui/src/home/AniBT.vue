<template>
  <el-dialog v-model="batchAdditionDialogVisible" align-center center title="正在批量添加订阅"
             width="500"
             :close-on-click-modal="false"
             :close-on-press-escape="false"
             :show-close="false">
    <div>
      <el-progress :percentage="Number.parseInt((batchAdditionNum / rssList.length) * 100.0)"/>
    </div>
    <div>
      {{ batchAdditionNum }} / {{ rssList.length }}
    </div>
  </el-dialog>
  <el-dialog v-model="matchDialogVisible" align-center center title="匹配" width="auto">
    <div class="match-content">
      <el-radio-group v-model="addAni.match">
        <div v-for="regexItems in regexList" class="match-item">
          <el-radio :label="JSON.stringify(regexItems)"
                    :value="JSON.stringify(regexItems.map(it => it.regex))">
            <el-tag v-if="regexItems.length" v-for="regexItem in regexItems" class="tag-margin">
              {{ regexItem.label }}
            </el-tag>
            <el-tag v-else type="success">全部</el-tag>
          </el-radio>
        </div>
      </el-radio-group>
    </div>
    <div class="dialog-footer">
      <el-button icon="Check" @click="async ()=>{
          emit('callback', addAni)
          dialogVisible = false
          matchDialogVisible = false
      }" text bg>确定
      </el-button>
    </div>
  </el-dialog>
  <el-dialog v-model="dialogVisible" center title="AniBT">
    <el-checkbox-group v-model="rssList">
      <div class="content-wrapper">
        <div class="search-section">
          <div class="flex season-selector">
            <el-select v-model="season" class="season-select"
                       @change="change">
              <el-option v-for="itemSeason in data.seasons" :key="itemSeason"
                         :label="itemSeason" :value="itemSeason">
              </el-option>
            </el-select>
            <el-button :disabled="rssList.length < 1" bg icon="Plus" text @click="batchAddition">批量添加</el-button>
          </div>
        </div>
        <div v-loading="loading" class="scroll-container">
          <el-scrollbar>
            <el-collapse v-model="activeName">
              <el-collapse-item v-for="item in data.items" :name="item.weekdayLabel">
                <template #title>
                  <span style="margin-left: 4px;font-weight: bold;">
                    {{ item.weekdayLabel }}
                  </span>
                </template>
                <div class="collapse-content">
                  <el-collapse accordion @change="collapseChange">
                    <el-collapse-item v-for="anime in item.animes" :name="anime['bgmId']">
                      <template #title>
                        <div class="flex collapse-title">
                          <img :src="img(anime)" class="cover"
                               @click.stop="open(`https://anibt.net/anime/${anime['bgmId']}`)">
                          <div class="flex collapse-title">
                            <el-text :truncated="false" line-clamp="1" size="small"
                                     class="title-text">
                              {{ anime.title.primary }}
                            </el-text>
                          </div>
                          <div v-if="anime['rating'] > 0" class="score-margin">
                            <h4 class="score-color">
                              {{ anime['rating'].toFixed(1) }}
                            </h4>
                          </div>
                          <el-badge v-if="anime['exists']" class="item badge-margin" type="primary"
                                    value="已订阅"/>
                        </div>
                      </template>
                      <div v-if="selectName === anime.bgmId" v-loading="groupLoading"
                           class="group-content">
                        <el-collapse accordion>
                          <el-collapse-item v-for="group in groups[anime.bgmId]">
                            <template #title>
                              <div class="group-title-wrapper">
                                <div class="group-checkbox-wrapper">
                                  <el-checkbox :value="JSON.stringify(group)" class="checkbox-margin" @click.stop/>
                                </div>
                                <div class="group-label">
                                  <el-text style="max-width: 100px;" truncated>{{ group.name }}</el-text>
                                  &nbsp;
                                  <el-text class="mx-1" size="small">{{ group['updateDay'] }}</el-text>
                                </div>
                                <div v-if="showTag()">
                                  <el-tag v-for="tag in group['tags'].slice(0, 5)"
                                          class="tag-margin">
                                    {{ tag }}
                                  </el-tag>
                                </div>
                                <div class="group-action">
                                  <el-button bg @click.stop="callback({
                                  bgmUrl:`https://bgm.tv/subject/${anime.bgmId}`,
                                  title:anime.title.primary,
                                  group:group.name,
                                  url:group['rss'],
                                  regexList:group['regexList']
                                })" icon="Plus">
                                    添加
                                  </el-button>
                                </div>
                              </div>
                            </template>
                            <div class="group-items">
                              <div v-for="ti in group.items" class="item-margin">
                                <el-card shadow="never">
                                  <div>
                                    <h5>
                                      {{ ti.title }}
                                    </h5>
                                    <div class="item-footer">
                                      <p>
                                        {{ formatTime(ti['publishedAt']) }}
                                        {{ ti['resolution'] }}
                                      </p>
                                      <div>
                                        <el-button :icon="DocumentCopy" bg text @click="copy(ti['magnet']  )"/>
                                      </div>
                                    </div>
                                  </div>
                                </el-card>
                              </div>
                            </div>
                          </el-collapse-item>
                        </el-collapse>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </el-collapse-item>
            </el-collapse>
          </el-scrollbar>
        </div>
      </div>
    </el-checkbox-group>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage, ElText} from "element-plus";
import {DocumentCopy} from "@element-plus/icons-vue";
import {authorization} from "@/js/global.js";
import * as http from "@/js/http.js";
import formatTime from "../js/format-time.js";

// 批量添加订阅
let rssList = ref([]);

let groupLoading = ref(false)
let activeName = ref("")
let dialogVisible = ref(false)
let loading = ref(false)
let data = ref({
  'seasons': [],
  'items': []
})

let season = ref('')

let show = (bgmUrl = '') => {
  dialogVisible.value = true
  data.value = {
    'seasons': [],
    'items': []
  }
  rssList.value = []
  list(bgmUrl)
}

let list = async (bgmUrl = '') => {
  loading.value = true
  return http.aniBT(season.value, bgmUrl)
      .then(res => {
        let {requestedSeason, availableSeasons, byWeekday} = res.data;

        season.value = requestedSeason
        data.value.seasons = availableSeasons
        data.value.items = byWeekday
        if (byWeekday.length) {
          activeName.value = byWeekday[0].weekdayLabel
        }
      })
      .finally(() => {
        loading.value = false
      });
}

let change = (v) => {
  let body = data.value.seasons.filter(item => item === v)
  if (body.length) {
    list()
  }
}

let selectName = ref('')
let groups = ref({})

let collapseChange = (v) => {
  if (!v) {
    return
  }
  selectName.value = v
  if (groups.value[v]) {
    return;
  }
  groupLoading.value = true
  http.aniBTGroup(v)
      .then(res => {
        groups.value[v] = res.data
      })
      .finally(() => {
        groupLoading.value = false
      })
}


let matchDialogVisible = ref(false)

let addAni = ref({
  'bgmUrl': '',
  'url': '',
  'match': '',
  'group': ''
})

let regexList = ref([])

let callback = v => {
  let {url, group, bgmUrl} = v
  regexList.value = JSON.parse(JSON.stringify(v.regexList))

  addAni.value.bgmUrl = bgmUrl
  addAni.value.url = url
  addAni.value.group = group
  addAni.value.match = '[]'

  regexList.value.push([])
  matchDialogVisible.value = true
}


let img = (it) => {
  return `api/aniBTCover?img=${btoa(it['cover'])}&s=${authorization.value}`;
}

let showTag = () => {
  return window.innerWidth > 900;
}

let open = url => {
  window.open(url);
}

defineExpose({show})

let emit = defineEmits(['callback'])

let batchAdditionNum = ref(0)
let batchAdditionDialogVisible = ref(false)

let batchAddition = async () => {
  batchAdditionNum.value = 0
  batchAdditionDialogVisible.value = true
  let getBgmId = (url) => {
    const parsedUrl = new URL(url);
    return parsedUrl.searchParams.get('bgmId');
  };

  try {
    ElMessage.success("添加中....")
    let map = rssList.value.reduce((acc, item) => {
      let bangumiId = getBgmId(JSON.parse(item)['rss']);
      if (!acc[bangumiId]) {
        acc[bangumiId] = [];
      }
      acc[bangumiId].push(JSON.parse(item));
      return acc;
    }, {})
    for (let item of Object.values(map)) {
      let ani = {
        "url": item[0]['rss'],
        "season": 1,
        "offset": 0,
        "title": "",
        "exclude": [],
        "totalEpisodeNumber": 0,
        "match": [],
        "type": "ani-bt",
        "bgmUrl": `https://bgm.tv/subject/${item[0].bgmId}`,
        "subgroup": item[0].name
      }

      ani = (await http.rssToAni(ani)).data
      if (item.length > 1) {
        ani.standbyRssList = item.slice(1)
            .map(o => {
              return {
                label: o.name,
                url: o['rss'],
                offset: 0
              }
            })
      }
      batchAdditionNum.value += item.length
      await http.addAni(ani)
    }
    ElMessage.success("添加成功")

    setTimeout(() => {
      location.reload()
    }, 1000)
  } catch (e) {
    ElMessage.error(e)
  } finally {
    batchAdditionDialogVisible.value = false
  }
}

let copy = (v) => {
  const input = document.createElement('input');
  input.value = v;
  document.body.appendChild(input);
  input.select();
  document.execCommand('copy');
  document.body.removeChild(input);
  ElMessage.success('已复制')
}

</script>

<style scoped>
.el-collapse {
  --el-collapse-header-height: 55px;
}

.match-item {
  margin-right: 12px;
  display: inline;
}

.tag-margin {
  margin-right: 4px;
}

.dialog-footer {
  display: flex;
  width: 100%;
  justify-content: end;
}

.content-wrapper {
  min-height: 300px;
}

.search-section {
  margin: 4px;
}

.season-selector {
  margin-top: 4px;
  width: 100%;
  justify-content: space-between;
}

.season-select {
  max-width: 140px;
}

.scroll-container {
  margin: 8px 0 4px 0;
  height: 600px;
}

.collapse-content {
  margin-left: 15px;
}

.collapse-title {
  align-items: center;
}

.title-text {
  margin-left: 6px;
  line-height: 1.6;
  font-weight: bold;
}

.score-margin {
  margin-left: 4px;
}

.score-color {
  color: #E800A4;
}

.badge-margin {
  margin-left: 4px;
}

.group-content {
  margin-left: 15px;
  min-height: 50px;
}

.group-title-wrapper {
  width: 100%;
  display: flex;
  justify-content: space-between;
}

.group-checkbox-wrapper {
  height: 100%;
}

.checkbox-margin {
  margin-right: 8px;
}

.group-label {
  display: flex;
  align-items: center;
  flex: 1;
  text-align: start;
}

.group-action {
  display: flex;
  align-items: center;
  margin-right: 14px;
  margin-left: 4px;
}

.group-items {
  margin-left: 15px;
}

.item-margin {
  margin-bottom: 4px;
}

.item-footer {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cover {
  border-radius: var(--el-border-radius-base);
  cursor: pointer;
  width: 45px;
  height: 45px;
}

.match-content {
  max-width: 500px;
  min-width: 200px;
  margin-bottom: 4px;
}
</style>
