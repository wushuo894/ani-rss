<template>
  <play-start ref="playStart"/>
  <el-dialog v-model="dialogVisible" :title="ani.title" center v-if="dialogVisible">
    <div style="max-height: 500px;min-height: 200px;" v-loading="listLoading">
      <el-scrollbar>
        <div class="i-grid-container">
          <div v-for="it in list">
            <el-card shadow="never">
              <div style="width: 100%;display: flex;justify-content: space-between;align-items: center;">
                <div>
                  {{ it.title }}
                </div>
                <el-button bg text icon="VideoPlay" @click="playStart?.show(ani,it)"/>
              </div>
            </el-card>
            <div style="height: 4px;"/>
          </div>
        </div>
      </el-scrollbar>
    </div>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import api from "../api.js";
import PlayStart from "./PlayStart.vue";

const dialogVisible = ref(false)
const listLoading = ref(false)
const list = ref([])

let ani = ref({})
let playStart = ref()

const show = (it) => {
  console.log(it);
  ani.value = it
  listLoading.value = true
  list.value = []
  dialogVisible.value = true
  api.post('/api/playlist', it)
      .then(res => {
        list.value = res.data
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