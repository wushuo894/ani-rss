<template>
  <div>
    <div class="artplayer-app"></div>
    <div class="flex" style="margin-top: 8px;width: 100%;justify-content: end;">
      <el-button bg text @click="openUrl(`potplayer://${src}`)">
        <template #icon>
          <img alt="PotPlayer" class="el-icon--left" height="20" src="../icon/icon-PotPlayer.webp" width="20"/>
        </template>
        Pot
      </el-button>
      <el-button bg text @click="openUrl(`vlc://${src}`)">
        <template #icon>
          <img alt="VLC" class="el-icon--left" height="20" src="../icon/icon-VLC.webp" width="20"/>
        </template>
        VLC
      </el-button>
      <el-button bg text @click="openUrl(`iina://weblink?url=${src}`)">
        <template #icon>
          <img alt="IINA" class="el-icon--left" height="20" src="../icon/icon-IINA.webp" width="20"/>
        </template>
        IINA
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
  console.log('openUrl', url)
  window.open(url)
}

let src = ref('')

onMounted(() => {
  src.value = encodeURI(location.href + props.src)

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

<style>
.artplayer-app {
  width: 700px;
  height: 450px;
  max-width: calc(100vw - 48px);
  max-height: calc(56.25vw - 27px);
}
</style>
