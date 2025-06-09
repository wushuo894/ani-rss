<template>
  <el-dialog v-model="dialogVisible" align-center center title="剧集组" width="400">
    <el-scrollbar v-loading="loading" style="height: 400px">
      <el-card v-for="group in groupList" shadow="never" style="margin-bottom: 4px;">
        <template #header>
          <div class="flex" style="width: 100%;justify-content: space-between;">
            <div>
              <el-link :href="`https://www.themoviedb.org/tv/${props.ani.tmdb['id']}/episode_group/${group.id}`"
                       target="_blank">
                {{ group.name }}
              </el-link>
              <el-badge v-if="props.ani.tmdb['tmdbGroupId'] === group.id" class="item" style="margin-left: 4px;" type="primary" value="已选择"/>
            </div>
            <el-button icon="Select" text @click="select(group)"/>
          </div>
        </template>
        <template #default>
          <div class="flex" style="width: 100%;justify-content: space-between;">
            <el-tag type="success">
              {{ group['typeName'] }}
            </el-tag>
            <div>
              <el-tag style="margin-right: 4px;">
                {{ group['group_count'] }} 组
              </el-tag>
              <el-tag>
                {{ group['episode_count'] }} 集
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
import api from "../js/api.js";

let dialogVisible = ref(false)

let groupList = ref([])

let loading = ref(false)

let show = () => {
  loading.value = true
  dialogVisible.value = true
  api.post('api/tmdb?method=getTmdbGroup', props.ani)
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
