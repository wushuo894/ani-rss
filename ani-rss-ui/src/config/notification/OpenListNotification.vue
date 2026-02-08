<template>
  <template v-if="notificationConfig['notificationType'] === 'OPEN_LIST_UPLOAD'">
    <el-form-item label="Host">
      <el-input v-model="notificationConfig['openListUploadHost']" placeholder="http://127.0.0.1:5244"/>
    </el-form-item>
    <el-form-item label="ApiKey">
      <el-input v-model="notificationConfig['openListUploadApiKey']" placeholder="openlist-xxxxxx"/>
    </el-form-item>
    <el-form-item label="上传位置">
      <el-input
          v-model="notificationConfig['openListUploadPath']"
          placeholder="/115/Media/番剧/${title}/Season ${season}"/>
    </el-form-item>
    <el-form-item label="上传位置 (剧场版)">
      <el-input
          v-model="notificationConfig['openListUploadOvaPath']"
          placeholder="/115/Media/剧场版/${title}"/>
    </el-form-item>
    <el-form-item label="其他">
      <div>
        <el-checkbox v-model="notificationConfig['openListUploadDeleteLocalFile']" label="上传完成后删除本地文件"/>
        <el-checkbox v-model="notificationConfig['openListUploadTask']" label="添加为上传任务"/>
        <br/>
        <el-checkbox v-model="notificationConfig['openListUploadDeleteOldEpisode']" label="删除旧的同集文件"/>
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
