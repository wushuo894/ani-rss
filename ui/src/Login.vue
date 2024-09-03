<template>
  <div style="display: flex;align-items: center;justify-content: center;height: 100%;width: 100%;"
       v-if="!authorization">
    <div id="form">
      <div style="text-align: center;">
        <img src="../public/mikan-pic.png" height="80" width="80"/>
      </div>
      <h2 style="text-align: center">ANI-RSS</h2>
      <div style="height: 30px;"></div>
      <el-form label-width="auto"
               @submit="login">
        <el-form-item label="用户名">
          <el-input v-model:model-value="user.username"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model:model-value="user.password" show-password></el-input>
        </el-form-item>
        <div style="display: flex;width: 100%;align-items: flex-end;flex-flow: column;">
          <el-button @click="login" :loading="loading">登录</el-button>
        </div>
      </el-form>
    </div>
  </div>
  <App v-else></App>
</template>

<script setup>

import {ref} from "vue";
import CryptoJS from "crypto-js"
import App from "./App.vue";

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

let login = () => {
  loading.value = true
  let my_user = JSON.parse(JSON.stringify(user.value))
  my_user.password = CryptoJS.MD5(my_user.password).toString();
  fetch('/api/login', {
    'method': 'POST',
    'body': JSON.stringify(my_user)
  }).then(res => res.json())
      .then(res => {
        localStorage.setItem("authorization", res.data)
        window.authorization = res.data
        authorization.value = res.data
      })
      .finally(() => {
        loading.value = false
      })
}
</script>

<style>
@media (max-width: 450px) {
  #form {
    width: 80%;
  }
}
</style>