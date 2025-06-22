<template>
  <el-dialog v-model="dialogVisible" align-center center style="max-width: 600px;" title="爱发电">
    <Afdian v-if="dialogVisible" :config="props.config"/>
  </el-dialog>
</template>


<script setup>
import {ref} from "vue";
import Afdian from "@/config/Afdian.vue";

let dialogVisible = ref(false)

let show = () => {
  dialogVisible.value = true
}

defineExpose({show})
let props = defineProps(['config'])
</script>
