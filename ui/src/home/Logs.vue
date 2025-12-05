<template>
  <el-dialog v-model="dialogVisible" center class="logs-dialog" title="日志" @close="close">
    <div style="width: 100%;justify-content: space-between;align-items: center;" class="auto">
      <el-checkbox-group v-model:model-value="selectLevels" @change="()=>getHtmlLogs()">
        <el-checkbox v-for="item in levels" :key="item" :label="item" :value="item" size="large"/>
      </el-checkbox-group>
      <div style="display: flex;">
        <el-select
            v-model="selectLoggerNames"
            multiple
            collapse-tags
            placeholder="类名"
            style="width: 260px;"
            @change="()=>getHtmlLogs()"
        >
          <el-option
              v-for="item in loggerNames"
              :key="item"
              :label="item"
              :value="item"
          />
        </el-select>
        <div style="width: 4px;"></div>
        <el-button icon="Download" bg text @click="downloadLogs"/>
        <div style="width: 4px;"></div>
        <el-button icon="Refresh" bg text @click="getLogs" :loading="getLogsLoading"/>
        <div style="width: 4px;"></div>
        <el-button icon="Delete" bg text @click="clear" :loading="clearLoading"/>
      </div>
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
import {onMounted, ref} from "vue";
import api from "@/js/api.js";
import {authorization} from "@/js/global.js";

import {createOnigurumaEngine} from 'shiki/engine/oniguruma'
import log from 'shiki/langs/log'
import nord from 'shiki/themes/nord'
import wasm from 'shiki/wasm'
import {createHighlighterCore} from "shiki";

let highlighter = undefined
onMounted(async () => {
  highlighter = await createHighlighterCore({
    themes: [nord],
    langs: [log],
    engine: createOnigurumaEngine(wasm)
  })
})

const dialogVisible = ref(false)
const loading = ref(true)
const logs = ref([])
const scrollbarRef = ref()
const innerRef = ref()

const levels = ['DEBUG', 'INFO', 'WARN', 'ERROR']
const selectLevels = ref([])
const htmlLogs = ref('')
const loggerNames = ref([])
const selectLoggerNames = ref([])

const getHtmlLogs = async () => {
  let log = logs.value
  log = log.filter(it => selectLevels.value.indexOf(it['level']) > -1)
  if (selectLoggerNames.value.length) {
    log = log.filter(it => selectLoggerNames.value.indexOf(it['loggerName']) > -1)
  }
  let code = log.map(it => it['message']).join('\r\n');
  htmlLogs.value = highlighter.codeToHtml(code, {
    lang: 'log',
    theme: 'nord'
  })
  setTimeout(() => {
    scrollbarRef.value?.setScrollTop(innerRef.value.clientHeight)
  })
}

const show = () => {
  logs.value = []
  dialogVisible.value = true
  loading.value = true
  htmlLogs.value = ''
  getLogs()
  selectLevels.value = levels
}

const getLogsLoading = ref(false)
const clearLoading = ref(false)

const clear = () => {
  clearLoading.value = true
  api.del('api/logs')
      .then(res => {
        getLogs();
      })
      .finally(() => {
        clearLoading.value = false
      })
}

const getLogs = () => {
  getLogsLoading.value = true
  api.get('api/logs')
      .then(async res => {
        logs.value = res.data
        loggerNames.value = []
        for (let datum of res.data) {
          if (loggerNames.value.indexOf(datum['loggerName']) > -1) {
            continue
          }
          loggerNames.value.push(datum['loggerName'])
        }
        getHtmlLogs()
      })
      .finally(() => {
        loading.value = false
        getLogsLoading.value = false
      })
}

let downloadLogs = () => {
  window.open(`api/downloadLogs?s=${authorization.value}`)
}

let close = () => {
  htmlLogs.value = ''
  loggerNames.value = []
  selectLoggerNames.value = []
}

defineExpose({show})
</script>

<style>
@media (min-width: 1000px) {
  .auto {
    display: flex;
  }
}

@media (min-width: 1400px) {
  .logs-dialog {
    width: 1000px;
  }
}
</style>
