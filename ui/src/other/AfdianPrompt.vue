<template>
  <AfdianDialog ref="afdian" :config="myConfig"/>
  <div>
    <el-text v-if="!myConfig['verifyExpirationTime']" class="mx-1" size="small">
      需要
      <el-button bg icon="Mug" size="small" text type="primary" @click="afdian?.show">捐赠</el-button>
      后才可解锁 <strong>{{ props.name }}</strong>
    </el-text>
  </div>
</template>

<script setup>
import AfdianDialog from "../config/AfdianDialog.vue";
import {onMounted, ref} from "vue";
import api from "../js/api.js";

let afdian = ref()

let myConfig = ref({
  verifyExpirationTime: false
})

onMounted(() => {
  if (props.config) {
    myConfig.value = props.config;
    return
  }

  api.get('api/config')
      .then(res => {
        myConfig.value = res.data
      })
})

let props = defineProps(['config', 'name'])
</script>
