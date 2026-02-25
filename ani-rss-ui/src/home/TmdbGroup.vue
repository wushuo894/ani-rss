<template>
  <el-dialog v-model="dialogVisible" align-center center title="剧集组" width="400">
    <el-scrollbar v-loading="loading" class="tmdb-scrollbar">
      <el-card v-for="group in groupList" shadow="never" class="tmdb-card">
        <template #header>
          <div class="flex tmdb-header">
            <div>
              <el-link :href="`https://www.themoviedb.org/tv/${props.ani.tmdb['id']}/episode_group/${group.id}`"
                       target="_blank">
                {{ group.name }}
              </el-link>
              <el-badge v-if="props.ani.tmdb['tmdbGroupId'] === group.id" class="item tmdb-badge"
                        type="primary" value="已选择"/>
            </div>
            <el-button icon="Select" text @click="select(group)"/>
          </div>
        </template>
        <template #default>
          <div class="flex tmdb-content">
            <el-tag type="success">
              {{ group['typeName'] }}
            </el-tag>
            <div>
              <el-tag class="tmdb-tag-spacer">
                {{ group['groupCount'] }} 组
              </el-tag>
              <el-tag>
                {{ group['episodeCount'] }} 集
              </el-tag>
            </div>
          </div>
        </template>
      </el-card>
    </el-scrollbar>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import * as http from "@/js/http.js";

let dialogVisible = ref(false)

let groupList = ref([])

let loading = ref(false)

let show = () => {
  loading.value = true
  dialogVisible.value = true
  http.getThemoviedbGroup(props.ani)
      .then(res => {
        groupList.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}

let select = (group) => {
  props.ani.tmdb['tmdbGroupId'] = group.id
  dialogVisible.value = false
}

defineExpose({show})
let props = defineProps(['ani'])
</script>

<style scoped>
.tmdb-scrollbar {
  height: 400px;
}

.tmdb-card {
  margin-bottom: 4px;
}

.tmdb-header {
  width: 100%;
  justify-content: space-between;
}

.tmdb-badge {
  margin-left: 4px;
}

.tmdb-content {
  width: 100%;
  justify-content: space-between;
}

.tmdb-tag-spacer {
  margin-right: 4px;
}
</style>
