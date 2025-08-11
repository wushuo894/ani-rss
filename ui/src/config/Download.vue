<template>
  <el-form label-width="auto"
           style="width: 100%"
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
    <template v-else-if="props.config.downloadToolType === 'Alist'">
      <el-form-item label="AlistToken">
        <el-input v-model:model-value="props.config.downloadToolPassword" placeholder="alist-xxxxxx" show-password>
          <template #prefix>
            <el-icon class="el-input__icon">
              <Key/>
            </el-icon>
          </template>
        </el-input>
        <br/>
        <el-text class="mx-1" size="small">
          请设置好 <strong>保存位置</strong> 才能通过测试<br/>
          请在 alist -> 设置-> 其他 -> 配置临时目录<br/>
          支持离线下载到 115、PikPak、迅雷云盘
        </el-text>
      </el-form-item>
      <el-form-item label="Driver">
        <el-select v-model="props.config['provider']" style="width: 150px;">
          <el-option v-for="it in ['115 Cloud','115 Open', 'Thunder', 'PikPak']" :key="it" :label="it" :value="it"/>
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
        <el-input v-model:model-value="props.config.downloadToolUsername" placeholder="username">
          <template #prefix>
            <el-icon class="el-input__icon">
              <User/>
            </el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model:model-value="props.config.downloadToolPassword" placeholder="password" show-password>
          <template #prefix>
            <el-icon class="el-input__icon">
              <Key/>
            </el-icon>
          </template>
        </el-input>
      </el-form-item>
    </template>
    <el-form-item>
      <div style="display:flex;width: 100%;justify-content: end;">
        <el-button @click="downloadLoginTest" bg text :loading="downloadLoginTestLoading" icon="Odometer">测试
        </el-button>
      </div>
    </el-form-item>
    <el-form-item label="保存位置">
      <div style="width: 100%;">
        <el-input v-model:model-value="props.config['downloadPathTemplate']"/>
        <el-alert
            v-if="!testPathTemplate(props.config['downloadPathTemplate'])"
            style="margin-top: 8px;"
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
      <div style="width: 100%;">
        <el-input v-model:model-value="props.config['ovaDownloadPathTemplate']"/>
        <el-alert
            v-if="!testPathTemplate(props.config['ovaDownloadPathTemplate'])"
            style="margin-top: 8px;"
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
    <el-form-item label="plex标题命名风格">
      <div>
        <el-switch v-model:model-value="props.config['plexTitleMode']"/>
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
          <strong>主RSS</strong> 将 <span style="color: red;">不会自动删除</span>，仅在其更新后删除对应备用RSS的任务与文件
        </el-text>
        <br>
        <el-checkbox v-model:model-value="props.config.deleteFiles">
          <span style="color: red;">删除本地文件</span>
        </el-checkbox>
        <br>
        <el-text class="mx-1" size="small">
          删除本地文件, 仅在同时开启了 <strong>alist上传</strong> 并上传成功后删除
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
    <el-form-item label="检测是否死种">
      <el-switch v-model:model-value="props.config.watchErrorTorrent"/>
    </el-form-item>
    <el-form-item label="优先保留">
      <div style="width: 100%">
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
      <el-collapse-item name="qb" title="qBittorrent 设置">
        <q-bittorrent v-if="activeName.indexOf('qb') > -1" :config="props.config"/>
      </el-collapse-item>
      <el-collapse-item name="alist" title="Alist 设置">
        <alist v-if="activeName.indexOf('alist') > -1" :config="props.config"/>
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
import Alist from "@/config/download/Alist.vue";
import PrioKeys from "@/config/PrioKeys.vue";
import CustomTags from "@/config/CustomTags.vue";

const downloadSelect = ref([
  'qBittorrent',
  'Transmission',
  'Aria2',
  'Alist'
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
