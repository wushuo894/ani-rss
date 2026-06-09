<template>
  <el-collapse v-model:model-value="activeName" accordion>
    <el-collapse-item name="page" title="页面设置">
      <page :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="add" title="添加订阅">
      <add :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="rename" title="重命名设置">
      <rename :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="scrape" title="刮削设置">
      <scrape :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="rss" title="RSS设置">
      <rss :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="trackers" title="Trackers">
      <trackers :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item v-if="props.config['bgmEnabled']" name="bangumi" title="Bangumi">
      <bangumi :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="other" title="其他">
      <other :config="props.config"/>
    </el-collapse-item>
    <el-collapse-item name="backup" title="备份">
      <backup :config="props.config"/>
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
import Backup from "./basic/Backup.vue";
import Scrape from "@/config/basic/Scrape.vue";

let activeName = ref('page')

let props = defineProps(['config'])
</script>
