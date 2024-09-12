<template>
  <Config ref="config"/>
  <Add ref="add" @load="list?.getList"/>
  <Logs ref="logs"/>
  <div id="header">
    <div style="margin: 10px;">
      <el-input
          v-model:model-value="title"
          placeholder="搜索"
          @input="currentPage = 1"
          prefix-icon="Search"
          clearable/>
    </div>
    <div style="margin: 10px;display: flex;justify-content: flex-end;">
      <div style="min-width: 120px;width:100%;margin-right: 15px;">
        <el-select v-model:model-value="enable"  @change="enableSelectChange">
          <el-option v-for="selectItem in enableSelect"
                     :key="selectItem.label"
                     :label="selectItem.label"
                     :value="selectItem.label"

          >
          </el-option>
        </el-select>
      </div>
      <el-button type="primary" @click="add?.showAdd" bg text>
        <el-icon :class="elIconClass()">
          <Plus/>
        </el-icon>
        <template v-if="itemsPerRow > 1">
          添加
        </template>
      </el-button>
      <div style="margin: 0 6px;">
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
  <List ref="list" v-model:title="title" v-model:current-page="currentPage" v-model:filter="filter"/>
  <div style="height: 10px;"></div>
</template>

<script setup>
import {onMounted, ref} from "vue";
import {Plus, Setting, Tickets} from "@element-plus/icons-vue"
import Config from "./Config.vue";
import List from "./List.vue";
import Add from "./Add.vue";
import Logs from "./Logs.vue";
import api from "../api.js";

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

</script>


