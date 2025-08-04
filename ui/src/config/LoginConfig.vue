<template>
  <el-form label-width="auto" @submit="(event)=>{
                      event.preventDefault()
                   }">
    <el-form-item label="用户名">
      <el-input v-model:model-value="props.config.login.username">
        <template #prefix>
          <el-icon class="el-input__icon">
            <User/>
          </el-icon>
        </template>
      </el-input>
    </el-form-item>
    <el-form-item label="密码">
      <el-input v-model:model-value="props.config.login.password">
        <template #prefix>
          <el-icon class="el-input__icon">
            <Key/>
          </el-icon>
        </template>
      </el-input>
    </el-form-item>
    <el-form-item label="登录有效">
      <el-input-number v-model:model-value="props.config.loginEffectiveHours" :min="1">
        <template #suffix>
          <span>小时</span>
        </template>
      </el-input-number>
    </el-form-item>
    <el-form-item label="其他">
      <el-checkbox v-model="props.config['multiLoginForbidden']" label="禁止多端登录"/>
      <el-checkbox v-model="props.config.innerIP" label="禁止公网访问"/>
      <el-checkbox v-model="props.config.verifyLoginIp" label="如果IP发生改变登录将失效"/>
    </el-form-item>
    <el-form-item label="IP白名单">
      <div style="width: 100%;">
        <div>
          <el-switch v-model:model-value="config['ipWhitelist']"/>
        </div>
        <div style="width: 100%;">
          <el-input style="width: 100%" type="textarea"
                    :autosize="{ minRows: 2}"
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
      <div class="flex" style="width: 100%;">
        <el-input v-model:model-value="props.config.apiKey" readonly/>
        <div style="margin-left: 12px;" class="flex">
          <el-button bg text @click="createApiKey">生成</el-button>
          <el-button bg text @click="copy(props.config.apiKey)">复制</el-button>
        </div>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>
import {ElMessage, ElText} from "element-plus";
import {Key, User} from "@element-plus/icons-vue";

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
  props.config.apiKey = generateRandomString(64);
}

let copy = (v) => {
  const input = document.createElement('input');
  input.value = v
  document.body.appendChild(input);
  input.select();
  document.execCommand('copy');
  document.body.removeChild(input);
  ElMessage.success('已复制')
}

let props = defineProps(['config'])
</script>
