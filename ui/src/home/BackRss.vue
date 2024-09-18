<template>
  <el-dialog v-model="dialogVisible" title="备用订阅" center v-if="dialogVisible">
    <div style="display: flex;width: 100%;justify-content: end;">
      <el-button text bg icon="Plus" @click="plus">添加
      </el-button>
    </div>
    <el-scrollbar>
      <el-table v-model:data="backRss" height="400px">
        <el-table-column label="RSS" min-width="400px">
          <template #default="it">
            <el-input v-model:model-value="backRss[it.$index]" placeholder="https://xxx.xxx"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="it">
            <el-button bg text @click="del(it.$index)" icon="Delete"/>
          </template>
        </el-table-column>
      </el-table>
    </el-scrollbar>
    <div style="display: flex;width: 100%;justify-content: end;margin-top: 10px">
      <el-button icon="Check" bg text @click="ok">确定</el-button>
    </div>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";

const dialogVisible = ref(false)
const backRss = ref()


let show = () => {
  dialogVisible.value = true
  backRss.value = JSON.parse(JSON.stringify(props.ani.backRss))
}

let plus = () => {
  if (!backRss.value.length) {
    backRss.value.push('')
    return
  }
  if (backRss.value[backRss.value.length - 1].trim()) {
    backRss.value.push('')
  }
}

let del = (index) => {
  backRss.value = backRss.value.filter((s, i) => i !== index)
}

let ok = () => {
  props.ani.backRss = backRss.value
      .map(s => s.trim())
      .filter((s, i) => s !== '')
  dialogVisible.value = false
}

defineExpose({show})
let props = defineProps(['ani'])

</script>
