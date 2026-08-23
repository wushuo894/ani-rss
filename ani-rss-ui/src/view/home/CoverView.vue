<template>
  <el-dialog v-model="dialogVisible" center title="封面">
    <div class="content">
      <div>
        <img
            :src="`${toApiFile(ani['cover'])}&t=${time}`"
            :alt="ani.title"
            class="cover"
        />
      </div>
      <div style="width: 12px;">
      </div>
      <div style="flex: 1">
        <el-form @submit.prevent label-width="auto">
          <el-form-item label="URL">
            <div class="full-width">
              <div class="flex full-width">
                <el-input v-model:model-value="ani.image" placeholder="https://lain.bgm.tv/pic/cover/1234.jpg"/>
                <div class="spacer"/>
                <el-button :disabled="!ani.image" :loading="reLoadIng" bg icon="Refresh" text @click="reLoad"/>
              </div>
              <div style="margin-top: 8px;">
                <UploadView url="api/upload"
                            :types="['image/jpeg', 'image/png']"
                            :callback="callback">
                  <el-button>选择并上传文件</el-button>
                </UploadView>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
    <div class="flex" style="justify-content: end;">
      <el-button :loading="okLoading" bg icon="Check" text @click="ok">确定</el-button>
    </div>
  </el-dialog>
</template>
<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import {toApiFile} from "@/js/global.js";
import * as http from "@/js/http.js";
import UploadView from "@/view/custom/UploadView.vue";

let reLoadIng = ref(false)
let reLoad = () => {
  reLoadIng.value = true
  http.refreshCover(ani.value)
      .then(res => {
        time.value = new Date().getTime()
        ani.value.cover = res.data
      })
      .finally(() => {
        reLoadIng.value = false
      })
}

let callback = res => {
  let {code, message, data} = res
  if (code === 200) {
    ani.value.cover = data
    time.value = new Date().getTime()
    return
  }
  ElMessage.error(message)
}

let dialogVisible = ref(false)

let ani = ref({})
let time = ref()

let show = (newAni) => {
  time.value = new Date().getTime()
  ani.value = JSON.parse(JSON.stringify(newAni))
  dialogVisible.value = true;
}

let okLoading = ref(false)
let ok = () => {
  okLoading.value = true
  http.setAni(false, ani.value)
      .then(res => {
        ElMessage.success(res.message)
        window.$reLoadList()
        dialogVisible.value = false
      })
      .finally(() => {
        okLoading.value = false
      })
}

defineExpose({show})
</script>

<style scoped>
.content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
}

.cover {
  border: 1px solid var(--el-border-color-light);
  border-radius: var(--el-border-radius-base);
  cursor: pointer;
  height: 260px;
  width: 180px;
}
</style>
