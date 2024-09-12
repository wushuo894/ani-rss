<template>
  <div>
    <el-form style="max-width: 600px" label-width="auto"
             @submit="(event)=>{
                      event.preventDefault()
                   }">
      <el-form-item label="RSS间隔(分钟)">
        <el-input-number v-model:model-value="props.config.sleep" :min="1"/>
      </el-form-item>
      <el-form-item label="自动重命名">
        <el-switch v-model:model-value="props.config.rename"/>
      </el-form-item>
      <el-form-item label="重命名间隔(分钟)">
        <el-input-number v-model:model-value="props.config.renameSleep" min="1"
                         :disabled="!config.rename"></el-input-number>
      </el-form-item>
      <el-form-item label="自动跳过">
        <div style="width: 100%">
          <el-switch v-model:model-value="props.config.fileExist" :disabled="!config.rename"/>
          <br>
          <el-text class="mx-1" size="small">
            文件已下载自动跳过 此选项必须启用 自动重命名。确保 下载工具 与本程序 docker 映射挂载路径一致
            <a href="https://docs.wushuo.top/docs#%E8%87%AA%E5%8A%A8%E8%B7%B3%E8%BF%87"
               target="_blank">详细说明</a>
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="自动推断剧集偏移">
        <el-switch v-model:model-value="props.config.offset"/>
      </el-form-item>
      <el-form-item label="TMDB">
        <div>
          <el-switch v-model:model-value="props.config.tmdb"/>
          <br>
          <el-text class="mx-1" size="small">
            自动使用TMDB的标题
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="自动禁用订阅">
        <div>
          <el-switch v-model:model-value="props.config.autoDisabled"/>
          <br>
          <el-text class="mx-1" size="small">
            根据 Bangumi 获取总集数 当所有集数都已下载时自动禁用该订阅
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="获取标题时带上年份">
        <el-switch v-model:model-value="props.config.titleYear"/>
      </el-form-item>
      <el-form-item label="自动跳过xx.5集数">
        <el-switch v-model:model-value="props.config.skip5"/>
      </el-form-item>
      <el-form-item label="DEBUG">
        <el-switch v-model:model-value="props.config.debug"/>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
let props = defineProps(['config'])
</script>