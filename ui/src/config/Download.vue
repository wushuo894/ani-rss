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
          <el-option v-for="it in ['115 Cloud', 'Thunder', 'PikPak']" :key="it" :label="it" :value="it"/>
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
      <el-input v-model:model-value="props.config['downloadPathTemplate']"></el-input>
    </el-form-item>
    <el-form-item label="剧场版保存位置">
      <el-input v-model:model-value="props.config['ovaDownloadPathTemplate']"></el-input>
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
    <el-collapse>
      <el-collapse-item title="qBittorrent 设置">
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
      </el-collapse-item>
      <el-collapse-item title="Alist 设置">
        <el-form-item label="AlistHost">
          <el-input v-model:model-value="props.config['alistHost']" placeholder="http://127.0.0.1:5244"/>
        </el-form-item>
        <el-form-item label="AlistToken">
          <el-input v-model:model-value="props.config['alistToken']" placeholder="alist-xxxxxx"/>
        </el-form-item>
        <el-form-item label="上传位置">
          <el-input v-model:model-value="props.config['alistPath']" placeholder="/"/>
        </el-form-item>
        <el-form-item label="剧场版上传位置">
          <el-input v-model:model-value="props.config['alistOvaPath']" placeholder="/"/>
        </el-form-item>
        <el-form-item label="失败重试次数">
          <el-input-number v-model:model-value="props.config['alistRetry']" :max="100" :min="1"/>
        </el-form-item>
        <el-form-item label="上传开关">
          <div style="width: 100%">
            <div>
              <el-switch v-model="props.config['alist']"/>
            </div>
            <div>
              <el-checkbox v-model="props.config['alistTask']" :disabled="!props.config['alist']" label="添加为任务"/>
            </div>
            <div>
              <el-text class="mx-1" size="small">
                自动将下载完成的文件上传至 alist
              </el-text>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="刷新开关">
          <div style="width: 100%">
            <div>
              <el-switch v-model:model-value="props.config['alistRefresh']"/>
            </div>
            <div>
              <el-text class="mx-1" size="small">
                刷新 alist 上传路径的文件列表
              </el-text>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="刷新延迟">
          <el-input-number v-model="props.config['alistRefreshDelayed']" :min="0">
            <template #suffix>
              <span>秒</span>
            </template>
          </el-input-number>
        </el-form-item>
      </el-collapse-item>
    </el-collapse>

  </el-form>
</template>

<script setup>


import {ref} from "vue";
import api from "@/js/api.js";
import {ElMessage} from "element-plus";
import {Key, User} from "@element-plus/icons-vue";

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

let props = defineProps(['config'])
</script>
