<template>
  <Edit ref="editRef"/>
  <PlayList ref="playListRef"/>
  <Cover ref="coverRef"/>
  <Del ref="delRef"/>
  <BgmRate ref="bgmRateRef"/>
  <div class="list-container" v-loading="loading">
    <el-scrollbar class="hide-scrollbar">
      <div class="list-content">
        <template v-if="showWeek">
          <div v-for="weekItem in filterList">
            <h2 class="list-week-title">
              {{ weekItem.weekLabel }}
            </h2>
            <div class="grid-container">
              <div v-for="item in weekItem.items" :key="item.id">
                <AniCard
                    :item="item"
                    @edit="editRef?.show"
                    @playlist="playListRef?.show"
                    @cover="coverRef?.show"
                    @del="delRef?.show"
                    @rate="bgmRateRef?.show"
                />
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="grid-container">
            <div v-for="item in flatFilterList">
              <AniCard
                  :item="item"
                  @edit="editRef?.show"
                  @playlist="playListRef?.show"
                  @cover="coverRef?.show"
                  @del="delRef?.show"
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
import {onMounted, ref} from "vue";
import Edit from "./Edit.vue";
import PlayList from "@/play/PlayList.vue";
import Cover from "./Cover.vue";
import Del from "./Del.vue";
import BgmRate from "./BgmRate.vue";
import formatTime from "@/js/format-time.js";
import {listAni} from "@/js/http.js";
import AniCard from "@/home/AniCard.vue";
import {showWeek} from "@/js/global.js";

const editRef = ref()
const delRef = ref()
const coverRef = ref()
const playListRef = ref()
const bgmRateRef = ref()

const weekList = ref([])
const filterList = ref([])
const flatFilterList = ref([])
const releaseDateList = ref([])

const loading = ref(true)

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
              return {...it, lastDownloadFormat: formatTime(it['lastDownloadTime'])}
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
      .sort((a, b) => a.sort > b.sort)
}

const getList = () => {
  loading.value = true

  listAni()
      .then(res => {
        let data = res.data
        weekList.value = data.weekList
        releaseDateList.value = data.releaseDateList

        updateGridLayout()
        changeFilterList(props.title)
      })
      .finally(() => {
        loading.value = false
      })
}

let updateGridLayout = () => {
  const app = document.querySelector('#app');
  let gridColumns = Math.max(1, Math.floor(app.offsetWidth / 400));

  const el = document.documentElement
  el.style.setProperty('--grid-columns', gridColumns)
}

onMounted(() => {
  window.addEventListener('resize', updateGridLayout);
  window.$reLoadList = getList
  getList()
})

defineExpose({
  releaseDateList,
  changeFilterList
})

let props = defineProps({
  title: String,
  filter: Function
})

</script>

<style scoped>
.grid-container {
  display: grid;
  grid-gap: 8px;
  width: 100%;
  grid-template-columns: repeat(var(--grid-columns), 1fr);
}

.list-container {
  height: 100%;
  overflow: hidden;
}

.list-content {
  margin: 0 10px;
}

.list-week-title {
  margin: 16px 0 8px 4px;
}

.list-bottom-spacer {
  height: 8px;
}
</style>


