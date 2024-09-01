<template>
  <div style="display: flex;align-items: center;justify-content: center;height: 100%;" v-if="!authorization">
    <el-form label-width="auto"
             @submit="(event)=>{
                event.preventDefault()
             }">
      <el-form-item label="用户名">
        <el-input v-model:model-value="user.username"></el-input>
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model:model-value="user.password" show-password></el-input>
      </el-form-item>
      <div style="display: flex;width: 100%;align-items: flex-end;flex-flow: column;">
        <el-button @click="login">登录</el-button>
      </div>
    </el-form>
  </div>
  <App v-else></App>
</template>

<script setup>

import {ref} from "vue";
import CryptoJS from "crypto-js"
import App from "./App.vue";
import {ElMessage} from "element-plus";

let authorization = ref("")
let user = ref({
  'username': '',
  'password': ''
})

authorization.value = localStorage.getItem("Authorization")
if (authorization.value) {
  window.authorization = authorization.value
}

let login = () => {
  let my_user = JSON.parse(JSON.stringify(user.value))
  my_user.password = CryptoJS.MD5(my_user.password).toString();
  fetch('/api/login', {
    'method': 'POST',
    'body': JSON.stringify(my_user)
  }).then(res => res.json())
      .then(res => {
        if (res.code !== 200) {
          ElMessage.error(res.message)
          return
        }
        localStorage.setItem("Authorization", res.data)
        window.authorization = res.data
        authorization.value = res.data
      })
}
</script>