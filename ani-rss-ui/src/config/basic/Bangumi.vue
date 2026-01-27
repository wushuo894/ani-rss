<template>
  <bangumi-me ref="bangumiMe"/>
  <el-form label-width="auto"
           class="form-full-width">
    <el-form-item label="获取方式">
      <el-radio-group v-model="props.config['bgmTokenType']">
        <el-radio label="手动输入" value="INPUT"/>
        <el-radio label="自动获取" value="AUTO"/>
      </el-radio-group>
    </el-form-item>
    <template v-if="props.config['bgmTokenType'] === 'INPUT'">
      <el-form-item label="Token">
        <div class="full-width">
          <el-input
              v-model="props.config.bgmToken"
              placeholder="ABCDEFGHIJKLMNOPQRS"
          />
          <div>
            <el-text class="mx-1" size="small">
              你可以在&nbsp;
              <el-link
                  type="primary"
                  class="text-extra-small"
                  href="https://next.bgm.tv/demo/access-token"
                  target="_blank">
                https://next.bgm.tv/demo/access-token
              </el-link>
              &nbsp;生成一个 Access Token
            </el-text>
          </div>
        </div>
      </el-form-item>
    </template>
    <template v-if="props.config['bgmTokenType'] === 'AUTO'">
      <el-form-item label="App ID">
        <el-input
            v-model="props.config['bgmAppID']"
            placeholder="bgm123456789"
        />
      </el-form-item>
      <el-form-item label="App Secret">
        <el-input
            v-model="props.config['bgmAppSecret']"
            placeholder="abcdefghijklm"
        />
      </el-form-item>
      <el-form-item label="回调地址">
        <div class="full-width">
          <el-input v-model="props.config['bgmRedirectUri']"/>
          <el-button bg icon="Refresh"
                     class="mt-6"
                     text
                     @click="setRedirectUri"/>
        </div>
      </el-form-item>
      <div class="flex justify-space-between">
        <el-text class="mx-1" size="small">
          自动获取可以实现token自动续期
          <br>
          前往&nbsp;
          <el-link
              class="text-extra-small"
              type="primary"
              target="_blank"
              href="https://bgm.tv/dev/app">
            Bangumi 开发者平台
          </el-link>
          &nbsp;设置你自己的应用
        </el-text>
        <div>
          <el-button bg
                     type="primary"
                     :disabled="!props.config['bgmAppSecret'] || !props.config['bgmAppID']"
                     @click="start"
                     :loading="loading"
          >
            获取授权
          </el-button>
          <el-button
              bg
              type="success"
              @click="bangumiMe?.show">
            查看授权状态
          </el-button>
        </div>
      </div>
    </template>
  </el-form>
  <div class="flex justify-start">
    <el-link type="primary"
             class="text-extra-small"
             href="https://docs.wushuo.top/config/basic/other#emby-webhook"
             target="_blank">支持自动点格子
    </el-link>
  </div>
</template>

<script setup>
import {ElText} from "element-plus";
import api from "@/js/api.js";
import BangumiMe from "@/config/basic/BangumiMe.vue";
import {onMounted} from "vue";

let bangumiMe = ref()

let props = defineProps(['config'])

let setRedirectUri = () => {
  let redirectUri = location.href
  if (!redirectUri.endsWith("/")) {
    redirectUri += '/';
  }
  props.config['bgmRedirectUri'] = redirectUri + 'bgm-oauth-callback'
}

onMounted(() => {
  if (props.config['bgmRedirectUri']) {
    return
  }
  setRedirectUri()
})

let loading = ref(false);

let start = () => {
  loading.value = true;
  api.post('api/config', props.config)
      .then(async res => {
        let redirect = window.encodeURI(props.config['bgmRedirectUri'])
        let url = `https://bgm.tv/oauth/authorize?client_id=${props.config['bgmAppID']}&response_type=code&redirect_uri=${redirect}`
        window.open(url)
        location.reload()
      })
      .finally(() => {
        loading.value = false;
      })
}

</script>

<style scoped>
.form-full-width {
  width: 100%;
}

.full-width {
  width: 100%;
}

.text-extra-small {
  font-size: var(--el-font-size-extra-small);
}

.mt-6 {
  margin-top: 6px;
}

.justify-space-between {
  justify-content: space-between;
}

.justify-start {
  justify-content: start;
}
</style>
