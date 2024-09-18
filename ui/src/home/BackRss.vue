<template>
  <el-dialog v-model="dialogVisible" title="备用订阅" center v-if="dialogVisible" @close="close">
    <div style="display: flex;width: 100%;justify-content: end;">
      <el-button text bg icon="Plus" @click="plus">添加
      </el-button>
    </div>
    <el-scrollbar>
      <el-table v-model:data="props.ani.backRss" height="400px">
        <el-table-column label="url" min-width="400px">
          <template #default="it">
            <el-input v-model:model-value="props.ani.backRss[it.$index]" placeholder="https://xxx.xxx"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="it">
            <el-button bg text @click="del(it.$index)" icon="Delete"/>
          </template>
        </el-table-column>
      </el-table>
    </el-scrollbar>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";

const dialogVisible = ref(false)


let show = () => {
  dialogVisible.value = true
}

let plus = () => {
  if (!props.ani.backRss.length) {
    props.ani.backRss.push('')
    return
  }
  if (props.ani.backRss[props.ani.backRss.length - 1].trim()) {
    props.ani.backRss.push('')
  }
}

let del = (index) => {
  props.ani.backRss = props.ani.backRss.filter((s, i) => i !== index)
}

let close = () => {
  props.ani.backRss = props.ani.backRss
      .map(s => s.trim())
      .filter((s, i) => s !== '')
}

defineExpose({show})
let props = defineProps(['ani'])

</script>
