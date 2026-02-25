<template>
  <Config ref="config"/>
  <Add ref="add"/>
  <Logs ref="logs"/>
  <Manage ref="manage"/>
  <Collection ref="collection"/>
  <TorrentsInfos ref="torrentsInfosRef"/>
  <div class="content">
    <div id="header">
      <div style="margin: 10px;" class="auto-flex">
        <div>
          <el-input
              v-model:model-value="title"
              clearable
              placeholder="搜索"
              prefix-icon="Search"
              style="min-width: 210px"/>
        </div>
        <div style="height: 8px;width: 8px;"></div>
        <div style="min-width: 300px;display: flex">
          <div style="flex: 1;">
            <el-select
                v-model:model-value="yearMonth"
                clearable
                @change="selectChange"
            >
              <el-option v-for="it in list?.yearMonth()"
                         :key="it" :label="it" :value="it"
              />
            </el-select>
          </div>
          <div style="height: 8px;width: 8px;"></div>
          <div style="flex: 1;">
            <el-select v-model:model-value="enable"
                       @change="selectChange">
              <el-option v-for="selectItem in enableSelect"
                         :key="selectItem.label"
                         :label="selectItem.label"
                         :value="selectItem.label"
              >
              </el-option>
            </el-select>
          </div>
        </div>
      </div>
      <div class="add-button">
        <div style="margin: 0 4px;">
          <el-dropdown trigger="click">
            <el-button bg text type="primary">
              <el-icon :class="elIconClass">
                <Plus/>
              </el-icon>
              <template v-if="isNotMobile">
                添加
              </template>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="add?.show">
                  添加订阅
                </el-dropdown-item>
                <el-dropdown-item @click="collection?.show">
                  添加合集
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div style="margin: 0 4px;">
          <el-button bg text @click="torrentsInfosRef?.show">
            <el-icon :class="elIconClass">
              <Download/>
            </el-icon>
            <template v-if="isNotMobile">
              下载
            </template>
          </el-button>
        </div>
        <div style="margin: 0 4px;">
          <popconfirm title="立即刷新全部订阅?" @confirm="refreshAni">
            <template #reference>
              <el-button bg text>
                <el-icon :class="elIconClass">
                  <Refresh/>
                </el-icon>
                <template v-if="isNotMobile">
                  刷新
                </template>
              </el-button>
            </template>
          </popconfirm>
        </div>
        <div style="margin: 0 4px;">
          <el-button text bg @click="manage?.show">
            <el-icon :class="elIconClass">
              <Fold/>
            </el-icon>
            <template v-if="isNotMobile">
              管理
            </template>
          </el-button>
        </div>
        <div style="margin: 0 4px;">
          <el-badge :is-dot="about.update" class="item">
            <el-button @click="config?.show(about.update)" text bg>
              <el-icon :class="elIconClass">
                <Setting/>
              </el-icon>
              <template v-if="isNotMobile">
                设置
              </template>
            </el-button>
          </el-badge>
        </div>
        <div style="margin-left: 4px;">
          <el-button @click="logs?.show" text bg>
            <el-icon :class="elIconClass">
              <Tickets/>
            </el-icon>
            <template v-if="isNotMobile">
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
import api from "@/js/api.js";
import {ElMessage} from "element-plus";
import Popconfirm from "@/other/Popconfirm.vue";
import Manage from "./Manage.vue";
import {useLocalStorage} from "@vueuse/core";
import Collection from "./Collection.vue";
import TorrentsInfos from "./TorrentsInfos.vue";
import {elIconClass, initLayout, isNotMobile} from "@/js/global.js";
import * as http from "@/js/http.js";

const collection = ref()
const manage = ref()
const torrentsInfosRef = ref()

const title = ref('')
const enable = useLocalStorage('select-enable', '已启用')
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

const selectChange = () => {
  filter.value = (it) => {
    if (!enableSelect.value.filter(it => it.label === enable.value)[0].fun(it)) {
      return false
    }
    if (!yearMonth.value) {
      return true
    }
    return yearMonth.value === `${it.year}-${it.month < 10 ? '0' + it.month : it.month}`;
  }
}

const config = ref()
const add = ref()
const logs = ref()
const list = ref()

const currentPage = ref(1)


const about = ref({
  'version': '',
  'latest': '',
  'update': false,
  'markdownBody': ''
})


let refreshAni = () => {
  http.refreshAll()
      .then(res => {
        ElMessage.success(res.message)
      })
}

onMounted(() => {
  initLayout()
  selectChange()

  http.about()
      .then(res => {
        about.value = res.data
      })
})
</script>

<style scoped>
.content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.add-button {
  margin: 10px;
  display: flex;
  justify-content: flex-end;
}
</style>

