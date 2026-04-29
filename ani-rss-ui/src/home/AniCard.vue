<template>
  <Edit ref="editRef"/>
  <PlayList ref="playListRef"/>
  <Cover ref="coverRef"/>
  <Del ref="delRef"/>
  <BgmRate ref="bgmRateRef"/>
  <el-card shadow="never">
    <div class="list-card-content">
      <div class="list-card-image-container">
        <img :src="`api/file?filename=${item['cover']}&s=${authorization}`" height="130" width="92"
             :alt="item.title"
             class="list-card-image"
             @click="coverRef?.show(item)"/>
      </div>
      <div class="list-card-info">
        <div class="list-card-info-inner">
          <div class="flex">
            <el-tooltip :content="item.title" placement="top">
              <el-text :line-clamp="1"
                       @click="openBgmUrl(item)"
                       class="list-card-title"
                       truncated>
                {{ item.title }}
              </el-text>
            </el-tooltip>
          </div>
          <div class="list-card-score-container" v-if="showScore">
            <h4 class="list-card-score" @click="bgmRateRef?.show(item)">
              {{ item['score'].toFixed(1) }}
            </h4>
          </div>
          <el-text v-else
                   line-clamp="2"
                   size="small"
                   class="list-card-url">
            {{ decodeURLComponentSafe(item.url) }}
          </el-text>
          <div class="list-card-tags"
               :class="isNotMobile ? 'gtc3' : 'gtc2'"
          >
            <el-tag>
              第 {{ item.season }} 季
            </el-tag>
            <el-tag type="success" v-if="item.enable">
              已启用
            </el-tag>
            <el-tag type="info" v-else>
              未启用
            </el-tag>
            <el-tag type="info">
              <el-tooltip :content="item['subgroup']">
                <el-text line-clamp="1" size="small" class="list-card-subgroup">
                  {{ item['subgroup'] ? item['subgroup'] : '未知字幕组' }}
                </el-text>
              </el-tooltip>
            </el-tag>
            <el-tag type="warning">
              {{ item['currentEpisodeNumber'] }} /
              {{ item['totalEpisodeNumber'] ? item['totalEpisodeNumber'] : '*' }}
            </el-tag>
            <el-tag type="danger" v-if="item.ova">
              ova
            </el-tag>
            <el-tag type="danger" v-else>
              tv
            </el-tag>
            <el-tag v-if="item.standbyRssList.length > 0">
              备用RSS
            </el-tag>
          </div>
          <el-text v-if="showLastDownloadTime && item.lastDownloadFormat" size="small"
                   type="info">
            {{ item.lastDownloadFormat }}
          </el-text>
        </div>
        <div class="list-card-actions">
          <el-button text @click="playListRef?.show(item)" bg v-if="showPlaylist">
            <el-icon>
              <Files/>
            </el-icon>
          </el-button>
          <div class="list-card-spacer" v-if="showPlaylist"></div>
          <el-button bg text @click="editRef?.show(item)">
            <el-icon>
              <EditIcon/>
            </el-icon>
          </el-button>
          <div class="list-card-spacer"></div>
          <el-button type="danger" text @click="delRef?.show([item])" bg>
            <el-icon>
              <Delete/>
            </el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import {authorization, isNotMobile, showLastDownloadTime, showPlaylist, showScore} from "@/js/global.js";
import {Delete, Edit as EditIcon, Files} from "@element-plus/icons-vue";
import Cover from "@/home/Cover.vue";
import BgmRate from "@/home/BgmRate.vue";
import Edit from "@/home/Edit.vue";
import PlayList from "@/play/PlayList.vue";
import Del from "@/home/Del.vue";
import {ref} from "vue";

const editRef = ref()
const delRef = ref()
const coverRef = ref()
const playListRef = ref()
const bgmRateRef = ref()

let openBgmUrl = (it) => {
  if (it.bgmUrl.length) {
    window.open(it.bgmUrl)
    return
  }
  if (it.title.length) {
    let title = it.title.replace(/ ?\((19|20)\d{2}\)/g, "").trim()
    title = title.replace(/ ?\[tmdbid=(\d+)]/g, "").trim()
    window.open(`https://bgm.tv/subject_search/${title}?cat=2`)
  }
}

let decodeURLComponentSafe = (str) => {
  return decodeURIComponent(str.replace('+', ' '));
}

let props = defineProps(["item"])
</script>

<style scoped>
.list-card-content {
  display: flex;
  width: 100%;
  align-items: center;
}

.list-card-image-container {
  height: 100%;
}

.list-card-image {
  border-radius: var(--el-border-radius-base);
  cursor: pointer;
}

.list-card-info {
  flex-grow: 1;
  position: relative;
}

.list-card-info-inner {
  margin-left: 8px;
}

.list-card-title {
  width: 200px;
  line-height: 1.6;
  letter-spacing: 0.0125em;
  font-weight: 500;
  font-size: 0.97em;
  cursor: pointer;
  color: var(--el-text-color-primary);
}

.list-card-score-container {
  margin-bottom: 8px;
}

.list-card-score {
  color: #E800A4;
  cursor: pointer;
}

.list-card-url {
  max-width: 300px;
}

.list-card-tags {
  width: 180px;
  display: grid;
  grid-gap: 4px;
}

.list-card-subgroup {
  max-width: 60px;
  color: var(--el-color-info);
}

.list-card-actions {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  flex-direction: column;
  position: absolute;
  right: 0;
  bottom: 0;
}

.list-card-spacer {
  height: 5px;
}

.gtc3 {
  grid-template-columns: repeat(3, 1fr);
}

.gtc2 {
  grid-template-columns: repeat(2, 1fr);
}
</style>