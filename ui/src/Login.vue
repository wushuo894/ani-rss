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
               @keyup.enter="login"
               @submit="login">
        <el-form-item>
          <el-input style="width: 200px;" v-model:model-value="user.username" placeholder="用户名"></el-input>
        </el-form-item>
        <el-form-item>
          <el-input style="width: 200px;" v-model:model-value="user.password" show-password placeholder="密码"></el-input>
        </el-form-item>
        <div style="display: flex;width: 100%;align-items: flex-end;flex-flow: column;">
          <el-button @click="login" :loading="loading" text bg icon="Right">登录</el-button>
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
import api from "./api.js";

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
  user.value.password = user.value.password.trim()
  user.value.username = user.value.username.trim()
  let my_user = JSON.parse(JSON.stringify(user.value))
  my_user.password = my_user.password ? CryptoJS.MD5(my_user.password).toString() : '';
  api.post('/api/login', my_user)
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