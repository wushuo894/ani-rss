<template>
  <div
      class="flex-center"
      style="justify-content: space-between;flex-flow: column; height: 100%;width: 100%;">
    <div id="login-page" class="flex-center" style="flex: 1">
      <div id="form" style="max-width: 200px;">
        <div style="text-align: center;">
          <img src="../public/icon.svg" height="80" width="80" alt="icon.svg"/>
        </div>
        <h2 style="text-align: center">ANI-RSS</h2>
        <div style="height: 30px;"></div>
        <el-form
            @keyup.enter="login"
            @submit="login">
          <el-form-item>
            <el-input v-model:model-value="user.username" placeholder="用户名" style="width: 200px;">
              <template #prefix>
                <el-icon class="el-input__icon">
                  <User/>
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input style="width: 200px;" v-model:model-value="user.password" show-password
                      placeholder="密码">
              <template #prefix>
                <el-icon class="el-input__icon">
                  <Key/>
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
          <div class="flex-center" style="width: 100%;justify-content: space-between;">
            <el-checkbox v-model:model-value="rememberThePassword.remember">记住密码</el-checkbox>
            <el-button @click="login" :loading="loading" text bg icon="Right">登录</el-button>
          </div>
        </el-form>
      </div>
    </div>
    <div style="margin-bottom: 16px;" id="link">
      <el-link type="default"
               href="https://docs.wushuo.top"
               target="_blank">
        ani-rss
      </el-link>
      &nbsp;
      <el-link type="default"
               href="https://github.com/wushuo894/ani-rss"
               target="_blank">
        github
      </el-link>
    </div>
  </div>
</template>

<script setup>

import {onMounted, ref} from "vue";
import CryptoJS from "crypto-js"
import api from "./js/api.js";
import {useLocalStorage} from '@vueuse/core'
import {Key} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";
import {authorization} from "@/js/global.js";

let loading = ref(false)

let user = ref({
  'username': '',
  'password': ''
})

/**
 * 保存登录信息
 */
let rememberThePassword = useLocalStorage('rememberThePassword', {
  remember: false,
  username: '',
  password: ''
})

/**
 * 登录
 */
let login = () => {
  user.value.password = user.value.password.trim()
  user.value.username = user.value.username.trim()

  if (!user.value.password || !user.value.username) {
    ElMessage.error('请输入账号与密码')
    return
  }

  let my_user = JSON.parse(JSON.stringify(user.value))
  my_user.password = CryptoJS['MD5'](my_user.password).toString()

  loading.value = true

  api.post('api/login', my_user)
      .then(res => {
        authorization.value = res.data

        // 记住密码
        if (rememberThePassword.value.remember) {
          rememberThePassword.value.username = user.value.username
          rememberThePassword.value.password = user.value.password
        } else {
          rememberThePassword.value.username = ''
          rememberThePassword.value.password = ''
        }
      })
      .finally(() => {
        loading.value = false
      })
}

/**
 * 测试是否处于白名单
 */
let test = () => {
  if (authorization.value) {
    return
  }
  fetch('api/test')
      .then(res => res.json())
      .then(res => {
        if (res.code === 200) {
          authorization.value = new Date().getTime() + '';
          return
        }
        authorization.value = ''
      })
}

onMounted(() => {
  test()
  let {remember, username, password} = rememberThePassword.value;
  if (remember && username && password) {
    user.value.username = username
    user.value.password = password
  }
})

</script>

<style>
@media (max-width: 450px) {
  #form {
    width: 80%;
  }
}
</style>
