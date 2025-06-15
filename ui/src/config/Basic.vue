<template>
  <div>
    <el-collapse v-model:model-value="activeName" accordion>
      <el-collapse-item title="页面设置" name="1">
        <el-form label-width="auto">
          <el-form-item label="外观">
            <el-radio-group v-model="store">
              <el-radio-button label="自动" value="auto"/>
              <el-radio-button label="浅色" value="light"/>
              <el-radio-button label="深色" value="dark"/>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="主题色">
            <el-color-picker v-model="color" :predefine="predefineColors"
                             @active-change="colorChange"
                             @change="colorChange(color)"
                             @blur="colorChange(color)"/>
          </el-form-item>
          <el-form-item label="按星期展示">
            <el-switch v-model:model-value="props.config.weekShow"/>
          </el-form-item>
          <el-form-item label="显示评分">
            <el-switch v-model:model-value="props.config.scoreShow"/>
          </el-form-item>
          <el-form-item label="显示视频列表">
            <el-switch v-model:model-value="props.config.showPlaylist"/>
          </el-form-item>
          <el-form-item label="显示更新时间">
            <el-switch v-model:model-value="props.config['showLastDownloadTime']"/>
          </el-form-item>
          <el-form-item label="自定义CSS">
            <div style="width: 100%;">
              <el-input v-model:model-value="props.config['customCss']" type="textarea"
                        placeholder="" :autosize="{ minRows: 2, maxRows: 4 }"/>
              <br>
              <div class="flex" style="justify-content:end; width: 100%;">
                <a href="https://github.com/wushuo894/ani-rss-css"
                   target="_blank">更多CSS</a>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="自定义JS">
            <el-input v-model:model-value="props.config['customJs']" type="textarea"
                      placeholder="" :autosize="{ minRows: 2, maxRows: 4 }"/>
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
          <el-form-item label="BGM日文标题">
            <el-switch v-model:model-value="props.config['bgmJpName']"/>
          </el-form-item>
          <el-form-item label="TMDB ID">
            <div>
              <el-switch v-model:model-value="props.config.tmdbId"/>
              <br>
              <el-text class="mx-1" size="small">
                自动获取tmdbId, 如: 女仆冥土小姐。 [tmdbid=242143]
              </el-text>
            </div>
          </el-form-item>
          <el-form-item label="TMDB标题">
            <div>
              <el-switch v-model:model-value="props.config.tmdb"/>
              <br>
              <el-text class="mx-1" size="small">
                自动使用TMDB的标题
              </el-text>
              <br>
              <el-checkbox v-model="props.config['tmdbAnime']" label="仅获取动漫"/>
            </div>
          </el-form-item>
          <el-form-item label="TMDB语言">
            <div>
              <div>
                <el-select v-model:model-value="props.config['tmdbLanguage']" style="width: 150px;">
                  <el-option v-for="language in ['zh-CN','zh-TW','ja-JP','en-US']" :value="language"
                             :key="language"></el-option>
                </el-select>
              </div>
              <div v-if="props.config['tmdbLanguage'] === 'ja-JP'">
                <el-checkbox v-model="props.config['tmdbRomaji']" label="优先获取罗马音"/>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="tvshow.nfo">
            <div>
              <el-switch v-model:model-value="props.config['tvShowNfo']"/>
              <br>
              <el-text class="mx-1" size="small">
                自动生成 <strong>tvshow.nfo</strong> 文件, 内包含 <strong>tmdbid</strong> 更方便 <strong>emby</strong>
                识别
              </el-text>
            </div>
          </el-form-item>
          <el-form-item label="默认开启全局排除">
            <el-switch v-model:model-value="props.config.enabledExclude" :disabled="props.config.importExclude"/>
          </el-form-item>
          <el-form-item label="默认导入全局排除">
            <el-switch v-model:model-value="props.config.importExclude" :disabled="props.config.enabledExclude"/>
          </el-form-item>
          <el-form-item label="封面质量">
            <el-select v-model:model-value="props.config['bgmImage']" style="width: 150px;">
              <el-option v-for="item in ['small','grid','large','medium','common']" :value="item"
                         :key="item"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="自定义集数规则">
            <div style="width: 100%;">
              <div>
                <el-switch v-model:model-value="props.config.customEpisode"/>
              </div>
              <div style="display: flex;width: 100%;">
                <el-input style="width: 100%"
                          v-model:model-value="props.config.customEpisodeStr"/>
                <div style="width: 4px;"></div>
                <el-input-number v-model:model-value="props.config.customEpisodeGroupIndex"/>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="自动上传">
            <el-switch v-model:model-value="props.config['upload']"/>
          </el-form-item>
        </el-form>
      </el-collapse-item>
      <el-collapse-item title="重命名设置" name="3">
        <el-form label-width="auto">
          <el-form-item label="自动重命名">
            <el-switch v-model:model-value="props.config.rename"/>
          </el-form-item>
          <el-form-item label="重命名间隔">
            <el-input-number v-model:model-value="props.config['renameSleepSeconds']"
                             :min="5"
                             :disabled="!config.rename">
              <template #suffix>
                <span>秒</span>
              </template>
            </el-input-number>
          </el-form-item>
          <el-form-item label="重命名模版">
            <div style="width: 100%">
              <el-input v-model:model-value="props.config.renameTemplate"
                        placeholder="${title} S${seasonFormat}E${episodeFormat}"/>
              <br>
              <el-text class="mx-1" size="small">
                <a href="https://docs.wushuo.top/config/basic/rename#rename-template"
                   target="_blank">详细说明</a>
              </el-text>
            </div>
          </el-form-item>
          <el-form-item label="剔除年份">
            <div>
              <el-switch v-model:model-value="props.config.renameDelYear"/>
              <br>
              <el-text class="mx-1" size="small">
                重命名时 ${title} 剔除 年份, 如 (2024)
              </el-text>
            </div>
          </el-form-item>
          <el-form-item label="剔除TMDB ID">
            <div>
              <el-switch v-model:model-value="props.config.renameDelTmdbId"/>
              <br>
              <el-text class="mx-1" size="small">
                重命名时 ${title} 剔除 tmdbid, 如 [tmdbid=242143]
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
          <el-form-item label="RSS间隔">
            <el-input-number v-model:model-value="props.config.sleep" :disabled="!props.config.rss" :min="5">
              <template #suffix>
                <span>分钟</span>
              </template>
            </el-input-number>
          </el-form-item>
          <el-form-item label="RSS超时">
            <el-input-number v-model:model-value="props.config['rssTimeout']"
                             :min="6" :max="60">
              <template #suffix>
                <span>秒</span>
              </template>
            </el-input-number>
          </el-form-item>
          <el-form-item label="自动跳过">
            <div style="width: 100%">
              <el-switch v-model:model-value="props.config.fileExist" :disabled="!config.rename"/>
              <br>
              <el-text class="mx-1" size="small">
                文件已下载自动跳过 此选项必须启用 自动重命名。确保 下载工具 与本程序 docker 映射挂载路径一致
                <a href="https://docs.wushuo.top/config/basic/rss#auto-skip"
                   target="_blank">详细说明</a>
              </el-text>
            </div>
          </el-form-item>
          <el-form-item label="自动禁用订阅">
            <div style="width: 100%;">
              <el-switch v-model:model-value="props.config.autoDisabled"/>
              <br>
              <el-text class="mx-1" size="small">
                根据 Bangumi 获取总集数 当所有集数都已下载时自动禁用该订阅
              </el-text>
              <div>
                <el-checkbox v-model="props.config['updateTotalEpisodeNumber']" label="自动更新总集数"/>
              </div>
              <div>
                <el-checkbox v-model="props.config['completed']" label="订阅完结迁移"
                             :disabled="!props.config['verifyExpirationTime'] || !props.config.autoDisabled"/>
              </div>
              <div>
                <el-input v-model="props.config['completedPathTemplate']"
                          :disabled="!props.config.autoDisabled || !props.config['completed']"/>
              </div>
              <AfdianPrompt :config="props.config" name="订阅完结迁移"/>
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
          <el-form-item label="摸鱼检测">
            <div>
              <div>
                <el-switch v-model="props.config['procrastinating']" :disabled="!props.config['verifyExpirationTime']"/>
              </div>
              <div>
                <el-input-number v-model="props.config['procrastinatingDay']"
                                 :disabled="!props.config['procrastinating']" :max="365"
                                 :min="7">
                  <template #suffix>
                    <span>天</span>
                  </template>
                </el-input-number>
              </div>
              <AfdianPrompt :config="props.config" name="摸鱼检测"/>
              <el-text class="mx-1" size="small">
                检测到主RSS更新摸鱼会发送通知<br>
                建议配合 <strong>自动禁用订阅</strong> 食用
              </el-text>
            </div>
          </el-form-item>
          <el-form-item label="备用RSS">
            <div style="width: 100%">
              <div>
                <el-switch v-model:model-value="props.config.standbyRss"/>
              </div>
              <div>
                <el-checkbox v-model="props.config['coexist']" :disabled="!props.config.standbyRss"
                             label="多字幕组共存模式"/>
              </div>
              <div class="flex" style="width: 100%;justify-content: end;">
                <el-text class="mx-1" size="small">
                  <a href="https://docs.wushuo.top/config/basic/rss#back-rss"
                     target="_blank">详细说明</a>
                </el-text>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </el-collapse-item>
      <el-collapse-item name="5" title="Trackers">
        <el-form label-width="auto">
          <el-form-item label="更新地址">
            <div style="width: 100%">
              <div>
                <el-input v-model:model-value="props.config.trackersUpdateUrls" placeholder="换行输入多个"
                          :autosize="{ minRows: 2}"
                          style="width: 100%" type="textarea"/>
              </div>
              <div style="height: 12px;"/>
              <div class="flex" style="justify-content: space-between;">
                <el-checkbox v-model:model-value="props.config.autoTrackersUpdate" label="每天1:00自动更新"/>
                <el-button :loading="trackersUpdateLoading" bg icon="Refresh" text @click="trackersUpdate">更新
                </el-button>
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
          <el-form-item label="TmdbApi">
            <el-input v-model:model-value="props.config['tmdbApi']" placeholder="https://api.themoviedb.org"/>
          </el-form-item>
          <el-form-item label="TmdbApiKey">
            <el-input v-model:model-value="props.config['tmdbApiKey']" placeholder="请自备 API 密钥, 留空使用系统默认"/>
          </el-form-item>
          <el-form-item label="BgmToken">
            <div style="width: 100%;">
              <el-input v-model:model-value="props.config.bgmToken" show-password type="password"/>
              <div>
                <el-text class="mx-1" size="small">
                  你可以在 <a target="_blank" href="https://next.bgm.tv/demo/access-token">https://next.bgm.tv/demo/access-token</a>
                  生成一个 Access Token
                  <br>
                  <a href="http://docs.wushuo.top/config/basic/other#emby-webhook" target="_blank">支持自动点格子</a>
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
          <el-form-item label="自动更新">
            <div style="width: 100%;">
              <div>
                <el-switch v-model:model-value="props.config.autoUpdate"/>
              </div>
              <div>
                <el-text class="mx-1" size="small">
                  每天 06:00 自动更新程序
                </el-text>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="GitHub加速">
            <div>
              <div>
                <el-select v-model="props.config['github']" style="width: 150px;">
                  <el-option v-for="it in githubList" :key="it" :label="it" :value="it"/>
                </el-select>
              </div>
              <div>
                <el-checkbox v-model="props.config['customGithub']" label="自定义"/>
              </div>
              <div v-if="props.config['customGithub']">
                <el-input v-model="props.config['customGithubUrl']" placeholder="https://xxxx.com"/>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="DEBUG">
            <el-switch v-model:model-value="props.config.debug"/>
          </el-form-item>
          <el-form-item label="缓存">
            <div style="width: 100%;">
              <div>
                <el-button :loading="clearCacheLoading" bg icon="Delete" text @click="clearCache">清理</el-button>
              </div>
              <div>
                <el-text class="mx-1" size="small">
                  清理现在不被使用的缓存
                </el-text>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="自动GC间隔">
            <el-input-number v-model:model-value="props.config['gcSleep']" :min="0">
              <template #suffix>
                <span>分钟</span>
              </template>
            </el-input-number>
          </el-form-item>
          <el-form-item label="自动备份配置">
            <div>
              <el-switch v-model="props.config['configBackup']"/>
              <br>
              <el-input-number v-model="props.config['configBackupDay']" :min="1">
                <template #suffix>
                  <span>天</span>
                </template>
              </el-input-number>
            </div>
          </el-form-item>
        </el-form>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import {ElMessage, ElText} from "element-plus";
