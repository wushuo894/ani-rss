<template>
  <el-form label-width="auto"
           style="width: 100%"
           @submit="(event)=>{
                    event.preventDefault()
                   }">
    <el-form-item label="更新地址">
      <div style="width: 100%">
        <div>
          <el-input v-model:model-value="props.config.trackersUpdateUrls" :autosize="{ minRows: 2}"
                    placeholder="换行输入多个"
                    style="width: 100%" type="textarea"/>
        </div>
        <div style="height: 12px;"/>
        <div class="flex" style="justify-content: space-between;">
          <el-checkbox v-model:model-value="props.config.autoTrackersUpdate" label="每天1:00自动更新"/>
          <el-button :loading="trackersUpdateLoading" bg icon="Refresh" text @click="trackersUpdate">更新
          </el-button>
        </div>
        <div>
          <el-text class="mx-1" size="small">
            该功能暂不支持 Transmission
          </el-text>
        </div>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>
import {ElMessage, ElText} from "element-plus";
import api from "@/js/api.js";
import {ref} from "vue";

let trackersUpdateLoading = ref(false)


let trackersUpdate = () => {
  trackersUpdateLoading.value = true
  api.post('api/trackersUpdate', props.config)
      .then(res => {
        ElMessage.success(res.message);
      })
      .finally(() => {
        trackersUpdateLoading.value = false
      })
}

let props = defineProps(['config'])
</script>