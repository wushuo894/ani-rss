<template>
  <div>
    <el-collapse v-model:model-value="activeName" accordion>
      <el-collapse-item title="页面设置" name="1">
        <el-form label-width="auto">
          <el-form-item label="按星期展示">
            <el-switch v-model:model-value="props.config.weekShow"/>
          </el-form-item>
          <el-form-item label="显示评分">
            <el-switch v-model:model-value="props.config.scoreShow"/>
          </el-form-item>
          <el-form-item label="显示视频列表">
            <el-switch v-model:model-value="props.config.showPlaylist"/>
          </el-form-item>
        </el-form>
      </el-collapse-item>
      <el-collapse-item title="添加订阅" name="2">
        <el-form label-width="auto">
          <el-form-item label="只下载最新集">
            <el-switch v-model:model-value="props.config.downloadNew"/>
          </el-form-item>
          <el-form-item label="标题添加年份">
            <el-switch v-model:model-value="props.config.titleYear"/>
          </el-form-item>
          <el-form-item label="自动推断剧集偏移">
            <el-switch v-model:model-value="props.config.offset"/>
          </el-form-item>
          <el-form-item label="TMDB标题">
            <div>
              <el-switch v-model:model-value="props.config.tmdb"/>
              <br>
              <el-text class="mx-1" size="small">
                自动使用TMDB的标题
              </el-text>
            </div>
          </el-form-item>
          <el-form-item label="默认开启全局排除">
            <el-switch v-model:model-value="props.config.enabledExclude" :disabled="props.config.importExclude"/>
          </el-form-item>
          <el-form-item label="默认导入全局排除">
            <el-switch v-model:model-value="props.config.importExclude" :disabled="props.config.enabledExclude"/>
          </el-form-item>
        </el-form>
      </el-collapse-item>
      <el-collapse-item title="重命名设置" name="3">
        <el-form label-width="auto">
          <el-form-item label="自动重命名">
            <el-switch v-model:model-value="props.config.rename"/>
          </el-form-item>
          <el-form-item label="重命名间隔(分钟)">
            <el-input-number v-model:model-value="props.config.renameSleep" min="1"
                             :disabled="!config.rename"/>
          </el-form-item>
          <el-form-item label="季命名方式">
            <el-select v-model:model-value="props.config.seasonName" style="width: 150px">
              <el-option v-for="it in ['Season 1','Season 01','S1','S01','None']" :key="it" :label="it" :value="it"/>
            </el-select>
          </el-form-item>
          <el-form-item label="重命名模版">
            <div style="width: 100%">
              <el-input v-model:model-value="props.config.renameTemplate"/>
              <br>
              <el-text class="mx-1" size="small">
                ${title} 标题 , ${subgroup} 字幕组 <br>
                ${seasonFormat} 季 01 ,
                ${episodeFormat} 集 01 <br>
                ${season} 季 1 ,
                ${episode} 集 1
                <br>
                请务必保留
                <strong>
                  ${title} S${seasonFormat}E${episodeFormat}
                </strong>。
                <br>
                <strong>
                  如果想使用洗版功能请不要在重命名模版添加 ${subgroup},
                  <br>
                  否则可能会出现 备用rss 洗版到 主rss 时出现错误 (点名 windows)
                </strong>
              </el-text>
            </div>
          </el-form-item>
        </el-form>
      </el-collapse-item>
      <el-collapse-item title="RSS设置" name="4">
        <el-form label-width="auto">
          <el-form-item label="RSS开关">
            <el-switch v-model:model-value="props.config.rss"/>
          </el-form-item>
          <el-form-item label="RSS间隔(分钟)">
            <el-input-number v-model:model-value="props.config.sleep" :disabled="!props.config.rss" :min="5"/>
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
          <el-form-item label="自动禁用订阅">
            <div>
              <el-switch v-model:model-value="props.config.autoDisabled"/>
              <br>
              <el-text class="mx-1" size="small">
                根据 Bangumi 获取总集数 当所有集数都已下载时自动禁用该订阅
              </el-text>
            </div>
          </el-form-item>
          <el-form-item label="自动跳过X.5集">
            <el-switch v-model:model-value="props.config.skip5"/>
          </el-form-item>
          <el-form-item label="遗漏检测">
            <div>
              <div>
                <el-switch v-model:model-value="props.config.omit"/>
              </div>
              <el-text class="mx-1" size="small">
                总开关 若检测到RSS中集数出现遗漏会发送通知
              </el-text>
            </div>
          </el-form-item>
          <el-form-item label="备用RSS">
            <div>
              <el-switch v-model:model-value="props.config.backRss"/>
              <div style="display: flex;width: 100%;justify-content: end;padding-top: 6px;">
                <el-text class="mx-1" size="small">
                  务必使用默认重命名模版
                  <br>
                  请勿将动漫花园的rss用作 <strong>其他</strong> rss的备用或主RSS
                  <br>
                  使用备用rss请同时开启qb的 <strong>修改任务标题</strong>
                  <br>
                  对 tr 与 aria2 的兼容性 <strong>不太稳定</strong>
                  <br>
                  若开启了 <strong>自动删除</strong> 将会 <strong>自动替换</strong> 备用rss 为 主rss 版本
                  (需要映射路径与下载器一致，否则若旧视频为mp4新视频为mkv时无法完成自动删除旧视频)
                </el-text>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </el-collapse-item>
      <el-collapse-item name="5" title="Trackers">
        <el-form label-width="auto">
          <el-form-item label="trackers更新地址">
            <div style="width: 100%">
              <div>
                <el-input v-model:model-value="props.config.trackersUpdateUrls" placeholder="换行输入多个"
                          style="width: 100%" type="textarea"/>
              </div>
              <div style="height: 12px;"/>
              <div style="display: flex;justify-content: space-between;">
                <el-checkbox v-model:model-value="props.config.autoTrackersUpdate" label="每天1:00自动更新"/>
                <el-button :loading="trackersUpdateLoading" bg text @click="trackersUpdate">更新</el-button>
              </div>
              <div>
                <el-text class="mx-1" size="small">
                  该功能暂不支持 Transmission
                </el-text>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </el-collapse-item>
      <el-collapse-item name="6" title="其他">
        <el-form label-width="auto">
          <el-form-item label="Mikan">
            <el-input v-model:model-value="props.config.mikanHost" placeholder="https://mikanime.tv"/>
          </el-form-item>
          <el-form-item label="BgmToken">
            <div style="width: 100%;">
              <el-input v-model:model-value="props.config.bgmToken"/>
              <div>
                <el-text class="mx-1" size="small">
                  你可以在 <a target="_blank" href="https://next.bgm.tv/demo/access-token">https://next.bgm.tv/demo/access-token</a>
                  生成一个 Access Token
                  <br>
                  <a target="_blank" href="http://docs.wushuo.top/docs#emby-webhook通知设置">支持自动点格子</a>
                </el-text>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="禁止公网访问">
            <el-switch v-model:model-value="props.config.innerIP"/>
          </el-form-item>
          <el-form-item label="最大日志条数">
            <div style="width: 150px">
              <el-select v-model:model-value="props.config.logsMax">
                <el-option v-for="it in [1024,2048,4096,8192]" :key="it" :label="it" :value="it"/>
              </el-select>
            </div>
          </el-form-item>
          <el-form-item label="DEBUG">
            <el-switch v-model:model-value="props.config.debug"/>
          </el-form-item>
        </el-form>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import {ElMessage, ElText} from "element-plus";
import {ref} from "vue";
import api from "../api.js";

let activeName = ref('1')

let trackersUpdateLoading = ref(false)

let trackersUpdate = () => {
  trackersUpdateLoading.value = true
  api.post('api/trackersUpdate', props.config)
      .then(res => {
        ElMessage.success(res.message);
      })
      .finally(() => {
        trackersUpdateLoading.value = false
      })
}

let props = defineProps(['config'])
</script>
