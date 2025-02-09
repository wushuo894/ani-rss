<template>
  <el-form label-width="auto"
           style="width: 100%"
           @submit="(event)=>{
                    event.preventDefault()
                   }">
    <el-form-item label="下载工具">
      <el-select v-model:model-value="props.config.download">
        <el-option v-for="item in downloadSelect"
                   :key="item"
                   :label="item"
                   :value="item"/>
      </el-select>
    </el-form-item>
    <el-form-item label="地址">
      <el-input v-model:model-value="props.config.host" placeholder="http://192.168.1.x:8080"></el-input>
    </el-form-item>
    <template v-if="props.config.download !== 'Aria2'">
      <el-form-item label="用户名">
        <el-input v-model:model-value="props.config.username" placeholder="username"></el-input>
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model:model-value="props.config.password" placeholder="password" show-password/>
      </el-form-item>
    </template>
    <el-form-item label="RPC 密钥" v-else>
      <el-input show-password v-model:model-value="props.config.password" placeholder=""></el-input>
    </el-form-item>
    <el-form-item>
      <div style="display:flex;width: 100%;justify-content: end;">
        <el-button @click="downloadLoginTest" bg text :loading="downloadLoginTestLoading" icon="Odometer">测试
        </el-button>
      </div>
    </el-form-item>
    <el-form-item label="保存位置">
      <el-input v-model:model-value="props.config.downloadPath" placeholder="/Media/番剧"></el-input>
    </el-form-item>
    <el-form-item label="剧场版保存位置">
      <el-input v-model:model-value="props.config.ovaDownloadPath" placeholder="/Media/剧场版"></el-input>
    </el-form-item>
    <el-form-item label="自动删除">
      <div>
        <el-switch v-model:model-value="props.config.delete"/>
        <br>
        <el-text class="mx-1" size="small">
          自动删除已完成的任务, 不会删除本地文件
          <br>
          如果同时开启了 <strong>备用rss功能</strong> 将会自动删除对应洗版视频, 以实现 <strong>主rss</strong> 的替换
        </el-text>
        <br>
        <el-checkbox v-model:model-value="props.config.awaitStalledUP"
                     :disabled="!props.config.delete"
                     label="等待做种完毕"/>
        <br>
        <el-checkbox v-model:model-value="props.config.deleteBackRSSOnly"
                     :disabled="!props.config.delete"
                     label="仅在主RSS更新后删除备用RSS"/>
        <br>
        <el-text class="mx-1" size="small">
          主RSS将不会自动删除，仅在其更新后删除对应备用RSS的任务与文件
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="拼音首字母">
      <div>
        <el-switch v-model:model-value="props.config.acronym" :disabled="props.config.quarter"/>
        <br>
        <el-text class="mx-1" size="small">
          存放到 #,0,A-Z 文件夹下
        </el-text>
      </div>
    </el-form-item>
    <el-form-item label="季度">
      <div>
        <el-switch v-model:model-value="props.config.quarter" :disabled="props.config.acronym"></el-switch>
        <br>
        <el-text class="mx-1" size="small">
          按季度存放, 如 2024-07
        </el-text>
      </div>
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
    <el-collapse>
      <el-collapse-item title="qBittorrent设置">
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
                       :disabled="config.download !== 'qBittorrent'"/>
            <br>
            <el-text class="mx-1" size="small">
              开启后将使用qBittorrent的临时下载位置 (最终下载位置不受影响)
            </el-text>
          </div>
        </el-form-item>
      </el-collapse-item>
      <el-collapse-item title="Alist">
        <el-form-item label="AlistHost">
          <el-input v-model:model-value="props.config['alistHost']" placeholder="http://127.0.0.1:5244"/>
        </el-form-item>
        <el-form-item label="AlistToken">
          <el-input v-model:model-value="props.config['alistToken']" placeholder="alist-xxxxxx"/>
        </el-form-item>
        <el-form-item label="AlistPath">
          <el-input v-model:model-value="props.config['alistPath']" placeholder="/"/>
        </el-form-item>
        <el-form-item label="开关">
          <div style="width: 100%">
            <div>
              <el-switch v-model:model-value="props.config['alist']"/>
            </div>
            <div>
              <el-text class="mx-1" size="small">
                自动将下载完成的文件上传至alist
              </el-text>
            </div>
            <el-checkbox v-model:model-value="props.config['alistDelete']">上传完成后删除原文件</el-checkbox>
          </div>
        </el-form-item>
      </el-collapse-item>
    </el-collapse>

  </el-form>
</template>

<script setup>


import {ref} from "vue";
import api from "../api.js";
import {ElMessage} from "element-plus";

const downloadSelect = ref([
  'qBittorrent',
  'Transmission',
  'Aria2'
])

const downloadLoginTestLoading = ref(false)
const downloadLoginTest = () => {
  if (props.config.host.endsWith("/")) {
    props.config.host = props.config.host.substring(0, props.config.host.length - 1);
  }
  downloadLoginTestLoading.value = true
  api.post("api/downloadLoginTestLoading", props.config)
      .then(res => {
        ElMessage.success(res.message)
      })
      .finally(() => {
        downloadLoginTestLoading.value = false
      })
}

let props = defineProps(['config'])
</script>
