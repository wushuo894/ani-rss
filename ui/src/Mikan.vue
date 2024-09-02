<template>
  <el-dialog v-model="dialogVisible" title="Mikan" center>
    <div style="min-height: 300px;" v-loading="loading">
      <el-select v-if="data.seasons.length" v-model:model-value="season" @change="change">
        <el-option :label="item.year+' '+item.season" :key="item.year+' '+item.season"
                   :value="item.year+' '+item.season" v-for="item in data.seasons">
        </el-option>
      </el-select>
      <div style="margin: 0 5px">
        <el-collapse v-model="activeName" accordion>
          <el-collapse-item v-for="item in data.items" :title="item.label" :name="item.label">
            <div style="margin-left: 15px;">
              <el-collapse @change="collapseChange" accordion>
                <el-collapse-item v-for="it in item.items" :name="it.url">
                  <template #title>
                    <img :src="it['cover']" height="40" width="40">
                    <div style="margin-left: 5px">
                      {{ it.title }}
                    </div>
                  </template>
                  <div style="margin-left: 15px;min-height: 50px;" v-if="selectName === it.url" v-loading="groupLoading">
                    <el-collapse accordion>
                      <el-collapse-item v-for="group in groups">
                        <template #title>
                          <div style="width: 100%;display: flex;justify-content: space-between;">
                            <span>
                              {{ group.label }}
                            </span>
                            <div style="display: flex;align-items: center;margin-right: 15px;">
                              <el-button @click.stop="add(group['rss'])">添加</el-button>
                            </div>
                          </div>
                        </template>
                        <el-card shadow="never" v-for="ti in group.items">
                          <div>
                            <div>
                              {{ ti.name }}
                            </div>
                            <div style="width: 100%;display: flex;justify-content: end;">
                              {{ ti['sizeStr'] }}
                              {{ ti['dateStr'] }}
                            </div>
                          </div>
                        </el-card>
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
  dialogVisible.value = true
  list({})
}

let list = (body) => {
  loading.value = true
  api.post('/api/mikan', body)
      .then(res => {
        let seasons = res.data.seasons
        let items = res.data.items
        if (seasons.length) {
          data.value.seasons = seasons
        }
        data.value.items = items
        loading.value = false
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
}

let change = (v) => {
  let body = data.value.seasons.filter(item => (item['year'] + ' ' + item['season']) === v)
  if (body.length) {
    list(body[0])
  }
}

let selectName = ref('')
let groups = ref([])

let collapseChange = (v) => {
  if (!v) {
    return
  }
  groupLoading.value = true
  groups.value = []
  selectName.value = v
  api.get('/api/mikan/group?url=' + v)
      .then(res => {
        groups.value = res.data
        groupLoading.value = false
      })
}

let add = (v) => {
  emit('add',v)
  dialogVisible.value = false
}

defineExpose({show})

const emit = defineEmits(['add'])

</script>