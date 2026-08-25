<template>
  <div class="cover-card">
    <div class="cover-image-container"
         :class="{'is-disabled': !item.enable}"
         @click="emit('cover', item)">
      <img v-if="item.cover"
           :src="toApiFile(item.cover)"
           :alt="item.title"
           class="cover-image">
      <div v-else class="cover-image cover-empty">
        <el-icon>
          <Picture/>
        </el-icon>
      </div>
      <button v-if="scoreText" class="cover-score" type="button" @click.stop="emit('rate', item)">
        <span>{{ scoreText }}</span>
      </button>
      <div v-if="!item.enable" class="cover-disabled-label">
        未启用
      </div>
      <div class="cover-overlay">
        <el-tooltip :content="item.title" placement="top">
          <el-text class="cover-title" line-clamp="2" @click.stop="openBgmUrl(item)">
            {{ item.title }}
          </el-text>
        </el-tooltip>
        <div class="cover-meta">
          <div class="cover-meta-line">
            <span class="cover-meta-fixed">{{ episodeText }}</span>
            <el-tooltip :content="subgroupText" placement="top">
              <span class="cover-subgroup">{{ subgroupText }}</span>
            </el-tooltip>
          </div>
          <div v-if="showLastDownloadTime || hasStandbyRss" class="cover-meta-line cover-meta-secondary">
            <span v-if="showLastDownloadTime" class="cover-meta-fixed">{{ updateText }}</span>
            <span v-if="hasStandbyRss" class="cover-meta-fixed">备用RSS</span>
          </div>
        </div>
      </div>
      <div class="cover-actions" :class="{'is-open': actionsVisible}">
        <el-dropdown trigger="click"
                     placement="top-end"
                     @visible-change="actionsVisible = $event">
          <el-button class="cover-action-button" bg circle text @click.stop>
            <el-icon>
              <Fold/>
            </el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="showPlaylist" @click="emit('playlist', item)">
                <el-icon>
                  <Files/>
                </el-icon>
                播放列表
              </el-dropdown-item>
              <el-dropdown-item @click="emit('cover', item)">
                <el-icon>
                  <Picture/>
                </el-icon>
                封面
              </el-dropdown-item>
              <el-dropdown-item v-if="showScore" @click="emit('rate', item)">
                <el-icon>
                  <Star/>
                </el-icon>
                评分
              </el-dropdown-item>
              <el-dropdown-item @click="emit('edit', item)">
                <el-icon>
                  <EditIcon/>
                </el-icon>
                编辑
              </el-dropdown-item>
              <el-dropdown-item divided @click="emit('del', [item])">
                <el-text type="danger">
                  <el-icon>
                    <Delete/>
                  </el-icon>
                  删除
                </el-text>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, ref} from "vue";
import {Delete, Edit as EditIcon, Files, Fold, Picture, Star} from "@element-plus/icons-vue";
import {showLastDownloadTime, showPlaylist, showScore, toApiFile} from "@/js/global.js";
import {fromNow} from "@/js/format.js";

const actionsVisible = ref(false)
const props = defineProps(["item"])

const hasStandbyRss = computed(() => (props.item.standbyRssList || []).length > 0)
const subgroupText = computed(() => props.item.subgroup || '未知字幕组')
const scoreText = computed(() => {
  if (!showScore.value) {
    return ''
  }
  const score = Number(props.item.score)
  return Number.isFinite(score) && score > 0 ? score.toFixed(1) : ''
})
const episodeText = computed(() => {
  const current = props.item.currentEpisodeNumber || 0
  const total = props.item.totalEpisodeNumber || '*'
  return `${current}/${total}`
})

const updateText = computed(() => {
  let {lastDownloadTime, releaseDate} = props.item;
  if (lastDownloadTime > 0) {
    return fromNow(lastDownloadTime)
  }
  if (releaseDate) {
    return fromNow(releaseDate)
  }
  return '未更新'
})

const openBgmUrl = it => {
  if (it.bgmUrl?.length) {
    window.open(it.bgmUrl, '_blank', 'noopener')
    return
  }
  if (it.title?.length) {
    let title = it.title.replace(/ ?\((19|20)\d{2}\)/g, "").trim()
    title = title.replace(/ ?\[tmdbid=(\d+)]/g, "").trim()
    window.open(`https://bgm.tv/subject_search/${encodeURIComponent(title)}?cat=2`, '_blank', 'noopener')
  }
}

const emit = defineEmits(['edit', 'playlist', 'cover', 'del', 'rate'])
</script>

<style scoped>
.cover-card {
  min-width: 0;
}

.cover-image-container {
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 3;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background-color: var(--el-fill-color-light);
}

.cover-image-container:after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(
      180deg,
      rgba(0, 0, 0, 0.04) 38%,
      rgba(0, 0, 0, 0.36) 68%,
      rgba(0, 0, 0, 0.78) 100%
  );
}

.cover-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  cursor: pointer;
  transition: filter 0.18s ease, transform 0.18s ease;
}

.cover-image-container:not(.is-disabled):hover .cover-image {
  transform: scale(1.03);
}

.cover-image-container.is-disabled .cover-image {
  filter: grayscale(1) brightness(0.58);
}

.cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  font-size: 28px;
}

.cover-score {
  position: absolute;
  right: 8px;
  top: 8px;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 20px;
  padding: 0 7px;
  border: 0;
  border-radius: 999px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  cursor: pointer;
  background-color: var(--el-color-primary);
}

.cover-disabled-label {
  position: absolute;
  left: 8px;
  top: 8px;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  color: #ffffff;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  background-color: rgba(96, 98, 102, 0.92);
  white-space: nowrap;
}

.cover-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1;
  min-height: 92px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 42px 8px 8px;
  box-sizing: border-box;
}

.cover-actions {
  position: absolute;
  right: 6px;
  bottom: 6px;
  z-index: 3;
  opacity: 0;
  transform: translateY(4px);
  pointer-events: none;
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.cover-card:hover .cover-actions,
.cover-actions.is-open {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.cover-action-button {
  width: 30px;
  height: 30px;
  color: var(--el-text-color-primary);
  background-color: var(--el-bg-color-overlay);
  box-shadow: var(--el-box-shadow-light);
}

.cover-title {
  width: 100%;
  display: -webkit-box;
  align-self: flex-start;
  text-align: left;
  line-height: 1.35;
  font-weight: 600;
  cursor: pointer;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.7);
}

.cover-meta {
  display: grid;
  gap: 2px;
  margin-top: 5px;
}

.cover-meta-line {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 8px;
  max-width: 100%;
  color: rgba(255, 255, 255, 0.86);
  font-size: 12px;
  line-height: 1.4;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.7);
}

.cover-meta-secondary {
  padding-right: 34px;
}

.cover-meta-fixed {
  flex-shrink: 0;
}

.cover-subgroup {
  flex: 1 1 auto;
  min-width: 0;
  overflow-wrap: anywhere;
}

@media (hover: none), (max-width: 800px) {
  .cover-actions {
    opacity: 1;
    transform: translateY(0);
    pointer-events: auto;
  }
}
</style>
