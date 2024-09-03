<template>
  <el-dialog v-model="editDialogVisible" title="修改订阅" center>
    <el-form label-width="auto"
             @submit="(event)=>{
                event.preventDefault()
             }"
    >
      <el-form-item label="标题">
        <el-input v-model:model-value="ani.title"></el-input>
      </el-form-item>
      <el-form-item label="季">
        <div style="display: flex;justify-content: end;width: 100%;">
          <el-input-number style="max-width: 200px" v-model:model-value="ani.season"></el-input-number>
        </div>
      </el-form-item>
      <el-form-item label="集数偏移">
        <div style="display: flex;justify-content: end;width: 100%;">
          <el-input-number v-model:model-value="ani.offset"></el-input-number>
        </div>
      </el-form-item>
      <el-form-item label="排除">
        <div class="flex gap-2">
          <el-tag
              v-for="tag in ani.exclude"
              :key="tag"
              closable
              :disable-transitions="false"
              @close="handleClose(tag)"
              style="margin-right: 4px;"
          >
            {{ tag }}
          </el-tag>
          <el-input
              style="max-width: 80px;"
              v-if="excludeVisible"
              ref="InputRef"
              v-model="excludeValue"
              class="w-20"
              size="small"
              @keyup.enter="handleInputConfirm"
              @blur="handleInputConfirm"
          />
          <el-button v-else class="button-new-tag" size="small" @click="showInput">
            +
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model:model-value="ani.enable"></el-switch>
      </el-form-item>
      <div style="display: flex;justify-content: end;width: 100%;margin-top: 10px;">
        <el-button :loading="editAniButtonLoading" @click="editAni">确定</el-button>
      </div>
    </el-form>
  </el-dialog>
</template>

<script setup>

import {ref} from "vue";
import {ElMessage} from "element-plus";
import api from "./api.js";

const editDialogVisible = ref(false)
const ani = ref({
  'url': '',
  'season': 1,
  'offset': 0,
  'title': '',
  'exclude': [],
  'enable': true
})


const excludeVisible = ref(false)
const excludeValue = ref('')

const handleClose = (tag) => {
  ani.value.exclude.splice(ani.value.exclude.indexOf(tag), 1)
}

const InputRef = ref()

const showInput = () => {
  excludeVisible.value = true
  InputRef.value?.input?.focus()
}

const handleInputConfirm = () => {
  if (excludeValue.value) {
    ani.value.exclude.push(excludeValue.value)
  }
  excludeVisible.value = false
  excludeValue.value = ''
}

const editAniButtonLoading = ref(false)

const editAni = () => {
  editAniButtonLoading.value = true
  api.put('/api/ani', ani.value)
      .then(res => {
        ElMessage.success(res.message)
        emit('load')
        editDialogVisible.value = false
      })
      .finally(() => {
        editAniButtonLoading.value = false
      })
}

const showEdit = (item) => {
  editDialogVisible.value = true
  ani.value = JSON.parse(JSON.stringify(item))
  excludeVisible.value = false
  excludeValue.value = ''
}

defineExpose({
  showEdit
})

const emit = defineEmits(['load'])

</script>
