<template>
  <div>
    <div class="art-app"></div>
    <div class="flex" style="justify-content: end;">
      <el-dropdown>
        <el-button bg text icon="MoreFilled"/>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="openUrl(`potplayer://${playItem.src}`)">
              <el-text>
                <el-icon>
                  <img alt="PotPlayer" class="el-icon--left icon" src="../icon/icon-PotPlayer.webp"/>
                </el-icon>
                Pot
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item @click="openUrl(`vlc://${playItem.src}`)">
              <el-text>
                <el-icon>
                  <img alt="VLC" class="el-icon--left icon" src="../icon/icon-VLC.webp"/>
                </el-icon>
                VLC
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item @click="openUrl(`iina://weblink?url=${encodeUrl(playItem.src)}`)">
              <el-text>
                <el-icon>
                  <img alt="IINA" class="el-icon--left icon" src="../icon/icon-IINA.webp"/>
                </el-icon>
                IINA
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item @click="openUrl(`mpvplay://${playItem.src}`)">
              <el-text>
                <el-icon>
                  <img alt="MPV" class="el-icon--left icon" src="../icon/icon-MPV.webp"/>
                </el-icon>
                MPV
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item @click="openUrl(`ddplay:${encodeUrl(playItem.src)}|filePath=${playItem.name}`)">
              <el-text>
                <el-icon>
                  <img alt="DandanPlay" class="el-icon--left icon" src="../icon/icon-DandanPlay.webp"/>
                </el-icon>
                弹弹Play
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item @click="openUrl(`anix://openVideo/${encodeUrl(playItem.src)}`)">
              <el-text>
                <el-icon>
                  <img alt="AnimacX" class="el-icon--left icon" src="../icon/icon-AnimacX.webp"/>
                </el-icon>
                AnimacX
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item
                @click="openUrl(`SenPlayer://x-callback-url/play?url=${playItem.src}&name=${playItem.name}`)">
              <el-text>
                <el-icon>
                  <img alt="SenPlayer" class="el-icon--left icon" src="../icon/icon-SenPlayer.webp"/>
                </el-icon>
                SenPlayer
              </el-text>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import {onBeforeUnmount, onMounted} from 'vue'
import Artplayer from 'artplayer';
import artplayerPluginMultipleSubtitles from 'artplayer-plugin-multiple-subtitles';

const props = defineProps(['playItem'])

let openUrl = (url) => {
  console.log(url);
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
  height: 14px;
  width: 14px;
}
</style>
