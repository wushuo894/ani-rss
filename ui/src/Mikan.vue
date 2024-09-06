<template>
  <el-dialog v-model="dialogVisible" title="Mikan" center>
    <div style="min-height: 300px;">
      <div style="margin: 4px;">
        <div style="display: flex;justify-content: space-between;">
          <el-input v-model:model-value="text" placeholder="请输入搜索标题" @keyup.enter="search"
                    clearable
                    @clear="()=>{
            text = ''
            search()
          }"></el-input>
          <div style="width: 4px;"></div>
          <el-button @click="search" :loading="searchLoading">搜索</el-button>
        </div>
        <div style="max-width: 280px;margin-top: 4px;">
          <el-select v-if="data.seasons.length" v-model:model-value="season" @change="change"
                     :disabled="text.length > 0 || loading">
            <el-option :label="item.year+' '+item.season" :key="item.year+' '+item.season"
                       :value="item.year+' '+item.season" v-for="item in data.seasons">
            </el-option>
          </el-select>
        </div>
      </div>
      <div style="margin: 0 5px;min-height: 200px;" v-loading="loading">
        <el-collapse v-model="activeName" accordion>
          <el-collapse-item v-for="item in data.items" :title="item.label" :name="item.label">
            <div style="margin-left: 15px;">
              <el-collapse @change="collapseChange" accordion>
                <el-collapse-item v-for="it in item.items" :name="it.url">
                  <template #title>
                    <img :src="it['cover']" height="40" width="40">
                    <div style="margin-left: 5px;
                                         max-width: 70%;
                                         overflow: hidden;
                                         white-space: nowrap;
                                         text-overflow: ellipsis;">
                      {{ it.title }}
                    </div>
                  </template>
                  <div style="margin-left: 15px;min-height: 50px;" v-if="selectName === it.url"
                       v-loading="groupLoading">
                    <el-collapse accordion>
                      <el-collapse-item v-for="group in groups[it.url]">
                        <template #title>
                          <div style="width: 100%;display: flex;justify-content: space-between;">
                            <div>
                              {{ group.label }}
                            </div>
                            <div style="display: flex;align-items: center;margin-right: 15px;">
                              <el-button @click.stop="add(group['rss'])">添加</el-button>
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
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import api from "./api.js";
import {ElMessage} from "element-plus";

let groupLoading = ref(false)
let activeName = ref("")
let dialogVisible = ref(false)
let loading = ref(false)
let data = ref({
  'seasons': [],
  'items': []
})

let season = ref('')

let show = () => {
  season.value = ''
  dialogVisible.value = true
  text.value = ''
  data.value = {
    'seasons': [],
    'items': []
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
  return api.post('/api/mikan?text=' + text, body)
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
        groups.value = {}
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
  api.get('/api/mikan/group?url=' + v)
      .then(res => {
        groups.value[v] = res.data
      })
      .finally(() => {
        groupLoading.value = false
      })
}

let add = (v) => {
  emit('add', v)
  dialogVisible.value = false
}

defineExpose({show})

const emit = defineEmits(['add'])

</script>

<style>
.el-card {
  --el-card-padding: 15px;
}
</style>