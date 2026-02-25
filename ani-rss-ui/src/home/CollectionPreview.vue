<template>
  <el-dialog v-model="dialogVisible"
             center
             class="el-dialog-auto-width"
             title="合集预览">
    <div v-loading="loading">
      <el-table :data="list" height="500"
                size="small"
                scrollbar-always-on
                stripe>
        <el-table-column label="标题" min-width="400" prop="title"/>
        <el-table-column label="重命名" min-width="280" prop="reName"/>
        <el-table-column label="集数" prop="episode"/>
        <el-table-column label="大小" min-width="100" prop="size"/>
      </el-table>
    </div>
    <div v-if="subgroup !== props.data.ani.subgroup && subgroup" style="margin-top:12px;">
      <el-alert close-text="应用" show-icon @close="closeAlert">
        <template #title>
          <div class="flex" style="width:100%;justify-content: space-between;">
            <span>
              检测到字幕组为 {{ subgroup }}
            </span>
          </div>
        </template>
      </el-alert>
    </div>
    <div class="action">
      <div>
        <span>共 {{ list.length }} 项</span>
      </div>
      <el-button bg icon="Close" text @click="dialogVisible = false">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import * as http from "@/js/http.js";

let dialogVisible = ref(false)
let loading = ref(false)

let list = ref([])

let subgroup = ref('')

let show = () => {
  subgroup.value = ''
  dialogVisible.value = true
  loading.value = true
  http.previewCollection(props.data)
      .then(res => {
        list.value = res.data
        subgroup.value = getSubgroup()
      })
      .finally(() => {
        loading.value = false
      })
}

let getSubgroup = () => {
  if (!list.value) {
    return ''
  }

  let subgroups = list.value
      .map(item => item['title'])
      .map(item => item.match(/^\[(.+?)]/))
      .filter(item => item)
      .map(item => item[1])

  if (subgroups) {
    return subgroups[0]
  }

  return ''
}

let closeAlert = () => {
  props.data.ani.subgroup = subgroup.value
  show()
}

defineExpose({show})

let props = defineProps(['data'])
</script>
<style scoped>
.action {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
}
</style>
