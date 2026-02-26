<template>
  <Bgm ref="bgmRef" @callback="bgmAdd"/>
  <CollectionPreview ref="collectionPreviewRef" v-model:data="data"/>
  <el-dialog v-model="dialogVisible"
             center
             title="添加合集">
    <div v-loading="loading" style="height: 500px;">
      <el-scrollbar style="padding: 0 12px;">
        <div>
          <el-form label-width="auto"
                   @submit="(event)=>{
                event.preventDefault()
             }">
            <el-form-item label="番剧名称">
              <div style="width: 100%;">
                <div class="flex" style="width: 100%;">
                  <el-input
                      v-model:model-value="data.ani.title"
                      :disabled="rssButtonLoading"
                      placeholder="请勿留空"
                      @keyup.enter="bgmRef?.show(data.ani.title)"
                  />
                  <div style="width: 4px;"></div>
                  <el-button :disabled="rssButtonLoading" bg icon="Search" text type="primary"
                             @click="bgmRef?.show(data.ani.title)"/>
                </div>
                <div v-if="data.show" class="change-title-button">
                  <el-button :loading="getBgmNameLoading"
                             bg
                             icon="DocumentAdd" text @click="getBgmName">
                    使用Bangumi
                  </el-button>
                  <el-button :disabled="data.ani.title === data.ani.themoviedbName || !data.ani.themoviedbName.length"
                             bg
                             icon="DocumentAdd"
                             text
                             @click="data.ani.title = data.ani.themoviedbName">
                    使用TMDB
                  </el-button>
                </div>
              </div>
            </el-form-item>
            <template v-if="data.show">
              <el-form-item label="TMDB">
                <div class="flex" style="width: 100%;justify-content: space-between;">
                  <div class="el-input is-disabled">
                    <div class="el-input__wrapper"
                         style="pointer-events: auto;cursor: auto;justify-content: left;padding: 0 11px;"
                         tabindex="-1">
                      <el-link v-if="data.ani?.tmdb?.id"
                               :href="`https://www.themoviedb.org/${data.ani.ova ? 'movie' : 'tv'}/${data.ani.tmdb.id}`"
                               target="_blank"
                               type="primary">
                        {{ data.ani.themoviedbName }}
                      </el-link>
                      <span v-else>{{ data.ani.themoviedbName }}</span>
                    </div>
                  </div>
                  <div style="width: 4px;"></div>
                  <el-button :loading="getThemoviedbNameLoading" bg icon="Refresh" text @click="getThemoviedbName"/>
                </div>
              </el-form-item>
              <el-form-item label="字幕组">
                <div class="form-item-flex">
                  <el-input v-model:model-value="data.ani.subgroup" placeholder="字幕组" style="width: 150px"/>
                </div>
              </el-form-item>
              <el-form-item label="季">
                <div class="form-item-flex">
                  <el-input-number v-model:model-value="data.ani.season" :min="0" style="max-width: 200px"/>
                </div>
              </el-form-item>
              <el-form-item label="集数偏移">
                <div class="form-item-flex">
                  <el-input-number v-model:model-value="data.ani.offset"/>
                </div>
              </el-form-item>
              <el-form-item label="日期">
                <div class="form-item-flex">
                  <el-date-picker
                      v-model="date"
                      style="max-width: 150px;"
                      @change="dateChange"
                  />
                </div>
              </el-form-item>
              <el-form-item label="匹配">
                <Exclude ref="match" v-model:exclude="data.ani.match" :import-exclude="false"/>
              </el-form-item>
              <el-form-item label="排除">
                <Exclude ref="exclude" v-model:exclude="data.ani.exclude" :import-exclude="true"/>
              </el-form-item>
              <el-form-item label="全局排除">
                <el-switch v-model:model-value="data.ani['globalExclude']"/>
              </el-form-item>
              <el-form-item label="剧场版">
                <el-switch v-model:model-value="data.ani.ova"/>
              </el-form-item>
              <el-form-item label="自定义集数规则">
                <div style="display: flex;width: 100%;">
                  <el-input v-model:model-value="data.ani.customEpisodeStr"
                            style="width: 100%"/>
                  <div style="width: 4px;"></div>
                  <el-input-number v-model:model-value="data.ani.customEpisodeGroupIndex"/>
                </div>
              </el-form-item>
              <el-form-item label="下载位置">
                <div style="width: 100%;">
                  <el-input v-model:model-value="data.ani.downloadPath" :autosize="{ minRows: 2}"
                            style="width: 100%"
                            type="textarea"/>
                </div>
                <div style="margin-top: 6px;">
                  <el-button :disabled="!data.ani.customDownloadPath"
                             :loading="downloadPathLoading" bg icon="Refresh"
                             text
                             @click="downloadPath"/>
                </div>
              </el-form-item>
              <el-form-item label="自定义标签">
                <custom-tags :config="data.ani"/>
              </el-form-item>
              <el-form-item label="Torrent">
                <el-tag v-if="data.filename" closable @close="()=>{
                data.filename = ''
                data.torrent = ''
              }">
                  <el-tooltip :content="data.filename">
                    <el-text line-clamp="1" size="small" class="filename">
                      {{ data.filename }}
                    </el-text>
                  </el-tooltip>
                </el-tag>
                <el-upload
                    v-else
                    :action="`api/upload?type=getBase64&s=${authorization}`"
                    :before-upload="beforeAvatarUpload"
                    :on-success="onSuccess"
                    :show-file-list="false"
                    class="upload-demo"
                    drag
                    multiple
                    style="width: 100%"
                >
                  <el-icon class="el-icon--upload">
                    <upload-filled/>
                  </el-icon>
                  <div class="el-upload__text">
                    在这里拖放 .torrent 文件或<em>点击上传</em>
                  </div>
                  <template #tip>
                    <div class="el-upload__tip flex" style="justify-content: end;">
                      .torrent 文件小于 5M
                    </div>
                  </template>
                </el-upload>
              </el-form-item>
            </template>
          </el-form>
        </div>
      </el-scrollbar>
    </div>
    <div class="action">
      <el-button :disabled="!data.filename" bg
                 icon="Grid"
                 text
                 @click="collectionPreviewRef?.show">
        预览
      </el-button>
      <el-button :disabled="!data.filename"
                 :loading="startLoading"
                 bg
                 icon="Check"
                 text
                 type="primary" @click="start">
        开始
      </el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref} from "vue";
