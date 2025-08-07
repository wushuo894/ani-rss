<template>
  <el-form label-width="auto"
           style="width: 100%">
    <el-form-item label="获取方式">
      <el-radio-group v-model="props.config['bgmTokenType']">
        <el-radio label="手动输入" value="INPUT"/>
        <el-radio label="自动获取" value="AUTO"/>
      </el-radio-group>
    </el-form-item>
    <template v-if="props.config['bgmTokenType'] === 'INPUT'">
      <el-form-item label="Token">
        <div style="width: 100%;">
          <el-input
              v-model="props.config.bgmToken"
              placeholder="ABCDEFGHIJKLMNOPQRS"
          />
          <div>
            <el-text class="mx-1" size="small">
              你可以在 <a href="https://next.bgm.tv/demo/access-token"
                          target="_blank">https://next.bgm.tv/demo/access-token</a>
              生成一个 Access Token
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
        <div style="width: 100%;">
          <el-input v-model="props.config['bgmRedirectUri']"/>
          <el-button bg icon="Refresh"
                     style="margin-top: 6px;"
                     text
                     @click="setRedirectUri"/>
        </div>
      </el-form-item>
      <div class="flex" style="justify-content: space-between;">
        <el-text class="mx-1" size="small">
          自动获取可以实现token自动续期
          <br>
          前往 <a target="_blank" href="https://bgm.tv/dev/app">Bangumi 开发者平台</a> 设置你自己的应用
        </el-text>
        <el-button bg text
                   type="primary"
                   :disabled="!props.config['bgmAppSecret'] || !props.config['bgmAppID']"
                   @click="start"
                   :loading="loading"
        >
          获取授权
        </el-button>
      </div>
    </template>
  </el-form>
  <div class="flex" style="justify-content: start;">
    <a href="https://docs.wushuo.top/config/basic/other#emby-webhook" target="_blank">支持自动点格子</a>
  </div>
</template>

<script setup>
import {ElText} from "element-plus";
import api from "@/js/api.js";

let props = defineProps(['config'])

let setRedirectUri = () => {
  props.config['bgmRedirectUri'] = `${location.href}api/bgm/callback`
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
        let redirect = window.encodeURI(`${props.config['bgmRedirectUri']}?s=${window.authorization}`)
        let url = `https://bgm.tv/oauth/authorize?client_id=${props.config['bgmAppID']}&response_type=code&redirect_uri=${redirect}`
        window.open(url)
        location.reload()
      })
      .finally(() => {
        loading.value = false;
      })
}

</script>
