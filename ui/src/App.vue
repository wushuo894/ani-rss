<template>
  <Config ref="config"></Config>
  <Add ref="add" @load="getList"></Add>
  <Edit ref="edit" @load="getList"></Edit>
  <Logs ref="logs"></Logs>
  <div id="header">
    <div style="margin: 10px;">
      <el-input v-model:model-value="title" placeholder="搜索" @input="currentPage = 1" clearable></el-input>
    </div>
    <div style="margin: 10px;display: flex;justify-content: flex-end;">
      <div style="min-width: 120px;width:100%;margin-right: 15px;">
        <el-select v-model:model-value="enable">
          <el-option v-for="selectItem in enableSelect"
                     :key="selectItem.label"
                     :label="selectItem.label"
                     :value="selectItem.label"
          >
          </el-option>
        </el-select>
      </div>
      <el-button @click="add?.showAdd">添加</el-button>
      <el-button @click="config?.showConfig">设置</el-button>
      <el-button @click="logs?.showLogs">日志</el-button>
    </div>
  </div>
  <div style="margin: 0 10px;min-height: 500px" v-loading="loading">
    <div class="grid-container">
      <div v-for="(item,index) in searchList().slice((currentPage-1)*pageSize,(currentPage-1)*pageSize+pageSize)">
        <el-card shadow="never">
          <div style="display: flex;width: 100%;">
            <img :src="'/api/file?filename='+item['cover']" height="130" width="92" :alt="item.title">
            <div style="flex-grow: 1;position: relative;">
              <div style="margin-left: 10px;">
                <div style="
                          column-count: 1;
                          overflow: hidden;
                          white-space: nowrap;
                          text-overflow: ellipsis;
                          width: 200px;
                          font-size: 0.97em;
                          line-height: 1.6;
                          font-weight: 500;
                          hyphens: auto;
                          letter-spacing: .0125em;">
                  {{ item.title }}
                </div>
                <div style="
                                    color: #9e9e9e !important;
                                    font-size: .75rem !important;
                                    font-weight: 400;
                                    line-height: 1.667;
                                    letter-spacing: .0333333333em !important;
                                    font-family: Roboto, sans-serif;
                                    text-transform: none !important;">
                  {{ item.url }}
                </div>
                <div style="
                        width: 180px;
                        display: grid;
                        grid-gap: 5px;
                        grid-template-columns: repeat(3, 1fr);
                        ">
                  <el-tag>
                    第 {{ item.season }} 季
                  </el-tag>
                  <el-tag type="success" v-if="item.enable">
                    已启用
                  </el-tag>
                  <el-tag type="danger" v-else>
                    未启用
                  </el-tag>
                  <el-tag type="info">
                    {{ item['subgroup'] }}
                  </el-tag>
                  <el-tag type="danger" v-if="item['currentEpisodeNumber'] && item['totalEpisodeNumber']">
                    {{ item['currentEpisodeNumber'] }} / {{ item['totalEpisodeNumber'] }}
                  </el-tag>
                </div>
              </div>
              <div
                  style="display: flex;align-items: flex-end;justify-content:flex-end; flex-direction: column;position: absolute;right: 0;bottom: 0;">
                <el-button @click="edit?.showEdit(item)">编辑
                </el-button>
                <div style="height: 5px;"></div>
                <el-popconfirm title="你确定要删除吗?" @confirm="delAni(item)">
                  <template #reference>
                    <el-button :loading="item['deleteLoading']">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
  <div style="margin: 10px;" id="page">
    <div style="margin-bottom: 10px;">
      <el-pagination background layout="prev, pager, next"
                     :total="searchList().length"
                     :pager-count="pagerCount"
                     v-model:current-page="currentPage"
                     v-model:page-size="pageSize"/>
    </div>
    <div style="display: flex;justify-content: space-between;width: 100%;">
      <div style="width: 100px;" id="page-size">
        <el-select v-model:model-value="pageSize" @change="updatePageSize">
          <el-option v-for="page in [10, 20, 40, 80, 160]"
                     :key="page"
                     :label="page"
                     :value="page"/>
        </el-select>
      </div>
      <div style="margin-left: 5px;">
        <el-popconfirm title="你确定要退出吗?" @confirm="logout">
          <template #reference>
            <el-button>退出登录</el-button>
          </template>
        </el-popconfirm>

      </div>
    </div>
  </div>
  <div style="height: 20px;"></div>
</template>

<script setup>
import {onMounted, ref} from "vue";
import {ElMessage} from 'element-plus'
import Config from "./Config.vue";
import Edit from "./Edit.vue";
import Add from "./Add.vue";
import Logs from "./Logs.vue";
import api from "./api.js";

const pagerCount = ref(10)

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

const config = ref()
const add = ref()
const edit = ref()
const logs = ref()
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(true)

const updatePageSize = (size) => {
  window.localStorage.setItem('pageSize', size.toString())
  currentPage.value = 1
}

const searchList = () => {
  const text = title.value.trim()
  const fun = enableSelect.value.filter(it => it.label === enable.value)[0].fun
  if (text.length < 1) {
    return list.value.filter(fun)
  }
  return list.value
      .filter(fun)
      .filter(it => {
        if (it['title'].toString().indexOf(text) > -1) {
          return true
        }
        let pinyin = it['pinyin']
        if (pinyin.indexOf(text) > -1) {
          return true
        }
        if (pinyin.replaceAll(' ', '').indexOf(text.replaceAll(' ', '')) > -1) {
          return true
        }
        return pinyin.split(' ').map(s => s.substring(0, 1)).join('').indexOf(text) > -1;
      });
}

const delAni = (ani) => {
  ani['deleteLoading'] = true
  api.del('/api/ani', ani)
      .then(res => {
        ElMessage.success(res.message)
        getList()
      })
      .finally(() => {
        ani['deleteLoading'] = false
      })
}

const list = ref([])

const getList = () => {
  api.get('/api/ani')
      .then(res => {
        list.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}

getList()

onMounted(() => {
  let size = window.localStorage.getItem('pageSize')
  if (size) {
    pageSize.value = Number.parseInt(size)
  }

  function updateGridLayout() {
    const gridContainer = document.querySelector('.grid-container');
    if (!gridContainer) {
      return
    }
    const windowWidth = window.innerWidth;
    const itemsPerRow = Math.max(1, Math.floor(windowWidth / 400));
    gridContainer.style.gridTemplateColumns = `repeat(${itemsPerRow}, 1fr)`;
    if (itemsPerRow === 1) {
      pagerCount.value = 4
    }
  }

  window.addEventListener('resize', updateGridLayout);
  updateGridLayout();
})

let logout = () => {
  localStorage.removeItem('authorization')
  location.reload()
}

</script>


