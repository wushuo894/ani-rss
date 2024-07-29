<template>
  <el-dialog v-model="logDialogVisible" title="日志" center>
    <div>
      <el-select v-model="selectValue" @change="()=>{getHtmlLogs()}">
        <el-option v-for="item in options"
                   :key="item"
                   :label="item"
                   :value="item"/>
      </el-select>
    </div>
    <div id="#logs" style="background-color:#2e3440ff;
                  color:#d8dee9ff;
                  margin-top: 5px;"
         v-loading="loading">
      <el-scrollbar ref="scrollbarRef" height="400px">
        <div ref="innerRef" style="padding: 5px;" v-html="htmlLogs">
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
const logs = ref([])
const scrollbarRef = ref()
const innerRef = ref()
const maxHeight = ref(0)

const options = ['All', 'DEBUG', 'INFO', 'WARNING', 'ERROR']

const selectValue = ref(options[0])
const htmlLogs = ref('')

const getHtmlLogs = async () => {
  let log = logs.value
  if (selectValue.value !== options[0]) {
    log = log.filter(it => it.toString().includes(selectValue.value))
  }
  htmlLogs.value = await codeToHtml(log.join('\r\n'), {
    lang: 'log',
    theme: 'nord'
  })
  setTimeout(() => {
    scrollbarRef.value?.setScrollTop(innerRef.value.clientHeight)
  })
}

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
        logs.value = res.data
        getHtmlLogs()
      })
}

defineExpose({showLogs})
</script>
