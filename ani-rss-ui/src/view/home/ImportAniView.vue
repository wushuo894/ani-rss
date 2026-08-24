<template>
  <el-dialog
      v-model="dialogVisible"
      title="导入数据"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
  >
    <div class="import-dialog">
      <!-- 文件上传区域 -->
      <div class="upload-section">
        <div class="section-title">
          <el-icon>
            <document/>
          </el-icon>
          <span>选择文件</span>
        </div>
        <div v-if="data.filename" class="file-selected">
          <el-tag
              closable
              @close="data.filename = ''"
              type="success"
              size="large"
              class="file-tag"
          >
            <el-icon>
              <document/>
            </el-icon>
            {{ data.filename }}
          </el-tag>
          <div class="file-info">
            <el-text type="info" size="small">
              已选择文件，共 {{ data.aniList.length }} 条数据
            </el-text>
          </div>
        </div>
        <UploadView url="api/uploadAndRead"
                    :extensions="['json']"
                    :callback="uploadCallback">
          <el-button bg>选择并上传文件</el-button>
        </UploadView>
      </div>

      <!-- 冲突处理设置 -->
      <div class="conflict-section" v-if="data.filename">
        <div class="section-title">
          <el-icon>
            <setting/>
          </el-icon>
          <span>冲突处理</span>
        </div>
        <div class="conflict-content">
          <el-radio-group v-model="data.conflict" class="conflict-options">
            <el-radio value="REPLACE" class="conflict-option">
              <div class="option-content">
                <div class="option-title">替换现有数据</div>
                <div class="option-desc">用新数据覆盖同名的现有数据</div>
              </div>
            </el-radio>
            <el-radio value="SKIP" class="conflict-option">
              <div class="option-content">
                <div class="option-title">跳过冲突数据</div>
                <div class="option-desc">保留现有数据，跳过重复项</div>
              </div>
            </el-radio>
          </el-radio-group>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-section">
        <el-button
            @click="dialogVisible = false"
            size="large"
        >
          取消
        </el-button>
        <el-button
            type="primary"
            :loading="importDataLoading"
            :disabled="!data.filename"
            @click="startImport"
            size="large"
        >
          <el-icon v-if="!importDataLoading">
            <upload/>
          </el-icon>
          {{ importDataLoading ? '导入中...' : '开始导入' }}
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>
<script setup>
import {getCurrentInstance, ref} from "vue";
import {Document, Setting, Upload} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";
import {importAni} from "@/js/http.js";
import UploadView from "@/view/custom/UploadView.vue";

let importDataLoading = ref(false);

let startImport = () => {
  importDataLoading.value = true;
  importAni(data.value)
      .then(res => {
        ElMessage.success(res.message)
        if (instance.vnode.props.onCallback) {
          emit('callback')
        }
        window.$reLoadList()
        dialogVisible.value = false
      })
      .finally(() => {
        importDataLoading.value = false
      })
}

let dialogVisible = ref(false);
let data = ref({
  filename: '',
  aniList: [],
  conflict: 'REPLACE'
})

let uploadCallback = (res, file) => {
  let {code, message} = res
  if (code === 200) {
    data.value.filename = file.name
    data.value.aniList = JSON.parse(res.data)
    return
  }
  ElMessage.error(message)
}

let show = () => {
  data.value = {
    filename: '',
    aniList: [],
    conflict: 'REPLACE'
  }
  dialogVisible.value = true;
}

defineExpose({show})

const instance = getCurrentInstance()

const emit = defineEmits(['callback'])
</script>

<style scoped>
.import-dialog {
  padding: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;
  font-size: 16px;
}

.upload-section {
  margin-bottom: 8px;
}

.file-selected {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  margin-bottom: 8px;
}

.file-tag {
  align-self: flex-start;
  font-size: 14px;
  padding: 8px 12px;
}

.file-info {
  margin-top: 4px;
}

.upload-sub-text em {
  color: #409eff;
  font-style: normal;
}

.conflict-section {
  margin-bottom: 24px;
  padding: 20px;
  border-radius: 8px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color);
}

.conflict-content {
  margin-top: 12px;
}

.conflict-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.conflict-option {
  margin: 0;
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  transition: all 0.3s;
  box-sizing: content-box;
}

.conflict-option:hover,
.conflict-option.is-checked {
  border-color: var(--el-color-primary);
  background: var(--el-fill-color);
}

.option-content {
  margin-left: 8px;
  width: 200px;
}

.option-title {
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.option-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.action-section {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color);
}
</style>
