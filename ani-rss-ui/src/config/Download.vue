<template>
  <el-form label-width="auto"
           class="download-form"
           @submit="(event)=>{
                    event.preventDefault()
                   }">
    <el-form-item label="下载工具">
      <el-select v-model:model-value="props.config.downloadToolType">
        <el-option v-for="item in downloadSelect"
                   :key="item"
                   :label="item"
                   :value="item"/>
      </el-select>
    </el-form-item>
    <el-form-item label="地址">
      <el-input v-model:model-value="props.config.downloadToolHost" placeholder="http://192.168.1.x:8080"/>
    </el-form-item>
    <el-form-item v-if="props.config.downloadToolType === 'Aria2'" label="RPC 密钥">
      <el-input v-model:model-value="props.config.downloadToolPassword" placeholder="" show-password>
        <template #prefix>
          <el-icon class="el-input__icon">
            <Key/>
          </el-icon>
        </template>
      </el-input>
    </el-form-item>
    <template v-else-if="props.config.downloadToolType === 'OpenList'">
      <el-form-item label="Token">
        <el-input v-model:model-value="props.config.downloadToolPassword" placeholder="OpenList-xxxxxx" show-password>
          <template #prefix>
            <el-icon class="el-input__icon">
              <Key/>
            </el-icon>
          </template>
        </el-input>
        <br/>
        <el-text class="mx-1" size="small">
          请设置好 <strong>保存位置</strong> 才能通过测试<br/>
          请在 OpenList -> 设置-> 其他 -> 配置临时目录<br/>
          支持离线下载到 115、PikPak、迅雷云盘
        </el-text>
      </el-form-item>
      <el-form-item label="Driver">
        <el-select v-model="props.config['provider']" class="download-provider-select">
          <el-option v-for="it in offlineList" :key="it.label" :label="it.label" :value="it.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="重试次数">
        <div>
          <el-input-number v-model="props.config['alistDownloadRetryNumber']" :min="-1"/>
          <br>
          <el-text class="mx-1" size="small">
            设置为 -1 将一直进行重试
          </el-text>
        </div>
      </el-form-item>
      <el-form-item label="离线超时">
        <el-input-number v-model:model-value="props.config['alistDownloadTimeout']" :min="1">
          <template #suffix>
            <span>分钟</span>
          </template>
        </el-input-number>
      </el-form-item>
    </template>
    <template v-else>
      <el-form-item label="用户名">
        <el-input v-model:model-value="props.config.downloadToolUsername" placeholder="username"
                  autocomplete="new-password">
          <template #prefix>
            <el-icon class="el-input__icon">
              <User/>
            </el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model:model-value="props.config.downloadToolPassword" placeholder="password" show-password
                  autocomplete="new-password">
          <template #prefix>
            <el-icon class="el-input__icon">
              <Key/>
            </el-icon>
          </template>
        </el-input>
      </el-form-item>
    </template>
    <el-form-item>
      <div class="download-test-button">
        <el-button @click="downloadLoginTest" bg text :loading="downloadLoginTestLoading" icon="Odometer">测试
        </el-button>
      </div>
    </el-form-item>
    <el-form-item label="保存位置">
      <div class="download-path-container">
        <el-input v-model:model-value="props.config['downloadPathTemplate']"/>
        <el-alert
            v-if="!testPathTemplate(props.config['downloadPathTemplate'])"
            class="download-alert"
            type="warning"
            show-icon
            :closable="false"
        >
          <template #title>
            你的 保存位置 并未按照模版填写, 可能会遇到下载位置错误
          </template>
        </el-alert>
      </div>
    </el-form-item>
    <el-form-item label="剧场版保存位置">
      <div class="download-path-container">
        <el-input v-model:model-value="props.config['ovaDownloadPathTemplate']"/>
        <el-alert
            v-if="!testPathTemplate(props.config['ovaDownloadPathTemplate'])"
            class="download-alert"
            type="warning"
            show-icon
            :closable="false"
        >
          <template #title>
            你的 剧场版保存位置 并未按照模版填写, 可能会遇到下载位置错误
          </template>
        </el-alert>
      </div>
    </el-form-item>
    <el-form-item label="自动删除">
      <div>
        <el-switch v-model:model-value="props.config.delete"/>
        <br>
        <el-text class="mx-1" size="small">
          自动删除已完成的任务
          <br>
          如果同时开启了 <strong>备用rss功能</strong> 将会自动删除对应洗版视频, 以实现 <strong>主rss</strong> 的替换
        </el-text>
        <br>
        <el-checkbox v-model:model-value="props.config.awaitStalledUP"
                     :disabled="!props.config.delete"
                     label="等待做种完毕"/>
        <br>
        <el-checkbox v-model:model-value="props.config.deleteStandbyRSSOnly"
                     :disabled="!props.config.delete"
                     label="仅在主RSS更新后删除备用RSS"/>
        <br>
        <el-text class="mx-1" size="small">
          <strong>主RSS</strong> 将 <span class="download-danger-text">不会自动删除</span>，仅在其更新后删除对应备用RSS的任务与文件
        </el-text>
        <br>
        <el-checkbox v-model:model-value="props.config.deleteFiles"
                     :disabled="!props.config.delete">
          <span class="download-danger-text">删除本地文件</span>
        </el-checkbox>
        <br>
        <el-text class="mx-1" size="small">
          删除本地文件, 仅在同时开启了 <strong>OpenList 上传</strong> 并上传成功后删除
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="失败重试次数">
      <el-input-number v-model:model-value="props.config['downloadRetry']" :max="100" :min="3"/>
    </el-form-item>
    <el-form-item label="同时下载限制">
      <div>
        <el-input-number v-model:model-value="props.config.downloadCount" :min="0"/>
        <div>
          设置为时 0 不做限制
        </div>
      </div>
    </el-form-item>
    <el-form-item label="延迟下载">
      <el-input-number v-model:model-value="props.config.delayedDownload" :min="0">
        <template #suffix>
          <span>分钟</span>
        </template>
      </el-input-number>
    </el-form-item>
    <el-form-item label="检测添加失败">
      <el-switch v-model:model-value="props.config.watchErrorTorrent"/>
    </el-form-item>
    <el-form-item label="优先保留">
      <div class="download-priority-container">
        <el-switch v-model:model-value="props.config.priorityKeywordsEnable"/>
        <div>
          <el-text class="mx-1" size="small">
            启用多文件种子的文件优先保留过滤
          </el-text>
        </div>
        <div v-if="props.config.priorityKeywordsEnable">
          <PrioKeys
              v-model:keywords="props.config.priorityKeywords"
              :import-global="false"
              :show-text="true"
          />
        </div>
      </div>
    </el-form-item>
    <el-form-item label="自定义标签">
      <custom-tags :config="props.config"/>
    </el-form-item>
    <el-collapse v-model="activeName">
      <el-collapse-item name="qBittorrent" title="qBittorrent 设置">
        <QBittorrent v-if="activeName.indexOf('qBittorrent') > -1" :config="props.config"/>
      </el-collapse-item>
      <el-collapse-item name="OpenList" title="OpenList 设置">
        <OpenList v-if="activeName.indexOf('OpenList') > -1" :config="props.config"/>
      </el-collapse-item>
    </el-collapse>
  </el-form>
