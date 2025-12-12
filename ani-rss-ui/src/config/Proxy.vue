<template>
  <el-form label-width="auto"
           @submit="(event)=>{
                      event.preventDefault()
                   }">
    <el-form-item label="IP">
      <el-input v-model:model-value="props.config.proxyHost" :disabled="!props.config.proxy"
                placeholder="192.168.0.x"/>
    </el-form-item>
    <el-form-item label="端口">
      <el-input-number v-model:model-value="props.config.proxyPort" :disabled="!props.config.proxy" :min="1"
                       :max="65535"/>
    </el-form-item>
    <el-form-item label="用户名">
      <el-input v-model:model-value="props.config.proxyUsername" :disabled="!props.config.proxy"
                placeholder="可以为空">
        <template #prefix>
          <el-icon class="el-input__icon">
            <User/>
          </el-icon>
        </template>
      </el-input>
    </el-form-item>
    <el-form-item label="密码">
      <el-input v-model:model-value="props.config.proxyPassword" :disabled="!props.config.proxy"
                placeholder="可以为空">
        <template #prefix>
          <el-icon class="el-input__icon">
            <Key/>
          </el-icon>
        </template>
      </el-input>
    </el-form-item>
    <el-form-item label="代理列表">
      <el-input
          class="proxy-list-input"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 3}"
          v-model="props.config.proxyList"
          :disabled="!props.config.proxy"/>
    </el-form-item>
    <el-form-item label="启用">
      <el-switch v-model:model-value="props.config.proxy"/>
    </el-form-item>
    <el-form-item label="ScrapeTest">
      <div class="auto-flex proxy-test-container">
        <div class="proxy-test-controls">
          <el-select v-model:model-value="url" class="proxy-test-select">
            <el-option :value="it" :label="it" :key="it" v-for="it in urls"/>
          </el-select>
          <div class="proxy-test-spacer"></div>
          <el-button bg text :loading="testLoading" @click="test" icon="Odometer">测试</el-button>
        </div>
        <div v-if="status && time">
          status: {{ status }}
          <br>
          time: {{ time }}ms
        </div>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>
import {onMounted, ref} from "vue";
import api from "@/js/api.js";
import {ElMessage} from "element-plus";
import {Key, User} from "@element-plus/icons-vue";

let urls = ref([
  'https://mikanani.me',
  'https://mikanime.tv',
  'https://nyaa.si',
  'https://acg.rip',
  'https://github.com',
  'https://www.google.com',
  'https://bgm.tv',
  'https://www.tmdb.org',
  'https://www.themoviedb.org'
])

let url = ref('')
let status = ref('')
let time = ref('')

onMounted(() => {
  url.value = urls.value[0]
})

let testLoading = ref(false)

let test = () => {
  testLoading.value = true
  status.value = ''
  time.value = ''
  api.post('api/proxy?url=' + btoa(url.value), props.config)
      .then(res => {
        status.value = res.data.status
        time.value = res.data.time
        ElMessage.success(res.message)
      })
      .finally(() => {
        testLoading.value = false
      })
}

let props = defineProps(['config'])

</script>

<style scoped>
.proxy-list-input {
  width: 100%;
}

.proxy-test-container {
  justify-content: space-between;
  width: 100%;
}

.proxy-test-controls {
  display: flex;
}

.proxy-test-select {
  width: 240px;
}

.proxy-test-spacer {
  width: 4px;
}
</style>
