<template>
  <TryOut ref="tryOutRef" :config="props.config"/>
  <el-form>
    <el-form-item label="捐赠状态">
      <div v-if="props.config['tryOut']">
        <el-tag v-if="props.config['verifyExpirationTime']" type="primary">
          试用中 过期时间: {{ timestampToDate(props.config['expirationTime']) }}
        </el-tag>
        <el-tag v-else type="warning">
          试用已过期
        </el-tag>
      </div>
      <div v-else>
        <el-tag v-if="props.config['verifyExpirationTime']" type="success">
          <div class="flex" style="align-items: center;">
            <el-icon>
              <Mug/>
            </el-icon>
            <span>
            已捐赠
            </span>
          </div>
        </el-tag>
        <el-tag v-else type="info">
          未捐赠
        </el-tag>
      </div>
    </el-form-item>
  </el-form>
  <div class="flex" style="justify-content: space-between">
    <a href="https://afdian.com/a/wushuo894" target="_blank">
      <img :src="support_aifadian" alt="support_aifadian">
    </a>
    <div style="flex: 1;padding-left: 8px;">
      <h3>捐赠后解锁</h3>
      <el-tag v-for="it in ['订阅完结通知','Emby媒体库刷新','添加合集','摸鱼检测','Alist上传通知','订阅完结迁移']"
              style="margin: 4px 4px 0 0;">
        {{ it }}
      </el-tag>
    </div>
  </div>
  <div style="margin-top: 18px" v-if="!props.config['verifyExpirationTime'] || props.config['tryOut']">
    <el-text class="mx-1" size="small">
      已经捐赠？在这里输入您的订单号以激活您的捐赠
    </el-text>
    <div class="flex" style="width: 100%;margin-top: 8px;justify-content: space-between;">
      <div class="flex">
        <el-input v-model="props.config.outTradeNo"
                  style="max-width: 200px;">
          <template #prefix>
            <el-icon class="el-input__icon">
              <EditPen/>
            </el-icon>
          </template>
        </el-input>
        <div style="width: 8px"></div>
        <el-button :loading="verifyNoLoading"
                   bg text
                   type="primary"
                   @click="verifyNo">
          验证
        </el-button>
      </div>
      <div style="margin-left: 8px;">
        <el-button :disabled="props.config['verifyExpirationTime']"
                   bg
                   text
                   @click="tryOutRef?.show">
          试用
        </el-button>
      </div>
    </div>
    <div style="margin-top: 8px">
      <a target="_blank" href="https://github.com/wushuo894/ani-rss/discussions/260">
        关于增加的捐赠功能，我想说的话
      </a>
    </div>
  </div>
  <div style="margin-top: 18px" v-else>
    <el-alert
        :closable="false"
        title="感谢您的捐赠支持🎁"
    />
  </div>
</template>


<script setup>
import {ref} from "vue";
import support_aifadian from "@/icon/support_aifadian.svg";
import api from "@/js/api.js";
import {ElMessage} from "element-plus";
import {EditPen, Mug} from "@element-plus/icons-vue";
import TryOut from "./TryOut.vue";

let tryOutRef = ref()

function timestampToDate(timestamp) {
  const date = new Date(timestamp);

  const year = date.getFullYear(); // 获取年份
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  const seconds = date.getSeconds().toString().padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

let verifyNoLoading = ref(false)

let verifyNo = () => {
  verifyNoLoading.value = true
  api.post('api/afdian?type=verifyNo', {
    outTradeNo: props.config.outTradeNo,
  }).then(res => {
    ElMessage.success(res.message)
    props.config['expirationTime'] = res.data
    props.config['verifyExpirationTime'] = true
    props.config['tryOut'] = false
  }).finally(() => {
    verifyNoLoading.value = false
  })
}

let props = defineProps(['config'])
</script>
