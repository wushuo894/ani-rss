<template>
  <Mikan ref="mikan" @add="args => {
    plus()
    backRss[editIndex].url = args.url
    backRss[editIndex].label = args.group
  }"/>
  <el-dialog v-model="dialogVisible" title="备用订阅" center v-if="dialogVisible">
    <div style="display: flex;width: 100%;">
      <div>
        <el-button text bg icon="Plus" @click="plus" type="primary"/>
      </div>
      <div style="margin: 3px;"></div>
      <div>
        <el-button @click="mikan?.show" text bg icon="VideoCamera"/>
      </div>
    </div>
    <el-scrollbar>
      <el-table v-model:data="backRss" height="400px">
        <el-table-column label="字幕组" min-width="100px">
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
              {{ backRss[it.$index].url }}
            </div>
            <div v-else>
              <el-input v-model:model-value="backRss[it.$index].url" placeholder="https://xxx.xxx" type="textarea"/>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="115">
          <template #default="it">
            <div style="display: flex;">
              <div>
                <el-button bg text icon="Edit" @click="editIndex = it.$index" v-if="editIndex !== it.$index"/>
                <el-button bg text icon="Check" @click="check" type="primary" v-else/>
              </div>
              <div style="margin: 3px;"></div>
              <div>
                <el-button bg text @click="del(it.$index)" icon="Delete" type="danger"/>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-scrollbar>
    <div style="display: flex;width: 100%;justify-content: end;margin-top: 10px">
      <el-button icon="Check" bg text @click="ok" :disabled="editIndex > -1">确定</el-button>
    </div>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import Mikan from "./Mikan.vue";

const editIndex = ref(-1)

const dialogVisible = ref(false)
const backRss = ref()
const mikan = ref()

let show = () => {
  editIndex.value = -1
  dialogVisible.value = true
  backRss.value = JSON.parse(JSON.stringify(props.ani.backRssList))
}

let plus = () => {
  if (!backRss.value.length) {
    backRss.value.push({
      label: '未知字幕组',
      url: ''
    })
    editIndex.value = backRss.value.length - 1
    return
  }
  if (backRss.value[backRss.value.length - 1].url.trim()) {
    backRss.value.push({
      label: '备用RSS',
      url: ''
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

defineExpose({show})
let props = defineProps(['ani'])

</script>
