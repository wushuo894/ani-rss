<template>
  <EditAniView ref="editAniRef"/>
  <PlayListView ref="playListRef"/>
  <CoverView ref="coverRef"/>
  <DelAniView ref="delAniRef"/>
  <BgmRateView ref="bgmRateRef"/>
  <div class="list-container" v-loading="loading">
    <el-scrollbar class="hide-scrollbar">
      <div class="list-content">
        <template v-if="showWeek">
          <div v-for="weekItem in filterList" :key="weekItem.weekLabel">
            <h2 class="list-week-title">
              {{ weekItem.weekLabel }}
            </h2>
            <div :class="gridClass">
              <div v-for="item in weekItem.items" :key="item.id">
                <component
                    :is="viewComponent"
                    :item="item"
                    @edit="editAniRef?.show"
                    @playlist="playListRef?.show"
                    @cover="coverRef?.show"
                    @del="delAniRef?.show"
                    @rate="bgmRateRef?.show"
                />
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <div :class="gridClass">
            <div v-for="item in flatFilterList" :key="item.id">
              <component
                  :is="viewComponent"
                  :item="item"
                  @edit="editAniRef?.show"
                  @playlist="playListRef?.show"
                  @cover="coverRef?.show"
                  @del="delAniRef?.show"
                  @rate="bgmRateRef?.show"
              />
            </div>
          </div>
        </template>
        <div class="list-bottom-spacer"></div>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from "vue";
import EditAniView from "./EditAniView.vue";
import PlayListView from "@/view/play/PlayListView.vue";
import CoverView from "./CoverView.vue";
import DelAniView from "./DelAniView.vue";
import BgmRateView from "./BgmRateView.vue";
import {fromNow} from "@/js/format.js";
import {listAni} from "@/js/http.js";
import AniCardView from "@/view/home/AniCardView.vue";
import AniCoverView from "@/view/home/AniCoverView.vue";
import {showWeek} from "@/js/global.js";

const props = defineProps({
  title: String,
  filter: Function,
  viewMode: {
    type: String,
    default: 'card'
  }
})
const emit = defineEmits(['loaded'])

const editAniRef = ref()
const delAniRef = ref()
const coverRef = ref()
const playListRef = ref()
const bgmRateRef = ref()

const weekList = ref([])
const filterList = ref([])
const flatFilterList = ref([])
const releaseDateList = ref([])

const loading = ref(true)
const viewComponent = computed(() => props.viewMode === 'cover' ? AniCoverView : AniCardView)
const gridClass = computed(() => [
  'grid-container',
  props.viewMode === 'cover' ? 'cover-grid-container' : 'card-grid-container'
])

const changeFilterList = (text = '') => {
  let tempList = weekList.value;
  tempList = JSON.parse(JSON.stringify(tempList))

  const filter = item => {
    if (text.length < 1) {
      return true
    }
    let {title, pinyin, pinyinInitials} = item
    return title.indexOf(text) > -1 ||
        pinyin.indexOf(text) > -1 ||
        pinyinInitials.indexOf(text) > -1;
  }

  filterList.value = tempList
      .map(it => {
        let items = it.items;
        items = items
            .filter(props.filter)
            .filter(filter)
            .map(it => {
              return {...it, lastDownloadFormat: fromNow(it['lastDownloadTime'])}
            });
        return {
          weekLabel: it.weekLabel,
          items
        }
      })
      .filter(it => it.items.length)

  // 当不按星期展示时，展平并排序
  flatFilterList.value = Array.from(filterList.value)
      .flatMap(it => it.items)
      .sort((a, b) => a.sort - b.sort)
}

const getList = () => {
  loading.value = true

  listAni()
      .then(res => {
        let data = res.data
        weekList.value = data.weekList
        releaseDateList.value = data.releaseDateList
        emit('loaded', {
          releaseDateList: releaseDateList.value,
          total: weekList.value.reduce((total, week) => total + week.items.length, 0)
        })

        changeFilterList(props.title)
      })
      .finally(() => {
        loading.value = false
      })
}

onMounted(() => {
  window.$reLoadList = getList
  getList()
})

defineExpose({
  releaseDateList,
  changeFilterList,
  getList
})

</script>

<style scoped>
.grid-container {
  display: grid;
  grid-gap: 8px;
  width: 100%;
}

.list-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.hide-scrollbar {
  flex: 1;
  min-height: 0;
}

.list-content {
  margin: 0;
}

.list-week-title {
  margin: 16px 0 8px 4px;
}

.list-bottom-spacer {
  height: 8px;
}

.card-grid-container {
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
}

.cover-grid-container {
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  grid-gap: 24px;
}

@media (max-width: 800px) {
  .card-grid-container {
    grid-template-columns: 1fr;
  }

  .cover-grid-container {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    grid-gap: 12px;
  }
}
</style>
