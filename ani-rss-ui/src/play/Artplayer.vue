<template>
  <div>
    <div class="art-app"></div>
    <div>
      <el-button bg text @click="openUrl(`potplayer://${playItem.src}`)">
        <template #icon>
          <img alt="PotPlayer" class="el-icon--left icon" src="../icon/icon-PotPlayer.webp"/>
        </template>
        Pot
      </el-button>
      <el-button bg text @click="openUrl(`vlc://${playItem.src}`)">
        <template #icon>
          <img alt="VLC" class="el-icon--left icon" src="../icon/icon-VLC.webp"/>
        </template>
        VLC
      </el-button>
      <el-button bg text @click="openUrl(`iina://weblink?url=${encodeUrl(playItem.src)}`)">
        <template #icon>
          <img alt="IINA" class="el-icon--left icon" src="../icon/icon-IINA.webp"/>
        </template>
        IINA
      </el-button>
      <el-button bg text @click="openUrl(`intent:${playItem.src}`)">
        <template #icon>
          <img alt="MXPlayer" class="el-icon--left icon" src="../icon/icon-MXPlayer.webp"/>
        </template>
        MX
      </el-button>
      <el-button bg text @click="openUrl(`mpvplay://${playItem.src}`)">
        <template #icon>
          <img alt="MPV" class="el-icon--left icon" src="../icon/icon-MPV.webp"/>
        </template>
        MPV
      </el-button>
      <el-button bg text @click="openUrl(`ddplay:${encodeUrl(playItem.src)}`)">
        <template #icon>
          <img alt="DandanPlay" class="el-icon--left icon" src="../icon/icon-DandanPlay.webp"/>
        </template>
        弹弹Play
      </el-button>
      <el-button bg text @click="openUrl(`anix://openVideo/${encodeUrl(playItem.src)}`)">
        <template #icon>
          <img alt="AnimacX" class="el-icon--left icon" src="../icon/icon-AnimacX.webp"/>
        </template>
        AnimacX
      </el-button>
    </div>
  </div>
</template>

<script setup>
import {onBeforeUnmount, onMounted} from 'vue'
import Artplayer from 'artplayer';
import artplayerPluginMultipleSubtitles from 'artplayer-plugin-multiple-subtitles';

const props = defineProps(['playItem'])

let openUrl = (url) => {
  window.open(url)
}

// 加密为 Base64
let encodeToBase64 = (str) => {
  return btoa(str);
}

let encodeUrl = (str) => {
  return encodeURIComponent(str);
}

let art = null

onMounted(() => {
  let {src, subtitles, extName} = props['playItem'];
  let defaultName = ''
  let settings = []
  if (subtitles.length) {
    subtitles[0]['default'] = true
    defaultName = subtitles[0].name
    settings = [
      {
        width: 200,
        html: 'Subtitle',
        tooltip: defaultName,
        selector: subtitles,
        onSelect: function (item) {
          art.plugins['multipleSubtitles'].tracks([item.name]);
          return item.html;
        },
      },
    ]
  }
  art = new Artplayer({
    container: '.art-app',
    url: src,
    type: extName,
    theme: '#646cff',
    playbackRate: true,
    aspectRatio: true,
    screenshot: true,
    setting: true,
    pip: true,
    fullscreen: true,
    fullscreenWeb: true,
    airplay: true,
    preload: true,
    plugins: [
      artplayerPluginMultipleSubtitles({
        subtitles: subtitles
      })
    ],
    settings: settings
  });
  art.on('video:canplay', () => {
    if (defaultName) {
      art.plugins['multipleSubtitles'].tracks([defaultName]);
    }
  });
})

onBeforeUnmount(() => {
  if (!art) {
    return
  }
  try {
    art.destroy(true);
    art = null;
  } catch (e) {
  }
})
</script>

<style scoped>
.art-app {
  width: 700px;
  height: 450px;
  max-width: calc(100vw - 48px);
  max-height: calc(56.25vw - 27px);
  margin-bottom: 8px;
}

.icon {
  height: 20px;
  width: 20px;
}
</style>