</template>

<script setup>
import {ref} from "vue";
import api from "@/js/api.js";
import {ElMessage, ElText} from "element-plus";
import {Key, User} from "@element-plus/icons-vue";
import QBittorrent from "@/config/download/qBittorrent.vue";
import OpenList from "@/config/download/OpenList.vue";
import PrioKeys from "@/config/PrioKeys.vue";
import CustomTags from "@/config/CustomTags.vue";

const downloadSelect = ref([
  'qBittorrent',
  'Transmission',
  'Aria2',
  'OpenList'
])

const offlineList = ref([
  {
    label: '115 网盘',
    value: '115 Cloud'
  },
  {
    label: '115 开放平台',
    value: '115 Open'
  },
  {
    label: '123 网盘',
    value: '123Pan'
  },
  {
    label: '123 开放平台',
    value: '123 Open'
  },
  {
    label: '迅雷',
    value: 'Thunder'
  },
  {
    label: 'PikPak',
    value: 'PikPak'
  }
])

const downloadLoginTestLoading = ref(false)
const downloadLoginTest = () => {
  downloadLoginTestLoading.value = true
  api.post("api/downloadLoginTestLoading", props.config)
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        downloadLoginTestLoading.value = false
      })
}

let testPathTemplate = (path) => {
  return new RegExp('\\$\{[A-z]+\}').test(path);
}

let activeName = ref([])

let props = defineProps(['config'])
</script>

<style scoped>
.download-form {
  width: 100%;
}

.download-provider-select {
  width: 150px;
}

.download-test-button {
  display: flex;
  width: 100%;
  justify-content: end;
}

.download-path-container {
  width: 100%;
}

.download-alert {
  margin-top: 8px;
}

.download-danger-text {
  color: red;
}

.download-priority-container {
  width: 100%;
}
</style>
