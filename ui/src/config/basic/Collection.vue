<template>
    <el-form label-width="auto" style="width: 100%" @submit="(event) => {
        event.preventDefault()
    }">
        <el-form-item label="开启合集软连接管理功能">
            <el-switch v-model:model-value="props.config.collectionSoftlinkSwitch" />
        </el-form-item>

        <el-form-item label="真实保存目录">
            <div style="width: 100%;">
                <el-input v-model:model-value="props.config.collectionSoftlinkRealPathTemplate" />
                <el-alert v-if="!testPathTemplate(props.config.collectionSoftlinkRealPathTemplate)" style="margin-top: 8px;"
                    type="warning" show-icon :closable="false">
                    <template #title>
                        你的 保存位置 并未按照模版填写, 可能会遇到下载位置错误
                    </template>
                </el-alert>
            </div>
        </el-form-item>

        <el-form-item label="软连接目标目录">
            <div style="width: 100%;">
                <el-input v-model:model-value="props.config.collectionSoftlinkTargetPathTemplate" />
                <el-alert v-if="!testPathTemplate(props.config.collectionSoftlinkTargetPathTemplate)" style="margin-top: 8px;"
                    type="warning" show-icon :closable="false">
                    <template #title>
                        你的 保存位置 并未按照模版填写, 可能会遇到下载位置错误
                    </template>
                </el-alert>
            </div>
        </el-form-item>
    </el-form>
</template>

<script setup>
import { ElMessage, ElText } from "element-plus";
import { ref } from "vue";
import api from "@/js/api.js";

let testPathTemplate = (path) => {
  return new RegExp('\\$\{[A-z]+\}').test(path);
}


let props = defineProps(['config'])
</script>