import {UploadFilled} from "@element-plus/icons-vue";
import {ElMessage, ElMessageBox} from "element-plus";
import Bgm from "./Bgm.vue";
import Exclude from "@/config/Exclude.vue";
import CollectionPreview from "./CollectionPreview.vue";
import CustomTags from "@/config/CustomTags.vue";
import {aniData} from "@/js/ani.js";
import {authorization} from "@/js/global.js";
import * as http from "@/js/http.js";
import {getBgmTitle} from "@/js/http.js";

let start = () => {
  startLoading.value = true
  http.startCollection(data.value)
      .then((res) => {
        ElMessageBox.confirm(
            res.message,
            'success',
            {
              confirmButtonText: 'OK',
              confirmButtonClass: 'is-text is-has-bg el-button--primary',
              type: 'success',
              center: true,
              showCancelButton: false
            }
        )
      })
      .finally(() => {
        startLoading.value = false
      })
}

let downloadPathLoading = ref(false)
let downloadPath = () => {
  downloadPathLoading.value = true
  let newAni = JSON.parse(JSON.stringify(data.value.ani))
  newAni.customDownloadPath = false
  http.downloadPath(newAni)
      .then(res => {
        data.value.ani.downloadPath = res.data.downloadPath
      })
      .finally(() => {
        downloadPathLoading.value = false
      })
}

let collectionPreviewRef = ref()

let startLoading = ref(false);

let date = ref()

let dateChange = () => {
  if (!date.value) {
    return
  }
  data.value.ani.year = date.value.getFullYear()
  data.value.ani.month = date.value.getMonth() + 1
  data.value.ani.date = date.value.getDate()
  let minYear = 1970
  if (data.value.ani.year < minYear) {
    data.value.ani.year = minYear
    init()
    ElMessage.error(`最小年份为 ${minYear}`)
  }
}

let init = () => {
  date.value = new Date(data.value.ani.year, data.value.ani.month - 1, data.value.ani.date);
}

let bgmRef = ref()

let rssButtonLoading = ref(false)
let loading = ref(false)

let bgmAdd = (bgm) => {
  loading.value = true
  data.value.show = false
  data.value.torrent = ''
  data.value.filename = ''
  http.getAniBySubjectId(bgm['id'])
      .then((res) => {
        data.value.ani = res.data
        data.value.ani.subgroup = '未知字幕组'
        data.value.ani.customEpisode = true
        data.value.show = true
        data.value.ani.match = []
        data.value.ani.exclude = ['^(SPs?|CDs|Scans|PV|menu)/', 'Fonts|NCED|NCOP|迷你动画']
      })
      .finally(() => {
        loading.value = false
      })
}

let onSuccess = (res) => {
  data.value.torrent = res.data
  // 获取字幕组
  http.getCollectionSubgroup(data.value)
      .then(res => {
        data.value.ani.subgroup = res.data
        if (res.data !== '未知字幕组') {
          ElMessage.success(`字幕组已更新为 ${res.data}`)
        }
      })
}

let data = ref({
  filename: '',
  torrent: '',
  ani: aniData,
  show: false,
})

let beforeAvatarUpload = (rawFile) => {
  data.value.filename = rawFile.name
  if (!rawFile.name.includes('.torrent')) {
    ElMessage.error('Avatar picture must be .torrent format!')
    return false
  }
  if (rawFile.size / 1024 / 1024 > 10) {
    ElMessage.error('Avatar picture size can not exceed 10MB!')
    return false
  }
  return true
}

let dialogVisible = ref(false)

let show = () => {
  init()
  data.value.show = false
  data.value.ani.title = ''
  data.value.torrent = ''
  data.value.filename = ''
  dialogVisible.value = true
}

let getBgmNameLoading = ref(false)

let getBgmName = () => {
  getBgmNameLoading.value = true
  getBgmTitle(data.value.ani)
      .then(res => {
        data.value.ani.title = res.data
      })
      .finally(() => {
        getBgmNameLoading.value = false
      })
}

let getThemoviedbNameLoading = ref(false)

let getThemoviedbName = () => {
  if (!data.value.ani.title.length) {
    return
  }

  getThemoviedbNameLoading.value = true
  http.getThemoviedbName(data.value.ani)
      .then(res => {
        ElMessage.success(res.message)
        data.value.ani['themoviedbName'] = res.data['themoviedbName']
        data.value.ani['tmdb'] = res.data['tmdb']
      })
      .finally(() => {
        getThemoviedbNameLoading.value = false
      })
}


defineExpose({show})

</script>

<style scoped>
.change-title-button {
  width: 100%;
  justify-content: end;
  display: flex;
  margin-top: 12px;
}

.form-item-flex {
  width: 100%;
  display: flex;
  justify-content: end;
}

.filename {
  max-width: 300px;
  color: var(--el-color-info);
}

.action {
  width: 100%;
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}
</style>
