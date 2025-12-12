<template>
  <el-dialog v-model="dialogVisible" align-center center width="300" title="评分">
    <div v-loading="loading">
      <div class="flex bgm-rate-rate-container">
        <el-rate v-model="ani.score"
                 :max="10"
                 :min="0"/>
      </div>
      <div class="bgm-rate-text-container">
        <p v-if="ani.score > 0" class="bgm-rate-text">{{ texts[ani.score - 1] }}</p>
      </div>
      <div class="flex bgm-rate-button-container">
        <el-button :icon="Ban" bg text @click="clearRate">清空评分</el-button>
        <el-button :icon="Save" bg text @click="rate(ani)">保存评分</el-button>
      </div>
    </div>
  </el-dialog>
</template>


<script setup>

import {ref} from "vue";
import api from "@/js/api.js";
import {Ban, Save} from "@vicons/fa";
import {ElMessage} from "element-plus";

let texts = ref([
  '不忍直视 1 (请谨慎评价)', '很差 2', '差 3', '较差 4', '不过不失 5',
  '还行 6', '推荐 7', '力荐 8', '神作 9', '超神作 10 (请谨慎评价)'
])

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

<style scoped>
.bgm-rate-rate-container {
  justify-content: center;
  width: 100%;
}

.bgm-rate-text-container {
  height: 12px;
}

.bgm-rate-text {
  text-align: center;
}

.bgm-rate-button-container {
  width: 100%;
  justify-content: space-between;
  margin-top: 14px;
}
</style>
