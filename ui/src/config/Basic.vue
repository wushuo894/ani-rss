<template>
  <el-form label-width="auto"
           style="width: 100%"
           @submit="(event)=>{
                    event.preventDefault()
                   }">
    <el-collapse v-model:model-value="activeName" accordion>
      <el-collapse-item title="页面设置" name="1">
        <page :config="props.config"/>
      </el-collapse-item>
      <el-collapse-item title="添加订阅" name="2">
        <add :config="props.config"/>
      </el-collapse-item>
      <el-collapse-item title="重命名设置" name="3">
        <rename :config="props.config"/>
      </el-collapse-item>
      <el-collapse-item title="RSS设置" name="4">
        <rss :config="props.config"/>
      </el-collapse-item>
      <el-collapse-item name="5" title="Trackers">
        <trackers :config="props.config"/>
      </el-collapse-item>
      <el-collapse-item name="6" title="其他">
        <other :config="props.config"/>
      </el-collapse-item>
    </el-collapse>
  </el-form>
</template>

<script setup>
import {ref} from "vue";
import Page from "@/config/basic/Page.vue";
import Add from "@/config/basic/Add.vue";
import Rename from "@/config/basic/Rename.vue";
import Rss from "@/config/basic/Rss.vue";
import Trackers from "@/config/basic/Trackers.vue";
import Other from "@/config/basic/Other.vue";

let activeName = ref('1')

let props = defineProps(['config'])
</script>
