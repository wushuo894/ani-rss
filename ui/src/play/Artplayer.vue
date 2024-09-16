<template>
  <div>
    <div class="artplayer-app"></div>
  </div>
</template>

<script setup>
import {onMounted} from 'vue'
import Artplayer from 'artplayer';
import artplayerPluginMultipleSubtitles from 'artplayer-plugin-multiple-subtitles';

const props = defineProps(['src', 'subtitles'])

onMounted(() => {
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
