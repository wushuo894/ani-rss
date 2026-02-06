<template>
  <template v-if="notificationConfig['notificationType'] === 'FILE_MOVE'">
    <el-form-item label="目标位置">
      <el-input
          v-model="notificationConfig['fileMoveTarget']"
          placeholder="/CD2/115/Media/番剧/${title}/Season ${season}"/>
    </el-form-item>
    <el-form-item label="目标位置 (剧场版)">
      <el-input
          v-model="notificationConfig['fileMoveOvaTarget']"
          placeholder="/CD2/115/Media/剧场版/${title}"/>
    </el-form-item>
    <el-form-item label="其他">
      <div>
        <el-checkbox v-model="notificationConfig['fileMoveDeleteOldEpisode']" label="删除旧的同集文件"/>
        <br/>
        <el-text class="mx-1" size="small">
          自动时将目标位置存在的同集文件删除, 可达到洗版的效果
        </el-text>
      </div>
    </el-form-item>
  </template>
</template>

<script setup>
let props = defineProps(['notificationConfig', 'config'])
</script>
