<template>
  <el-dialog
      v-model="dialogVisible"
      align-center
      center
      title="更换封面"
      width="min(680px, calc(100vw - 24px))">
    <div class="cover-layout">
      <div class="cover-preview-column">
        <div class="cover-preview">
          <el-image
              v-if="previewUrl"
              :alt="ani.title"
              :preview-src-list="[previewUrl]"
              :src="previewUrl"
              fit="cover"
              hide-on-click-modal>
            <template #error>
              <div class="cover-placeholder">
                <el-icon>
                  <Picture/>
                </el-icon>
              </div>
            </template>
          </el-image>
          <div v-else class="cover-placeholder">
            <el-icon>
              <Picture/>
            </el-icon>
          </div>
        </div>
        <el-tooltip :content="ani.title" placement="bottom">
          <el-text class="cover-anime-title" truncated>{{ ani.title }}</el-text>
        </el-tooltip>
      </div>

      <div class="cover-controls">
        <section class="cover-source-section">
          <div class="cover-source-title">
            <el-icon>
              <Link/>
            </el-icon>
            <span>图片链接</span>
          </div>
          <div class="cover-url-row">
            <el-input
                v-model="ani.image"
                :prefix-icon="Link"
                clearable
                placeholder="https://lain.bgm.tv/pic/cover/1234.jpg"
                @keyup.enter="refreshCover"/>
            <el-tooltip content="获取封面" placement="top">
              <el-button
                  :disabled="!ani.image"
                  :loading="refreshLoading"
                  bg
                  @click="refreshCover">
                <el-icon>
                  <Refresh/>
                </el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </section>

        <section class="cover-source-section">
          <div class="cover-source-title">
            <el-icon>
              <UploadFilled/>
            </el-icon>
            <span>本地文件</span>
          </div>
          <UploadView
              url="api/upload"
              :extensions="['jpg', 'jpeg', 'png']"
              :callback="uploadCallback">
            <el-button bg>
              <el-icon>
                <UploadFilled/>
              </el-icon>
              选择图片
            </el-button>
          </UploadView>
        </section>
      </div>
    </div>

    <template #footer>
      <div class="cover-footer">
        <el-button @click="dialogVisible = false" bg text>取消</el-button>
        <el-button :loading="saveLoading" type="primary" bg text @click="save">
          <el-icon>
            <Check/>
          </el-icon>
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import {computed, ref} from "vue";
import {ElMessage} from "element-plus";
import {Check, Link, Picture, Refresh, UploadFilled} from "@element-plus/icons-vue";
import {toApiFile} from "@/js/global.js";
import * as http from "@/js/http.js";
import UploadView from "@/view/custom/UploadView.vue";

const dialogVisible = ref(false)
const ani = ref({})
const time = ref()
const refreshLoading = ref(false)
const saveLoading = ref(false)
let sourceAni

const previewUrl = computed(() => ani.value.cover
    ? `${toApiFile(ani.value.cover)}&t=${time.value}`
    : '')

const refreshCover = () => {
  refreshLoading.value = true
  http.refreshCover(ani.value)
      .then(res => {
        time.value = new Date().getTime()
        ani.value.cover = res.data
      })
      .finally(() => {
        refreshLoading.value = false
      })
}

const uploadCallback = res => {
  const {code, message, data} = res
  if (code === 200) {
    ani.value.cover = data
    time.value = new Date().getTime()
    return
  }
  ElMessage.error(message)
}

const show = newAni => {
  sourceAni = newAni
  time.value = new Date().getTime()
  ani.value = JSON.parse(JSON.stringify(newAni))
  dialogVisible.value = true
}

const save = () => {
  saveLoading.value = true
  http.setAni(false, ani.value)
      .then(res => {
        ElMessage.success(res.message)
        if (sourceAni) {
          Object.assign(sourceAni, ani.value)
        }
        window.$reLoadList?.()
        dialogVisible.value = false
      })
      .finally(() => {
        saveLoading.value = false
      })
}

defineExpose({show})
</script>

<style scoped>
.cover-layout {
  min-width: 0;
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.cover-preview-column {
  min-width: 0;
  display: grid;
  justify-items: center;
  gap: 8px;
}

.cover-preview {
  width: 190px;
  aspect-ratio: 2 / 3;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background-color: var(--el-fill-color-light);
}

.cover-preview :deep(.el-image) {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  background-color: var(--el-fill-color-light);
  font-size: 34px;
}

.cover-anime-title {
  display: block;
  max-width: 190px;
  font-weight: 600;
  text-align: center;
}

.cover-controls {
  min-width: 0;
  display: grid;
}

.cover-source-section {
  min-width: 0;
  padding: 4px 0 18px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.cover-source-section + .cover-source-section {
  padding-top: 18px;
  border-bottom: none;
}

.cover-source-title {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.cover-url-row {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  gap: 8px;
}

.cover-url-row .el-button {
  width: 34px;
  height: 32px;
  padding: 0;
}

.cover-footer {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 600px) {
  .cover-layout {
    grid-template-columns: minmax(0, 1fr);
    gap: 18px;
  }

  .cover-preview {
    width: min(170px, 52vw);
  }

  .cover-anime-title {
    max-width: min(240px, 80vw);
  }
}
</style>
