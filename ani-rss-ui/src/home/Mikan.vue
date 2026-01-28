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
  <el-dialog v-model="matchDialogVisible" align-center center title="匹配" width="500">
    <div>
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
  <el-dialog v-model="dialogVisible" center title="Mikan">
    <el-checkbox-group v-model="rssList">
      <div class="content-wrapper">
        <div class="search-section">
          <div class="search-header">
            <el-input v-model:model-value="text" clearable placeholder="请输入搜索标题"
                      prefix-icon="Search"
                      @clear="()=>{
            text = ''
            search()
          }"
                      @keyup.enter="search"></el-input>
            <div class="spacer"></div>
            <el-button :loading="searchLoading" bg icon="Search" text @click="search">搜索</el-button>
          </div>
          <div v-if="data.seasons.length" class="flex season-selector">
            <el-select v-model:model-value="season" :disabled="text.length > 0 || loading" class="season-select"
                       @change="change">
              <el-option v-for="item in data.seasons" :key="item.year+' '+item.season"
                         :label="item.year+' '+item.season" :value="item.year+' '+item.season">
              </el-option>
            </el-select>
            <el-button :disabled="rssList.length < 1" bg icon="Plus" text @click="batchAddition">批量添加</el-button>
          </div>
        </div>
        <div v-loading="loading" class="scroll-container">
          <el-scrollbar>
            <el-collapse v-model="activeName">
              <el-collapse-item v-for="item in data.items" :name="item.label">
                <template #title>
                  <span style="margin-left: 4px;">
                    {{ item.label }}
                  </span>
                </template>
                <div class="collapse-content">
                  <el-collapse accordion @change="collapseChange">
                    <el-collapse-item v-for="it in item.items" :name="it.url">
                      <template #title>
                        <div class="flex collapse-title">
                          <img :src="img(it)" class="cover" @click.stop="open(it.url)">
                          <div class="flex collapse-title">
                            <el-text :truncated="false" line-clamp="1" size="small"
                                     class="title-text">
                              {{ it.title }}
                            </el-text>
                          </div>
                          <div v-if="it['score'] > 0" class="score-margin">
                            <h4 class="score-color">
                              {{ it['score'].toFixed(1) }}
                            </h4>
                          </div>
                          <el-badge v-if="it['exists']" class="item badge-margin" type="primary"
                                    value="已订阅"/>
                        </div>
                      </template>
                      <div v-if="selectName === it.url" v-loading="groupLoading"
                           class="group-content">
                        <el-collapse accordion>
                          <el-collapse-item v-for="group in groups[it.url]">
                            <template #title>
                              <div class="group-title-wrapper">
                                <div class="group-checkbox-wrapper">
                                  <el-checkbox :value="JSON.stringify(group)" class="checkbox-margin" @click.stop/>
                                </div>
                                <div class="single-line group-label">
                                  {{ group.label }}
                                  <el-text class="mx-1" size="small">{{ group['updateDay'] }}</el-text>
                                </div>
                                <div v-if="showTag()">
                                  <el-tag v-for="regexItem in group['regexList'].flat().slice(0, 5)" class="tag-margin">
                                    {{ regexItem.label }}
                                  </el-tag>
                                </div>
                                <div class="group-action">
                                  <el-button bg text @click.stop="callback({
                                  title:it.title,
                                  group:group.label,
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
                                      {{ ti.name }}
                                    </h5>
                                    <div class="item-footer">
                                      <p>
                                        {{ ti['sizeStr'] }}
                                        {{ ti['dateStr'] }}
                                      </p>
                                      <div>
                                        <el-button :icon="DocumentCopy" bg text @click="copy(ti['magnet'])"/>
                                        <el-button :icon="DownloadIcon" bg text @click="openUrl(ti['torrent'])"/>
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
import api from "@/js/api.js";
import {ElMessage, ElText} from "element-plus";
import {DocumentCopy, Download as DownloadIcon} from "@element-plus/icons-vue";
import {authorization} from "@/js/global.js";

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

