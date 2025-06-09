<template>
  <Mikan ref="mikan" @add="args => {
    plus()
    backRss[editIndex].url = args.url
    backRss[editIndex].label = args.group
    editIndex = -1
  }" match="false"/>
  <el-dialog v-model="dialogVisible" center title="备用订阅">
    <el-alert v-if="!config.backRss" :closable="false"
              show-icon
              style="margin-bottom: 8px;" type="warning">
      <template #title>
        当前备用RSS功能并未开启, 可前往 <strong>设置-基本设置-RSS设置-备用RSS</strong> 启用
      </template>
    </el-alert>
    <div class="flex" style="width: 100%;">
      <div>
        <el-button text bg icon="Plus" @click="plus" type="primary"/>
      </div>
      <div style="margin: 3px;"></div>
      <div>
        <el-button @click="mikan?.show(ani.title)" text bg icon="VideoCamera"/>
      </div>
    </div>
    <div>
      <el-table v-model:data="backRss" height="400px">
        <el-table-column fixed label="字幕组" min-width="100px">
          <template #default="it">
            <div v-if="editIndex !== it.$index">
              {{ backRss[it.$index].label }}
            </div>
            <div v-else>
              <el-input v-model:model-value="backRss[it.$index].label" placeholder="未知字幕组"/>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="RSS" min-width="400px">
          <template #default="it">
            <div v-if="editIndex !== it.$index">
              <el-text line-clamp="1" size="small" truncated>
                {{ backRss[it.$index].url }}
              </el-text>
            </div>
            <div v-else>
              <el-input v-model:model-value="backRss[it.$index].url" placeholder="https://xxx.xxx" type="textarea"
                        size="small"
                        autosize/>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="偏移" width="150px">
          <template #default="it">
            <div v-if="editIndex !== it.$index">
              {{ backRss[it.$index].offset }}
            </div>
            <el-input-number v-else v-model:model-value="backRss[it.$index].offset" size="small"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="it">
            <div class="flex">
              <div>
                <el-button bg text icon="Edit" @click="editIndex = it.$index" v-if="editIndex !== it.$index"/>
                <el-button bg text icon="Check" @click="check" type="primary" v-else/>
              </div>
              <div style="margin-left: 4px;">
                <el-button bg text @click="del(it.$index)" icon="Delete" type="danger"/>
              </div>
              <div style="margin-left: 4px;">
                <el-button :disabled="it.$index < 1" bg icon="ArrowUpBold" text type="primary"
                           @click="move(it.$index,-1)"/>
              </div>
              <div style="margin-left: 4px;">
                <el-button :disabled="it.$index >= backRss.length-1" bg icon="ArrowDownBold" text type="primary"
                           @click="move(it.$index,1)"/>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="flex" style="width: 100%;justify-content: end;margin-top: 10px">
      <el-button icon="Check" bg text @click="ok" :disabled="editIndex > -1">确定</el-button>
    </div>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import Mikan from "./Mikan.vue";
import api from "../js/api.js";

const editIndex = ref(-1)

const dialogVisible = ref(false)
const backRss = ref()
const mikan = ref()
const config = ref({
  backRss: true
})

let show = () => {
  editIndex.value = -1
  dialogVisible.value = true
  backRss.value = JSON.parse(JSON.stringify(props.ani.backRssList))

  api.get('api/config')
      .then(res => {
        config.value = res.data;
      })
}

let plus = () => {
  if (!backRss.value.length) {
    backRss.value.push({
      label: '未知字幕组',
      url: '',
      offset: props.ani.offset
    })
    editIndex.value = backRss.value.length - 1
    return
  }
  if (backRss.value[backRss.value.length - 1].url.trim()) {
    backRss.value.push({
      label: '备用RSS',
      url: '',
      offset: props.ani.offset
    })
    editIndex.value = backRss.value.length - 1
  }
}

let del = (index) => {
  editIndex.value = -1
  backRss.value = backRss.value.filter((s, i) => i !== index)
}

let check = () => {
  editIndex.value = -1
  backRss.value = backRss.value
      .map(it => {
        it.url = it.url.trim()
        return it;
      })
      .filter(it => it.url !== '')
}

let ok = () => {
  check()
  props.ani.backRssList = backRss.value
  dialogVisible.value = false
}

let move = (index, offset) => {
  let v = backRss.value[index]
  backRss.value[index] = backRss.value[index + offset]
  backRss.value[index + offset] = v
}

defineExpose({show})
let props = defineProps(['ani'])

</script>
