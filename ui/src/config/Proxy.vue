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
    <el-form-item label="启用">
      <el-switch v-model:model-value="props.config.proxy"/>
    </el-form-item>
    <el-form-item label="Test">
      <div style="justify-content: space-between;width: 100%;" class="auto">
        <div style="display: flex;">
          <el-select v-model:model-value="url" style="width: 240px;">
            <el-option :value="it" :label="it" :key="it" v-for="it in urls"/>
          </el-select>
          <div style="width: 4px;"></div>
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
import api from "../js/api.js";
import {ElMessage} from "element-plus";
import {Key, User} from "@element-plus/icons-vue";

let urls = ref([
  'https://mikanani.me',
  'https://mikanime.tv',
  'https://nyaa.si',
  'https://github.com',
  'https://www.google.com',
  'https://bgm.tv'
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

<style>
@media (min-width: 1000px) {
  .auto {
    display: flex;
  }
}
</style>
