<template>
  <ImportAni ref="importAniRef" @callback="getList"/>
  <Del ref="refDel" @callback="getList"/>
  <el-dialog v-model="dialogVisible" center title="管理">
    <div class="manage-content" v-loading="loading">
      <div class="manage-header">
        <div class="auto-flex">
          <div class="select-width">
            <el-select v-model:model-value="selectFilter"
                       @change="selectChange">
              <el-option v-for="filter in selectFilters"
                         :key="filter.label"
                         :label="filter.label"
                         :value="filter.label"/>
            </el-select>
          </div>
          <div class="spacer"></div>
          <div class="select-width">
            <el-select
                v-model:model-value="yearMonthValue"
                clearable
                @change="selectChange">
              <el-option v-for="it in yearMonth(list)"
                         :key="it" :label="it" :value="it"
              />
            </el-select>
          </div>
        </div>
        <div>
          <el-dropdown :trigger="'click'">
            <el-button bg text icon="MoreFilled"/>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="updateTotalEpisodeNumber(false)">
                  <el-text>
                    <el-icon>
                      <Refresh/>
                    </el-icon>
                    更新总集数
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item @click="updateTotalEpisodeNumber(true)">
                  <el-text type="warning">
                    <el-icon>
                      <Refresh/>
                    </el-icon>
                    更新总集数 [F]
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item divided @click="importAniRef?.show">
                  <el-text>
                    <el-icon>
                      <Download/>
                    </el-icon>
                    导入
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item @click="exportData">
                  <el-text>
                    <el-icon>
                      <Upload/>
                    </el-icon>
                    导出
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item divided @click="batchEnable(true)">
                  <el-text type="primary">
                    <el-icon>
                      <CircleCheck/>
                    </el-icon>
                    启用
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item @click="batchEnable(false)">
                  <el-text type="warning">
                    <el-icon>
                      <CircleClose/>
                    </el-icon>
                    禁用
                  </el-text>
                </el-dropdown-item>
                <el-dropdown-item divided @click="refDel?.show(selectList)">
                  <el-text type="danger">
                    <el-icon>
                      <Remove/>
                    </el-icon>
                    删除
                  </el-text>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <el-table
          size="small"
          @selection-change="handleSelectionChange"
          v-model:data="searchList"
          height="400px"
          stripe
      >
        <el-table-column type="selection" width="55" fixed/>
        <el-table-column label="状态" width="80">
          <template #default="it">
            <el-tag v-if="searchList[it.$index].enable">
              已启用
            </el-tag>
            <el-tag v-else type="info">
              未启用
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" width="200">
          <template #default="it">
            <el-text :line-clamp="2" size="small">
              {{ searchList[it.$index].title }}
            </el-text>
          </template>
        </el-table-column>
        <el-table-column label="季" prop="season" width="50"/>
        <el-table-column label="字幕组" width="100">
          <template #default="it">
            <el-text size="small" truncated>
              {{ searchList[it.$index].subgroup }}
            </el-text>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="100">
          <template #default="it">
            <el-tag type="warning">
              {{ searchList[it.$index]['currentEpisodeNumber'] }} /
              {{ searchList[it.$index]['totalEpisodeNumber'] ? searchList[it.$index]['totalEpisodeNumber'] : '*' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="it">
            <el-tag type="danger" v-if="searchList[it.$index].ova">
              ova
            </el-tag>
            <el-tag type="danger" v-else>
              tv
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="URL" width="600">
          <template #default="it">
            <el-text :line-clamp="1" size="small" truncated>
              {{ searchList[it.$index].url }}
            </el-text>
          </template>
        </el-table-column>
      </el-table>
      <div>
        <p class="manage-count">共 {{ searchList.length }} 项</p>
      </div>
    </div>
  </el-dialog>
</template>
<script setup>
import {ref} from "vue";
import api from "@/js/api.js";
import {ElMessage} from "element-plus";
import Del from "./Del.vue";
import ImportAni from "@/home/ImportAni.vue";
import {CircleCheck, CircleClose, Refresh, Remove, Upload} from "@element-plus/icons-vue";

let yearMonth = (list) => {
  return new Set(
      list
          .map(it => `${it['year']}-${it['month'] < 10 ? '0' + it['month'] : it['month']}`)
          .sort((a, b) => a > b ? -1 : 1)
  );
}

let refDel = ref()
let importAniRef = ref()

let selectFilter = ref('全部')

let selectFilters = ref([
  {
    label: '全部',
    fun: () => true
  },
  {
    label: '已启用',
    fun: it => it.enable
  },
  {
    label: '未启用',
    fun: it => !it.enable
  },
])

let searchList = ref([])

let selectChange = () => {
  searchList.value = list.value
      .filter(it => !yearMonthValue.value || yearMonthValue.value === `${it.year}-${it.month < 10 ? '0' + it.month : it.month}`)
      .filter(selectFilters.value.filter(item => selectFilter.value === item.label)[0].fun)
}

let dialogVisible = ref(false)
let loading = ref(false)

let show = () => {
  yearMonthValue.value = ''
  selectFilter.value = '全部'
  dialogVisible.value = true
  selectList.value = []
  getList()
}

const list = ref([])

const getList = () => {
  loading.value = true
  api.get('api/ani')
      .then(res => {
        list.value = res.data
        selectChange()
        window.$reLoadList()
      })
      .finally(() => {
        loading.value = false
      })
}

let selectList = ref([])

let handleSelectionChange = (v) => {
  selectList.value = v
}

let exportData = () => {
  if (!selectList.value.length) {
    ElMessage.error('未选择订阅')
    return
  }
  console.log(111);
  const textContent = JSON.stringify(selectList.value);
  const blob = new Blob([textContent], {type: "text/plain"});
  const url = URL.createObjectURL(blob);

  const a = document.createElement("a");
  a.style.display = "none";
  a.href = url;
  a.download = "ani.v2.json";
  document.body.appendChild(a);
  a.click();
  URL.revokeObjectURL(url);
  document.body.removeChild(a);
}

let batchEnable = (value) => {
  loading.value = true
  let ids = selectList.value.map(it => it['id']);
  api.post('api/ani?type=batchEnable&value=' + value, ids)
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        getList()
      })
}

let yearMonthValue = ref('')

let updateTotalEpisodeNumber = (force) => {
  let ids = selectList.value.map(it => it['id']);
  api.post('api/ani?type=updateTotalEpisodeNumber&force=' + force, ids)
      .then(res => {
        ElMessage.success(res.message)
        getList()
      })
}

defineExpose({show})
</script>

<style scoped>
.manage-content {
  min-height: 300px;
}

.manage-header {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.select-width {
  width: 120px;
}

.spacer {
  height: 8px;
  width: 8px;
}

.manage-count {
  margin: 6px;
  text-align: end;
}
</style>
