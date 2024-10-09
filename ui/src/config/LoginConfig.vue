<template>
  <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
    <el-form-item label="用户名">
      <el-input v-model:model-value="props.config.login.username"/>
    </el-form-item>
    <el-form-item label="密码">
      <el-input v-model:model-value="props.config.login.password"/>
    </el-form-item>
    <el-form-item label="IP校验">
      <div>
        <el-switch v-model:model-value="props.config.login.verifyIp"/>
      </div>
      <div>
        <el-text class="mx-1" size="small">
          如果IP发生改变登录将失效
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="IP白名单">
      <div style="width: 100%;">
        <div>
          <el-switch v-model:model-value="config['ipWhitelist']"/>
        </div>
        <div style="width: 100%;">
          <el-input style="width: 100%" type="textarea"
                    :disabled="!config['ipWhitelist']"
                    :placeholder="'127.0.0.1\n192.168.1.0/24'" v-model:model-value="config['ipWhitelistStr']"/>
          <br>
          <el-text class="mx-1" size="small">
            对IP白名单跳过身份验证, 换行可填写多个
          </el-text>
        </div>
      </div>
    </el-form-item>
    <el-form-item label="Api Key">
      <div style="display: flex;width: 100%;">
        <el-input v-model:model-value="props.config.apiKey" clearable/>
        <div style="width: 4px;"></div>
        <el-button bg text @click="createApiKey">生成</el-button>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>

import {ElText} from "element-plus";

let generateRandomString = (length) => {
  const charset = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let randomString = '';
  for (let i = 0; i < length; i++) {
    const randomIndex = Math.floor(Math.random() * charset.length);
    randomString += charset[randomIndex];
  }
  return randomString;
}

let createApiKey = () => {
  props.config.apiKey = generateRandomString(32);
}

let props = defineProps(['config'])
</script>
