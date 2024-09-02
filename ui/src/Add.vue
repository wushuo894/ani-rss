<template>
  <Mikan ref="mikan" @add="args => ani.url = args"></Mikan>
  <el-dialog v-model="addDialogVisible" title="添加订阅" center>
    <div v-if="showRss" @keydown.enter="getRss">
      <el-form label-width="auto"
               v-if="showRss" @keydown.enter="getRss"
               @submit="(event)=>{
                event.preventDefault()
             }">
        <el-form-item label="RSS 地址">
          <el-input v-model:model-value="ani.url"></el-input>
        </el-form-item>
      </el-form>
      <div style="display: flex;justify-content: space-between;width: 100%;margin-top: 10px;">
        <el-button @click="mikan?.show">Mikan</el-button>
        <el-button :loading="rssButtonLoading" @click="getRss">确定</el-button>
      </div>
    </div>
    <div v-else>
      <el-form label-width="auto"
               @submit="(event)=>{
                event.preventDefault()
             }">
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
          <el-button :loading="addAniButtonLoading" @click="addAni">确定</el-button>
        </div>
      </el-form>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import api from "./api.js";
import Mikan from "./Mikan.vue";

const showRss = ref(true)
const mikan = ref()

const addDialogVisible = ref(false)

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

const rssButtonLoading = ref(false)

const getRss = () => {
  rssButtonLoading.value = true
  api.post('/api/rss', ani.value)
      .then(res => {
        rssButtonLoading.value = false
        if (res.code !== 200) {
          ElMessage.error(res.message)
          return
        }
        ani.value = res.data
        showRss.value = false
      })
}


const addAniButtonLoading = ref(false)

const addAni = () => {
  addAniButtonLoading.value = true
  api.post('/api/ani', ani.value)
      .then(res => {
        addAniButtonLoading.value = false
        if (res.code !== 200) {
          ElMessage.error(res.message)
          emit('load')
          return
        }
        ElMessage.success(res.message)
        emit('load')
        addDialogVisible.value = false
      })
}

const showAdd = () => {
  ani.value = {
    'url': '',
    'season': 1,
    'offset': 0,
    'title': '',
    'exclude': []
  }
  showRss.value = true
  addDialogVisible.value = true
  excludeVisible.value = false
  excludeValue.value = ''
  addAniButtonLoading.value = false
  rssButtonLoading.value = false
}

defineExpose({showAdd})
const emit = defineEmits(['load'])

</script>

