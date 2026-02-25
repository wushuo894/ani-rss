<template>
  <el-dialog v-model="dialogVisible" align-center center width="300" title="删除订阅">
    <div>
      <div v-if="aniList.length === 1">
        <el-text class="mx-1" size="large">是否删除 {{ aniList[0].title }} 第{{ aniList[0].season }}季?</el-text>
      </div>
      <div v-else>
        <el-text class="mx-1" size="large">是否删除共 {{ aniList.length }} 个订阅?</el-text>
      </div>
      <el-checkbox v-model="deleteFiles" class="el-checkbox-danger">同时删除下载任务与本地已下载的文件</el-checkbox>
    </div>
    <div class="action">
      <el-button icon="Check" :loading="okLoading" @click="delAni" text bg type="danger">确定
      </el-button>
      <el-button icon="Close" bg text @click="dialogVisible = false">取消</el-button>
    </div>
  </el-dialog>
</template>

<script setup>

import {getCurrentInstance, markRaw, ref} from "vue";
import * as http from "@/js/http.js";
import {deleteAni} from "@/js/http.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {Delete} from "@element-plus/icons-vue";

const dialogVisible = ref(false)

const aniList = ref([])

let okLoading = ref(false)
let deleteFiles = ref(false)

const delAni = async () => {
  okLoading.value = true
  let ids = aniList.value.map(it => it['id'])
  let action = () => deleteAni(deleteFiles.value, ids)
      .then(res => {
        ElMessage.success(res.message)
        if (instance.vnode.props.onCallback) {
          emit('callback')
        } else {
          window.$reLoadList()
        }
        dialogVisible.value = false
      })
      .finally(() => {
        okLoading.value = false
      });

  if (!deleteFiles.value) {
    await action()
    return
  }

  let downloadPath = ''

  if (aniList.value.length === 1) {
    let res = await http.downloadPath(aniList.value[0])
    downloadPath = res.data['downloadPath']
  }

  ElMessageBox.confirm(
      `<strong style="color: var(--el-color-danger);">
        将会删除整个文件夹, 是否执意继续?
        <br>
        ${downloadPath}
       </strong>`,
      '警告',
      {
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
  if (!anis.length) {
    ElMessage.error('未选择订阅')
    return
  }

  aniList.value = JSON.parse(JSON.stringify(anis))
  deleteFiles.value = false
  dialogVisible.value = true
}

defineExpose({
  show
})

const instance = getCurrentInstance()

const emit = defineEmits(['callback'])
</script>

<style scoped>
.action {
  width: 100%;
  display: flex;
  justify-content: end;
  margin-top: 8px;
}
</style>

