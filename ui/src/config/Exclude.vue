<template>
  <el-dialog title="添加正则" v-if="add" v-model:model-value="add" center align-center width="300">
    <el-form label-width="auto">
      <el-form-item label="字幕组">
        <el-input placeholder="留空匹配所有字幕组" v-model="subgroup"></el-input>
      </el-form-item>
      <div class="exclude-spacer"></div>
      <el-form-item label="正则">
        <el-input placeholder="如 720、简、\d-\d" v-model="exclude"></el-input>
      </el-form-item>
    </el-form>
    <div class="flex exclude-dialog-footer">
      <el-button bg text @click="addExclude" icon="Plus">添加</el-button>
    </div>
  </el-dialog>
  <div class="exclude-container">
    <div class="gap-2">
      <el-tag v-if="!props.exclude.length"
              type="info"
              class="exclude-tag">
        无
      </el-tag>
      <el-tag
          v-for="tag in props.exclude"
          :key="tag"
          closable
          :disable-transitions="false"
          @close="handleClose(tag)"
          class="exclude-tag"
      >
        <el-tooltip :content="tag">
          <el-text line-clamp="1" size="small" class="exclude-tag-text">
            {{ tag }}
          </el-text>
        </el-tooltip>
      </el-tag>
      <el-button bg
                 icon="Plus"
                 size="small"
                 class="exclude-tag"
                 text
                 @click="()=> add = true"
      />
      <el-button
          v-if="props.exclude.length"
          bg
          icon="Delete"
          size="small"
          class="exclude-delete-button"
          text
          type="danger"
          @click="() => props.exclude.length = 0"
      />
    </div>
    <div class="flex exclude-footer">
      <el-button bg text size="small" @click="importExclude" v-if="props.importExclude"
                 :disabled="disabledImportExclude" :loading="importExcludeLoading">
        <el-icon>
          <Download/>
        </el-icon>
        导入全局排除
      </el-button>
      <el-text class="mx-1" size="small" v-if="props.showText">
        支持&nbsp;
        <el-link
            class="exclude-link"
            type="primary"
            href="https://www.runoob.com/regexp/regexp-syntax.html"
            target="_blank">
          正则表达式
        </el-link>
      </el-text>
    </div>
  </div>
</template>

<script setup>
import {ref} from "vue";
import api from "@/js/api.js";
import {ElMessage} from "element-plus";

const excludeValue = ref('')

const handleClose = (tag) => {
  props.exclude.splice(props.exclude.indexOf(tag), 1)
}

const add = ref(false)
const InputRef = ref()

const handleInputConfirm = () => {
  if (excludeValue.value) {
    props.exclude.push(excludeValue.value)
  }
  excludeValue.value = ''
}

let importExcludeLoading = ref(false)
let disabledImportExclude = ref(false)

let importExclude = () => {
  importExcludeLoading.value = true
  api.get("api/config")
      .then(res => {
        disabledImportExclude.value = true
        for (let it of res.data.exclude) {
          if (props.exclude.indexOf(it) > -1) {
            continue
          }
          props.exclude.push(it)
        }
      })
      .finally(() => {
        importExcludeLoading.value = false
      })

}

let subgroup = ref('')
let exclude = ref('')

let addExclude = () => {
  if (!exclude.value) {
    ElMessage.error('正则为空')
    return
  }
  if (subgroup.value) {
    exclude.value = `{{${subgroup.value}}}:${exclude.value}`
  }
  props.exclude.push(exclude.value)
  subgroup.value = ''
  exclude.value = ''
  add.value = false
}

let props = defineProps(['exclude', 'importExclude', 'showText'])
</script>

<style scoped>
.exclude-spacer {
  margin: 4px;
}

.exclude-dialog-footer {
  width: 100%;
  justify-content: end;
  margin-top: 8px;
}

.exclude-container {
  width: 100%;
}

.exclude-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.exclude-tag-text {
  max-width: 300px;
  color: var(--el-color-primary);
}

.exclude-delete-button {
  margin-left: 0;
  margin-bottom: 4px;
}

.exclude-footer {
  margin-top: 4px;
  width: 100%;
  justify-content: space-between;
}

.exclude-link {
  font-size: var(--el-font-size-extra-small);
}
</style>
