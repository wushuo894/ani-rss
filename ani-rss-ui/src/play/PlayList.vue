<template>
  <play-start ref="playStart"/>
  <el-dialog v-model="dialogVisible" :title="ani.title" center>
    <div v-loading="listLoading" class="content">
      <div v-if="list.length">
        <el-scrollbar>
          <div class="grid-container"
               style="max-height: 500px;">
            <div v-for="it in list">
              <el-card shadow="never">
                <div class="grid-item">
                  <div>
                    {{ it.title }}
                    <br/>
                    <el-text size="small" type="info">
                      {{ it.lastModifyFormat }}
                    </el-text>
                  </div>
                  <el-button circle
                             icon="VideoPlay"
                             size="large"
                             text
                             type="primary"
                             @click="playStart?.show(ani,it)"
                  />
                </div>
              </el-card>
            </div>
          </div>
        </el-scrollbar>
        <div>
          <p class="total-text">共 {{ list.length }} 项</p>
        </div>
      </div>
      <el-text type="danger" v-else>
        未下载集数或 docker 映射存在问题
      </el-text>
    </div>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import api from "@/js/api.js";
import PlayStart from "./PlayStart.vue";
import formatTime from "@/js/format-time.js";

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


<style scoped>
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
