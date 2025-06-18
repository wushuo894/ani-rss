<template>
  <el-dialog v-model="dialogVisible" align-center center width="300">
    <div>
      <div v-if="aniList.length === 1">
        <el-text class="mx-1" size="large">是否删除 {{ aniList[0].title }} 第{{ aniList[0].season }}季?</el-text>
      </div>
      <div v-else>
        <el-text class="mx-1" size="large">是否删除共 {{ aniList.length }} 个订阅?</el-text>
      </div>
      <el-checkbox v-model="deleteFiles">同时删除下载任务与本地已下载的文件</el-checkbox>
    </div>
    <div style="width:100%;display: flex;justify-content: end;margin-top: 8px;">
      <el-button icon="Check" :loading="okLoading" @click="delAni" text bg type="danger">确定
      </el-button>
      <el-button icon="Close" bg text @click="dialogVisible = false">取消</el-button>
    </div>
  </el-dialog>
</template>

<script setup>

import {markRaw, ref} from "vue";
import api from "../js/api.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {Delete} from "@element-plus/icons-vue";

const dialogVisible = ref(false)

const aniList = ref([])

let okLoading = ref(false)
let deleteFiles = ref(false)

const delAni = async () => {
  okLoading.value = true
  let action = () => api.del('api/ani?deleteFiles=' + deleteFiles.value, aniList.value.map(it => it['id']))
      .then(res => {
        ElMessage.success(res.message)
        emit('load')
        dialogVisible.value = false
      })
      .finally(() => {
        okLoading.value = false
      });

  if (!deleteFiles.value) {
    action()
    return
  }

  let downloadPath = ''

  if (aniList.value.length === 1) {
    let res = await api.post('api/downloadPath', aniList.value[0])
    downloadPath = res.data['downloadPath']
  }

  let s = downloadPath ? `我确定删除文件夹 ${downloadPath}` : `我确定删除共${aniList.value.length}个订阅与本地文件`

  ElMessageBox.prompt(
      `<strong style="color: var(--el-color-danger);">
        将会删除整个文件夹, 请在输入框输入对应文本继续！
       </strong>
       <br>
       <span class="el-text el-text--small mx-1">
        <strong>将此文本填入文本框</strong> [${s}]
       </span>`,
      '警告',
      {
        inputPlaceholder: '在此输入确认文本',
        inputValidator: (it) => it === s,
        inputErrorMessage: `请输入 [${s}]`,
        dangerouslyUseHTMLString: true,
        confirmButtonText: '执意继续删除',
        confirmButtonClass: 'is-text is-has-bg el-button--danger',
        cancelButtonText: '取消',
        cancelButtonClass: 'is-text is-has-bg',
        type: 'warning',
        icon: markRaw(Delete),
      }
  )
      .then(action)
      .finally(() => {
        okLoading.value = false
      })
}

const show = (anis) => {
  aniList.value = JSON.parse(JSON.stringify(anis))
  deleteFiles.value = false
  dialogVisible.value = true
}

defineExpose({
  show
})

const emit = defineEmits(['load'])
</script>

<style scoped>
.el-checkbox {
  --el-checkbox-checked-text-color: var(--el-color-danger);
  color: var(--el-color-danger);
}
</style>

