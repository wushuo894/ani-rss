<template>
  <el-dialog title="添加正则" v-if="add" v-model:model-value="add" center align-center width="300">
    <div>
      <SettingsItem label="字幕组" :label-width="50">
        <el-input placeholder="留空匹配所有字幕组" v-model="subgroup"></el-input>
      </SettingsItem>
      <SettingsItem label="正则" :label-width="50">
        <el-input placeholder="如 720、简、\d-\d" v-model="exclude"></el-input>
      </SettingsItem>
    </div>
    <div class="flex exclude-dialog-footer">
      <el-button bg text @click="addExclude" icon="Plus">添加</el-button>
    </div>
  </el-dialog>
  <div class="full-width">
    <div class="tags-content">
      <el-tag v-if="!props.exclude.length"
              type="info">
        无
      </el-tag>
      <el-tag
          v-for="tag in props.exclude"
          :key="tag"
          closable
          :disable-transitions="false"
          @close="handleClose(tag)"
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
                 :disabled="disabledImportExclude" :loading="importExcludeLoading" icon="Download">
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
import {ElMessage} from "element-plus";
import {config} from "@/js/http.js";
import SettingsItem from "@/view/custom/SettingsItem.vue";

const handleClose = (tag) => {
  props.exclude.splice(props.exclude.indexOf(tag), 1)
}

const add = ref(false)

let importExcludeLoading = ref(false)
let disabledImportExclude = ref(false)

let importExclude = () => {
  importExcludeLoading.value = true
  config()
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

let props = defineProps({
  exclude: Array,
  importExclude: Boolean,
  showText: Boolean
})
</script>

<style scoped>
.exclude-dialog-footer {
  width: 100%;
  justify-content: end;
  margin-top: 8px;
}

.exclude-tag-text {
  max-width: 300px;
  color: var(--el-color-primary);
}

.exclude-footer {
  margin-top: 4px;
  width: 100%;
  justify-content: space-between;
}

.exclude-link {
  font-size: var(--el-font-size-extra-small);
}

.tags-content {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;

  .el-button {
    margin: 0;
  }
}
</style>
