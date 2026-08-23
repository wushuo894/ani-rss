<template>
  <div class="content flex">
    <el-button bg @click="exportConfig" icon="Upload">导出设置</el-button>
    <el-button bg @click="importConfig" icon="Download">导入设置</el-button>
    <UploadView ref="uploadRef" url="api/importConfig" :types="['application/zip']" :callback="callback"/>
  </div>
</template>
<script setup>
import {authorization} from "@/js/global.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {markRaw, ref} from "vue";
import {WarnTriangleFilled} from "@element-plus/icons-vue";
import UploadView from "@/view/custom/UploadView.vue";

let uploadRef = ref()

let importConfig = () => {
  ElMessageBox.confirm(
      `<strong style="color: var(--el-color-danger);">
        将会覆盖掉现有的设置、订阅、下载记录, 是否执意继续?
       </strong>`,
      '警告',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '继续',
        confirmButtonClass: 'is-text is-has-bg el-button--danger',
        cancelButtonText: '取消',
        cancelButtonClass: 'is-text is-has-bg',
        type: 'warning',
        icon: markRaw(WarnTriangleFilled),
      }
  )
      .then(() => {
        uploadRef.value.selectAndUpload();
      })
}

let callback = res => {
  let {code, message} = res
  if (code !== 200) {
    ElMessage.error(message)
    return
  }
  ElMessage.success(message)
  setTimeout(() => {
    location.reload();
  }, 1000)
}

let exportConfig = () => {
  let element = document.createElement('a');
  element.href = `api/exportConfig?s=${authorization.value}`

  document.body.appendChild(element);

  element.click();

  document.body.removeChild(element);
}

let props = defineProps(['config'])
</script>
<style scoped>
.content {
  width: 100%;
  justify-content: center;
}
</style>