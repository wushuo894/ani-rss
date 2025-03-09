<template>
  <el-dialog v-model="matchDialogVisible" align-center center title="匹配" width="500">
    <div>
      <el-radio-group v-model="addAni.match">
        <div v-for="match in matchList" style="margin-right: 12px;">
          <el-radio :label="JSON.stringify(match)" :value="JSON.stringify(match)">
            <el-tag v-if="match.length" v-for="item in match" style="margin-right: 4px;">{{ item }}</el-tag>
            <el-tag v-else type="success">全部</el-tag>
          </el-radio>
        </div>
      </el-radio-group>
    </div>
    <div style="display: flex;width: 100%;justify-content: end;">
      <el-button icon="Check" @click="async ()=>{
          emit('add', addAni)
          dialogVisible = false
          matchDialogVisible = false
      }" text bg>确定
      </el-button>
    </div>
  </el-dialog>
  <el-dialog v-model="dialogVisible" center title="Mikan">
    <div style="min-height: 300px;">
      <div style="margin: 4px;">
        <div style="display: flex;justify-content: space-between;">
          <el-input v-model:model-value="text" placeholder="请输入搜索标题" @keyup.enter="search"
                    prefix-icon="Search"
                    clearable
                    @clear="()=>{
            text = ''
            search()
          }"></el-input>
          <div style="width: 4px;"></div>
          <el-button @click="search" :loading="searchLoading" text bg icon="Search">搜索</el-button>
        </div>
        <div style="max-width: 140px;margin-top: 4px;">
          <el-select v-if="data.seasons.length" v-model:model-value="season" @change="change"
                     :disabled="text.length > 0 || loading">
            <el-option :label="item.year+' '+item.season" :key="item.year+' '+item.season"
                       :value="item.year+' '+item.season" v-for="item in data.seasons">
            </el-option>
          </el-select>
        </div>
      </div>
      <div style="margin: 8px 0 4px 0;height: 600px;" v-loading="loading">
        <el-scrollbar>
          <el-collapse v-model="activeName">
            <el-collapse-item v-for="item in data.items" :title="item.label" :name="item.label">
              <div style="margin-left: 15px;">
                <el-collapse @change="collapseChange" accordion>
                  <el-collapse-item v-for="it in item.items" :name="it.url">
                    <template #title>
                      <img :src="img(it)" height="40" width="40" @click.stop="open(it.url)">
                      <div class="single-line" style="margin-left: 5px;max-width: 70%;">
                        {{ it.title }}
                      </div>
                      <el-badge value="已订阅" class="item" type="primary" v-if="it['exists']"/>
                    </template>
                    <div style="margin-left: 15px;min-height: 50px;" v-if="selectName === it.url"
                         v-loading="groupLoading">
                      <el-collapse accordion>
                        <el-collapse-item v-for="group in groups[it.url]">
                          <template #title>
                            <div style="width: 100%;display: flex;justify-content: space-between;">
                              <div class="single-line" style="flex: 1;text-align: start;">
                                {{ group.label }}
                                <el-text class="mx-1" size="small">{{ group['updateDay'] }}</el-text>
                              </div>
                              <div v-if="showTag()">
                                <el-tag v-for="tag in group['tags'].slice(0, 5)" style="margin-right: 4px;">{{
                                    tag
                                  }}
                                </el-tag>
                              </div>
                              <div style="display: flex;align-items: center;margin-right: 14px;margin-left: 4px;">
                                <el-button text bg @click.stop="add({
                                  'title':it.title,
                                  'group':group.label,
                                  'url':group['rss'],
                                  'matchList':group['matchList']
                                })" icon="Plus">
                                  添加
                                </el-button>
                              </div>
                            </div>
                          </template>
                          <div style="margin-left: 15px;">
                            <div v-for="ti in group.items" style="margin-bottom: 4px;">
                              <el-card shadow="never">
                                <div>
                                  <h5>
                                    {{ ti.name }}
                                  </h5>
                                  <div style="width: 100%;display: flex;justify-content: end;">
                                    {{ ti['sizeStr'] }}
                                    {{ ti['dateStr'] }}
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
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import api from "../api.js";
import {ElMessage, ElText} from "element-plus";

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
        let seasons = res.data['seasons']
        let items = res.data['items']
        if (seasons.length) {
          data.value.seasons = seasons
        }
        data.value.items = items
        if (items.length) {
          activeName.value = items[0].label
          if (!items[0].items.length) {
            ElMessage.warning("搜索结果为空")
          }
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
  'match': ''
})

let matchList = ref([])

let add = (v) => {
  matchList.value = JSON.parse(JSON.stringify(v.matchList))
  let all = []
  addAni.value.url = v.url
  addAni.value.group = v.group
  addAni.value.match = JSON.stringify(all)

  if (matchList.value.length === 1 || props.match) {
    dialogVisible.value = false
    emit('add', addAni.value)
    return
  }

  matchList.value.push(all)
  matchDialogVisible.value = true
}


let img = (it) => {
  return `api/file?img=${btoa(it['cover'])}&s=${window.authorization}`;
}

let showTag = () => {
  return window.innerWidth > 900;
}

let open = (url) => {
  window.open(url);
}

defineExpose({show})

let props = defineProps(['match'])
let emit = defineEmits(['add'])

</script>

<style>
.el-card {
  --el-card-padding: 15px;
}
</style>
