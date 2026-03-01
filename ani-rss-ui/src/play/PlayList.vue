<template>
  <play-start ref="playStart"/>
  <el-dialog v-model="dialogVisible" :title="ani.title" center>
    <div v-loading="listLoading" v-if="list.length || listLoading">
      <el-scrollbar style="height: 500px;">
        <div class="grid-container">
          <div v-for="it in list">
            <el-card shadow="never">
              <div class="grid-item">
                <div>
                  <el-tooltip :content="it.title" placement="top">
                    <el-text :line-clamp="2">
                      {{ it.title }}
                    </el-text>
                  </el-tooltip>
                  <br/>
                  <el-text size="small" type="info">
                    {{ it.size }}&nbsp;|&nbsp;{{ it.lastModifyFormat }}
                  </el-text>
                </div>
                <el-button circle
                           icon="VideoPlay"
                           size="large"
                           text
                           type="primary"
                           @click="playStartShow(it)"
                />
              </div>
            </el-card>
          </div>
        </div>
        <div class="bottom-spacer"></div>
      </el-scrollbar>
      <div>
        <p class="total-text">共 {{ list.length }} 项</p>
      </div>
    </div>
    <div class="content" v-else>
      <el-text type="danger">
        未下载集数或 docker 映射存在问题
      </el-text>
    </div>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import PlayStart from "./PlayStart.vue";
import formatTime from "@/js/format-time.js";
import * as http from "@/js/http.js";

const dialogVisible = ref(false)
const listLoading = ref(false)
const list = ref([])

let ani = ref({})
let playStart = ref()

let playStartShow = (it) => {
  playStart.value?.show(JSON.parse(JSON.stringify(it)))
}

const show = (it) => {
  ani.value = it
  listLoading.value = true
  list.value = []
  dialogVisible.value = true
  http.playList(it)
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


<style scoped>
.bottom-spacer {
  height: 5px;
}

.content {
  min-height: 200px;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.grid-container {
  display: grid;
  grid-gap: 5px;
  width: 100%;
  grid-template-columns: repeat(2, 1fr);
  padding: 0 5px;
}

.grid-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.total-text {
  margin: 6px;
  text-align: end;
}
</style>
