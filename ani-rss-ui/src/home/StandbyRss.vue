<template>
  <Mikan ref="mikanRef" @callback="mikanCallback"/>
  <el-dialog v-model="dialogVisible" center title="备用订阅">
    <el-alert v-if="!config.standbyRss" :closable="false"
              show-icon
              class="standby-alert" type="warning">
      <template #title>
        当前备用RSS功能并未开启, 可前往 <strong>设置-基本设置-RSS设置-备用RSS</strong> 启用
      </template>
    </el-alert>
    <div class="flex standby-toolbar">
      <div>
        <el-button text bg icon="Plus" @click="plus" type="primary"/>
      </div>
      <div class="standby-spacer"></div>
      <div>
        <el-button
            @click="mikanShow"
            text bg
            icon="VideoCamera"/>
      </div>
    </div>
    <div>
      <el-table v-model:data="standbyRss" height="400px" size="small">
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
              <div>
                <el-button bg text icon="Edit" @click="editIndex = it.$index" v-if="editIndex !== it.$index"/>
                <el-button bg text icon="Check" @click="check" type="primary" v-else/>
              </div>
              <div class="standby-action-spacer">
                <el-button bg text @click="del(it.$index)" icon="Delete" type="danger"/>
              </div>
              <div class="standby-action-spacer">
                <el-button :disabled="it.$index < 1" bg icon="ArrowUpBold" text type="primary"
                           @click="move(it.$index,-1)"/>
              </div>
              <div class="standby-action-spacer">
                <el-button :disabled="it.$index >= standbyRss.length-1" bg icon="ArrowDownBold" text type="primary"
                           @click="move(it.$index,1)"/>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="flex standby-footer">
      <el-button icon="Check" bg text @click="ok" :disabled="editIndex > -1">确定</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import Mikan from "./Mikan.vue";
import api from "@/js/api.js";

const editIndex = ref(-1)

const dialogVisible = ref(false)
const standbyRss = ref()
const mikanRef = ref()
const config = ref({
  standbyRss: true
})

let show = () => {
  editIndex.value = -1
  dialogVisible.value = true
  standbyRss.value = JSON.parse(JSON.stringify(props.ani.standbyRssList))

  config
      .then(res => {
        config.value = res.data;
      })
}

let plus = () => {
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

let check = () => {
  editIndex.value = -1
  standbyRss.value = standbyRss.value
      .map(it => {
        it.url = it.url.trim()
        return it;
      })
      .filter(it => it.url !== '')
}

let ok = () => {
  check()
  props.ani.standbyRssList = standbyRss.value
  dialogVisible.value = false
}

let move = (index, offset) => {
  let v = standbyRss.value[index]
  standbyRss.value[index] = standbyRss.value[index + offset]
  standbyRss.value[index + offset] = v
}

let mikanCallback = v => {
  let {group, match, url} = v

  let later = plus()
  later.url = url
  later.label = group

  let newMatch = JSON.parse(match).map(s => `{{${group}}}:${s}`)

  // 剔除旧的同字幕组规则
  props.ani.match = props.ani.match.filter(it => it.indexOf(`{{${group}}}:`) !== 0)

  props.ani.match.push(...newMatch)

  editIndex.value = -1
}

let mikanShow = () => {
  let query = props.ani.mikanTitle ? props.ani.mikanTitle : props.ani.title;

  if (props.ani.url) {
    let url = new URL(props.ani.url);
    let searchParams = url.searchParams;
    let bangumiId = searchParams.get("bangumiId");
    if (bangumiId) {
      query = `bangumiId: ${bangumiId}`
    }
  }

  mikanRef.value?.show(query)
}

defineExpose({show})
let props = defineProps(['ani'])

</script>

<style scoped>
.standby-alert {
  margin-bottom: 8px;
}

.standby-toolbar {
  width: 100%;
}

.standby-spacer {
  margin: 3px;
}

.standby-action-spacer {
  margin-left: 4px;
}

.standby-footer {
  width: 100%;
  justify-content: end;
  margin-top: 10px;
}
</style>
