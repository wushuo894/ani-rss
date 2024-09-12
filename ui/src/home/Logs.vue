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
      <el-scrollbar ref="scrollbarRef" height="450">
        <div ref="innerRef" style="min-height: 400px;" v-html="htmlLogs">
        </div>
      </el-scrollbar>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {codeToHtml} from 'shiki'
import api from "../api.js";

const logDialogVisible = ref(false)
const loading = ref(true)
const logs = ref([])
const scrollbarRef = ref()
const innerRef = ref()
const maxHeight = ref(0)

const options = ['All', 'DEBUG', 'INFO', 'WARN', 'ERROR']

const selectValue = ref(options[0])
const htmlLogs = ref('')

const getHtmlLogs = async () => {
  let log = logs.value
  if (selectValue.value !== options[0]) {
    log = log.filter(it => it['level'] === selectValue.value)
  }
  let code = log.map(it => it['message']).join('\r\n');
  htmlLogs.value = await codeToHtml(code, {
    lang: 'log',
    theme: 'nord'
  })
  setTimeout(() => {
    scrollbarRef.value?.setScrollTop(innerRef.value.clientHeight)
  })
}

const showLogs = () => {
  logs.value = []
  logDialogVisible.value = true
  loading.value = true
  htmlLogs.value = ''
  getLogs()
}

const getLogs = () => {
  api.get('/api/logs')
      .then(async res => {
        logs.value = res.data
        getHtmlLogs()
      })
      .finally(() => {
        loading.value = false
      })
}

defineExpose({showLogs})
</script>