import {ref} from "vue";
import api from "../js/api.js";
import {useColorMode, useLocalStorage} from "@vueuse/core";
import AfdianPrompt from "../other/AfdianPrompt.vue";

let githubList = new Set([
  'None',
  'gh-proxy.com',
  'gh.h233.eu.org',
  'ghproxy.1888866.xyz',
  'slink.ltd',
  'hub.gitmirror.com',
  'github.boki.moe',
  'github.moeyy.xyz',
  'gh-proxy.net',
  'ghfast.top',
  'pd.zwc365.com',
  'ghproxy.cfd',
  'gh.jasonzeng.dev',
  'gh.monlor.com',
  'github.tbedu.top',
  'gh-proxy.linioi.com',
  'mirrors.chenby.cn',
  'github.ednovas.xyz',
  'ghp.keleyaa.com',
  'github.wuzhij.com',
  'gh.cache.cloudns.org',
  'gh.chjina.com',
  'ghpxy.hwinzniej.top',
  'cdn.crashmc.com',
  'gitproxy.mrhjx.cn',
  'gh.xxooo.cf',
  'gh.944446.xyz',
  'api-gh.muran.eu.org',
  'gh.zwnes.xyz',
  'gh.llkk.cc',
  'gh-proxy.ygxz.in',
  'gh.nxnow.top',
  'gh-proxy.ygxz.in',
  'gh.zwy.one',
  'ghproxy.monkeyray.net',
  'gh.xx9527.cn',
  'ghfast.top'
])

console.log(githubList);

const {store} = useColorMode()

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


let clearCacheLoading = ref(false)
let clearCache = () => {
  clearCacheLoading.value = true
  api.post('api/clearCache')
      .then(res => {
        ElMessage.success(res.message);
      })
      .finally(() => {
        clearCacheLoading.value = false
      })
}

let predefineColors = ref([
  '#409eff', '#109D58', '#BF3545', '#CB7574',
  '#9AAEC7', '#2EC5B6', '#1C1C1C', '#F7B1A9',
  '#B18874', '#E9BA86', '#F68F6C', '#F0458B',
  '#C35653', '#40494E', '#6F0000', '#8D3647',
  '#E6C5D0', '#2377B3', '#49312D', '#7C9AB6',
  '#A5B18D', '#E8662A', '#AB5D50'
])

let color = useLocalStorage('--el-color-primary', '#409eff')

let colorChange = (v) => {
  // document.documentElement 是全局变量时
  const el = document.documentElement
  // const el = document.getElementById('xxx')

  // 获取 css 变量
  getComputedStyle(el).getPropertyValue(`--el-color-primary`)

  // 设置 css 变量
  el.style.setProperty('--el-color-primary', v)
}

let props = defineProps(['config'])
</script>
