<template>
  <div>
    <movi-player
        class="movi-app"
        :src="playItem.src"
        :controls="true"/>
    <div class="flex" style="justify-content: end;">
      <el-dropdown>
        <el-button bg text icon="MoreFilled"/>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="openUrl(`potplayer://${playItem.src}`)">
              <el-text>
                <el-icon>
                  <img alt="PotPlayer" class="el-icon--left icon" src="@/icon/icon-PotPlayer.webp"/>
                </el-icon>
                Pot
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item @click="openUrl(`vlc://${playItem.src}`)">
              <el-text>
                <el-icon>
                  <img alt="VLC" class="el-icon--left icon" src="@/icon/icon-VLC.webp"/>
                </el-icon>
                VLC
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item
                @click="openUrl(`iina://weblink?url=${encodeUrl(playItem.src)}&mpv_force-media-title=${playItem.name}`)">
              <el-text>
                <el-icon>
                  <img alt="IINA" class="el-icon--left icon" src="@/icon/icon-IINA.webp"/>
                </el-icon>
                IINA
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item @click="openUrl(`mpvplay://${playItem.src}&mpv_force-media-title=${playItem.name}`)">
              <el-text>
                <el-icon>
                  <img alt="MPV" class="el-icon--left icon" src="@/icon/icon-MPV.webp"/>
                </el-icon>
                MPV
              </el-text>
            </el-dropdown-item>
            <el-dropdown-item
                @click="openUrl(`infuse://x-callback-url/play?url=${encodeUrl(playItem.src)}&filename=${playItem.name}`)">
              <el-text>
                <el-icon>
                  <img alt="Infuse" class="el-icon--left icon" src="@/icon/icon-Infuse.png"/>
                </el-icon>
                Infuse
              </el-text>
            </el-dropdown-item>
          </el-dropdown-menu>
          <el-dropdown-item @click="openUrl(`ddplay:${encodeUrl(playItem.src)}|filePath=${playItem.name}`)">
            <el-text>
              <el-icon>
                <img alt="DandanPlay" class="el-icon--left icon" src="@/icon/icon-DandanPlay.webp"/>
              </el-icon>
              弹弹Play
            </el-text>
          </el-dropdown-item>
          <el-dropdown-item @click="openUrl(`anix://openVideo/${encodeUrl(playItem.src)}`)">
            <el-text>
              <el-icon>
                <img alt="AnimacX" class="el-icon--left icon" src="@/icon/icon-AnimacX.webp"/>
              </el-icon>
              AnimacX
            </el-text>
          </el-dropdown-item>
          <el-dropdown-item
              @click="openUrl(`SenPlayer://x-callback-url/play?url=${encodeUrl(playItem.src)}&name=${playItem.name}`)">
            <el-text>
              <el-icon>
                <img alt="SenPlayer" class="el-icon--left icon" src="@/icon/icon-SenPlayer.webp"/>
              </el-icon>
              SenPlayer
            </el-text>
          </el-dropdown-item>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
// 使用 slim 构建，将 FFmpeg WASM 拆成独立资源，避免把大体积 WASM 内嵌到 JS。
import 'movi-player'

const props = defineProps(['playItem'])

let openUrl = (url) => {
  window.open(url)
}

let encodeUrl = (str) => {
  return encodeURIComponent(str);
}

</script>

<style scoped>
.movi-app {
  display: block;
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
