<template>
  <Config ref="config"/>
  <Add ref="add" @load="list?.getList"/>
  <Logs ref="logs"/>
  <div style="height: 100%;    display: flex;
    flex-direction: column;">
    <div id="header">
      <div style="margin: 10px;">
        <el-input
            v-model:model-value="title"
            placeholder="搜索"
            @input="list.value?.setCurrentPage(1)"
            prefix-icon="Search"
            clearable/>
      </div>
      <div style="margin: 10px;display: flex;justify-content: flex-end;">
        <div style="min-width: 120px;width:100%;margin-right: 4px;">
          <el-select v-model:model-value="enable" @change="enableSelectChange">
            <el-option v-for="selectItem in enableSelect"
                       :key="selectItem.label"
                       :label="selectItem.label"
                       :value="selectItem.label"

            >
            </el-option>
          </el-select>
        </div>
        <div style="margin: 0 4px;">
          <el-button type="primary" @click="add?.showAdd" bg text>
            <el-icon :class="elIconClass()">
              <Plus/>
            </el-icon>
            <template v-if="itemsPerRow > 1">
              添加
            </template>
          </el-button>
        </div>
        <div style="margin: 0 4px;">
          <el-badge :is-dot="about.update" class="item">
            <el-button @click="config?.showConfig(about.update)" text bg>
              <el-icon :class="elIconClass()">
                <Setting/>
              </el-icon>
              <template v-if="itemsPerRow > 1">
                设置
              </template>
            </el-button>
          </el-badge>
        </div>
        <div style="margin: 0 4px;">
          <el-popconfirm title="立即刷新全部订阅?" @confirm="download">
            <template #reference>
              <el-button text bg :loading="downloadLoading">
                <el-icon :class="elIconClass()">
                  <Refresh/>
                </el-icon>
                <template v-if="itemsPerRow > 1">
                  刷新
                </template>
              </el-button>
            </template>
            <template #actions="{ confirm, cancel }">
              <el-button size="small" @click="cancel" bg text icon="Close">取消</el-button>
              <div style="margin: 4px;"></div>
              <el-button
                  type="danger"
                  size="small"
                  @click="confirm"
                  bg text
                  icon="Check"
              >
                确定
              </el-button>
            </template>
          </el-popconfirm>
        </div>
        <div style="margin-left: 4px;">
          <el-button @click="logs?.showLogs" text bg>
            <el-icon :class="elIconClass()">
              <Tickets/>
            </el-icon>
            <template v-if="itemsPerRow > 1">
              日志
            </template>
          </el-button>
        </div>
      </div>
    </div>
    <div style="flex: 1;overflow: hidden;">
      <List ref="list" v-model:title="title" v-model:current-page="currentPage" v-model:filter="filter"/>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from "vue";
import {Plus, Refresh, Setting, Tickets} from "@element-plus/icons-vue"
import Config from "./Config.vue";
import List from "./List.vue";
import Add from "./Add.vue";
import Logs from "./Logs.vue";
import api from "../api.js";
import {ElMessage} from "element-plus";

const title = ref('')
const enable = ref('已启用')
const enableSelect = ref([
  {
    label: '全部',
    fun: () => true
  },
  {
    label: '已启用',
    fun: item => item.enable
  },
  {
    label: '未启用',
    fun: item => !item.enable
  }
])
const filter = ref(() => true)

const enableSelectChange = () => {
  filter.value = enableSelect.value.filter(it => it.label === enable.value)[0].fun
}

const config = ref()
const add = ref()
const logs = ref()
const list = ref()

const currentPage = ref(1)
const itemsPerRow = ref(1)

onMounted(() => {
  function updateGridLayout() {
    const windowWidth = window.innerWidth;
    itemsPerRow.value = Math.max(1, Math.floor(windowWidth / 400));
  }

  window.addEventListener('resize', updateGridLayout);
  updateGridLayout();

  enableSelectChange()
})

let elIconClass = () => {
  return itemsPerRow.value > 1 ? 'el-icon--left' : '';
}

const about = ref({
  'version': '',
  'latest': '',
  'update': false,
  'markdownBody': ''
})

api.get('/api/about')
    .then(res => {
      about.value = res.data
    })

let downloadLoading = ref(false)

let download = () => {
  downloadLoading.value = true
  api.post('/api/ani?download=true')
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        downloadLoading.value = false
      })
}

</script>


