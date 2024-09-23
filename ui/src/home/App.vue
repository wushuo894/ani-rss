<template>
  <Config ref="config"/>
  <Add ref="add" @load="list?.getList"/>
  <Logs ref="logs"/>
  <Manage ref="manage" @load="list?.getList"/>
  <div style="height: 100%;display: flex;flex-direction: column;">
    <div id="header">
      <div style="margin: 10px;display: flex;">
        <el-input
            v-model:model-value="title"
            placeholder="搜索"
            style="min-width: 210px"
            prefix-icon="Search"
            clearable/>
        <div style="width: 16px;"></div>
        <el-select
            v-model:model-value="yearMonth"
            @change="enableSelectChange"
            style="min-width: 120px"
            clearable
        >
          <el-option v-for="it in list?.yearMonth()"
                     :value="it" :label="it" :key="it"
          />
        </el-select>
      </div>
      <div style="margin: 10px;display: flex;justify-content: flex-end;">
        <div style="min-width: 100px;width:100%;margin-right: 4px;">
          <el-select v-model:model-value="enable"
                     @change="enableSelectChange">
            <el-option v-for="selectItem in enableSelect"
                       :key="selectItem.label"
                       :label="selectItem.label"
                       :value="selectItem.label"

            >
            </el-option>
          </el-select>
        </div>
        <div style="margin: 0 4px;">
          <el-button type="primary" @click="add?.show" bg text>
            <el-icon :class="elIconClass()">
              <Plus/>
            </el-icon>
            <template v-if="itemsPerRow > 1">
              添加
            </template>
          </el-button>
        </div>
        <div style="margin: 0 4px;">
          <popconfirm title="立即刷新全部订阅?" @confirm="download">
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
          </popconfirm>
        </div>
        <div style="margin: 0 4px;">
          <el-button text bg @click="manage?.show">
            <el-icon :class="elIconClass()">
              <Fold/>
            </el-icon>
            <template v-if="itemsPerRow > 1">
              管理
            </template>
          </el-button>
        </div>
        <div style="margin: 0 4px;">
          <el-badge :is-dot="about.update" class="item">
            <el-button @click="config?.show(about.update)" text bg>
              <el-icon :class="elIconClass()">
                <Setting/>
              </el-icon>
              <template v-if="itemsPerRow > 1">
                设置
              </template>
            </el-button>
          </el-badge>
        </div>
        <div style="margin-left: 4px;">
          <el-button @click="logs?.show" text bg>
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
import {Fold, Plus, Refresh, Setting, Tickets} from "@element-plus/icons-vue"
import Config from "./Config.vue";
import List from "./List.vue";
import Add from "./Add.vue";
import Logs from "./Logs.vue";
import api from "../api.js";
import {ElMessage} from "element-plus";
import Popconfirm from "../other/Popconfirm.vue";
import Manage from "./Manage.vue";

const manage = ref()

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
const yearMonth = ref('')

const enableSelectChange = () => {
  filter.value = (it) => {
    if (!enableSelect.value.filter(it => it.label === enable.value)[0].fun(it)) {
      return false
    }
    if (!yearMonth.value) {
      return true
    }
    if (yearMonth.value !== `${it.year}-${it.month < 10 ? '0' + it.month : it.month}`) {
      return false
    }
    return true
  }
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


