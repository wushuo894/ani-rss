<template>
  <el-form-item label="下载速度限制">
    <el-input-number v-model:model-value="props.config['dlLimit']" :min="0">
      <template #suffix>
        <span>kiB/s</span>
      </template>
    </el-input-number>
  </el-form-item>
  <el-form-item label="上传速度限制">
    <el-input-number v-model:model-value="props.config['upLimit']" :min="0">
      <template #suffix>
        <span>kiB/s</span>
      </template>
    </el-input-number>
  </el-form-item>
  <el-form-item label="分享率">
    <div>
      <el-input-number v-model:model-value="props.config.ratioLimit" :min="-2"/>
      <br>
      <el-text class="mx-1" size="small">
        "-1"表示禁用, "-2"使用全局设置
      </el-text>
    </div>
  </el-form-item>
  <el-form-item label="总做种时长">
    <div>
      <el-input-number v-model:model-value="props.config.seedingTimeLimit" :min="-2">
        <template #suffix>
          <span>分钟</span>
        </template>
      </el-input-number>
      <br>
      <el-text class="mx-1" size="small">
        "-1"表示禁用, "-2"使用全局设置
      </el-text>
    </div>
  </el-form-item>
  <el-form-item label="非活跃时长">
    <div>
      <el-input-number v-model:model-value="props.config.inactiveSeedingTimeLimit" :min="-2">
        <template #suffix>
          <span>分钟</span>
        </template>
      </el-input-number>
      <br>
      <el-text class="mx-1" size="small">
        "-1"表示禁用, "-2"使用全局设置
      </el-text>
    </div>
  </el-form-item>
  <el-form-item label="qb保存路径">
    <div>
      <el-switch v-model:model-value="props.config.qbUseDownloadPath"
                 :disabled="config.downloadToolType !== 'qBittorrent'"/>
      <br>
      <el-text class="mx-1" size="small">
        开启后将使用qBittorrent的临时下载位置 (最终下载位置不受影响)
      </el-text>
    </div>
  </el-form-item>
</template>

<script setup>
let props = defineProps(['config'])
</script>