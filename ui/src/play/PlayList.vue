<template>
  <play-start ref="playStart"/>
  <el-dialog v-model="dialogVisible" :title="ani.title" center>
    <div style="max-height: 500px;min-height: 200px;" v-loading="listLoading">
      <el-scrollbar>
        <div v-if="list.length" class="i-grid-container" style="max-height: 500px;">
          <div v-for="it in list">
            <el-card shadow="never">
              <div style="width: 100%;display: flex;justify-content: space-between;align-items: center;">
                <div>
                  {{ it.title }}
                  <br/>
                  <el-text size="small">
                    {{ it.lastModifyFormat }}
                  </el-text>
                </div>
                <el-button bg text icon="VideoPlay" @click="playStart?.show(ani,it)"/>
              </div>
            </el-card>
            <div style="height: 4px;"/>
          </div>
        </div>
        <div v-else>
          <el-alert :closable="false" center show-icon title="未下载集数或docker映射存在问题" type="error"/>
        </div>
      </el-scrollbar>
    </div>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import api from "../api.js";
import PlayStart from "./PlayStart.vue";
import formatTime from "../date-format.js";

const dialogVisible = ref(false)
const listLoading = ref(false)
const list = ref([])

let ani = ref({})
let playStart = ref()

const show = (it) => {
  ani.value = it
  listLoading.value = true
  list.value = []
  dialogVisible.value = true
  api.post('api/playlist', it)
      .then(res => {
        list.value = res.data.map(it => {
          return {...it, lastModifyFormat: formatTime(it['lastModify'])}
        })
      })
      .finally(() => {
        listLoading.value = false
      })
}

defineExpose({
  show
})
</script>


<style>
.i-grid-container {
  display: grid;
  grid-gap: 5px;
  width: 100%;
  grid-template-columns: repeat(2, 1fr);
}
</style>