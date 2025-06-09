<template>
  <div
      class="flex-center"
      style="justify-content: space-between;flex-flow: column; height: 100%;width: 100%;"
      v-if="!authorization">
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
      <a href="https://docs.wushuo.top" target="_blank">ani-rss</a> | <a href="https://github.com/wushuo894/ani-rss"
                                                                         target="_blank">github</a>
    </div>
  </div>
  <App v-else></App>
</template>

<script setup>

import {onMounted, ref} from "vue";
import CryptoJS from "crypto-js"
import App from "./home/App.vue";
import api from "./js/api.js";
import {useDark, useLocalStorage} from '@vueuse/core'
import {Key} from "@element-plus/icons-vue";

let loading = ref(false)

let authorization = ref("")
let user = ref({
  'username': '',
  'password': ''
})

authorization.value = localStorage.getItem("authorization")
if (authorization.value) {
  window.authorization = authorization.value
}

let rememberThePassword = useLocalStorage('rememberThePassword', {
  remember: false,
  username: '',
  password: ''
})

let login = () => {
  loading.value = true
  user.value.password = user.value.password.trim()
  user.value.username = user.value.username.trim()
  let my_user = JSON.parse(JSON.stringify(user.value))
  my_user.password = my_user.password ? CryptoJS.MD5(my_user.password).toString() : '';
  api.post('api/login', my_user)
      .then(res => {
        localStorage.setItem("authorization", res.data)
        window.authorization = res.data
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

let test = () => {
  if (window.authorization) {
    return
  }
  fetch('api/test', {
    'headers': {
      'Authorization': window.authorization
    }
  })
      .then(res => res.json())
      .then(res => {
        if (res.code === 200) {
          localStorage.setItem("authorization", '1')
          window.authorization = '1'
          authorization.value = '1'
          return
        }
        localStorage.removeItem("authorization")
        window.authorization = ''
        authorization.value = ''
      })
}

useDark()

onMounted(() => {
  test()
  let {remember, username, password} = rememberThePassword.value;
  if (remember && username && password) {
    user.value.username = username
    user.value.password = password
  }
})


// document.documentElement 是全局变量时
const el = document.documentElement
// const el = document.getElementById('xxx')

// 获取 css 变量
getComputedStyle(el).getPropertyValue(`--el-color-primary`)

// 设置 css 变量
el.style.setProperty('--el-color-primary', useLocalStorage('--el-color-primary', '#409eff').value)

</script>

<style>
@media (max-width: 450px) {
  #form {
    width: 80%;
  }
}
</style>
