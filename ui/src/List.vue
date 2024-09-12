<template>
  <Edit ref="edit" @load="getList"></Edit>
  <div style="margin: 0 10px;min-height: 500px" v-loading="loading">
    <el-empty v-if="!getPage().length" style="min-height: 500px"></el-empty>
    <div class="grid-container" v-show="getPage().length">
      <div v-for="(item,index) in getPage()">
        <el-card shadow="never">
          <div style="display: flex;width: 100%;align-items: center;">
            <div style="height: 100%;">
              <img :src="`/api/file?filename=${item['cover']}&s=${authorization()}`" height="130" width="92"
                   :alt="item.title"
                   style="border-radius: 4px;">
            </div>
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
                                    font-weight: 300;
                                    line-height: 1.667;
                                    -webkit-line-clamp: 2;
                                    max-width: 220px;
                                    overflow: hidden;
                                    text-overflow: ellipsis;
                                    letter-spacing: .0333333333em !important;
                                    font-family: Roboto, sans-serif;
                                    text-transform: none !important;">
                  {{ item.url }}
                </div>
                <div style="
                        width: 180px;
                        display: grid;
                        grid-gap: 4px;
                        "
                     :class="itemsPerRow > 1 ? 'gtc3' : 'gtc2'"
                >
                  <el-tag>
                    第 {{ item.season }} 季
                  </el-tag>
                  <el-tag type="success" v-if="item.enable">
                    已启用
                  </el-tag>
                  <el-tag type="success" v-else>
                    未启用
                  </el-tag>
                  <el-tag type="info" v-if="itemsPerRow > 1">
                    {{ item['subgroup'] ? item['subgroup'] : '未知' }}
                  </el-tag>
                  <el-tag type="info" v-else>
                    {{ (item['subgroup'] ? item['subgroup'] : '未知').substr(0, 6) }}
                  </el-tag>
                  <el-tag type="warning" v-if="item['currentEpisodeNumber']">
                    {{ item['currentEpisodeNumber'] }} /
                    {{ item['totalEpisodeNumber'] ? item['totalEpisodeNumber'] : '*' }}
                  </el-tag>
                  <el-tag type="danger" v-if="item.ova">
                    ova
                  </el-tag>
                  <el-tag type="danger" v-else>
                    tv
                  </el-tag>
                </div>
              </div>
              <div
                  style="display: flex;align-items: flex-end;justify-content:flex-end; flex-direction: column;position: absolute;right: 0;bottom: 0;">
                <el-button text @click="edit?.showEdit(item)" bg>
                  <el-icon>
                    <EditIcon/>
                  </el-icon>
                </el-button>
                <div style="height: 5px;"></div>
                <el-popconfirm title="你确定要删除吗?" @confirm="delAni(item)">
                  <template #reference>
                    <el-button type="danger" text :loading="item['deleteLoading']" bg>
                      <el-icon>
                        <Delete/>
                      </el-icon>
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
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
  <div style="height: 8px;"></div>
  <div style="margin: 0 10px;" id="page">
    <div style="margin-bottom: 10px;">
      <el-pagination background layout="prev, pager, next"
                     :total="searchList().length"
                     :pager-count="pagerCount"
                     v-model:current-page="props.currentPage"
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
            <el-button type="danger" bg text>
              <el-icon :class="elIconClass()">
                <Back/>
              </el-icon>
              <template v-if="itemsPerRow > 1">
                退出登录
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
    </div>
  </div>
  <div style="height: 10px;"></div>
</template>

<script setup>
import {onMounted, ref} from "vue";
import {ElMessage} from 'element-plus'
import {Back, Edit as EditIcon} from "@element-plus/icons-vue"
import Edit from "./Edit.vue";
import api from "./api.js";

const pagerCount = ref(10)

const edit = ref()
const pageSize = ref(10)
const loading = ref(true)

const updatePageSize = (size) => {
  window.localStorage.setItem('pageSize', size.toString())
  props.currentPage = 1
}

const searchList = () => {
  const text = props.title.trim()
  if (text.length < 1) {
    return list.value.filter(props.filter)
  }
  return list.value
      .filter(props.filter)
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

const getPage = () => {
  if (!props) {
    return searchList();
  }
  let start = (props?.currentPage - 1) * pageSize.value
  let end = (props?.currentPage - 1) * pageSize.value + pageSize.value
  return searchList().slice(start, end);
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

let authorization = () => {
  return window.authorization;
}

const itemsPerRow = ref(1)

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
    itemsPerRow.value = Math.max(1, Math.floor(windowWidth / 400));
    gridContainer.style.gridTemplateColumns = `repeat(${itemsPerRow.value}, 1fr)`;
    if (itemsPerRow.value === 1) {
      pagerCount.value = 4
    }
  }

  window.addEventListener('resize', updateGridLayout);
  updateGridLayout();

  getList()
})

let logout = () => {
  localStorage.removeItem('authorization')
  location.reload()
}

let elIconClass = () => {
  return itemsPerRow.value > 1 ? 'el-icon--left' : '';
}

defineExpose({
  getList
})

let props = defineProps(['title', 'currentPage', 'filter'])

</script>

<style>
.gtc3 {
  grid-template-columns: repeat(3, 1fr);
}

.gtc2 {
  grid-template-columns: repeat(2, 1fr);
}
</style>


