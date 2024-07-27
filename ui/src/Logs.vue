<template>
  <el-dialog v-model="logDialogVisible" title="日志" center>
    <div id="#logs" style="background-color:#2e3440ff;
                  color:#d8dee9ff;"
         v-loading="loading">
      <el-scrollbar ref="scrollbarRef" height="400px">
        <div ref="innerRef" style="padding: 5px;" v-html="logs">
        </div>
      </el-scrollbar>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import {codeToHtml} from 'shiki'

const logDialogVisible = ref(false)
const loading = ref(true)
const logs = ref('')
const scrollbarRef = ref()
const innerRef = ref()
const maxHeight = ref(0)

const showLogs = () => {
  logDialogVisible.value = true
  fetch('/api/logs', {
    'method': 'GET'
  }).then(res => res.json())
      .then(async res => {
        loading.value = false
        if (res.code !== 200) {
          ElMessage.error(res.message)
          return
        }
        logs.value = await codeToHtml(res.data.join('\r\n'), {
          lang: 'log',
          theme: 'nord'
        })
        setTimeout(() => {
          scrollbarRef.value?.setScrollTop(innerRef.value.clientHeight)
        })
      })

}

defineExpose({showLogs})
</script>