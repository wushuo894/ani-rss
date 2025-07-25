<template>
  <Del ref="refDel" @load="getList"/>
  <el-dialog v-model="dialogVisible" center class="manage-dialog" title="管理">
    <div style="min-height: 300px;" v-loading="loading">
      <div style="display: flex;justify-content: space-between;width: 100%;">
        <div class="auto">
          <div style="width: 120px;">
            <el-select v-model:model-value="selectFilter"
                       @change="selectChange">
              <el-option v-for="filter in selectFilters"
                         :key="filter.label"
                         :label="filter.label"
                         :value="filter.label"/>
            </el-select>
          </div>
          <div style="height: 8px;width: 8px;"></div>
          <div style="width: 120px;">
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
          <el-button :disabled="!selectList.length" bg icon="Upload" text @click="exportData">
            导出
          </el-button>
          <el-button :loading="importDataLoading" bg icon="Download" text @click="importData">
            导入
          </el-button>
          <el-button type="primary" :disabled="!selectList.length" bg icon="CircleCheck" text
                     @click="batchEnable(true)">
            启用
          </el-button>
          <el-button type="warning" :disabled="!selectList.length" bg icon="CircleClose" text
                     @click="batchEnable(false)">
            禁用
          </el-button>
          <el-button icon="Remove" bg text :disabled="!selectList.length" type="danger"
                     @click="refDel?.show(selectList)">删除
          </el-button>
        </div>
      </div>
      <el-table
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
        <p style="margin: 6px;text-align: end;">共 {{ searchList.length }} 项</p>
      </div>
    </div>
  </el-dialog>
</template>
<script setup>
import {ref} from "vue";
import api from "@/js/api.js";
import {ElMessage} from "element-plus";
import Del from "./Del.vue";

let yearMonth = (list) => {
  return new Set(
      list
          .map(it => `${it['year']}-${it['month'] < 10 ? '0' + it['month'] : it['month']}`)
          .sort((a, b) => a > b ? -1 : 1)
  );
}

let refDel = ref()

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
        emit('load')
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

let importDataLoading = ref(false)

let importData = () => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '.json';
  input.style.display = 'none';
  document.body.appendChild(input);
  input.addEventListener('change', async () => {
    const file = input.files[0];
    const reader = new FileReader();

    reader.onload = function (e) {
      const fileContent = e.target.result;
      importDataLoading.value = true
      api.post('api/ani/import', JSON.parse(fileContent.toString()))
          .then(res => {
            ElMessage.success(res.message)
            getList()
          })
          .finally(() => {
            importDataLoading.value = false
          })
      document.body.removeChild(input);
    };
    reader.readAsText(file);
  });
  input.click();
}

let batchEnable = (value) => {
  loading.value = true
  api.post('api/ani?type=batchEnable&value=' + value, selectList.value.map(it => it['id']))
      .then(res => {
        ElMessage.success(res.message)
        getList()
      })
}

let yearMonthValue = ref('')

defineExpose({show})
const emit = defineEmits(['load'])
</script>

<style>
@media (min-width: 1400px) {
  .manage-dialog {
    width: 1000px;
  }
}
</style>
