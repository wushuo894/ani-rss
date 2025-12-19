<template>
  <div>
    <div class="artplayer-app"></div>
    <div>
      <el-button bg text @click="openUrl(`potplayer://${src}`)">
        <template #icon>
          <img alt="PotPlayer" class="el-icon--left icon" src="../icon/icon-PotPlayer.webp"/>
        </template>
        Pot
      </el-button>
      <el-button bg text @click="openUrl(`vlc://${src}`)">
        <template #icon>
          <img alt="VLC" class="el-icon--left icon" src="../icon/icon-VLC.webp"/>
        </template>
        VLC
      </el-button>
      <el-button bg text @click="openUrl(`iina://weblink?url=${encodeUrl(src)}`)">
        <template #icon>
          <img alt="IINA" class="el-icon--left icon" src="../icon/icon-IINA.webp"/>
        </template>
        IINA
      </el-button>
      <el-button bg text @click="openUrl(`intent:${src}`)">
        <template #icon>
          <img alt="MXPlayer" class="el-icon--left icon" src="../icon/icon-MXPlayer.webp"/>
        </template>
        MX
      </el-button>
      <el-button bg text @click="openUrl(`mpvplay://${src}`)">
        <template #icon>
          <img alt="MPV" class="el-icon--left icon" src="../icon/icon-MPV.webp"/>
        </template>
        MPV
      </el-button>
      <el-button bg text @click="openUrl(`ddplay:${encodeUrl(src)}`)">
        <template #icon>
          <img alt="DandanPlay" class="el-icon--left icon" src="../icon/icon-DandanPlay.webp"/>
        </template>
        弹弹Play
      </el-button>
      <el-button bg text @click="openUrl(`anix://openVideo/${encodeUrl(src)}`)">
        <template #icon>
          <img alt="AnimacX" class="el-icon--left icon" src="../icon/icon-AnimacX.webp"/>
        </template>
        AnimacX
      </el-button>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import Artplayer from 'artplayer';
import artplayerPluginMultipleSubtitles from 'artplayer-plugin-multiple-subtitles';

const props = defineProps(['src', 'subtitles'])

let openUrl = (url) => {
  window.open(url)
}

let src = ref('')

// 加密为 Base64
let encodeToBase64 = (str) =>
    btoa(str);

let encodeUrl = (str) =>
    encodeURIComponent(str);

onMounted(() => {
  src.value = location.href + props.src

  let selector = props.subtitles
  let defaultName = ''
  let settings = []
  if (selector.length) {
    selector[0]['default'] = true
    defaultName = selector[0].name
    settings = [
      {
        width: 200,
        html: 'Subtitle',
        tooltip: defaultName,
        selector: selector,
        onSelect: function (item) {
          art.plugins.multipleSubtitles.tracks([item.name]);
          return item.html;
        },
      },
    ]
  }
  const art = new Artplayer({
    container: '.artplayer-app',
    url: props.src,
    theme: '#646cff',
    playbackRate: true,
    aspectRatio: true,
    screenshot: true,
    setting: true,
    pip: true,
    fullscreen: true,
    fullscreenWeb: true,
    airplay: true,
    plugins: [artplayerPluginMultipleSubtitles({
      subtitles: props.subtitles
    })],
    settings: settings,
  });
  art.on('video:canplay', () => {
    if (defaultName) {
      art.plugins.multipleSubtitles.tracks([defaultName]);
    }
  });
})
</script>

<style scoped>
.artplayer-app {
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
