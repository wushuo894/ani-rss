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
                  {{ it.lastModifyFormat }}
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

const dialogVisible = ref(false)
const listLoading = ref(false)
const list = ref([])

let ani = ref({})
let playStart = ref()
function formatDownloadTime(timestamp) {
  console.log('form',timestamp)
  function zeroize(num) {
    return (String(num).length == 1 ? '0' : '') + num;
  }
  timestamp = timestamp / 1000;
  var curTimestamp = parseInt(new Date().getTime() / 1000); //当前时间戳
  var timestampDiff = curTimestamp - timestamp; // 参数时间戳与当前时间戳相差秒数

  var curDate = new Date(curTimestamp * 1000); // 当前时间日期对象
  var tmDate = new Date(timestamp * 1000);  // 参数时间戳转换成的日期对象

  var Y = tmDate.getFullYear(), m = tmDate.getMonth() + 1, d = tmDate.getDate();
  var H = tmDate.getHours(), i = tmDate.getMinutes(), s = tmDate.getSeconds();

  if (timestampDiff < 60) { // 一分钟以内
    return "刚刚";
  } else if (timestampDiff < 3600) { // 一小时前之内
    return Math.floor(timestampDiff / 60) + "分钟前";
  } else if (curDate.getFullYear() == Y && curDate.getMonth() + 1 == m && curDate.getDate() == d) {
    return '今天' + zeroize(H) + ':' + zeroize(i);
  } else {
    var newDate = new Date((curTimestamp - 86400) * 1000); // 参数中的时间戳加一天转换成的日期对象
    if (newDate.getFullYear() == Y && newDate.getMonth() + 1 == m && newDate.getDate() == d) {
      return '昨天' + zeroize(H) + ':' + zeroize(i);
    } else if (curDate.getFullYear() == Y) {
      return zeroize(m) + '月' + zeroize(d) + '日 ' + zeroize(H) + ':' + zeroize(i);
    } else {
      return Y + '年' + zeroize(m) + '月' + zeroize(d) + '日 ' + zeroize(H) + ':' + zeroize(i);
    }
  }
}
const show = (it) => {
  ani.value = it
  listLoading.value = true
  list.value = []
  dialogVisible.value = true
  api.post('api/playlist', it)
      .then(res => {
        list.value = res.data.map(v=>{
          return {...v,lastModifyFormat:formatDownloadTime(v.lastModify)}
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