let show = (name) => {
  season.value = ''
  dialogVisible.value = true
  text.value = ''
  data.value = {
    'seasons': [],
    'items': []
  }
  rssList.value = []
  if (name) {
    name = name.replace(/ ?\((19|20)\d{2}\)/g, "").trim()
    name = name.replace(/ ?\[tmdbid=(\d+)]/g, "").trim()
    if (name.length > 2) {
      text.value = name
      search()
      return
    }
  }
  list({})
}

let text = ref('')

let searchLoading = ref(false)
let search = () => {
  if (text.value.length === 1) {
    ElMessage.error("搜索最少需要两个字符")
    return
  }
  searchLoading.value = true
  list({}, text.value).finally(() => {
    searchLoading.value = false
  })
}

let list = async (body, text) => {
  loading.value = true
  text = text ? text : ''
  body = body ? body : {}
  return api.post('api/mikan?text=' + text, body)
      .then(res => {
        let {seasons, items, totalItems} = res.data;

        if (totalItems < 1) {
          ElMessage.warning("搜索结果为空")
        }

        if (seasons.length) {
          data.value.seasons = seasons
        }
        data.value.items = items
        if (items.length) {
          activeName.value = items[0].label
        }
        for (let item of data.value.seasons) {
          if (item['select'] && !season.value) {
            season.value = item['year'] + ' ' + item['season']
            return
          }
        }
      })
      .finally(() => {
        loading.value = false
      });
}

let change = (v) => {
  let body = data.value.seasons.filter(item => (item['year'] + ' ' + item['season']) === v)
  if (body.length) {
    list(body[0])
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
  api.get('api/mikan/group?url=' + v)
      .then(res => {
        groups.value[v] = res.data
      })
      .finally(() => {
        groupLoading.value = false
      })
}


let matchDialogVisible = ref(false)

let addAni = ref({
  'url': '',
  'match': '',
  'group': ''
})

let regexList = ref([])

let callback = v => {
  let {url, group} = v
  regexList.value = JSON.parse(JSON.stringify(v.regexList))

  addAni.value.url = url
  addAni.value.group = group
  addAni.value.match = '[]'

  regexList.value.push([])
  matchDialogVisible.value = true
}


let img = (it) => {
  return `api/file?img=${btoa(it['cover'])}&s=${authorization.value}`;
}

let showTag = () => {
  return window.innerWidth > 900;
}

let open = url => {
  window.open(url);
}

defineExpose({show})

let props = defineProps(['match'])
let emit = defineEmits(['callback'])


let batchAdditionNum = ref(0)
let batchAdditionDialogVisible = ref(false)

let batchAddition = async () => {
  batchAdditionNum.value = 0
  batchAdditionDialogVisible.value = true
  let getBangumiId = (url) => {
    console.log(url);
    const parsedUrl = new URL(url);
    return parsedUrl.searchParams.get('bangumiId');
  };

  try {
    ElMessage.success("添加中....")
    let map = rssList.value.reduce((acc, item) => {
      let bangumiId = getBangumiId(JSON.parse(item)['rss']);
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
        "type": "mikan"
      }

      ani = (await api.post('api/rss', ani)).data
      if (item.length > 1) {
        ani.standbyRssList = item.slice(1)
            .map(o => {
              return {
                label: o.label,
                url: o['rss'],
                offset: 0
              }
            })
      }
      batchAdditionNum.value += item.length
      await api.post('api/ani', ani)
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

let openUrl = (url) => window.open(url)

</script>

<style scoped>
.el-card {
  --el-card-padding: 15px;
}

.match-item {
  margin-right: 12px;
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

.search-header {
  display: flex;
  justify-content: space-between;
}

.spacer {
  width: 4px;
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
  margin-left: 4px;
  line-height: 1.6;
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
  width: 40px;
  height: 40px;
}
</style>
