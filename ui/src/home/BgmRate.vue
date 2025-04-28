<template>
  <el-dialog v-model="dialogVisible" align-center center style="max-width: 300px;" title="评分">
    <div v-loading="loading">
      <div class="flex" style="justify-content: center;width: 100%;">
        <el-rate v-model="ani.score" :max="10" :min="0"/>
      </div>
      <div class="flex" style="width: 100%;justify-content: space-between;margin-top: 14px;">
        <el-button :icon="Ban" bg text @click="clearRate">清空评分</el-button>
        <el-button :icon="Save" bg text @click="rate(ani)">保存评分</el-button>
      </div>
    </div>
  </el-dialog>
</template>


<script setup>

import {ref} from "vue";
import api from "../api.js";
import {Ban, Save} from "@vicons/fa";
import {ElMessage} from "element-plus";

let dialogVisible = ref(false)

let ani = ref({
  score: 0
})

let loading = ref(false);

let show = (v) => {
  ani.value = JSON.parse(JSON.stringify(v))
  ani.value.score = 0

  let tmpAni = JSON.parse(JSON.stringify(ani.value))
  tmpAni.score = null

  rate(tmpAni)

  dialogVisible.value = true
}

let clearRate = () => {
  ani.value.score = 0
  rate(ani.value)
}

let rate = (v) => {
  loading.value = true
  api.post('api/bgm?type=rate', v)
      .then(res => {
        ani.value.score = res.data

        let message = res.message
        if (message) {
          ElMessage.success(message)
        }
      })
      .finally(() => {
        loading.value = false
      })
}

defineExpose({show})
</script>