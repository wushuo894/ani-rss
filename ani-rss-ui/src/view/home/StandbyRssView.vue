<template>
  <AniBTView ref="aniBTRef" @callback="mikanCallback"/>
  <MikanView ref="mikanRef" @callback="mikanCallback"/>
  <AnimeGardenView ref="animeGardenRef" @callback="mikanCallback"/>
  <div class="standby-rss-view">
    <el-alert v-if="!config.standbyRss" :closable="false"
              show-icon
              class="standby-alert" type="warning">
      <template #title>
        当前备用RSS功能并未开启, 可前往 <strong>设置-基本设置-RSS设置-备用RSS</strong> 启用
      </template>
    </el-alert>
    <div class="flex standby-toolbar">
      <el-button text bg icon="Plus" @click="plus" type="primary"/>
      <el-button
          @click="mikanRef?.show(props.ani)"
          text bg>
        <template #icon>
          <img src="@/icon/icon-Mikan.png" alt="mikan" class="icon"/>
        </template>
      </el-button>
      <el-button
          @click="aniBTShow"
          text bg>
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
    <div>
      <el-table :data="standbyRss" height="400px" size="small">
        <el-table-column fixed label="字幕组" min-width="100px">
          <template #default="it">
            <div v-if="editIndex !== it.$index">
              {{ standbyRss[it.$index].label }}
            </div>
            <div v-else>
              <el-input v-model:model-value="standbyRss[it.$index].label" placeholder="未知字幕组"/>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="RSS" min-width="400px">
          <template #default="it">
            <div v-if="editIndex !== it.$index">
              <el-text line-clamp="1" size="small" truncated>
                {{ standbyRss[it.$index].url }}
              </el-text>
            </div>
            <div v-else>
              <el-input v-model:model-value="standbyRss[it.$index].url" placeholder="https://xxx.xxx" type="textarea"
                        size="small"
                        autosize/>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="偏移" width="150px">
          <template #default="it">
            <div v-if="editIndex !== it.$index">
              {{ standbyRss[it.$index].offset }}
            </div>
            <el-input-number v-else v-model:model-value="standbyRss[it.$index].offset" size="small"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="it">
            <div class="flex">
              <el-button bg text icon="Edit" @click="editIndex = it.$index" v-if="editIndex !== it.$index"/>
              <el-button bg text icon="Check" @click="normalize" type="primary" v-else/>
              <el-button bg text @click="del(it.$index)" icon="Delete" type="danger"/>
              <el-button :disabled="it.$index < 1" bg icon="ArrowUpBold" text type="primary"
                         @click="move(it.$index,-1)"/>
              <el-button :disabled="it.$index >= standbyRss.length-1" bg icon="ArrowDownBold" text type="primary"
                         @click="move(it.$index,1)"/>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from "vue";
import MikanView from "./MikanView.vue";
import AniBTView from "@/view/home/AniBTView.vue";
import AnimeGardenView from "@/view/home/AnimeGardenView.vue";
import * as http from "@/js/http.js";

const props = defineProps(['ani'])
const editIndex = ref(-1)
const aniBTRef = ref()
const mikanRef = ref()
const animeGardenRef = ref()
const config = ref({
  standbyRss: true
})

const standbyRss = computed({
  get: () => Array.isArray(props.ani.standbyRssList) ? props.ani.standbyRssList : [],
  set: value => {
    props.ani.standbyRssList = value
  }
})

onMounted(() => {
  http.config()
      .then(res => {
        config.value = res.data;
      })
})

let plus = () => {
  if (!Array.isArray(props.ani.standbyRssList)) {
    props.ani.standbyRssList = []
  }
  let object = {
    label: '未知字幕组',
    url: '',
    offset: props.ani.offset
  }
  standbyRss.value.push(object)
  editIndex.value = standbyRss.value.length - 1
  return object
}

let del = (index) => {
  editIndex.value = -1
  standbyRss.value = standbyRss.value.filter((s, i) => i !== index)
}

const normalize = () => {
  editIndex.value = -1
  standbyRss.value = standbyRss.value
      .map(it => {
        it.url = it.url.trim()
        return it;
      })
      .filter(it => it.url !== '')
}

let move = (index, offset) => {
  let v = standbyRss.value[index]
  standbyRss.value[index] = standbyRss.value[index + offset]
  standbyRss.value[index + offset] = v
}

let mikanCallback = v => {
  let {subgroup, match, url} = v

  let later = plus()
  later.url = url
  later.label = subgroup

  let newMatch = JSON.parse(match).map(s => `{{${subgroup}}}:${s}`)

  // 剔除旧的同字幕组规则
  props.ani.match = props.ani.match.filter(it => it.indexOf(`{{${subgroup}}}:`) !== 0)

  props.ani.match.push(...newMatch)

  editIndex.value = -1
}

let animeGardenShow = () => {
  let bgmUrl = props.ani.bgmUrl;
  animeGardenRef.value?.show(bgmUrl)
}

let aniBTShow = () => {
  let bgmUrl = props.ani.bgmUrl;
  aniBTRef.value?.show(bgmUrl)
}

defineExpose({normalize})

</script>

<style scoped>
.standby-rss-view {
  height: 500px;
  min-width: 0;
  overflow: hidden;
}

.standby-alert {
  margin-bottom: 8px;
}

.standby-toolbar {
  width: 100%;
  margin-bottom: 8px;
}

.standby-spacer {
  margin: 3px;
}

.standby-action-spacer {
  margin-left: 4px;
}

.icon {
  width: 24px;
  height: 24px;
  border-radius: 8px;
}
</style>
