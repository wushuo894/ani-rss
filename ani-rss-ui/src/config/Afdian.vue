<template>
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
          <div class="flex afdian-tag-content">
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
  <div class="flex afdian-header">
    <a href="https://ifdian.net/a/wushuo894" target="_blank">
      <img :src="support_aifadian" alt="support_aifadian">
    </a>
    <div class="afdian-unlock">
      <h3>捐赠后解锁</h3>
      <el-tag v-for="it in ['Mikan/AnimeGarden 番剧列表显示评分']"
              class="afdian-tag">
        {{ it }}
      </el-tag>
    </div>
  </div>
  <div class="afdian-section" v-if="!props.config['verifyExpirationTime']">
    <el-text class="mx-1" size="small">
      已经捐赠？在这里输入您的订单号以激活您的捐赠
    </el-text>
    <div class="flex afdian-input-group">
      <div class="flex">
        <el-input v-model="props.config.outTradeNo"
                  class="afdian-input">
          <template #prefix>
            <el-icon class="el-input__icon">
              <EditPen/>
            </el-icon>
          </template>
        </el-input>
        <div class="afdian-spacer"></div>
        <el-button :loading="verifyNoLoading"
                   bg text
                   type="primary"
                   @click="verifyNo">
          验证
        </el-button>
      </div>
    </div>
    <div class="afdian-link">
      <el-link
          type="primary"
          target="_blank"
          href="https://github.com/wushuo894/ani-rss/discussions/260">
        关于增加的捐赠功能，我想说的话
      </el-link>
    </div>
  </div>
  <div class="afdian-section" v-else>
    <el-alert
        :closable="false"
        title="感谢您的捐赠支持🎁"
    />
  </div>
</template>

<style scoped>
.afdian-tag-content {
  align-items: center;
}

.afdian-header {
  justify-content: space-between;
}

.afdian-unlock {
  flex: 1;
  padding-left: 8px;
}

.afdian-tag {
  margin: 4px 4px 0 0;
}

.afdian-section {
  margin-top: 18px;
}

.afdian-input-group {
  width: 100%;
  margin-top: 8px;
  justify-content: space-between;
}

.afdian-input {
  max-width: 200px;
}

.afdian-spacer {
  width: 8px;
}

.afdian-link {
  margin-top: 8px;
}
</style>

<script setup>
import {ref} from "vue";
import support_aifadian from "@/icon/support_aifadian.svg";
import {ElMessage} from "element-plus";
import {EditPen, Mug} from "@element-plus/icons-vue";
import * as http from "@/js/http.js";

let timestampToDate = (timestamp) => {
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
  http.verifyNo({
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
