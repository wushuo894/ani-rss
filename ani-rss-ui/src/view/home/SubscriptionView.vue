<template>
  <div class="subscription-page app-page-layout">
    <AddView ref="addRef"/>
    <CollectionView ref="collectionRef"/>
    <ManageView ref="manageRef"/>
    <PageHeaderView title="订阅" :subtitle="`共 ${subscriptionTotal} 个订阅`"/>
    <div class="subscription-body app-page-content app-page-padding">
      <div class="subscription-toolbar">
        <div class="subscription-filters">
          <el-input
              v-model="title"
              class="subscription-search"
              clearable
              placeholder="搜索"
              prefix-icon="Search"
              @clear="changeFilterList"
              @input="changeFilterList"/>
          <el-select
              v-model:model-value="releaseDate"
              class="subscription-select"
              clearable
              placeholder="日期"
              @change="selectChange">
            <el-option v-for="it in releaseDateList"
                       :key="it"
                       :label="it"
                       :value="it"/>
          </el-select>
          <el-select
              v-model:model-value="enable"
              class="subscription-select"
              @change="selectChange">
            <el-option v-for="selectItem in enableSelect"
                       :key="selectItem.label"
                       :label="selectItem.label"
                       :value="selectItem.label"/>
          </el-select>
        </div>
        <div class="subscription-actions">
          <el-dropdown trigger="click">
            <el-button aria-label="添加" type="primary" class="auto-button" icon="Plus">
              添加
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="addRef?.show">
                  添加订阅
                </el-dropdown-item>
                <el-dropdown-item @click="collectionRef?.show">
                  添加合集
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <PopconfirmView title="立即刷新全部订阅?" @confirm="refreshAni">
            <template #reference>
              <el-button aria-label="刷新" :loading="refreshLoading" class="auto-button" icon="Refresh">
                刷新
              </el-button>
            </template>
          </PopconfirmView>
          <el-button aria-label="管理" @click="manageRef?.show" class="auto-button" icon="Fold">
            管理
          </el-button>
        </div>
      </div>
      <SubscriptionListView
          ref="listRef"
          :filter="filter"
          :title="title"
          :view-mode="subscriptionViewMode"
          @loaded="listLoaded"/>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from "vue";
import {ElMessage} from "element-plus";
import {useLocalStorage} from "@vueuse/core";
import SubscriptionListView from "@/view/home/SubscriptionListView.vue";
import AddView from "@/view/home/AddView.vue";
import CollectionView from "@/view/home/CollectionView.vue";
import ManageView from "@/view/home/ManageView.vue";
import PopconfirmView from "@/view/custom/PopconfirmView.vue";
import PageHeaderView from "@/view/custom/PageHeaderView.vue";
import {subscriptionViewMode} from "@/js/global.js";
import * as http from "@/js/http.js";

const listRef = ref()
const addRef = ref()
const collectionRef = ref()
const manageRef = ref()
const title = ref('')
const releaseDate = ref('')
const releaseDateList = ref([])
const subscriptionTotal = ref(0)
const refreshLoading = ref(false)
const enable = useLocalStorage('select-enable', '已启用')
const enableSelect = [
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
]
const filter = ref(() => true)

const changeFilterList = () => {
  listRef.value?.changeFilterList(title.value)
}

const selectChange = () => {
  filter.value = it => {
    const selectedEnable = enableSelect.find(item => item.label === enable.value)
    if (selectedEnable && !selectedEnable.fun(it)) {
      return false
    }
    if (!releaseDate.value) {
      return true
    }

    return releaseDate.value === it.releaseDate.replace(/-\d{2}$/, '')
  }
  changeFilterList()
}

const listLoaded = data => {
  releaseDateList.value = data.releaseDateList || []
  subscriptionTotal.value = data.total || 0
}

const refreshAni = () => {
  refreshLoading.value = true
  http.refreshAll()
      .then(res => {
        ElMessage.success(res.message)
        listRef.value?.getList()
      })
      .finally(() => {
        refreshLoading.value = false
      })
}

onMounted(() => {
  selectChange()
})
</script>

<style scoped>
.subscription-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-bottom: 10px;
}

.subscription-filters,
.subscription-actions {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.subscription-filters {
  flex: 1;
}

.subscription-actions {
  flex-shrink: 0;
}

.subscription-actions > * {
  margin: 0 !important;
}

.subscription-actions :deep(.el-button) {
  margin: 0;
}

.subscription-search {
  width: 220px;
}

.subscription-select {
  width: 128px;
}

@media (max-width: 900px) {
  .subscription-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .subscription-filters,
  .subscription-actions {
    width: 100%;
  }

  .subscription-filters {
    flex-wrap: wrap;
  }

  .subscription-actions {
    justify-content: flex-end;
  }

  .subscription-search {
    flex: 1 1 180px;
  }

  .subscription-select {
    flex: 1 1 120px;
  }
}

@media (max-width: 560px) {
  .subscription-toolbar {
    padding-bottom: 8px;
  }

  .subscription-actions {
    justify-content: flex-end;
    gap: 4px;
  }
}
</style>
