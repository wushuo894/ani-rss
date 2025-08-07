<template>
  <el-collapse v-model:model-value="activeName" accordion>
    <el-collapse-item name="1" title="页面设置">
      <page :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="2" title="添加订阅">
      <add :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="3" title="重命名设置">
      <rename :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="4" title="RSS设置">
      <rss :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="5" title="Trackers">
      <trackers :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="6" title="Bangumi">
      <bangumi :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="7" title="其他">
      <other :config="props.config"/>
    </el-collapse-item>
  </el-collapse>
</template>

<script setup>
import {ref} from "vue";
import Page from "@/config/basic/Page.vue";
import Add from "@/config/basic/Add.vue";
import Rename from "@/config/basic/Rename.vue";
import Rss from "@/config/basic/Rss.vue";
import Trackers from "@/config/basic/Trackers.vue";
import Other from "@/config/basic/Other.vue";
import Bangumi from "@/config/basic/Bangumi.vue";

let activeName = ref('1')

let props = defineProps(['config'])
</script>
